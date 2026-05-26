# openHAB Matter Android Completion Audit

Date: 2026-05-25

## Objective

Create the Android companion app described in `docs/research.md`: a phone-based helper that can commission Matter-over-Thread devices through BLE and a Thread dataset, open a commissioning window, and guide openHAB pairing through Matter Scan Input.

## Current Evidence

- The debug APK builds and the install helper can verify package readiness without a device.
- The app has a native Android UI for Thread, Wi-Fi, and multi-admin flows.
- The app validates Thread datasets, Matter QR setup payloads, explicit setup payloads, and manual setup/multi-admin handoff codes.
- The app stores the Thread dataset and optional openHAB REST API token in encrypted app-private storage and persists openHAB/OTBR configuration.
- The app can check openHAB REST readiness, require an online Matter controller Thing, observe Inbox state, and watch Inbox SSE events with optional bearer-token authentication.
- The app has a selectable connectedhomeip Java-controller path behind a simulated fallback, with reflection seams for platform initialization, BLE connection, Thread commissioning, attestation continuation, OpenCommissioningWindow, and fabric-restore probing.
- Gradle rejects missing, empty, and structurally invalid connectedhomeip controller artifacts.
- Camera and location hardware features are optional, so manual flows can install on devices without those features.

## Requirements Audit

| Requirement from `docs/research.md` | Current status | Evidence |
| --- | --- | --- |
| Build an installable Android APK | Implemented for debug builds; latest helper install succeeded on a connected phone | `README.md` build/install section, `scripts/install_debug.ps1`, helper install output on device `62311e26` |
| Thread dataset configuration and storage | Implemented | `ThreadDataset`, `SharedPreferencesAppConfigRepository`, `AndroidKeystoreSecretCodec`, `docs/implementation-status.md` |
| Basic OTBR connectivity check | Implemented as IP/host reachability, with optional HTTP diagnostic URL support | `HttpOtbrClient`, `MainActivity.checkOtbrConnectivity()`, `docs/implementation-status.md` |
| QR/manual Matter setup payload parsing | Implemented for Matter QR, explicit input, and handoff manual codes | `MatterSetupPayloadParser`, `MatterHandoffCodeParser`, parser tests |
| Runtime permission readiness | Implemented | `CommissioningPermissionPlanner`, `MainActivityPresentationTest` |
| BLE Thread commissioning on Android through connectedhomeip | Offline integration seams implemented; not proven on hardware | `ConnectedHomeIpMatterController`, `ConnectedHomeIpReflectionGateway`, `docs/chip-jni-integration.md` |
| Attestation handling with developer bypass option | Implemented as a persisted option and reflection seam; production trust-store behavior still needs real artifact validation | `ConnectedHomeIpReflectionAttestationHandler`, `MainActivity`, `docs/chip-jni-integration.md` |
| OpenCommissioningWindow from Android | Offline reflection and result model implemented; not proven on hardware | `ConnectedHomeIpOpenCommissioningWindowCallback`, `ConnectedHomeIpReflectionCommandFactory`, tests |
| Display temporary code and openHAB Scan Input instructions | Implemented for simulated/OCW result path, including QR display when the OCW result includes a QR payload | `OpenHabInstructions`, `MainActivity.runOpenCommissioningWindow()`, `QrCodeMatrix` |
| openHAB Matter readiness and Inbox confirmation | Implemented with optional bearer-token authentication | `HttpOpenHabClient`, `HttpOpenHabInboxClient`, `OpenHabInboxSseClient` |
| Real connectedhomeip artifacts packaged into APK | Packaging gates implemented; real artifacts are not present in this workspace | `docs/chip-jni-integration.md`, `build.gradle`, `scripts/test_connectedhomeip_artifacts.ps1` |
| Real-device Matter/Thread validation | Not complete | `docs/implementation-status.md` Not Implemented Yet |
| Standalone troubleshooting/help screen | Implemented as an in-app guide action | `OpenHabInstructions.troubleshootingGuide()`, `MainActivity`, `OpenHabInstructionsTest` |
| Temporary QR display for OCW result | Implemented when connectedhomeip returns a QR payload; manual code remains available | `MatterOpenCommissioningWindowResult`, `ConnectedHomeIpOpenCommissioningWindowCallback`, `QrCodeMatrix`, `MainActivity.runOpenCommissioningWindow()` |
| Wi-Fi/generic multi-admin direct openHAB REST pairing | Not required for the copy-paste path and not implemented; current app supports validated handoff to Scan Input | `docs/research.md`, `MatterHandoffCodeParser` |

## Remaining Completion Gates

1. Build or obtain valid connectedhomeip Android controller artifacts.
2. Package those artifacts through `-PopenhabMatterChipControllerArtifactsDir=$ArtifactDir` and verify Gradle gates pass.
3. Install the debug APK on an Android device with BLE and location capability.
4. Validate Android BLE scan/connect against a real Matter-over-Thread device in pairing mode.
5. Validate PASE, attestation continuation or deliberate developer bypass, and Thread dataset provisioning into the target OTBR network.
6. Validate fabric restore after app restart using the connectedhomeip fabric-restore probe.
7. Validate OpenCommissioningWindow returns a usable temporary manual or QR code on real hardware.
8. Enter that temporary code in openHAB Matter Scan Input and verify the device appears in the openHAB Inbox.
9. Decide whether the phone bootstrap fabric is intentionally ephemeral or must persist full connectedhomeip fabric state across restarts.

## Completion Judgment

The app is installable as a debug APK and has strong offline-verifiable seams for the target architecture. The full objective is not complete because real connectedhomeip artifacts and real Matter/Thread hardware validation are still missing.
