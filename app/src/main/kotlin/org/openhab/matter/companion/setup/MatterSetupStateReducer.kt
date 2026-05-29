package org.openhab.matter.companion.setup

object MatterSetupStateReducer {
    fun reset(openHabConfigured: Boolean, openHabUrl: String): MatterSetupUiState {
        return if (openHabConfigured) {
            MatterSetupUiState.initial(openHabConfigured = true)
        } else {
            openHabSetup(openHabUrl)
        }
    }

    fun editSettings(openHabUrl: String): MatterSetupUiState {
        return openHabSetup(openHabUrl, showBackToMainMenu = true)
    }

    fun phoneDeviceList(hasDevices: Boolean, message: String = ""): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.PhoneDeviceList,
            title = "Devices on this phone",
            message = message.ifBlank {
                if (hasDevices) {
                    "Matter devices staged by this app for openHAB handoff."
                } else {
                    "No staged Matter devices are stored on this phone."
                }
            },
            primaryAction = MatterSetupAction.BackToSettings,
            primaryActionLabel = "Back to settings",
            secondaryActions = listOf(MatterSetupAction.BackToMainMenu)
        )
    }

    fun openHabSetup(
        openHabUrl: String,
        showBackToMainMenu: Boolean = false
    ): MatterSetupUiState {
        val openHabUrlConfigured = openHabUrl.trim().isNotBlank()
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = "Connect to your openHAB home before adding Matter devices.",
            primaryAction = MatterSetupAction.TestOpenHab,
            primaryActionLabel = "Test connection",
            primaryActionEnabled = openHabUrlConfigured,
            secondaryActions = buildList {
                if (showBackToMainMenu) {
                    add(MatterSetupAction.BackToMainMenu)
                }
                add(MatterSetupAction.ShowTroubleshooting)
            }
        )
    }

    fun openHabSetupReady(openHabUrl: String): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = "openHAB is ready. You can go back and scan a Matter QR code.",
            primaryAction = MatterSetupAction.TestOpenHab,
            primaryActionLabel = "Test connection",
            primaryActionEnabled = openHabUrl.trim().isNotBlank(),
            secondaryActions = listOf(
                MatterSetupAction.BackToMainMenu,
                MatterSetupAction.ShowTroubleshooting
            )
        )
    }

    fun openHabSetupNotReady(
        openHabUrl: String,
        message: String,
        failure: MatterSetupFailure? = null,
        diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty(),
        showBackToMainMenu: Boolean = false
    ): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = message.ifBlank { "openHAB is not ready yet. Check the details in troubleshooting." },
            primaryAction = MatterSetupAction.TestOpenHab,
            primaryActionLabel = "Test connection",
            primaryActionEnabled = openHabUrl.trim().isNotBlank(),
            secondaryActions = buildList {
                if (showBackToMainMenu) {
                    add(MatterSetupAction.BackToMainMenu)
                }
                add(MatterSetupAction.ShowTroubleshooting)
            },
            failure = failure,
            diagnostics = diagnostics
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
        val primaryAction = when (current.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking -> MatterSetupAction.BackToSettings
            else -> MatterSetupAction.Retry
        }
        return MatterSetupUiState(
            stage = MatterSetupStage.AdvancedTroubleshooting,
            title = "Advanced troubleshooting",
            message = current.failure?.message ?: "Review setup diagnostics before trying again.",
            primaryAction = primaryAction,
            primaryActionLabel = if (primaryAction == MatterSetupAction.BackToSettings) {
                "Back to settings"
            } else {
                "Back to setup"
            },
            failure = current.failure,
            diagnostics = current.diagnostics
        )
    }
}
