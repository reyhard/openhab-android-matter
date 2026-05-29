package org.openhab.matter.companion.controller;

public final class MatterOpenCommissioningWindowResult {
    private final String temporaryCode;
    private final String qrCode;
    private final String controllerState;

    public MatterOpenCommissioningWindowResult(String temporaryCode, String controllerState) {
        this(temporaryCode, "", controllerState);
    }

    public MatterOpenCommissioningWindowResult(String temporaryCode, String qrCode, String controllerState) {
        String safeTemporaryCode = nullToEmpty(temporaryCode).trim();
        String safeQrCode = nullToEmpty(qrCode).trim();
        if (safeTemporaryCode.isEmpty() && safeQrCode.isEmpty()) {
            throw new IllegalArgumentException("temporaryCode or qrCode is required");
        }
        this.temporaryCode = safeTemporaryCode;
        this.qrCode = safeQrCode;
        this.controllerState = nullToEmpty(controllerState);
    }

    public String temporaryCode() {
        return temporaryCode;
    }

    public String manualCode() {
        return temporaryCode;
    }

    public String qrCode() {
        return qrCode;
    }

    public boolean hasQrCode() {
        return !qrCode.isEmpty();
    }

    public String controllerState() {
        return controllerState;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
