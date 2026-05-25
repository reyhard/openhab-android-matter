package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ChipMatterControllerCommandContractTest {
    @Test
    public void commissionBleThreadMapsControllerRequestAndResult() throws Exception {
        CapturingNativeChipBridge bridge = new CapturingNativeChipBridge();
        ChipMatterController controller = new ChipMatterController(bridge, new ChipMatterControllerConfig(
                "custom_chip",
                true));
        ThreadDataset dataset = ThreadDataset.parse("hex:0e080000000000010000");
        MatterSetupPayload payload = new MatterSetupPayload(
                "pin=20202021;disc=3840",
                20202021L,
                3840,
                "Aqara",
                "U200",
                false);

        MatterCommissioningResult result = controller.commissionBleThread(
                dataset,
                payload,
                "incoming-controller-state",
                ignored -> { });

        assertEquals("0E080000000000010000", bridge.commissioningRequest.datasetHex());
        assertEquals(20202021L, bridge.commissioningRequest.setupPin());
        assertEquals(3840, bridge.commissioningRequest.discriminator());
        assertTrue(bridge.commissioningRequest.attestationBypassEnabled());
        assertEquals("incoming-controller-state", bridge.commissioningRequest.controllerState());
        assertEquals(987654321L, result.nodeId());
        assertEquals("updated-controller-state", result.controllerState());
    }

    @Test
    public void openCommissioningWindowMapsControllerRequestAndResult() throws Exception {
        CapturingNativeChipBridge bridge = new CapturingNativeChipBridge();
        ChipMatterController controller = new ChipMatterController(bridge, ChipMatterControllerConfig.defaultConfig());

        MatterOpenCommissioningWindowResult result = controller.openCommissioningWindow(
                987654321L,
                300,
                3840,
                "incoming-controller-state",
                ignored -> { });

        assertEquals(987654321L, bridge.openCommissioningWindowRequest.nodeId());
        assertEquals(300, bridge.openCommissioningWindowRequest.timeoutSeconds());
        assertEquals(3840, bridge.openCommissioningWindowRequest.discriminator());
        assertEquals("incoming-controller-state", bridge.openCommissioningWindowRequest.controllerState());
        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("ocw-updated-controller-state", result.controllerState());
    }

    @Test
    public void openCommissioningWindowRejectsBlankTemporaryCodeFromBridge() {
        NativeChipBridge bridge = new CapturingNativeChipBridge() {
            @Override
            public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
                return new NativeOpenCommissioningWindowResult("", "ocw-updated-controller-state");
            }
        };
        ChipMatterController controller = new ChipMatterController(bridge, ChipMatterControllerConfig.defaultConfig());

        assertThrows(IllegalArgumentException.class, () -> controller.openCommissioningWindow(
                987654321L,
                300,
                3840,
                "incoming-controller-state",
                ignored -> { }));
    }

    private static class CapturingNativeChipBridge implements NativeChipBridge {
        private NativeCommissioningRequest commissioningRequest;
        private NativeOpenCommissioningWindowRequest openCommissioningWindowRequest;

        @Override
        public void load(String libraryName) {
        }

        @Override
        public String metadata() {
            return "kind=connectedhomeip;version=2026.05;production=true;message=connectedhomeip controller ready";
        }

        @Override
        public NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request) {
            commissioningRequest = request;
            return new NativeCommissioningResult(987654321L, "updated-controller-state");
        }

        @Override
        public NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request) {
            openCommissioningWindowRequest = request;
            return new NativeOpenCommissioningWindowResult("3497-0112-332", "ocw-updated-controller-state");
        }
    }
}
