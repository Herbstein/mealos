import test from "node:test";
import assert from "node:assert/strict";
import { createMealOsServer, parseAllowedUids } from "../src/server.mjs";

async function withServer(run) {
  const server = createMealOsServer({
    allowedUid: "owner-uid",
    verifyToken: async token => token === "valid-token" ? { uid: "owner-uid" } : Promise.reject(new Error("invalid")),
    runAgent: async () => ({ reply: "Ready", questions: [], sources: [], recipeDocument: null })
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try { await run(`http://127.0.0.1:${server.address().port}`); }
  finally { await new Promise(resolve => server.close(resolve)); }
}

test("health is public but chat requires Firebase authentication", () => withServer(async base => {
  assert.equal((await fetch(`${base}/health`)).status, 200);
  assert.equal((await fetch(`${base}/v1/recipe-chat`, { method: "POST", body: "{}" })).status, 401);
  assert.equal((await fetch(`${base}/v1/recipe-chat`, {
    method: "POST",
    headers: { authorization: "Bearer invalid-token", "content-type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
  })).status, 401);
}));

test("chat returns a stateless agent response", () => withServer(async base => {
  const response = await fetch(`${base}/v1/recipe-chat`, {
    method: "POST",
    headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
  });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).reply, "Ready");
}));

test("parses, trims, and deduplicates a UID allowlist", () => {
  assert.deepEqual([...parseAllowedUids("owner-uid, friend-uid,owner-uid", [" third-uid "])], [
    "owner-uid",
    "friend-uid",
    "third-uid"
  ]);
});

test("allows every Firebase user in the configured allowlist", async () => {
  const server = createMealOsServer({
    allowedUids: "owner-uid,friend-uid",
    verifyToken: async () => ({ uid: "friend-uid" }),
    runAgent: async () => ({ reply: "Ready", questions: [], sources: [], recipeDocument: null })
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/v1/recipe-chat`, {
      method: "POST",
      headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
      body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
    });
    assert.equal(response.status, 200);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});

test("passes only an allowed model to the agent", async () => {
  let receivedModel;
  const server = createMealOsServer({
    allowedUid: "owner-uid",
    verifyToken: async () => ({ uid: "owner-uid" }),
    runAgent: async (_messages, model) => {
      receivedModel = model;
      return { reply: "Ready", questions: [], sources: [], recipeDocument: null };
    }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    const response = await fetch(`${base}/v1/recipe-chat`, {
      method: "POST",
      headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
      body: JSON.stringify({ model: "opus", messages: [{ role: "user", content: "Make dinner" }] })
    });
    assert.equal(response.status, 200);
    assert.equal(receivedModel, "opus");
    const rejected = await fetch(`${base}/v1/recipe-chat`, {
      method: "POST",
      headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
      body: JSON.stringify({ model: "unknown", messages: [{ role: "user", content: "Make dinner" }] })
    });
    assert.equal(rejected.status, 400);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});

test("rejects a valid Firebase user who is not on the allowlist", async () => {
  const server = createMealOsServer({
    allowedUid: "owner-uid",
    verifyToken: async () => ({ uid: "someone-else" }),
    runAgent: async () => ({ reply: "Ready" })
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/v1/recipe-chat`, {
      method: "POST",
      headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
      body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
    });
    assert.equal(response.status, 403);
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});

test("limits concurrent agent work", async () => {
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  const server = createMealOsServer({
    allowedUid: "owner-uid",
    verifyToken: async () => ({ uid: "owner-uid" }),
    runAgent: async () => { await gate; return { reply: "Ready", questions: [], sources: [], recipeDocument: null }; }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const options = {
    method: "POST",
    headers: { authorization: "Bearer valid-token", "content-type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", content: "Make dinner" }] })
  };
  try {
    const first = fetch(`${base}/v1/recipe-chat`, options);
    const second = fetch(`${base}/v1/recipe-chat`, options);
    await new Promise(resolve => setTimeout(resolve, 20));
    assert.equal((await fetch(`${base}/v1/recipe-chat`, options)).status, 429);
    release();
    assert.deepEqual((await Promise.all([first, second])).map(response => response.status), [200, 200]);
  } finally {
    release();
    await new Promise(resolve => server.close(resolve));
  }
});
