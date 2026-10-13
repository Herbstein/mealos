# Recipe Assistant

Current iOS builds send assistant requests to the [Rust backend](../backend/README.md#claude-assistant), authenticated with Sign in with Apple backend sessions. Set `ANTHROPIC_API_KEY` and `ASSISTANT_ALLOWED_USER_IDS` on `forkware-api`; no model credentials are bundled in the app.

Claude uses the Messages API with bounded web search and returns a recipe draft for review. If credits run out or the provider is unavailable, the app offers Apple Private Cloud Compute on supported iOS 27 devices. PCC uses recent text plus the current draft, has no web search, and respects Apple's availability and daily quota. Scans continue to use local OCR; only extracted text is sent when the user chooses a provider.

Firebase library synchronization remains independent of assistant access. Sign in with Apple to use Claude; Google sign-in is only for the legacy library.

The old Node/Claude Agent SDK service in `bridge/` is deprecated. Its setup is preserved in [bridge/README.md](../bridge/README.md) solely for older clients. Do not move its subscription credential into the Rust service. Keep it deployed until old clients migrate, then separately retire the service and revoke the credential.
