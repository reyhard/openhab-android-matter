# Developer Attestation Bypass Setting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persisted developer attestation bypass setting that is visible in the Android UI and passed into the native CHIP controller configuration before commissioning.

**Architecture:** Keep the bypass flag in app-private configuration next to existing OTBR/openHAB settings, defaulting to `false` for legacy installs. `MainActivity` owns the runtime checkbox state and rebuilds the `ChipMatterController` from persisted or saved config so native readiness and selection use the current value.

**Tech Stack:** Java Android app, SharedPreferences config repository, JUnit unit tests, Gradle Android build.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java` - add immutable `attestationBypassEnabled` field and backward-compatible constructors.
- `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java` - persist the boolean under a new SharedPreferences key with default `false`.
- `app/src/main/java/org/openhab/matter/companion/config/InMemoryAppConfigRepository.java` - no special logic; constructor defaults must keep in-memory config false.
- `app/src/main/java/org/openhab/matter/companion/ui/AppState.java` - add runtime `attestationBypassEnabled` field.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - add a checkbox, save/load the flag, include state restoration, rebuild `ChipMatterController` with `ChipMatterControllerConfig`.
- `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` - add warning/save/readiness messages that disclose bypass state without leaking Matter secrets.
- `app/src/test/java/org/openhab/matter/companion/config/InMemoryAppConfigRepositoryTest.java` - assert default and saved flag behavior.
- `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java` - assert the mapper preserves the flag when called with the new signature and defaults legacy calls to false.
- `app/src/test/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepositoryTest.java` - assert the SharedPreferences key defaults false and persists true.
- `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java` - assert warning/save/readiness text.
- `README.md`, `docs/implementation-status.md`, `docs/chip-jni-integration.md` - document that the app now persists and passes an explicit developer bypass setting, without claiming real attestation is implemented.

---

### Task 1: Persist AppConfig Attestation Bypass

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/config/SecureAppConfigMapper.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/config/InMemoryAppConfigRepositoryTest.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java`
- Create: `app/src/test/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepositoryTest.java`

- [ ] **Step 1: Write failing config tests**

Add to `InMemoryAppConfigRepositoryTest`:

```java
    @Test
    public void emptyRepositoryDefaultsAttestationBypassToFalse() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        AppConfig config = repository.load();

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void saveAndLoadPersistsAttestationBypass() {
        AppConfigRepository repository = new InMemoryAppConfigRepository();

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "http://openhab.local:8080",
                "http://otbr.local",
                false,
                true));
        AppConfig config = repository.load();

        assertEquals(true, config.attestationBypassEnabled());
    }
```

Add to `SecureAppConfigMapperTest`:

```java
    @Test
    public void preservesAttestationBypassWhenDecodingStoredConfig() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "http://otbr.local",
                true);

        assertEquals(true, config.attestationBypassEnabled());
    }

    @Test
    public void legacyMapperCallsDefaultAttestationBypassToFalse() throws Exception {
        SecretCodec codec = new FixedSecretCodec();
        SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

        AppConfig config = mapper.fromStoredValues(
                "enc:v1:encoded(hex:001122)",
                "http://openhab.local:8080",
                "http://otbr.local");

        assertEquals(false, config.attestationBypassEnabled());
    }
```

Create `SharedPreferencesAppConfigRepositoryTest` with a fake `SharedPreferences` based on `SharedPreferencesMatterBootstrapStateRepositoryTest`, then add:

```java
    @Test
    public void loadDefaultsAttestationBypassToFalseWhenPreferenceMissing() {
        SharedPreferencesAppConfigRepository repository = repository(new FakeSharedPreferences());

        AppConfig config = repository.load();

        assertEquals(false, config.attestationBypassEnabled());
    }

    @Test
    public void saveAndLoadPersistsAttestationBypassBoolean() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        SharedPreferencesAppConfigRepository repository = repository(preferences);

        repository.save(new AppConfig(
                "hex:0E080000000000010000",
                "http://openhab.local:8080",
                "http://otbr.local",
                false,
                true));

        assertEquals(true, preferences.getBoolean("attestation_bypass_enabled", false));
        assertEquals(true, repository.load().attestationBypassEnabled());
    }
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.config.InMemoryAppConfigRepositoryTest" --tests "org.openhab.matter.companion.config.SecureAppConfigMapperTest" --tests "org.openhab.matter.companion.config.SharedPreferencesAppConfigRepositoryTest"
```

Expected: compilation fails because `AppConfig.attestationBypassEnabled()` and the five-argument constructor do not exist.

- [ ] **Step 3: Implement minimal config persistence**

Update `AppConfig` to add:

```java
private final boolean attestationBypassEnabled;

public AppConfig(String threadDataset, String openHabBaseUrl, String otbrBaseUrl, boolean threadDatasetUnreadable) {
    this(threadDataset, openHabBaseUrl, otbrBaseUrl, threadDatasetUnreadable, false);
}

public AppConfig(
        String threadDataset,
        String openHabBaseUrl,
        String otbrBaseUrl,
        boolean threadDatasetUnreadable,
        boolean attestationBypassEnabled) {
    this.threadDataset = threadDataset == null ? "" : threadDataset;
    this.openHabBaseUrl = openHabBaseUrl == null ? "" : openHabBaseUrl;
    this.otbrBaseUrl = otbrBaseUrl == null ? "" : otbrBaseUrl;
    this.threadDatasetUnreadable = threadDatasetUnreadable;
    this.attestationBypassEnabled = attestationBypassEnabled;
}

public boolean attestationBypassEnabled() {
    return attestationBypassEnabled;
}
```

Update `SecureAppConfigMapper` to add:

```java
public AppConfig fromStoredValues(
        String threadDataset,
        String openHabBaseUrl,
        String otbrBaseUrl,
        boolean attestationBypassEnabled) {
    String safeThreadDataset = threadDataset == null ? "" : threadDataset;
    if (isLegacyPlaintextThreadDataset(safeThreadDataset)) {
        return new AppConfig(safeThreadDataset, openHabBaseUrl, otbrBaseUrl, false, attestationBypassEnabled);
    }
    if (safeThreadDataset.isEmpty()) {
        return new AppConfig("", openHabBaseUrl, otbrBaseUrl, false, attestationBypassEnabled);
    }

    try {
        return new AppConfig(secretCodec.decode(safeThreadDataset), openHabBaseUrl, otbrBaseUrl, false,
                attestationBypassEnabled);
    } catch (GeneralSecurityException e) {
        return new AppConfig("", openHabBaseUrl, otbrBaseUrl, true, attestationBypassEnabled);
    }
}
```

Keep the existing three-argument `fromStoredValues(...)` delegating with `false`.

Update `SharedPreferencesAppConfigRepository`:

```java
private static final String KEY_ATTESTATION_BYPASS_ENABLED = "attestation_bypass_enabled";
```

Load with:

```java
boolean storedAttestationBypassEnabled = preferences.getBoolean(KEY_ATTESTATION_BYPASS_ENABLED, false);
AppConfig config = mapper.fromStoredValues(
        storedThreadDataset,
        storedOpenHabBaseUrl,
        storedOtbrBaseUrl,
        storedAttestationBypassEnabled);
```

Save with:

```java
.putBoolean(KEY_ATTESTATION_BYPASS_ENABLED, config.attestationBypassEnabled())
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.config.InMemoryAppConfigRepositoryTest" --tests "org.openhab.matter.companion.config.SecureAppConfigMapperTest" --tests "org.openhab.matter.companion.config.SharedPreferencesAppConfigRepositoryTest"
```

Expected: selected config tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/config app/src/test/java/org/openhab/matter/companion/config docs/superpowers/plans/2026-05-25-attestation-bypass-setting.md
git commit -m "feat: persist attestation bypass config"
```

---

### Task 2: Wire UI Setting Into Native Controller

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/ui/AppState.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`

- [ ] **Step 1: Write failing presentation tests**

Add to `MainActivityPresentationTest`:

```java
    @Test
    public void describesAttestationBypassWarning() {
        assertEquals(
                "Developer attestation bypass skips device attestation verification for lab devices only. Leave it off for production pairing.",
                MainActivityPresentation.attestationBypassWarning());
    }

    @Test
    public void describesConfigSaveWithAttestationBypassDisabled() {
        assertEquals(
                "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: off. Setup payloads and PINs are not saved.",
                MainActivityPresentation.encryptedConfigSaved(false));
    }

    @Test
    public void describesConfigSaveWithAttestationBypassEnabled() {
        assertEquals(
                "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: on. Setup payloads and PINs are not saved.",
                MainActivityPresentation.encryptedConfigSaved(true));
    }

    @Test
    public void describesNativeChipReadinessWithAttestationBypass() {
        ChipMatterControllerStatus status = new ChipMatterControllerStatus(
                true,
                "openhab_matter_chip",
                true,
                "connectedhomeip",
                true,
                "Native CHIP library loaded: openhab_matter_chip");

        assertEquals(
                "Native CHIP controller ready: openhab_matter_chip. Developer attestation bypass: on.",
                MainActivityPresentation.nativeChipReadiness(status));
    }
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.MainActivityPresentationTest"
```

Expected: compilation fails because new presentation methods/signatures do not exist.

- [ ] **Step 3: Implement presentation methods**

Add to `MainActivityPresentation`:

```java
static String encryptedConfigSaved(boolean attestationBypassEnabled) {
    return "Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: "
            + onOff(attestationBypassEnabled)
            + ". Setup payloads and PINs are not saved.";
}

static String attestationBypassWarning() {
    return "Developer attestation bypass skips device attestation verification for lab devices only. Leave it off for production pairing.";
}

private static String onOff(boolean enabled) {
    return enabled ? "on" : "off";
}
```

Keep `encryptedConfigSaved()` as a backward-compatible wrapper:

```java
static String encryptedConfigSaved() {
    return encryptedConfigSaved(false);
}
```

Update `nativeChipReadiness(...)` so both ready and not-ready messages append:

```java
+ ". Developer attestation bypass: " + onOff(status.attestationBypassEnabled()) + "."
```

- [ ] **Step 4: Implement UI wiring**

In `MainActivity`, import `android.widget.CheckBox` and `org.openhab.matter.companion.controller.ChipMatterControllerConfig`.

Change:

```java
private final ChipMatterController chipMatterController = new ChipMatterController();
```

to:

```java
private ChipMatterController chipMatterController = newChipMatterController(false);
```

Add fields:

```java
private static final String KEY_ATTESTATION_BYPASS_ENABLED = "attestationBypassEnabled";
private CheckBox attestationBypassInput;
```

After loading persisted config, call:

```java
chipMatterController = newChipMatterController(state.attestationBypassEnabled);
```

Create the checkbox after `openHabInput`:

```java
attestationBypassInput = new CheckBox(this);
attestationBypassInput.setText("Developer attestation bypass for native CHIP commissioning");
attestationBypassInput.setTextColor(TEXT_COLOR);
attestationBypassInput.setChecked(state.attestationBypassEnabled);
attestationBypassInput.setLayoutParams(blockParams());
```

Add the warning panel and checkbox to the root before commissioning buttons:

```java
root.addView(section("Native CHIP attestation"));
root.addView(panel(MainActivityPresentation.attestationBypassWarning()));
root.addView(attestationBypassInput);
```

In `saveConfiguration()`:

```java
state.attestationBypassEnabled = attestationBypassInput.isChecked();
chipMatterController = newChipMatterController(state.attestationBypassEnabled);
configRepository.save(new AppConfig(
        state.dataset,
        state.openHabBaseUrl,
        state.otbrBaseUrl,
        false,
        state.attestationBypassEnabled));
append(MainActivityPresentation.encryptedConfigSaved(state.attestationBypassEnabled));
```

In `loadPersistedConfig()`:

```java
state.attestationBypassEnabled = config.attestationBypassEnabled();
```

In `onSaveInstanceState(...)` and `restoreState(...)`, persist `KEY_ATTESTATION_BYPASS_ENABLED`.

Add helper:

```java
private ChipMatterController newChipMatterController(boolean attestationBypassEnabled) {
    return new ChipMatterController(
            new ChipMatterControllerConfig("openhab_matter_chip", attestationBypassEnabled));
}
```

If that constructor does not exist, add this overload to `ChipMatterController`:

```java
public ChipMatterController(ChipMatterControllerConfig config) {
    this(new SystemNativeChipBridge(), config);
}
```

- [ ] **Step 5: Run tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "org.openhab.matter.companion.MainActivityPresentationTest"
```

Expected: selected presentation tests pass.

- [ ] **Step 6: Run full unit tests and assemble**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion app/src/test/java/org/openhab/matter/companion
git commit -m "feat: expose attestation bypass setting"
```

---

### Task 3: Document Attestation Bypass Scope

**Files:**
- Modify: `README.md`
- Modify: `docs/implementation-status.md`
- Modify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Update docs**

Add to README current MVP list:

```markdown
- Persisted developer attestation bypass setting for the future native CHIP controller path; it defaults off and is passed to the JNI command contract only when explicitly enabled.
```

Add to `docs/implementation-status.md` implemented list:

```markdown
- Persisted developer attestation bypass setting is exposed in the UI and passed into native CHIP commissioning requests.
```

In `docs/chip-jni-integration.md`, add after the JNI signature block:

```markdown
`attestationBypassEnabled` is controlled by the app's persisted developer setting. It defaults to `false`; production native implementations should only treat `true` as a lab/development equivalent of `chip-tool --bypass-attestation-verifier true`.
```

- [ ] **Step 2: Verify docs do not overclaim**

Run:

```powershell
rg -n "real BLE|Real Matter|attestation|developer attestation|bypass" README.md docs/implementation-status.md docs/chip-jni-integration.md
```

Expected: docs still state real BLE/PASE/attestation/Thread provisioning and connectedhomeip implementation are not implemented yet.

- [ ] **Step 3: Run final verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add README.md docs/implementation-status.md docs/chip-jni-integration.md docs/superpowers/plans/2026-05-25-attestation-bypass-setting.md
git commit -m "docs: document attestation bypass setting"
```

---

## Self-Review

- Spec coverage: This plan advances the `docs/research.md` requirement for attestation handling with either a PAA store or developer bypass flag. It does not implement real attestation verification or the connectedhomeip JNI stack.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified test instructions remain.
- Type consistency: The persisted field name is `attestationBypassEnabled` in `AppConfig`, `AppState`, `ChipMatterControllerConfig`, and presentation methods.
