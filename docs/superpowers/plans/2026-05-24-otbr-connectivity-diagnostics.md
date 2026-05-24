# OTBR Connectivity Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a basic OTBR connectivity check to the Thread dataset setup flow so the app can validate the configured Thread border router endpoint before real BLE Thread commissioning.

**Architecture:** Mirror the existing openHAB HTTP diagnostics pattern with dependency-free `HttpURLConnection` clients. Persist an optional OTBR base URL alongside the encrypted Thread dataset and openHAB base URL. Keep diagnostics generic because OTBR deployments vary: successful HTTP 2xx/3xx means reachable; malformed URLs, unsupported protocols, network failures, and HTTP errors are reported as not reachable.

**Tech Stack:** Java 17, Android Views, `HttpURLConnection`, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/otbr/OtbrClient.java` - interface for OTBR readiness diagnostics.
- `app/src/main/java/org/openhab/matter/companion/otbr/HttpOtbrClient.java` - `HttpURLConnection` implementation.
- `app/src/main/java/org/openhab/matter/companion/otbr/OtbrStatus.java` - immutable readiness result.
- `app/src/test/java/org/openhab/matter/companion/otbr/HttpOtbrClientTest.java` - local one-shot HTTP tests.
- `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java` - add optional OTBR base URL.
- `app/src/main/java/org/openhab/matter/companion/config/SecureAppConfigMapper.java` - preserve OTBR base URL while encrypting only the Thread dataset.
- `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java` - persist OTBR base URL.
- `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java` - prove OTBR URL survives mapping.
- `app/src/main/java/org/openhab/matter/companion/ui/AppState.java` - add OTBR base URL UI state.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - add OTBR input, readiness button, save/restore/persist integration.
- `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` - add OTBR result formatting and update saved-config text.
- `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java` - presentation tests.
- `README.md` and `docs/implementation-status.md` - document OTBR connectivity diagnostics.

---

### Task 23: OTBR HTTP Diagnostic Client

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/otbr/OtbrClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/otbr/HttpOtbrClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/otbr/OtbrStatus.java`
- Test: `app/src/test/java/org/openhab/matter/companion/otbr/HttpOtbrClientTest.java`

- [ ] **Step 1: Write failing HTTP client tests**

Create tests proving:

```java
@Test
public void reportsReachableForSuccessfulHttpResponse() throws Exception {
    OneShotHttpServer server = new OneShotHttpServer(200, "otbr");
    server.start();

    OtbrStatus status = new HttpOtbrClient().checkReadiness(server.baseUrl());

    assertTrue(status.reachable());
    assertEquals("OTBR endpoint is reachable", status.message());
    assertTrue(status.details().contains("HTTP 200"));
}

@Test
public void reportsUnreachableForUnsupportedProtocol() {
    OtbrStatus status = new HttpOtbrClient().checkReadiness("file:/tmp/otbr");

    assertFalse(status.reachable());
    assertEquals("OTBR base URL is invalid", status.message());
    assertTrue(status.details().contains("Unsupported protocol"));
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.otbr.HttpOtbrClientTest
```

Expected: compile fails because the OTBR classes do not exist.

- [ ] **Step 3: Implement OTBR status/client**

Implement:

```java
public interface OtbrClient {
    OtbrStatus checkReadiness(String baseUrl);
}
```

`OtbrStatus` exposes `reachable()`, `message()`, and `details()`.

`HttpOtbrClient` normalizes trailing slashes, calls the base URL with `GET`, uses 3000 ms connect/read timeouts, accepts only `http` and `https`, returns reachable for HTTP 2xx/3xx, and returns not reachable for malformed URL, unsupported protocol, non-success HTTP, and `IOException`.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.otbr.HttpOtbrClientTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/otbr app/src/test/java/org/openhab/matter/companion/otbr
git commit -m "feat: add OTBR connectivity diagnostics"
```

---

### Task 24: OTBR UI, Persistence, And Docs

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/config/AppConfig.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/config/SecureAppConfigMapper.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java`
- Test: `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/ui/AppState.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Test: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing persistence/presentation tests**

Add `SecureAppConfigMapperTest` coverage proving `otbrBaseUrl` survives `toStoredValues(...)` and `fromStoredValues(...)`.

Add presentation tests proving:

```java
assertEquals(
        "OTBR connectivity: endpoint reachable.",
        MainActivityPresentation.otbrConnectivityResult(new OtbrStatus(true, "OTBR endpoint is reachable", "HTTP 200")));

assertEquals(
        "Saved Thread dataset in encrypted app storage, saved OTBR base URL, and saved openHAB base URL. Setup payloads and PINs are not saved.",
        MainActivityPresentation.encryptedConfigSaved());
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.config.SecureAppConfigMapperTest --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected: compile or assertion failure because OTBR config/presentation support is missing.

- [ ] **Step 3: Add OTBR config persistence**

Add `otbrBaseUrl` to `AppConfig`, `SecureAppConfigMapper.StoredConfig`, and `SharedPreferencesAppConfigRepository` with key `otbr_base_url`. Keep Thread dataset encryption behavior unchanged.

- [ ] **Step 4: Wire OTBR UI diagnostics**

In `MainActivity`:

- Add state key `KEY_OTBR_BASE_URL` and input ID `OTBR_INPUT_ID`.
- Add `EditText otbrInput`.
- Add an "OTBR base URL, for example http://otbr.local" input under the Thread dataset section.
- Add button `Check OTBR connectivity`.
- Add `HttpOtbrClient`.
- On click, validate non-empty URL, append sanitized "Checking OTBR connectivity at ...", run `otbrClient.checkReadiness(...)` on a worker thread, and append `MainActivityPresentation.otbrConnectivityResult(status)` plus sanitized details.
- Include OTBR URL in save, restore, and persisted config load.

- [ ] **Step 5: Update docs**

Add implemented bullets for OTBR connectivity diagnostics and persisted OTBR base URL. Keep real BLE/PASE/Thread provisioning and connectedhomeip JNI listed as not implemented.

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/config app/src/test/java/org/openhab/matter/companion/config app/src/main/java/org/openhab/matter/companion/ui/AppState.java app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java README.md docs/implementation-status.md
git commit -m "feat: integrate OTBR diagnostics UI"
```

---

## Self-Review

- Spec coverage: This plan implements the `docs/research.md` requirement for a Thread Dataset Setup screen with a basic OTBR connectivity check. It does not claim real BLE scanning, PASE, Thread provisioning, OCW, connectedhomeip JNI, or CameraX QR scanning.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified test commands remain.
- Type consistency: `OtbrClient`, `HttpOtbrClient`, `OtbrStatus`, `otbrBaseUrl`, and `MainActivityPresentation.otbrConnectivityResult(...)` are used consistently.
