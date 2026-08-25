import test from "node:test";
import assert from "node:assert/strict";
import { createMealOsServer } from "../src/server.mjs";

async function withServer(run) {
  const server = createMealOsServer({ token: "test-secret", runAgent: async () => ({ reply: "Ready", questions: [], sources: [], recipeDocument: null }) });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try { await run(`http://127.0.0.1:${server.address().port}`); }
  finally { await new Promise(resolve => server.close(resolve)); }
}

test("health is public but chat requires the bearer token", () => withServer(async base => {
  assert.equal((await fetch(`${base}/health`)).status, 200);
  assert.equal((await fetch(`${base}/v1/recipe-chat`, { method: "POST", body: "{}" })).status, 401);
}));

test("chat returns a stateless agent response", () => withServer(async base => {
  const response = await fetch(`${base}/v1/recipe-chat`, {
    method: "POST",
    headers: { authorization: "Bearer test-secret", "content-type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
  });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).reply, "Ready");
}));
