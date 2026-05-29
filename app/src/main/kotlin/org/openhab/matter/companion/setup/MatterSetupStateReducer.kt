package org.openhab.matter.companion.setup

object MatterSetupStateReducer {
    fun reset(setupComplete: Boolean, openHabUrl: String): MatterSetupUiState {
        return if (setupComplete) {
            MatterSetupUiState.addMatterDevice()
        } else {
            MatterSetupUiState.welcome()
        }
    }

    fun requiredSetup(openHabUrl: String): MatterSetupUiState {
        val effectiveUrl = openHabUrl.ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = "Connect to your openHAB home and Thread network before adding Matter devices.",
            primaryAction = MatterSetupAction.TestSettings,
            primaryActionLabel = "Test settings",
            primaryActionEnabled = effectiveUrl.trim().isNotBlank(),
            secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting),
            openHabUrlFallback = effectiveUrl
        )
    }

    fun settings(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.Settings,
            title = "Settings",
            message = "Manage openHAB, Thread network, and local phone staging for Matter setup."
        )
    }

    fun changeToken(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ChangeToken,
            title = "Change token",
            message = "Enter a new openHAB access token and test it before saving.",
            primaryAction = MatterSetupAction.SaveChangedToken,
            primaryActionLabel = "Save token"
        )
    }

    fun changeTokenChecking(): MatterSetupUiState {
        return changeToken().copy(
            message = "Checking openHAB access token.",
            primaryActionLabel = "Checking...",
            primaryActionEnabled = false
        )
    }

    fun threadNetworkEditor(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ThreadNetworkEditor,
            title = "Thread network",
            message = "Used to commission Matter devices to your Thread network before handing them to openHAB.",
            primaryAction = MatterSetupAction.SaveThreadSettings,
            primaryActionLabel = "Save"
        )
    }

    fun manualCodeEntry(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.EnteringManualCode,
            title = "Enter setup code",
            message = "Type the 11-digit Matter setup code printed on the device or box.",
            primaryAction = MatterSetupAction.SubmitManualCode,
            primaryActionLabel = "Continue"
        )
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
            primaryActionLabel = "Back to settings"
        )
    }

    fun openHabSetupChecking(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.OpenHabSetupChecking,
            title = "Checking settings",
            message = "Checking openHAB, Thread dataset, and Border Router settings.",
            primaryAction = MatterSetupAction.TestSettings,
            primaryActionLabel = "Checking...",
            primaryActionEnabled = false
        )
    }

    fun openHabSetupNotReady(
        openHabUrl: String,
        message: String,
        failure: MatterSetupFailure? = null,
        diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty()
    ): MatterSetupUiState {
        return requiredSetup(openHabUrl).copy(
            message = message.ifBlank { "Settings are not ready yet. Check the details in troubleshooting." },
            failure = failure,
            diagnostics = diagnostics
        )
    }

    fun advancedTroubleshooting(current: MatterSetupUiState): MatterSetupUiState {
        val primaryAction = when (current.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking,
            MatterSetupStage.Settings,
            MatterSetupStage.ChangeToken,
            MatterSetupStage.ThreadNetworkEditor -> MatterSetupAction.BackToSettings
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
