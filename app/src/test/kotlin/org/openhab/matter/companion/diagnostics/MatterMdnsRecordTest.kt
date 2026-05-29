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

        assertEquals("Phone can see the device", record.interpretationTitle)
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

        assertEquals("Phone found a Matter service", record.interpretationTitle)
    }
}
