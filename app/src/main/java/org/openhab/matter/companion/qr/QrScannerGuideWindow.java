package org.openhab.matter.companion.qr;

public final class QrScannerGuideWindow {
    public final float left;
    public final float top;
    public final float right;
    public final float bottom;
    public final float guidelinesLeft;
    public final float guidelinesTop;
    public final float guidelinesRight;
    public final float guidelinesBottom;

    QrScannerGuideWindow(
            float left,
            float top,
            float right,
            float bottom,
            float guidelinesLeft,
            float guidelinesTop,
            float guidelinesRight,
            float guidelinesBottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.guidelinesLeft = guidelinesLeft;
        this.guidelinesTop = guidelinesTop;
        this.guidelinesRight = guidelinesRight;
        this.guidelinesBottom = guidelinesBottom;
    }
}
