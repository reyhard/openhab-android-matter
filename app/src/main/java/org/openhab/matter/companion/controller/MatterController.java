package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public interface MatterController {
    interface ProgressListener {
        void onProgress(CommissioningStep step);
    }

    MatterCommissioningResult commissionBleThread(
            ThreadDataset dataset,
            MatterSetupPayload payload,
            String controllerState,
            ProgressListener listener) throws Exception;

    MatterOpenCommissioningWindowResult openCommissioningWindow(
            long nodeId,
            int timeoutSeconds,
            int discriminator,
            String controllerState,
            ProgressListener listener) throws Exception;

    default MatterDeviceDetails readDeviceDetails(
            long nodeId,
            String controllerState,
            ProgressListener listener) throws Exception {
        throw new UnsupportedOperationException("Matter device details are not supported by this controller.");
    }
}
