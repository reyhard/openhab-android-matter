package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class FakeMatterController implements MatterController {
    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        emit(listener, "Simulated: BLE scan matched discriminator " + payload.discriminator(), false);
        emit(listener, "Simulated: PASE session established", false);
        emit(listener, "Simulated: attestation bypass path selected for demo mode", false);
        emit(listener, "Simulated: Thread dataset accepted without displaying credentials", false);
        emit(listener, "Simulated: Thread network join completed", true);
        return 1L;
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        emit(listener, "Simulated: opening commissioning window on node " + nodeId + " for " + timeoutSeconds + " seconds", false);
        emit(listener, "Simulated: temporary setup code generated for discriminator " + discriminator, true);
        return "3497-0112-332";
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }
}
