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
    private final ConnectedHomeIpDeviceMetadataReader metadataReader;
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
                ConnectedHomeIpDeviceMetadataReader.none(),
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
        this(
                controllerProvider,
                bleConnectionProvider,
                nodeIdAllocator,
                commissioningMonitor,
                attestationHandler,
                devicePointerProvider,
                commandFactory,
                ConnectedHomeIpDeviceMetadataReader.none(),
                openCommissioningWindowTimeoutMillis);
    }

    public ConnectedHomeIpReflectionGateway(
            ConnectedHomeIpControllerProvider controllerProvider,
            ConnectedHomeIpBleConnectionProvider bleConnectionProvider,
            ConnectedHomeIpNodeIdAllocator nodeIdAllocator,
            ConnectedHomeIpCommissioningMonitor commissioningMonitor,
            ConnectedHomeIpAttestationHandler attestationHandler,
            ConnectedHomeIpDevicePointerProvider devicePointerProvider,
            ConnectedHomeIpReflectionCommandFactory commandFactory,
            ConnectedHomeIpDeviceMetadataReader metadataReader) {
        this(
                controllerProvider,
                bleConnectionProvider,
                nodeIdAllocator,
                commissioningMonitor,
                attestationHandler,
                devicePointerProvider,
                commandFactory,
                metadataReader,
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
            ConnectedHomeIpDeviceMetadataReader metadataReader,
            long openCommissioningWindowTimeoutMillis) {
        this.controllerProvider = require(controllerProvider, "controllerProvider");
        this.bleConnectionProvider = require(bleConnectionProvider, "bleConnectionProvider");
        this.nodeIdAllocator = require(nodeIdAllocator, "nodeIdAllocator");
        this.commissioningMonitor = require(commissioningMonitor, "commissioningMonitor");
        this.attestationHandler = require(attestationHandler, "attestationHandler");
        this.devicePointerProvider = require(devicePointerProvider, "devicePointerProvider");
        this.commandFactory = require(commandFactory, "commandFactory");
        this.metadataReader = require(metadataReader, "metadataReader");
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
        commandFactory.invokeClose(controller);
        ConnectedHomeIpDiagnostics.emit("Cleared stale connectedhomeip BLE controller state before commissioning");
        attestationHandler.prepareForCommissioning(controller, nodeId, request.attestationBypassEnabled());
        commissioningMonitor.prepare(controller);
        MatterCommissioningResult result;
        try (ConnectedHomeIpBleConnection connection = bleConnectionProvider.connect(request.discriminator())) {
            commandFactory.invokePairDeviceThroughBle(
                    controller,
                    connection.gatt(),
                    connection.connectionId(),
                    nodeId,
                    request.setupPin(),
                    commissionParameters);
            result = commissioningMonitor.awaitCommissioned(nodeId, request.controllerState());
        } catch (Exception exception) {
            try {
                commandFactory.invokeClose(controller);
                ConnectedHomeIpDiagnostics.emit("Cleared connectedhomeip BLE controller state after commissioning failure");
            } catch (ReflectiveOperationException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        return result.withMetadata(readVendorAndProduct(controller, result.nodeId()));
    }

    @Override
    public MatterDeviceDetails readDeviceDetails(long nodeId) throws Exception {
        Object controller = controllerProvider.controller();
        MatterDeviceDetails details = metadataReader.readDeviceDetails(controller, nodeId);
        return details == null ? MatterDeviceDetails.empty() : details;
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

    @Override
    public ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) {
        return new ConnectedHomeIpFabricRestoreProbe(controllerProvider, devicePointerProvider).check(bootstrapNodeId);
    }

    @Override
    public ConnectedHomeIpRuntimePreflightStatus checkRuntimePreflight() {
        try {
            Object controller = controllerProvider.controller();
            if (bleConnectionProvider instanceof ConnectedHomeIpRuntimePreflightChecker) {
                ConnectedHomeIpRuntimePreflightStatus bleStatus =
                        ((ConnectedHomeIpRuntimePreflightChecker) bleConnectionProvider).checkRuntimePreflight();
                if (!bleStatus.ready()) {
                    return bleStatus;
                }
            }
            return new ConnectedHomeIpRuntimePreflightStatus(
                    true,
                    "controller " + controller.getClass().getName() + " and BLE manager initialized");
        } catch (Exception | LinkageError ex) {
            return new ConnectedHomeIpRuntimePreflightStatus(
                    false,
                    "connectedhomeip runtime preflight failed: " + safeMessage(ex));
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private MatterDeviceMetadata readVendorAndProduct(Object controller, long nodeId) {
        try {
            MatterDeviceDetails details = metadataReader.readDeviceDetails(controller, nodeId);
            MatterDeviceMetadata metadata = details == null
                    ? MatterDeviceMetadata.empty()
                    : new MatterDeviceMetadata(details.vendorName(), details.productName());
            if (!metadata.isEmpty()) {
                ConnectedHomeIpDiagnostics.emit("Read Matter Basic Information vendor/product metadata");
            }
            return metadata;
        } catch (Exception | LinkageError exception) {
            ConnectedHomeIpDiagnostics.emit(
                    "Unable to read Matter Basic Information vendor/product metadata: "
                            + safeMessage(exception));
            return MatterDeviceMetadata.empty();
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }
}
