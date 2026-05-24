package org.openhab.matter.companion.qr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QrScanIntentFactoryTest {
    @Test
    public void createsQrScannerIntentRequest() {
        QrScanIntentRequest request = QrScanIntentFactory.createScanRequest();

        assertEquals("com.google.zxing.client.android.SCAN", request.action());
        assertEquals("QR_CODE_MODE", request.scanMode());
        assertEquals(false, request.saveHistory());
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
}
