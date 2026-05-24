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
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Native CHIP controller readiness diagnostics report whether the JNI library is available.
- Runtime controller selection can switch from the simulated controller to `ChipMatterController` when the native JNI library is bundled and readiness passes.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Native CameraX QR scanning and in-app QR decoding.
- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through the connectedhomeip Android JNI stack.
- Encrypted storage for future Matter fabric keys.

## Production Replacement Seam

Replace `FakeMatterController` construction in `MainActivity` with `ChipMatterController` after the connectedhomeip Android JNI library is available.
