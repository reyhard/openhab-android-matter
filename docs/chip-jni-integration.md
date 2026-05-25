# CHIP JNI Integration

## Native Bridge Required

`SystemNativeChipBridge` loads `libopenhab_matter_chip.so` and requires these JNI entry points:

```java
private static native String nativeControllerMetadata();
private static native String nativeCommissionBleThread(
        String datasetHex,
        long pin,
        int discriminator,
        boolean attestationBypassEnabled,
        String controllerState);
private static native String nativeOpenCommissioningWindow(
        long nodeId,
        int timeoutSeconds,
        int discriminator,
        String controllerState);
```

`nativeControllerMetadata()` must return semicolon-separated key/value metadata:

```text
kind=connectedhomeip;version=<build-or-sdk-version>;production=true;message=<operator-readable-status>
```

The Android app treats any bridge that is missing metadata, has `production=false`, or has `kind` other than `connectedhomeip` as not ready and continues with the simulated controller.

`controllerState` is opaque connectedhomeip controller/fabric material loaded from the encrypted bootstrap repository. The native bridge must not log it. It must return updated state as UTF-8 Base64 in semicolon-separated result metadata so delimiters, newlines, and serialized binary-safe text do not corrupt parsing:

```text
nodeId=<decimal-node-id>;controllerStateBase64=<base64-opaque-updated-state>
temporaryCode=<manual-or-qr-code>;controllerStateBase64=<base64-opaque-updated-state>
```

The Java layer rejects missing node IDs and missing or blank temporary setup codes instead of treating them as successful commissioning.

The debug APK currently packages a JNI stub that returns `kind=stub;production=false`; this proves native packaging and loading only. It does not perform Matter commissioning.

## connectedhomeip Behavior To Mirror

- `chip-tool pairing ble-thread <NODE_ID> hex:<DATASET_HEX> <PIN> <DISCRIMINATOR>`
- `chip-tool pairing open-commissioning-window <NODE_ID> 1 300 1000 <DISCRIMINATOR>`

## Acceptance

- Given a known Thread device in pairing mode and an OTBR Active Operational Dataset, Android commissions the device over BLE.
- OTBR CLI shows the device joined the Thread network.
- Android opens a commissioning window and displays a temporary setup code.
- Entering the temporary code in openHAB Matter Scan Input makes the device appear in the Inbox.
