# openHAB Matter Android Completion Audit

Date: 2026-05-26

## Objective

Create the Android companion app described in `docs/research.md`: a phone-based helper that can commission Matter-over-Thread devices through BLE and a Thread dataset, open a commissioning window, and guide openHAB pairing through Matter Scan Input.

## Current Evidence

- The debug APK builds and the install helper can verify package readiness without a device.
- The app has a native Android UI for Thread, Wi-Fi, and multi-admin flows.
- The app validates Thread datasets, Matter QR setup payloads, explicit setup payloads, and manual setup/multi-admin handoff codes.
- The app stores the Thread dataset and optional openHAB REST API token in encrypted app-private storage and persists openHAB/OTBR configuration.
- The app can check openHAB REST readiness, require an online Matter controller Thing, observe Inbox state, and watch Inbox SSE events with optional bearer-token authentication.
- The app auto-selects the connectedhomeip Java-controller path when packaged artifacts are present and readiness passes, with reflection seams for platform initialization, BLE connection, Thread commissioning, attestation continuation, OpenCommissioningWindow, and fabric-restore probing.
- Thread commissioning and OpenCommissioningWindow now stop when connectedhomeip is not ready instead of silently running the fake controller path.
- Gradle rejects missing, empty, and structurally invalid connectedhomeip controller artifacts.
- connectedhomeip Android controller artifacts were built from connectedhomeip `v1.5.1.0` and packaged into a debug APK from `D:\Source\Openhab\openhab-android-matter\.artifacts\connectedhomeip-v1.5.1.0-android-arm64`.
- The real-artifact debug APK was installed on Android device `62311e26`; logcat confirmed `libCHIPController.so` loaded and `AndroidChipPlatform JNI_OnLoad()` ran.
- BLE discovery now uses a two-stage Android scan: exact connectedhomeip-style service-data filtering first, then a broad scan that parses Matter service data in app code before reporting "No Matter BLE advertisement found" for the parsed discriminator.
- On real device `62311e26`, the app observed Matter BLE service data `00CC067C11008001`, decoded discriminator `1740`, matched it, and attempted a GATT connection to `E6:7C:CE:5C:07:03`.
- The first real GATT connection attempt failed before service discovery with Android `GATT_Status(62)` / reason `0x003e`; the app now retries status `62` and `133`, but the retry build has not yet been exercised because the phone is currently locked/AOD.
- The app can now be launched with action `org.openhab.matter.companion.action.COMMISSION_THREAD` to start Thread commissioning after the phone is unlocked and the activity is foregroundable.
- After adding the adb launch action and extending the BLE scan timeout, the phone foregrounded the app and ran parsed plus exact Matter scans for discriminator `1740`; both timed out without seeing `FFF6` Matter service data, while Google Play Services still logged FastPair-like advertisement headers. The next runtime attempt needs the target device pairing window reset/reopened.
- connectedhomeip runtime preflight now verifies controller construction and BLE manager callback access without Matter hardware, and native selection is blocked if that preflight fails.
- Camera and location hardware features are optional, so manual flows can install on devices without those features.

## Requirements Audit

| Requirement from `docs/research.md` | Current status | Evidence |
| --- | --- | --- |
| Build an installable Android APK | Implemented for debug builds; latest helper install succeeded on a connected phone | `README.md` build/install section, `scripts/install_debug.ps1`, helper install output on device `62311e26` |
| Thread dataset configuration and storage | Implemented | `ThreadDataset`, `SharedPreferencesAppConfigRepository`, `AndroidKeystoreSecretCodec`, `docs/implementation-status.md` |
| Basic OTBR connectivity check | Implemented as IP/host reachability, with optional HTTP diagnostic URL support | `HttpOtbrClient`, `MainActivity.checkOtbrConnectivity()`, `docs/implementation-status.md` |
| QR/manual Matter setup payload parsing | Implemented for Matter QR, explicit input, and handoff manual codes | `MatterSetupPayloadParser`, `MatterHandoffCodeParser`, parser tests |
| Runtime permission readiness | Implemented | `CommissioningPermissionPlanner`, `MainActivityPresentationTest` |
| BLE Thread commissioning on Android through connectedhomeip | Real artifacts package and native JNI loads; BLE scan and discriminator matching are proven with a real pairing device; GATT connect reached Android status `62` before service discovery and still needs a successful retry/hardware validation | `ConnectedHomeIpMatterController`, `ConnectedHomeIpReflectionGateway`, `ConnectedHomeIpAndroidBleConnectionProvider`, install/logcat evidence on device `62311e26` |
| Attestation handling with developer bypass option | Implemented as a persisted option and reflection seam; production trust-store behavior still needs real artifact validation | `ConnectedHomeIpReflectionAttestationHandler`, `MainActivity`, `docs/chip-jni-integration.md` |
| OpenCommissioningWindow from Android | Offline reflection and result model implemented; not proven on hardware | `ConnectedHomeIpOpenCommissioningWindowCallback`, `ConnectedHomeIpReflectionCommandFactory`, tests |
| Display temporary code and openHAB Scan Input instructions | Implemented for simulated/OCW result path, including QR display when the OCW result includes a QR payload | `OpenHabInstructions`, `MainActivity.runOpenCommissioningWindow()`, `QrCodeMatrix` |
| openHAB Matter readiness and Inbox confirmation | Implemented with optional bearer-token authentication | `HttpOpenHabClient`, `HttpOpenHabInboxClient`, `OpenHabInboxSseClient` |
| Real connectedhomeip artifacts packaged into APK | Implemented and verified for the debug APK using connectedhomeip `v1.5.1.0` Android arm64 artifacts | `docs/chip-jni-integration.md`, `build.gradle`, `scripts/test_connectedhomeip_artifacts.ps1`, `.artifacts/connectedhomeip-v1.5.1.0-android-arm64` |
| Real-device Matter/Thread validation | Not complete | `docs/implementation-status.md` Not Implemented Yet |
| Standalone troubleshooting/help screen | Implemented as an in-app guide action | `OpenHabInstructions.troubleshootingGuide()`, `MainActivity`, `OpenHabInstructionsTest` |
| Temporary QR display for OCW result | Implemented when connectedhomeip returns a QR payload; manual code remains available | `MatterOpenCommissioningWindowResult`, `ConnectedHomeIpOpenCommissioningWindowCallback`, `QrCodeMatrix`, `MainActivity.runOpenCommissioningWindow()` |
| Wi-Fi/generic multi-admin direct openHAB REST pairing | Not required for the copy-paste path and not implemented; current app supports validated handoff to Scan Input | `docs/research.md`, `MatterHandoffCodeParser` |

## Remaining Completion Gates

1. Reset/reopen the target Matter device BLE pairing window, keep the phone unlocked with the app foregrounded, and retry commissioning from the latest APK or with `adb shell am start -n org.openhab.matter.companion/.MainActivity -a org.openhab.matter.companion.action.COMMISSION_THREAD`.
2. Validate Android GATT connection reaches service discovery, MTU request, and connectedhomeip GATT callback forwarding from this APK.
3. Validate PASE, attestation continuation or deliberate developer bypass, and Thread dataset provisioning into the target OTBR network.
4. Validate fabric restore after app restart using the connectedhomeip fabric-restore probe.
5. Validate OpenCommissioningWindow returns a usable temporary manual or QR code on real hardware.
6. Enter that temporary code in openHAB Matter Scan Input and verify the device appears in the openHAB Inbox.
7. Decide whether the phone bootstrap fabric is intentionally ephemeral or must persist full connectedhomeip fabric state across restarts.

## Completion Judgment

The app is installable as a debug APK, packages real connectedhomeip Android controller artifacts, and loads the connectedhomeip native JNI stack on device. The full objective is not complete because real BLE pairing, PASE, Thread provisioning, fabric restore, OpenCommissioningWindow, and openHAB Inbox validation still require Matter/Thread hardware in pairing mode.
