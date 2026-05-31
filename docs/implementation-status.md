# Implementation Status

## Implemented

- Android Gradle project builds a debug APK.
- PowerShell install-readiness helper builds/tests the debug APK, forwards connectedhomeip/native prebuilt Gradle properties, verifies the APK artifact, supports no-device preflight mode, checks ADB devices, and installs only when a target device/emulator is unambiguous.
- Native Android UI covers Thread, Wi-Fi, and multi-admin handoff flows.
- Thread dataset validation accepts `hex:` and raw hex input.
- Compose settings expose Thread dataset manual entry, local dataset validation, encrypted save, OTBR diagnostic target editing, developer attestation bypass, and automatic `_meshcop._udp` Thread Border Router discovery.
- Explicit setup payload parser accepts `pin=...;disc=...;vendor=...;product=...`.
- Wi-Fi/multi-admin handoff accepts Matter QR payloads and normalized Verhoeff-validated 11- or 21-digit manual setup or multi-admin codes for openHAB Scan Input.
- Matter `MT:` QR setup payload parser decodes PIN, discriminator, vendor/product IDs, commissioning flow, and discovery capabilities.
- openHAB readiness check calls `/rest/` and verifies an online Matter controller through `/rest/things`, using a configured bearer token when provided.
- openHAB Inbox observation calls `/rest/inbox` from a configured base URL and reports whether a Matter Inbox entry was detected, using a configured bearer token when provided.
- openHAB SSE streaming observes `/rest/events?topics=openhab/inbox/*` and reports Matter Inbox events, using a configured bearer token when provided.
- OpenCommissioningWindow success can automatically submit the returned 11-digit manual setup code to openHAB Matter discovery scan input through `/rest/discovery/bindings/matter/scan?input=...`, using the configured bearer token and then observing Inbox status.
- OTBR connectivity diagnostics check a configured IP/host address or optional HTTP diagnostic URL before Thread commissioning.
- Standalone troubleshooting guide in the app covers openHAB Matter binding/controller setup, REST token authentication, OTBR address expectations, IPv6 routing, mDNS/Avahi, connectedhomeip artifact packaging, and Scan Input handoff.
- Runtime commissioning permission readiness uses the Android SDK-specific permission plan and requests missing runtime permissions.
- Android Keystore-backed AES-GCM encrypted app-private storage stores the OTBR Thread dataset.
- App-private configuration persistence stores the openHAB base URL and encrypted openHAB REST API token.
- App-private configuration persistence stores the OTBR address or diagnostic URL.
- Persisted developer attestation bypass setting is exposed in the UI and passed into connectedhomeip/native Matter commissioning requests.
- Encrypted app-private bootstrap controller state repository stores the bootstrap node id and reserves an encrypted opaque controller-state slot.
- Legacy native bridge command contract passes attestation-bypass intent plus opaque controller state into and out of commissioning and OCW calls.
- Java-side connectedhomeip controller command seam maps this app's `MatterController` commands to the reflection-backed `ChipDeviceController` gateway, including Thread dataset, PIN, discriminator, attestation bypass, controller state, and OCW iteration inputs.
- Thread datasets can be converted from normalized hex to `byte[]` for connectedhomeip `NetworkCredentials.ThreadCredentials`.
- Reflection command factory can construct connectedhomeip Thread `NetworkCredentials`, `CommissionParameters`, and locate the `pairDeviceThroughBLE(...)` and `openPairingWindowWithPINCallback(...)` controller methods without compile-time CHIP dependencies.
- Reflection command factory can invoke `pairDeviceThroughBLE(...)` and bridge `OpenCommissioningCallback` success/error responses, including manual and QR setup codes, into the app's OCW result model.
- OpenCommissioningWindow results can display a temporary Matter setup QR code in the app when connectedhomeip returns one, and expose a copy button for the returned manual setup code for openHAB Scan Input.
- Reflection-backed `ConnectedHomeIpControllerGateway` orchestration invokes the connectedhomeip BLE Thread and OCW commands through injectable controller, BLE connection, node-id, attestation-handler, commissioning-monitor, and connected-device-pointer seams.
- Reflection bridges cover connectedhomeip `CompletionListener`, `DeviceAttestationDelegate`, `continueCommissioning(...)`, `getConnectedDevicePointer(...)`, and `releaseConnectedDevicePointer(...)`.
- Reflection-backed attestation trust-store setup loads packaged PAA/CD assets and applies `setAttestationTrustStoreDelegate(...)` before commissioning while preserving developer attestation-bypass behavior.
- OpenCommissioningWindow uses a concrete connectedhomeip callback class when packaged, avoiding the JNI return-type mismatch observed with a dynamic proxy callback on real Android hardware.
- Reflection commissioning monitor installs a fresh connectedhomeip completion listener before each BLE pairing command, preventing stale one-shot callback state across commissioning attempts.
- Reflection-backed controller provider can initialize the connectedhomeip Android platform dependency graph, build `ControllerParams`, and construct/cache `ChipDeviceController` without compile-time CHIP dependencies.
- Android BLE connection provider can build the Matter BLE discriminator scan filter, scan/connect through injectable Android BLE seams, register the connected `BluetoothGatt` with connectedhomeip's BLE manager, and clean up registered GATT connections.
- Android BLE scanning first uses the connectedhomeip-style exact service-data filter, then falls back to a broad foreground scan that parses Matter service data in app code before reporting that no Matter BLE advertisement was found for the target discriminator.
- Android BLE scanning accepts both connectedhomeip-style and Android-observed Matter service-data first-byte variants while matching the long discriminator bytes.
- Android BLE scan timeout is extended for sparse/intermittent Matter pairing advertisements.
- Android BLE GATT connection attempts retry observed retryable connection failures (`62`) and Android's generic retryable GATT failure (`133`) before reporting failure.
- The main activity keeps the screen awake while foregrounded and supports an explicit adb launch action (`org.openhab.matter.companion.action.COMMISSION_THREAD`) to trigger Thread commissioning through the same UI flow after the phone is unlocked.
- connectedhomeip fabric-restore diagnostic seam is exposed through a user-triggered app check and can probe a persisted bootstrap node id by acquiring and immediately releasing a connected device pointer, so real artifact builds can distinguish restored fabric readiness from later OCW failures.
- connectedhomeip runtime preflight can construct the packaged Java/JNI controller stack and validate BLE manager callback access without Matter hardware; native selection is refused if this preflight fails.
- Unreadable encrypted Matter bootstrap state now fails closed instead of reusing a stale saved-instance bootstrap node id for OpenCommissioningWindow.
- Connectedhomeip artifact readiness checks use non-initializing class lookup, include reflected nested classes and BLE callback classes, and report linkage/inspection failures as not-ready fallback instead of crashing the app.
- Native in-app CameraX QR scanning decodes Matter setup QR payloads with ML Kit barcode scanning.
- External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.
- Camera and location hardware features are declared optional in the APK manifest and checked by an offline `aapt dump badging` smoke test.
- Native CHIP bridge metadata distinguishes missing, stub, and production connectedhomeip libraries.
- Debug APK packages a JNI stub `libopenhab_matter_chip.so` for native loading and metadata verification.
- Gradle can package ABI-specific prebuilt `libopenhab_matter_chip.so` replacements instead of the bundled JNI stub.
- Gradle can validate and package official CHIPTool-style connectedhomeip Android controller artifacts supplied as valid non-empty required jars containing the controller/platform class entries probed at runtime plus ABI-specific `libCHIPController.so` and `libc++_shared.so` files, with a PowerShell smoke helper for the validation contract.
- Runtime controller selection automatically switches from the fake controller to the reflection-backed connectedhomeip Java controller when connectedhomeip artifacts are present and readiness passes.
- Thread commissioning and OpenCommissioningWindow require the connectedhomeip Java controller at execution time and stop instead of silently using the fake controller when connectedhomeip is not ready.
- connectedhomeip `v1.5.1.0` Android arm64 controller artifacts were built, packaged into the debug APK, installed on Android device `62311e26`, and observed in logcat loading `libCHIPController.so` plus AndroidChipPlatform JNI.
- Real Android/Thread device validation succeeded on device `62311e26`: BLE Thread commissioning completed through connectedhomeip, the app restored the commissioned node id, acquired an operational device pointer over IPv6, opened the commissioning window, displayed/copy-ready the returned manual setup code and QR code, and openHAB pairing worked after stale mDNS/Avahi records were cleared.
- Deterministic fake Matter controller simulates BLE Thread commissioning and OCW.
- Kotlin/Jetpack Compose automated Matter setup UI guides users through openHAB setup, QR scan, pairing-mode confirmation, Thread commissioning, OpenCommissioningWindow, openHAB scan submission, and Inbox detection.
- The automated setup workflow exposes user-facing progress states and a 300-second commissioning-window countdown while preserving the connectedhomeip fail-closed readiness gate.
- Real-device validation of the Compose automated setup entry point succeeded across QR scan, BLE Thread commissioning, OpenCommissioningWindow, openHAB scan submission, and Inbox success.
- The Compose automated readiness gate includes Android Bluetooth, location-service, runtime permission, Wi-Fi/mobile-data, and VPN diagnostics before BLE Thread commissioning.
- Failure recovery keeps sanitized diagnostics and links to advanced troubleshooting guidance for openHAB readiness, network/VPN hints, OTBR reachability, mDNS/Avahi visibility including stale `_matterc._udp` records, IPv6 reachability, expired pairing windows, and pairing-mode checks.
- The Compose advanced troubleshooting screen exposes phone-side Matter mDNS browsing for `_matterc._udp` and `_matter._tcp`, plus a best-effort IPv6 reachability check for a user-entered device IPv6 address. The UI states that these checks run from the phone and may differ from openHAB-side Avahi/router results.
- The Compose advanced troubleshooting screen exposes one-tap OpenCommissioningWindow retry for the currently staged bootstrap device and a forget-from-phone cleanup action that clears this app's stored bootstrap state without claiming to factory reset the device.
- The Compose settings flow has a dedicated **Devices on this phone** submenu for the app-stored bootstrap device, with visible back navigation, OpenCommissioningWindow retry, and forget-from-phone cleanup. This is currently a view of this app's persisted staging state, not a full connectedhomeip fabric inventory.

## Not Implemented Yet

- Broader hardware validation across multiple Matter/Thread devices, Android versions, and OTBR/router combinations.
- Full enumeration of every device on the Android connectedhomeip fabric, including live attributes such as IP address, node name, device name, and firmware version.
- Production-grade remediation for stale or conflicting `_matterc._udp` mDNS/Avahi records on the openHAB host; the app can browse from the phone, but cannot clear Avahi/router caches.
- Long-run connectedhomeip fabric persistence hardening across app restarts, phone reboots, and upgrade/reinstall scenarios. The current real-device flow restores enough controller state to open the commissioning window, but broader lifecycle coverage is still needed.

## Production Controller Seam

`MainActivity` keeps `FakeMatterController` for deterministic offline tests and non-production diagnostics, but Thread commissioning and OpenCommissioningWindow now call `selectNativeIfReady()` immediately before execution. If the reflection-backed connectedhomeip Java controller is not ready, those flows report the readiness failure and stop rather than performing simulated commissioning.

The packaged `libopenhab_matter_chip.so` remains a non-production JNI stub for the legacy bridge seam. Real commissioning is performed by the packaged connectedhomeip Java/JNI controller artifacts, which have now been validated on one real Android phone and Thread Matter device. Remaining work is production hardening, broader device/router validation, and better in-app diagnostics for stale mDNS/Avahi discovery state during openHAB handoff.
