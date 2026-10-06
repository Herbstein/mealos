package com.mealos.data

import com.mealos.model.GroceryItem
import com.mealos.model.PlanEntry
import com.mealos.model.Recipe

object GroceryAggregator {
    fun aggregate(
        entries: List<PlanEntry>,
        recipes: List<Recipe>,
        pantry: Set<String>
    ): List<GroceryItem> {
        val recipeById = recipes.associateBy { it.id }
        return entries
            .flatMap { entry ->
                val recipe = recipeById[entry.recipeId] ?: return@flatMap emptyList()
                val multiplier = entry.portionMode.servings.toDouble() / recipe.baseServings
                recipe.ingredients
                    .filterNot { it.id in pantry }
                    .map { ingredient ->
                        GroceryItem(
                            id = "${ingredient.id}:${ingredient.unit}",
                            name = ingredient.name,
                            amount = ingredient.amount * multiplier,
                            unit = ingredient.unit,
                            category = ingredient.category,
                            plannedDays = setOf(entry.dayIndex),
                            pantryStaple = ingredient.pantryStaple
                        )
                    }
            }
            .groupBy { it.id }
            .map { (_, items) -> items.first().copy(amount = items.sumOf { it.amount }, plannedDays = items.flatMap { it.plannedDays }.toSet(), pantryStaple = items.any { it.pantryStaple }) }
            .sortedWith(compareBy<GroceryItem> { it.category.ordinal }.thenBy { it.name })
    }
}
