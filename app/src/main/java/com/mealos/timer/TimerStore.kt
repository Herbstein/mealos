package com.mealos.timer

import android.content.Context
import com.mealos.model.CookingTimer
import org.json.JSONArray
import org.json.JSONObject

class TimerStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): Map<String, CookingTimer> = decode(preferences.getString(TIMERS_KEY, null))

    @Suppress("ApplySharedPref", "UseKtx")
    fun save(timers: Map<String, CookingTimer>) {
        preferences.edit().putString(TIMERS_KEY, encode(timers.values)).commit()
    }

    fun update(id: String, transform: (CookingTimer) -> CookingTimer) {
        val timers = load().toMutableMap()
        val timer = timers[id] ?: return
        timers[id] = transform(timer)
        save(timers)
    }

    private fun encode(timers: Collection<CookingTimer>): String = JSONArray().apply {
        timers.forEach { timer ->
            put(JSONObject().apply {
                put("id", timer.id)
                put("label", timer.label)
                put("duration_ms", timer.durationMs)
                put("end_at_epoch_ms", timer.endAtEpochMs ?: JSONObject.NULL)
                put("remaining_ms", timer.remainingMs)
                put("running", timer.running)
            })
        }
    }.toString()

    private fun decode(raw: String?): Map<String, CookingTimer> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).associate { index ->
            val value = array.getJSONObject(index)
            val timer = CookingTimer(
                id = value.getString("id"),
                label = value.getString("label"),
                durationMs = value.getLong("duration_ms"),
                endAtEpochMs = value.optLongOrNull("end_at_epoch_ms"),
                remainingMs = value.getLong("remaining_ms"),
                running = value.optBoolean("running", false)
            )
            timer.id to timer
        }
    }.getOrDefault(emptyMap())

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private companion object {
        const val PREFERENCES_NAME = "mealos_timers"
        const val TIMERS_KEY = "active_timers"
    }
}
