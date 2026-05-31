package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class MatterDeviceDetailFormatterTest {
    @Test
    fun batteryHalfPercentFormatsWithTypeWhenPresent() {
        assertEquals("52% · 2×AAA", MatterDeviceDetailFormatter.battery(104, 2, "AAA"))
        assertEquals("52%", MatterDeviceDetailFormatter.battery(104, null, ""))
        assertEquals("Unknown", MatterDeviceDetailFormatter.battery(null, 2, "AAA"))
    }

    @Test
    fun batteryOddHalfPercentFormatsWithDecimal() {
        assertEquals("51.5%", MatterDeviceDetailFormatter.battery(103, null, ""))
        assertEquals("51.5% · 1×CR2032", MatterDeviceDetailFormatter.battery(103, 1, "CR2032"))
        assertEquals("52%", MatterDeviceDetailFormatter.battery(104, null, ""))
    }

    @Test
    fun threadAndOtaFormattersUseStableFallbacks() {
        assertEquals("OpenThread · Channel 25", MatterDeviceDetailFormatter.threadNetwork("OpenThread", 25))
        assertEquals("OpenThread", MatterDeviceDetailFormatter.threadNetwork("OpenThread", null))
        assertEquals("Channel 25", MatterDeviceDetailFormatter.threadNetwork("", 25))
        assertEquals("Unknown", MatterDeviceDetailFormatter.threadNetwork("", null))
        assertEquals("Possible", MatterDeviceDetailFormatter.otaUpdate(true))
        assertEquals("Not available", MatterDeviceDetailFormatter.otaUpdate(false))
        assertEquals("Unknown", MatterDeviceDetailFormatter.otaUpdate(null))
    }
}
