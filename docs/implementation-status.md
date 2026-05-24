# Implementation Status

## Implemented

- Android Gradle project builds a debug APK.
- Native Android UI covers Thread, Wi-Fi, and multi-admin handoff flows.
- Thread dataset validation accepts `hex:` and raw hex input.
- Explicit setup payload parser accepts `pin=...;disc=...;vendor=...;product=...`.
- Matter `MT:` QR setup payload parser decodes PIN, discriminator, vendor/product IDs, commissioning flow, and discovery capabilities.
- openHAB REST readiness check calls `/rest/` from a configured base URL.
- openHAB Inbox observation calls `/rest/inbox` from a configured base URL and reports whether a Matter Inbox entry was detected.
- Runtime commissioning permission readiness uses the Android SDK-specific permission plan and requests missing runtime permissions.
- App-private configuration persistence stores the OTBR Thread dataset and openHAB base URL only.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Camera QR scanning.
- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through the connectedhomeip Android JNI stack.
- openHAB SSE streaming of Inbox changes after the user enters the temporary setup code.
- Encrypted storage for Thread datasets and future Matter fabric keys.

## Production Replacement Seam

Replace `FakeMatterController` construction in `MainActivity` with `ChipMatterController` after the connectedhomeip Android JNI library is available.
