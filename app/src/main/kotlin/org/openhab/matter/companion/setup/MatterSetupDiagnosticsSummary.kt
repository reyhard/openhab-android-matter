package org.openhab.matter.companion.setup

data class MatterSetupDiagnosticsSummary(
    val checks: List<String>,
    val warnings: List<String>,
    val details: List<String>
) {
    companion object {
        fun empty(): MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary(
            checks = emptyList(),
            warnings = emptyList(),
            details = emptyList()
        )
    }
}
