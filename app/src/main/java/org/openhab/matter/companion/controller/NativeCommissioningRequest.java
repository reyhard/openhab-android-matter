package org.openhab.matter.companion.controller;

public final class NativeCommissioningRequest {
    private final String datasetHex;
    private final long setupPin;
    private final int discriminator;
    private final boolean attestationBypassEnabled;
    private final String controllerState;

    public NativeCommissioningRequest(
            String datasetHex,
            long setupPin,
            int discriminator,
            boolean attestationBypassEnabled,
            String controllerState) {
        this.datasetHex = nullToEmpty(datasetHex);
        this.setupPin = setupPin;
        this.discriminator = discriminator;
        this.attestationBypassEnabled = attestationBypassEnabled;
        this.controllerState = nullToEmpty(controllerState);
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
