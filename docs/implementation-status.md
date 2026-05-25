# Implementation Status

## Implemented

- Android Gradle project builds a debug APK.
- Native Android UI covers Thread, Wi-Fi, and multi-admin handoff flows.
- Thread dataset validation accepts `hex:` and raw hex input.
- Explicit setup payload parser accepts `pin=...;disc=...;vendor=...;product=...`.
- Matter `MT:` QR setup payload parser decodes PIN, discriminator, vendor/product IDs, commissioning flow, and discovery capabilities.
- openHAB REST readiness check calls `/rest/` from a configured base URL.
- openHAB Inbox observation calls `/rest/inbox` from a configured base URL and reports whether a Matter Inbox entry was detected.
- openHAB SSE streaming observes `/rest/events?topics=openhab/inbox/*` and reports Matter Inbox events.
- OTBR connectivity diagnostics check a configured base URL before Thread commissioning.
- Runtime commissioning permission readiness uses the Android SDK-specific permission plan and requests missing runtime permissions.
- Android Keystore-backed AES-GCM encrypted app-private storage stores the OTBR Thread dataset.
- App-private configuration persistence stores the openHAB base URL.
- App-private configuration persistence stores the OTBR base URL.
- Persisted developer attestation bypass setting is exposed in the UI and passed into connectedhomeip/native Matter commissioning requests.
- Encrypted app-private bootstrap controller state repository stores the bootstrap node id and reserves an encrypted opaque controller-state slot.
- Legacy native bridge command contract passes attestation-bypass intent plus opaque controller state into and out of commissioning and OCW calls.
- Java-side connectedhomeip controller command seam maps this app's `MatterController` commands to the reflection-backed `ChipDeviceController` gateway, including Thread dataset, PIN, discriminator, attestation bypass, controller state, and OCW iteration inputs.
- Thread datasets can be converted from normalized hex to `byte[]` for connectedhomeip `NetworkCredentials.ThreadCredentials`.
- Reflection command factory can construct connectedhomeip Thread `NetworkCredentials`, `CommissionParameters`, and locate the `pairDeviceThroughBLE(...)` and `openPairingWindowWithPINCallback(...)` controller methods without compile-time CHIP dependencies.
- Reflection command factory can invoke `pairDeviceThroughBLE(...)` and bridge `OpenCommissioningCallback` success/error responses into the app's OCW result model.
- Reflection-backed `ConnectedHomeIpControllerGateway` orchestration invokes the connectedhomeip BLE Thread and OCW commands through injectable controller, BLE connection, node-id, attestation-handler, commissioning-monitor, and connected-device-pointer seams.
- Reflection bridges cover connectedhomeip `CompletionListener`, `DeviceAttestationDelegate`, `continueCommissioning(...)`, `getConnectedDevicePointer(...)`, and `releaseConnectedDevicePointer(...)`.
- Reflection commissioning monitor installs a fresh connectedhomeip completion listener before each BLE pairing command, preventing stale one-shot callback state across commissioning attempts.
- Reflection-backed controller provider can initialize the connectedhomeip Android platform dependency graph, build `ControllerParams`, and construct/cache `ChipDeviceController` without compile-time CHIP dependencies.
- Android BLE connection provider can build the Matter BLE discriminator scan filter, scan/connect through injectable Android BLE seams, register the connected `BluetoothGatt` with connectedhomeip's BLE manager, and clean up registered GATT connections.
- Connectedhomeip artifact readiness checks use non-initializing class lookup, include reflected nested classes and BLE callback classes, and report linkage/inspection failures as not-ready fallback instead of crashing the app.
- Native in-app CameraX QR scanning decodes Matter setup QR payloads with ML Kit barcode scanning.
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Native CHIP bridge metadata distinguishes missing, stub, and production connectedhomeip libraries.
- Debug APK packages a JNI stub `libopenhab_matter_chip.so` for native loading and metadata verification.
- Gradle can package ABI-specific prebuilt `libopenhab_matter_chip.so` replacements instead of the bundled JNI stub.
- Gradle can validate and package official CHIPTool-style connectedhomeip Android controller artifacts supplied as required jars plus ABI-specific `libCHIPController.so` and `libc++_shared.so` files.
- Runtime controller selection can switch from the simulated controller to the reflection-backed connectedhomeip Java controller only when connectedhomeip artifacts are present and readiness passes; missing or failed initialization stays on the simulated controller.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Real-device validation of BLE scanning, PASE, attestation handling, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through packaged connectedhomeip Android controller artifacts and their Java/JNI stack.
- Real-device validation of Android BLE scan/connect and GATT callback forwarding, plus real fabric restore/persistence.
- Real connectedhomeip Matter fabric key persistence and restore; the Java bridge and encrypted repository can carry opaque state, but the packaged native stub does not emit or consume real fabric material.

## Production Controller Seam

`MainActivity` keeps `FakeMatterController` as the safe fallback and uses `ConnectedHomeIpMatterControllerFactory` as the selectable native Matter candidate. The factory only selects the reflection-backed connectedhomeip Java controller when official connectedhomeip Android controller artifacts are present, linkage-safe readiness passes, and gateway initialization succeeds.

The packaged `libopenhab_matter_chip.so` remains a non-production JNI stub for the legacy bridge seam. Real commissioning now depends on packaging and validating the official connectedhomeip Android controller jars/native libraries, then verifying BLE Thread commissioning and OpenCommissioningWindow on hardware.
