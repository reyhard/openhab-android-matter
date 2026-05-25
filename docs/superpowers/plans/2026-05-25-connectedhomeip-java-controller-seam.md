# connectedhomeip Java Controller Seam Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a testable Java-side connectedhomeip Matter controller seam that maps this app's MatterController commands to a future Android `ChipDeviceController` gateway.

**Architecture:** Keep `MatterController` as the UI-facing boundary and add `ConnectedHomeIpMatterController` as a sibling of `ChipMatterController`. It depends on `ConnectedHomeIpControllerArtifacts` for readiness and a small `ConnectedHomeIpControllerGateway` interface for the actual CHIPTool-equivalent operations, so future work can implement the gateway using `AndroidChipPlatform`, BLE, `ChipDeviceController.pairDeviceThroughBLE(...)`, and `openPairingWindowWithPINCallback(...)` without changing UI orchestration.

**Tech Stack:** Java 17, Android JVM unit tests, connectedhomeip Android controller concepts (`ChipDeviceController`, `NetworkCredentials`, `CommissionParameters`, `OpenCommissioningCallback`).

---

### Task 1: Java Controller Command Seam

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterController.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerGateway.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpCommissioningRequest.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpOpenCommissioningWindowRequest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerTest.java`
- Modify: `docs/implementation-status.md`
- Modify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Write failing command mapping tests**

Create `ConnectedHomeIpMatterControllerTest` with a fake artifact checker and fake gateway. The commissioning test must prove:

```java
MatterCommissioningResult result = controller.commissionBleThread(
        ThreadDataset.parse("hex:0e080000000000010000"),
        new MatterSetupPayload("pin=20202021;disc=3840", 20202021L, 3840, "Aqara", "U200", false),
        "controller-state",
        step -> steps.add(step.message()));
```

Expected request fields:

```text
datasetHex=0E080000000000010000
setupPin=20202021
discriminator=3840
attestationBypassEnabled=true
controllerState=controller-state
```

Expected result:

```text
nodeId=987654321
controllerState=updated-state
```

Expected progress includes:

```text
Starting connectedhomeip Java BLE Thread commissioning
connectedhomeip Java BLE Thread commissioning complete for node 987654321
```

- [ ] **Step 2: Write failing OCW mapping tests**

Add a test that calls:

```java
controller.openCommissioningWindow(987654321L, 300, 3840, "controller-state", step -> steps.add(step.message()));
```

Expected request fields:

```text
nodeId=987654321
timeoutSeconds=300
iteration=1000
discriminator=3840
controllerState=controller-state
```

Expected result:

```text
temporaryCode=3497-0112-332
controllerState=ocw-state
```

- [ ] **Step 3: Write failing readiness guard test**

Add a test where `ConnectedHomeIpControllerArtifactsStatus.ready()` is false. Both controller commands must throw `IllegalStateException` with the artifact readiness message and must not call the gateway.

- [ ] **Step 4: Run RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpMatterControllerTest"
```

Expected: FAIL because the new controller classes do not exist.

- [ ] **Step 5: Implement value objects and gateway**

Create immutable request classes with validation:

```java
public final class ConnectedHomeIpCommissioningRequest {
    public ConnectedHomeIpCommissioningRequest(
            String datasetHex,
            long setupPin,
            int discriminator,
            boolean attestationBypassEnabled,
            String controllerState) { ... }
}
```

```java
public final class ConnectedHomeIpOpenCommissioningWindowRequest {
    public ConnectedHomeIpOpenCommissioningWindowRequest(
            long nodeId,
            int timeoutSeconds,
            long iteration,
            int discriminator,
            String controllerState) { ... }
}
```

Create `ConnectedHomeIpControllerGateway`:

```java
public interface ConnectedHomeIpControllerGateway {
    MatterCommissioningResult commissionBleThread(ConnectedHomeIpCommissioningRequest request) throws Exception;

    MatterOpenCommissioningWindowResult openCommissioningWindow(
            ConnectedHomeIpOpenCommissioningWindowRequest request) throws Exception;
}
```

- [ ] **Step 6: Implement controller**

Create `ConnectedHomeIpMatterController` with constructor:

```java
public ConnectedHomeIpMatterController(
        ConnectedHomeIpControllerArtifacts artifacts,
        ConnectedHomeIpControllerGateway gateway,
        boolean attestationBypassEnabled)
```

It must:

- call `artifacts.check()` before each command;
- throw `IllegalStateException(status.message())` when not ready;
- map `ThreadDataset.hex()`, `MatterSetupPayload.pin()`, and `MatterSetupPayload.discriminator()` into the commissioning request;
- use OCW iteration `1000L`;
- emit start and completion progress messages;
- return gateway results without altering controller state.

- [ ] **Step 7: Run GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpMatterControllerTest"
```

Expected: PASS.

- [ ] **Step 8: Update docs**

Update implementation docs to say the Java-side controller command seam exists, but the concrete Android gateway that initializes `AndroidChipPlatform`, scans/connects BLE, calls `pairDeviceThroughBLE(...)`, and calls `openPairingWindowWithPINCallback(...)` is still not implemented.

- [ ] **Step 9: Full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller docs/implementation-status.md docs/chip-jni-integration.md docs/superpowers/plans/2026-05-25-connectedhomeip-java-controller-seam.md
git commit -m "feat: add connectedhomeip Java controller seam"
```

---

## Self-Review

- Spec coverage: This moves `docs/research.md` Phase 4/5 forward by adding the Java controller command boundary required before wiring the real Android CHIPTool APIs.
- Placeholder scan: No placeholders or deferred fields are present; the remaining gateway implementation is explicitly out of this slice and documented as not implemented.
- Type consistency: `ConnectedHomeIpMatterController`, `ConnectedHomeIpControllerGateway`, request field names, and result types match the existing `MatterController` abstractions.
