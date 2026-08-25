package com.mealos.data

import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.Recipe
import com.mealos.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeDocumentTest {
    @Test
    fun validationAcceptsACompleteRecipe() {
        assertTrue(RecipeDocument.validate(validRecipe()).isEmpty())
    }

    @Test
    fun validationExplainsEveryInvalidCookingField() {
        val errors = RecipeDocument.validate(
            validRecipe().copy(
                name = "",
                totalMinutes = 5,
                effortMinutes = 10,
                ingredients = listOf(validRecipe().ingredients.first().copy(amount = 0.0)),
                steps = listOf(RecipeStep("", "", "", timerSeconds = -1))
            )
        )

        assertTrue(errors.any { it.contains("recipe name") })
        assertTrue(errors.any { it.contains("Total time") })
        assertTrue(errors.any { it.contains("above zero") })
        assertTrue(errors.any { it.contains("needs a title") })
        assertTrue(errors.any { it.contains("needs an instruction") })
        assertTrue(errors.any { it.contains("needs equipment") })
        assertTrue(errors.any { it.contains("invalid timer") })
    }

    @Test
    fun ingredientIdsAreStableForGroceryConsolidation() {
        assertEquals("baby-potatoes", RecipeDocument.ingredientId("  Baby Potatoes! "))
    }

    private fun validRecipe() = Recipe(
        id = "test-recipe",
        name = "Test chicken and rice",
        shortName = "Chicken & rice",
        effortMinutes = 10,
        totalMinutes = 30,
        baseServings = 2,
        equipment = listOf("CRISPi", "Rice cooker"),
        tags = listOf("Low effort"),
        leftoverNote = "Pack one serving for lunch.",
        ingredients = listOf(
            Ingredient("chicken", "Chicken breast", 1.0, "lb", IngredientCategory.MEAT)
        ),
        steps = listOf(
            RecipeStep("Cook chicken", "Air fry until cooked through.", "CRISPi", 600)
        )
    )
}
