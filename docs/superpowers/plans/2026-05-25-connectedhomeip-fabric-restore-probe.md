# connectedhomeip Fabric Restore Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a side-effect-light diagnostic that verifies whether connectedhomeip can restore a persisted bootstrap fabric by acquiring and releasing a device pointer for the stored node id.

**Architecture:** Keep commissioning and OCW flows unchanged. Add a controller-package probe around the existing `ConnectedHomeIpControllerProvider` and `ConnectedHomeIpDevicePointerProvider` seams, returning a status object that can be surfaced by UI/docs without logging setup payloads, Thread datasets, or opaque controller state.

**Tech Stack:** Java 17, JUnit 4.13.2, reflection-backed connectedhomeip controller seams.

---

### Task 1: Fabric Restore Probe

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpFabricRestoreStatus.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpFabricRestoreProbe.java`
- Create: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpFabricRestoreProbeTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing probe tests**

Create tests for:

```java
returnsSkippedWhenBootstrapNodeIdMissing()
returnsReadyWhenDevicePointerCanBeAcquiredAndReleased()
releasesPointerAfterSuccessfulProbe()
returnsNotReadyWhenAcquireThrows()
```

The ready path should construct `ConnectedHomeIpFabricRestoreProbe` with fake provider/pointer seams and assert that the returned status is ready, references the node id, and closes the pointer.

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpFabricRestoreProbeTest"
```

Expected: FAIL because probe/status classes do not exist.

- [ ] **Step 3: Implement probe and status**

`ConnectedHomeIpFabricRestoreStatus` fields:

```java
boolean checked()
boolean ready()
long nodeId()
String message()
```

`ConnectedHomeIpFabricRestoreProbe.check(long bootstrapNodeId)` behavior:

- Node id `< 0`: return checked `false`, ready `false`, message `No connectedhomeip bootstrap fabric has been commissioned yet.`
- Otherwise call `controllerProvider.controller()`.
- Call `devicePointerProvider.acquire(controller, bootstrapNodeId)` inside try-with-resources.
- If acquisition succeeds, close/release immediately and return ready.
- If acquisition fails, return checked `true`, ready `false`, message `connectedhomeip fabric restore is not ready for node <id>: <safe exception message>`.

- [ ] **Step 4: Add presentation copy**

Add `MainActivityPresentation.connectedHomeIpFabricRestore(...)`:

```text
connectedhomeip fabric restore: no bootstrap fabric has been commissioned yet.
connectedhomeip fabric restore ready for node <id>.
connectedhomeip fabric restore not ready: <message>
```

- [ ] **Step 5: Verify GREEN and build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion app/src/test/java/org/openhab/matter/companion docs
git commit -m "feat: add connectedhomeip fabric restore probe"
```

---

## Self-Review

- Spec coverage: This directly advances the remaining `docs/research.md` production gap for real connectedhomeip fabric restore diagnostics without claiming hardware commissioning is complete.
- Placeholder scan: No TBD/TODO/fill-in steps remain.
- Type consistency: The plan consistently uses existing connectedhomeip provider and device-pointer seams.
