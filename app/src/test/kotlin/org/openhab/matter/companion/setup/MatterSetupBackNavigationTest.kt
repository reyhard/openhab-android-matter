package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatterSetupBackNavigationTest {
    @Test
    fun welcomeBackIsNotIntercepted() {
        val state = MatterSetupStateReducer.reset(
            setupComplete = false,
            openHabUrl = "http://openhab:8080"
        )

        assertNull(MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun settingsBackReturnsToMainScreen() {
        val state = MatterSetupStateReducer.settings()

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun threadEditorBackReturnsToSettings() {
        val state = MatterSetupStateReducer.threadNetworkEditor()

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun tokenEditorBackReturnsToSettings() {
        val state = MatterSetupStateReducer.changeToken()

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun phoneDevicesBackReturnsToSettings() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true)

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun phoneDevicesFromRequiredSetupTroubleshootingBackReturnsToRequiredSetup() {
        val state = MatterSetupStateReducer.phoneDeviceList(
            hasDevices = false,
            returnAction = MatterSetupAction.BackToRequiredSetup
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun troubleshootingFromSettingsBackReturnsToSettings() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(MatterSetupStateReducer.settings())

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun troubleshootingFromRequiredSetupBackReturnsToRequiredSetup() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(
            MatterSetupStateReducer.requiredSetup("http://openhab:8080")
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun manualCodeBackReturnsToMainScreen() {
        val state = MatterSetupStateReducer.manualCodeEntry()

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun scanningQrBackReturnsToMainScreen() {
        val state = MatterSetupUiState(
            stage = MatterSetupStage.ScanningQr,
            title = "Scan Matter QR code",
            message = "Point the camera at the Matter QR code."
        )

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun pairingModeBackReturnsToMainScreen() {
        val state = MatterSetupUiState(
            stage = MatterSetupStage.NeedsPairingMode,
            title = "Put device in pairing mode",
            message = "Make sure the Matter device is ready."
        )

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun progressBackIsNotIntercepted() {
        val state = MatterSetupUiState.progress(MatterSetupStage.ReadinessChecking)

        assertNull(MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun failureBackIsNotIntercepted() {
        val state = MatterSetupUiState.failed(
            MatterSetupFailure(
                step = MatterSetupStage.ReadinessChecking,
                message = "Setup failed"
            ),
            MatterSetupDiagnosticsSummary.empty()
        )

        assertNull(MatterSetupBackNavigation.systemBackAction(state))
    }
}
