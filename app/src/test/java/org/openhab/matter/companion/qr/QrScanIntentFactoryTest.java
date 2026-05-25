package org.openhab.matter.companion.qr;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class QrScanIntentFactoryTest {
    @Test
    public void createsQrScannerIntentRequest() {
        QrScanIntentRequest request = QrScanIntentFactory.createScanRequest();

        assertEquals("com.google.zxing.client.android.SCAN", request.action());
        assertEquals("SCAN_MODE", request.scanModeExtra());
        assertEquals("QR_CODE_MODE", request.scanMode());
        assertEquals("SAVE_HISTORY", request.saveHistoryExtra());
        assertEquals(false, request.saveHistory());
        assertEquals("SCAN_RESULT", request.scanResultExtra());
    }

    @Test
    public void extractsScanResultFromActivityData() {
        assertEquals("MT:Y.K9042C00KA0648G00",
                QrScanIntentFactory.extractResultText("  MT:Y.K9042C00KA0648G00  "));
    }

    @Test
    public void missingScanResultReturnsEmptyString() {
        assertEquals("", QrScanIntentFactory.extractResultText(null));
    }

    @Test
    public void validatesSupportedMatterScanResult() {
        assertEquals("MT:Y.K9042C00KA0648G00",
                QrScanIntentFactory.extractMatterSetupPayloadText("  MT:Y.K9042C00KA0648G00  "));
    }

    @Test
    public void validatesManualMatterHandoffScanResult() {
        assertEquals("34970112332",
                QrScanIntentFactory.extractMatterSetupPayloadText("  3497-0112-332  "));
    }

    @Test
    public void rejectsInvalidManualMatterHandoffScanResult() {
        assertEquals("", QrScanIntentFactory.extractMatterSetupPayloadText("34970112333"));
    }

    @Test
    public void createsInAppScanResultIntent() {
        Intent data = QrScanIntentFactory.createScanResultIntent("  MT:Y.K9042C00KA0648G00  ");

        assertEquals("MT:Y.K9042C00KA0648G00", QrScanIntentFactory.extractMatterSetupPayload(data));
    }

    @Test
    public void rejectsUnsupportedMatterScanResult() {
        assertEquals("", QrScanIntentFactory.extractMatterSetupPayloadText("https://example.test/not-matter"));
    }
}
