# Fabric Restore UI Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the connectedhomeip fabric-restore probe through a user-triggered controller diagnostic so users can check whether a persisted bootstrap node id is restorable before OpenCommissioningWindow.

**Architecture:** Keep the probe side-effect-light and explicitly user-triggered. The restore check runs through the existing connectedhomeip native candidate and `ConnectedHomeIpControllerGateway`, so `MainActivity` does not construct a second connectedhomeip runtime or duplicate JNI/platform setup.

**Tech Stack:** Java 17, Android SDK unit tests, JUnit 4.13.2.

---

### Task 1: Gateway Restore Diagnostic

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpFabricRestoreChecker.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerGateway.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGateway.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterController.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGatewayTest.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerTest.java`

- [ ] **Step 1: Write failing gateway/controller tests**

Add tests proving:

```java
public void checkFabricRestoreDelegatesThroughExistingControllerAndPointerProvider()
public void checkFabricRestoreDelegatesFromConnectedHomeIpMatterController()
```

Expected behavior:
- `ConnectedHomeIpReflectionGateway.checkFabricRestore(nodeId)` uses the same `controllerProvider` and `devicePointerProvider` fields already used by OCW.
- The acquired pointer is closed immediately.
- `ConnectedHomeIpMatterController.checkFabricRestore(nodeId)` checks artifact readiness first, then delegates to the gateway.

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionGatewayTest" --tests "*ConnectedHomeIpMatterControllerTest"
```

Expected: FAIL because `checkFabricRestore(...)` and `ConnectedHomeIpFabricRestoreChecker` do not exist.

- [ ] **Step 3: Implement gateway and controller method**

Add:

```java
public interface ConnectedHomeIpFabricRestoreChecker {
    ConnectedHomeIpFabricRestoreStatus checkFabricRestore(long bootstrapNodeId) throws Exception;
}
```

Make `ConnectedHomeIpControllerGateway` include the same method. Implement `ConnectedHomeIpReflectionGateway.checkFabricRestore(...)` with `new ConnectedHomeIpFabricRestoreProbe(controllerProvider, devicePointerProvider).check(bootstrapNodeId)`. Make `ConnectedHomeIpMatterController` implement `ConnectedHomeIpFabricRestoreChecker` and delegate after `requireArtifactsReady()`.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpReflectionGatewayTest" --tests "*ConnectedHomeIpMatterControllerTest"
```

Expected: PASS.

### Task 2: MainActivity Fabric Restore Button

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `docs/implementation-status.md`
- Modify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Add presentation label test and helper**

Add a test for:

```java
MainActivityPresentation.checkFabricRestoreButtonLabel()
```

Expected text:

```text
Check connectedhomeip fabric restore
```

- [ ] **Step 2: Wire button and worker action**

Add a button near the existing connectedhomeip controller buttons. Its click handler loads `MatterBootstrapState`, resolves the node id with `MatterBootstrapStateResolver`, obtains `controllerSession.selectionRequest()`, and:

- If no node id exists, appends `MainActivityPresentation.connectedHomeIpFabricRestore(...)` for a skipped status.
- If `request.nativeController()` implements `ConnectedHomeIpFabricRestoreChecker`, calls `checkFabricRestore(nodeId)` on a worker thread.
- Otherwise appends checked/not-ready status from the native candidate readiness message.

Do not construct connectedhomeip platform/provider/gateway objects in `MainActivity`. Do not echo controller state.

- [ ] **Step 3: Update docs**

Update implementation status and CHIP integration docs to say the fabric-restore probe is surfaced through a user-triggered connectedhomeip fabric restore check.

- [ ] **Step 4: Verify full build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpFabricRestoreProbeTest" --tests "*ConnectedHomeIpReflectionGatewayTest" --tests "*ConnectedHomeIpMatterControllerTest" --tests "*MainActivityPresentationTest"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion app/src/test/java/org/openhab/matter/companion docs
git commit -m "feat: surface connectedhomeip fabric restore check"
```

---

## Self-Review

- Spec coverage: This moves the existing restore probe from a library seam into a user-triggered install-readiness diagnostic without claiming hardware validation.
- Placeholder scan: No TBD/TODO/fill-in steps remain.
- Type consistency: Uses existing `ConnectedHomeIpFabricRestoreStatus`, `ConnectedHomeIpFabricRestoreProbe`, and `MainActivityPresentation.connectedHomeIpFabricRestore(...)`.
