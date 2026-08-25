package com.mealos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mealos.data.RecipeCatalog
import com.mealos.data.RecipeDocument
import com.mealos.data.RecipeImportResult
import com.mealos.model.CookingTimer
import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.MealOsUserState
import com.mealos.model.PlanEntry
import com.mealos.model.Recipe
import com.mealos.model.RecipeEditorState
import com.mealos.model.RecipeOrigin
import com.mealos.model.RecipeStep
import com.mealos.timer.TimerNotifications
import com.mealos.timer.TimerStore
import com.mealos.timer.refreshedAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class MealOsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealOsApplication).repository
    private val timerStore = TimerStore(application)

    val userState: StateFlow<MealOsUserState> = repository.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MealOsUserState()
    )

    val recipes: StateFlow<List<Recipe>> = repository.recipes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RecipeCatalog.recipes
    )

    private val _editorState = MutableStateFlow<RecipeEditorState?>(null)
    val editorState = _editorState.asStateFlow()

    private val _timers = MutableStateFlow(timerStore.load())
    val timers = _timers.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _timers.update { timers ->
                    var completed = false
                    val updated = timers.mapValues { (_, timer) ->
                        val refreshed = timer.refreshedAt(now)
                        if (timer.running && !refreshed.running) completed = true
                        refreshed
                    }
                    if (completed) timerStore.save(updated)
                    updated
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

    fun startNewRecipe() {
        _editorState.value = RecipeEditorState(blankRecipe())
    }

    fun startEditingRecipe(recipeId: String) {
        val recipe = recipes.value.firstOrNull { it.id == recipeId } ?: return
        _editorState.value = if (recipe.origin == RecipeOrigin.CORE) {
            RecipeEditorState(
                recipe.copy(id = UUID.randomUUID().toString(), origin = RecipeOrigin.MANUAL),
                message = "This is a personal copy. The core recipe stays unchanged."
            )
        } else RecipeEditorState(recipe)
    }

    fun importRecipe(raw: String): RecipeImportResult {
        val result = RecipeDocument.decode(raw)
        result.recipe?.let {
            _editorState.value = RecipeEditorState(
                recipe = it,
                imported = true,
                message = "Imported as a draft. Review it before saving."
            )
        }
        return result
    }

    fun updateEditorRecipe(transform: (Recipe) -> Recipe) {
        _editorState.update { state -> state?.copy(recipe = transform(state.recipe), message = null) }
    }

    fun saveEditorRecipe(): List<String> {
        val state = _editorState.value ?: return listOf("No recipe draft is open.")
        val errors = RecipeDocument.validate(state.recipe)
        if (errors.isNotEmpty()) return errors
        launch { repository.saveRecipe(state.recipe) }
        _editorState.value = state.copy(message = "Saved to your recipe library.")
        return emptyList()
    }

    fun closeEditor() {
        _editorState.value = null
    }

    fun deleteRecipe(recipeId: String) = launch { repository.deleteRecipe(recipeId) }

    fun addTimer(label: String, seconds: Int) {
        val id = UUID.randomUUID().toString()
        val duration = seconds * 1_000L
        mutateTimers { it + (id to CookingTimer(id, label, duration, remainingMs = duration)) }
    }

    fun toggleTimer(id: String) {
        val current = _timers.value[id] ?: return
        if (current.running) {
            TimerNotifications.cancel(getApplication(), id)
            val remaining = ((current.endAtEpochMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0)
            mutateTimers { it + (id to current.copy(running = false, endAtEpochMs = null, remainingMs = remaining)) }
        } else {
            val endAt = System.currentTimeMillis() + current.remainingMs
            mutateTimers { it + (id to current.copy(running = true, endAtEpochMs = endAt)) }
            TimerNotifications.schedule(getApplication(), id, current.label, endAt)
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
        mutateTimers { it + (id to updated) }
    }

    fun rescheduleRunningTimers() {
        TimerNotifications.reschedulePersisted(getApplication())
        _timers.value = timerStore.load()
    }

    fun seedPlanIfEmpty() {
        if (userState.value.plan.isNotEmpty()) return
        recipes.value.take(5).forEachIndexed { index, recipe ->
            setPlan(PlanEntry(index, recipe.id))
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun mutateTimers(transform: (Map<String, CookingTimer>) -> Map<String, CookingTimer>) {
        val updated = transform(_timers.value)
        timerStore.save(updated)
        _timers.value = updated
    }

    private fun blankRecipe() = Recipe(
        id = UUID.randomUUID().toString(),
        name = "",
        shortName = "",
        effortMinutes = 10,
        totalMinutes = 30,
        baseServings = 2,
        equipment = emptyList(),
        tags = emptyList(),
        leftoverNote = "",
        ingredients = listOf(Ingredient(UUID.randomUUID().toString(), "", 1.0, "count", IngredientCategory.PANTRY)),
        steps = listOf(RecipeStep("", "", "Kitchen")),
        origin = RecipeOrigin.MANUAL
    )
}
