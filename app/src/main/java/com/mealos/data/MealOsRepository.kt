package com.mealos.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mealos.model.MealOsUserState
import com.mealos.model.PlanEntry
import com.mealos.model.PortionMode
import com.mealos.model.Recipe
import com.mealos.model.RecipeOrigin
import com.mealos.model.defaultPantry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "mealos_state")

class MealOsRepository(private val context: Context) {
    private object Keys {
        val favorites = stringSetPreferencesKey("favorites")
        val ratings = stringPreferencesKey("ratings")
        val notes = stringPreferencesKey("notes")
        val pantry = stringSetPreferencesKey("pantry")
        val plan = stringPreferencesKey("weekly_plan")
        val checkedGroceries = stringSetPreferencesKey("checked_groceries")
        val customRecipes = stringPreferencesKey("custom_recipes")
    }

    val state: Flow<MealOsUserState> = context.dataStore.data.map(::decodeState)
    val customRecipes: Flow<List<Recipe>> = context.dataStore.data.map { prefs ->
        RecipeDocument.decodeRecipes(prefs[Keys.customRecipes])
            .filterNot { custom -> RecipeCatalog.recipes.any { it.id == custom.id } }
    }
    val recipes: Flow<List<Recipe>> = customRecipes.map { RecipeCatalog.recipes + it }
    private val cloudSync = MealOsCloudSync(this)
    val cloudSyncState = cloudSync.state

    fun connectCloud(uid: String) = cloudSync.start(uid)
    fun disconnectCloud() = cloudSync.stop()

    suspend fun toggleFavorite(recipeId: String) = mutateSet(Keys.favorites, recipeId)
    suspend fun togglePantry(ingredientId: String) = mutateSet(Keys.pantry, ingredientId, defaultPantry)
    suspend fun toggleGrocery(itemId: String) = mutateSet(Keys.checkedGroceries, itemId)

    suspend fun setRating(recipeId: String, rating: Int) {
        context.dataStore.edit { prefs ->
            val map = decodeIntMap(prefs[Keys.ratings]).toMutableMap()
            map[recipeId] = rating.coerceIn(1, 5)
            prefs[Keys.ratings] = JSONObject(map).toString()
        }
    }

    suspend fun setNote(recipeId: String, note: String) {
        context.dataStore.edit { prefs ->
            val map = decodeStringMap(prefs[Keys.notes]).toMutableMap()
            if (note.isBlank()) map.remove(recipeId) else map[recipeId] = note
            prefs[Keys.notes] = JSONObject(map as Map<*, *>).toString()
        }
    }

    suspend fun setPlanEntry(entry: PlanEntry) {
        context.dataStore.edit { prefs ->
            val plan = decodePlan(prefs[Keys.plan]).filterNot { it.dayIndex == entry.dayIndex } + entry
            prefs[Keys.plan] = encodePlan(plan)
            prefs[Keys.checkedGroceries] = emptySet()
        }
    }

    suspend fun removePlanEntry(dayIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.plan] = encodePlan(decodePlan(prefs[Keys.plan]).filterNot { it.dayIndex == dayIndex })
            prefs[Keys.checkedGroceries] = emptySet()
        }
    }

    suspend fun clearPlan() {
        context.dataStore.edit { prefs ->
            prefs[Keys.plan] = "[]"
            prefs[Keys.checkedGroceries] = emptySet()
        }
    }

    suspend fun saveRecipe(recipe: Recipe) {
        require(RecipeDocument.validate(recipe).isEmpty())
        require(recipe.origin != RecipeOrigin.CORE)
        context.dataStore.edit { prefs ->
            val recipes = RecipeDocument.decodeRecipes(prefs[Keys.customRecipes]).toMutableList()
            val saved = recipe.copy(origin = recipe.origin.takeUnless { it == RecipeOrigin.CORE } ?: RecipeOrigin.MANUAL)
            val index = recipes.indexOfFirst { it.id == saved.id }
            if (index >= 0) recipes[index] = saved else recipes.add(saved)
            prefs[Keys.customRecipes] = RecipeDocument.encodeRecipes(recipes)
        }
    }

    suspend fun deleteRecipe(recipeId: String) {
        context.dataStore.edit { prefs ->
            val recipes = RecipeDocument.decodeRecipes(prefs[Keys.customRecipes]).filterNot { it.id == recipeId }
            prefs[Keys.customRecipes] = RecipeDocument.encodeRecipes(recipes)
            prefs[Keys.favorites] = prefs[Keys.favorites].orEmpty() - recipeId
            prefs[Keys.plan] = encodePlan(decodePlan(prefs[Keys.plan]).filterNot { it.recipeId == recipeId })
        }
    }

    internal suspend fun replaceUserState(state: MealOsUserState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.favorites] = state.favorites
            prefs[Keys.ratings] = JSONObject(state.ratings as Map<*, *>).toString()
            prefs[Keys.notes] = JSONObject(state.notes as Map<*, *>).toString()
            prefs[Keys.pantry] = state.pantry
            prefs[Keys.plan] = encodePlan(state.plan)
            prefs[Keys.checkedGroceries] = state.checkedGroceries
        }
    }

    internal suspend fun replaceCustomRecipes(recipes: List<Recipe>) {
        val valid = recipes
            .filter { it.origin != RecipeOrigin.CORE && RecipeDocument.validate(it).isEmpty() }
            .distinctBy { it.id }
        context.dataStore.edit { prefs -> prefs[Keys.customRecipes] = RecipeDocument.encodeRecipes(valid) }
    }

    private suspend fun mutateSet(
        key: Preferences.Key<Set<String>>,
        value: String,
        default: Set<String> = emptySet()
    ) {
        context.dataStore.edit { prefs ->
            val set = (prefs[key] ?: default).toMutableSet()
            if (!set.add(value)) set.remove(value)
            prefs[key] = set
        }
    }

    private fun decodeState(prefs: Preferences) = MealOsUserState(
        favorites = prefs[Keys.favorites].orEmpty(),
        ratings = decodeIntMap(prefs[Keys.ratings]),
        notes = decodeStringMap(prefs[Keys.notes]),
        pantry = prefs[Keys.pantry] ?: defaultPantry,
        plan = decodePlan(prefs[Keys.plan]),
        checkedGroceries = prefs[Keys.checkedGroceries].orEmpty()
    )

    private fun decodeIntMap(raw: String?): Map<String, Int> = runCatching {
        val objectValue = JSONObject(raw ?: "{}")
        objectValue.keys().asSequence().associateWith { objectValue.getInt(it) }
    }.getOrDefault(emptyMap())

    private fun decodeStringMap(raw: String?): Map<String, String> = runCatching {
        val objectValue = JSONObject(raw ?: "{}")
        objectValue.keys().asSequence().associateWith { objectValue.getString(it) }
    }.getOrDefault(emptyMap())

    private fun decodePlan(raw: String?): List<PlanEntry> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            PlanEntry(
                dayIndex = item.getInt("day"),
                recipeId = item.getString("recipe"),
                portionMode = PortionMode.valueOf(item.optString("portion", PortionMode.TWO.name))
            )
        }
    }.getOrDefault(emptyList())

    private fun encodePlan(entries: List<PlanEntry>): String = JSONArray().apply {
        entries.sortedBy { it.dayIndex }.forEach { entry ->
            put(JSONObject().apply {
                put("day", entry.dayIndex)
                put("recipe", entry.recipeId)
                put("portion", entry.portionMode.name)
            })
        }
    }.toString()
}
