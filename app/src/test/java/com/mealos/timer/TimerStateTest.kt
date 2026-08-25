package com.mealos.timer

import com.mealos.model.CookingTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    @Test
    fun pausedTimerKeepsItsSavedTime() {
        val timer = timer(running = false, endAt = null, remaining = 90_000)

        assertSame(timer, timer.refreshedAt(nowEpochMs = 50_000))
    }

    @Test
    fun runningTimerUsesItsAbsoluteFinishTimeAfterProcessRestart() {
        val timer = timer(running = true, endAt = 180_000, remaining = 120_000)

        val refreshed = timer.refreshedAt(nowEpochMs = 150_000)

        assertTrue(refreshed.running)
        assertEquals(30_000L, refreshed.remainingMs)
        assertEquals(180_000L, refreshed.endAtEpochMs)
    }

    @Test
    fun elapsedTimerBecomesFinishedInsteadOfRestarting() {
        val timer = timer(running = true, endAt = 180_000, remaining = 30_000)

        val refreshed = timer.refreshedAt(nowEpochMs = 200_000)

        assertFalse(refreshed.running)
        assertEquals(0L, refreshed.remainingMs)
        assertNull(refreshed.endAtEpochMs)
    }

    private fun timer(running: Boolean, endAt: Long?, remaining: Long) = CookingTimer(
        id = "timer-1",
        label = "Chicken",
        durationMs = 120_000,
        endAtEpochMs = endAt,
        remainingMs = remaining,
        running = running
    )
}
