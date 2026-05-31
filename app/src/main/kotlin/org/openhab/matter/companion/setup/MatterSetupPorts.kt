package org.openhab.matter.companion.setup

interface MatterSetupPorts {
    fun loadConfig(): MatterSetupConfig

    fun checkReadiness(config: MatterSetupConfig): ReadinessResult

    fun commissionToPhone(
        setupPayload: String,
        config: MatterSetupConfig,
        progress: (String) -> Unit
    ): CommissionResult

    fun openCommissioningWindow(nodeId: Long, controllerState: String): OpenWindowResult

    fun sendCodeToOpenHab(manualCode: String, config: MatterSetupConfig): OpenHabScanResult

    fun readOpenHabInbox(config: MatterSetupConfig): InboxResult {
        return InboxResult(
            matterEntryDetected = false,
            details = "openHAB Inbox baseline was not checked",
            matterEntryIds = emptySet()
        )
    }

    fun waitForOpenHabInbox(config: MatterSetupConfig, timeoutSeconds: Int): InboxResult

    fun waitForOpenHabInbox(
        config: MatterSetupConfig,
        timeoutSeconds: Int,
        baselineMatterEntryIds: Set<String>
    ): InboxResult {
        val result = waitForOpenHabInbox(config, timeoutSeconds)
        if (baselineMatterEntryIds.isEmpty()) {
            return result
        }
        val newEntryIds = result.matterEntryIds - baselineMatterEntryIds
        return result.copy(
            matterEntryDetected = newEntryIds.isNotEmpty(),
            details = if (newEntryIds.isNotEmpty()) {
                result.details
            } else {
                listOf(result.details, "Only pre-existing Matter Inbox entries were detected.")
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }
        )
    }

    fun runDiagnostics(
        failure: MatterSetupFailure,
        context: MatterSetupDiagnosticsContext
    ): MatterSetupDiagnosticsSummary

    data class ReadinessResult(
        val ready: Boolean,
        val details: List<String>,
        val warnings: List<String> = emptyList()
    ) {
        override fun toString(): String {
            return "ReadinessResult(" +
                "ready=$ready, " +
                "details=<redacted>, " +
                "warnings=<redacted>" +
                ")"
        }
    }

    data class CommissionResult(
        val nodeId: Long,
        val controllerState: String,
        val vendorName: String = "",
        val productName: String = ""
    ) {
        override fun toString(): String {
            return "CommissionResult(nodeId=$nodeId, controllerState=<redacted>)"
        }
    }

    data class OpenWindowResult(
        val manualCode: String,
        val qrCode: String,
        val controllerState: String,
        val timeoutSeconds: Int,
        val vendorName: String = "",
        val productName: String = ""
    ) {
        override fun toString(): String {
            return "OpenWindowResult(" +
                "manualCode=<redacted>, " +
                "qrCode=<redacted>, " +
                "controllerState=<redacted>, " +
                "timeoutSeconds=$timeoutSeconds" +
                ")"
        }
    }

    data class OpenHabScanResult(
        val started: Boolean,
        val timeoutSeconds: Int,
        val details: String
    ) {
        override fun toString(): String {
            return "OpenHabScanResult(" +
                "started=$started, " +
                "timeoutSeconds=$timeoutSeconds, " +
                "details=<redacted>" +
                ")"
        }
    }

    data class InboxResult(
        val matterEntryDetected: Boolean,
        val details: String,
        val matterEntryIds: Set<String> = emptySet()
    ) {
        override fun toString(): String {
            return "InboxResult(" +
                "matterEntryDetected=$matterEntryDetected, " +
                "details=<redacted>" +
                ")"
        }
    }
}

data class MatterSetupDiagnosticsContext(
    val openHabBaseUrl: String,
    val otbrBaseUrl: String,
    val attestationBypassEnabled: Boolean
) {
    override fun toString(): String {
        return "MatterSetupDiagnosticsContext(" +
            "openHabBaseUrl=${openHabBaseUrl.toLogSafeUrl()}, " +
            "otbrBaseUrl=${otbrBaseUrl.toLogSafeUrl()}, " +
            "attestationBypassEnabled=$attestationBypassEnabled" +
            ")"
    }
}
