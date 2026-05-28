package org.openhab.matter.companion.setup

class MatterSetupWorkflow(
    private val ports: MatterSetupPorts,
    private val emit: (MatterSetupUiState) -> Unit
) {
    fun startAutomatedSetup(setupPayload: String) {
        val config = ports.loadConfig()
        if (!config.openHabConfigured) {
            emit(MatterSetupUiState.initial(openHabConfigured = false))
            return
        }

        runCatching {
            emit(MatterSetupUiState.progress(MatterSetupStage.ReadinessChecking))
            val readiness = ports.checkReadiness(config)
            if (!readiness.ready) {
                fail(
                    MatterSetupStage.ReadinessChecking,
                    "Setup is not ready yet",
                    readiness.warnings.joinToString("; "),
                    config
                )
                return
            }

            emit(MatterSetupUiState.progress(MatterSetupStage.CommissioningToPhone))
            val commission = ports.commissionToPhone(setupPayload, config)

            emit(MatterSetupUiState.progress(MatterSetupStage.OpeningCommissioningWindow))
            val window = ports.openCommissioningWindow(commission.nodeId, commission.controllerState)
            emit(MatterSetupUiState.progress(MatterSetupStage.CommissioningWindowOpen, window.timeoutSeconds))

            emit(MatterSetupUiState.progress(MatterSetupStage.SendingCodeToOpenHab, window.timeoutSeconds))
            val scan = ports.sendCodeToOpenHab(window.manualCode, config)
            if (!scan.started) {
                fail(MatterSetupStage.SendingCodeToOpenHab, "openHAB could not start pairing", scan.details, config)
                return
            }

            emit(MatterSetupUiState.progress(MatterSetupStage.WatchingOpenHabInbox, window.timeoutSeconds))
            val inbox = ports.waitForOpenHabInbox(config, scan.timeoutSeconds)
            if (!inbox.matterEntryDetected) {
                fail(
                    MatterSetupStage.WatchingOpenHabInbox,
                    "openHAB did not report the device yet",
                    inbox.details,
                    config
                )
                return
            }

            emit(
                MatterSetupUiState(
                    stage = MatterSetupStage.SuccessInboxDetected,
                    title = "Device found by openHAB",
                    message = "openHAB reported a Matter Inbox entry for this device.",
                    primaryAction = MatterSetupAction.AddAnotherDevice,
                    primaryActionLabel = "Add another device"
                )
            )
        }.onFailure { error ->
            fail(MatterSetupStage.Failed, "Setup could not finish", error.message.orEmpty(), config)
        }
    }

    private fun fail(stage: MatterSetupStage, message: String, details: String, config: MatterSetupConfig) {
        val failure = MatterSetupFailure(step = stage, message = message, details = details)
        emit(MatterSetupUiState.failed(failure, ports.runDiagnostics(failure, config)))
    }
}
