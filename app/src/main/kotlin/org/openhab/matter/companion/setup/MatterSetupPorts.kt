package org.openhab.matter.companion.setup

interface MatterSetupPorts {
    fun loadConfig(): MatterSetupConfig

    fun checkReadiness(config: MatterSetupConfig): ReadinessResult

    fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): CommissionResult

    fun openCommissioningWindow(nodeId: Long, controllerState: String): OpenWindowResult

    fun sendCodeToOpenHab(manualCode: String, config: MatterSetupConfig): OpenHabScanResult

    fun waitForOpenHabInbox(config: MatterSetupConfig, timeoutSeconds: Int): InboxResult

    fun runDiagnostics(failure: MatterSetupFailure, config: MatterSetupConfig): MatterSetupDiagnosticsSummary

    data class ReadinessResult(
        val ready: Boolean,
        val details: List<String>,
        val warnings: List<String> = emptyList()
    )

    data class CommissionResult(
        val nodeId: Long,
        val controllerState: String
    ) {
        override fun toString(): String {
            return "CommissionResult(nodeId=$nodeId, controllerState=<redacted>)"
        }
    }

    data class OpenWindowResult(
        val manualCode: String,
        val qrCode: String,
        val controllerState: String,
        val timeoutSeconds: Int
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
        val details: String
    ) {
        override fun toString(): String {
            return "InboxResult(" +
                "matterEntryDetected=$matterEntryDetected, " +
                "details=<redacted>" +
                ")"
        }
    }
}
