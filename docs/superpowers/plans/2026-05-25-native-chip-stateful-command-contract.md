# Native CHIP Stateful Command Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Carry opaque connectedhomeip controller/fabric state through commissioning and OpenCommissioningWindow so a production native controller can restore and update its bootstrap fabric material without plaintext storage or lossy primitive APIs.

**Architecture:** Replace primitive native bridge command signatures with explicit request/result value objects. Keep `MatterController` as the UI-facing abstraction, but make it return result objects containing both user-visible outputs and updated opaque controller state. `MainActivity` remains the orchestration/persistence boundary and saves returned state through the existing encrypted `MatterBootstrapStateRepository`.

**Tech Stack:** Java 17, Android JVM tests, existing Android SharedPreferences/Keystore storage seam, JNI stub updated to match Java signatures.

---

## File Structure

- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeCommissioningRequest.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeCommissioningResult.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeOpenCommissioningWindowRequest.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeOpenCommissioningWindowResult.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterCommissioningResult.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterOpenCommissioningWindowResult.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipBridge.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/SystemNativeChipBridge.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/cpp/openhab_matter_chip_stub.cpp`
- Modify: `docs/chip-jni-integration.md`, `README.md`, `docs/implementation-status.md`
- Add/modify controller tests under `app/src/test/java/org/openhab/matter/companion/controller/`

---

### Task 1: Native Bridge Request/Result Contract

**Files:**
- Create: `NativeCommissioningRequest.java`
- Create: `NativeCommissioningResult.java`
- Create: `NativeOpenCommissioningWindowRequest.java`
- Create: `NativeOpenCommissioningWindowResult.java`
- Modify: `NativeChipBridge.java`
- Modify: `SystemNativeChipBridge.java`
- Test: `NativeChipBridgeCommandContractTest.java`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/org/openhab/matter/companion/controller/NativeChipBridgeCommandContractTest.java` with tests asserting:
- `NativeCommissioningRequest` exposes `datasetHex`, `setupPin`, `discriminator`, `attestationBypassEnabled`, and `controllerState`.
- `NativeCommissioningResult` exposes `nodeId` and `controllerState`.
- `NativeOpenCommissioningWindowRequest` exposes `nodeId`, `timeoutSeconds`, `discriminator`, and `controllerState`.
- `NativeOpenCommissioningWindowResult` exposes `temporaryCode` and `controllerState`.

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: compile fails because the new types and signatures do not exist.

- [ ] **Step 2: Implement value objects and interface signatures**

Add final immutable classes with null-safe string fields. Change `NativeChipBridge` to:

```java
NativeCommissioningResult commissionBleThread(NativeCommissioningRequest request);

NativeOpenCommissioningWindowResult openCommissioningWindow(NativeOpenCommissioningWindowRequest request);
```

Update `SystemNativeChipBridge` to call native methods with all scalar request fields and wrap scalar native outputs into result objects. Until the native layer returns controller state, keep returned controller state equal to the request controller state.

- [ ] **Step 3: Update JNI stub signatures**

Update `openhab_matter_chip_stub.cpp` method signatures to include `jboolean attestationBypassEnabled` and `jstring controllerState` for commissioning, and `jstring controllerState` for OCW. Keep the stub throwing `UnsupportedOperationException`.

- [ ] **Step 4: Run unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: tests either pass or fail only in adapter callers that still need Task 2 updates.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/main/cpp/openhab_matter_chip_stub.cpp app/src/test/java/org/openhab/matter/companion/controller/NativeChipBridgeCommandContractTest.java
git commit -m "feat: add stateful native chip bridge contract"
```

---

### Task 2: Controller Adapter Result Mapping

**Files:**
- Create: `MatterCommissioningResult.java`
- Create: `MatterOpenCommissioningWindowResult.java`
- Modify: `MatterController.java`
- Modify: `ChipMatterController.java`
- Modify: `FakeMatterController.java`
- Modify tests: `ChipMatterControllerReadinessTest.java`, `MatterControllerSelectorTest.java`, `FakeMatterControllerTest.java`
- Test: `ChipMatterControllerCommandContractTest.java`

- [ ] **Step 1: Write failing adapter tests**

Create `ChipMatterControllerCommandContractTest` with a capturing `NativeChipBridge` that returns production metadata and records requests.

Assert commissioning maps:
- dataset hex from `ThreadDataset`
- setup PIN and discriminator from `MatterSetupPayload`
- `ChipMatterControllerConfig.attestationBypassEnabled()`
- incoming controller state string
- returned node id and updated controller state

Assert OCW maps:
- node id, timeout, discriminator
- incoming controller state string
- returned temporary code and updated controller state

Run unit tests and expect compile failures until controller signatures are updated.

- [ ] **Step 2: Update `MatterController` API**

Change:

```java
MatterCommissioningResult commissionBleThread(ThreadDataset dataset, MatterSetupPayload payload, String controllerState, ProgressListener listener) throws Exception;

MatterOpenCommissioningWindowResult openCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator, String controllerState, ProgressListener listener) throws Exception;
```

- [ ] **Step 3: Update `ChipMatterController`**

Build native request objects, pass controller state into the native bridge, preserve readiness gating, and return `MatterCommissioningResult` / `MatterOpenCommissioningWindowResult`. Continue wrapping `UnsatisfiedLinkError` as `IllegalStateException`.

- [ ] **Step 4: Update fake and existing tests**

`FakeMatterController` should return deterministic non-empty controller state strings such as `fake-controller-state:<nodeId>` so state-flow tests can catch dropped values. Update anonymous bridge stubs in existing readiness/selector tests to the new request/result signatures.

- [ ] **Step 5: Run unit tests and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: all unit tests pass.

Commit:

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller
git commit -m "feat: return stateful matter controller results"
```

---

### Task 3: MainActivity Bootstrap State Flow

**Files:**
- Modify: `MainActivity.java`
- Test: `MainActivityPresentationTest.java` only if presentation text changes
- Optional pure Java helper/test if needed: `MatterBootstrapStateFlowTest.java`

- [ ] **Step 1: Write failing state-flow tests or use existing unit seams**

Prefer a pure Java helper if MainActivity logic becomes hard to test. Required behavior:
- Commissioning passes current persisted controller state into the selected controller.
- Commissioning saves returned node id plus returned controller state.
- OCW passes current persisted controller state into the selected controller.
- OCW saves updated controller state without dropping node id.
- Simulated controller does not preserve stale native controller state.

- [ ] **Step 2: Update MainActivity orchestration**

When commissioning:
- Load current `MatterBootstrapState`.
- Pass `controllerState()` to `commissionBleThread`.
- Save `new MatterBootstrapState(result.nodeId(), result.controllerState(), false)`.
- Update `persistedBootstrapState` after save.

When opening OCW:
- Load current state before call.
- Pass `controllerState()` to `openCommissioningWindow`.
- Save returned controller state with the bootstrap node id.

Do not log controller state, dataset contents, setup PIN, or raw fabric material.

- [ ] **Step 3: Run unit tests and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Commit:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/test/java/org/openhab/matter/companion
git commit -m "feat: persist native chip controller state flow"
```

---

### Task 4: Docs And Final Verification

**Files:**
- Modify: `docs/chip-jni-integration.md`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Update docs**

Document that the Java/native command contract now carries:
- attestation bypass flag
- prior opaque controller state
- updated opaque controller state in results

Keep real BLE/PASE/CASE/Thread provisioning, attestation implementation, OCW, and real connectedhomeip persistence listed as not implemented until a production native library exists.

- [ ] **Step 2: Run verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
jar tf app\build\outputs\apk\debug\app-debug.apk | Select-String "libopenhab_matter_chip.so"
D:\Tools\Android\SDK\platform-tools\adb.exe devices
```

If a device is attached and allows installs:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

- [ ] **Step 3: Commit docs**

```powershell
git add README.md docs/chip-jni-integration.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-native-chip-stateful-command-contract.md
git commit -m "docs: document stateful native chip command contract"
```

---

## Self-Review

- Spec coverage: This plan advances the research requirement that the phone act as a bootstrap Matter controller by preventing loss of connectedhomeip fabric/controller state between BLE Thread commissioning and OpenCommissioningWindow.
- Placeholder scan: No TBD/TODO/fill-in steps remain; all required behaviors and commands are explicit.
- Type consistency: Native request/result types are distinct from UI-facing Matter result types; controller state remains opaque string material and is never logged.
