package org.openhab.matter.companion.diagnostics

data class MatterMdnsRecord(
    val serviceType: String,
    val instanceName: String,
    val host: String,
    val ipv6Addresses: List<String>,
    val port: Int,
    val txt: Map<String, String>
) {
    val interpretationTitle: String =
        if (ipv6Addresses.isEmpty()) {
            "Phone found a Matter service"
        } else {
            "Phone can see the device"
        }

    companion object {
        const val phoneDiscoveryLimitation: String =
            "This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup."
    }
}
