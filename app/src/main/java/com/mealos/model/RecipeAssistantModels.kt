package com.mealos.model

import java.util.UUID

enum class ChatRole { USER, ASSISTANT }

enum class RecipeAssistantModel(
    val label: String,
    val requestValue: String,
    val description: String
) {
    SONNET("Sonnet", "sonnet", "Balanced"),
    OPUS("Opus", "opus", "Most capable"),
    HAIKU("Haiku", "claude-haiku-4-5", "Fastest")
}

data class RecipeChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val attachmentLabel: String? = null,
    val attachmentDetail: String? = null
)

data class RecipeSource(val title: String, val url: String)

data class RecipeAssistantState(
    val messages: List<RecipeChatMessage> = emptyList(),
    val draft: Recipe? = null,
    val questions: List<String> = emptyList(),
    val sources: List<RecipeSource> = emptyList(),
    val model: RecipeAssistantModel = RecipeAssistantModel.SONNET,
    val sending: Boolean = false,
    val scanning: Boolean = false,
    val error: String? = null
)
