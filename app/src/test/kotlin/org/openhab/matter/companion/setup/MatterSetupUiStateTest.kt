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
        assertEquals(5, state.steps.size)
        assertEquals("Checking setup", state.steps[0].label)
        assertEquals("Opening pairing window", state.steps[2].label)
        assertEquals(MatterSetupStepStatus.Active, state.steps[2].status)
        assertEquals("Preparing the device for openHAB.", state.steps[2].detail)
        assertEquals(300, state.countdownSeconds)
    }

    @Test
    fun commissioningToPhoneStateHighlightsSinglePhoneCommissioningStep() {
        val state = MatterSetupUiState.progress(
            MatterSetupStage.CommissioningToPhone,
            activeDetail = "Seeking Bluetooth device"
        )

        assertEquals("Adding device to this phone", state.steps[1].label)
        assertEquals(MatterSetupStepStatus.Active, state.steps[1].status)
        assertEquals("Seeking Bluetooth device", state.activeDetail)
        assertEquals("Seeking Bluetooth device", state.steps[1].detail)
        assertFalse(state.steps.any { it.label == "Connecting to device" })
    }

    @Test
    fun commissioningWindowOpenStateMarksPairingWindowStepComplete() {
        val state = MatterSetupUiState.progress(MatterSetupStage.CommissioningWindowOpen, countdownSeconds = 300)

        assertEquals("Opening pairing window", state.steps[2].label)
        assertEquals(MatterSetupStepStatus.Complete, state.steps[2].status)
        assertFalse(state.steps.any { it.status == MatterSetupStepStatus.Active })
        assertEquals(300, state.countdownSeconds)
    }

    @Test
    fun nonProgressStageDoesNotActivateFirstProgressStep() {
        val state = MatterSetupUiState.progress(MatterSetupStage.ReadyToScan)

        assertEquals("Checking setup", state.steps[0].label)
        assertEquals(MatterSetupStepStatus.Pending, state.steps[0].status)
        assertFalse(state.steps.any { it.status == MatterSetupStepStatus.Active })
    }

    @Test
    fun failureStateUsesSanitizedFailureFieldsAsProvided() {
        val failure = MatterSetupFailure(
            step = MatterSetupStage.SendingCodeToOpenHab,
            message = "openHAB could not start pairing",
            details = "HTTP 401 for redacted request"
        )

        val state = MatterSetupUiState.failed(failure, MatterSetupDiagnosticsSummary.empty())

        assertEquals(MatterSetupStage.Failed, state.stage)
        assertEquals("openHAB could not start pairing", state.message)
        assertEquals("HTTP 401 for redacted request", state.failure?.details)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }
}
