package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpCommissioningRequest {
    private final String datasetHex;
    private final long setupPin;
    private final int discriminator;
    private final boolean attestationBypassEnabled;
    private final String controllerState;

    public ConnectedHomeIpCommissioningRequest(
            String datasetHex,
            long setupPin,
            int discriminator,
            boolean attestationBypassEnabled,
            String controllerState) {
        if (datasetHex == null || datasetHex.trim().isEmpty()) {
            throw new IllegalArgumentException("datasetHex is required");
        }
        this.datasetHex = datasetHex;
        this.setupPin = setupPin;
        this.discriminator = discriminator;
        this.attestationBypassEnabled = attestationBypassEnabled;
        this.controllerState = controllerState == null ? "" : controllerState;
    }

    public String datasetHex() {
        return datasetHex;
    }

    public long setupPin() {
        return setupPin;
    }

    public int discriminator() {
        return discriminator;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }

    public String controllerState() {
        return controllerState;
    }
}
