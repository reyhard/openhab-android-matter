package org.openhab.matter.companion.setup

class MatterSetupWorkflow(
    private val ports: MatterSetupPorts,
    private val emit: (MatterSetupUiState) -> Unit
) {
    fun startAutomatedSetup(setupPayload: String) {
        var activeStage = MatterSetupStage.ReadyToScan
        var diagnosticsContext = emptyDiagnosticsContext()
        val redactor = SensitiveValueRedactor(setupPayload)

        runCatching {
            val config = ports.loadConfig()
            diagnosticsContext = config.toDiagnosticsContext()
            redactor.add(config.openHabApiToken)
            redactor.add(config.threadDataset)
            if (!config.openHabConfigured) {
                emit(MatterSetupUiState.initial(openHabConfigured = false))
                return
            }

            activeStage = MatterSetupStage.ReadinessChecking
            emit(MatterSetupUiState.progress(activeStage))
            val readiness = ports.checkReadiness(config)
            if (!readiness.ready) {
                fail(
                    activeStage,
                    "Setup is not ready yet",
                    readiness.failureDetails(),
                    diagnosticsContext,
                    redactor
                )
                return
            }

            activeStage = MatterSetupStage.CommissioningToPhone
            emit(MatterSetupUiState.progress(activeStage))
            val commission = ports.commissionToPhone(setupPayload, config)
            redactor.add(commission.controllerState)

            activeStage = MatterSetupStage.OpeningCommissioningWindow
            emit(MatterSetupUiState.progress(activeStage))
            val window = ports.openCommissioningWindow(commission.nodeId, commission.controllerState)
            redactor.add(window.manualCode)
            redactor.add(window.qrCode)
            redactor.add(window.controllerState)
            if (window.manualCode.isBlank()) {
                fail(
                    activeStage,
                    "OpenCommissioningWindow did not return a manual setup code",
                    "Blank manual setup code returned by Matter controller",
                    diagnosticsContext,
                    redactor
                )
                return
            }

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
                    diagnosticsContext,
                    redactor
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
                    diagnosticsContext,
                    redactor
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
            fail(activeStage, "Setup could not finish", error.message.orEmpty(), diagnosticsContext, redactor)
        }
    }

    private fun fail(
        stage: MatterSetupStage,
        message: String,
        details: String,
        diagnosticsContext: MatterSetupDiagnosticsContext,
        redactor: SensitiveValueRedactor
    ) {
        val sanitizedDetails = redactor.sanitize(details)
        val failure = MatterSetupFailure(step = stage, message = message, details = sanitizedDetails)
        val diagnostics = runCatching {
            ports.runDiagnostics(failure, diagnosticsContext).sanitize(redactor)
        }.getOrElse {
            MatterSetupDiagnosticsSummary.empty()
        }
        emit(MatterSetupUiState.failed(failure, diagnostics))
    }

    private fun MatterSetupPorts.ReadinessResult.failureDetails(): String {
        return (warnings + details)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("; ")
    }

    private fun MatterSetupConfig.toDiagnosticsContext(): MatterSetupDiagnosticsContext {
        return MatterSetupDiagnosticsContext(
            openHabBaseUrl = openHabBaseUrl,
            otbrBaseUrl = otbrBaseUrl,
            attestationBypassEnabled = attestationBypassEnabled
        )
    }

    private fun emptyDiagnosticsContext(): MatterSetupDiagnosticsContext {
        return MatterSetupDiagnosticsContext(
            openHabBaseUrl = "",
            otbrBaseUrl = "",
            attestationBypassEnabled = false
        )
    }

    private fun MatterSetupDiagnosticsSummary.sanitize(
        redactor: SensitiveValueRedactor
    ): MatterSetupDiagnosticsSummary {
        return copy(
            checks = checks.map(redactor::sanitize),
            warnings = warnings.map(redactor::sanitize),
            details = details.map(redactor::sanitize)
        )
    }

    private class SensitiveValueRedactor(vararg initialValues: String) {
        private val values = linkedSetOf<String>()

        init {
            initialValues.forEach(::add)
        }

        fun add(value: String) {
            if (value.isNotBlank()) {
                values.add(value)
            }
        }

        fun sanitize(text: String): String {
            return values.sortedByDescending { it.length }.fold(text) { sanitized, value ->
                sanitized.replace(value, "<redacted>")
            }
        }
    }
}
