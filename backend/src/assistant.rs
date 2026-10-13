//! Server-owned Claude credentials. No prompts, responses, or credentials are logged.
use axum::{
    Json,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use std::{
    collections::{HashSet, VecDeque},
    time::{Duration, Instant},
};
use tokio::sync::{Mutex, Semaphore};
use uuid::Uuid;

const SYSTEM: &str = include_str!("recipe-prompt.txt");
#[derive(Deserialize, Serialize)]
pub struct Message {
    pub role: String,
    pub content: String,
}
#[derive(Deserialize)]
pub struct Chat {
    pub model: String,
    pub messages: Vec<Message>,
}
#[derive(Debug)]
pub struct Failure {
    pub status: StatusCode,
    pub code: &'static str,
    pub message: &'static str,
    pub fallback: bool,
}
impl IntoResponse for Failure {
    fn into_response(self) -> Response {
        (
            self.status,
            Json(json!({"error":self.message,"code":self.code,"pccFallbackAllowed":self.fallback})),
        )
            .into_response()
    }
}
fn failure(
    status: StatusCode,
    code: &'static str,
    message: &'static str,
    fallback: bool,
) -> Failure {
    Failure {
        status,
        code,
        message,
        fallback,
    }
}
fn unavailable() -> Failure {
    failure(
        StatusCode::SERVICE_UNAVAILABLE,
        "provider_unavailable",
        "Claude is unavailable. You can try Apple Private Cloud Compute.",
        true,
    )
}
impl Chat {
    pub fn model_id(&self) -> Result<&'static str, Failure> {
        match self.model.as_str() {
            "sonnet" => Ok("claude-sonnet-5-5"),
            "opus" => Ok("claude-opus-5-5"),
            "claude-haiku-4-5" | "haiku" => Ok("claude-haiku-5-5"),
            _ => Err(failure(
                StatusCode::BAD_REQUEST,
                "invalid_request",
                "Choose a supported assistant model.",
                false,
            )),
        }
    }
    pub fn validate(&self) -> Result<(), Failure> {
        self.model_id()?;
        if self.messages.is_empty()
            || self.messages.len() > 30
            || self.messages.last().is_none_or(|m| m.role != "user")
            || self.messages.iter().any(|m| {
                !["user", "assistant"].contains(&m.role.as_str())
                    || m.content.trim().is_empty()
                    || m.content.encode_utf16().count() > 40_000
            })
        {
            return Err(failure(
                StatusCode::BAD_REQUEST,
                "invalid_request",
                "Send up to 30 messages, each under 40,000 characters, ending with your request.",
                false,
            ));
        }
        Ok(())
    }
}
pub struct Assistant {
    client: reqwest::Client,
    key: String,
    allowed: HashSet<Uuid>,
    slots: Semaphore,
    requests: Mutex<VecDeque<Instant>>,
    endpoint: String,
}
impl Assistant {
    pub fn from_env() -> Result<Option<Self>, Box<dyn std::error::Error>> {
        let key = std::env::var("ANTHROPIC_API_KEY").unwrap_or_default();
        if key.trim().is_empty() {
            return Ok(None);
        }
        let allowed = std::env::var("ASSISTANT_ALLOWED_USER_IDS")
            .unwrap_or_default()
            .split(',')
            .filter(|s| !s.trim().is_empty())
            .map(|s| s.trim().parse())
            .collect::<Result<HashSet<Uuid>, _>>()?;
        if allowed.is_empty() {
            return Err(
                "ASSISTANT_ALLOWED_USER_IDS must contain backend user UUIDs when Claude is enabled"
                    .into(),
            );
        }
        Ok(Some(Self::new(key, allowed)?))
    }
    pub fn new(key: String, allowed: HashSet<Uuid>) -> Result<Self, reqwest::Error> {
        Ok(Self {
            client: reqwest::Client::builder()
                .timeout(Duration::from_secs(100))
                .redirect(reqwest::redirect::Policy::none())
                .build()?,
            key,
            allowed,
            slots: Semaphore::new(2),
            requests: Mutex::new(VecDeque::new()),
            endpoint: "https://api.anthropic.com/v1/messages".into(),
        })
    }
    pub fn allows(&self, user: Uuid) -> bool {
        self.allowed.contains(&user)
    }
    pub async fn chat(&self, user: Uuid, input: Chat) -> Result<Value, Failure> {
        if !self.allows(user) {
            return Err(failure(
                StatusCode::FORBIDDEN,
                "assistant_forbidden",
                "Claude access is not enabled for this account.",
                false,
            ));
        }
        input.validate()?;
        let _slot = self.slots.try_acquire().map_err(|_| {
            failure(
                StatusCode::TOO_MANY_REQUESTS,
                "assistant_busy",
                "Claude is busy. Try again shortly or use Apple Private Cloud Compute.",
                true,
            )
        })?;
        {
            let mut requests = self.requests.lock().await;
            let now = Instant::now();
            while requests
                .front()
                .is_some_and(|t| now.duration_since(*t) >= Duration::from_secs(60))
            {
                requests.pop_front();
            }
            if requests.len() >= 20 {
                return Err(failure(
                    StatusCode::TOO_MANY_REQUESTS,
                    "assistant_busy",
                    "Claude request limit reached. Try again shortly or use Apple Private Cloud Compute.",
                    true,
                ));
            }
            requests.push_back(now);
        }
        let body = json!({"model": input.model_id()?, "max_tokens": 8192, "system": SYSTEM, "messages": input.messages,
            "tools": [{"type":"web_search_20250305","name":"web_search","max_uses":3}]});
        // Never retry paid requests automatically; a lost response may still be billed.
        let mut response = self
            .client
            .post(&self.endpoint)
            .header("x-api-key", &self.key)
            .header("anthropic-version", "2023-06-01")
            .json(&body)
            .send()
            .await
            .map_err(|_| unavailable())?;
        let status = response.status();
        let mut bytes = Vec::new();
        while let Some(chunk) = response.chunk().await.map_err(|_| unavailable())? {
            if bytes.len() + chunk.len() > 2_000_000 {
                return Err(unavailable());
            }
            bytes.extend_from_slice(&chunk);
        }
        let value: Value = serde_json::from_slice(&bytes).map_err(|_| unavailable())?;
        if !status.is_success() {
            return Err(provider_failure(status, &value));
        }
        tracing::info!(
            input_tokens = value["usage"]["input_tokens"].as_u64().unwrap_or(0),
            output_tokens = value["usage"]["output_tokens"].as_u64().unwrap_or(0),
            "Assistant token usage"
        );
        normalize(&value)
    }
}
pub fn disabled() -> Failure {
    failure(
        StatusCode::SERVICE_UNAVAILABLE,
        "assistant_disabled",
        "Claude has not been configured. You can try Apple Private Cloud Compute.",
        true,
    )
}
fn provider_failure(status: StatusCode, value: &Value) -> Failure {
    // Anthropic currently reports an exhausted credit balance as invalid_request_error (400).
    let message = value["error"]["message"]
        .as_str()
        .unwrap_or("")
        .to_ascii_lowercase();
    if status == StatusCode::PAYMENT_REQUIRED
        || (status == StatusCode::BAD_REQUEST && message.contains("credit balance"))
    {
        failure(
            StatusCode::PAYMENT_REQUIRED,
            "credits_exhausted",
            "Claude credits are exhausted. You can continue with Apple Private Cloud Compute.",
            true,
        )
    } else if status == StatusCode::TOO_MANY_REQUESTS {
        failure(
            StatusCode::TOO_MANY_REQUESTS,
            "provider_limit",
            "Claude's usage limit has been reached. You can try Apple Private Cloud Compute.",
            true,
        )
    } else {
        unavailable()
    }
}
fn normalize(value: &Value) -> Result<Value, Failure> {
    if value["stop_reason"] == "refusal" {
        return Err(failure(
            StatusCode::UNPROCESSABLE_ENTITY,
            "provider_refusal",
            "Claude could not help with this request. Please rephrase it.",
            false,
        ));
    }
    if value["stop_reason"] != "end_turn" {
        return Err(unavailable());
    }
    let blocks = value["content"].as_array().ok_or_else(unavailable)?;
    // Claude may narrate a search in an earlier text block. Only the final text
    // block is the structured reply; tool results remain available for citations.
    let text = blocks
        .iter()
        .rev()
        .find(|b| b["type"] == "text")
        .and_then(|b| b["text"].as_str())
        .ok_or_else(unavailable)?;
    let text = text
        .trim()
        .strip_prefix("```json")
        .or_else(|| text.trim().strip_prefix("```"))
        .unwrap_or(text.trim())
        .trim()
        .trim_end_matches("```")
        .trim();
    let mut result: Value = serde_json::from_str(text).map_err(|_| unavailable())?;
    if result["reply"].as_str().is_none_or(|s| s.trim().is_empty())
        || !result["questions"].is_array()
        || !result["questions"]
            .as_array()
            .unwrap()
            .iter()
            .all(Value::is_string)
    {
        return Err(unavailable());
    }
    if !result["recipeDocument"].is_null()
        && (result["recipeDocument"]["format"] != "mealos.recipe"
            || result["recipeDocument"]["schemaVersion"] != 1
            || !result["recipeDocument"]["recipe"].is_object())
    {
        return Err(unavailable());
    }
    // Only expose citations actually returned by the search tool, not model-invented URLs.
    let mut sources = Vec::new();
    for block in blocks {
        let items = if block["type"] == "web_search_tool_result" {
            block["content"].as_array()
        } else {
            block["citations"].as_array()
        };
        for item in items.into_iter().flatten() {
            if let (Some(url), Some(title)) = (item["url"].as_str(), item["title"].as_str())
                && url.starts_with("https://")
                && !sources.iter().any(|s: &Value| s["url"] == url)
            {
                sources.push(json!({"title":title,"url":url}));
            }
        }
    }
    result["sources"] = json!(sources);
    result["provider"] = json!("claude");
    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{Router, extract::State, http::HeaderMap, routing::post};
    use std::sync::Arc;

    fn chat() -> Chat {
        Chat {
            model: "sonnet".into(),
            messages: vec![Message {
                role: "user".into(),
                content: "Make chicken rice".into(),
            }],
        }
    }
    fn response() -> Value {
        json!({"stop_reason":"end_turn","usage":{"input_tokens":100,"output_tokens":50},"content":[{"type":"text","text":json!({"reply":"Tell me your servings.","questions":["How many servings?"],"sources":[{"title":"Invented","url":"https://invalid.example"}],"recipeDocument":null}).to_string()}]})
    }
    #[test]
    fn validates_model_roles_and_size() {
        assert!(SYSTEM.starts_with("You are Forkware Recipe Assistant"));
        assert!(SYSTEM.contains("schemaVersion 1"));
        assert!(chat().validate().is_ok());
        let mut input = chat();
        input.model = "arbitrary-expensive-model".into();
        assert!(input.validate().is_err());
        let mut input = chat();
        input.messages[0].role = "system".into();
        assert!(input.validate().is_err());
        let mut input = chat();
        input.messages[0].content = "x".repeat(40_001);
        assert!(input.validate().is_err());
    }
    #[test]
    fn normalizes_only_complete_json_and_real_search_sources() {
        let mut value = response();
        value["content"]
            .as_array_mut()
            .unwrap()
            .insert(0, json!({"type":"text","text":"I will search first."}));
        assert_eq!(normalize(&value).unwrap()["sources"], json!([]));
        value["content"].as_array_mut().unwrap().push(json!({"type":"web_search_tool_result","content":[{"title":"Source","url":"https://example.com"},{"title":"Bad","url":"javascript:alert(1)"}]}));
        assert_eq!(
            normalize(&value).unwrap()["sources"]
                .as_array()
                .unwrap()
                .len(),
            1
        );
        value["stop_reason"] = json!("max_tokens");
        assert!(normalize(&value).is_err());
        value["stop_reason"] = json!("refusal");
        assert!(normalize(&value).is_err());
        value["stop_reason"] = json!("end_turn");
        value["content"][1]["text"] = json!("not JSON");
        assert!(normalize(&value).is_err());
    }
    #[test]
    fn credits_and_rate_errors_are_safe_fallbacks() {
        let exhausted = provider_failure(
            StatusCode::BAD_REQUEST,
            &json!({"error":{"message":"Your credit balance is too low to access the Anthropic API."}}),
        );
        assert_eq!(exhausted.code, "credits_exhausted");
        assert!(exhausted.fallback);
        assert_eq!(
            provider_failure(StatusCode::TOO_MANY_REQUESTS, &json!({})).code,
            "provider_limit"
        );
        let unauthorized = provider_failure(
            StatusCode::UNAUTHORIZED,
            &json!({"error":{"message":"secret upstream details"}}),
        );
        assert!(!unauthorized.message.contains("secret"));
    }
    #[tokio::test]
    async fn sends_server_key_and_bounded_search_without_retrying() {
        type Capture = Arc<Mutex<Vec<(HeaderMap, Value)>>>;
        async fn endpoint(
            State(capture): State<Capture>,
            headers: HeaderMap,
            Json(body): Json<Value>,
        ) -> (StatusCode, Json<Value>) {
            let success = body["messages"][0]["content"] == "success";
            capture.lock().await.push((headers, body));
            if success {
                return (StatusCode::OK, Json(response()));
            }
            (
                StatusCode::BAD_REQUEST,
                Json(json!({"error":{"message":"Your credit balance is too low"}})),
            )
        }
        let captured: Capture = Arc::new(Mutex::new(Vec::new()));
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let router = Router::new()
            .route("/messages", post(endpoint))
            .with_state(captured.clone());
        let task = tokio::spawn(async move { axum::serve(listener, router).await.unwrap() });
        let user = Uuid::new_v4();
        let mut assistant = Assistant::new("test-secret".into(), HashSet::from([user])).unwrap();
        assistant.endpoint = format!("http://{address}/messages");
        assert_eq!(
            assistant
                .chat(Uuid::new_v4(), chat())
                .await
                .unwrap_err()
                .status,
            StatusCode::FORBIDDEN
        );
        assert!(captured.lock().await.is_empty());
        assert_eq!(
            assistant.chat(user, chat()).await.unwrap_err().code,
            "credits_exhausted"
        );
        let mut success = chat();
        success.messages[0].content = "success".into();
        assert_eq!(
            assistant.chat(user, success).await.unwrap()["reply"],
            "Tell me your servings."
        );
        let captured = captured.lock().await;
        assert_eq!(captured.len(), 2);
        assert_eq!(captured[0].0["x-api-key"], "test-secret");
        assert_eq!(captured[0].1["model"], "claude-sonnet-5-5");
        assert_eq!(captured[0].1["max_tokens"], 8192);
        assert_eq!(captured[0].1["tools"][0]["max_uses"], 3);
        task.abort();
    }
}
