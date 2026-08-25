# MealOS recipe bridge

This is the stateless request gateway used by the native MealOS recipe assistant. It stores no chats, scans, or recipes.

Railway variables:

- `CLAUDE_CODE_OAUTH_TOKEN`: Claude Code subscription token created for the service.
- `MEALOS_BRIDGE_TOKEN`: a separate long random bearer token also supplied to the Android build.

Set the Railway service root directory to `/bridge`. The included Dockerfile installs the Claude Agent SDK and starts `POST /v1/recipe-chat`. `GET /health` is available for Railway health checks.

The agent can use `WebSearch`; shell, filesystem, subagent, and editing tools are blocked. The Android client sends the recent conversation on every request, so the server does not need a session store.

Requests may select `sonnet`, `opus`, or `claude-haiku-4-5`. The gateway rejects every other model value and defaults older clients to Sonnet.
