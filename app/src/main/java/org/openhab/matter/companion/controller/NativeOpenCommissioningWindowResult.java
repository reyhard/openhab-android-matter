package org.openhab.matter.companion.controller;

public final class NativeOpenCommissioningWindowResult {
    private final String temporaryCode;
    private final String controllerState;

    public NativeOpenCommissioningWindowResult(String temporaryCode, String controllerState) {
        if (temporaryCode == null || temporaryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("temporaryCode is required");
        }
        this.temporaryCode = temporaryCode;
        this.controllerState = nullToEmpty(controllerState);
    }

    public String temporaryCode() {
        return temporaryCode;
    }

    public String controllerState() {
        return controllerState;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
