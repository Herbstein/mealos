package com.mealos.data

import com.mealos.model.MealOsUserState
import com.mealos.model.PlanEntry
import com.mealos.model.PortionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudDocumentCodecTest {
    @Test
    fun stateRoundTripsWithoutLosingMealPlanningData() {
        val state = MealOsUserState(
            favorites = setOf("chicken-parm"),
            ratings = mapOf("chicken-parm" to 5),
            notes = mapOf("chicken-parm" to "More sauce"),
            pantry = setOf("salt", "olive-oil"),
            plan = listOf(PlanEntry(20732, "chicken-parm", PortionMode.DINNER_PLUS_LUNCH), PlanEntry(20739, "chicken-parm", PortionMode.ONE)),
            checkedGroceries = setOf("mozzarella")
        )

        assertEquals(state, CloudDocumentCodec.decodeState(CloudDocumentCodec.encodeState(state)))
    }

    @Test
    fun migratesLegacyWeekAndPreservesDatesOnSubsequentReads() {
        val legacy = """{"schemaVersion":1,"plan":[{"day":0,"recipe":"pizza","portion":"TWO"},{"day":6,"recipe":"pizza","portion":"ONE"}]}"""
        val migrated = CloudDocumentCodec.decodeState(legacy)!!
        val monday = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay().toInt()
        assertEquals(listOf(monday, monday + 6), migrated.plan.map { it.dayIndex })
        assertEquals(migrated, CloudDocumentCodec.decodeState(CloudDocumentCodec.encodeState(migrated)))
    }

    @Test
    fun rejectsUnknownCloudSchema() {
        assertNull(CloudDocumentCodec.decodeState("""{"schemaVersion":99}"""))
    }
}
