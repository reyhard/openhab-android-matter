package org.openhab.matter.companion.qr;

import android.content.Intent;

public final class QrScanIntentFactory {
    private QrScanIntentFactory() {
    }

    public static QrScanIntentRequest createScanRequest() {
        return new QrScanIntentRequest(
                QrScanContract.ACTION_SCAN,
                QrScanContract.QR_CODE_MODE,
                false);
    }

    public static Intent createScanIntent() {
        QrScanIntentRequest request = createScanRequest();
        Intent intent = new Intent(request.action());
        intent.putExtra(QrScanContract.EXTRA_SCAN_MODE, request.scanMode());
        intent.putExtra(QrScanContract.EXTRA_SAVE_HISTORY, request.saveHistory());
        return intent;
    }

    public static String extractResult(Intent data) {
        if (data == null) {
            return "";
        }
        return extractResultText(data.getStringExtra(QrScanContract.EXTRA_SCAN_RESULT));
    }

    public static String extractResultText(String result) {
        return result == null ? "" : result.trim();
    }
}
