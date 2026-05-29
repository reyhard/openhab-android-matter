package org.openhab.matter.companion.setup

data class MatterSetupUiState(
    val stage: MatterSetupStage,
    val title: String,
    val message: String,
    val steps: List<MatterSetupStep> = emptyList(),
    val activeDetail: String? = null,
    val countdownSeconds: Int? = null,
    val deviceIdentity: MatterSetupDeviceIdentity? = null,
    val primaryAction: MatterSetupAction? = null,
    val primaryActionLabel: String = "",
    val primaryActionEnabled: Boolean = true,
    val secondaryActions: List<MatterSetupAction> = emptyList(),
    val failure: MatterSetupFailure? = null,
    val diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty(),
    val openHabUrlFallback: String = ""
) {
    companion object {
        fun initial(openHabConfigured: Boolean): MatterSetupUiState {
            return if (openHabConfigured) {
                addMatterDevice()
            } else {
                welcome()
            }
        }

        fun welcome(): MatterSetupUiState {
            return MatterSetupUiState(
                stage = MatterSetupStage.Welcome,
                title = "Set up Matter with openHAB",
                message = "A guided local setup for adding Matter devices to your openHAB home.",
                primaryAction = MatterSetupAction.GetStarted,
                primaryActionLabel = "Get started"
            )
        }

        fun addMatterDevice(): MatterSetupUiState {
            return MatterSetupUiState(
                stage = MatterSetupStage.ReadyToScan,
                title = "Add Matter device",
                message = "Scan the device QR code or enter the setup code manually.",
                primaryAction = MatterSetupAction.StartScan,
                primaryActionLabel = "Scan code",
                secondaryActions = listOf(MatterSetupAction.EditSettings)
            )
        }

        fun progress(
            stage: MatterSetupStage,
            countdownSeconds: Int? = null,
            activeDetail: String? = null,
            deviceIdentity: MatterSetupDeviceIdentity? = null
        ): MatterSetupUiState {
            return MatterSetupUiState(
                stage = stage,
                title = "Setting up your device",
                message = progressMessage(stage),
                steps = progressSteps(stage, activeDetail.orEmpty()),
                activeDetail = activeDetail?.takeIf { it.isNotBlank() },
                countdownSeconds = countdownSeconds,
                deviceIdentity = deviceIdentity?.takeIf { it.visible() },
                secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting)
            )
        }

        fun failed(
            failure: MatterSetupFailure,
            diagnostics: MatterSetupDiagnosticsSummary
        ): MatterSetupUiState {
            return MatterSetupUiState(
                stage = MatterSetupStage.Failed,
                title = "Setup needs attention",
                message = failure.message,
                failure = failure,
                diagnostics = diagnostics,
                primaryAction = MatterSetupAction.Retry,
                primaryActionLabel = "Try again",
                secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting)
            )
        }

        private fun progressMessage(stage: MatterSetupStage): String = when (stage) {
            MatterSetupStage.ReadinessChecking -> "Checking your phone, network, and openHAB."
            MatterSetupStage.CommissioningToPhone -> "Adding the device to this phone first."
            MatterSetupStage.OpeningCommissioningWindow -> "Preparing the device for openHAB."
            MatterSetupStage.CommissioningWindowOpen -> "The device is ready for openHAB."
            MatterSetupStage.SendingCodeToOpenHab -> "Sending the setup code to openHAB."
            MatterSetupStage.WatchingOpenHabInbox -> "Waiting for openHAB to find the device."
            else -> "This usually takes less than a minute."
        }

        private fun progressSteps(activeStage: MatterSetupStage, activeDetail: String): List<MatterSetupStep> {
            val stages = listOf(
                MatterSetupStage.ReadinessChecking to "Checking setup",
                MatterSetupStage.CommissioningToPhone to "Adding device to this phone",
                MatterSetupStage.OpeningCommissioningWindow to "Opening pairing window",
                MatterSetupStage.SendingCodeToOpenHab to "Sending setup code to openHAB",
                MatterSetupStage.WatchingOpenHabInbox to "Waiting for openHAB"
            )
            val activeIndex = when (activeStage) {
                MatterSetupStage.CommissioningWindowOpen -> null
                else -> stages.indexOfFirst { it.first == activeStage }.takeIf { it >= 0 }
            }
            val completeThroughIndex = when (activeStage) {
                MatterSetupStage.CommissioningWindowOpen -> stages.indexOfFirst {
                    it.first == MatterSetupStage.OpeningCommissioningWindow
                }
                else -> activeIndex?.minus(1) ?: -1
            }
            return stages.mapIndexed { index, (_, label) ->
                val status = when {
                    index <= completeThroughIndex -> MatterSetupStepStatus.Complete
                    activeIndex == index -> MatterSetupStepStatus.Active
                    else -> MatterSetupStepStatus.Pending
                }
                MatterSetupStep(
                    label = label,
                    status = status,
                    detail = if (status == MatterSetupStepStatus.Active) {
                        activeDetail.ifBlank { progressMessage(activeStage) }
                    } else {
                        ""
                    }
                )
            }
        }
    }
}
