package org.openhab.matter.companion.diagnostics

interface Ipv6ReachabilityProbe {
    fun isReachable(ipv6Address: String, timeoutMillis: Int = 2_000): Boolean
}
