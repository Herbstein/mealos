package com.mealos.data

import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.Recipe
import com.mealos.model.RecipeOrigin
import com.mealos.model.RecipeStep
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val RECIPE_DOCUMENT_MIME = "application/vnd.mealos.recipe+json"
const val RECIPE_DOCUMENT_EXTENSION = "mealosrecipe"

data class RecipeImportResult(
    val recipe: Recipe? = null,
    val errors: List<String> = emptyList()
)

object RecipeDocument {
    const val FORMAT = "mealos.recipe"
    const val SCHEMA_VERSION = 1

    fun encode(recipe: Recipe): String = JSONObject().apply {
        put("format", FORMAT)
        put("schemaVersion", SCHEMA_VERSION)
        put("recipe", encodeRecipe(recipe))
    }.toString(2)

    fun encodeRecipes(recipes: List<Recipe>): String = JSONArray().apply {
        recipes.forEach { put(encodeRecipe(it)) }
    }.toString()

    fun decode(raw: String, imported: Boolean = true): RecipeImportResult {
        val cleaned = extractJson(raw)
        val objectValue = runCatching { JSONObject(cleaned) }.getOrElse {
            return RecipeImportResult(errors = listOf("MealOS could not read this as a recipe file."))
        }
        val recipeObject = when {
            objectValue.optString("format") == FORMAT -> {
                val version = objectValue.optInt("schemaVersion", 0)
                if (version != SCHEMA_VERSION) {
                    return RecipeImportResult(errors = listOf("This recipe uses unsupported schema version $version."))
                }
                objectValue.optJSONObject("recipe")
            }
            objectValue.has("ingredients") && objectValue.has("steps") -> objectValue
            else -> null
        } ?: return RecipeImportResult(errors = listOf("The recipe object is missing."))

        return runCatching {
            val recipe = decodeRecipe(recipeObject).copy(
                id = if (imported) UUID.randomUUID().toString() else recipeObject.optString("id", UUID.randomUUID().toString()),
                origin = if (imported) RecipeOrigin.CHATGPT_IMPORT else decodeOrigin(recipeObject)
            )
            val errors = validate(recipe)
            RecipeImportResult(recipe = recipe.takeIf { errors.isEmpty() }, errors = errors)
        }.getOrElse { error ->
            RecipeImportResult(errors = listOf(error.message ?: "This recipe contains invalid fields."))
        }
    }

    fun decodeRecipes(raw: String?): List<Recipe> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            runCatching { decodeRecipe(item) }.getOrNull()
        }
    }.getOrDefault(emptyList())

    fun validate(recipe: Recipe): List<String> = buildList {
        if (recipe.name.isBlank()) add("Add a recipe name.")
        if (recipe.shortName.isBlank()) add("Add a short name for compact screens.")
        if (recipe.baseServings < 1) add("Servings must be at least 1.")
        if (recipe.effortMinutes < 0 || recipe.totalMinutes < 1) add("Check the recipe times.")
        if (recipe.totalMinutes < recipe.effortMinutes) add("Total time cannot be shorter than hands-on time.")
        if (recipe.ingredients.isEmpty()) add("Add at least one ingredient.")
        recipe.ingredients.forEachIndexed { index, ingredient ->
            if (ingredient.name.isBlank()) add("Ingredient ${index + 1} needs a name.")
            if (ingredient.amount <= 0) add("${ingredient.name.ifBlank { "Ingredient ${index + 1}" }} needs an amount above zero.")
            if (ingredient.unit.isBlank()) add("${ingredient.name.ifBlank { "Ingredient ${index + 1}" }} needs a unit.")
        }
        if (recipe.steps.isEmpty()) add("Add at least one cooking step.")
        recipe.steps.forEachIndexed { index, step ->
            if (step.title.isBlank()) add("Step ${index + 1} needs a title.")
            if (step.instruction.isBlank()) add("Step ${index + 1} needs an instruction.")
            if (step.equipment.isBlank()) add("Step ${index + 1} needs equipment or a work surface.")
            if (step.timerSeconds != null && step.timerSeconds <= 0) add("Step ${index + 1} has an invalid timer.")
        }
    }

    fun ingredientId(name: String): String = name.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { UUID.randomUUID().toString() }

    private fun encodeRecipe(recipe: Recipe) = JSONObject().apply {
        put("id", recipe.id)
        put("name", recipe.name)
        put("shortName", recipe.shortName)
        put("description", recipe.description)
        put("effortMinutes", recipe.effortMinutes)
        put("totalMinutes", recipe.totalMinutes)
        put("baseServings", recipe.baseServings)
        put("equipment", JSONArray(recipe.equipment))
        put("tags", JSONArray(recipe.tags))
        put("leftoverNote", recipe.leftoverNote)
        put("origin", recipe.origin.name)
        put("ingredients", JSONArray().apply {
            recipe.ingredients.forEach { ingredient ->
                put(JSONObject().apply {
                    put("id", ingredient.id)
                    put("name", ingredient.name)
                    put("amount", ingredient.amount)
                    put("unit", ingredient.unit)
                    put("category", ingredient.category.name)
                    put("pantryStaple", ingredient.pantryStaple)
                })
            }
        })
        put("steps", JSONArray().apply {
            recipe.steps.forEach { step ->
                put(JSONObject().apply {
                    put("title", step.title)
                    put("instruction", step.instruction)
                    put("equipment", step.equipment)
                    step.timerSeconds?.let { put("timerSeconds", it) }
                    step.tip?.let { put("tip", it) }
                })
            }
        })
    }

    private fun decodeRecipe(value: JSONObject): Recipe {
        val ingredients = value.getJSONArray("ingredients").objects().map { item ->
            Ingredient(
                id = item.optString("id").ifBlank { ingredientId(item.getString("name")) },
                name = item.getString("name"),
                amount = item.getDouble("amount"),
                unit = item.getString("unit"),
                category = enumValueOrDefault(item.optString("category"), IngredientCategory.PANTRY),
                pantryStaple = item.optBoolean("pantryStaple", false)
            )
        }
        val steps = value.getJSONArray("steps").objects().map { item ->
            RecipeStep(
                title = item.getString("title"),
                instruction = item.getString("instruction"),
                equipment = item.optString("equipment", "Kitchen"),
                timerSeconds = item.optInt("timerSeconds").takeIf { item.has("timerSeconds") && it > 0 },
                tip = item.optString("tip").takeIf { it.isNotBlank() }
            )
        }
        return Recipe(
            id = value.optString("id", UUID.randomUUID().toString()),
            name = value.optString("name"),
            shortName = value.optString("shortName", value.optString("name")),
            effortMinutes = value.optInt("effortMinutes", 10),
            totalMinutes = value.optInt("totalMinutes", 30),
            baseServings = value.optInt("baseServings", 2),
            equipment = value.optJSONArray("equipment")?.strings().orEmpty(),
            tags = value.optJSONArray("tags")?.strings().orEmpty(),
            leftoverNote = value.optString("leftoverNote"),
            ingredients = ingredients,
            steps = steps,
            description = value.optString("description"),
            origin = decodeOrigin(value)
        )
    }

    private fun decodeOrigin(value: JSONObject): RecipeOrigin =
        enumValueOrDefault(value.optString("origin"), RecipeOrigin.MANUAL)

    private fun extractJson(raw: String): String {
        val first = raw.indexOf('{')
        val last = raw.lastIndexOf('}')
        return if (first >= 0 && last > first) raw.substring(first, last + 1) else raw.trim()
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> =
        (0 until length()).map { getString(it) }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, default: T): T =
        enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
}
