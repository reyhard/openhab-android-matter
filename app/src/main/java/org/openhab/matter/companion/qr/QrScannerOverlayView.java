package org.openhab.matter.companion.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.openhab.matter.companion.R;

public final class QrScannerOverlayView extends View {
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable guidelines;

    public QrScannerOverlayView(Context context) {
        super(context);
        dimPaint.setColor(Color.argb(QrScannerOverlayGeometry.DIM_ALPHA, 0, 0, 0));
        guidelines = ContextCompat.getDrawable(context, R.drawable.qr_scan_guidelines);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        QrScannerGuideWindow guideWindow = QrScannerOverlayGeometry.guideWindow(width, height);

        canvas.drawRect(0, 0, width, guideWindow.top, dimPaint);
        canvas.drawRect(0, guideWindow.bottom, width, height, dimPaint);
        canvas.drawRect(0, guideWindow.top, guideWindow.left, guideWindow.bottom, dimPaint);
        canvas.drawRect(guideWindow.right, guideWindow.top, width, guideWindow.bottom, dimPaint);

        if (guidelines != null) {
            guidelines.setBounds(new Rect(
                    Math.round(guideWindow.guidelinesLeft),
                    Math.round(guideWindow.guidelinesTop),
                    Math.round(guideWindow.guidelinesRight),
                    Math.round(guideWindow.guidelinesBottom)));
            guidelines.draw(canvas);
        }
    }
}
