package org.openhab.matter.companion.diagnostics

data class MatterMdnsRecord(
    val serviceType: String,
    val instanceName: String,
    val host: String,
    val ipv6Addresses: List<String>,
    val port: Int,
    val txt: Map<String, String>,
    val resolutionFailure: String? = null
) {
    val interpretationTitle: String = when {
        resolutionFailure != null -> "Matter service advertised but resolution failed"
        ipv6Addresses.isEmpty() -> "Matter service resolved without an IPv6 address"
        else -> "Matter service resolved to IPv6 (reachability not verified)"
    }

    companion object {
        const val phoneDiscoveryLimitation: String =
            "This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup."
    }
}
