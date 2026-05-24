package org.openhab.matter.companion.qr;

public final class QrScanIntentRequest {
    private final String action;
    private final String scanMode;
    private final boolean saveHistory;

    public QrScanIntentRequest(String action, String scanMode, boolean saveHistory) {
        this.action = action == null ? "" : action;
        this.scanMode = scanMode == null ? "" : scanMode;
        this.saveHistory = saveHistory;
    }

    public String action() {
        return action;
    }

    public String scanMode() {
        return scanMode;
    }

    public boolean saveHistory() {
        return saveHistory;
    }
}
