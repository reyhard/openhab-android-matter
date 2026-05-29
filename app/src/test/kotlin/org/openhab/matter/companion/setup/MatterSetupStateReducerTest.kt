package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupStateReducerTest {
    @Test
    fun openHabSetupActionIsEnabledWhenUrlHasNonBlankText() {
        val state = MatterSetupStateReducer.openHabSetup("  http://openhab.local:8080  ")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals(MatterSetupAction.TestOpenHab, state.primaryAction)
        assertTrue(state.primaryActionEnabled)
    }

    @Test
    fun openHabSetupActionIsDisabledWhenUrlIsBlank() {
        val state = MatterSetupStateReducer.openHabSetup("   ")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertFalse(state.primaryActionEnabled)
    }

    @Test
    fun resetReturnsOpenHabSetupWhenOpenHabIsNotConfigured() {
        val state = MatterSetupStateReducer.reset(
            openHabConfigured = false,
            openHabUrl = "http://openhab.local:8080"
        )

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals(MatterSetupAction.TestOpenHab, state.primaryAction)
        assertTrue(state.primaryActionEnabled)
    }

    @Test
    fun resetReturnsReadyToScanWhenOpenHabIsConfigured() {
        val state = MatterSetupStateReducer.reset(
            openHabConfigured = true,
            openHabUrl = "http://openhab.local:8080"
        )

        assertEquals(MatterSetupStage.ReadyToScan, state.stage)
    }

    @Test
    fun editSettingsReturnsOpenHabSetupWithCurrentUrlPreserved() {
        val state = MatterSetupStateReducer.editSettings("http://openhab.local:8080")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals(MatterSetupAction.TestOpenHab, state.primaryAction)
        assertTrue(state.primaryActionEnabled)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun firstRunOpenHabSetupDoesNotExposeBackToMainMenu() {
        val state = MatterSetupStateReducer.openHabSetup("http://openhab.local:8080")

        assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun openHabSetupReadyKeepsUserInSettingsWithBackAndTroubleshootingActions() {
        val state = MatterSetupStateReducer.openHabSetupReady("http://openhab.local:8080")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals("openHAB is ready. You can go back and scan a Matter QR code.", state.message)
        assertEquals(MatterSetupAction.TestOpenHab, state.primaryAction)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun openHabSetupIncludesTroubleshootingAction() {
        val state = MatterSetupStateReducer.openHabSetup("http://openhab.local:8080")

        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun phoneDeviceListUsesDedicatedStageAndBackActions() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true)

        assertEquals(MatterSetupStage.PhoneDeviceList, state.stage)
        assertEquals(MatterSetupAction.BackToSettings, state.primaryAction)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun emptyPhoneDeviceListExplainsThereAreNoStoredDevices() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = false)

        assertEquals("No staged Matter devices are stored on this phone.", state.message)
    }

    @Test
    fun advancedTroubleshootingPreservesFailureAndDiagnostics() {
        val failure = MatterSetupFailure(
            step = MatterSetupStage.WatchingOpenHabInbox,
            message = "openHAB did not report the device yet",
            details = "mDNS record was stale"
        )
        val diagnostics = MatterSetupDiagnosticsSummary(
            checks = listOf("openHAB URL configured=true"),
            warnings = listOf("inbox timeout"),
            details = listOf("Avahi still advertised old address")
        )
        val failedState = MatterSetupUiState.failed(failure, diagnostics)

        val state = MatterSetupStateReducer.advancedTroubleshooting(failedState)

        assertEquals(MatterSetupStage.AdvancedTroubleshooting, state.stage)
        assertSame(failure, state.failure)
        assertSame(diagnostics, state.diagnostics)
    }

    @Test
    fun advancedTroubleshootingFromSettingsReturnsToSettings() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(
            MatterSetupStateReducer.editSettings("http://openhab.local:8080")
        )

        assertEquals(MatterSetupAction.BackToSettings, state.primaryAction)
        assertEquals("Back to settings", state.primaryActionLabel)
    }
}
