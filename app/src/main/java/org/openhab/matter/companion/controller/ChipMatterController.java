package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    static {
        System.loadLibrary("openhab_matter_chip");
    }

    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        emit(listener, "Starting CHIP BLE Thread commissioning", false);
        long nodeId = nativeCommissionBleThread(dataset.hex(), payload.pin(), payload.discriminator());
        emit(listener, "CHIP BLE Thread commissioning complete for node " + nodeId, true);
        return nodeId;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        emit(listener, "Opening CHIP commissioning window", false);
        String code = nativeOpenCommissioningWindow(nodeId, timeoutSeconds, discriminator);
        emit(listener, "CHIP commissioning window opened", true);
        return code;
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }

    private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);

    private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
}
