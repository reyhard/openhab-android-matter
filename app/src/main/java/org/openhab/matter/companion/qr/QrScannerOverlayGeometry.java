package org.openhab.matter.companion.qr;

public final class QrScannerOverlayGeometry {
    public static final int DIM_ALPHA = 51;

    private static final float SVG_WIDTH = 1448f;
    private static final float SVG_HEIGHT = 1086f;
    private static final float GUIDE_LEFT = 372f;
    private static final float GUIDE_TOP = 198f;
    private static final float GUIDE_RIGHT = 1083f;
    private static final float GUIDE_BOTTOM = 895f;

    private QrScannerOverlayGeometry() {
    }

    public static QrScannerGuideWindow guideWindow(float width, float height) {
        float scale = Math.min(width / SVG_WIDTH, height / SVG_HEIGHT);
        float guidelinesWidth = SVG_WIDTH * scale;
        float guidelinesHeight = SVG_HEIGHT * scale;
        float guidelinesLeft = (width - guidelinesWidth) / 2f;
        float guidelinesTop = (height - guidelinesHeight) / 2f;
        return new QrScannerGuideWindow(
                guidelinesLeft + GUIDE_LEFT * scale,
                guidelinesTop + GUIDE_TOP * scale,
                guidelinesLeft + GUIDE_RIGHT * scale,
                guidelinesTop + GUIDE_BOTTOM * scale,
                guidelinesLeft,
                guidelinesTop,
                guidelinesLeft + guidelinesWidth,
                guidelinesTop + guidelinesHeight);
    }
}
