import { query } from "@anthropic-ai/claude-agent-sdk";
import { buildPrompt, normalizeAgentResult } from "./protocol.mjs";

const BLOCKED_TOOLS = [
  "Bash", "BashOutput", "Edit", "ExitPlanMode", "Glob", "Grep", "KillShell",
  "NotebookEdit", "Read", "Skill", "SlashCommand", "Task", "TodoWrite", "WebFetch", "Write"
];

export async function runRecipeAgent(messages, signal) {
  let resultText = null;
  const stream = query({
    prompt: buildPrompt(messages),
    options: {
      model: process.env.CLAUDE_MODEL || "sonnet",
      maxTurns: 8,
      tools: ["WebSearch"],
      allowedTools: ["WebSearch"],
      disallowedTools: BLOCKED_TOOLS,
      settingSources: [],
      permissionMode: "dontAsk",
      persistSession: false,
      abortController: signal ? abortControllerFrom(signal) : undefined,
      canUseTool: async (toolName, input) => toolName === "WebSearch"
        ? { behavior: "allow", updatedInput: input }
        : { behavior: "deny", message: `${toolName} is disabled in MealOS.` }
    }
  });

  for await (const message of stream) {
    if (message.type === "result") {
      if (message.subtype !== "success") throw new Error(message.errors?.join(" ") || "Claude could not complete the recipe request.");
      resultText = message.result;
    }
  }
  if (!resultText) throw new Error("Claude returned no recipe response.");
  return normalizeAgentResult(resultText);
}

function abortControllerFrom(signal) {
  const controller = new AbortController();
  if (signal.aborted) controller.abort(signal.reason);
  else signal.addEventListener("abort", () => controller.abort(signal.reason), { once: true });
  return controller;
}
