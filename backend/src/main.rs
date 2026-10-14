use forkware_backend::{apple::AppleVerifier, assistant::Assistant, db, http, recipes};
use std::{env, time::Duration};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()),
        )
        .init();
    let pool = db::pool(&env::var("DATABASE_URL")?)?;
    let command = env::args().nth(1).unwrap_or_else(|| "serve".into());
    match command.as_str() {
        "migrate" => db::migrate(&pool).await?,
        "seed" => {
            let file = env::args()
                .nth(2)
                .ok_or("Pass the bundled recipes.json path explicitly.")?;
            let data = std::fs::read(file)?;
            let input: Vec<serde_json::Value> = serde_json::from_slice(&data)?;
            let count = recipes::seed(&pool, &input).await?;
            tracing::info!(count, "Core catalog seeded");
        }
        "serve" => {
            let audience = env::var("APPLE_CLIENT_ID")?;
            if audience.trim().is_empty() {
                return Err("APPLE_CLIENT_ID is required".into());
            }
            let state = http::AppState::with_assistant(
                pool.clone(),
                AppleVerifier::new(audience)?,
                Assistant::from_env()?,
            );
            let port: u16 = env::var("PORT").unwrap_or_else(|_| "3000".into()).parse()?;
            let listener =
                tokio::net::TcpListener::bind((std::net::Ipv4Addr::UNSPECIFIED, port)).await?;
            tracing::info!(port, "Forkware backend listening");
            axum::serve(listener, http::router(state))
                .with_graceful_shutdown(shutdown())
                .await?;
        }
        _ => return Err("Use serve, migrate, or seed <path>.".into()),
    }
    pool.close().await;
    Ok(())
}
async fn shutdown() {
    #[cfg(unix)]
    {
        let mut term = tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
            .expect("signal handler");
        tokio::select! {_ = term.recv()=>{},_ = tokio::signal::ctrl_c()=>{}}
    }
    #[cfg(not(unix))]
    {
        tokio::signal::ctrl_c().await.expect("signal handler");
    }
    tokio::spawn(async {
        tokio::time::sleep(Duration::from_secs(35)).await;
        std::process::exit(1);
    });
}
