package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupStateReducerTest {
    @Test
    fun openHabSetupActionIsEnabledWhenUrlHasNonBlankText() {
        val state = MatterSetupStateReducer.requiredSetup("  http://openhab.local:8080  ")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals(MatterSetupAction.TestSettings, state.primaryAction)
        assertTrue(state.primaryActionEnabled)
    }

    @Test
    fun requiredSetupUsesDefaultUrlWhenUrlIsBlank() {
        val state = MatterSetupStateReducer.requiredSetup("   ")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertTrue(state.primaryActionEnabled)
        assertEquals("http://openhab:8080", state.openHabUrlFallback)
    }

    @Test
    fun resetReturnsWelcomeWhenConfigurationIsIncomplete() {
        val state = MatterSetupStateReducer.reset(
            setupComplete = false,
            openHabUrl = "http://openhab:8080"
        )

        assertEquals(MatterSetupStage.Welcome, state.stage)
        assertEquals("Set up Matter with openHAB", state.title)
        assertEquals(MatterSetupAction.GetStarted, state.primaryAction)
        assertEquals("Get started", state.primaryActionLabel)
    }

    @Test
    fun resetReturnsReadyToScanWhenConfigurationIsComplete() {
        val state = MatterSetupStateReducer.reset(
            setupComplete = true,
            openHabUrl = "http://openhab.local:8080"
        )

        assertEquals(MatterSetupStage.ReadyToScan, state.stage)
    }

    @Test
    fun getStartedReturnsRequiredSetupWithDefaultOpenHabAddress() {
        val state = MatterSetupStateReducer.requiredSetup("")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals("Connect to openHAB", state.title)
        assertEquals(MatterSetupAction.TestSettings, state.primaryAction)
        assertEquals("Continue", state.primaryActionLabel)
        assertEquals("http://openhab:8080", state.openHabUrlFallback)
        assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
        assertFalse(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun settingsStateUsesDedicatedSettingsStage() {
        val state = MatterSetupStateReducer.settings()

        assertEquals(MatterSetupStage.Settings, state.stage)
        assertEquals("Settings", state.title)
        assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun openHabConnectionEditorUsesOneScreenForAddressTokenAndTesting() {
        val state = MatterSetupStateReducer.openHabAddressEditor()

        assertEquals(MatterSetupStage.OpenHabAddressEditor, state.stage)
        assertEquals("openHAB connection", state.title)
        assertEquals(MatterSetupAction.SaveOpenHabAddress, state.primaryAction)
        assertEquals("Save", state.primaryActionLabel)
    }

    @Test
    fun openHabSetupReadyDoesNotExposeTroubleshootingBeforeValidationFails() {
        val state = MatterSetupStateReducer.requiredSetup("http://openhab.local:8080")

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals(MatterSetupAction.TestSettings, state.primaryAction)
        assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
        assertFalse(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun failedOpenHabSetupIncludesTroubleshootingAction() {
        val state = MatterSetupStateReducer.openHabSetupNotReady(
            openHabUrl = "http://openhab.local:8080",
            message = "Settings are not ready yet."
        )

        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun phoneDeviceListDoesNotExposeBackToMainMenu() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true)

        assertEquals(MatterSetupStage.PhoneDeviceList, state.stage)
        assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun emptyPhoneDeviceListExplainsThereAreNoStoredDevices() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = false)

        assertEquals("No staged Matter devices are stored on this phone.", state.message)
    }

    @Test
    fun phoneDeviceListCanReturnToRequiredSetup() {
        val state = MatterSetupStateReducer.phoneDeviceList(
            hasDevices = false,
            returnAction = MatterSetupAction.BackToRequiredSetup
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, state.primaryAction)
    }

    @Test
    fun phoneDeviceDetailsUsesInitialDeviceDetails() {
        val device = PhoneMatterDevice(
            nodeId = 0x4D2,
            controllerStateStored = true,
            stateReadable = true,
            vendorName = "Aqara",
            productName = "U200"
        )

        val state = MatterSetupStateReducer.phoneDeviceDetails(
            device = device,
            fetching = true,
            message = "Reading device details"
        )

        assertEquals(MatterSetupStage.PhoneDeviceDetails, state.stage)
        assertEquals("Device details", state.title)
        assertEquals("U200", state.phoneDeviceDetails.deviceName)
        assertEquals("Aqara", state.phoneDeviceDetails.vendor)
        assertEquals("U200", state.phoneDeviceDetails.product)
        assertEquals("0x4D2", state.phoneDeviceDetails.nodeId)
        assertTrue(state.phoneDeviceDetailsFetching)
        assertEquals("Reading device details", state.phoneDeviceDetailsMessage)
        assertEquals(MatterSetupAction.BackToSettings, state.primaryAction)
    }

    @Test
    fun phoneDeviceDetailsPreservesReturnActionFromPhoneDeviceList() {
        val state = MatterSetupStateReducer.phoneDeviceDetails(
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true
            ),
            returnAction = MatterSetupAction.BackToRequiredSetup
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, state.primaryAction)
        assertEquals("Back to setup", state.primaryActionLabel)
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
            MatterSetupStateReducer.settings()
        )

        assertEquals(MatterSetupAction.BackToSettings, state.primaryAction)
        assertEquals("Back to settings", state.primaryActionLabel)
    }

    @Test
    fun advancedTroubleshootingFromRequiredSetupReturnsToRequiredSetup() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(
            MatterSetupStateReducer.requiredSetup("http://openhab:8080")
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, state.primaryAction)
        assertEquals("Back to setup", state.primaryActionLabel)
    }
}
