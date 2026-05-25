package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.CommissioningStep;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class FakeMatterController implements MatterController {
    @Override
    public MatterCommissioningResult commissionBleThread(
            ThreadDataset dataset,
            MatterSetupPayload payload,
            String controllerState,
            ProgressListener listener) {
        emit(listener, "Simulated: BLE scan matched discriminator " + payload.discriminator(), false);
        emit(listener, "Simulated: PASE session established", false);
        emit(listener, "Simulated: attestation bypass path selected for demo mode", false);
        emit(listener, "Simulated: Thread dataset accepted without displaying credentials", false);
        emit(listener, "Simulated: Thread network join completed", true);
        return new MatterCommissioningResult(1L, "fake-controller-state:1");
    }

    @Override
    public MatterOpenCommissioningWindowResult openCommissioningWindow(
            long nodeId,
            int timeoutSeconds,
            int discriminator,
            String controllerState,
            ProgressListener listener) {
        emit(listener, "Simulated: opening commissioning window on node " + nodeId + " for " + timeoutSeconds + " seconds", false);
        emit(listener, "Simulated: temporary setup code generated for discriminator " + discriminator, true);
        return new MatterOpenCommissioningWindowResult("3497-0112-332", "fake-controller-state:" + nodeId);
    }

    private static void emit(ProgressListener listener, String message, boolean complete) {
        if (listener != null) {
            listener.onProgress(new CommissioningStep(message, complete));
        }
    }
}
