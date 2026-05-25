# Native QR Scanner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the QR-scanning gap with a real in-app CameraX scanner that decodes Matter QR payloads and populates the existing setup payload field, while keeping the external scanner handoff as fallback.

**Architecture:** Enable AndroidX and add CameraX preview/image-analysis plus ML Kit bundled barcode scanning. Implement a dedicated `InAppQrScannerActivity` that returns the same `SCAN_RESULT` extra consumed by existing QR parsing logic, so `MainActivity` can share result validation with the external scanner flow. Keep all setup-code validation in `QrScanIntentFactory`/`MatterSetupPayloadParser` and never log raw QR/PIN payloads.

**Tech Stack:** Java 17, AndroidX CameraX, ML Kit Barcode Scanning bundled model, Android runtime `CAMERA` permission, existing Android Gradle project.

---

## File Structure

- Modify `gradle.properties` to set `android.useAndroidX=true`.
- Modify `app/build.gradle` to add CameraX, AndroidX core/activity, and ML Kit barcode dependencies.
- Modify `app/src/main/AndroidManifest.xml` to add `android.permission.CAMERA` and declare `.qr.InAppQrScannerActivity`.
- Create `app/src/main/java/org/openhab/matter/companion/qr/InAppQrScannerActivity.java`.
- Modify `app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java` to create scanner result intents.
- Modify `app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java` for in-app result intent extraction.
- Modify `app/src/main/java/org/openhab/matter/companion/MainActivity.java` to add an in-app scan button, request camera permission, and handle the result.
- Modify `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` and tests for camera permission denial/failure text.
- Modify `README.md` and `docs/implementation-status.md` to move native QR scanning to implemented.

## Task 1: Dependencies And Result Contract

**Files:**
- Modify: `gradle.properties`
- Modify: `app/build.gradle`
- Modify: `app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java`

- [ ] **Step 1: Write failing result-intent test**

Add a test:

```java
@Test
public void createsInAppScanResultIntent() {
    Intent data = QrScanIntentFactory.createScanResultIntent("  MT:Y.K9042C00KA0648G00  ");

    assertEquals("MT:Y.K9042C00KA0648G00", QrScanIntentFactory.extractMatterSetupPayload(data));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.qr.QrScanIntentFactoryTest
```

Expected: compilation fails because `createScanResultIntent` does not exist.

- [ ] **Step 3: Add contract helper and dependencies**

Set:

```properties
android.useAndroidX=true
```

Add dependencies:

```gradle
def camerax_version = "1.6.1"
implementation "androidx.core:core:1.16.0"
implementation "androidx.activity:activity:1.10.1"
implementation "androidx.camera:camera-core:$camerax_version"
implementation "androidx.camera:camera-camera2:$camerax_version"
implementation "androidx.camera:camera-lifecycle:$camerax_version"
implementation "androidx.camera:camera-view:$camerax_version"
implementation "com.google.mlkit:barcode-scanning:17.3.0"
```

Add:

```java
public static Intent createScanResultIntent(String result) {
    Intent intent = new Intent();
    intent.putExtra(QrScanContract.EXTRA_SCAN_RESULT, extractResultText(result));
    return intent;
}
```

- [ ] **Step 4: Resolve/build dependencies**

Run without `--offline` if dependencies are not cached:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests org.openhab.matter.companion.qr.QrScanIntentFactoryTest
```

Expected: dependencies resolve and focused tests pass.

- [ ] **Step 5: Commit**

```powershell
git add gradle.properties app/build.gradle app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java
git commit -m "feat: add native QR scanner dependencies"
```

## Task 2: Scanner Activity

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/org/openhab/matter/companion/qr/InAppQrScannerActivity.java`

- [ ] **Step 1: Add manifest permission and activity**

Add:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

and:

```xml
<activity
    android:name=".qr.InAppQrScannerActivity"
    android:exported="false" />
```

- [ ] **Step 2: Implement scanner activity**

Activity requirements:
- Extend `androidx.activity.ComponentActivity`.
- Build UI programmatically using `PreviewView`, instruction text, and cancel button.
- Bind `Preview` and `ImageAnalysis` to back camera through `ProcessCameraProvider`.
- Use ML Kit `BarcodeScannerOptions` limited to `Barcode.FORMAT_QR_CODE`.
- For each barcode raw value, call `QrScanIntentFactory.extractMatterSetupPayloadText(rawValue)`.
- If valid, call `setResult(RESULT_OK, QrScanIntentFactory.createScanResultIntent(payload))` and `finish()`.
- Close each `ImageProxy` in an `addOnCompleteListener`.
- Shut down executor and close scanner in `onDestroy`.

- [ ] **Step 3: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

Expected: compile succeeds.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/org/openhab/matter/companion/qr/InAppQrScannerActivity.java
git commit -m "feat: add in-app QR scanner activity"
```

## Task 3: MainActivity Wiring And Docs

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Add failing presentation tests**

Add tests for:

```java
assertEquals("Camera permission is required for in-app Matter QR scanning.",
        MainActivityPresentation.cameraPermissionRequired());
assertEquals("In-app QR scanner did not return a supported Matter setup payload.",
        MainActivityPresentation.invalidInAppQrScannerResult());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected: compilation fails because methods do not exist.

- [ ] **Step 3: Wire in-app scanner**

MainActivity requirements:
- Add request code `IN_APP_QR_SCAN_REQUEST`.
- Add request code `CAMERA_PERMISSION_REQUEST`.
- Add button `Scan Matter QR in app`.
- If `CAMERA` is granted, launch `InAppQrScannerActivity`.
- Otherwise request `CAMERA`.
- In `onRequestPermissionsResult`, launch scanner when camera is granted; log `cameraPermissionRequired()` when denied.
- In `onActivityResult`, handle in-app result by extracting payload through `QrScanIntentFactory.extractMatterSetupPayload(data)` and populating `payloadInput`.

- [ ] **Step 4: Update docs**

README:
- Change native QR scanning from not implemented to implemented.
- Keep external scanner as fallback.

Implementation status:
- Move native CameraX QR scanning/in-app QR decoding to implemented.

- [ ] **Step 5: Run full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java README.md docs/implementation-status.md
git commit -m "feat: wire in-app QR scanner"
```

## Self-Review Notes

- CameraX 1.6.1 includes the AndroidX Camera 1.6.x `ListenableFuture` compile fix; ML Kit docs list bundled barcode scanning `com.google.mlkit:barcode-scanning:17.3.0`.
- This plan requires online dependency resolution once because CameraX and ML Kit are not present in the local Gradle cache.
- This plan does not implement real BLE/PASE/CASE/Thread provisioning or connectedhomeip; it removes the native QR scanning gap only.
