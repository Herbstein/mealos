package com.mealos.data

import android.content.Context
import com.mealos.model.ChatRole
import com.mealos.model.RecipeAssistantState
import com.mealos.model.RecipeAssistantModel
import com.mealos.model.RecipeChatMessage
import com.mealos.model.RecipeSource
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RecipeAssistantStore(context: Context) {
    private val preferences = context.getSharedPreferences("mealos_recipe_assistant", Context.MODE_PRIVATE)

    fun load(): RecipeAssistantState = runCatching {
        val root = JSONObject(preferences.getString(KEY, "{}") ?: "{}")
        val messages = root.optJSONArray("messages")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { value ->
                    RecipeChatMessage(
                        id = value.optString("id").ifBlank { UUID.randomUUID().toString() },
                        role = runCatching { ChatRole.valueOf(value.optString("role")) }.getOrDefault(ChatRole.USER),
                        content = value.optString("content"),
                        attachmentLabel = value.optString("attachmentLabel").takeIf(String::isNotBlank),
                        attachmentDetail = value.optString("attachmentDetail").takeIf(String::isNotBlank)
                    ).takeIf { it.content.isNotBlank() }
                }
            }
        }.orEmpty()
        val draft = root.optJSONObject("draft")?.let { RecipeDocument.decode(it.toString()).recipe }
        val questions = root.optJSONArray("questions").strings()
        val sources = root.optJSONArray("sources")?.let { array ->
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let {
                RecipeSource(it.optString("title"), it.optString("url"))
            } }
        }.orEmpty()
        val model = runCatching { RecipeAssistantModel.valueOf(root.optString("model")) }
            .getOrDefault(RecipeAssistantModel.SONNET)
        RecipeAssistantState(messages = messages, draft = draft, questions = questions, sources = sources, model = model)
    }.getOrDefault(RecipeAssistantState())

    fun save(state: RecipeAssistantState) {
        val root = JSONObject().apply {
            put("messages", JSONArray().apply { state.messages.takeLast(30).forEach { message ->
                put(JSONObject().apply {
                    put("id", message.id)
                    put("role", message.role.name)
                    put("content", message.content)
                    message.attachmentLabel?.let { put("attachmentLabel", it) }
                    message.attachmentDetail?.let { put("attachmentDetail", it) }
                })
            } })
            state.draft?.let { put("draft", JSONObject(RecipeDocument.encode(it))) }
            put("questions", JSONArray(state.questions))
            put("sources", JSONArray().apply { state.sources.forEach { source ->
                put(JSONObject().put("title", source.title).put("url", source.url))
            } })
            put("model", state.model.name)
        }
        preferences.edit().putString(KEY, root.toString()).apply()
    }

    fun clear() = preferences.edit().remove(KEY).apply()

    private fun JSONArray?.strings(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()

    private companion object { const val KEY = "state" }
}
