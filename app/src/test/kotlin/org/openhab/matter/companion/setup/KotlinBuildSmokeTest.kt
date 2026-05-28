package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinBuildSmokeTest {
    @Test
    fun kotlinTestsRun() {
        assertEquals("openHAB", "open" + "HAB")
    }
}
