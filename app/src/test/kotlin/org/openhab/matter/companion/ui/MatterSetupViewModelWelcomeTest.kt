package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStage
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MatterSetupViewModelWelcomeTest {
    @Test
    fun getStartedMovesFromWelcomeToRequiredSetup() {
        val viewModel = MatterSetupViewModel(
            application = RuntimeEnvironment.getApplication(),
            initialize = false
        )

        assertEquals(MatterSetupStage.Welcome, viewModel.uiState.stage)

        viewModel.handleAction(MatterSetupAction.GetStarted)

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, viewModel.uiState.stage)
        assertEquals("Connect to openHAB", viewModel.uiState.title)
    }
}
