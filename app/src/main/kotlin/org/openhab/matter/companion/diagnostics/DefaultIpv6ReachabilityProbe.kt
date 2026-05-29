package org.openhab.matter.companion.diagnostics

import java.net.Inet6Address
import java.net.InetAddress

class DefaultIpv6ReachabilityProbe : Ipv6ReachabilityProbe {
    private val addressResolver: (String) -> InetAddress

    constructor() : this(InetAddress::getByName)

    internal constructor(addressResolver: (String) -> InetAddress) {
        this.addressResolver = addressResolver
    }

    override fun isReachable(ipv6Address: String, timeoutMillis: Int): Boolean {
        val address = resolveIpv6Literal(ipv6Address, addressResolver) ?: return false

        return try {
            address.isReachable(timeoutMillis)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun isIpv6Literal(address: String): Boolean {
            return resolveIpv6Literal(address, InetAddress::getByName) != null
        }

        private fun resolveIpv6Literal(
            address: String,
            addressResolver: (String) -> InetAddress
        ): Inet6Address? {
            val literal = normalizeIpv6Literal(address) ?: return null

            return try {
                addressResolver(literal) as? Inet6Address
            } catch (e: Exception) {
                null
            }
        }

        private fun normalizeIpv6Literal(address: String): String? {
            val trimmedAddress = address.trim()
            if (trimmedAddress.isEmpty()) {
                return null
            }

            val unbracketedAddress = if (trimmedAddress.startsWith("[")) {
                if (!trimmedAddress.endsWith("]")) {
                    return null
                }
                trimmedAddress.substring(startIndex = 1, endIndex = trimmedAddress.lastIndex)
            } else {
                if (trimmedAddress.contains("[") || trimmedAddress.contains("]")) {
                    return null
                }
                trimmedAddress
            }

            if (!unbracketedAddress.contains(":")) {
                return null
            }

            val scopeStart = unbracketedAddress.indexOf('%')
            val addressPart = if (scopeStart >= 0) {
                unbracketedAddress.substring(startIndex = 0, endIndex = scopeStart)
            } else {
                unbracketedAddress
            }
            val scopePart = if (scopeStart >= 0) {
                unbracketedAddress.substring(startIndex = scopeStart + 1)
            } else {
                null
            }

            if (addressPart.isEmpty() || !addressPart.all(::isIpv6AddressPartCharacter)) {
                return null
            }
            if (scopePart != null && (scopePart.isEmpty() || !scopePart.all(::isIpv6ScopeCharacter))) {
                return null
            }

            return unbracketedAddress
        }

        private fun isIpv6AddressPartCharacter(character: Char): Boolean {
            return character in '0'..'9' ||
                character in 'a'..'f' ||
                character in 'A'..'F' ||
                character == ':' ||
                character == '.'
        }

        private fun isIpv6ScopeCharacter(character: Char): Boolean {
            return character in '0'..'9' ||
                character in 'a'..'z' ||
                character in 'A'..'Z' ||
                character == '_' ||
                character == '-' ||
                character == '.'
        }
    }
}
