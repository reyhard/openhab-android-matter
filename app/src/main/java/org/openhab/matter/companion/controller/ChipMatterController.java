package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.ThreadDataset;

public final class ChipMatterController implements MatterController {
    @Override
    public long commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, ProgressListener listener) {
        throw new UnsupportedOperationException("CHIP JNI library is not bundled yet. Use FakeMatterController for the installable MVP.");
    }

    @Override
    public String openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, ProgressListener listener) {
        throw new UnsupportedOperationException("CHIP JNI library is not bundled yet. Use FakeMatterController for the installable MVP.");
    }
}