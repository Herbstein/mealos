# Railway backend and recipe data plan

Updated October 13, 2026. Target: Forkware for iPhone and iPad only.

Use Railway Serverless for the Rust API, with Postgres in the same Railway project. Build the new service in `backend/`; the legacy assistant bridge has been removed. Keep Postgres running initially and connect over Railway's private network. This replaces the earlier Supabase proposal and targets a small operating budget for the first 100 users.

The Rust API and Postgres foundation are deployed in the existing Railway `mealos` project. Later import/write/sync phases remain planned. Firebase remains active until migration is complete.

The first backend foundation is implemented in `backend/` using Rust, Axum, Tokio, and SQLx: transactional schema migrations, Apple identity verification and rotating sessions, protected recipe reads, and Railway infrastructure configuration. See [backend setup and endpoint contracts](../backend/README.md). The iOS 0.2 client includes native Apple sign-in and Keychain session handling. Signed-device verification and the later import/write/sync phases remain pending. The API base is `https://forkware.arjunakankipati.com/api`; the hostname root is reserved for the marketing site.

## Hosting and identity

- One Rust API will handle recipe access, imports, publishing, and synchronization. Claude assistant execution now uses this service with a workspace-scoped API key; supported Apple devices offer PCC fallback. The legacy bridge is removed.
- Enable Railway Serverless on the API and redeploy for the setting to take effect.
- One Postgres service holds shared recipes and access-controlled personal data. Only the backend connects to it; database credentials never ship in the app.
- Use a small object-storage bucket for photos and temporary attachments.
- Use native Sign in with Apple. The backend verifies the Apple identity and manages renewable, revocable sessions; the iOS app stores credentials in Keychain.
- Enforce ownership and publication access in backend queries and transactions, with tests for cross-user access.
- Keep local storage and an offline mutation queue on iOS. Bundled and downloaded recipes remain usable without a network connection.

No Supabase or CloudKit is required. Search and durable import-job storage use Postgres. A separate always-running worker, Redis, and realtime subscriptions are deferred.

## Design for serverless sleep

Railway detects inactivity from network traffic. Services generally sleep 5–10 minutes after their last outbound traffic. Active database connections, polling, and telemetry can prevent sleep. Traffic wakes the service, including traffic over the private network; the first request can be delayed or return a 502. See [Railway Serverless](https://docs.railway.com/deployments/serverless).

- Configure a small connection pool that releases idle connections and can reach zero connections.
- Avoid periodic database polling, keepalive queries, and frequent external uptime checks.
- Render local iOS data immediately and synchronize in the background when the app opens.
- Retry transient wake failures with bounded backoff. Give writes unique operation IDs so a retry cannot create duplicate records.
- Trigger import work when a request arrives and persist its state before processing. Bound individual processing attempts and external calls; do not rely on work existing only in memory.
- Use job leases and retry counts. Recover abandoned jobs on API startup and relevant requests, plus a brief scheduled recovery run for jobs left without subsequent user traffic. That run wakes the API only when recovery is necessary; its database checks and runtime still incur usage.
- Finish or safely release active jobs on shutdown. Stop processing when no jobs remain.

Keep Postgres awake for the first release. Database sleep is a later experiment requiring wake, reconnection, and durability tests; it is not assumed in the budget. Measure actual API sleep time before estimating savings from user count alone.

## Recipe data model

A recipe has a stable identity, immutable saved versions, editable drafts, and explicit publication state. The core catalog is a curated collection of published versions, so a contributed recipe can become part of it without changing storage formats.

| Entity | Purpose |
| --- | --- |
| User and session | Apple identity mapping, profile, renewable session credentials |
| Recipe | Stable ID, owner, source, parent recipe and exact parent version for a fork |
| Recipe version | Immutable validated content, creator, timestamp, change summary |
| Draft | Editable content, owner, base version, optimistic revision counter |
| Source | Original URL or book reference, author, import method, retrieval date |
| Library entry | User, saved recipe version, favorite, personal rating and notes |
| Publication | Specific version, public or unlisted visibility, review state |
| Collection entry | Published version included in a curated collection |
| Import job | Input reference, state, lease, retries, warnings, result and usage cost |
| Meal-plan entry | User, date, pinned recipe version and portions |
| Personal selection | User's pantry selections and grocery checkmarks |
| Media asset | Owner, storage key, source and permission metadata, associated version |

Use relational columns for identity, permissions, lineage, and searchable metadata. Store complete version content as validated JSON, preserving the existing Swift recipe structure where practical.

Ingredient lines retain original text alongside parsed quantity, unit, name, and preparation. Support ranges, missing quantities, and “to taste.” Stable ingredient-line IDs survive edits. Uncertain parsing must not silently invent amounts or combine incompatible grocery units.

## Import flow

1. Accept a URL, photo, pasted text, recipe file, or Forkware shared link.
2. For websites, extract structured recipe metadata first. For photos, use the existing on-device OCR. Validate recipe files directly. Shared Forkware links reference existing versions.
3. Use AI only when deterministic extraction is insufficient. Preserve source attribution and flag uncertainty rather than invent missing facts.
4. Present a reviewable draft. Saving creates a private recipe version and library entry, immediately usable for planning, groceries, and cooking.
5. Offer modification and sharing as separate actions.

Persist import states: queued, extracting, needs review, saved, or failed. Use idempotency keys to prevent duplicate imports on retry. Preserve failed input long enough for recovery, then expire temporary uploads according to a documented retention policy.

URL fetching must limit response sizes, timeouts, and redirects, and block internal network destinations. Treat fetched text as untrusted data. For inaccessible pages, offer pasted text or a scan. Video transcription and automated browser scraping are outside the initial scope.

## Modification and sharing

- Saving another person's recipe adds a reference to a specific version.
- Editing one's own recipe starts a draft and produces a new immutable version on save.
- Customizing another person's recipe creates a private fork linked to its exact source version.
- Publishing exposes only the selected version, never private drafts, notes, or import attachments.
- New published versions appear as available updates. Saved versions and existing meal plans do not change automatically.
- Public recipes are discoverable after review. Unlisted links are accessible to anyone holding the link and can be revoked; they are not confidential sharing.
- Publishing requires explicit source and permission information. Attribution alone does not automatically qualify an import for publication.
- Curated collection membership is an editorial action, independent of recipe ownership.

Provide a minimal shared-link web preview and an “Open in Forkware” action; a full web client is deferred. Define withdrawal and deletion behavior before launch, including how saved versions, attribution, and removed public content are handled.

## Local data and synchronization

Preserve the current JSON snapshot during migration. Introduce a local persistence abstraction and move to SQLite when per-record versions and the durable mutation queue are implemented.

Save local edits and their pending operations atomically. Upload with an operation ID and expected server revision, then fetch changes using a server-issued cursor. Retain deletion markers so offline devices cannot resurrect deleted records. Recipe conflicts preserve both edits for resolution. Small independent preferences can use simpler per-record conflict rules.

Keep each account's cache and queued writes separate. Allow local use before sign-in, with an explicit merge when attaching local data to an account. Timers, cooking progress, and assistant conversations remain local initially.

## Monthly budget

Planning estimates in USD, using prices checked October 13, 2026. Assume light traffic from about 100 registered users, a small database, compressed images, and an API that sleeps much of the day. Registered users are not equivalent to daily active users.

| Configuration | Estimated hosting per month |
| --- | ---: |
| API and Postgres always running | $6–12 |
| Serverless API sleeping most of the day, Postgres running | $5–8 |
| Initial hosting budget with headroom | $10–15 |

The awake-resource assumptions are approximately 150–250 MB RAM for the API, 250–500 MB for Postgres, and 0.02–0.05 average combined CPU cores in the always-running comparison. These are estimates to validate, not measurements of the existing agent runtime. Photos, volume storage, incremental backups, network traffic, and occasional recovery runs also contribute to usage.

Railway Hobby has a $5 workspace minimum including usage, not an additional $5 service charge. The Pro workspace minimum is $20, so the $5–8 bill estimate applies only on Hobby. Existing services share the workspace usage total. Storage continues to cost money while the API sleeps. See [Railway pricing](https://railway.com/pricing).

AI charges are separate and do not fall merely because serverless is enabled. Prefer structured imports and on-device OCR; set per-user import allowances, bounded retries, and an application-enforced AI spending budget. Do not assume the existing subscription-backed assistant credential supports unrestricted multi-user usage. Meter assistant and extraction costs separately.

Enable scheduled database-volume backups and test restoration. Railway bills backups for incremental storage; retain an independent database export for recovery outside the project. See [Railway backups](https://docs.railway.com/volumes/backups). Distribution fees, taxes, engineering, and human curation are outside the hosting estimate.

## Implementation order and acceptance

1. **Foundation:** database migrations, Apple authentication, sessions, recipe/version tables, private-network connection, access-control tests, and API Serverless configuration. Confirm the API actually sleeps after idle connections close.
2. **Import and personal library:** extraction, review, attribution, durable jobs, local writes, and sync. Verify retries do not duplicate records and interrupted jobs recover without continuous polling.
3. **Modification and sharing:** forks, published versions, links, review, and curated collections. Verify edits never mutate someone else's saved recipe or planned groceries.
4. **Migration and release:** back up Firebase and local data, import authenticated accounts as private libraries, preserve existing recipe IDs, and map bundled recipes to version 1. Link identities through authenticated migration rather than email matching. Keep Firebase until two-device verification succeeds.

Before release, test cold starts and transient 502s, offline edits, concurrent changes, account switching, deletion propagation, process interruption during imports, and database restore. Enable spending alerts and measure a representative week of resource usage, sleep time, image delivery, and AI cost before tightening the budget.
