import { createServer } from "node:http";
import { timingSafeEqual } from "node:crypto";
import { pathToFileURL } from "node:url";
import { runRecipeAgent } from "./agent.mjs";
import { MAX_REQUEST_BYTES, validateRequest } from "./protocol.mjs";

export function createMealOsServer({ token = process.env.MEALOS_BRIDGE_TOKEN, runAgent = runRecipeAgent } = {}) {
  if (!token) throw new Error("MEALOS_BRIDGE_TOKEN is required.");
  return createServer(async (request, response) => {
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Content-Type", "application/json; charset=utf-8");

    if (request.method === "GET" && request.url === "/health") return send(response, 200, { ok: true });
    if (request.method !== "POST" || request.url !== "/v1/recipe-chat") return send(response, 404, { error: "Not found." });
    if (!authorized(request.headers.authorization, token)) return send(response, 401, { error: "Unauthorized." });

    try {
      const body = await readJson(request);
      const invalid = validateRequest(body);
      if (invalid) return send(response, 400, { error: invalid });
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(new Error("Recipe request timed out.")), 110_000);
      request.once("aborted", () => controller.abort());
      response.once("close", () => {
        if (!response.writableEnded) controller.abort();
      });
      try {
        return send(response, 200, await runAgent(body.messages, controller.signal));
      } finally {
        clearTimeout(timeout);
      }
    } catch (error) {
      const status = error?.statusCode || 500;
      if (status >= 500) console.error(error);
      return send(response, status, { error: status >= 500 ? "Recipe assistant is temporarily unavailable." : error.message });
    }
  });
}

function authorized(header, expected) {
  if (!header?.startsWith("Bearer ")) return false;
  const supplied = Buffer.from(header.slice(7));
  const wanted = Buffer.from(expected);
  return supplied.length === wanted.length && timingSafeEqual(supplied, wanted);
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > MAX_REQUEST_BYTES) {
      const error = new Error("Request is too large.");
      error.statusCode = 413;
      throw error;
    }
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    const error = new Error("Request body must be valid JSON.");
    error.statusCode = 400;
    throw error;
  }
}

function send(response, status, body) {
  if (response.writableEnded) return;
  response.writeHead(status);
  response.end(JSON.stringify(body));
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const port = Number(process.env.PORT || 3000);
  createMealOsServer().listen(port, "0.0.0.0", () => console.log(`MealOS bridge listening on ${port}`));
}
