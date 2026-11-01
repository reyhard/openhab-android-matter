package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class MatterMdnsRecordTest {
    @Test
    fun phoneDiscoveryLimitationDescribesPhoneOnlyDiscovery() {
        assertEquals(
            "This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup.",
            MatterMdnsRecord.phoneDiscoveryLimitation
        )
    }

    @Test
    fun recordWithIpv6AddressShowsDeviceVisibleFromPhone() {
        val record = MatterMdnsRecord(
            serviceType = "_matterc._udp",
            instanceName = "Test Device",
            host = "test.local",
            ipv6Addresses = listOf("fd00::1234"),
            port = 5540,
            txt = mapOf("D" to "3840")
        )

        assertEquals("Matter service resolved to IPv6 (reachability not verified)", record.interpretationTitle)
    }

    @Test
    fun failedResolutionIsNotMistakenForReachableDevice() {
        val record = MatterMdnsRecord(
            serviceType = "_matterc._udp",
            instanceName = "Stale Device",
            host = "",
            ipv6Addresses = emptyList(),
            port = 0,
            txt = emptyMap(),
            resolutionFailure = "Android NSD error 3"
        )
        assertEquals("Matter service advertised but resolution failed", record.interpretationTitle)
    }

    @Test
    fun recordWithoutIpv6AddressShowsMatterServiceFound() {
        val record = MatterMdnsRecord(
            serviceType = "_matterc._udp",
            instanceName = "Test Device",
            host = "test.local",
            ipv6Addresses = emptyList(),
            port = 5540,
            txt = mapOf("D" to "3840")
        )

        assertEquals("Matter service resolved without an IPv6 address", record.interpretationTitle)
    }
}
