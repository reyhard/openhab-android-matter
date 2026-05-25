# Prebuilt Native CHIP Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow the Android app to package a real connectedhomeip-backed `libopenhab_matter_chip.so` prebuilt per ABI instead of always compiling the bundled JNI stub.

**Architecture:** Keep the default build safe and dependency-light by compiling the existing stub. Add an opt-in Gradle property that switches native inputs to a prebuilt `jniLibs` directory, validates required ABI files before build, and documents how connectedhomeip Android outputs map to this app’s JNI contract.

**Tech Stack:** Android Gradle Plugin, Groovy Gradle script, CMake/JNI stub, PowerShell verification, local connectedhomeip Android CHIPTool source.

---

## File Structure

- `app/build.gradle` - add `openhabMatterChipNativeMode` and `openhabMatterChipPrebuiltDir` handling, validation task, conditional CMake/sourceSet wiring.
- `gradle.properties` - document default `openhabMatterChipNativeMode=stub` behavior.
- `docs/chip-jni-integration.md` - document prebuilt layout and connectedhomeip API map.
- `README.md` - document how to build with the default stub and how to point at a prebuilt production bridge.
- `docs/implementation-status.md` - state that prebuilt native replacement packaging is supported, while the real bridge implementation remains not implemented.

---

### Task 1: Add Gradle Native Mode Switch

**Files:**
- Modify: `app/build.gradle`
- Modify: `gradle.properties`

- [ ] **Step 1: Add Gradle validation before implementation and observe RED**

Add a temporary verification command that should fail before the Gradle task exists:

```powershell
.\gradlew.bat :app:verifyOpenhabMatterChipNativeInputs --offline
```

Expected: FAIL with `Task 'verifyOpenhabMatterChipNativeInputs' not found`.

- [ ] **Step 2: Implement native mode configuration**

At the top of `app/build.gradle`, add:

```groovy
def chipNativeMode = (findProperty("openhabMatterChipNativeMode") ?: "stub").toString().trim()
def chipPrebuiltDir = (findProperty("openhabMatterChipPrebuiltDir") ?: "").toString().trim()
def chipSupportedAbis = ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]

if (!["stub", "prebuilt"].contains(chipNativeMode)) {
    throw new GradleException("openhabMatterChipNativeMode must be 'stub' or 'prebuilt'.")
}
```

Change the existing `externalNativeBuild` block so it is only configured when `chipNativeMode == "stub"`:

```groovy
if (chipNativeMode == "stub") {
    externalNativeBuild {
        cmake {
            path file("src/main/cpp/CMakeLists.txt")
            version "3.22.1"
        }
    }
} else {
    sourceSets {
        main {
            jniLibs.srcDirs = [file(chipPrebuiltDir)]
        }
    }
}
```

Add after the `android { ... }` block:

```groovy
tasks.register("verifyOpenhabMatterChipNativeInputs") {
    doLast {
        if (chipNativeMode == "stub") {
            if (!file("src/main/cpp/openhab_matter_chip_stub.cpp").isFile()) {
                throw new GradleException("Stub native mode requires src/main/cpp/openhab_matter_chip_stub.cpp.")
            }
            return
        }
        if (chipPrebuiltDir.isEmpty()) {
            throw new GradleException("prebuilt native mode requires -PopenhabMatterChipPrebuiltDir=<dir>.")
        }
        File root = file(chipPrebuiltDir)
        if (!root.isDirectory()) {
            throw new GradleException("Prebuilt native directory does not exist: " + root)
        }
        chipSupportedAbis.each { abi ->
            File lib = new File(new File(root, abi), "libopenhab_matter_chip.so")
            if (!lib.isFile()) {
                throw new GradleException("Missing prebuilt native library: " + lib)
            }
        }
    }
}

preBuild.dependsOn("verifyOpenhabMatterChipNativeInputs")
```

In `gradle.properties`, add:

```properties
# Native CHIP bridge mode. Use "stub" for the bundled non-production JNI stub.
# Use -PopenhabMatterChipNativeMode=prebuilt -PopenhabMatterChipPrebuiltDir=<dir>
# to package ABI directories containing libopenhab_matter_chip.so.
openhabMatterChipNativeMode=stub
```

- [ ] **Step 3: Verify default stub mode**

Run:

```powershell
.\gradlew.bat :app:verifyOpenhabMatterChipNativeInputs :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`, and the APK still contains `lib/<abi>/libopenhab_matter_chip.so` from the stub build.

- [ ] **Step 4: Verify invalid prebuilt mode fails safely**

Run:

```powershell
.\gradlew.bat :app:verifyOpenhabMatterChipNativeInputs --offline -PopenhabMatterChipNativeMode=prebuilt -PopenhabMatterChipPrebuiltDir=missing-dir
```

Expected: FAIL with `Prebuilt native directory does not exist`.

- [ ] **Step 5: Commit**

```powershell
git add app/build.gradle gradle.properties docs/superpowers/plans/2026-05-25-prebuilt-native-chip-packaging.md
git commit -m "build: support prebuilt native chip bridge"
```

---

### Task 2: Document connectedhomeip Artifact Mapping

**Files:**
- Modify: `docs/chip-jni-integration.md`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Update docs**

Add to `docs/chip-jni-integration.md`:

```markdown
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
.\gradlew.bat :app:assembleDebug --offline -PopenhabMatterChipNativeMode=prebuilt -PopenhabMatterChipPrebuiltDir=<prebuilt-dir>
```

The prebuilt library must return `kind=connectedhomeip;production=true` from `nativeControllerMetadata()` or the Java selector will keep using the simulated controller.

The local connectedhomeip Android APIs that the production bridge should mirror are:

- `ChipDeviceController.pairDeviceThroughBLE(...)` with `NetworkCredentials.forThread(...)` for BLE Thread commissioning.
- `ChipDeviceController.setDeviceAttestationDelegate(...)` and `continueCommissioning(..., ignoreAttestationFailure)` for the persisted developer bypass flag.
- `ChipDeviceController.setAttestationTrustStoreDelegate(...)` for production PAA trust-store verification.
- `ChipDeviceController.openPairingWindowWithPINCallback(...)` for Enhanced OpenCommissioningWindow and returned manual/QR codes.
```

Add README build note under `Build`:

```markdown
By default, the APK packages the non-production JNI stub. To package a prebuilt production bridge, pass `-PopenhabMatterChipNativeMode=prebuilt -PopenhabMatterChipPrebuiltDir=<dir>` where `<dir>` contains ABI subdirectories with `libopenhab_matter_chip.so`.
```

Add implemented status:

```markdown
- Gradle can package ABI-specific prebuilt `libopenhab_matter_chip.so` replacements instead of the bundled JNI stub.
```

Keep the not-implemented real connectedhomeip bullets unchanged.

- [ ] **Step 2: Verify docs and build**

Run:

```powershell
rg -n "prebuilt|connectedhomeip|Real Matter|Real BLE|stub|production=false" README.md docs/chip-jni-integration.md docs/implementation-status.md
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: docs show prebuilt packaging support but still state real commissioning is not implemented with the bundled stub. Build succeeds.

- [ ] **Step 3: Commit**

```powershell
git add README.md docs/chip-jni-integration.md docs/implementation-status.md
git commit -m "docs: describe prebuilt native chip bridge packaging"
```

---

## Self-Review

- Spec coverage: This plan moves toward the research requirement for an embedded CHIP controller by allowing the app build to consume a real connectedhomeip-backed bridge artifact without Java changes. It does not claim to implement the bridge itself.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified test instructions remain.
- Type consistency: The Gradle properties are named `openhabMatterChipNativeMode` and `openhabMatterChipPrebuiltDir` throughout.
