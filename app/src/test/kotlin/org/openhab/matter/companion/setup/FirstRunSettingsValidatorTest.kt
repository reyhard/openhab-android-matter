package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.otbr.OtbrStatus

class FirstRunSettingsValidatorTest {
    @Test
    fun validInputsPass() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertTrue(result.ready)
        assertEquals(emptyList<String>(), result.warnings)
    }

    @Test
    fun missingTokenFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("openHAB access token is required"))
    }

    @Test
    fun invalidDatasetFailsWithoutEchoingInput() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "not-a-dataset",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("Thread dataset is not valid."))
        assertFalse(result.details.any { it.contains("not-a-dataset") })
    }

    @Test
    fun openHabMatterControllerFailureFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(false, true, false, "Matter controller offline", "thing offline"),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("Matter controller offline"))
    }

    @Test
    fun invalidOtbrFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "bad host",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(false, "OTBR address is invalid", "bad host")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("OTBR address is invalid"))
    }
}
