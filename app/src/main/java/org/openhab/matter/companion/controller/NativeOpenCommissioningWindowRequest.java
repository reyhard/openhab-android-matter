package org.openhab.matter.companion.controller;

public final class NativeOpenCommissioningWindowRequest {
    private final long nodeId;
    private final int timeoutSeconds;
    private final int discriminator;
    private final String controllerState;

    public NativeOpenCommissioningWindowRequest(long nodeId, int timeoutSeconds, int discriminator, String controllerState) {
        this.nodeId = nodeId;
        this.timeoutSeconds = timeoutSeconds;
        this.discriminator = discriminator;
        this.controllerState = nullToEmpty(controllerState);
    }

    public long nodeId() {
        return nodeId;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public int discriminator() {
        return discriminator;
    }

    public String controllerState() {
        return controllerState;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
