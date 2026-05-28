---
name: openhab-android-matter-connectedhomeip-artifacts
description: Use when building, updating, packaging, validating, or diagnosing connectedhomeip Android controller artifacts for openhab-android-matter.
---

# connectedhomeip Android Artifacts

Use this skill when working on connectedhomeip Android controller artifacts, Gradle packaging, runtime readiness, or failures caused by missing CHIP Java/JNI classes.

## Artifact Reality

- The legacy `libopenhab_matter_chip.so` is a non-production JNI stub for the old bridge seam.
- Real commissioning uses connectedhomeip Java/JNI controller artifacts, including `ChipDeviceController` and native `libCHIPController.so`.
- Placeholder jars or empty native libraries are invalid and must not pass readiness.
- If the local connectedhomeip checkout is stale, fetch/update it before rebuilding artifacts.

## Expected Layout

The artifact directory should contain controller/platform jars and ABI-specific native libraries:

```text
CHIPController.jar
CHIPInteractionModel.jar
CHIPClusterID.jar
CHIPClusters.jar
AndroidPlatform.jar
OnboardingPayload.jar
libMatterTlv.jar
libMatterJson.jar
jniLibs\arm64-v8a\libCHIPController.so
jniLibs\arm64-v8a\libc++_shared.so
```

Other ABIs may be present, but the device build used in this repo commonly targets `arm64-v8a`.

## Build Commands

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PopenhabMatterChipControllerArtifactsDir=<artifact-dir> -PopenhabMatterChipControllerAbis=arm64-v8a
```

Install after build:

```powershell
$adb='D:\Tools\Android\SDK\platform-tools\adb.exe'
& $adb devices
& $adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

## Runtime Readiness

Readiness should verify:

- Required connectedhomeip classes can be inspected without crashing app startup.
- Native `libCHIPController.so` and `libc++_shared.so` load on the target ABI.
- AndroidChipPlatform initialization succeeds.
- BLE manager callback access is available.
- `ChipDeviceController` construction succeeds.

When readiness fails, Thread commissioning and OCW must stop rather than falling back to fake controller behavior.

## Common Failure Signals

- Missing `chip.devicecontroller.ChipDeviceControllerNative` means the packaged Java/JNI artifacts are incomplete or mismatched.
- `UnsatisfiedLinkError` usually means missing or incompatible native libraries.
- Class linkage failures can mean connectedhomeip jar versions are inconsistent.
- Runtime preflight passing without hardware does not prove BLE/Thread commissioning works; real-device logs are still required.

