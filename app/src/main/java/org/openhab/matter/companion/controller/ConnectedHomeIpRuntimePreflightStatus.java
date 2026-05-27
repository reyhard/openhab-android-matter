package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpRuntimePreflightStatus {
    private final boolean ready;
    private final String message;

    public ConnectedHomeIpRuntimePreflightStatus(boolean ready, String message) {
        this.ready = ready;
        this.message = message == null ? "" : message;
    }

    public boolean ready() {
        return ready;
    }

    public String message() {
        return message;
    }
}
