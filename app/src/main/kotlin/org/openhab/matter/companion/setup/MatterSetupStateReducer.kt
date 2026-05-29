package org.openhab.matter.companion.setup

object MatterSetupStateReducer {
    fun reset(openHabConfigured: Boolean, openHabUrl: String): MatterSetupUiState {
        return if (openHabConfigured) {
            MatterSetupUiState.initial(openHabConfigured = true)
        } else {
            openHabSetup(openHabUrl)
        }
    }

    fun openHabSetup(openHabUrl: String): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = "Connect to your openHAB home before adding Matter devices.",
            primaryAction = MatterSetupAction.TestOpenHab,
            primaryActionLabel = "Test connection",
            primaryActionEnabled = openHabUrl.trim().isNotBlank()
        )
    }

    fun openHabSetupChecking(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.OpenHabSetupChecking,
            title = "Checking openHAB",
            message = "Checking the openHAB REST API and Matter controller.",
            primaryAction = MatterSetupAction.TestOpenHab,
            primaryActionLabel = "Checking...",
            primaryActionEnabled = false
        )
    }

    fun advancedTroubleshooting(current: MatterSetupUiState): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.AdvancedTroubleshooting,
            title = "Advanced troubleshooting",
            message = current.failure?.message ?: "Review setup diagnostics before trying again.",
            primaryAction = MatterSetupAction.Retry,
            primaryActionLabel = "Back to setup",
            failure = current.failure,
            diagnostics = current.diagnostics
        )
    }
}
