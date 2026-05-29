package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTransportSummaryTest {
    @Test
    fun wifiWithoutVpnPrefersLocalNetworkWithoutWarnings() {
        val summary = NetworkTransportSummary(wifi = true, cellular = false, vpn = false)

        assertTrue(summary.localNetworkPreferred)
        assertTrue(summary.warnings.isEmpty())
    }

    @Test
    fun cellularWithoutWifiWarnsToConnectToWifi() {
        val summary = NetworkTransportSummary(wifi = false, cellular = true, vpn = false)

        assertEquals(
            listOf("Connect to Wi-Fi before adding local Matter devices."),
            summary.warnings
        )
    }

    @Test
    fun noWifiWithoutCellularDoesNotWarn() {
        val summary = NetworkTransportSummary(wifi = false, cellular = false, vpn = false)

        assertTrue(summary.warnings.isEmpty())
    }

    @Test
    fun activeVpnWarnsAboutLocalRouting() {
        val summary = NetworkTransportSummary(wifi = true, cellular = false, vpn = true)

        assertEquals(
            listOf("VPN is active and may block openHAB, mDNS, or IPv6 routing."),
            summary.warnings
        )
    }
}
