package org.openhab.matter.companion.controller;

public final class ChipMatterControllerStatus {
    private final boolean ready;
    private final String libraryName;
    private final boolean attestationBypassEnabled;
    private final String message;

    public ChipMatterControllerStatus(boolean ready, String libraryName, boolean attestationBypassEnabled, String message) {
        this.ready = ready;
        this.libraryName = libraryName == null ? "" : libraryName;
        this.attestationBypassEnabled = attestationBypassEnabled;
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

    public String message() {
        return message;
    }
}
