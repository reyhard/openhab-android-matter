package org.openhab.matter.companion.setup

class MatterSetupWorkflow(
    private val ports: MatterSetupPorts,
    private val emit: (MatterSetupUiState) -> Unit
) {
    fun startAutomatedSetup(setupPayload: String) {
        val config = ports.loadConfig()
        var activeStage = MatterSetupStage.ReadyToScan
        var manualCode = ""
        if (!config.openHabConfigured) {
            emit(MatterSetupUiState.initial(openHabConfigured = false))
            return
        }

        runCatching {
            activeStage = MatterSetupStage.ReadinessChecking
            emit(MatterSetupUiState.progress(activeStage))
            val readiness = ports.checkReadiness(config)
            if (!readiness.ready) {
                fail(
                    activeStage,
                    "Setup is not ready yet",
                    readiness.warnings.joinToString("; "),
                    config,
                    manualCode,
                    setupPayload
                )
                return
            }

            activeStage = MatterSetupStage.CommissioningToPhone
            emit(MatterSetupUiState.progress(activeStage))
            val commission = ports.commissionToPhone(setupPayload, config)

            activeStage = MatterSetupStage.OpeningCommissioningWindow
            emit(MatterSetupUiState.progress(activeStage))
            val window = ports.openCommissioningWindow(commission.nodeId, commission.controllerState)
            manualCode = window.manualCode
            activeStage = MatterSetupStage.CommissioningWindowOpen
            emit(MatterSetupUiState.progress(activeStage, window.timeoutSeconds))

            activeStage = MatterSetupStage.SendingCodeToOpenHab
            emit(MatterSetupUiState.progress(activeStage, window.timeoutSeconds))
            val scan = ports.sendCodeToOpenHab(window.manualCode, config)
            if (!scan.started) {
                fail(
                    activeStage,
                    "openHAB could not start pairing",
                    scan.details,
                    config,
                    manualCode,
                    setupPayload
                )
                return
            }

            activeStage = MatterSetupStage.WatchingOpenHabInbox
            emit(MatterSetupUiState.progress(activeStage, scan.timeoutSeconds))
            val inbox = ports.waitForOpenHabInbox(config, scan.timeoutSeconds)
            if (!inbox.matterEntryDetected) {
                fail(
                    activeStage,
                    "openHAB did not report the device yet",
                    inbox.details,
                    config,
                    manualCode,
                    setupPayload
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
            fail(activeStage, "Setup could not finish", error.message.orEmpty(), config, manualCode, setupPayload)
        }
    }

    private fun fail(
        stage: MatterSetupStage,
        message: String,
        details: String,
        config: MatterSetupConfig,
        manualCode: String,
        setupPayload: String
    ) {
        val sanitizedDetails = sanitizeFailureDetails(details, config, manualCode, setupPayload)
        val failure = MatterSetupFailure(step = stage, message = message, details = sanitizedDetails)
        emit(MatterSetupUiState.failed(failure, ports.runDiagnostics(failure, config)))
    }

    private fun sanitizeFailureDetails(
        details: String,
        config: MatterSetupConfig,
        manualCode: String,
        setupPayload: String
    ): String {
        return details
            .redact(manualCode)
            .redact(config.openHabApiToken)
            .redact(config.threadDataset)
            .redact(setupPayload)
    }

    private fun String.redact(secret: String): String {
        return if (secret.isBlank()) {
            this
        } else {
            replace(secret, "<redacted>")
        }
    }
}
