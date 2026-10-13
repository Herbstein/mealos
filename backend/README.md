# Forkware backend

The Rust backend uses Axum, Tokio, and SQLx for Postgres, native Sign in with Apple, and recipe access. Assistant execution is deferred until the PCC/other cloud processing decision; no AI SDK or subscription credentials are loaded here. The legacy assistant remains in `../bridge/`. The current iOS client still uses Firebase; native Apple sign-in and Keychain session integration are wired in the iOS 0.2 client. The Railway deployment is configured below; signed-device validation remains pending.

## Local development

Use Rust 1.99 and Postgres 14+. Export `DATABASE_URL` and `APPLE_CLIENT_ID` (see `.env.example`); the binary does not load `.env` automatically.

```sh
cargo run --locked -- migrate
cargo run --locked -- serve
```

After migration, explicitly seed the bundled catalog against the intended database:

```sh
cargo run --locked -- seed ../ios/MealOS/Resources/recipes.json
```

Seeding preserves existing recipe IDs, creates version 1 and the core collection, and publishes only this operator-supplied catalog. It is transactional and repeatable, refuses conflicting content/ownership, and does not reverse withdrawals. It is not run automatically on deployment and is never used for personal imports.

Migrations run transactionally under a Postgres advisory lock and refuse changes to already-applied files. Add a new migration for subsequent schema changes. The baseline supports Postgres 14+ and includes identities, sessions, recipe versions, sources, forks, drafts, publications, libraries, collections, meal plans, pantry/grocery selections, imports, and media metadata. Immutable versions and composite foreign keys protect recipe/version relationships. Full recipe-content validation and write endpoints arrive with the import phase; JSON object checks alone do not validate a recipe.

The database is backend-only. Do not expose database credentials or grant mobile clients SQL access. The recipe read endpoint uses owner/publication predicates; no draft, source, library, or session tables are directly exposed.

## Public URL routing

The iOS client defaults to `https://forkware.arjunakankipati.com/api`. Reserve `/`
for the marketing site and route `/api/*` to this service without stripping the
prefix. All API routes also remain available without `/api` for direct Railway
access. The deployment health check remains `/ready`. The custom hostname currently routes directly to the Rust service. `/` returns
404, reserving it for the future marketing site. When that site is deployed, add
path-based routing in front of both services and keep the `/api` prefix intact.

## Apple authentication contract

Enable Sign in with Apple for the native bundle ID in the Apple Developer account before device testing. Configure `APPLE_CLIENT_ID` to that exact bundle ID. Apple keys are fetched over HTTPS and cached on demand; signature algorithm, issuer, audience, expiry, issue time, subject, and nonce are validated.

1. `POST /v1/auth/apple/challenge` with `{}` returns `challengeId`, a random `nonce`, and `expiresAt` (five minutes).
2. On iOS, set `ASAuthorizationAppleIDRequest.nonce` to the lowercase SHA-256 hex digest of that nonce. Start the native authorization sheet.
3. `POST /v1/auth/apple` with `{ "challengeId": "...", "identityToken": "..." }` exchanges the Apple identity token for a backend session. A challenge is consumed once, atomically. Email or client-supplied user IDs are never used to link accounts.
4. Store the returned `accessToken` and `refreshToken` in Keychain. Responses include `user.id`, `tokenType`, `accessExpiresAt`, and `sessionExpiresAt`. Access tokens last 15 minutes; sessions have a fixed 30-day maximum.
5. Send `Authorization: Bearer <accessToken>` to `GET /v1/me`, `GET /v1/recipes/<encodedRecipeId>`.
6. `POST /v1/auth/refresh` with `{ "refreshToken": "..." }` rotates both tokens without extending the absolute session lifetime. Serialize refreshes on the client and persist the replacement pair together. Reuse fails; if a refresh response is lost, a fresh Apple sign-in is required.
7. `POST /v1/auth/logout` with `{}` and the access bearer token revokes that session only. Tokens are stored only as SHA-256 hashes in Postgres.

`GET /v1/me` returns only the authenticated user's ID and `assistantAllowed: false`. `POST /v1/recipe-chat` returns 501 until assistant integration is implemented. Private or unavailable recipes return the same 404. An owner reads their latest saved version; other authenticated users read only an approved, active, public version. Unlisted links and saved-library version access are not implemented yet.

This foundation verifies native identity tokens, rather than exchanging Apple authorization codes for Apple refresh tokens. It does not yet process Apple server-to-server revocation notifications or implement account deletion/token revocation with Apple. Complete those lifecycle integrations before a public release. The native client must check credential state and clear/revoke its backend session when Apple authorization is revoked. Logout here is a Forkware session logout, not revocation of Apple consent.

Authentication POST bodies must be JSON objects. Auth endpoints accept at most 24 KB and share a 60-request/minute per-process limit. Use one API replica initially; distributed abuse limits are a later requirement before scaling replicas. No credentials or claim payloads are logged.

## Railway deployment

`.railway/railway.ts` defines a new backend environment with a Postgres service, its private `DATABASE_URL` reference, the API Docker build, pre-deploy migrations, readiness checks, and API Serverless enabled. It uses the current `railway/iac` SDK; the installed SDK requires Railway CLI 5.42.1 or newer.

For a new staging environment, run `npm ci` for the deployment-only Railway SDK, link the intended Railway project/environment, then run `railway config plan` from this directory. Review the project/resource names before applying. Connect the API's source repository with root `/backend`. This definition is for a new environment, not the existing production services listed below.

For the existing production project, run `railway config pull` into a separate review location first and merge the backend settings with its actual service names, variables, and other resources. Keep the existing assistant service separate, preserving its secrets, domain, and Firebase settings. Do not blindly apply this bootstrap definition over an existing environment. The named partial limits ownership to these resources, but existing projects must use a compatible partial setup.

Before routing traffic:

- Run migrations using the pre-deploy command; a failed migration stops deployment. `/ready` checks database connectivity and baseline migration presence. `/health` is a database-free liveness endpoint.
- Keep Postgres awake. Only the API uses Serverless. The pool has a maximum of four connections, a zero minimum, and a ten-second idle timeout. There are no database polling or maintenance timers.
- Enable scheduled volume backups and test a restore. Keep an independent export as described in the backend plan.
- Redeploy after enabling Serverless. Observe actual sleep/wake behavior, cold-start retries, and Railway resource costs; local pool-drain tests cannot verify Railway sleep.

The runtime container contains only the Rust binary and certificate roots. Railway infrastructure uses its TypeScript SDK as deployment tooling only. API requests time out after 30 seconds; shutdown drains for up to 35 seconds and closes the pool. Import processing and scheduled job recovery are not implemented in this foundation.

## Tests

```sh
cargo test --locked
python3 scripts/test-postgres.py
```

The second command requires `cargo`, `initdb`, `postgres`, and `pg_isready` on PATH. It starts an isolated temporary database bound to loopback, runs the complete suite, then stops and deletes only that temporary instance. It never uses `DATABASE_URL`. The integration test requires the `test-fixtures` feature and is ignored by default. Never enable that feature in a production build or supply a real database as `TEST_DATABASE_URL`.

Tests cover signed Apple tokens and invalid claims, nonce replay, concurrent refresh, expiry, session isolation, disabled assistant execution, public/private recipe access, immutable versions, foreign keys, repeated migrations and seeding, and pool draining.

The SQL baseline and migration checksum remain compatible with the initial Node prototype. Future schema changes need new migrations and a corresponding migration-runner entry.

## Production deployment

Railway project `mealos` (`3179ba45-5610-44e0-ae37-8ff2c41c6d05`), environment
`production`, now contains the separate `forkware-api` service and `Postgres`.
The legacy `mealos` assistant service is unchanged. Database credentials are
referenced privately as `${{Postgres.DATABASE_URL}}`; they are never copied into
the app. `APPLE_CLIENT_ID` is `com.arjunakankipati.forkware`.

The API has Serverless enabled, a `/ready` health check, and
`forkware-backend migrate` as its pre-deploy command. Postgres remains awake.
These settings are persisted on the Railway service; verify the live settings
after deployment. Railway now rejects deprecated `railway.json` configuration,
so no such file is retained. The initial upload did not apply those settings,
which was caught by readiness checks. After changing live service settings,
upload a fresh deployment; redeploying a prior deployment can reuse old settings.
The `.railway/railway.ts` definition remains a bootstrap for new environments.

### GitHub deployments

Production is connected to `kanki6315/mealos`, branch `main`, with root directory
`/backend`. Every push to `main` (including each merged pull request) triggers
Railway's native GitHub deployment. There is no path filter, so every merge
triggers a deployment. Railway builds `backend/Dockerfile`, runs
`forkware-backend migrate`, and checks `/ready` before promoting the deployment.
No Railway credential is stored in GitHub Actions.

`.github/workflows/backend.yml` runs formatting, Clippy, Apple authentication and
Postgres integration tests, and a production Docker build on pull requests and
`main`. Merge after this check passes. CI and Railway autodeploy are separate;
branch protection or Railway's Wait for CI setting is needed to enforce a gate.

Use pull requests for code changes. Direct `railway up` uploads are reserved for
an explicit operator-requested exception; they bypass the repository deployment
flow. The initial deployed snapshot remains active until the first successful
GitHub deployment. Infrastructure settings and credentials remain on Railway;
the bootstrap IaC file must not be applied over production without importing and
reconciling its existing resources first.

Public endpoint: `https://forkware.arjunakankipati.com/api`.
Direct Railway endpoint: `https://forkware-api-production.up.railway.app/api`.
Cloudflare CNAME `forkware` points to `fujcv393.up.railway.app`, with Railway's
`_railway-verify.forkware` TXT ownership record. Railway issues the HTTPS
certificate. Keep the existing `mealos` DNS record unchanged.

The catalog is not seeded automatically. Account deletion, Apple server-side
revocation notifications, backup/restore verification, and full recipe sync
remain required before a public release.

Verified October 13, 2026: custom-domain HTTPS and ownership validation, `/api/health`
and `/api/ready` return 200, Apple challenge returns 201, and unauthenticated
account access returns 401. The root returns 404. The live deployment manifest
confirms automatic migrations, readiness checks, and Serverless enabled. Actual
idle sleep/wake behavior and genuine Apple authorization still need device testing.
