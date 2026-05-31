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

    @Test
    fun stagedDeviceUsesProductAndVendorFallbacksSeparately() {
        val device = PhoneMatterDevice(
            nodeId = 0x4D2,
            controllerStateStored = false,
            stateReadable = true
        )

        assertEquals("Unknown Matter device", device.displayProductName)
        assertEquals("Unknown vendor", device.displayVendorName)
        assertEquals("0x4D2", device.displayNodeId)
        assertEquals("missing", device.displayControllerState)
        assertEquals("yes", device.displayStateReadable)
    }

    @Test
    fun fetchedDetailsMergeDoesNotClearExistingValues() {
        val original = PhoneMatterDeviceDetails(
            deviceName = "BILRESA scroll wheel",
            vendor = "IKEA of Sweden",
            product = "BILRESA scroll wheel",
            nodeId = "0x165BC267A7E344D0",
            firmwareVersion = "1.8.7"
        )

        val merged = original.merge(PhoneMatterDeviceDetails(hardwareVersion = "P2.0"))

        assertEquals("1.8.7", merged.firmwareVersion)
        assertEquals("P2.0", merged.hardwareVersion)
        assertEquals("BILRESA scroll wheel", merged.deviceName)
    }
}
