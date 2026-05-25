# openhab-android-matter

Android companion app for openHAB Matter pairing.

## Current MVP

This branch builds an installable Android APK with:

- Thread dataset validation.
- Matter setup payload validation for explicit `pin=...;disc=...` input.
- Wi-Fi/multi-admin openHAB handoff validation for Matter QR payloads and Verhoeff-validated 11- or 21-digit manual setup codes.
- Local Matter `MT:` QR setup payload decoding for PIN, discriminator, vendor/product IDs, commissioning flow, and discovery capabilities.
- openHAB REST readiness checking via the configured base URL.
- openHAB Inbox observation via `/rest/inbox` to report whether a Matter Inbox entry is visible.
- openHAB Inbox SSE observation via `/rest/events?topics=openhab/inbox/*`.
- OTBR connectivity diagnostics via a configured base URL.
- Runtime commissioning permission readiness checks for the current Android SDK version.
- Encrypted app-private storage for the OTBR Thread dataset using Android Keystore-backed AES-GCM.
- App-private persistence for the openHAB base URL.
- App-private persistence for the OTBR base URL.
- Persisted developer attestation bypass setting for connectedhomeip commissioning; it defaults off and is passed to the selectable native Matter controller only when explicitly enabled.
- Encrypted app-private bootstrap controller state repository for future native Matter fabric/controller state.
- Legacy native bridge command contract carries attestation-bypass intent and opaque controller state through commissioning and OpenCommissioningWindow results.
- Native in-app Matter QR scanning using CameraX and ML Kit barcode scanning.
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Camera and location hardware features are marked optional so manual-entry and non-camera flows can install on headless emulators or devices without those features.
- Native Matter controller readiness diagnostics check the selectable connectedhomeip Java controller path and keep the simulated controller selected when artifacts or initialization are not ready.
- A packaged JNI stub verifies Android native-library packaging, but it reports `production=false` and is never selected for real commissioning.
- Runtime controller selection can switch from the simulated controller to the reflection-backed connectedhomeip Java controller when official connectedhomeip Android artifacts are bundled and readiness passes.
- A deterministic fake Matter controller that simulates BLE Thread commissioning and OpenCommissioningWindow.
- A native Android UI that displays the temporary code and openHAB Matter Scan Input instructions.

The real connectedhomeip controller path is isolated behind `MatterController`. The debug APK keeps a deterministic simulated controller as the safe fallback and exposes a connectedhomeip controller selection path for builds that include the official Android controller artifacts.

This MVP does not perform real BLE discovery, Thread provisioning, Matter PASE/CASE commissioning, attestation, or real OpenCommissioningWindow calls yet.
Setup payloads, setup PINs, QR payloads, and device credentials are not persisted.
The bootstrap controller state repository persists the bootstrap node id and provides an encrypted opaque state slot; the Java/native command contract can carry updated controller state, but the packaged JNI stub does not emit real connectedhomeip fabric material.

## Build Configuration

This workspace uses:

- Android SDK: `D:\Tools\Android\SDK`
- Android Gradle Plugin: `8.11.1`
- Compile SDK: `36`
- Target SDK: `35`
- Min SDK: `26`
- Java: `17`

## Build

Build and test the debug APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

By default, the APK packages the non-production JNI stub. To package a prebuilt production bridge, pass `-PopenhabMatterChipNativeMode=prebuilt "-PopenhabMatterChipPrebuiltDir=<dir>"` where `<dir>` contains ABI subdirectories with `libopenhab_matter_chip.so`.

To package the official connectedhomeip Android controller artifacts used by the selectable Java controller path, pass `"-PopenhabMatterChipControllerArtifactsDir=<dir>"` where `<dir>` contains the CHIPTool-style jars and native libraries:

```text
CHIPController.jar
CHIPInteractionModel.jar
CHIPClusterID.jar
CHIPClusters.jar
AndroidPlatform.jar
OnboardingPayload.jar
libMatterTlv.jar
libMatterJson.jar
jniLibs\arm64-v8a\libCHIPController.so
jniLibs\arm64-v8a\libc++_shared.so
jniLibs\armeabi-v7a\libCHIPController.so
jniLibs\armeabi-v7a\libc++_shared.so
jniLibs\x86\libCHIPController.so
jniLibs\x86\libc++_shared.so
jniLibs\x86_64\libCHIPController.so
jniLibs\x86_64\libc++_shared.so
```

This local connectedhomeip checkout does not include those prebuilt binaries under `D:\Source\connectedhomeip\examples\android\CHIPTool\app\libs`; it contains README placeholders only. Build the artifacts from connectedhomeip or supply them from a trusted connectedhomeip Android build output.
The Gradle verifier rejects missing native libraries, empty files, corrupt controller jars, and controller jar sets that do not contain the class entries required by runtime readiness, so placeholders cannot pass the packaging gate.

Run the synthetic artifact validation smoke test with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
```

Run the APK badging smoke test with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_apk_badging.ps1
```

The APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install

Connect an Android device with USB debugging enabled, then run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1
```

The helper runs the offline unit tests and debug APK build, verifies `app\build\outputs\apk\debug\app-debug.apk`, lists ready ADB devices, and installs only when exactly one device/emulator is attached. If multiple devices are attached, pass `-Serial <device-id>`. If the APK is already built and you only want the ADB check/install step, pass `-SkipBuild`.

To verify package readiness without an attached device, stop before ADB with:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
```

The same helper can build artifact-specific variants by forwarding Gradle properties:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly -ChipControllerArtifactsDir <artifact-dir>
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly -ChipNativeMode prebuilt -ChipPrebuiltDir <prebuilt-dir>
```

## Real connectedhomeip Controller Work

The selectable connectedhomeip Java controller path now wires the app to reflection-backed equivalents of:

- `commissionBleThread(datasetHex, pin, discriminator)`
- `openCommissioningWindow(nodeId, timeout, discriminator)`

The bundled `libopenhab_matter_chip.so` is still a JNI packaging stub for the legacy native bridge seam. Real commissioning through the current selectable path requires official valid connectedhomeip Android controller jars containing the required controller/platform class entries plus ABI-specific `libCHIPController.so` and `libc++_shared.so` to be packaged with the APK.
Until those connectedhomeip artifacts are bundled and verified on hardware, the app is installable and validates the openHAB user flow, but it falls back to simulation and does not actually provision Matter devices.
Real-device validation still needs a Matter-over-Thread device, OTBR dataset, Android BLE commissioning, OpenCommissioningWindow, and openHAB Inbox confirmation.
