package org.openhab.matter.companion.controller;

import static org.junit.Assert.assertEquals;

import chip.devicecontroller.ChipClusters;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectedHomeIpReflectionDeviceMetadataReaderTest {
    @Before
    public void setUp() {
        ChipClusters.reset();
    }

    @After
    public void tearDown() {
        ChipClusters.reset();
    }

    @Test
    public void readDeviceDetailsDecodesStableIpv6AddressFromGeneralDiagnostics() throws Exception {
        ChipClusters.networkInterfaces = Arrays.asList(new ChipClusters.NetworkInterfaceStruct(Arrays.asList(
                bytes(0xfe, 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
                bytes(0xfd, 0x88, 0x93, 0x26, 0x57, 0xd6, 0x00, 0x01, 0xaf, 0x67, 0x97, 0xfb, 0x40, 0x7a,
                        0xcb, 0xad))));
        ConnectedHomeIpReflectionDeviceMetadataReader reader = new ConnectedHomeIpReflectionDeviceMetadataReader(
                (controller, nodeId) -> new ConnectedHomeIpDevicePointer(123L, () -> {}));

        MatterDeviceDetails details = reader.readDeviceDetails(new Object(), 0x165BC267A7E344D0L);

        assertEquals("fd88:9326:57d6:0001:af67:97fb:407a:cbad", details.ipv6Address());
    }

    @Test
    public void readDeviceDetailsUsesClusterSpecificAttributeCallbacks() throws Exception {
        ChipClusters.batPercentRemaining = 153;
        ChipClusters.batQuantity = 2;
        ChipClusters.batReplacementDescription = "AAA";
        ChipClusters.threadNetworkName = "OpenThread";
        ChipClusters.threadChannel = 25;
        ConnectedHomeIpReflectionDeviceMetadataReader reader = new ConnectedHomeIpReflectionDeviceMetadataReader(
                (controller, nodeId) -> new ConnectedHomeIpDevicePointer(123L, () -> {}));

        MatterDeviceDetails details = reader.readDeviceDetails(new Object(), 0x165BC267A7E344D0L);

        assertEquals(Integer.valueOf(153), details.batteryPercentRemaining());
        assertEquals(Integer.valueOf(2), details.batteryQuantity());
        assertEquals("AAA", details.batteryDesignation());
        assertEquals("OpenThread", details.threadNetworkName());
        assertEquals(Integer.valueOf(25), details.threadChannel());
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
