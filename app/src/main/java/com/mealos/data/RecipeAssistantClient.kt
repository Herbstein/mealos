package com.mealos.data

import com.mealos.BuildConfig
import com.mealos.model.ChatRole
import com.mealos.model.RecipeChatMessage
import com.mealos.model.RecipeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RecipeAssistantResponse(
    val reply: String,
    val questions: List<String>,
    val sources: List<RecipeSource>,
    val draft: com.mealos.model.Recipe?
)

class RecipeAssistantClient(
    private val baseUrl: String = BuildConfig.RECIPE_BRIDGE_URL,
    private val token: String = BuildConfig.RECIPE_BRIDGE_TOKEN
) {
    suspend fun chat(messages: List<RecipeChatMessage>): RecipeAssistantResponse = withContext(Dispatchers.IO) {
        check(baseUrl.isNotBlank() && token.isNotBlank()) {
            "Recipe Assistant has not been connected yet. Add the Railway address and MealOS token to the app build."
        }
        val connection = (URL("${baseUrl.trimEnd('/')}/v1/recipe-chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val request = JSONObject().put("messages", JSONArray().apply {
                recentMessagesForRequest(messages).forEach { message ->
                    put(JSONObject().apply {
                        put("role", if (message.role == ChatRole.USER) "user" else "assistant")
                        put("content", message.content)
                    })
                }
            })
            connection.outputStream.bufferedWriter().use { it.write(request.toString()) }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val body = runCatching { JSONObject(raw) }.getOrNull()
            if (status !in 200..299) throw IllegalStateException(body?.optString("error")?.takeIf { it.isNotBlank() }
                ?: "Recipe Assistant could not connect ($status).")
            val reply = body?.optString("reply")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Recipe Assistant returned an empty response.")
            val questions = body.optJSONArray("questions").stringValues()
            val sources = body.optJSONArray("sources")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optJSONObject(index)?.let { source ->
                        val title = source.optString("title")
                        val url = source.optString("url")
                        RecipeSource(title, url).takeIf { title.isNotBlank() && url.startsWith("https://") }
                    }
                }
            }.orEmpty()
            val draft = body.optJSONObject("recipeDocument")?.let { document -> RecipeDocument.decode(document.toString()).recipe }
            RecipeAssistantResponse(reply, questions, sources, draft)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray?.stringValues(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()
}

internal fun recentMessagesForRequest(messages: List<RecipeChatMessage>, maxCharacters: Int = 180_000): List<RecipeChatMessage> {
    val selected = ArrayDeque<RecipeChatMessage>()
    var used = 0
    for (message in messages.asReversed().take(30)) {
        val cost = message.content.length + 32
        if (selected.isNotEmpty() && used + cost > maxCharacters) break
        selected.addFirst(message)
        used += cost
    }
    return selected.toList()
}
