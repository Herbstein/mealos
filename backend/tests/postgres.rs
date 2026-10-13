#![cfg(feature = "test-fixtures")]
mod support;
use axum::{
    body::Body,
    http::{Request, StatusCode},
};
use forkware_backend::{
    apple::AppleVerifier,
    auth, db,
    http::{self, AppState},
    recipes,
};
use http_body_util::BodyExt;
use serde_json::{Value, json};
use sqlx::PgPool;
use support::{AUDIENCE, Signing};
use tower::ServiceExt;
use uuid::Uuid;

async fn request(
    app: &axum::Router,
    method: &str,
    path: &str,
    body: Value,
    token: Option<&str>,
) -> (StatusCode, Value) {
    let mut req = Request::builder()
        .method(method)
        .uri(path)
        .header("content-type", "application/json");
    if let Some(token) = token {
        req = req.header("authorization", format!("Bearer {token}"));
    }
    let response = app
        .clone()
        .oneshot(req.body(Body::from(body.to_string())).unwrap())
        .await
        .unwrap();
    let status = response.status();
    let bytes = response.into_body().collect().await.unwrap().to_bytes();
    (status, serde_json::from_slice(&bytes).unwrap())
}
async fn credentials(app: &axum::Router, signer: &Signing, user: &str) -> Value {
    let (status, challenge) =
        request(app, "POST", "/api/v1/auth/apple/challenge", json!({}), None).await;
    assert_eq!(status, 201);
    json!({"challengeId":challenge["challengeId"],"identityToken":signer.sign(&signer.claims(user,&auth::digest(challenge["nonce"].as_str().unwrap())))})
}
async fn login(app: &axum::Router, signer: &Signing, user: &str) -> Value {
    let input = credentials(app, signer, user).await;
    let (status, result) = request(app, "POST", "/api/v1/auth/apple", input, None).await;
    assert_eq!(status, 200, "{result}");
    result
}
fn access(session: &Value) -> Option<&str> {
    session["accessToken"].as_str()
}
async fn execute(pool: &PgPool, sql: &str) {
    sqlx::query(sql).execute(pool).await.unwrap();
}

// Explicitly ignored without the disposable database harness, rather than
// accidentally connecting to the developer's DATABASE_URL.
#[tokio::test]
#[ignore = "run scripts/test-postgres.py"]
async fn database_http_and_serverless_lifecycle() {
    let url = std::env::var("TEST_DATABASE_URL").expect("Disposable TEST_DATABASE_URL required");
    let pool = db::pool(&url).unwrap();
    db::migrate(&pool).await.unwrap();
    db::migrate(&pool).await.unwrap();
    let count: i64 = sqlx::query_scalar("SELECT count(*) FROM schema_migrations")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(count, 1);
    let signer = Signing::new();
    let app = http::router(AppState::new(
        pool.clone(),
        AppleVerifier::with_test_keys(AUDIENCE.into(), signer.keys.clone()),
    ));
    assert_eq!(
        request(&app, "GET", "/health", Value::Null, None).await.0,
        200
    );
    assert_eq!(
        request(&app, "GET", "/ready", Value::Null, None).await.0,
        200
    );
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, None)
            .await
            .0,
        401
    );
    assert_eq!(
        request(&app, "POST", "/api/v1/auth/apple", Value::Null, None)
            .await
            .0,
        400
    );
    assert_eq!(
        request(
            &app,
            "POST",
            "/api/v1/auth/apple",
            json!({"identityToken":"x".repeat(25_000)}),
            None
        )
        .await
        .0,
        413
    );
    let owner = login(&app, &signer, "owner").await;
    let guest = login(&app, &signer, "guest").await;
    let owner_id: Uuid = owner["user"]["id"].as_str().unwrap().parse().unwrap();
    assert_ne!(owner["user"]["id"], guest["user"]["id"]);
    let stored: String = sqlx::query_scalar("SELECT access_hash FROM sessions WHERE user_id=$1")
        .bind(owner_id)
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(stored, auth::digest(access(&owner).unwrap()));
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&owner))
            .await
            .1["assistantAllowed"],
        false
    );
    assert_eq!(
        request(
            &app,
            "POST",
            "/api/v1/recipe-chat",
            json!({}),
            access(&owner)
        )
        .await
        .0,
        501
    );

    let input = credentials(&app, &signer, "owner").await;
    let (a, b) = tokio::join!(
        request(&app, "POST", "/api/v1/auth/apple", input.clone(), None),
        request(&app, "POST", "/api/v1/auth/apple", input, None)
    );
    let mut statuses = [a.0.as_u16(), b.0.as_u16()];
    statuses.sort();
    assert_eq!(statuses, [200, 401]);
    let input = credentials(&app, &signer, "guest").await;
    sqlx::query("UPDATE auth_challenges SET expires_at=now()-interval '1 minute' WHERE id=$1")
        .bind(
            input["challengeId"]
                .as_str()
                .unwrap()
                .parse::<Uuid>()
                .unwrap(),
        )
        .execute(&pool)
        .await
        .unwrap();
    assert_eq!(
        request(&app, "POST", "/api/v1/auth/apple", input, None)
            .await
            .0,
        401
    );
    let mut mismatch = credentials(&app, &signer, "guest").await;
    mismatch["identityToken"] = credentials(&app, &signer, "guest").await["identityToken"].clone();
    assert_eq!(
        request(&app, "POST", "/api/v1/auth/apple", mismatch, None)
            .await
            .0,
        401
    );

    let old = login(&app, &signer, "refresh").await;
    let refresh = json!({"refreshToken":old["refreshToken"]});
    let (a, b) = tokio::join!(
        request(&app, "POST", "/api/v1/auth/refresh", refresh.clone(), None),
        request(&app, "POST", "/api/v1/auth/refresh", refresh, None)
    );
    let mut statuses = [a.0.as_u16(), b.0.as_u16()];
    statuses.sort();
    assert_eq!(statuses, [200, 401]);
    let new = if a.0 == 200 { a.1 } else { b.1 };
    assert_eq!(new["sessionExpiresAt"], old["sessionExpiresAt"]);
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&old))
            .await
            .0,
        401
    );
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&new))
            .await
            .0,
        200
    );
    assert_eq!(
        request(
            &app,
            "POST",
            "/api/v1/auth/logout",
            json!({"userId":guest["user"]["id"]}),
            access(&new)
        )
        .await
        .0,
        200
    );
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&new))
            .await
            .0,
        401
    );
    assert_eq!(
        request(
            &app,
            "POST",
            "/api/v1/auth/refresh",
            json!({"refreshToken":new["refreshToken"]}),
            None
        )
        .await
        .0,
        401
    );
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&guest))
            .await
            .0,
        200
    );
    let expiry = login(&app, &signer, "expiry").await;
    sqlx::query("UPDATE sessions SET access_expires_at=now()-interval '1 minute',expires_at=now()-interval '30 seconds' WHERE access_hash=$1").bind(auth::digest(access(&expiry).unwrap())).execute(&pool).await.unwrap();
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&expiry))
            .await
            .0,
        401
    );
    assert_eq!(
        request(
            &app,
            "POST",
            "/api/v1/auth/refresh",
            json!({"refreshToken":expiry["refreshToken"]}),
            None
        )
        .await
        .0,
        401
    );

    sqlx::query("INSERT INTO recipes(id,owner_id) VALUES ('private',$1)")
        .bind(owner_id)
        .execute(&pool)
        .await
        .unwrap();
    let v1:Uuid=sqlx::query_scalar("INSERT INTO recipe_versions(recipe_id,version,title,content) VALUES ('private',1,'Dinner','{}') RETURNING id").fetch_one(&pool).await.unwrap();
    execute(&pool,"INSERT INTO recipe_versions(recipe_id,version,title,content) VALUES ('private',2,'Private revision','{}')").await;
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&guest)
        )
        .await
        .0,
        404
    );
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&owner)
        )
        .await
        .1["version"],
        2
    );
    sqlx::query(
        "INSERT INTO publications(recipe_id,version_id,visibility) VALUES ('private',$1,'public')",
    )
    .bind(v1)
    .execute(&pool)
    .await
    .unwrap();
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&guest)
        )
        .await
        .0,
        404
    );
    execute(
        &pool,
        "UPDATE publications SET review_state='approved' WHERE recipe_id='private'",
    )
    .await;
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&guest)
        )
        .await
        .1["version"],
        1
    );
    execute(&pool,"UPDATE publications SET visibility='unlisted',share_token_hash='test' WHERE recipe_id='private'").await;
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&guest)
        )
        .await
        .0,
        404
    );
    execute(
        &pool,
        "UPDATE publications SET visibility='public',withdrawn_at=now() WHERE recipe_id='private'",
    )
    .await;
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&guest)
        )
        .await
        .0,
        404
    );
    assert!(
        sqlx::query("UPDATE recipe_versions SET title='Bad' WHERE id=$1")
            .bind(v1)
            .execute(&pool)
            .await
            .is_err()
    );
    execute(&pool, "INSERT INTO recipes(id) VALUES ('other')").await;
    assert!(
        sqlx::query(
            "INSERT INTO library_entries(user_id,recipe_id,version_id) VALUES ($1,'other',$2)"
        )
        .bind(owner_id)
        .bind(v1)
        .execute(&pool)
        .await
        .is_err()
    );
    execute(
        &pool,
        "UPDATE recipes SET deleted_at=now() WHERE id='private'",
    )
    .await;
    assert_eq!(
        request(
            &app,
            "GET",
            "/api/v1/recipes/private",
            Value::Null,
            access(&owner)
        )
        .await
        .0,
        404
    );

    let catalog: Vec<Value> =
        serde_json::from_str(include_str!("../../ios/MealOS/Resources/recipes.json")).unwrap();
    recipes::seed(&pool, &catalog).await.unwrap();
    recipes::seed(&pool, &catalog).await.unwrap();
    let count: i64 = sqlx::query_scalar("SELECT count(*) FROM collection_entries")
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(count as usize, catalog.len());
    let id = catalog[0]["id"].as_str().unwrap();
    let path = format!("/api/v1/recipes/{id}");
    assert_eq!(
        request(&app, "GET", &path, Value::Null, access(&guest))
            .await
            .0,
        200
    );
    let mut changed = catalog.clone();
    changed[0]["name"] = json!("Changed");
    assert!(recipes::seed(&pool, &changed).await.is_err());
    sqlx::query("UPDATE publications SET withdrawn_at=now() WHERE recipe_id=$1")
        .bind(id)
        .execute(&pool)
        .await
        .unwrap();
    recipes::seed(&pool, &catalog).await.unwrap();
    assert_eq!(
        request(&app, "GET", &path, Value::Null, access(&guest))
            .await
            .0,
        404
    );
    // SQLx's idle reaper runs on an interval; allow two idle windows.
    for _ in 0..30 {
        if pool.size() == 0 {
            break;
        }
        tokio::time::sleep(std::time::Duration::from_secs(1)).await;
    }
    assert_eq!(pool.size(), 0, "Idle pool must drain for serverless");
    assert_eq!(
        request(&app, "GET", "/api/v1/me", Value::Null, access(&owner))
            .await
            .0,
        200
    );
    pool.close().await;
}
