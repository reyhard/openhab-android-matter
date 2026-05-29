package org.openhab.matter.companion.diagnostics

data class ReadinessDiagnostic(
    val name: String,
    val ok: Boolean,
    val message: String
)
