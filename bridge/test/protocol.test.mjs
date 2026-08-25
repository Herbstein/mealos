import test from "node:test";
import assert from "node:assert/strict";
import { buildPrompt, normalizeAgentResult, requestModel, validateRequest } from "../src/protocol.mjs";

test("validates a compact conversation", () => {
  assert.equal(validateRequest({ messages: [{ role: "user", content: "Make dinner" }] }), null);
  assert.match(validateRequest({ messages: [] }), /one message/i);
  assert.match(validateRequest({ messages: [{ role: "system", content: "no" }] }), /valid role/i);
  assert.equal(validateRequest({ model: "opus", messages: [{ role: "user", content: "Make dinner" }] }), null);
  assert.match(validateRequest({ model: "anything", messages: [{ role: "user", content: "Make dinner" }] }), /supported/i);
  assert.equal(requestModel({ messages: [] }), "sonnet");
  assert.equal(requestModel({ model: "claude-haiku-4-5" }), "claude-haiku-4-5");
});

test("prompt carries the conversation and product constraints", () => {
  const prompt = buildPrompt([{ role: "user", content: "Use chicken" }]);
  assert.match(prompt, /Avoid seafood and mushrooms/);
  assert.match(prompt, /USER:\nUse chicken/);
  assert.match(prompt, /mealos\.recipe/);
});

test("normalizes fenced or prefixed JSON", () => {
  const result = normalizeAgentResult('```json\n{"reply":"Ready","questions":[],"sources":[],"recipeDocument":null}\n```');
  assert.equal(result.reply, "Ready");
  assert.equal(result.recipeDocument, null);
});
