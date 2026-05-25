# openhab-android-matter

Android companion app for openHAB Matter pairing.

## Current MVP

This branch builds an installable Android APK with:

- Thread dataset validation.
- Matter setup payload validation for explicit `pin=...;disc=...` input.
- Local Matter `MT:` QR setup payload decoding for PIN, discriminator, vendor/product IDs, commissioning flow, and discovery capabilities.
- openHAB REST readiness checking via the configured base URL.
- openHAB Inbox observation via `/rest/inbox` to report whether a Matter Inbox entry is visible.
- openHAB Inbox SSE observation via `/rest/events?topics=openhab/inbox/*`.
- OTBR connectivity diagnostics via a configured base URL.
- Runtime commissioning permission readiness checks for the current Android SDK version.
- Encrypted app-private storage for the OTBR Thread dataset using Android Keystore-backed AES-GCM.
- App-private persistence for the openHAB base URL.
- App-private persistence for the OTBR base URL.
- Persisted developer attestation bypass setting for the future native CHIP controller path; it defaults off and is passed to the JNI command contract only when explicitly enabled.
- Encrypted app-private bootstrap controller state repository for future native Matter fabric/controller state.
- Stateful native CHIP command contract carries attestation-bypass intent and opaque controller state through commissioning and OpenCommissioningWindow results.
- Native in-app Matter QR scanning using CameraX and ML Kit barcode scanning.
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Native CHIP controller readiness diagnostics load `libopenhab_matter_chip.so` and require bridge metadata that declares a production connectedhomeip implementation.
- A packaged JNI stub verifies Android native-library packaging, but it reports `production=false` and is never selected for real commissioning.
- Runtime controller selection can switch from the simulated controller to `ChipMatterController` only when a production native JNI library is bundled and readiness passes.
- A deterministic fake Matter controller that simulates BLE Thread commissioning and OpenCommissioningWindow.
- A native Android UI that displays the temporary code and openHAB Matter Scan Input instructions.

The real connectedhomeip/CHIP JNI controller is intentionally isolated behind `MatterController`. `ChipMatterController` is present as the replacement point for the native implementation.

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

To also package the official connectedhomeip Android controller artifacts for the future Java-side controller integration, pass `"-PopenhabMatterChipControllerArtifactsDir=<dir>"` where `<dir>` contains the CHIPTool-style jars and native libraries:

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

The APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install

Connect an Android device with USB debugging enabled, then run:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

## Real CHIP Controller Work

The remaining production work is to replace `FakeMatterController` with a connectedhomeip-backed `ChipMatterController` that implements:

- `commissionBleThread(datasetHex, pin, discriminator)`
- `openCommissioningWindow(nodeId, timeout, discriminator)`

The bundled `libopenhab_matter_chip.so` is currently a JNI packaging stub. A production replacement must return metadata like `kind=connectedhomeip;production=true;...` and implement real BLE Thread commissioning plus OpenCommissioningWindow before the selector will use it.
The official connectedhomeip Android controller artifacts can now be validated and packaged separately, but the app still needs the actual Java controller orchestration that initializes `AndroidChipPlatform`, performs BLE Thread commissioning, and opens the commissioning window.
Until that production connectedhomeip library is bundled, the app is installable and validates the openHAB user flow, but it does not actually provision Matter devices.
When that native library is added, its persistent controller/fabric material flows through the encrypted bootstrap state repository instead of logs or plaintext app config.
