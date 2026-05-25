package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpControllerArtifactsStatus {
    private final boolean ready;
    private final String libraryName;
    private final String message;

    public ConnectedHomeIpControllerArtifactsStatus(boolean ready, String libraryName, String message) {
        this.ready = ready;
        this.libraryName = libraryName == null ? "" : libraryName;
        this.message = message == null ? "" : message;
    }

    public boolean ready() {
        return ready;
    }

    public String libraryName() {
        return libraryName;
    }

    public String message() {
        return message;
    }
}
