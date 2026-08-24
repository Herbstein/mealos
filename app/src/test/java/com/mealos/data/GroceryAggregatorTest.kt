package com.mealos.data

import com.mealos.model.PlanEntry
import com.mealos.model.PortionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroceryAggregatorTest {
    @Test
    fun catalogContainsTheFifteenCoreMeals() {
        assertEquals(15, RecipeCatalog.recipes.size)
        assertTrue(RecipeCatalog.recipes.any { it.id == "garlic-herb-chicken" })
        assertTrue(RecipeCatalog.recipes.any { it.id == "pizza" })
        assertTrue(RecipeCatalog.recipes.any { it.id == "lazy-chicken-sandwich" })
    }

    @Test
    fun catalogAvoidsDislikedCoreIngredients() {
        val names = RecipeCatalog.recipes.flatMap { it.ingredients }.map { it.name.lowercase() }
        assertFalse(names.any { "mushroom" in it })
        assertFalse(names.any { "salmon" in it || "shrimp" in it || "fish" in it })
        assertFalse(names.any { "pork" in it })
    }

    @Test
    fun groceryListConsolidatesAndExcludesPantry() {
        val result = GroceryAggregator.aggregate(
            entries = listOf(
                PlanEntry(0, "garlic-herb-chicken", PortionMode.TWO),
                PlanEntry(1, "bbq-chicken", PortionMode.DINNER_PLUS_LUNCH)
            ),
            recipes = RecipeCatalog.recipes,
            pantry = setOf("olive-oil", "salt")
        )
        assertEquals(3.0, result.first { it.name == "Chicken breasts" }.amount, 0.001)
        assertEquals(3.0, result.first { it.name == "Baby potatoes" }.amount, 0.001)
        assertFalse(result.any { it.name == "Olive oil" || it.name == "Salt" })
    }
}
