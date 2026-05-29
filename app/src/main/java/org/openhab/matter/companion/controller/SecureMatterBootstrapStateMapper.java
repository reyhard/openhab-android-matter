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
                secretCodec.encode(safeState.controllerState()),
                safeState.vendorName(),
                safeState.productName());
    }

    public MatterBootstrapState fromStoredValues(long bootstrapNodeId, String controllerState) {
        return fromStoredValues(bootstrapNodeId, controllerState, "", "");
    }

    public MatterBootstrapState fromStoredValues(
            long bootstrapNodeId,
            String controllerState,
            String vendorName,
            String productName) {
        String safeControllerState = controllerState == null ? "" : controllerState;
        if (safeControllerState.isEmpty()) {
            return new MatterBootstrapState(bootstrapNodeId, "", false, vendorName, productName);
        }

        try {
            return new MatterBootstrapState(
                    bootstrapNodeId,
                    secretCodec.decode(safeControllerState),
                    false,
                    vendorName,
                    productName);
        } catch (GeneralSecurityException e) {
            return new MatterBootstrapState(-1L, "", true);
        }
    }

    public static final class StoredBootstrapState {
        private final long bootstrapNodeId;
        private final String controllerState;
        private final String vendorName;
        private final String productName;

        StoredBootstrapState(long bootstrapNodeId, String controllerState, String vendorName, String productName) {
            this.bootstrapNodeId = bootstrapNodeId;
            this.controllerState = controllerState == null ? "" : controllerState;
            this.vendorName = vendorName == null ? "" : vendorName.trim();
            this.productName = productName == null ? "" : productName.trim();
        }

        public long bootstrapNodeId() {
            return bootstrapNodeId;
        }

        public String controllerState() {
            return controllerState;
        }

        public String vendorName() {
            return vendorName;
        }

        public String productName() {
            return productName;
        }
    }
}
