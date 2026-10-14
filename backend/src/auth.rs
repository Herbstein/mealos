use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use chrono::{DateTime, Utc};
use rand::{Rng, rand_core::UnwrapErr, rngs::SysRng};
use serde::Serialize;
use sha2::{Digest, Sha256};
use sqlx::{PgPool, Postgres, Transaction};
use uuid::Uuid;

use crate::Error;

fn bytes_to_hex(bytes: &[u8]) -> String {
    let mut s = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        s.push_str(&format!("{:02x}", byte));
    }
    s
}

pub fn digest(value: &str) -> String {
    bytes_to_hex(&Sha256::digest(value.as_bytes()))
}
fn token(prefix: &str) -> String {
    let mut bytes = [0u8; 32];
    // The system generator can fail. We automatically unwrap if it does
    // If checking the error is preferred use `try_fill_bytes`
    UnwrapErr(SysRng).fill_bytes(&mut bytes);
    format!("{prefix}{}", URL_SAFE_NO_PAD.encode(bytes))
}
fn valid_token(value: &str, prefix: &str) -> bool {
    value.strip_prefix(prefix).is_some_and(|v| {
        v.len() == 43
            && v.bytes()
                .all(|b| b.is_ascii_alphanumeric() || b == b'_' || b == b'-')
    })
}
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Challenge {
    pub challenge_id: Uuid,
    pub nonce: String,
    pub expires_at: DateTime<Utc>,
}
#[derive(Serialize)]
pub struct User {
    pub id: Uuid,
}
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Tokens {
    pub user: User,
    pub token_type: &'static str,
    pub access_token: String,
    pub refresh_token: String,
    pub access_expires_at: DateTime<Utc>,
    pub session_expires_at: DateTime<Utc>,
}
#[derive(sqlx::FromRow)]
pub struct Identity {
    pub id: Uuid,
    pub session_id: Uuid,
}

pub async fn challenge(pool: &PgPool) -> Result<Challenge, Error> {
    let nonce = token("");

    // TODO: Cleanup should really be done in a background thread instead.
    // 10k challenges in 4 minutes are only cleaned up if a challenge arrives 5 minutes after the first challenge
    // DB garbage collection currently runs while initializing a new challenge
    let challenge = sqlx::query!(
        r#"
WITH del_challenges AS (
    DELETE FROM auth_challenges WHERE expires_at < now()
),
del_sessions AS (
    DELETE FROM sessions WHERE expires_at < now()
)
INSERT INTO auth_challenges (nonce_hash, expires_at)
VALUES ($1, now() + interval '5 minutes')
RETURNING id, expires_at
"#,
        nonce
    )
    .fetch_one(pool)
    .await?;

    Ok(Challenge {
        challenge_id: challenge.id,
        expires_at: challenge.expires_at,
        nonce,
    })
}
pub async fn challenge_nonce(pool: &PgPool, id: Uuid) -> Result<String, Error> {
    sqlx::query_scalar("SELECT nonce_hash FROM auth_challenges WHERE id=$1 AND consumed_at IS NULL AND expires_at>now()")
        .bind(id).fetch_optional(pool).await?.ok_or_else(Error::unauthorized)
}
// Called only after cryptographic Apple verification; consume and create session
// in one transaction so concurrent retries cannot replay the same login.
pub async fn finish_sign_in(
    pool: &PgPool,
    challenge: Uuid,
    verified_subject: &str,
) -> Result<Tokens, Error> {
    let mut tx = pool.begin().await?;
    let consumed = sqlx::query("UPDATE auth_challenges SET consumed_at=now() WHERE id=$1 AND consumed_at IS NULL AND expires_at>now()")
        .bind(challenge).execute(&mut *tx).await?;
    if consumed.rows_affected() != 1 {
        return Err(Error::unauthorized());
    }
    let user_id = sqlx::query_scalar("INSERT INTO users(apple_subject) VALUES ($1) ON CONFLICT(apple_subject) DO UPDATE SET apple_subject=EXCLUDED.apple_subject RETURNING id")
        .bind(verified_subject).fetch_one(&mut *tx).await?;
    let result = write_tokens(&mut tx, user_id, None).await?;
    tx.commit().await?;
    Ok(result)
}
async fn write_tokens(
    tx: &mut Transaction<'_, Postgres>,
    user_id: Uuid,
    existing: Option<Uuid>,
) -> Result<Tokens, Error> {
    let access_token = token("fwat_");
    let refresh_token = token("fwrt_");
    let access_hash = digest(&access_token);
    let refresh_hash = digest(&refresh_token);
    let (access_expires_at, session_expires_at) = if let Some(id) = existing {
        sqlx::query_as::<_,(DateTime<Utc>,DateTime<Utc>)>("UPDATE sessions SET access_hash=$2,refresh_hash=$3,access_expires_at=LEAST(now()+interval '15 minutes',expires_at) WHERE id=$1 RETURNING access_expires_at,expires_at")
            .bind(id).bind(access_hash).bind(refresh_hash).fetch_one(&mut **tx).await?
    } else {
        sqlx::query_as::<_,(DateTime<Utc>,DateTime<Utc>)>("INSERT INTO sessions(user_id,access_hash,refresh_hash,access_expires_at,expires_at) VALUES ($1,$2,$3,now()+interval '15 minutes',now()+interval '30 days') RETURNING access_expires_at,expires_at")
            .bind(user_id).bind(access_hash).bind(refresh_hash).fetch_one(&mut **tx).await?
    };
    Ok(Tokens {
        user: User { id: user_id },
        token_type: "Bearer",
        access_token,
        refresh_token,
        access_expires_at,
        session_expires_at,
    })
}
pub async fn refresh(pool: &PgPool, refresh_token: &str) -> Result<Tokens, Error> {
    if !valid_token(refresh_token, "fwrt_") {
        return Err(Error::unauthorized());
    }
    let mut tx = pool.begin().await?;
    let (id,user_id) = sqlx::query_as::<_,(Uuid,Uuid)>("SELECT id,user_id FROM sessions WHERE refresh_hash=$1 AND revoked_at IS NULL AND expires_at>now() FOR UPDATE")
        .bind(digest(refresh_token)).fetch_optional(&mut *tx).await?.ok_or_else(Error::unauthorized)?;
    let result = write_tokens(&mut tx, user_id, Some(id)).await?;
    tx.commit().await?;
    Ok(result)
}
pub async fn authenticate(pool: &PgPool, access_token: &str) -> Result<Identity, Error> {
    if !valid_token(access_token, "fwat_") {
        return Err(Error::unauthorized());
    }
    sqlx::query_as("SELECT user_id AS id,id AS session_id FROM sessions WHERE access_hash=$1 AND revoked_at IS NULL AND access_expires_at>now() AND expires_at>now()")
        .bind(digest(access_token)).fetch_optional(pool).await?.ok_or_else(Error::unauthorized)
}
pub async fn logout(pool: &PgPool, identity: Identity) -> Result<(), Error> {
    sqlx::query("UPDATE sessions SET revoked_at=now() WHERE id=$1 AND user_id=$2")
        .bind(identity.session_id)
        .bind(identity.id)
        .execute(pool)
        .await?;
    Ok(())
}
