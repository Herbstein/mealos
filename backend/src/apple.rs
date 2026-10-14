use crate::Error;
use jsonwebtoken::{Algorithm, DecodingKey, Validation, decode, decode_header, jwk::JwkSet};
use serde::Deserialize;
use std::time::{Duration, Instant};
use tokio::sync::Mutex;

#[derive(Debug, Deserialize)]
struct Claims {
    sub: String,
    nonce: String,
    iat: u64,
    exp: u64,
}
struct Cache {
    keys: Option<JwkSet>,
    fetched: Option<Instant>,
}

pub struct AppleVerifier {
    audience: String,
    client: reqwest::Client,
    cache: Mutex<Cache>,
}
impl AppleVerifier {
    pub fn new(audience: String) -> Result<Self, reqwest::Error> {
        Ok(Self {
            audience,
            client: reqwest::Client::builder()
                .timeout(Duration::from_secs(5))
                .build()?,
            cache: Mutex::new(Cache {
                keys: None,
                fetched: None,
            }),
        })
    }
    #[cfg(feature = "test-fixtures")]
    pub fn with_test_keys(audience: String, keys: JwkSet) -> Self {
        Self {
            audience,
            client: reqwest::Client::new(),
            cache: Mutex::new(Cache {
                keys: Some(keys),
                fetched: Some(Instant::now()),
            }),
        }
    }
    pub async fn verify(&self, token: &str, nonce: &str) -> Result<String, Error> {
        let header = decode_header(token).map_err(|_| Error::unauthorized())?;
        if header.alg != Algorithm::RS256 {
            return Err(Error::unauthorized());
        }
        let kid = header.kid.ok_or_else(Error::unauthorized)?;
        let mut cache = self.cache.lock().await;
        let age = cache.fetched.map(|t| t.elapsed());
        let missing = cache
            .keys
            .as_ref()
            .and_then(|keys| keys.find(&kid))
            .is_none();
        // Refresh on expiry or an unknown rotation key, with a cooldown to stop
        // arbitrary kid values causing unbounded Apple requests. No timers.
        if age.is_none()
            || age.is_some_and(|a| a >= Duration::from_secs(3600))
            || (missing && age.is_some_and(|a| a >= Duration::from_secs(60)))
        {
            let response = self
                .client
                .get("https://appleid.apple.com/auth/keys")
                .send()
                .await
                .map_err(|_| Error::unavailable())?
                .error_for_status()
                .map_err(|_| Error::unavailable())?;
            cache.keys = Some(response.json().await.map_err(|_| Error::unavailable())?);
            cache.fetched = Some(Instant::now());
        }
        verify_with_keys(
            token,
            nonce,
            &self.audience,
            cache.keys.as_ref().ok_or_else(Error::unavailable)?,
        )
    }
}

pub fn verify_with_keys(
    token: &str,
    nonce: &str,
    audience: &str,
    keys: &JwkSet,
) -> Result<String, Error> {
    let header = decode_header(token).map_err(|_| Error::unauthorized())?;
    if header.alg != Algorithm::RS256 {
        return Err(Error::unauthorized());
    }
    let key = keys
        .find(header.kid.as_deref().ok_or_else(Error::unauthorized)?)
        .ok_or_else(Error::unauthorized)?;
    let mut validation = Validation::new(Algorithm::RS256);
    validation.set_issuer(&["https://appleid.apple.com"]);
    validation.set_audience(&[audience]);
    validation.set_required_spec_claims(&["exp", "iat", "sub", "iss", "aud", "nonce"]);
    validation.leeway = 5;
    let key = DecodingKey::from_jwk(key).map_err(|_| Error::unauthorized())?;
    let claims = decode::<Claims>(token, &key, &validation)
        .map_err(|_| Error::unauthorized())?
        .claims;
    let now = chrono::Utc::now().timestamp().max(0) as u64;
    if claims.nonce != nonce
        || claims.sub.is_empty()
        || claims.sub.len() > 255
        || claims.iat > now + 5
        || now > claims.iat.saturating_add(605)
        || claims.exp <= claims.iat
    {
        return Err(Error::unauthorized());
    }
    Ok(claims.sub)
}
