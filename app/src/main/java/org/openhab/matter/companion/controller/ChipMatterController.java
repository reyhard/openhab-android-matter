package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    private final NativeChipBridge bridge;
    private final ChipMatterControllerConfig config;
    private ChipMatterControllerStatus status;

    public ChipMatterController() {
        this(new SystemNativeChipBridge(), ChipMatterControllerConfig.defaultConfig());
    }

    public ChipMatterController(NativeLibraryLoader loader, ChipMatterControllerConfig config) {
        this(new LoaderBackedNativeChipBridge(loader), config);
    }

    public ChipMatterController(NativeChipBridge bridge, ChipMatterControllerConfig config) {
        this.bridge = bridge == null ? new SystemNativeChipBridge() : bridge;
        this.config = config == null ? ChipMatterControllerConfig.defaultConfig() : config;
    }

    public synchronized ChipMatterControllerStatus readiness() {
        return ensureReady();
    }

    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        requireReady();
        emit(listener, "Starting CHIP BLE Thread commissioning", false);
        long nodeId;
        try {
            nodeId = bridge.commissionBleThread(dataset.hex(), payload.pin(), payload.discriminator());
        } catch (UnsatisfiedLinkError error) {
            throw new IllegalStateException("Native CHIP controller entry point is missing: " + error.getMessage(), error);
        }
        emit(listener, "CHIP BLE Thread commissioning complete for node " + nodeId, true);
        return nodeId;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        requireReady();
        emit(listener, "Opening CHIP commissioning window", false);
        String code;
        try {
            code = bridge.openCommissioningWindow(nodeId, timeoutSeconds, discriminator);
        } catch (UnsatisfiedLinkError error) {
            throw new IllegalStateException("Native CHIP controller entry point is missing: " + error.getMessage(), error);
        }
        emit(listener, "CHIP commissioning window opened", true);
        return code;
    }

    private synchronized void requireReady() {
        ChipMatterControllerStatus currentStatus = ensureReady();
        if (!currentStatus.ready()) {
            throw new IllegalStateException(currentStatus.message());
        }
    }

    private ChipMatterControllerStatus ensureReady() {
        if (status == null) {
            String libraryName = config.nativeLibraryName();
            try {
                bridge.load(libraryName);
                NativeChipBridgeMetadata metadata = NativeChipBridgeMetadata.parse(bridge.metadata());
                boolean ready = metadata.productionReady();
                status = new ChipMatterControllerStatus(
                        ready,
                        libraryName,
                        config.attestationBypassEnabled(),
                        metadata.kind(),
                        metadata.productionReady(),
                        ready
                                ? "Native CHIP library loaded: " + libraryName
                                : notProductionMessage(metadata));
            } catch (UnsatisfiedLinkError | SecurityException exception) {
                status = new ChipMatterControllerStatus(
                        false,
                        libraryName,
                        config.attestationBypassEnabled(),
                        "unknown",
                        false,
                        exception.getMessage());
            }
        }
        return status;
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }

    private static String notProductionMessage(NativeChipBridgeMetadata metadata) {
        String message = metadata.message();
        String prefix = message.isEmpty() ? "" : message + ". ";
        return prefix + "Native CHIP bridge is not a production connectedhomeip implementation";
    }

    private static final class LoaderBackedNativeChipBridge implements NativeChipBridge {
        private final NativeLibraryLoader loader;
        private final SystemNativeChipBridge delegate = new SystemNativeChipBridge();

        private LoaderBackedNativeChipBridge(NativeLibraryLoader loader) {
            this.loader = loader == null ? new SystemNativeLibraryLoader() : loader;
        }

        @Override
        public void load(String libraryName) {
            loader.load(libraryName);
        }

        @Override
        public String metadata() {
            return delegate.metadata();
        }

        @Override
        public long commissionBleThread(String datasetHex, long pin, int discriminator) {
            return delegate.commissionBleThread(datasetHex, pin, discriminator);
        }

        @Override
        public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator) {
            return delegate.openCommissioningWindow(nodeId, timeoutSeconds, discriminator);
        }
    }
}
