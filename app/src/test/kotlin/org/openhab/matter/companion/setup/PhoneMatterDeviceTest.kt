package org.openhab.matter.companion.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.controller.MatterBootstrapState

class PhoneMatterDeviceTest {
    @Test
    fun readableNodeCanAttemptDebugPairingWindowWithoutControllerState() {
        val device = PhoneMatterDevice(
            nodeId = 1234L,
            controllerStateStored = false,
            stateReadable = true
        )

        assertFalse(device.canOpenCommissioningWindow)
        assertTrue(device.canAttemptCommissioningWindowForDebug)
    }

    @Test
    fun unreadableStateCannotAttemptDebugPairingWindow() {
        val device = PhoneMatterDevice(
            nodeId = 1234L,
            controllerStateStored = false,
            stateReadable = false
        )

        assertFalse(device.canAttemptCommissioningWindowForDebug)
    }

    @Test
    fun fromBootstrapStateIncludesVendorAndProduct() {
        val device = PhoneMatterDevice.fromBootstrapState(
            MatterBootstrapState(1234L, "controller-state", false, "Aqara", "U200")
        )

        assertEquals("Aqara", device?.vendorName)
        assertEquals("U200", device?.productName)
        assertEquals("Aqara U200", device?.displayName)
    }
}
