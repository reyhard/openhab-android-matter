package org.openhab.matter.companion.controller;

import org.junit.Test;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ConnectedHomeIpMatterControllerTest {
    @Test
    public void commissionBleThreadMapsRequestAndResult() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                true);
        List<String> steps = new ArrayList<>();

        MatterCommissioningResult result = controller.commissionBleThread(
                ThreadDataset.parse("hex:0e080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                "controller-state",
                step -> steps.add(step.message()));

        assertEquals("0E080000000000010000", gateway.commissioningRequest.datasetHex());
        assertEquals(20202021L, gateway.commissioningRequest.setupPin());
        assertEquals(3840, gateway.commissioningRequest.discriminator());
        assertTrue(gateway.commissioningRequest.attestationBypassEnabled());
        assertEquals("controller-state", gateway.commissioningRequest.controllerState());
        assertEquals(987654321L, result.nodeId());
        assertEquals("updated-state", result.controllerState());
        assertEquals("Starting connectedhomeip Java BLE Thread commissioning", steps.get(0));
        assertEquals("connectedhomeip Java BLE Thread commissioning complete for node 987654321", steps.get(1));
    }

    @Test
    public void openCommissioningWindowMapsRequestAndResult() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                false);
        List<String> steps = new ArrayList<>();

        MatterOpenCommissioningWindowResult result = controller.openCommissioningWindow(
                987654321L,
                300,
                3840,
                "controller-state",
                step -> steps.add(step.message()));

        assertEquals(987654321L, gateway.openCommissioningWindowRequest.nodeId());
        assertEquals(300, gateway.openCommissioningWindowRequest.timeoutSeconds());
        assertEquals(1000L, gateway.openCommissioningWindowRequest.iteration());
        assertEquals(3840, gateway.openCommissioningWindowRequest.discriminator());
        assertEquals("controller-state", gateway.openCommissioningWindowRequest.controllerState());
        assertEquals("3497-0112-332", result.temporaryCode());
        assertEquals("ocw-state", result.controllerState());
        assertEquals("Opening connectedhomeip Java commissioning window", steps.get(0));
        assertEquals("connectedhomeip Java commissioning window opened", steps.get(1));
    }

    @Test
    public void commandsRejectMissingConnectedHomeIpArtifactsBeforeCallingGateway() {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                missingArtifacts(),
                gateway,
                false);

        IllegalStateException commissioning = assertThrows(IllegalStateException.class,
                () -> controller.commissionBleThread(
                        ThreadDataset.parse("hex:0e080000000000010000"),
                        new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
                        "controller-state",
                        ignored -> { }));
        IllegalStateException ocw = assertThrows(IllegalStateException.class,
                () -> controller.openCommissioningWindow(
                        987654321L,
                        300,
                        3840,
                        "controller-state",
                        ignored -> { }));

        assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController",
                commissioning.getMessage());
        assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController",
                ocw.getMessage());
        assertEquals(0, gateway.callCount);
    }

    private static ConnectedHomeIpControllerArtifacts readyArtifacts() {
        return new ConnectedHomeIpControllerArtifacts(name -> true, name -> { });
    }

    private static ConnectedHomeIpControllerArtifacts missingArtifacts() {
        return new ConnectedHomeIpControllerArtifacts(
                name -> !"chip.devicecontroller.ChipDeviceController".equals(name),
                name -> { });
    }

    private static final class CapturingGateway implements ConnectedHomeIpControllerGateway {
        private ConnectedHomeIpCommissioningRequest commissioningRequest;
        private ConnectedHomeIpOpenCommissioningWindowRequest openCommissioningWindowRequest;
        private int callCount;

        @Override
        public MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) {
            callCount++;
            commissioningRequest = request;
            return new MatterCommissioningResult(987654321L, "updated-state");
        }

        @Override
        public MatterOpenCommissioningWindowResult openCommissioningWindow(
                ConnectedHomeIpOpenCommissioningWindowRequest request) {
            callCount++;
            openCommissioningWindowRequest = request;
            return new MatterOpenCommissioningWindowResult("3497-0112-332", "ocw-state");
        }
    }
}
