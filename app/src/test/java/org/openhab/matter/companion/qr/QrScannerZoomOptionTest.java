package org.openhab.matter.companion.qr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QrScannerZoomOptionTest {
    @Test
    public void oneXUsesNeutralCameraZoom() {
        assertEquals(1f, QrScannerZoomOption.ONE_X.zoomRatio(1f, 4f), 0.01f);
    }

    @Test
    public void twoXRequestsTwoTimesZoomWhenSupported() {
        assertEquals(2f, QrScannerZoomOption.TWO_X.zoomRatio(1f, 4f), 0.01f);
    }

    @Test
    public void twoXClampsToCameraMaximumWhenNeeded() {
        assertEquals(1.5f, QrScannerZoomOption.TWO_X.zoomRatio(1f, 1.5f), 0.01f);
    }
}
