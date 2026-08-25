package com.mealos.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.mealos.model.CloudSyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(FlowPreview::class)
internal class MealOsCloudSync(private val repository: MealOsRepository) {
    private val firestore = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(CloudSyncStatus.SIGNED_OUT)
    val state = _state.asStateFlow()
    private var scope: CoroutineScope? = null
    private var startup: Job? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private var lastStatePayload: String? = null
    private var lastRecipesPayload: String? = null

    fun start(uid: String) {
        stop()
        _state.value = CloudSyncStatus.CONNECTING
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope
        startup = newScope.launch {
            val base = firestore.collection("users").document(uid).collection("mealos")
            val stateDocument = base.document("preferences")
            val recipesDocument = base.document("recipes")
            runCatching {
                prepareState(stateDocument)
                prepareRecipes(recipesDocument)
                listenForState(stateDocument, newScope)
                listenForRecipes(recipesDocument, newScope)
                uploadLocalState(stateDocument, newScope)
                uploadLocalRecipes(recipesDocument, newScope)
                _state.value = CloudSyncStatus.SYNCED
            }.onFailure(::reportFailure)
        }
    }

    fun stop() {
        startup?.cancel()
        startup = null
        listeners.forEach(ListenerRegistration::remove)
        listeners.clear()
        scope?.cancel()
        scope = null
        lastStatePayload = null
        lastRecipesPayload = null
        _state.value = CloudSyncStatus.SIGNED_OUT
    }

    private suspend fun prepareState(document: DocumentReference) {
        val remote = document.get().awaitResult().getString("payload")
        val decoded = remote?.let(CloudDocumentCodec::decodeState)
        if (decoded != null) {
            lastStatePayload = CloudDocumentCodec.encodeState(decoded)
            repository.replaceUserState(decoded)
        } else {
            val local = CloudDocumentCodec.encodeState(repository.state.first())
            lastStatePayload = local
            document.set(cloudValue(local)).awaitResult()
        }
    }

    private suspend fun prepareRecipes(document: DocumentReference) {
        val remote = document.get().awaitResult().getString("payload")
        val decoded = remote?.let(RecipeDocument::decodeRecipes)
        if (remote != null) {
            val canonical = RecipeDocument.encodeRecipes(decoded.orEmpty())
            lastRecipesPayload = canonical
            repository.replaceCustomRecipes(decoded.orEmpty())
        } else {
            val local = RecipeDocument.encodeRecipes(repository.customRecipes.first())
            lastRecipesPayload = local
            document.set(cloudValue(local)).awaitResult()
        }
    }

    private fun listenForState(document: DocumentReference, worker: CoroutineScope) {
        listeners += document.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) return@addSnapshotListener reportFailure(error)
            val payload = snapshot?.getString("payload") ?: return@addSnapshotListener
            if (snapshot.metadata.hasPendingWrites()) _state.value = CloudSyncStatus.SYNCING
            else _state.value = CloudSyncStatus.SYNCED
            val decoded = CloudDocumentCodec.decodeState(payload) ?: return@addSnapshotListener
            val canonical = CloudDocumentCodec.encodeState(decoded)
            if (canonical != lastStatePayload) {
                lastStatePayload = canonical
                worker.launch { repository.replaceUserState(decoded) }
            }
        }
    }

    private fun listenForRecipes(document: DocumentReference, worker: CoroutineScope) {
        listeners += document.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) return@addSnapshotListener reportFailure(error)
            val payload = snapshot?.getString("payload") ?: return@addSnapshotListener
            if (snapshot.metadata.hasPendingWrites()) _state.value = CloudSyncStatus.SYNCING
            else _state.value = CloudSyncStatus.SYNCED
            val recipes = RecipeDocument.decodeRecipes(payload)
            val canonical = RecipeDocument.encodeRecipes(recipes)
            if (canonical != lastRecipesPayload) {
                lastRecipesPayload = canonical
                worker.launch { repository.replaceCustomRecipes(recipes) }
            }
        }
    }

    private fun uploadLocalState(document: DocumentReference, worker: CoroutineScope) {
        worker.launch {
            repository.state
                .map(CloudDocumentCodec::encodeState)
                .distinctUntilChanged()
                .debounce(450)
                .collect { payload ->
                    if (payload != lastStatePayload) {
                        lastStatePayload = payload
                        _state.value = CloudSyncStatus.SYNCING
                        document.set(cloudValue(payload), SetOptions.merge())
                            .addOnFailureListener(::reportFailure)
                    }
                }
        }
    }

    private fun uploadLocalRecipes(document: DocumentReference, worker: CoroutineScope) {
        worker.launch {
            repository.customRecipes
                .map(RecipeDocument::encodeRecipes)
                .distinctUntilChanged()
                .debounce(450)
                .collect { payload ->
                    if (payload != lastRecipesPayload) {
                        lastRecipesPayload = payload
                        _state.value = CloudSyncStatus.SYNCING
                        document.set(cloudValue(payload), SetOptions.merge())
                            .addOnFailureListener(::reportFailure)
                    }
                }
        }
    }

    private fun cloudValue(payload: String): Map<String, Any> = mapOf(
        "schemaVersion" to CloudDocumentCodec.SCHEMA_VERSION,
        "payload" to payload,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    private fun reportFailure(error: Throwable) {
        _state.value = if ((error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            CloudSyncStatus.OFFLINE
        } else CloudSyncStatus.ERROR
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
