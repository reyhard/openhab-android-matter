package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupUiStateTest {
    @Test
    fun initialStateRequiresOpenHabSetupWhenNotConfigured() {
        val state = MatterSetupUiState.initial(openHabConfigured = false)

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals("Connect to openHAB", state.title)
        assertFalse(state.primaryActionEnabled)
        assertNull(state.countdownSeconds)
    }

    @Test
    fun progressStateContainsStableStepLabels() {
        val state = MatterSetupUiState.progress(MatterSetupStage.OpeningCommissioningWindow, countdownSeconds = 300)

        assertEquals("Setting up your device", state.title)
        assertEquals(6, state.steps.size)
        assertEquals("Checking setup", state.steps[0].label)
        assertEquals("Opening pairing window", state.steps[3].label)
        assertEquals(MatterSetupStepStatus.Active, state.steps[3].status)
        assertEquals(300, state.countdownSeconds)
    }

    @Test
    fun failureStateDoesNotExposeSensitiveValues() {
        val failure = MatterSetupFailure(
            step = MatterSetupStage.SendingCodeToOpenHab,
            message = "openHAB could not start pairing",
            details = "HTTP 401 for redacted request"
        )

        val state = MatterSetupUiState.failed(failure, MatterSetupDiagnosticsSummary.empty())

        assertEquals(MatterSetupStage.Failed, state.stage)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
        assertFalse(state.toString().contains("34970112332"))
        assertFalse(state.toString().contains("ohab_"))
    }
}
