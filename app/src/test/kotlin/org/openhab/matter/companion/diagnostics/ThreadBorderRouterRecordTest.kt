package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadBorderRouterRecordTest {
    @Test
    fun displayNamePrefersInstanceName() {
        val record = ThreadBorderRouterRecord(
            instanceName = "OpenThread Border Router",
            host = "otbr.local",
            addresses = listOf("fd00::1"),
            port = 49191
        )

        assertEquals("OpenThread Border Router", record.displayName)
    }

    @Test
    fun endpointPrefersFirstAddressAndPort() {
        val record = ThreadBorderRouterRecord(
            instanceName = "",
            host = "otbr.local",
            addresses = listOf("fd00::1"),
            port = 49191
        )

        assertEquals("[fd00::1]:49191", record.endpoint)
    }

    @Test
    fun endpointFallsBackToHostWithoutPortWhenAddressMissing() {
        val record = ThreadBorderRouterRecord(
            instanceName = "",
            host = "otbr.local",
            addresses = emptyList(),
            port = 0
        )

        assertEquals("otbr.local", record.endpoint)
    }
}
