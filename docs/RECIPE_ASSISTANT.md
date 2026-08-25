# Recipe Assistant setup

MealOS 0.5 adds a native recipe conversation and photo import flow. Conversation history, extracted scan text, sources, the selected model, and the current draft are stored on the Android device. The Railway service is a stateless request gateway.

## Railway service

Create a Railway service from this repository and set its root directory to `bridge`. Railway will use `bridge/Dockerfile`.

Set these Railway variables:

- `CLAUDE_CODE_OAUTH_TOKEN`: the Claude Code credential used by the Agent SDK.
- `MEALOS_BRIDGE_TOKEN`: a separate, long random value used to authorize the private APK.

Set the health check path to `/health`. After deployment, verify that it returns `{ "ok": true }`. Do not expose Railway logs containing request bodies or either token.

The agent receives only the `WebSearch` tool. Shell, filesystem, editing, subagent, and fetch tools are explicitly blocked. Search is used when current food-safety, appliance, or timing facts need verification.

The Assistant header offers Sonnet (default), Opus, and Haiku. The choice applies to the next reply and survives a new chat or app restart. The gateway validates its fixed allowlist before invoking the Agent SDK; arbitrary model identifiers from a modified client are rejected.

## Android release

Add two private GitHub Actions secrets before tagging 0.5.0:

- `MEALOS_BRIDGE_URL`: the public Railway service URL, without `/v1/recipe-chat`.
- `MEALOS_BRIDGE_TOKEN`: the same random gateway value configured in Railway.

The values are compiled into the private APK. This is suitable for the current single-owner/private-distribution model, but an APK bearer token is recoverable by a determined party. If the APK or token leaks, rotate `MEALOS_BRIDGE_TOKEN` in Railway and publish a new APK. A future multi-user release should replace this with user authentication and short-lived tokens.

`CLAUDE_CODE_OAUTH_TOKEN` must never be added to GitHub Actions or the APK. It belongs only in Railway.

## Scan privacy and first use

The document scanner UI is supplied by Google Play services and can capture pages or import gallery images. Its scanner module may download the first time it is opened. Latin-script OCR is bundled with MealOS and runs on-device. MealOS sends extracted text—not the original page images—to the Railway request gateway.

Every generated recipe opens in the local editor as an unsaved draft. Review ingredients, temperatures, timers, equipment, and leftover instructions before saving.
