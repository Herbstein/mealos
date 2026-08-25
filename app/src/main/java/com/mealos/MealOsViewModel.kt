package com.mealos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mealos.data.RecipeCatalog
import com.mealos.data.RecipeAssistantClient
import com.mealos.data.RecipeAssistantStore
import com.mealos.data.RecipeDocument
import com.mealos.data.RecipeImportResult
import com.mealos.model.CookingTimer
import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.MealOsUserState
import com.mealos.model.MealOsAccountState
import com.mealos.model.PlanEntry
import com.mealos.model.Recipe
import com.mealos.model.RecipeEditorState
import com.mealos.model.RecipeOrigin
import com.mealos.model.RecipeStep
import com.mealos.model.ChatRole
import com.mealos.model.RecipeAssistantState
import com.mealos.model.RecipeAssistantModel
import com.mealos.model.RecipeChatMessage
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class MealOsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MealOsApplication).repository
    private val timerStore = TimerStore(application)
    private val assistantStore = RecipeAssistantStore(application)
    private val assistantClient = RecipeAssistantClient()
    private val firebaseAuth = FirebaseAuth.getInstance()

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
    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    private val _timers = MutableStateFlow(timerStore.load())
    val timers = _timers.asStateFlow()
    private val _assistantState = MutableStateFlow(assistantStore.load())
    val assistantState = _assistantState.asStateFlow()
    private val _accountState = MutableStateFlow(firebaseAuth.currentUser.toAccountState())
    val accountState = _accountState.asStateFlow()
    private var assistantRequestJob: Job? = null
    private var connectedCloudUid: String? = null
    private val authListener = FirebaseAuth.AuthStateListener { auth -> handleAuthUser(auth.currentUser) }

    init {
        firebaseAuth.addAuthStateListener(authListener)
        viewModelScope.launch {
            repository.cloudSyncState.collect { status ->
                _accountState.update { it.copy(syncStatus = status) }
            }
        }
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
                message = "This is a personal copy. The core recipe stays unchanged.",
                hasUnsavedChanges = true
            )
        } else RecipeEditorState(recipe)
    }

    fun importRecipe(raw: String): RecipeImportResult {
        val result = RecipeDocument.decode(raw)
        result.recipe?.let {
            _editorState.value = RecipeEditorState(
                recipe = it,
                imported = true,
                message = "Imported as a draft. Review it before saving.",
                hasUnsavedChanges = true
            )
        }
        if (result.errors.isNotEmpty()) _importError.value = result.errors.joinToString("\n")
        return result
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun beginGoogleSignIn() {
        _accountState.update { it.copy(signingIn = true, message = null) }
    }

    fun finishGoogleSignIn(error: String? = null) {
        _accountState.update { it.copy(signingIn = false, message = error) }
    }

    fun signOut() {
        firebaseAuth.signOut()
        finishGoogleSignIn()
    }

    fun retryCloudSync() {
        _accountState.value.uid?.let(repository::connectCloud)
    }

    fun setAssistantScanning(scanning: Boolean) {
        _assistantState.update { it.copy(scanning = scanning, error = null) }
    }

    fun attachScannedRecipe(text: String, pageCount: Int) {
        val cleaned = text.trim().take(38_000)
        if (cleaned.isBlank()) {
            _assistantState.update { it.copy(scanning = false, error = "MealOS could not find readable text in that scan. Try brighter, flatter pages.") }
            return
        }
        val message = RecipeChatMessage(
            role = ChatRole.USER,
            content = "I scanned a recipe. Here is the on-device OCR text:\n\n$cleaned",
            attachmentLabel = "Scanned recipe",
            attachmentDetail = "$pageCount ${if (pageCount == 1) "page" else "pages"} · text extracted on device"
        )
        mutateAssistant { it.copy(messages = it.messages + message, scanning = false, error = null) }
    }

    fun assistantScanFailed(message: String) {
        _assistantState.update { it.copy(scanning = false, error = message) }
    }

    fun sendAssistantMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || _assistantState.value.sending) return
        val userMessage = RecipeChatMessage(role = ChatRole.USER, content = trimmed)
        mutateAssistant { it.copy(messages = it.messages + userMessage, sending = true, error = null) }
        requestAssistantResponse()
    }

    fun retryAssistant() {
        if (_assistantState.value.messages.isEmpty() || _assistantState.value.sending) return
        _assistantState.update { it.copy(sending = true, error = null) }
        requestAssistantResponse()
    }

    fun setAssistantModel(model: RecipeAssistantModel) {
        if (_assistantState.value.sending) return
        mutateAssistant { it.copy(model = model, error = null) }
    }

    fun clearAssistantChat() {
        val model = _assistantState.value.model
        assistantRequestJob?.cancel()
        assistantRequestJob = null
        assistantStore.clear()
        _assistantState.value = RecipeAssistantState(model = model)
        assistantStore.save(_assistantState.value)
    }

    fun reviewAssistantDraft() {
        val draft = _assistantState.value.draft ?: return
        importRecipe(RecipeDocument.encode(draft))
    }

    fun updateEditorRecipe(transform: (Recipe) -> Recipe) {
        _editorState.update { state -> state?.copy(recipe = transform(state.recipe), message = null, hasUnsavedChanges = true) }
    }

    fun saveEditorRecipe(): List<String> {
        val state = _editorState.value ?: return listOf("No recipe draft is open.")
        val errors = RecipeDocument.validate(state.recipe)
        if (errors.isNotEmpty()) return errors
        launch { repository.saveRecipe(state.recipe) }
        _editorState.value = state.copy(message = "Saved to your recipe library.", hasUnsavedChanges = false)
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

    private fun handleAuthUser(user: FirebaseUser?) {
        if (user == null) {
            connectedCloudUid = null
            repository.disconnectCloud()
            _accountState.value = MealOsAccountState()
            return
        }
        _accountState.update { current -> user.toAccountState().copy(signingIn = false, message = current.message) }
        if (connectedCloudUid != user.uid) {
            connectedCloudUid = user.uid
            repository.connectCloud(user.uid)
        }
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private fun requestAssistantResponse() {
        val messages = _assistantState.value.messages
        val model = _assistantState.value.model
        assistantRequestJob?.cancel()
        assistantRequestJob = viewModelScope.launch {
            runCatching { assistantClient.chat(messages, model) }
                .onSuccess { response ->
                    val assistantMessage = RecipeChatMessage(role = ChatRole.ASSISTANT, content = response.reply)
                    mutateAssistant { current -> current.copy(
                        messages = current.messages + assistantMessage,
                        draft = response.draft ?: current.draft,
                        questions = response.questions,
                        sources = response.sources,
                        sending = false,
                        error = null
                    ) }
                }
                .onFailure { error ->
                    _assistantState.update { it.copy(sending = false, error = error.message ?: "Recipe Assistant could not connect.") }
                }
        }
    }

    private fun mutateAssistant(transform: (RecipeAssistantState) -> RecipeAssistantState) {
        val updated = transform(_assistantState.value)
        _assistantState.value = updated
        assistantStore.save(updated.copy(sending = false, scanning = false, error = null))
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

private fun FirebaseUser?.toAccountState(): MealOsAccountState = this?.let { user ->
    MealOsAccountState(uid = user.uid, displayName = user.displayName, email = user.email)
} ?: MealOsAccountState()
