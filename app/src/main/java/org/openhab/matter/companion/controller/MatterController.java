package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public interface MatterController {
    interface ProgressListener {
        void onProgress(CommissioningStep step);
    }

    long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) throws Exception;

    String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) throws Exception;
}