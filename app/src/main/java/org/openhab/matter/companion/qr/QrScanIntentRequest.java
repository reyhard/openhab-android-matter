package org.openhab.matter.companion.qr;

public final class QrScanIntentRequest {
    private final String action;
    private final String scanModeExtra;
    private final String scanMode;
    private final String saveHistoryExtra;
    private final boolean saveHistory;

    QrScanIntentRequest(String action, String scanModeExtra, String scanMode, String saveHistoryExtra, boolean saveHistory) {
        if (action == null || scanModeExtra == null || scanMode == null || saveHistoryExtra == null) {
            throw new IllegalArgumentException("QR scan request fields must not be null");
        }
        this.action = action;
        this.scanModeExtra = scanModeExtra;
        this.scanMode = scanMode;
        this.saveHistoryExtra = saveHistoryExtra;
        this.saveHistory = saveHistory;
    }

    public String action() {
        return action;
    }

    public String scanMode() {
        return scanMode;
    }

    public String scanModeExtra() {
        return scanModeExtra;
    }

    public String saveHistoryExtra() {
        return saveHistoryExtra;
    }

    public String scanResultExtra() {
        return QrScanContract.EXTRA_SCAN_RESULT;
    }

    public boolean saveHistory() {
        return saveHistory;
    }
}
