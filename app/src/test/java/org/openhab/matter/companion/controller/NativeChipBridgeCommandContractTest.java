package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class NativeChipBridgeCommandContractTest {
    @Test
    public void commissioningRequestExposesNativeCommandInputs() {
        NativeCommissioningRequest request = new NativeCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                true,
                "opaque-controller-state");

        assertEquals("0E080000000000010000", request.datasetHex());
        assertEquals(20202021L, request.setupPin());
        assertEquals(3840, request.discriminator());
        assertTrue(request.attestationBypassEnabled());
        assertEquals("opaque-controller-state", request.controllerState());
    }

    @Test
    public void commissioningResultExposesNodeIdAndControllerState() {
        NativeCommissioningResult result = new NativeCommissioningResult(1234L, "updated-controller-state");

        assertEquals(1234L, result.nodeId());
        assertEquals("updated-controller-state", result.controllerState());
    }

    @Test
    public void openCommissioningWindowRequestExposesNativeCommandInputs() {
        NativeOpenCommissioningWindowRequest request = new NativeOpenCommissioningWindowRequest(
                1234L,
                300,
                3840,
                "opaque-controller-state");

        assertEquals(1234L, request.nodeId());
        assertEquals(300, request.timeoutSeconds());
        assertEquals(3840, request.discriminator());
        assertEquals("opaque-controller-state", request.controllerState());
    }

    @Test
    public void openCommissioningWindowResultExposesTemporaryCodeAndControllerState() {
        NativeOpenCommissioningWindowResult result = new NativeOpenCommissioningWindowResult(
                "3497-3840-123",
                "updated-controller-state");

        assertEquals("3497-3840-123", result.temporaryCode());
        assertEquals("updated-controller-state", result.controllerState());
    }

    @Test
    public void openCommissioningWindowResultRejectsBlankTemporaryCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new NativeOpenCommissioningWindowResult("", "updated-controller-state"));
    }

    @Test
    public void bridgeUsesRequestAndResultObjectsForStatefulCommands() {
        NativeChipBridge bridge = new NativeChipBridge() {
            @Override
            public void load(String libraryName) {
            }

            @Override
            public String metadata() {
                return "kind=connectedhomeip;version=2026.05;production=true";
            }

            @Override
            public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
                return new NativeCommissioningResult(1234L, request.controllerState());
            }

            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(
                    NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("3497-3840-123", request.controllerState());
            }
        };

        NativeCommissioningResult commissioningResult = bridge.commissionBleThread(new NativeCommissioningRequest(
                "0E080000000000010000",
                20202021L,
                3840,
                true,
                "commissioning-state"));
        NativeOpenCommissioningWindowResult windowResult = bridge.openCommissioningWindow(
                new NativeOpenCommissioningWindowRequest(1234L, 300, 3840, "window-state"));

        assertEquals(1234L, commissioningResult.nodeId());
        assertEquals("commissioning-state", commissioningResult.controllerState());
        assertEquals("3497-3840-123", windowResult.temporaryCode());
        assertEquals("window-state", windowResult.controllerState());
    }
}
