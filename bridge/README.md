# MealOS recipe bridge

This is the stateless request gateway used by the native MealOS recipe assistant. It stores no chats, scans, or recipes.

Railway variables:

- `CLAUDE_CODE_OAUTH_TOKEN`: Claude Code subscription token created for the service.
- `FIREBASE_PROJECT_ID`: `mealos-af8fc`.
- `MEALOS_ALLOWED_FIREBASE_UIDS`: comma-separated Firebase Authentication UIDs authorized to use the assistant.

Set the Railway service root directory to `/bridge`. The included Dockerfile installs the Claude Agent SDK and starts `POST /v1/recipe-chat`. `GET /health` is available for Railway health checks.

The agent can use `WebSearch`; shell, filesystem, subagent, and editing tools are blocked. The Android client sends the recent conversation on every request, so the server does not need a session store.

Every chat request carries a short-lived Firebase ID token. The bridge verifies it with Firebase Admin and returns `403` for any valid account outside the configured allowlist. No Firebase service-account key or permanent APK bearer token is required. The older `MEALOS_ALLOWED_FIREBASE_UID` variable remains supported as a fallback during migration.

Requests may select `sonnet`, `opus`, or `claude-haiku-4-5`. The gateway rejects every other model value and defaults older clients to Sonnet.
