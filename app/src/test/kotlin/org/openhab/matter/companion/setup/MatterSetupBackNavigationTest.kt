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
    fun troubleshootingFromSettingsBackReturnsToSettings() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(MatterSetupStateReducer.settings())

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun manualCodeBackReturnsToMainScreen() {
        val state = MatterSetupStateReducer.manualCodeEntry()

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }
}
