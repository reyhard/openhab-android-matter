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
- Persisted developer attestation bypass setting is exposed in the UI and passed into native CHIP commissioning requests.
- Encrypted app-private bootstrap controller state repository stores the bootstrap node id and reserves an encrypted opaque controller-state slot.
- Stateful native CHIP command contract passes attestation-bypass intent plus opaque controller state into and out of commissioning and OCW calls.
- Native in-app CameraX QR scanning decodes Matter setup QR payloads with ML Kit barcode scanning.
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Native CHIP bridge metadata distinguishes missing, stub, and production connectedhomeip libraries.
- Debug APK packages a JNI stub `libopenhab_matter_chip.so` for native loading and metadata verification.
- Gradle can package ABI-specific prebuilt `libopenhab_matter_chip.so` replacements instead of the bundled JNI stub.
- Gradle can validate and package official CHIPTool-style connectedhomeip Android controller artifacts supplied as required jars plus ABI-specific `libCHIPController.so` and `libc++_shared.so` files.
- Runtime controller selection can switch from the simulated controller to `ChipMatterController` only when a production native JNI library is bundled and readiness passes.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through the connectedhomeip Android JNI stack.
- Java-side controller orchestration for `AndroidChipPlatform`, `ChipDeviceController.pairDeviceThroughBLE(...)`, and `openPairingWindowWithPINCallback(...)`.
- Real connectedhomeip Matter fabric key persistence and restore; the Java bridge and encrypted repository can carry opaque state, but the packaged native stub does not emit or consume real fabric material.

## Production Replacement Seam

Replace `FakeMatterController` construction in `MainActivity` with `ChipMatterController` after the connectedhomeip Android JNI library is available and reports `kind=connectedhomeip;production=true`.
