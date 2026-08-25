package com.mealos.data

import com.mealos.model.MealOsUserState
import com.mealos.model.PlanEntry
import com.mealos.model.PortionMode
import org.json.JSONArray
import org.json.JSONObject

internal object CloudDocumentCodec {
    const val SCHEMA_VERSION = 1

    fun encodeState(state: MealOsUserState): String = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("favorites", JSONArray(state.favorites.sorted()))
        put("ratings", JSONObject(state.ratings as Map<*, *>))
        put("notes", JSONObject(state.notes as Map<*, *>))
        put("pantry", JSONArray(state.pantry.sorted()))
        put("checkedGroceries", JSONArray(state.checkedGroceries.sorted()))
        put("plan", JSONArray().apply {
            state.plan.sortedBy { it.dayIndex }.forEach { entry ->
                put(JSONObject().apply {
                    put("day", entry.dayIndex)
                    put("recipe", entry.recipeId)
                    put("portion", entry.portionMode.name)
                })
            }
        })
    }.toString()

    fun decodeState(raw: String): MealOsUserState? = runCatching {
        val value = JSONObject(raw)
        if (value.optInt("schemaVersion") != SCHEMA_VERSION) return null
        MealOsUserState(
            favorites = value.optJSONArray("favorites").strings().toSet(),
            ratings = value.optJSONObject("ratings").intMap(),
            notes = value.optJSONObject("notes").stringMap(),
            pantry = value.optJSONArray("pantry").strings().toSet(),
            checkedGroceries = value.optJSONArray("checkedGroceries").strings().toSet(),
            plan = value.optJSONArray("plan")?.let { plan ->
                (0 until plan.length()).mapNotNull { index ->
                    plan.optJSONObject(index)?.let { item ->
                        PlanEntry(
                            dayIndex = item.optInt("day").coerceIn(0, 6),
                            recipeId = item.optString("recipe"),
                            portionMode = runCatching { PortionMode.valueOf(item.optString("portion")) }
                                .getOrDefault(PortionMode.TWO)
                        ).takeIf { it.recipeId.isNotBlank() }
                    }
                }.distinctBy { it.dayIndex }
            }.orEmpty()
        )
    }.getOrNull()

    private fun JSONArray?.strings(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()

    private fun JSONObject?.intMap(): Map<String, Int> = this?.let { value ->
        value.keys().asSequence().associateWith { value.optInt(it).coerceIn(1, 5) }
    }.orEmpty()

    private fun JSONObject?.stringMap(): Map<String, String> = this?.let { value ->
        value.keys().asSequence().mapNotNull { key ->
            value.optString(key).takeIf(String::isNotBlank)?.let { key to it }
        }.toMap()
    }.orEmpty()
}
