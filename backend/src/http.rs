use crate::{Error, apple::AppleVerifier, assistant, auth, library_sync, recipes};
use axum::{
    Json, Router,
    extract::{DefaultBodyLimit, Path, Request, State, rejection::JsonRejection},
    http::{HeaderMap, HeaderValue, StatusCode},
    middleware::{self, Next},
    response::{IntoResponse, Response},
    routing::{get, post},
};
use serde::Deserialize;
use serde_json::{Value, json};
use sqlx::PgPool;
use std::{
    collections::VecDeque,
    sync::Arc,
    time::{Duration, Instant},
};
use tokio::sync::Mutex;
use uuid::Uuid;

pub struct AppState {
    pub pool: PgPool,
    pub apple: AppleVerifier,
    pub assistant: Option<assistant::Assistant>,
    auth_requests: Mutex<VecDeque<Instant>>,
}
impl AppState {
    pub fn new(pool: PgPool, apple: AppleVerifier) -> Arc<Self> {
        Self::with_assistant(pool, apple, None)
    }
    pub fn with_assistant(
        pool: PgPool,
        apple: AppleVerifier,
        assistant: Option<assistant::Assistant>,
    ) -> Arc<Self> {
        Arc::new(Self {
            assistant,
            pool,
            apple,
            auth_requests: Mutex::new(VecDeque::new()),
        })
    }
}
pub fn router(state: Arc<AppState>) -> Router {
    let auth_routes = Router::new()
        .route("/v1/auth/apple/challenge", post(challenge))
        .route("/v1/auth/apple", post(sign_in))
        .route("/v1/auth/refresh", post(refresh))
        .route("/v1/auth/logout", post(logout))
        .route_layer(middleware::from_fn_with_state(state.clone(), auth_limit));
    let api = Router::new()
        .merge(auth_routes)
        .route("/health", get(|| async { Json(json!({"ok":true})) }))
        .route("/ready", get(ready))
        .route("/v1/me", get(me))
        .route("/v1/recipes/{id}", get(recipe))
        .merge(
            Router::new()
                .route("/v1/library", get(library_read).post(library_write))
                .layer(DefaultBodyLimit::max(2_000_000)),
        )
        .merge(
            Router::new()
                .route("/v1/recipe-chat", post(chat))
                .layer(DefaultBodyLimit::max(256_000))
                .layer(middleware::from_fn(assistant_timeout)),
        )
        .fallback(|| async { Error(StatusCode::NOT_FOUND, "Not found.") })
        .layer(DefaultBodyLimit::max(24_000))
        .layer(middleware::from_fn(headers_and_timeout))
        .with_state(state);
    Router::new().nest("/api", api.clone()).merge(api)
}
async fn headers_and_timeout(request: Request, next: Next) -> Response {
    let mut response = match tokio::time::timeout(
        Duration::from_secs(if request.uri().path().ends_with("/v1/recipe-chat") {
            115
        } else {
            30
        }),
        next.run(request),
    )
    .await
    {
        Ok(response) => response,
        Err(_) => Error(StatusCode::REQUEST_TIMEOUT, "Request timed out.").into_response(),
    };
    response
        .headers_mut()
        .insert("cache-control", HeaderValue::from_static("no-store"));
    response.headers_mut().insert(
        "x-content-type-options",
        HeaderValue::from_static("nosniff"),
    );
    response
}
async fn auth_limit(State(state): State<Arc<AppState>>, request: Request, next: Next) -> Response {
    let now = Instant::now();
    let mut times = state.auth_requests.lock().await;
    while times
        .front()
        .is_some_and(|t| now.duration_since(*t) >= Duration::from_secs(60))
    {
        times.pop_front();
    }
    if times.len() >= 60 {
        let mut response = Error(
            StatusCode::TOO_MANY_REQUESTS,
            "Too many authentication requests.",
        )
        .into_response();
        response
            .headers_mut()
            .insert("retry-after", HeaderValue::from_static("60"));
        return response;
    }
    times.push_back(now);
    drop(times);
    next.run(request).await
}
fn body<T>(value: Result<Json<T>, JsonRejection>) -> Result<T, Error> {
    value.map(|Json(v)| v).map_err(|e| {
        if e.status() == StatusCode::PAYLOAD_TOO_LARGE {
            Error(StatusCode::PAYLOAD_TOO_LARGE, "Request is too large.")
        } else {
            Error::bad_request()
        }
    })
}
fn object(value: Result<Json<Value>, JsonRejection>) -> Result<(), Error> {
    if body(value)?.is_object() {
        Ok(())
    } else {
        Err(Error::bad_request())
    }
}
fn bearer(headers: &HeaderMap) -> Result<&str, Error> {
    headers
        .get("authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .ok_or_else(Error::unauthorized)
}
async fn ready(State(state): State<Arc<AppState>>) -> Result<Json<Value>, Error> {
    let present: bool = sqlx::query_scalar(
        "SELECT count(*)=2 FROM schema_migrations WHERE name IN ('001_foundation.sql','002_library_sync.sql')",
    )
    .fetch_one(&state.pool)
    .await?;
    if !present {
        return Err(Error::unavailable());
    }
    Ok(Json(json!({"ok":true})))
}
async fn challenge(
    State(state): State<Arc<AppState>>,
    input: Result<Json<Value>, JsonRejection>,
) -> Result<(StatusCode, Json<auth::Challenge>), Error> {
    object(input)?;
    Ok((
        StatusCode::CREATED,
        Json(auth::challenge(&state.pool).await?),
    ))
}
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SignIn {
    challenge_id: Uuid,
    identity_token: String,
}
async fn sign_in(
    State(state): State<Arc<AppState>>,
    input: Result<Json<SignIn>, JsonRejection>,
) -> Result<Json<auth::Tokens>, Error> {
    let input = body(input)?;
    if input.identity_token.is_empty() || input.identity_token.len() > 16_384 {
        return Err(Error::bad_request());
    }
    let nonce = auth::challenge_nonce(&state.pool, input.challenge_id).await?;
    let subject = state.apple.verify(&input.identity_token, &nonce).await?;
    Ok(Json(
        auth::finish_sign_in(&state.pool, input.challenge_id, &subject).await?,
    ))
}
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct Refresh {
    refresh_token: String,
}
async fn refresh(
    State(state): State<Arc<AppState>>,
    input: Result<Json<Refresh>, JsonRejection>,
) -> Result<Json<auth::Tokens>, Error> {
    Ok(Json(
        auth::refresh(&state.pool, &body(input)?.refresh_token).await?,
    ))
}
async fn logout(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    input: Result<Json<Value>, JsonRejection>,
) -> Result<Json<Value>, Error> {
    object(input)?;
    let identity = auth::authenticate(&state.pool, bearer(&headers)?).await?;
    auth::logout(&state.pool, identity).await?;
    Ok(Json(json!({"ok":true})))
}
async fn me(State(state): State<Arc<AppState>>, headers: HeaderMap) -> Result<Json<Value>, Error> {
    let identity = auth::authenticate(&state.pool, bearer(&headers)?).await?;
    Ok(Json(
        json!({"user":{"id":identity.id},"assistantAllowed":state.assistant.as_ref().is_some_and(|a| a.allows(identity.id))}),
    ))
}
async fn recipe(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<recipes::Recipe>, Error> {
    let identity = auth::authenticate(&state.pool, bearer(&headers)?).await?;
    if id.len() > 200 {
        return Err(Error::bad_request());
    }
    Ok(Json(
        recipes::read(&state.pool, &id, identity.id)
            .await?
            .ok_or(Error(StatusCode::NOT_FOUND, "Recipe not found."))?,
    ))
}

async fn assistant_timeout(request: Request, next: Next) -> Response {
    match tokio::time::timeout(Duration::from_secs(110), next.run(request)).await {
        Ok(response) => response,
        Err(_) => assistant::Failure {
            status: StatusCode::GATEWAY_TIMEOUT,
            code: "provider_timeout",
            message: "Claude timed out. You can try Apple Private Cloud Compute.",
            fallback: true,
        }
        .into_response(),
    }
}
async fn chat(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    input: Result<Json<assistant::Chat>, JsonRejection>,
) -> Response {
    let identity = match bearer(&headers) {
        Ok(token) => match auth::authenticate(&state.pool, token).await {
            Ok(identity) => identity,
            Err(e) => return e.into_response(),
        },
        Err(e) => return e.into_response(),
    };
    let Some(assistant) = &state.assistant else {
        return assistant::disabled().into_response();
    };
    let input = match body(input) {
        Ok(input) => input,
        Err(e) => return e.into_response(),
    };
    match assistant.chat(identity.id, input).await {
        Ok(value) => Json(value).into_response(),
        Err(e) => e.into_response(),
    }
}

async fn library_read(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
) -> Result<Json<library_sync::Snapshot>, Error> {
    let identity = auth::authenticate(&state.pool, bearer(&headers)?).await?;
    Ok(Json(library_sync::read(&state.pool, identity.id).await?))
}
async fn library_write(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    input: Result<Json<library_sync::Write>, JsonRejection>,
) -> Result<Json<Value>, Error> {
    let identity = auth::authenticate(&state.pool, bearer(&headers)?).await?;
    let Json(input) = input.map_err(|_| Error::bad_request())?;
    Ok(Json(
        library_sync::write(&state.pool, identity.id, input).await?,
    ))
}
