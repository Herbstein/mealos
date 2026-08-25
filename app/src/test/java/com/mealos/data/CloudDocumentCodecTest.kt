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
            plan = listOf(PlanEntry(2, "chicken-parm", PortionMode.DINNER_PLUS_LUNCH)),
            checkedGroceries = setOf("mozzarella")
        )

        assertEquals(state, CloudDocumentCodec.decodeState(CloudDocumentCodec.encodeState(state)))
    }

    @Test
    fun rejectsUnknownCloudSchema() {
        assertNull(CloudDocumentCodec.decodeState("""{"schemaVersion":99}"""))
    }
}
