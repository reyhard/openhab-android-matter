package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ConnectedHomeIpMatterController implements MatterControllerCandidate, ConnectedHomeIpFabricRestoreChecker,
        ConnectedHomeIpRuntimePreflightChecker {
    private static final long ENHANCED_COMMISSIONING_WINDOW_ITERATION = 1000L;

    private final ConnectedHomeIpControllerArtifacts artifacts;
    private final ConnectedHomeIpControllerGateway gateway;
    private final boolean attestationBypassEnabled;

    public ConnectedHomeIpMatterController(
            ConnectedHomeIpControllerArtifacts artifacts,
            ConnectedHomeIpControllerGateway gateway,
            boolean attestationBypassEnabled) {
        this.artifacts = artifacts == null ? new ConnectedHomeIpControllerArtifacts() : artifacts;
        this.gateway = gateway;
        this.attestationBypassEnabled = attestationBypassEnabled;
        if (this.gateway == null) {
            throw new IllegalArgumentException("gateway is required");
        }
    }

    @Override
    public ChipMatterControllerStatus readiness() {
        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();
        return new ChipMatterControllerStatus(
                status.ready(),
                status.libraryName(),
                attestationBypassEnabled,
                "connectedhomeip-java",
                status.ready(),
                status.message());
    }

    @Override
    public MatterCommissioningResult commissionBleThread(
            ThreadDataset dataset,
            MatterSetupPayload payload,
            String controllerState,
            ProgressListener listener) throws Exception {
        requireArtifactsReady();
        emit(listener, "Starting connectedhomeip Java BLE Thread commissioning", false);
        MatterCommissioningResult result = ConnectedHomeIpDiagnostics.withListener(
                message -> emit(listener, message, false),
                () -> gateway.commissionBleThread(new ConnectedHomeIpCommissioningRequest(
                        dataset.hex(),
                        payload.pin(),
                        payload.discriminator(),
                        attestationBypassEnabled,
                        controllerState)));
        emit(listener, "connectedhomeip Java BLE Thread commissioning complete for node " + result.nodeId(), true);
        return result;
    }

    @Override
    public MatterOpenCommissioningWindowResult openCommissioningWindow(
            long nodeId,
            int timeoutSeconds,
            int discriminator,
            String controllerState,
            ProgressListener listener) throws Exception {
        requireArtifactsReady();
        emit(listener, "Opening connectedhomeip Java commissioning window", false);
        MatterOpenCommissioningWindowResult result = ConnectedHomeIpDiagnostics.withListener(
                message -> emit(listener, message, false),
                () -> gateway.openCommissioningWindow(
                        new ConnectedHomeIpOpenCommissioningWindowRequest(
                                nodeId,
                                timeoutSeconds,
                                ENHANCED_COMMISSIONING_WINDOW_ITERATION,
                                discriminator,
                                controllerState)));
        emit(listener, "connectedhomeip Java commissioning window opened", true);
        return result;
    }

    @Override
    public MatterDeviceDetails readDeviceDetails(
            long nodeId,
            String controllerState,
            ProgressListener listener) throws Exception {
        requireArtifactsReady();
        emit(listener, "Reading connectedhomeip Java device details", false);
        MatterDeviceDetails result = ConnectedHomeIpDiagnostics.withListener(
                message -> emit(listener, message, false),
                () -> gateway.readDeviceDetails(nodeId));
        emit(listener, "connectedhomeip Java device details read complete", true);
        return result == null ? MatterDeviceDetails.empty() : result;
    }

    @Override
    public ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) throws Exception {
        requireArtifactsReady();
        return gateway.checkFabricRestore(bootstrapNodeId);
    }

    @Override
    public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();
        if (!status.ready()) {
            return new ConnectedHomeIpRuntimePreflightStatus(false, status.message());
        }
        return gateway.checkRuntimePreflight();
    }

    private void requireArtifactsReady() {
        ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();
        if (!status.ready()) {
            throw new IllegalStateException(status.message());
        }
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }
}
