use crate::{Error, auth::digest};
use sqlx::{
    PgPool,
    postgres::{PgConnectOptions, PgPoolOptions},
};
use std::{str::FromStr, time::Duration};

pub fn pool(url: &str) -> Result<PgPool, sqlx::Error> {
    let options = PgConnectOptions::from_str(url)?.options([
        ("statement_timeout", "10000"),
        ("idle_in_transaction_session_timeout", "10000"),
    ]);
    Ok(PgPoolOptions::new()
        .max_connections(4)
        .min_connections(0)
        .idle_timeout(Duration::from_secs(10))
        .max_lifetime(None)
        .acquire_timeout(Duration::from_secs(5))
        .connect_lazy_with(options))
}

// Same ledger/checksum format as the initial Node prototype; never reapply an
// already-recorded migration or silently change its SQL.
pub async fn migrate(pool: &PgPool) -> Result<(), Error> {
    let mut tx = pool.begin().await?;
    sqlx::query("SELECT pg_advisory_xact_lock(72984101)")
        .execute(&mut *tx)
        .await?;
    sqlx::query("CREATE TABLE IF NOT EXISTS schema_migrations (name text PRIMARY KEY, checksum text NOT NULL, applied_at timestamptz NOT NULL DEFAULT now())").execute(&mut *tx).await?;
    for (name, sql) in [
        (
            "001_foundation.sql",
            include_str!("../migrations/001_foundation.sql"),
        ),
        (
            "002_library_sync.sql",
            include_str!("../migrations/002_library_sync.sql"),
        ),
    ] {
        let checksum = digest(sql);
        let existing: Option<String> =
            sqlx::query_scalar("SELECT checksum FROM schema_migrations WHERE name=$1")
                .bind(name)
                .fetch_optional(&mut *tx)
                .await?;
        if let Some(existing) = existing {
            if existing != checksum {
                return Err(Error(
                    axum::http::StatusCode::CONFLICT,
                    "An applied migration has changed.",
                ));
            }
            continue;
        }
        sqlx::raw_sql(sql).execute(&mut *tx).await?;
        sqlx::query("INSERT INTO schema_migrations(name,checksum) VALUES ($1,$2)")
            .bind(name)
            .bind(checksum)
            .execute(&mut *tx)
            .await?;
    }
    tx.commit().await?;
    Ok(())
}
