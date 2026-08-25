package com.mealos.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseIdTokenProvider {
    suspend fun get(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Sign in with Google to use Recipe Assistant.")
        return user.getIdToken(false).awaitToken().token
            ?: throw IllegalStateException("Google sign-in did not return an access token. Sign in again.")
    }
}

private suspend fun <T> Task<T>.awaitToken(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
