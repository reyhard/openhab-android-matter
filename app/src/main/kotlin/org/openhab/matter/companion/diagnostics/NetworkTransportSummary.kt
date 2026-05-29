package org.openhab.matter.companion.diagnostics

data class NetworkTransportSummary(
    val wifi: Boolean,
    val cellular: Boolean,
    val vpn: Boolean
) {
    val localNetworkPreferred: Boolean = wifi && !vpn
    val warnings: List<String> = buildList {
        if (!wifi) {
            add("Connect to Wi-Fi before adding local Matter devices.")
        }
        if (vpn) {
            add("VPN is active and may block openHAB, mDNS, or IPv6 routing.")
        }
    }
}
