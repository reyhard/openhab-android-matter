package org.openhab.matter.companion.qr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QrScannerOverlayGeometryTest {
    @Test
    public void mapsSvgGuideWindowAtNativeSize() {
        QrScannerGuideWindow rect = QrScannerOverlayGeometry.guideWindow(1448, 1086);

        assertEquals(372f, rect.left, 0.01f);
        assertEquals(198f, rect.top, 0.01f);
        assertEquals(1083f, rect.right, 0.01f);
        assertEquals(895f, rect.bottom, 0.01f);
    }

    @Test
    public void scalesSvgGuideWindowToViewSize() {
        QrScannerGuideWindow rect = QrScannerOverlayGeometry.guideWindow(724, 543);

        assertEquals(186f, rect.left, 0.01f);
        assertEquals(99f, rect.top, 0.01f);
        assertEquals(541.5f, rect.right, 0.01f);
        assertEquals(447.5f, rect.bottom, 0.01f);
    }

    @Test
    public void keepsGuideWindowSquareLikeInPortraitView() {
        QrScannerGuideWindow rect = QrScannerOverlayGeometry.guideWindow(420, 880);

        assertEquals(107.9f, rect.left, 0.1f);
        assertEquals(339.9f, rect.top, 0.1f);
        assertEquals(314.1f, rect.right, 0.1f);
        assertEquals(542.1f, rect.bottom, 0.1f);
    }

    @Test
    public void exposesTwentyPercentDimAlpha() {
        assertEquals(51, QrScannerOverlayGeometry.DIM_ALPHA);
    }
}
