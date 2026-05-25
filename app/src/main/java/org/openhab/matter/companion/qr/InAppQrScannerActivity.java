package org.openhab.matter.companion.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InAppQrScannerActivity extends ComponentActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 4101;

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final BarcodeScanner scanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build());
    private ProcessCameraProvider cameraProvider;
    private volatile boolean resultDelivered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        showScanner();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION_REQUEST) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showScanner();
            return;
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private void showScanner() {
        PreviewView previewView = new PreviewView(this);
        previewView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView instructions = new TextView(this);
        instructions.setText("Point the camera at a Matter QR code.");
        instructions.setTextColor(Color.WHITE);
        instructions.setTextSize(18);
        instructions.setGravity(Gravity.CENTER);
        instructions.setBackgroundColor(Color.argb(150, 0, 0, 0));
        instructions.setPadding(dp(16), dp(12), dp(16), dp(12));
        FrameLayout.LayoutParams instructionsParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        cancelParams.setMargins(0, 0, 0, dp(24));

        FrameLayout root = new FrameLayout(this);
        root.addView(previewView);
        root.addView(instructions, instructionsParams);
        root.addView(cancel, cancelParams);
        setContentView(root);

        startCamera(previewView);
    }

    @Override
    protected void onDestroy() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        scanner.close();
        cameraExecutor.shutdown();
        super.onDestroy();
    }

    private void startCamera(PreviewView previewView) {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindCameraUseCases(previewView);
            } catch (Exception ex) {
                setResult(RESULT_CANCELED);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(PreviewView previewView) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis);
    }

    @ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        if (resultDelivered) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees());
        try {
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String payload = QrScanIntentFactory.extractMatterSetupPayloadText(barcode.getRawValue());
                            if (!payload.isEmpty()) {
                                deliverResult(payload);
                                return;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (RuntimeException ex) {
            imageProxy.close();
        }
    }

    private void deliverResult(String payload) {
        if (resultDelivered) {
            return;
        }
        resultDelivered = true;
        runOnUiThread(() -> {
            setResult(RESULT_OK, QrScanIntentFactory.createScanResultIntent(payload));
            finish();
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
