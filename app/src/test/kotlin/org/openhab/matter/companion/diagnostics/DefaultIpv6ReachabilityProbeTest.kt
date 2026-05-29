package org.openhab.matter.companion.diagnostics

import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultIpv6ReachabilityProbeTest {
    @Test
    fun isIpv6LiteralRecognizesIpv6Address() {
        assertTrue(DefaultIpv6ReachabilityProbe.isIpv6Literal("fd00::1234"))
    }

    @Test
    fun isIpv6LiteralRejectsIpv4Address() {
        assertFalse(DefaultIpv6ReachabilityProbe.isIpv6Literal("192.168.1.10"))
    }

    @Test
    fun isIpv6LiteralRejectsHostname() {
        assertFalse(DefaultIpv6ReachabilityProbe.isIpv6Literal("openhab.local"))
    }

    @Test
    fun isIpv6LiteralRecognizesScopedIpv6Literal() {
        assertTrue(DefaultIpv6ReachabilityProbe.isIpv6Literal("fe80::1234%1"))
    }

    @Test
    fun isIpv6LiteralRecognizesBracketedUriLiteral() {
        assertTrue(DefaultIpv6ReachabilityProbe.isIpv6Literal("[fd00::1234]"))
    }

    @Test
    fun isIpv6LiteralRejectsMalformedColonString() {
        assertFalse(DefaultIpv6ReachabilityProbe.isIpv6Literal("openhab.local:5540"))
    }

    @Test
    fun isReachableRejectsNonLiteralWithoutResolving() {
        var resolverCalled = false
        val probe = DefaultIpv6ReachabilityProbe(
            addressResolver = {
                resolverCalled = true
                InetAddress.getByName(it)
            }
        )

        assertFalse(probe.isReachable("openhab.local:5540", timeoutMillis = 1))
        assertFalse(resolverCalled)
    }
}
