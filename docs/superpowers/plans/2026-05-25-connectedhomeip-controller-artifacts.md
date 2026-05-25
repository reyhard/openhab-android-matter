# connectedhomeip Controller Artifacts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Android app validate, package, and diagnose the official connectedhomeip Android controller artifacts needed before real BLE Thread commissioning can be wired.

**Architecture:** Keep the existing custom `libopenhab_matter_chip.so` bridge contract, but add a separate connectedhomeip artifact contract for CHIPTool-style jars and ABI-specific `libCHIPController.so`/`libc++_shared.so`. Gradle handles packaging and artifact validation; runtime Java code uses reflection and explicit native-load probing so the app can report whether the official controller stack is present.

**Tech Stack:** Android Gradle Plugin, Java 17, JUnit 4, connectedhomeip Android controller (`chip.devicecontroller.*`, `chip.platform.*`).

---

### Task 1: Runtime Artifact Diagnostics

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifacts.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsStatus.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`

- [ ] **Step 1: Write failing tests**

Add tests that prove diagnostics require all connectedhomeip classes and `libCHIPController`:

```java
@Test
public void reportsReadyWhenRequiredClassesAndNativeLibraryArePresent() {
    RecordingLoader loader = new RecordingLoader();
    ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
            name -> true,
            loader);

    ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

    assertTrue(status.ready());
    assertEquals("CHIPController", loader.loadedLibraryName);
}
```

Add tests for a missing class and failed native load:

```java
@Test
public void reportsMissingClass() {
    ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
            name -> !"chip.platform.AndroidChipPlatform".equals(name),
            name -> { });

    ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

    assertFalse(status.ready());
    assertTrue(status.message().contains("chip.platform.AndroidChipPlatform"));
}

@Test
public void reportsNativeLoadFailure() {
    ConnectedHomeIpControllerArtifacts artifacts = new ConnectedHomeIpControllerArtifacts(
            name -> true,
            name -> { throw new UnsatisfiedLinkError("missing"); });

    ConnectedHomeIpControllerArtifactsStatus status = artifacts.check();

    assertFalse(status.ready());
    assertTrue(status.message().contains("libCHIPController"));
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpControllerArtifactsTest"
```

Expected: FAIL because `ConnectedHomeIpControllerArtifacts` does not exist.

- [ ] **Step 3: Implement diagnostics**

Create `ConnectedHomeIpControllerArtifactsStatus` with immutable fields:

```java
public final class ConnectedHomeIpControllerArtifactsStatus {
    private final boolean ready;
    private final String libraryName;
    private final String message;
}
```

Create `ConnectedHomeIpControllerArtifacts` with injectable class lookup and native loader. Required class names:

```java
"chip.devicecontroller.ChipDeviceController"
"chip.devicecontroller.ControllerParams"
"chip.devicecontroller.NetworkCredentials"
"chip.devicecontroller.CommissionParameters"
"chip.devicecontroller.OpenCommissioningCallback"
"chip.platform.AndroidChipPlatform"
"chip.platform.AndroidBleManager"
"chip.platform.NsdManagerServiceResolver"
```

The native library name must be `CHIPController`.

- [ ] **Step 4: Surface diagnostic presentation**

Add `MainActivityPresentation.connectedHomeIpArtifacts(...)` returning:

```text
connectedhomeip Android controller artifacts ready: CHIPController.
```

or:

```text
connectedhomeip Android controller artifacts not ready: <reason>
```

- [ ] **Step 5: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpControllerArtifactsTest" --tests "*MainActivityPresentationTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java docs/superpowers/plans/2026-05-25-connectedhomeip-controller-artifacts.md
git commit -m "feat: diagnose connectedhomeip controller artifacts"
```

### Task 2: Gradle Artifact Packaging Contract

**Files:**
- Modify: `app/build.gradle`
- Modify: `gradle.properties`
- Modify: `README.md`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Run failing Gradle contract check**

Run:

```powershell
.\gradlew.bat :app:verifyConnectedHomeIpControllerArtifacts --offline
```

Expected: FAIL because the task does not exist.

- [ ] **Step 2: Add Gradle properties**

Add optional properties:

```properties
openhabMatterChipControllerArtifactsDir=
openhabMatterChipControllerAbis=arm64-v8a;armeabi-v7a;x86;x86_64
```

- [ ] **Step 3: Add packaging and validation**

In `app/build.gradle`, when `openhabMatterChipControllerArtifactsDir` is non-empty:

```groovy
sourceSets {
    main {
        jniLibs.srcDirs += [file("${chipControllerArtifactsDir}/jniLibs")]
    }
}
dependencies {
    implementation fileTree(dir: chipControllerArtifactsDir, include: ["*.jar"])
}
```

Register `verifyConnectedHomeIpControllerArtifacts` that validates:

```text
<dir>/CHIPController.jar exists
<dir>/CHIPInteractionModel.jar exists
<dir>/CHIPClusterID.jar exists
<dir>/CHIPClusters.jar exists
<dir>/AndroidPlatform.jar exists
<dir>/OnboardingPayload.jar exists
<dir>/libMatterTlv.jar exists
<dir>/libMatterJson.jar exists
<dir>/jniLibs/<abi>/libCHIPController.so exists for each configured ABI
<dir>/jniLibs/<abi>/libc++_shared.so exists for each configured ABI
```

Make `preBuild` depend on it.

- [ ] **Step 4: Verify negative validation**

Run:

```powershell
.\gradlew.bat :app:verifyConnectedHomeIpControllerArtifacts --offline -PopenhabMatterChipControllerArtifactsDir=missing-dir
```

Expected: FAIL with `connectedhomeip controller artifact directory does not exist`.

- [ ] **Step 5: Verify positive validation with synthetic artifacts**

Create synthetic files under `build/tmp/chip-controller-artifacts-smoke`:

```text
CHIPController.jar
CHIPInteractionModel.jar
CHIPClusterID.jar
CHIPClusters.jar
AndroidPlatform.jar
OnboardingPayload.jar
libMatterTlv.jar
libMatterJson.jar
jniLibs/arm64-v8a/libCHIPController.so
jniLibs/arm64-v8a/libc++_shared.so
jniLibs/armeabi-v7a/libCHIPController.so
jniLibs/armeabi-v7a/libc++_shared.so
jniLibs/x86/libCHIPController.so
jniLibs/x86/libc++_shared.so
jniLibs/x86_64/libCHIPController.so
jniLibs/x86_64/libc++_shared.so
```

Run:

```powershell
.\gradlew.bat :app:verifyConnectedHomeIpControllerArtifacts --offline "-PopenhabMatterChipControllerArtifactsDir=<absolute-smoke-dir>"
```

Expected: PASS.

- [ ] **Step 6: Update docs**

Document that `D:\Source\connectedhomeip\examples\android\CHIPTool\app\libs` currently contains README placeholders in this checkout, so real artifacts must be built externally from connectedhomeip and supplied in the above layout. Cite local connectedhomeip evidence:

```text
examples/android/CHIPTool/app/build.gradle uses fileTree(dir: "libs", include: ["*.jar", "*.so"])
examples/android/CHIPTool/app/build.gradle uses jniLibs.srcDirs = ['libs/jniLibs']
src/controller/java/BUILD.gn outputs libCHIPController.so
scripts/build/builders/android.py copies CHIPTool jars plus libCHIPController.so and libc++_shared.so
```

- [ ] **Step 7: Run build verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add app/build.gradle gradle.properties README.md docs/chip-jni-integration.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-connectedhomeip-controller-artifacts.md
git commit -m "build: support connectedhomeip controller artifacts"
```

---

## Self-Review

- Spec coverage: This plan covers the next prerequisite for the `docs/research.md` Android embedded controller path: official connectedhomeip Java classes and native libraries must be packageable and diagnosable before real BLE Thread commissioning code can be safely added.
- Placeholder scan: No TBD/TODO/fill-in steps remain.
- Type consistency: Artifact status, diagnostics, Gradle property names, and library names consistently use `ConnectedHomeIpControllerArtifacts`, `openhabMatterChipControllerArtifactsDir`, CHIPTool-style jars, `libCHIPController.so`, and `libc++_shared.so`.
