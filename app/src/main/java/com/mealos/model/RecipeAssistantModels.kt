package com.mealos.model

import java.util.UUID

enum class ChatRole { USER, ASSISTANT }

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
    val sending: Boolean = false,
    val scanning: Boolean = false,
    val error: String? = null
)
