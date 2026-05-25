# Bootstrap State Fail-Closed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the app from reusing a stale bootstrap node id when encrypted Matter bootstrap controller state cannot be read.

**Architecture:** Keep encrypted bootstrap persistence unchanged, but make restore logic fail closed. `MatterBootstrapStateResolver` becomes the single policy point for deciding whether a persisted node id or saved-instance node id can be used, and `MainActivity` blocks OpenCommissioningWindow if the latest repository read reports unreadable state.

**Tech Stack:** Java 17, Android SDK unit tests, JUnit 4.13.2.

---

### Task 1: Fail Closed On Unreadable Bootstrap State

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapStateResolver.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/controller/MatterBootstrapStateResolverTest.java`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write the failing resolver test**

Change `savedInstanceNodeIdIsUsedWhenPersistedStateIsUnreadable` so unreadable persisted state returns `-1L` instead of a stale saved-instance node id:

```java
@Test
public void unreadablePersistedStateClearsSavedInstanceNodeId() {
    long nodeId = MatterBootstrapStateResolver.resolveNodeId(
            42L,
            new MatterBootstrapState(-1L, "", true));

    assertEquals(-1L, nodeId);
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*MatterBootstrapStateResolverTest"
```

Expected: FAIL because the current resolver returns the saved-instance node id `42`.

- [ ] **Step 3: Implement resolver fail-closed policy**

Update `MatterBootstrapStateResolver.resolveNodeId(...)`:

```java
if (persistedState != null && persistedState.stateUnreadable()) {
    return -1L;
}
if (persistedState != null && persistedState.bootstrapNodeId() >= 0) {
    return persistedState.bootstrapNodeId();
}
return savedInstanceNodeId;
```

- [ ] **Step 4: Guard OCW against latest unreadable state**

In `MainActivity.runOpenCommissioningWindow()`, after loading `MatterBootstrapState bootstrapState = bootstrapStateRepository.load();`, add:

```java
if (bootstrapState.stateUnreadable()) {
    state.commissionedNodeId = -1L;
    persistedBootstrapState = bootstrapState;
    persistedBootstrapStateUnreadable = true;
    append(MainActivityPresentation.bootstrapStateUnreadable());
    return;
}
```

Then resolve the node id from the latest repository state:

```java
long nodeId = MatterBootstrapStateResolver.resolveNodeId(state.commissionedNodeId, bootstrapState);
if (nodeId < 0) {
    clearBootstrapState();
    append("Run Thread commissioning first so the app has a bootstrap node id.");
    return;
}
state.commissionedNodeId = nodeId;
```

- [ ] **Step 5: Verify GREEN and full build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapStateResolver.java app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/test/java/org/openhab/matter/companion/controller/MatterBootstrapStateResolverTest.java docs/implementation-status.md docs/superpowers/plans/2026-05-25-bootstrap-state-fail-closed.md
git commit -m "fix: fail closed on unreadable bootstrap state"
```

---

## Self-Review

- Spec coverage: This closes one restore-safety gap under the remaining "real fabric restore/persistence" status item without claiming hardware validation.
- Placeholder scan: No TODO/TBD/fill-in steps remain.
- Type consistency: The plan consistently uses existing `MatterBootstrapState`, `MatterBootstrapStateResolver`, and `MainActivityPresentation.bootstrapStateUnreadable()`.
