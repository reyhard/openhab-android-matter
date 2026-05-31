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
            redactor.add(config.openHabBaseUrl, config.openHabBaseUrl.toLogSafeUrl())
            redactor.add(config.otbrBaseUrl, config.otbrBaseUrl.toLogSafeUrl())
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
            val commission = ports.commissionToPhone(setupPayload, config) { progressMessage ->
                val detail = commissioningProgressDetail(redactor.sanitize(progressMessage))
                if (detail != null) {
                    emit(MatterSetupUiState.progress(activeStage, activeDetail = detail))
                }
            }
            val commissionedIdentity = MatterSetupDeviceIdentity(commission.vendorName, commission.productName)
            redactor.add(commission.controllerState)

            activeStage = MatterSetupStage.OpeningCommissioningWindow
            emit(MatterSetupUiState.progress(activeStage))
            val window = ports.openCommissioningWindow(commission.nodeId, commission.controllerState)
            val windowIdentity = MatterSetupDeviceIdentity(window.vendorName, window.productName)
                .takeIf { it.visible() } ?: commissionedIdentity
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
            emit(MatterSetupUiState.progress(activeStage, window.timeoutSeconds, deviceIdentity = windowIdentity))

            activeStage = MatterSetupStage.SendingCodeToOpenHab
            emit(MatterSetupUiState.progress(activeStage, window.timeoutSeconds, deviceIdentity = windowIdentity))
            val baselineInbox = ports.readOpenHabInbox(config)
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
            val inbox = ports.waitForOpenHabInbox(config, scan.timeoutSeconds, baselineInbox.matterEntryIds)
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
            openHabBaseUrl = openHabBaseUrl.toLogSafeUrl(),
            otbrBaseUrl = otbrBaseUrl.toLogSafeUrl(),
            attestationBypassEnabled = attestationBypassEnabled
        )
    }

    private fun commissioningProgressDetail(message: String): String? {
        val value = message.trim()
        if (value.isBlank()) {
            return null
        }
        return when {
            value.contains("BLE scan", ignoreCase = true) ||
                value.contains("advertisement", ignoreCase = true) ||
                value.contains("discriminator", ignoreCase = true) -> "Seeking Bluetooth device"

            value.contains("GATT", ignoreCase = true) ||
                value.contains("Matter services", ignoreCase = true) ||
                value.contains("MTU", ignoreCase = true) -> "Sending setup data over BLE"

            value.contains("attestation", ignoreCase = true) ||
                value.contains("PASE", ignoreCase = true) ||
                value.contains("Thread dataset", ignoreCase = true) ||
                value.contains("commissioning stage started", ignoreCase = true) -> {
                if (value.contains("FindOperational", ignoreCase = true)) {
                    "Waiting for device on the Thread network"
                } else {
                    "Connecting device to Thread network"
                }
            }

            value.contains("FindOperational", ignoreCase = true) ||
                value.contains("operational", ignoreCase = true) ||
                value.contains("commissioning complete", ignoreCase = true) -> {
                "Waiting for device on the Thread network"
            }

            else -> value.take(120)
        }
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
        private val replacements = linkedMapOf<String, String>()

        init {
            initialValues.forEach(::add)
        }

        fun add(value: String) {
            add(value, "<redacted>")
        }

        fun add(value: String, replacement: String) {
            if (value.isNotBlank()) {
                replacements[value] = replacement
            }
        }

        fun sanitize(text: String): String {
            val urlRedacted = text.sanitizeLogUrls()
            return replacements.entries.sortedByDescending { it.key.length }.fold(urlRedacted) { sanitized, entry ->
                sanitized.replace(entry.key, entry.value)
            }
        }
    }
}
