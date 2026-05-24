package org.openhab.matter.companion.controller;

import org.openhab.matter.companion.config.SecretCodec;

import java.security.GeneralSecurityException;

public final class SecureMatterBootstrapStateMapper {
    private final SecretCodec secretCodec;

    public SecureMatterBootstrapStateMapper(SecretCodec secretCodec) {
        this.secretCodec = secretCodec;
    }

    public StoredBootstrapState toStoredValues(MatterBootstrapState state) throws GeneralSecurityException {
        MatterBootstrapState safeState = state == null ? MatterBootstrapState.empty() : state;
        return new StoredBootstrapState(
                safeState.bootstrapNodeId(),
                secretCodec.encode(safeState.controllerState()));
    }

    public MatterBootstrapState fromStoredValues(long bootstrapNodeId, String controllerState) {
        String safeControllerState = controllerState == null ? "" : controllerState;
        if (safeControllerState.isEmpty()) {
            return MatterBootstrapState.empty();
        }

        try {
            return new MatterBootstrapState(bootstrapNodeId, secretCodec.decode(safeControllerState), false);
        } catch (GeneralSecurityException e) {
            return new MatterBootstrapState(-1L, "", true);
        }
    }

    public static final class StoredBootstrapState {
        private final long bootstrapNodeId;
        private final String controllerState;

        StoredBootstrapState(long bootstrapNodeId, String controllerState) {
            this.bootstrapNodeId = bootstrapNodeId;
            this.controllerState = controllerState == null ? "" : controllerState;
        }

        public long bootstrapNodeId() {
            return bootstrapNodeId;
        }

        public String controllerState() {
            return controllerState;
        }
    }
}
