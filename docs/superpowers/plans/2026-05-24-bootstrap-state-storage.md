# Bootstrap State Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an encrypted, app-private storage seam for Matter bootstrap controller state so the future connectedhomeip-backed controller can persist fabric/bootstrap material without logging or plaintext storage.

**Architecture:** Keep bootstrap controller state separate from general app configuration. Store non-secret identifiers such as the bootstrap node id alongside an encrypted opaque controller-state blob; unreadable encrypted state must not crash app startup. Wire only the currently known node id into the UI flow, and keep documentation explicit that real fabric key material is not yet produced by the native CHIP layer.

**Tech Stack:** Java 17, Android SharedPreferences, Android Keystore-backed `SecretCodec`, JVM unit tests with fixed fake codecs, existing Gradle offline test/build gate.

---

## File Structure

- Create `app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapState.java`
  - Immutable value object for `bootstrapNodeId`, encrypted-state plaintext input/output, and unreadable flag.
- Create `app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapStateRepository.java`
  - Small `load()`, `save(...)`, `clear()` interface.
- Create `app/src/main/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapper.java`
  - Converts between domain state and stored values; encrypts the opaque controller state.
- Create `app/src/main/java/org/openhab/matter/companion/controller/SharedPreferencesMatterBootstrapStateRepository.java`
  - Android production repository using app-private SharedPreferences and `AndroidKeystoreSecretCodec`.
- Create `app/src/main/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepository.java`
  - Test/demo repository with production-like load/save/clear semantics.
- Modify `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
  - Load persisted bootstrap node id on startup.
  - Save bootstrap node id after commissioning succeeds.
  - Clear stale bootstrap state if opening the commissioning window fails because no node id exists.
- Modify `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
  - Add a short user-safe message for unreadable bootstrap state.
- Test `app/src/test/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapperTest.java`
  - Mapper encryption/decryption and unreadable behavior.
- Test `app/src/test/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepositoryTest.java`
  - In-memory repository semantics.
- Modify `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
  - Presentation text for unreadable state.
- Modify `README.md` and `docs/implementation-status.md`
  - Document the new storage seam without claiming real connectedhomeip fabric persistence.

## Task 1: Bootstrap State Domain And Mapper

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapState.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapper.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapperTest.java`

- [ ] **Step 1: Write failing mapper tests**

Add tests for:

```java
@Test
public void encryptsOpaqueControllerStateButKeepsNodeIdReadable() throws Exception {
    SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

    SecureMatterBootstrapStateMapper.StoredBootstrapState stored = mapper.toStoredValues(
            new MatterBootstrapState(1234L, "fabric-secret-material", false));

    assertEquals(1234L, stored.bootstrapNodeId());
    assertEquals("enc:v1:encoded(fabric-secret-material)", stored.controllerState());
}

@Test
public void decodesEncryptedOpaqueControllerState() {
    SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

    MatterBootstrapState state = mapper.fromStoredValues(1234L, "enc:v1:encoded(fabric-secret-material)");

    assertEquals(1234L, state.bootstrapNodeId());
    assertEquals("fabric-secret-material", state.controllerState());
    assertEquals(false, state.stateUnreadable());
}

@Test
public void decodeFailureClearsStateAndMarksUnreadable() {
    SecureMatterBootstrapStateMapper mapper = new SecureMatterBootstrapStateMapper(new FixedSecretCodec());

    MatterBootstrapState state = mapper.fromStoredValues(1234L, "enc:v1:broken");

    assertEquals(-1L, state.bootstrapNodeId());
    assertEquals("", state.controllerState());
    assertEquals(true, state.stateUnreadable());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.controller.SecureMatterBootstrapStateMapperTest
```

Expected: compilation fails because the new classes do not exist.

- [ ] **Step 3: Implement domain and mapper**

Implement:

```java
public final class MatterBootstrapState {
    private final long bootstrapNodeId;
    private final String controllerState;
    private final boolean stateUnreadable;

    public MatterBootstrapState(long bootstrapNodeId, String controllerState, boolean stateUnreadable) {
        this.bootstrapNodeId = bootstrapNodeId;
        this.controllerState = controllerState == null ? "" : controllerState;
        this.stateUnreadable = stateUnreadable;
    }

    public static MatterBootstrapState empty() {
        return new MatterBootstrapState(-1L, "", false);
    }

    public long bootstrapNodeId() {
        return bootstrapNodeId;
    }

    public String controllerState() {
        return controllerState;
    }

    public boolean stateUnreadable() {
        return stateUnreadable;
    }
}
```

Mapper behavior:
- `toStoredValues` encrypts `controllerState`.
- `fromStoredValues` returns empty state for empty stored state.
- Decode failure returns `new MatterBootstrapState(-1L, "", true)`.

- [ ] **Step 4: Run mapper tests**

Run the focused command from Step 2. Expected: pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapState.java app/src/main/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapper.java app/src/test/java/org/openhab/matter/companion/controller/SecureMatterBootstrapStateMapperTest.java
git commit -m "feat: add encrypted bootstrap state mapper"
```

## Task 2: Bootstrap State Repositories

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapStateRepository.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepository.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/SharedPreferencesMatterBootstrapStateRepository.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepositoryTest.java`

- [ ] **Step 1: Write failing repository tests**

Add tests that verify:
- New in-memory repository loads `MatterBootstrapState.empty()`.
- `save(...)` round-trips node id and opaque state.
- `clear()` returns the repository to empty state.

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.controller.InMemoryMatterBootstrapStateRepositoryTest
```

Expected: compilation fails because repository classes do not exist.

- [ ] **Step 3: Implement repositories**

Interface:

```java
public interface MatterBootstrapStateRepository {
    MatterBootstrapState load();
    void save(MatterBootstrapState state);
    void clear();
}
```

Production SharedPreferences keys:
- Preference file: `openhab_matter_bootstrap_state`
- `bootstrap_node_id`
- `controller_state`

Production repository must use:

```java
new SecureMatterBootstrapStateMapper(new AndroidKeystoreSecretCodec())
```

- [ ] **Step 4: Run repository tests**

Run the focused command from Step 2. Expected: pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller/MatterBootstrapStateRepository.java app/src/main/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepository.java app/src/main/java/org/openhab/matter/companion/controller/SharedPreferencesMatterBootstrapStateRepository.java app/src/test/java/org/openhab/matter/companion/controller/InMemoryMatterBootstrapStateRepositoryTest.java
git commit -m "feat: add bootstrap state repositories"
```

## Task 3: UI Wiring And Documentation

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing presentation test**

Add:

```java
@Test
public void warnsWhenBootstrapStateCannotBeRead() {
    assertEquals(
            "Stored Matter bootstrap controller state could not be decrypted. Re-run Thread commissioning before opening a commissioning window.",
            MainActivityPresentation.bootstrapStateUnreadable());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected: compilation fails because `bootstrapStateUnreadable()` does not exist.

- [ ] **Step 3: Wire bootstrap repository**

In `MainActivity`:
- Add a `MatterBootstrapStateRepository bootstrapStateRepository`.
- Initialize it with `new SharedPreferencesMatterBootstrapStateRepository(this)`.
- Load it during startup after app config.
- If loaded state has `stateUnreadable()`, append the presentation warning.
- If loaded state has `bootstrapNodeId() >= 0`, assign it to `state.commissionedNodeId`.
- After successful commissioning, save `new MatterBootstrapState(nodeId, "", false)`.

Do not log `controllerState()`.

- [ ] **Step 4: Update docs**

README:
- Move "Matter fabric keys" out of the plain "not persisted" statement.
- Add that an encrypted bootstrap-state repository exists for future native controller state, but the current native bridge does not yet emit real connectedhomeip fabric material.

Implementation status:
- Add implemented item: encrypted app-private bootstrap controller state repository.
- Keep not implemented item: real connectedhomeip fabric key persistence/restore.

- [ ] **Step 5: Run full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java README.md docs/implementation-status.md
git commit -m "feat: persist bootstrap controller state"
```

## Self-Review Notes

- This plan does not claim to implement real connectedhomeip fabric persistence; it creates the encrypted seam needed for it.
- Native in-app CameraX QR scanning is not planned here because offline dependency inspection found no cached CameraX, ML Kit, or ZXing decoder artifacts and AndroidX is currently disabled.
- Real BLE/PASE/CASE/Thread provisioning remains blocked until the connectedhomeip Android native library is added.
