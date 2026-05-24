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
- Runtime commissioning permission readiness uses the Android SDK-specific permission plan and requests missing runtime permissions.
- Android Keystore-backed AES-GCM encrypted app-private storage stores the OTBR Thread dataset.
- App-private configuration persistence stores the openHAB base URL.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.

## Not Implemented Yet

- Camera QR scanning.
- Real BLE scanning, PASE, attestation, Thread dataset provisioning, and OpenCommissioningWindow.
- Real Matter/Thread commissioning through the connectedhomeip Android JNI stack.
- Encrypted storage for future Matter fabric keys.

## Production Replacement Seam

Replace `FakeMatterController` construction in `MainActivity` with `ChipMatterController` after the connectedhomeip Android JNI library is available.
