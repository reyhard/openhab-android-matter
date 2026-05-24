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
- Runtime commissioning permission readiness checks for the current Android SDK version.
- Encrypted app-private storage for the OTBR Thread dataset using Android Keystore-backed AES-GCM.
- App-private persistence for the openHAB base URL.
- A deterministic fake Matter controller that simulates BLE Thread commissioning and OpenCommissioningWindow.
- A native Android UI that displays the temporary code and openHAB Matter Scan Input instructions.

The real connectedhomeip/CHIP JNI controller is intentionally isolated behind `MatterController`. `ChipMatterController` is present as the replacement point for the native implementation.

This MVP does not perform Camera QR scanning, real BLE discovery, Thread provisioning, Matter PASE/CASE commissioning, attestation, or real OpenCommissioningWindow calls yet.
Setup payloads, setup PINs, QR payloads, Matter fabric keys, and device credentials are not persisted.

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

Until that native library is bundled, the app is installable and validates the openHAB user flow, but it does not actually provision Matter devices.
