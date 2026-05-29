package org.openhab.matter.companion.setup

object MatterSetupBackNavigation {
    fun systemBackAction(state: MatterSetupUiState): MatterSetupAction? {
        return when (state.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking -> {
                MatterSetupAction.BackToMainMenu.takeIf {
                    MatterSetupAction.BackToMainMenu in state.secondaryActions
                }
            }

            else -> null
        }
    }
}
