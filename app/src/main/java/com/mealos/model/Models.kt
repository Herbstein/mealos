package com.mealos.model

import java.text.DecimalFormat

enum class IngredientCategory(val label: String) {
    PRODUCE("Produce"),
    MEAT("Meat"),
    DAIRY("Dairy"),
    BAKERY("Bakery"),
    PANTRY("Pantry"),
    FROZEN("Frozen")
}

enum class PortionMode(val label: String, val servings: Int) {
    ONE("Just me", 1),
    TWO("Dinner for two", 2),
    DINNER_PLUS_LUNCH("Dinner + lunch tomorrow", 4)
}

enum class RecipeOrigin {
    CORE,
    MANUAL,
    CHATGPT_IMPORT
}

data class Ingredient(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val category: IngredientCategory,
    val pantryStaple: Boolean = false
)

data class RecipeStep(
    val title: String,
    val instruction: String,
    val equipment: String,
    val timerSeconds: Int? = null,
    val tip: String? = null
)

data class Recipe(
    val id: String,
    val name: String,
    val shortName: String,
    val effortMinutes: Int,
    val totalMinutes: Int,
    val baseServings: Int = 2,
    val equipment: List<String>,
    val tags: List<String>,
    val leftoverNote: String,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
    val description: String = "",
    val origin: RecipeOrigin = RecipeOrigin.CORE
)

data class RecipeEditorState(
    val recipe: Recipe,
    val imported: Boolean = false,
    val message: String? = null,
    val hasUnsavedChanges: Boolean = false
)

data class PlanEntry(
    // Local calendar date as days since 1970-01-01 (schema 2).
    val dayIndex: Int,
    val recipeId: String,
    val portionMode: PortionMode = PortionMode.TWO
)

data class MealOsUserState(
    val favorites: Set<String> = emptySet(),
    val ratings: Map<String, Int> = emptyMap(),
    val notes: Map<String, String> = emptyMap(),
    val pantry: Set<String> = defaultPantry,
    val plan: List<PlanEntry> = emptyList(),
    val checkedGroceries: Set<String> = emptySet()
)

data class GroceryItem(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val category: IngredientCategory,
    val plannedDays: Set<Int> = emptySet(),
    val pantryStaple: Boolean = false
) {
    val displayAmount: String
        get() = "${numberFormat.format(amount)} $unit".trim()
}

data class CookingTimer(
    val id: String,
    val label: String,
    val durationMs: Long,
    val endAtEpochMs: Long? = null,
    val remainingMs: Long = durationMs,
    val running: Boolean = false
)

val defaultPantry = setOf(
    "olive-oil", "salt", "black-pepper", "garlic-powder", "italian-seasoning"
)

private val numberFormat = DecimalFormat("0.##")

fun scaledAmount(ingredient: Ingredient, recipe: Recipe, mode: PortionMode): String {
    val scaled = ingredient.amount * mode.servings / recipe.baseServings
    return "${numberFormat.format(scaled)} ${ingredient.unit}".trim()
}
