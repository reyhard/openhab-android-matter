package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatterSetupBackNavigationTest {
    @Test
    fun settingsBackReturnsToMainMenuWhenSettingsWasOpenedFromMainMenu() {
        val state = MatterSetupStateReducer.editSettings("http://openhab.local:8080")

        assertEquals(
            MatterSetupAction.BackToMainMenu,
            MatterSetupBackNavigation.systemBackAction(state)
        )
    }

    @Test
    fun firstRunSettingsBackIsNotIntercepted() {
        val state = MatterSetupUiState.initial(openHabConfigured = false)

        assertNull(MatterSetupBackNavigation.systemBackAction(state))
    }
}
