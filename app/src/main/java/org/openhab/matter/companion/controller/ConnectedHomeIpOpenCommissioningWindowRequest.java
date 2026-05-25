package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpOpenCommissioningWindowRequest {
    private final long nodeId;
    private final int timeoutSeconds;
    private final long iteration;
    private final int discriminator;
    private final String controllerState;

    public ConnectedHomeIpOpenCommissioningWindowRequest(
            long nodeId,
            int timeoutSeconds,
            long iteration,
            int discriminator,
            String controllerState) {
        this.nodeId = nodeId;
        this.timeoutSeconds = timeoutSeconds;
        this.iteration = iteration;
        this.discriminator = discriminator;
        this.controllerState = controllerState == null ? "" : controllerState;
    }

    public long nodeId() {
        return nodeId;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public long iteration() {
        return iteration;
    }

    public int discriminator() {
        return discriminator;
    }

    public String controllerState() {
        return controllerState;
    }
}
