package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    private final NativeLibraryLoader loader;
    private final ChipMatterControllerConfig config;
    private ChipMatterControllerStatus status;

    public ChipMatterController() {
        this(new SystemNativeLibraryLoader(), ChipMatterControllerConfig.defaultConfig());
    }

    ChipMatterController(NativeLibraryLoader loader, ChipMatterControllerConfig config) {
        this.loader = loader;
        this.config = config == null ? ChipMatterControllerConfig.defaultConfig() : config;
    }

    ChipMatterControllerStatus readiness() {
        return ensureReady();
    }

    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        requireReady();
        emit(listener, "Starting CHIP BLE Thread commissioning", false);
        long nodeId = nativeCommissionBleThread(dataset.hex(), payload.pin(), payload.discriminator());
        emit(listener, "CHIP BLE Thread commissioning complete for node " + nodeId, true);
        return nodeId;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        requireReady();
        emit(listener, "Opening CHIP commissioning window", false);
        String code = nativeOpenCommissioningWindow(nodeId, timeoutSeconds, discriminator);
        emit(listener, "CHIP commissioning window opened", true);
        return code;
    }

    private void requireReady() {
        ChipMatterControllerStatus currentStatus = ensureReady();
        if (!currentStatus.ready()) {
            throw new IllegalStateException(currentStatus.message());
        }
    }

    private ChipMatterControllerStatus ensureReady() {
        if (status == null) {
            String libraryName = config.nativeLibraryName();
            try {
                loader.load(libraryName);
                status = new ChipMatterControllerStatus(
                        true,
                        libraryName,
                        config.attestationBypassEnabled(),
                        "Native CHIP library loaded: " + libraryName);
            } catch (UnsatisfiedLinkError | SecurityException exception) {
                status = new ChipMatterControllerStatus(
                        false,
                        libraryName,
                        config.attestationBypassEnabled(),
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

    private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);

    private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
