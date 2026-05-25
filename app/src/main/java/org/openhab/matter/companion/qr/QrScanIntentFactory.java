package org.openhab.matter.companion.qr;

import android.content.Intent;

import org.openhab.matter.companion.domain.MatterHandoffCodeParser;

public final class QrScanIntentFactory {
    private QrScanIntentFactory() {
    }

    public static QrScanIntentRequest createScanRequest() {
        return new QrScanIntentRequest(
                QrScanContract.ACTION_SCAN,
                QrScanContract.EXTRA_SCAN_MODE,
                QrScanContract.QR_CODE_MODE,
                QrScanContract.EXTRA_SAVE_HISTORY,
                false);
    }

    public static Intent createScanIntent() {
        QrScanIntentRequest request = createScanRequest();
        Intent intent = new Intent(request.action());
        intent.putExtra(request.scanModeExtra(), request.scanMode());
        intent.putExtra(request.saveHistoryExtra(), request.saveHistory());
        return intent;
    }

    public static Intent createScanResultIntent(String result) {
        Intent intent = new Intent();
        intent.putExtra(QrScanContract.EXTRA_SCAN_RESULT, createScanResultPayload(result));
        return intent;
    }

    public static String createScanResultPayload(String result) {
        return extractResultText(result);
    }

    public static String extractResult(Intent data) {
        if (data == null) {
            return "";
        }
        return extractResultText(data.getStringExtra(QrScanContract.EXTRA_SCAN_RESULT));
    }

    public static String extractMatterSetupPayload(Intent data) {
        if (data == null) {
            return "";
        }
        return extractMatterSetupPayloadText(data.getStringExtra(QrScanContract.EXTRA_SCAN_RESULT));
    }

    public static String extractResultText(String result) {
        return result == null ? "" : result.trim();
    }

    public static String extractMatterSetupPayloadText(String result) {
        String scanResult = extractResultText(result);
        if (scanResult.isEmpty()) {
            return "";
        }
        try {
            return MatterHandoffCodeParser.parseForOpenHabScanInput(scanResult);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
