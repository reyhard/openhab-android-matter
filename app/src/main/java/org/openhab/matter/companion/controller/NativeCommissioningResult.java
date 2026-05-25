package org.openhab.matter.companion.controller;

public final class NativeCommissioningResult {
    private final long nodeId;
    private final String controllerState;

    public NativeCommissioningResult(long nodeId, String controllerState) {
        this.nodeId = nodeId;
        this.controllerState = nullToEmpty(controllerState);
    }

    public long nodeId() {
        return nodeId;
    }

    public String controllerState() {
        return controllerState;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
