package org.openhab.matter.companion.qr;

public enum QrScannerZoomOption {
    ONE_X(1f),
    TWO_X(2f);

    private final float requestedRatio;

    QrScannerZoomOption(float requestedRatio) {
        this.requestedRatio = requestedRatio;
    }

    public float zoomRatio(float minZoomRatio, float maxZoomRatio) {
        return Math.max(minZoomRatio, Math.min(requestedRatio, maxZoomRatio));
    }
}
