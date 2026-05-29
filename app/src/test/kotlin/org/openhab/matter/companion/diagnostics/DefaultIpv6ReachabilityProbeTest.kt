package org.openhab.matter.companion.diagnostics

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
}
