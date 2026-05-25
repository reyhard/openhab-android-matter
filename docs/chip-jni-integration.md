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

`attestationBypassEnabled` is controlled by the app's persisted developer setting. It defaults to `false`; production native implementations should only treat `true` as a lab/development equivalent of `chip-tool --bypass-attestation-verifier true`.

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

## Prebuilt Production Bridge Packaging

The default APK builds the JNI stub from `app/src/main/cpp`. To package a production bridge instead, build a connectedhomeip-backed shared library that exports this app's `SystemNativeChipBridge` JNI symbols and place it in:

```text
<prebuilt-dir>/arm64-v8a/libopenhab_matter_chip.so
<prebuilt-dir>/armeabi-v7a/libopenhab_matter_chip.so
<prebuilt-dir>/x86/libopenhab_matter_chip.so
<prebuilt-dir>/x86_64/libopenhab_matter_chip.so
```

Then build with:

```powershell
.\gradlew.bat :app:assembleDebug --offline -PopenhabMatterChipNativeMode=prebuilt "-PopenhabMatterChipPrebuiltDir=<prebuilt-dir>"
```

The prebuilt library must return `kind=connectedhomeip;production=true` from `nativeControllerMetadata()` or the Java selector will keep using the simulated controller.

## Official connectedhomeip Android Controller Artifacts

The connectedhomeip Android Java stack uses CHIPTool-style jar and native artifacts in addition to any app-specific bridge library. To package those artifacts for the future Java-side controller integration, build with:

```powershell
.\gradlew.bat :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=<artifact-dir>"
```

The artifact directory must use this layout:

```text
<artifact-dir>/CHIPController.jar
<artifact-dir>/CHIPInteractionModel.jar
<artifact-dir>/CHIPClusterID.jar
<artifact-dir>/CHIPClusters.jar
<artifact-dir>/AndroidPlatform.jar
<artifact-dir>/OnboardingPayload.jar
<artifact-dir>/libMatterTlv.jar
<artifact-dir>/libMatterJson.jar
<artifact-dir>/jniLibs/arm64-v8a/libCHIPController.so
<artifact-dir>/jniLibs/arm64-v8a/libc++_shared.so
<artifact-dir>/jniLibs/armeabi-v7a/libCHIPController.so
<artifact-dir>/jniLibs/armeabi-v7a/libc++_shared.so
<artifact-dir>/jniLibs/x86/libCHIPController.so
<artifact-dir>/jniLibs/x86/libc++_shared.so
<artifact-dir>/jniLibs/x86_64/libCHIPController.so
<artifact-dir>/jniLibs/x86_64/libc++_shared.so
```

The local connectedhomeip checkout shows the expected prebuilt packaging seam, but does not contain binaries in `D:\Source\connectedhomeip\examples\android\CHIPTool\app\libs` in this workspace:

- `examples/android/CHIPTool/app/build.gradle` uses `implementation fileTree(dir: "libs", include: ["*.jar", "*.so"])` for prebuilt mode.
- `examples/android/CHIPTool/app/build.gradle` uses `jniLibs.srcDirs = ['libs/jniLibs']` for prebuilt native libraries.
- `src/controller/java/BUILD.gn` builds the Android JNI shared library with `output_name = "libCHIPController"`.
- `scripts/build/builders/android.py` copies `libCHIPController.so`, `libc++_shared.so`, `CHIPController.jar`, `CHIPInteractionModel.jar`, `CHIPClusterID.jar`, `CHIPClusters.jar`, `AndroidPlatform.jar`, `OnboardingPayload.jar`, `libMatterTlv.jar`, and `libMatterJson.jar` for CHIPTool Android prebuilts.

Runtime diagnostics should require the Java classes used by CHIPTool (`chip.devicecontroller.ChipDeviceController`, `chip.devicecontroller.NetworkCredentials`, `chip.devicecontroller.CommissionParameters`, `chip.devicecontroller.OpenCommissioningCallback`, and `chip.platform.AndroidChipPlatform`) and successful loading of `libCHIPController.so`.

The local connectedhomeip Android APIs that the production bridge should mirror are:

- `ChipDeviceController.pairDeviceThroughBLE(...)` with `NetworkCredentials.forThread(...)` for BLE Thread commissioning.
- `ChipDeviceController.setDeviceAttestationDelegate(...)` and `continueCommissioning(..., ignoreAttestationFailure)` for the persisted developer bypass flag.
- `ChipDeviceController.setAttestationTrustStoreDelegate(...)` for production PAA trust-store verification.
- `ChipDeviceController.openPairingWindowWithPINCallback(...)` for Enhanced OpenCommissioningWindow and returned manual/QR codes.

## connectedhomeip Behavior To Mirror

- `chip-tool pairing ble-thread <NODE_ID> hex:<DATASET_HEX> <PIN> <DISCRIMINATOR>`
- `chip-tool pairing open-commissioning-window <NODE_ID> 1 300 1000 <DISCRIMINATOR>`

## Acceptance

- Given a known Thread device in pairing mode and an OTBR Active Operational Dataset, Android commissions the device over BLE.
- OTBR CLI shows the device joined the Thread network.
- Android opens a commissioning window and displays a temporary setup code.
- Entering the temporary code in openHAB Matter Scan Input makes the device appear in the Inbox.
