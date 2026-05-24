package org.openhab.matter.companion.qr;

public final class QrScanContract {
    public static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String EXTRA_SCAN_MODE = "SCAN_MODE";
    public static final String EXTRA_SAVE_HISTORY = "SAVE_HISTORY";
    public static final String EXTRA_SCAN_RESULT = "SCAN_RESULT";
    public static final String QR_CODE_MODE = "QR_CODE_MODE";

    private QrScanContract() {
    }
}
