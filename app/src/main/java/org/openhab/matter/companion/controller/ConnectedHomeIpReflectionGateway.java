package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.ThreadDataset;

public final class ConnectedHomeIpReflectionGateway implements ConnectedHomeIpControllerGateway {
    private static final long DEFAULT_OPEN_COMMISSIONING_WINDOW_TIMEOUT_MILLIS = 300_000L;

    private final ConnectedHomeIpControllerProvider controllerProvider;
    private final ConnectedHomeIpBleConnectionProvider bleConnectionProvider;
    private final ConnectedHomeIpNodeIdAllocator nodeIdAllocator;
    private final ConnectedHomeIpCommissioningMonitor commissioningMonitor;
    private final ConnectedHomeIpAttestationHandler attestationHandler;
    private final ConnectedHomeIpDevicePointerProvider devicePointerProvider;
    private final ConnectedHomeIpReflectionCommandFactory commandFactory;
    private final long openCommissioningWindowTimeoutMillis;

    public ConnectedHomeIpReflectionGateway(
            ConnectedHomeIpControllerProvider controllerProvider,
            ConnectedHomeIpBleConnectionProvider bleConnectionProvider,
            ConnectedHomeIpNodeIdAllocator nodeIdAllocator,
            ConnectedHomeIpCommissioningMonitor commissioningMonitor,
            ConnectedHomeIpAttestationHandler attestationHandler,
            ConnectedHomeIpDevicePointerProvider devicePointerProvider,
            ConnectedHomeIpReflectionCommandFactory commandFactory) {
        this(
                controllerProvider,
                bleConnectionProvider,
                nodeIdAllocator,
                commissioningMonitor,
                attestationHandler,
                devicePointerProvider,
                commandFactory,
                DEFAULT_OPEN_COMMISSIONING_WINDOW_TIMEOUT_MILLIS);
    }

    public ConnectedHomeIpReflectionGateway(
            ConnectedHomeIpControllerProvider controllerProvider,
            ConnectedHomeIpBleConnectionProvider bleConnectionProvider,
            ConnectedHomeIpNodeIdAllocator nodeIdAllocator,
            ConnectedHomeIpCommissioningMonitor commissioningMonitor,
            ConnectedHomeIpAttestationHandler attestationHandler,
            ConnectedHomeIpDevicePointerProvider devicePointerProvider,
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            long openCommissioningWindowTimeoutMillis) {
        this.controllerProvider = require(controllerProvider, "controllerProvider");
        this.bleConnectionProvider = require(bleConnectionProvider, "bleConnectionProvider");
        this.nodeIdAllocator = require(nodeIdAllocator, "nodeIdAllocator");
        this.commissioningMonitor = require(commissioningMonitor, "commissioningMonitor");
        this.attestationHandler = require(attestationHandler, "attestationHandler");
        this.devicePointerProvider = require(devicePointerProvider, "devicePointerProvider");
        this.commandFactory = require(commandFactory, "commandFactory");
        if (openCommissioningWindowTimeoutMillis <= 0) {
            throw new IllegalArgumentException("openCommissioningWindowTimeoutMillis must be positive");
        }
        this.openCommissioningWindowTimeoutMillis = openCommissioningWindowTimeoutMillis;
    }

    @Override
    public MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        Object controller = controllerProvider.controller();
        long nodeId = nodeIdAllocator.nextNodeId();
        Object commissionParameters = commandFactory.newThreadCommissionParameters(ThreadDataset.parse(request.datasetHex()));
        attestationHandler.prepareForCommissioning(controller, nodeId, request.attestationBypassEnabled());
        try (ConnectedHomeIpBleConnection connection = bleConnectionProvider.connect(request.discriminator())) {
            commandFactory.invokePairDeviceThroughBle(
                    controller,
                    connection.gatt(),
                    connection.connectionId(),
                    nodeId,
                    request.setupPin(),
                    commissionParameters);
            return commissioningMonitor.awaitCommissioned(nodeId, request.controllerState());
        }
    }

    @Override
    public MatterOpenCommissioningWindowResult openCommissioningWindow(
            ConnectedHomeIpOpenCommissioningWindowRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        Object controller = controllerProvider.controller();
        try (ConnectedHomeIpDevicePointer pointer = devicePointerProvider.acquire(controller, request.nodeId())) {
            ConnectedHomeIpOpenCommissioningWindowCallback callback = commandFactory.newOpenCommissioningWindowCallback(
                    request.controllerState());
            boolean started = commandFactory.invokeOpenPairingWindowWithPinCallback(
                    controller,
                    pointer.value(),
                    request,
                    null,
                    callback.proxy());
            if (!started) {
                throw new IllegalStateException("OpenCommissioningWindow command did not start for node " + request.nodeId());
            }
            return callback.awaitResult(openCommissioningWindowTimeoutMillis);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
