package com.mealos.timer

import com.mealos.model.CookingTimer

internal fun CookingTimer.refreshedAt(nowEpochMs: Long): CookingTimer {
    val endAt = endAtEpochMs
    if (!running || endAt == null) return this
    val remaining = (endAt - nowEpochMs).coerceAtLeast(0)
    return if (remaining == 0L) {
        copy(running = false, endAtEpochMs = null, remainingMs = 0)
    } else {
        copy(remainingMs = remaining)
    }
}
