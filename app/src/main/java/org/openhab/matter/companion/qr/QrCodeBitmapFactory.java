package org.openhab.matter.companion.qr;

import android.graphics.Bitmap;
import android.graphics.Color;

public final class QrCodeBitmapFactory {
    private static final int QUIET_ZONE_MODULES = 4;

    private QrCodeBitmapFactory() {
    }

    public static Bitmap createBitmap(String text, int moduleScale) {
        if (moduleScale <= 0) {
            throw new IllegalArgumentException("moduleScale must be positive");
        }
        QrCodeMatrix matrix = QrCodeMatrix.encodeText(text);
        boolean[][] modules = matrix.modules();
        int moduleCount = matrix.size() + QUIET_ZONE_MODULES * 2;
        int pixelSize = moduleCount * moduleScale;
        Bitmap bitmap = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        for (int row = 0; row < matrix.size(); row++) {
            for (int column = 0; column < matrix.size(); column++) {
                if (modules[row][column]) {
                    fillModule(bitmap, row + QUIET_ZONE_MODULES, column + QUIET_ZONE_MODULES, moduleScale);
                }
            }
        }
        return bitmap;
    }

    private static void fillModule(Bitmap bitmap, int moduleRow, int moduleColumn, int moduleScale) {
        int startY = moduleRow * moduleScale;
        int startX = moduleColumn * moduleScale;
        for (int y = 0; y < moduleScale; y++) {
            for (int x = 0; x < moduleScale; x++) {
                bitmap.setPixel(startX + x, startY + y, Color.BLACK);
            }
        }
    }
}
