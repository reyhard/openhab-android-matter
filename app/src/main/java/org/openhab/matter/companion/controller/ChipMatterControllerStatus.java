package org.openhab.matter.companion.controller;

public final class ChipMatterControllerStatus {
    private final boolean ready;
    private final String libraryName;
    private final boolean attestationBypassEnabled;
    private final String bridgeKind;
    private final boolean productionReady;
    private final String message;

    public ChipMatterControllerStatus(boolean ready, String libraryName, boolean attestationBypassEnabled, String message) {
        this(ready, libraryName, attestationBypassEnabled, "unknown", ready, message);
    }

    public ChipMatterControllerStatus(
            boolean ready,
            String libraryName,
            boolean attestationBypassEnabled,
            String bridgeKind,
            boolean productionReady,
            String message) {
        this.ready = ready;
        this.libraryName = libraryName == null ? "" : libraryName;
        this.attestationBypassEnabled = attestationBypassEnabled;
        this.bridgeKind = bridgeKind == null || bridgeKind.isEmpty() ? "unknown" : bridgeKind;
        this.productionReady = productionReady;
        this.message = message == null ? "" : message;
    }

    public boolean ready() {
        return ready;
    }

    public String libraryName() {
        return libraryName;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }

    public String bridgeKind() {
        return bridgeKind;
    }

    public boolean productionReady() {
        return productionReady;
    }

    public String message() {
        return message;
    }
}
