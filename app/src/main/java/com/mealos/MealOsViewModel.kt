package com.mealos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealos.data.RecipeCatalog
import com.mealos.model.CookingTimer
import com.mealos.model.MealOsUserState
import com.mealos.model.PlanEntry
import com.mealos.timer.TimerNotifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MealOsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealOsApplication).repository

    val userState: StateFlow<MealOsUserState> = repository.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MealOsUserState()
    )

    private val _timers = MutableStateFlow<Map<String, CookingTimer>>(emptyMap())
    val timers = _timers.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _timers.update { timers ->
                    timers.mapValues { (_, timer) ->
                        if (!timer.running || timer.endAtEpochMs == null) timer
                        else timer.copy(remainingMs = (timer.endAtEpochMs - now).coerceAtLeast(0))
                    }
                }
                delay(1_000)
            }
        }
    }

    fun toggleFavorite(id: String) = launch { repository.toggleFavorite(id) }
    fun setRating(id: String, rating: Int) = launch { repository.setRating(id, rating) }
    fun setNote(id: String, note: String) = launch { repository.setNote(id, note) }
    fun togglePantry(id: String) = launch { repository.togglePantry(id) }
    fun toggleGrocery(id: String) = launch { repository.toggleGrocery(id) }
    fun setPlan(entry: PlanEntry) = launch { repository.setPlanEntry(entry) }
    fun removePlan(dayIndex: Int) = launch { repository.removePlanEntry(dayIndex) }
    fun clearPlan() = launch { repository.clearPlan() }

    fun addTimer(label: String, seconds: Int) {
        val id = label.lowercase().replace(" ", "-")
        val duration = seconds * 1_000L
        _timers.update { it + (id to CookingTimer(id, label, duration, remainingMs = duration)) }
    }

    fun toggleTimer(id: String) {
        val current = _timers.value[id] ?: return
        if (current.running) {
            TimerNotifications.cancel(getApplication(), id)
            val remaining = ((current.endAtEpochMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0)
            _timers.update { it + (id to current.copy(running = false, endAtEpochMs = null, remainingMs = remaining)) }
        } else {
            val endAt = System.currentTimeMillis() + current.remainingMs
            TimerNotifications.schedule(getApplication(), id, current.label, endAt)
            _timers.update { it + (id to current.copy(running = true, endAtEpochMs = endAt)) }
        }
    }

    fun addMinute(id: String) {
        val current = _timers.value[id] ?: return
        val updated = if (current.running) {
            current.copy(endAtEpochMs = (current.endAtEpochMs ?: System.currentTimeMillis()) + 60_000)
        } else current.copy(remainingMs = current.remainingMs + 60_000)
        if (updated.running && updated.endAtEpochMs != null) {
            TimerNotifications.schedule(getApplication(), id, updated.label, updated.endAtEpochMs)
        }
        _timers.update { it + (id to updated) }
    }

    fun seedPlanIfEmpty() {
        if (userState.value.plan.isNotEmpty()) return
        RecipeCatalog.recipes.take(5).forEachIndexed { index, recipe ->
            setPlan(PlanEntry(index, recipe.id))
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
