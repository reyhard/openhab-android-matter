package org.openhab.matter.companion.setup

import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus
import org.openhab.matter.companion.openhab.OpenHabStatus

class AndroidMatterSetupPorts(
    private val configLoader: () -> MatterSetupConfig,
    private val readinessChecker: (String, String) -> OpenHabStatus,
    private val matterRunner: MatterRunner,
    private val discoveryScan: (String, String, String) -> OpenHabMatterDiscoveryScanStatus,
    private val inboxWaiter: (String, String, Int) -> MatterSetupPorts.InboxResult,
    private val diagnosticsRunner: (MatterSetupFailure, MatterSetupDiagnosticsContext) -> MatterSetupDiagnosticsSummary
) : MatterSetupPorts {
    interface MatterRunner {
        fun commissionToPhone(
            setupPayload: String,
            config: MatterSetupConfig
        ): MatterSetupPorts.CommissionResult

        fun openCommissioningWindow(
            nodeId: Long,
            controllerState: String
        ): MatterSetupPorts.OpenWindowResult
    }

    override fun loadConfig(): MatterSetupConfig {
        return configLoader()
    }

    override fun checkReadiness(config: MatterSetupConfig): MatterSetupPorts.ReadinessResult {
        val status = readinessChecker(config.openHabBaseUrl, config.openHabApiToken)
        val ready = status.online() && status.restReachable() && status.matterControllerReady()
        val sanitizer = DetailSanitizer(config)
        val details = listOf(
            "openHAB REST reachable=${status.restReachable()}",
            "openHAB Matter controller ready=${status.matterControllerReady()}",
            status.message().orEmpty(),
            status.details().orEmpty()
        ).sanitizeWith(sanitizer)
        val warnings = buildList {
            if (!status.restReachable()) {
                add("openHAB REST API is not reachable")
            }
            if (!status.matterControllerReady()) {
                add(status.message().orEmpty().ifBlank { "openHAB Matter controller is not ready" })
            }
        }.sanitizeWith(sanitizer)

        return MatterSetupPorts.ReadinessResult(
            ready = ready,
            details = details,
            warnings = warnings
        )
    }

    override fun commissionToPhone(
        setupPayload: String,
        config: MatterSetupConfig
    ): MatterSetupPorts.CommissionResult {
        return matterRunner.commissionToPhone(setupPayload, config)
    }

    override fun openCommissioningWindow(
        nodeId: Long,
        controllerState: String
    ): MatterSetupPorts.OpenWindowResult {
        return matterRunner.openCommissioningWindow(nodeId, controllerState)
    }

    override fun sendCodeToOpenHab(
        manualCode: String,
        config: MatterSetupConfig
    ): MatterSetupPorts.OpenHabScanResult {
        val status = discoveryScan(config.openHabBaseUrl, manualCode, config.openHabApiToken)
        val sanitizer = DetailSanitizer(config, manualCode)
        return MatterSetupPorts.OpenHabScanResult(
            started = status.started(),
            timeoutSeconds = status.timeoutSeconds(),
            details = listOf(status.message().orEmpty(), status.details().orEmpty())
                .sanitizeWith(sanitizer)
                .joinToString(": ")
        )
    }

    override fun waitForOpenHabInbox(
        config: MatterSetupConfig,
        timeoutSeconds: Int
    ): MatterSetupPorts.InboxResult {
        return inboxWaiter(config.openHabBaseUrl, config.openHabApiToken, timeoutSeconds)
    }

    override fun runDiagnostics(
        failure: MatterSetupFailure,
        context: MatterSetupDiagnosticsContext
    ): MatterSetupDiagnosticsSummary {
        return diagnosticsRunner(failure, context)
    }

    private fun List<String>.sanitizeWith(sanitizer: DetailSanitizer): List<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(sanitizer::sanitize)
            .distinct()
    }

    private class DetailSanitizer(
        config: MatterSetupConfig,
        manualCode: String = ""
    ) {
        private val replacements = linkedMapOf<String, String>()

        init {
            add(config.openHabBaseUrl, config.openHabBaseUrl.toLogSafeUrl())
            add(config.otbrBaseUrl, config.otbrBaseUrl.toLogSafeUrl())
            add(config.openHabApiToken)
            add(config.threadDataset)
            add(manualCode)
        }

        fun sanitize(value: String): String {
            return replacements.entries
                .sortedByDescending { it.key.length }
                .fold(value.sanitizeLogUrls()) { sanitized, entry ->
                    sanitized.replace(entry.key, entry.value)
                }
        }

        private fun add(value: String, replacement: String = "<redacted>") {
            if (value.isNotBlank()) {
                replacements[value] = replacement
            }
        }
    }
}
