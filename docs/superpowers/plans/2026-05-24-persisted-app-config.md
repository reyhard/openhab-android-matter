# Persisted App Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the OTBR Thread dataset and openHAB base URL so the app matches the research flow where users configure the Thread dataset once.

**Architecture:** Add a small configuration package with a model, repository interface, Android `SharedPreferences` implementation, and in-memory fake for JVM tests. Integrate it into `MainActivity` without storing setup PINs or transient Matter payloads.

**Tech Stack:** Java 17, Android `SharedPreferences`, JUnit 4.13.2, no external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java` - immutable saved config model.
- `app/src/main/java/org/openhab/matter/companion/config/AppConfigRepository.java` - persistence interface.
- `app/src/main/java/org/openhab/matter/companion/config/InMemoryAppConfigRepository.java` - deterministic fake for tests.
- `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java` - Android persistence implementation.
- `app/src/test/java/org/openhab/matter/companion/config/InMemoryAppConfigRepositoryTest.java` - repository behavior tests.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - load saved dataset/base URL at startup and save them explicitly.
- `app/src/main/java/org/openhab/matter/companion/ui/AppState.java` - already has persisted fields; no setup PIN persistence.
- `README.md` and `docs/implementation-status.md` - document persisted config behavior and security limitation.

---

### Task 10: Config Repository

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/AppConfigRepository.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/InMemoryAppConfigRepository.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java`
- Test: `app/src/test/java/org/openhab/matter/companion/config/InMemoryAppConfigRepositoryTest.java`

- [ ] **Step 1: Write failing repository tests**

Create `InMemoryAppConfigRepositoryTest` proving that an empty repository returns empty config and that saving only persists dataset/openHAB base URL.

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: FAIL because config classes do not exist.

- [ ] **Step 3: Implement config classes**

`AppConfig` exposes `threadDataset()` and `openHabBaseUrl()`. `AppConfigRepository` exposes `load()` and `save(AppConfig config)`. `SharedPreferencesAppConfigRepository` stores keys `thread_dataset` and `openhab_base_url` under preference file `openhab_matter_config`.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/config app/src/test/java/org/openhab/matter/companion/config
git commit -m "feat: add persisted app configuration"
```

---

### Task 11: UI Persistence Integration

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Load saved config**

Instantiate `SharedPreferencesAppConfigRepository` in `MainActivity` and load saved dataset/base URL before creating input fields.

- [ ] **Step 2: Add save action**

Add a "Save dataset and openHAB URL" button. On click, validate Thread dataset format when present, save dataset/base URL, and log a sanitized success/failure message. Do not persist setup payload or setup PIN.

- [ ] **Step 3: Verify build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS and debug APK generated.

- [ ] **Step 4: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java README.md docs/implementation-status.md
git commit -m "feat: persist Thread and openHAB configuration"
```

---

## Self-Review

- Spec coverage: Covers the research requirement that the app stores the Active Operational Dataset once and reuses openHAB REST configuration.
- Security note: This uses Android app-private `SharedPreferences`, not encrypted storage. That is acceptable as a next step but remains below the production-grade secure storage recommendation from `docs/research.md`.
- Scope: Does not store setup PINs, QR payloads, or Matter fabric keys.
