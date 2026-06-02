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
    public void commissionBleThreadForwardsNativeDiagnosticsToProgressListener() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        gateway.commissioningDiagnostic = "Matter BLE scan round 1 of 3 for discriminator 1740";
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                true);
        List<String> steps = new ArrayList<>();

        controller.commissionBleThread(
                ThreadDataset.parse("hex:0e080000000000010000"),
                new MatterSetupPayload("pin=20202021;disc=1740", 20202021L, 1740, "Aqara", "U200", false),
                "controller-state",
                step -> steps.add(step.message()));

        assertEquals("Matter BLE scan round 1 of 3 for discriminator 1740", steps.get(1));
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
    public void readDeviceDetailsRequiresReadyArtifactsAndDelegatesToGateway() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        gateway.details = new MatterDeviceDetails.Builder()
                .vendorName("IKEA of Sweden")
                .productName("BILRESA scroll wheel")
                .softwareVersionString("1.8.7")
                .build();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                false);
        List<String> steps = new ArrayList<>();

        MatterDeviceDetails details = controller.readDeviceDetails(
                0x165BC267A7E344D0L,
                "controller-state",
                step -> steps.add(step.message()));

        assertEquals(0x165BC267A7E344D0L, gateway.detailsNodeId);
        assertEquals("IKEA of Sweden", details.vendorName());
        assertEquals("BILRESA scroll wheel", details.productName());
        assertEquals("1.8.7", details.softwareVersionString());
        assertEquals("Reading connectedhomeip Java device details", steps.get(0));
        assertEquals("connectedhomeip Java device details read complete", steps.get(1));
    }

    @Test
    public void unpairRequiresReadyArtifactsAndDelegatesToGateway() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                false);
        List<String> steps = new ArrayList<>();

        controller.unpair(
                0x165BC267A7E344D0L,
                "controller-state",
                step -> steps.add(step.message()));

        assertEquals(0x165BC267A7E344D0L, gateway.unpairNodeId);
        assertEquals("Unpairing connectedhomeip Java device", steps.get(0));
        assertEquals("connectedhomeip Java device unpaired", steps.get(1));
    }

    @Test
    public void readDeviceDetailsRejectsMissingArtifactsBeforeCallingGateway() {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                missingArtifacts(),
                gateway,
                false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controller.readDeviceDetails(1234L, "state", ignored -> { }));

        assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController",
                error.getMessage());
        assertEquals(0, gateway.callCount);
    }

    @Test
    public void unpairRejectsMissingArtifactsBeforeCallingGateway() {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                missingArtifacts(),
                gateway,
                false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controller.unpair(1234L, "state", ignored -> { }));

        assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController",
                error.getMessage());
        assertEquals(0, gateway.callCount);
    }

    @Test
    public void checkFabricRestoreDelegatesToGateway() throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                false);

        ConnectedHomeIpFabricRestoreStatus status = controller.checkFabricRestore(987654321L);

        assertTrue(status.checked());
        assertTrue(status.ready());
        assertEquals(987654321L, gateway.fabricRestoreNodeId);
        assertEquals("restore-ok", status.message());
    }

    @Test
    public void checkRuntimePreflightDelegatesToGatewayWhenArtifactsAreReady() {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                readyArtifacts(),
                gateway,
                false);

        ConnectedHomeIpRuntimePreflightStatus status = controller.checkRuntimePreflight();

        assertTrue(status.ready());
        assertEquals("runtime-ok", status.message());
        assertEquals(1, gateway.runtimePreflightCalls);
    }

    @Test
    public void checkRuntimePreflightRejectsMissingArtifactsBeforeCallingGateway() {
        CapturingGateway gateway = new CapturingGateway();
        ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
                missingArtifacts(),
                gateway,
                false);

        ConnectedHomeIpRuntimePreflightStatus status = controller.checkRuntimePreflight();

        assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController",
                status.message());
        assertEquals(0, gateway.runtimePreflightCalls);
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
        private long fabricRestoreNodeId = -1L;
        private int runtimePreflightCalls;
        private int callCount;
        private String commissioningDiagnostic;
        private MatterDeviceDetails details = MatterDeviceDetails.empty();
        private long detailsNodeId = -1L;
        private long unpairNodeId = -1L;

        @Override
        public MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) {
            callCount++;
            commissioningRequest = request;
            if (commissioningDiagnostic != null) {
                ConnectedHomeIpDiagnostics.emit(commissioningDiagnostic);
            }
            return new MatterCommissioningResult(987654321L, "updated-state");
        }

        @Override
        public MatterOpenCommissioningWindowResult openCommissioningWindow(
                ConnectedHomeIpOpenCommissioningWindowRequest request) {
            callCount++;
            openCommissioningWindowRequest = request;
            return new MatterOpenCommissioningWindowResult("3497-0112-332", "ocw-state");
        }

        @Override
        public MatterDeviceDetails readDeviceDetails(long nodeId) {
            callCount++;
            detailsNodeId = nodeId;
            return details;
        }

        @Override
        public void unpair(long nodeId) {
            callCount++;
            unpairNodeId = nodeId;
        }

        public ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) {
            callCount++;
            fabricRestoreNodeId = bootstrapNodeId;
            return new ConnectedHomeIpFabricRestoreStatus(true, true, bootstrapNodeId, "restore-ok");
        }

        @Override
        public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
            runtimePreflightCalls++;
            return new ConnectedHomeIpRuntimePreflightStatus(true, "runtime-ok");
        }
    }
}
