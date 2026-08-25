package com.mealos.data

import com.mealos.model.ChatRole
import com.mealos.model.RecipeChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAssistantClientTest {
    @Test
    fun requestContextKeepsNewestMessagesWithinBudget() {
        val messages = (1..8).map { index ->
            RecipeChatMessage(id = "$index", role = ChatRole.USER, content = "$index".repeat(20))
        }

        val selected = recentMessagesForRequest(messages, maxCharacters = 110)

        assertEquals("8", selected.last().id)
        assertTrue(selected.size < messages.size)
        assertEquals(listOf("7", "8"), selected.map { it.id })
    }

    @Test
    fun requestContextAlwaysKeepsLatestLargeMessage() {
        val latest = RecipeChatMessage(id = "latest", role = ChatRole.USER, content = "x".repeat(200))
        assertEquals(listOf(latest), recentMessagesForRequest(listOf(latest), maxCharacters = 20))
    }
}
