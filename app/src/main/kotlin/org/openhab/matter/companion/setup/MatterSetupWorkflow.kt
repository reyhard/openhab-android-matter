package org.openhab.matter.companion.setup

class MatterSetupWorkflow(
    private val ports: MatterSetupPorts,
    private val emit: (MatterSetupUiState) -> Unit
) {
    fun startAutomatedSetup(setupPayload: String) {
        var activeStage = MatterSetupStage.ReadyToScan
        var diagnosticsContext = emptyDiagnosticsContext()
        val redactor = SensitiveValueRedactor(setupPayload)
        var lastCommissioningStageError: String? = null
        val progressTracker = MatterCommissioningProgressTracker()
        var commissioningStartedAtMs = 0L
        var stageStartedAtMs = 0L
        val stageTimeline = mutableListOf<String>()
        var lastStageName: String? = null

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
            commissioningStartedAtMs = android.os.SystemClock.elapsedRealtime()
            stageStartedAtMs = commissioningStartedAtMs
            emit(MatterSetupUiState.progress(
                activeStage,
                commissioningStartedAtMs = commissioningStartedAtMs,
                commissioningStageStartedAtMs = stageStartedAtMs
            ))
            val commission = ports.commissionToPhone(setupPayload, config) { progressMessage ->
                val safeMessage = redactor.sanitize(progressMessage)
                val now = android.os.SystemClock.elapsedRealtime()
                val started = Regex(
                    """commissioning stage started:\s*([A-Za-z0-9_]+)""",
                    RegexOption.IGNORE_CASE
                ).find(safeMessage)
                if (started != null) {
                    val nextStage = started.groupValues[1]
                    if (lastStageName != nextStage) {
                        lastStageName?.let { previous ->
                            stageTimeline.add("$previous: ${now - stageStartedAtMs} ms")
                        }
                        lastStageName = nextStage
                        stageStartedAtMs = now
                    }
                }
                val detail = progressTracker.accept(safeMessage)
                if (detail != null) {
                    if (detail.startsWith("Matter stage ")) {
                        lastCommissioningStageError = detail
                    }
                    emit(MatterSetupUiState.progress(
                        activeStage,
                        activeDetail = detail,
                        commissioningStartedAtMs = commissioningStartedAtMs,
                        commissioningStageStartedAtMs = stageStartedAtMs
                    ))
                }
            }
            lastCommissioningStageError = null
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
            emit(MatterSetupUiState.progress(activeStage, scan.timeoutSeconds, deviceIdentity = windowIdentity))
            val discoveryContext = MatterDiscoveryContext(
                vendorName = commission.vendorName.ifBlank { window.vendorName },
                productName = commission.productName.ifBlank { window.productName },
                nodeId = commission.nodeId
            )
            val inbox = ports.waitForOpenHabInbox(config, scan.timeoutSeconds, discoveryContext)
            if (!inbox.matterEntryDetected) {
                fail(
                    activeStage,
                    inbox.failureMessage(),
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
                    message = inbox.successMessage(),
                    primaryAction = MatterSetupAction.AddAnotherDevice,
                    primaryActionLabel = "Add another device"
                )
            )
        }.onFailure { error ->
            val details = listOfNotNull(
                error.message?.takeIf { it.isNotBlank() },
                lastCommissioningStageError?.takeIf { activeStage == MatterSetupStage.CommissioningToPhone },
                if (activeStage == MatterSetupStage.CommissioningToPhone && commissioningStartedAtMs > 0L) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    (stageTimeline + listOfNotNull(lastStageName?.let {
                        "$it: ${now - stageStartedAtMs} ms (last)"
                    })).joinToString("; ", prefix = "Stage timeline: ")
                        .takeIf { stageTimeline.isNotEmpty() || lastStageName != null }
                } else null
            ).joinToString("; ")
            fail(activeStage, "Setup could not finish", details, diagnosticsContext, redactor)
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
/**
 * Converts native commissioning stage callbacks to monotonic, user-facing progress.
 *
 * The connectedhomeip diagnostic stream also contains asynchronous GATT callbacks.
 * Once a commissioning stage has started, transport diagnostics must not rewind
 * progress to "connecting over Bluetooth".
 */
private class MatterCommissioningProgressTracker {
    private enum class Phase(val order: Int) {
        Scan(0),
        Bluetooth(1),
        SecurePairing(2),
        Attestation(3),
        Credentials(4),
        Network(5),
        Operational(6),
        Complete(7)
    }

    private data class Presentation(val phase: Phase, val detail: String)

    private val stageStarted = Regex(
        """commissioning stage started:\s*([A-Za-z0-9_]+)""",
        RegexOption.IGNORE_CASE
    )
    private val stageFailed = Regex(
        """commissioning stage update:\s*([A-Za-z0-9_]+)\s+error\s+(\d+)""",
        RegexOption.IGNORE_CASE
    )
    private val stageSucceeded = Regex(
        """commissioning stage update:\s*([A-Za-z0-9_]+)\s+succeeded""",
        RegexOption.IGNORE_CASE
    )

    private var highestPhase = Phase.Scan
    private var sdkStageObserved = false
    private var stageFailureObserved = false
    private var currentDetail: String? = null

    fun accept(message: String): String? {
        val value = message.trim()
        if (value.isBlank()) return null

        // A failing SDK status is authoritative, even if it arrives after a later
        // generic BLE diagnostic. Keep it visible until the final result arrives.
        stageFailed.find(value)?.let { match ->
            val (stage, errorCode) = match.destructured
            stageFailureObserved = true
            val detail = "Matter stage $stage error $errorCode (awaiting final result)"
            currentDetail = detail
            return detail
        }
        if (stageFailureObserved) return null

        // "succeeded" acknowledges the preceding stage; it is not a new phase.
        if (stageSucceeded.containsMatchIn(value)) return null

        stageStarted.find(value)?.let { match ->
            sdkStageObserved = true
            val presentation = presentStage(match.groupValues[1])
            return advance(presentation)
        }

        // BLE scan and GATT diagnostics are lower-level transport messages.
        // They should help before SDK stage callbacks begin, never override them.
        if (sdkStageObserved) return null
        val presentation = when {
            value.contains("BLE scan", ignoreCase = true) ||
                value.contains("advertisement", ignoreCase = true) ||
                value.contains("discriminator", ignoreCase = true) ->
                Presentation(Phase.Scan, "Seeking Bluetooth device")

            value.contains("GATT", ignoreCase = true) ||
                value.contains("Matter services", ignoreCase = true) ||
                value.contains("MTU", ignoreCase = true) ->
                Presentation(Phase.Bluetooth, "Connecting over Bluetooth")

            value.contains("PASE", ignoreCase = true) ->
                Presentation(Phase.SecurePairing, "Establishing secure pairing")

            value.contains("attestation", ignoreCase = true) ->
                Presentation(Phase.Attestation, "Verifying device certificates")

            value.contains("Thread dataset", ignoreCase = true) ->
                Presentation(Phase.Network, "Sending Thread network credentials")

            value.contains("FindOperational", ignoreCase = true) ||
                value.contains("operational discovery", ignoreCase = true) ->
                Presentation(Phase.Operational, "Finding device on Thread network")

            else -> null
        }
        return presentation?.let(::advance)
    }

    private fun advance(presentation: Presentation): String? {
        if (presentation.phase.order < highestPhase.order) return null
        highestPhase = presentation.phase
        if (presentation.detail == currentDetail) return null
        currentDetail = presentation.detail
        return presentation.detail
    }

    private fun presentStage(stage: String): Presentation {
        val name = stage.lowercase()
        return when {
            name.contains("findoperational") || name.contains("casesession") ->
                Presentation(Phase.Operational, "Finding device on Thread network")

            name.contains("sendcomplete") || name.contains("cleanup") ||
                name.contains("icd") || name.contains("commissioningcomplete") ->
                Presentation(Phase.Complete, "Finishing device setup")

            name.contains("connectnetwork") || name.contains("enablenetwork") ->
                Presentation(Phase.Network, "Connecting device to Thread network")

            name.contains("network") || name.contains("thread") || name.contains("wifi") ->
                Presentation(Phase.Network, "Sending Thread network credentials")

            name.contains("noc") || name.contains("trustedroot") || name.contains("fabric") ->
                Presentation(Phase.Credentials, "Installing device credentials")

            name.contains("attest") || name.contains("certificate") ||
                name.contains("csr") || name.contains("sendpai") || name.contains("senddac") ->
                Presentation(Phase.Attestation, "Verifying device certificates")

            name.contains("securepair") || name.contains("pase") ||
                name.contains("failsafe") || name.contains("regulatory") ||
                name.contains("readcommissioning") ->
                Presentation(Phase.SecurePairing, "Establishing secure pairing")

            // Do not claim that an unrecognized SDK stage means Thread is joining.
            else -> Presentation(Phase.SecurePairing, "Commissioning device ($stage)")
        }
    }
}

