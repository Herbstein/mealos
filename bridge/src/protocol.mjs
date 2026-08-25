export const MAX_REQUEST_BYTES = 256_000;
export const MAX_MESSAGES = 30;
export const ALLOWED_MODELS = new Set(["sonnet", "opus", "claude-haiku-4-5"]);

export function validateRequest(value) {
  if (!value || typeof value !== "object") return "Request body must be a JSON object.";
  if (value.model !== undefined && !ALLOWED_MODELS.has(value.model)) return "Choose a supported recipe assistant model.";
  if (!Array.isArray(value.messages) || value.messages.length === 0) return "At least one message is required.";
  if (value.messages.length > MAX_MESSAGES) return `Keep the conversation to ${MAX_MESSAGES} messages or fewer.`;
  for (const message of value.messages) {
    if (!message || !["user", "assistant"].includes(message.role)) return "Each message needs a valid role.";
    if (typeof message.content !== "string" || !message.content.trim() || message.content.length > 40_000) {
      return "Each message needs content no longer than 40,000 characters.";
    }
  }
  return null;
}

export function requestModel(value) {
  return value?.model || "sonnet";
}

export function buildPrompt(messages) {
  const transcript = messages.map(({ role, content }) => `${role.toUpperCase()}:\n${content.trim()}`).join("\n\n");
  return `You are MealOS Recipe Assistant. Help one home cook create or import familiar, low-effort recipes.

Preferences and equipment:
- Avoid seafood and mushrooms. Prefer chicken or beef to pork.
- Favor minimal cleanup and familiar ingredients.
- Available: Ninja CRISPi DualZone glass air fryer, rice cooker, Ooni Volt, toaster, microwave, pots and pans.
- When current safety, timing, or appliance facts matter, use WebSearch and cite concise source titles and URLs.
- Never claim a recipe is verified merely because it sounds plausible. Surface ambiguities as questions.

Return one JSON object only with this shape:
{
  "reply": "short conversational response",
  "questions": ["unresolved question"],
  "sources": [{"title": "source title", "url": "https://..."}],
  "recipeDocument": null OR a complete MealOS recipe document
}

A complete MealOS recipe document has format "mealos.recipe", schemaVersion 1, and recipe fields: name, shortName, description, effortMinutes, totalMinutes, baseServings, equipment[], tags[], leftoverNote, ingredients[] and steps[]. Ingredient fields: id, name, amount (number above zero), unit, category (PRODUCE, MEAT, DAIRY, BAKERY, PANTRY, or FROZEN), pantryStaple (boolean). Step fields: title, instruction, equipment, optional timerSeconds, optional tip. Do not include a recipeDocument until there is enough information for a useful reviewable draft. The user will review before saving.

Conversation:
${transcript}`;
}

export function normalizeAgentResult(raw) {
  const text = typeof raw === "string" ? raw : JSON.stringify(raw);
  const first = text.indexOf("{");
  const last = text.lastIndexOf("}");
  if (first < 0 || last <= first) throw new Error("The assistant returned an unreadable response.");
  const parsed = JSON.parse(text.slice(first, last + 1));
  if (typeof parsed.reply !== "string" || !parsed.reply.trim()) throw new Error("The assistant response is missing its reply.");
  return {
    reply: parsed.reply.trim(),
    questions: Array.isArray(parsed.questions) ? parsed.questions.filter(value => typeof value === "string") : [],
    sources: Array.isArray(parsed.sources) ? parsed.sources.filter(source => source && typeof source.title === "string" && /^https:\/\//.test(source.url)) : [],
    recipeDocument: parsed.recipeDocument && typeof parsed.recipeDocument === "object" ? parsed.recipeDocument : null
  };
}
