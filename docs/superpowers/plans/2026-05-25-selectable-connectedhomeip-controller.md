# Selectable ConnectedHomeIp Controller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the app's selectable native-controller path able to choose a reflection-backed connectedhomeip Java controller while preserving the simulated fallback when artifacts are absent.

**Architecture:** Introduce a small `MatterControllerCandidate` interface for controllers that can report readiness. Keep `ChipMatterController` compatible by implementing that interface, make `ConnectedHomeIpMatterController` report connectedhomeip artifact readiness, and add a factory that returns either a fully wired connectedhomeip controller or an unavailable candidate with a safe readiness message.

**Tech Stack:** Android Java, JUnit4/Robolectric unit tests, reflection-only connectedhomeip Java integration.

---

### Task 1: Generic Selectable Controller Candidate

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerCandidate.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/MatterControllerSelector.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/NativeChipControllerSession.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/MatterControllerSelectorTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/NativeChipControllerSessionTest.java`

- [ ] **Step 1: Write failing selector/session tests**

Add tests proving `MatterControllerSelector` and `NativeChipControllerSession` accept a non-`ChipMatterController` candidate whose `readiness()` returns `ChipMatterControllerStatus`.

- [ ] **Step 2: Run targeted tests to verify compile failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*MatterControllerSelectorTest" --tests "*NativeChipControllerSessionTest"`

Expected: FAIL because `MatterControllerCandidate` does not exist and session/selector signatures are `ChipMatterController`-specific.

- [ ] **Step 3: Implement minimal generic candidate seam**

```java
public interface MatterControllerCandidate extends MatterController {
    ChipMatterControllerStatus readiness();
}
```

Update `ChipMatterController implements MatterControllerCandidate`, change selector/session factory/request types from `ChipMatterController` to `MatterControllerCandidate`, and keep existing public behavior/messages unchanged.

- [ ] **Step 4: Run targeted tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*MatterControllerSelectorTest" --tests "*NativeChipControllerSessionTest"`

Expected: PASS.

### Task 2: ConnectedHomeIp Candidate Factory

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactory.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpRandomNodeIdAllocator.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifacts.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactoryTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerArtifactsTest.java`

- [ ] **Step 1: Write failing factory tests**

Cover:
- missing artifacts return an unavailable candidate and do not invoke gateway construction
- ready artifacts return a `ConnectedHomeIpMatterController`
- gateway construction failure returns an unavailable candidate instead of throwing

- [ ] **Step 2: Run targeted tests to verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpMatterControllerFactoryTest" --tests "*ConnectedHomeIpControllerArtifactsTest"`

Expected: FAIL because the factory and random node-id allocator do not exist.

- [ ] **Step 3: Implement minimal factory**

`ConnectedHomeIpMatterControllerFactory.create(boolean)` checks `ConnectedHomeIpControllerArtifacts.check()` first. If not ready, it returns an unavailable `MatterControllerCandidate` whose commands throw the readiness message. If ready, it constructs a `ConnectedHomeIpMatterController` with a gateway factory. The default gateway wires:

```java
ConnectedHomeIpPlatformControllerProvider platformProvider = new ConnectedHomeIpPlatformControllerProvider(context);
ConnectedHomeIpReflectionCommandFactory commandFactory = ConnectedHomeIpReflectionCommandFactory.fromDefaultClassLoader();
return new ConnectedHomeIpReflectionGateway(
        platformProvider,
        new ConnectedHomeIpAndroidBleConnectionProvider(context, platformProvider),
        new ConnectedHomeIpRandomNodeIdAllocator(),
        new ConnectedHomeIpReflectionCommissioningMonitor(commandFactory),
        new ConnectedHomeIpReflectionAttestationHandler(commandFactory, 120),
        new ConnectedHomeIpReflectionDevicePointerProvider(commandFactory, 300_000L),
        commandFactory);
```

Add `chip.platform.BleCallback` to artifact readiness because the default BLE provider reflects it during setup.
Also add `chip.devicecontroller.CommissionParameters$Builder` and `chip.devicecontroller.ChipDeviceController$CompletionListener`; these are reflected directly by the command factory and listener installation path.
Use non-initializing class lookup and treat `LinkageError` or `SecurityException` during class inspection as a not-ready status so partial bundles fall back to the simulator instead of crashing readiness checks.

- [ ] **Step 4: Run targeted tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpMatterControllerFactoryTest" --tests "*ConnectedHomeIpControllerArtifactsTest"`

Expected: PASS.

### Task 3: MainActivity Wiring

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Wire the session factory**

Change `MainActivity.newNativeControllerSession(...)` to create connectedhomeip Java candidates via `ConnectedHomeIpMatterControllerFactory`. Keep the existing simulated controller as the fallback and keep the same button workflow.
Create selection requests inside the background worker thread and sequence requests in `NativeChipControllerSession` so stale asynchronous readiness results cannot overwrite newer requests.

- [ ] **Step 2: Run full offline verification**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/org/openhab/matter/companion app/src/test/java/org/openhab/matter/companion docs
git commit -m "feat: wire selectable connectedhomeip controller"
```
