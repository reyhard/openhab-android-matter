package org.openhab.matter.companion.diagnostics

data class ThreadBorderRouterRecord(
    val instanceName: String,
    val host: String,
    val addresses: List<String>,
    val port: Int
) {
    val displayName: String
        get() = instanceName.ifBlank { host.ifBlank { "Thread Border Router" } }

    val endpoint: String
        get() {
            val target = addresses.firstOrNull().orEmpty().ifBlank { host }
            if (target.isBlank()) {
                return "Address unavailable"
            }
            val formattedTarget = if (target.contains(":") && !target.startsWith("[")) {
                "[$target]"
            } else {
                target
            }
            return if (port > 0) {
                "$formattedTarget:$port"
            } else {
                formattedTarget
            }
        }
}

interface ThreadBorderRouterBrowser {
    fun browse(timeoutMillis: Long): List<ThreadBorderRouterRecord>
}
