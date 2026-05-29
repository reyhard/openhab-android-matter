package org.openhab.matter.companion.diagnostics

import java.net.Inet6Address
import java.net.InetAddress

class DefaultIpv6ReachabilityProbe : Ipv6ReachabilityProbe {
    override fun isReachable(ipv6Address: String, timeoutMillis: Int): Boolean {
        if (!isIpv6Literal(ipv6Address)) {
            return false
        }

        return try {
            InetAddress.getByName(ipv6Address).isReachable(timeoutMillis)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun isIpv6Literal(address: String): Boolean {
            if (!address.contains(":")) {
                return false
            }

            return try {
                InetAddress.getByName(address) is Inet6Address
            } catch (e: Exception) {
                false
            }
        }
    }
}
