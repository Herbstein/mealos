package com.mealos.model

enum class CloudSyncStatus {
    SIGNED_OUT,
    CONNECTING,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class MealOsAccountState(
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val signingIn: Boolean = false,
    val syncStatus: CloudSyncStatus = CloudSyncStatus.SIGNED_OUT,
    val message: String? = null
) {
    val signedIn: Boolean get() = uid != null
}
