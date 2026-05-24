# Permissions And Inbox Observation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add runtime permission readiness for future BLE commissioning and openHAB Inbox observation after the user enters the generated setup code.

**Architecture:** Keep permission decisions in a JVM-testable Android-independent planner, with `MainActivity` only responsible for requesting permissions. Add an openHAB Inbox client beside the existing REST readiness client, using simple HTTP and deterministic parsing to detect Matter inbox entries without adding dependencies.

**Tech Stack:** Java 17, Android runtime permissions, `HttpURLConnection`, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/permissions/CommissioningPermissionPlanner.java` - maps Android SDK versions to required runtime permissions.
- `app/src/test/java/org/openhab/matter/companion/permissions/CommissioningPermissionPlannerTest.java` - permission mapping tests.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxClient.java` - Inbox observation interface.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxStatus.java` - observation result model.
- `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabInboxClient.java` - `/rest/inbox` HTTP client.
- `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabInboxClient.java` - deterministic fake for tests.
- `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabInboxClientTest.java` - fake observation tests.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - request permissions and add Inbox observation button.
- `README.md` and `docs/implementation-status.md` - update feature status.

---

### Task 12: Commissioning Runtime Permission Planner

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/permissions/CommissioningPermissionPlanner.java`
- Test: `app/src/test/java/org/openhab/matter/companion/permissions/CommissioningPermissionPlannerTest.java`

- [ ] **Step 1: Write failing permission planner tests**

Create tests proving:

```java
assertEquals(Arrays.asList("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"), CommissioningPermissionPlanner.requiredPermissions(31));
assertEquals(Arrays.asList("android.permission.ACCESS_FINE_LOCATION"), CommissioningPermissionPlanner.requiredPermissions(30));
assertEquals(Collections.emptyList(), CommissioningPermissionPlanner.requiredPermissions(22));
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: FAIL because `CommissioningPermissionPlanner` does not exist.

- [ ] **Step 3: Implement permission planner**

For SDK 31+, return `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`. For SDK 23-30, return `ACCESS_FINE_LOCATION`. Below SDK 23, return an empty list.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/permissions app/src/test/java/org/openhab/matter/companion/permissions
git commit -m "feat: add commissioning permission planner"
```

---

### Task 13: openHAB Inbox Observation Client

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxStatus.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabInboxClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabInboxClient.java`
- Test: `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabInboxClientTest.java`

- [ ] **Step 1: Write failing fake inbox tests**

Create tests proving online/detected and online/no-Matter-device statuses.

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: FAIL because Inbox classes do not exist.

- [ ] **Step 3: Implement Inbox observation classes**

`OpenHabInboxStatus` exposes `reachable()`, `matterEntryDetected()`, `message()`, and `details()`.

`OpenHabInboxClient` exposes `OpenHabInboxStatus checkInbox(String baseUrl) throws Exception`.

`HttpOpenHabInboxClient` calls normalized `<baseUrl>/rest/inbox`, reads up to a small bounded response string, and reports Matter detected when the response contains `matter:` or `"bindingId":"matter"`.

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/openhab app/src/test/java/org/openhab/matter/companion/openhab
git commit -m "feat: add openHAB Inbox observation"
```

---

### Task 14: UI Integration For Permissions And Inbox

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Add permission request action**

Add a "Check commissioning permissions" button that requests permissions from `CommissioningPermissionPlanner.requiredPermissions(Build.VERSION.SDK_INT)`. Log whether runtime permissions are needed or requested.

- [ ] **Step 2: Add Inbox observation action**

Add a "Check openHAB Inbox" button that calls `HttpOpenHabInboxClient` off the UI thread using the configured base URL. Log whether a Matter Inbox entry was detected.

- [ ] **Step 3: Update docs**

Move openHAB Inbox observation and runtime permission readiness into implemented status. Keep real BLE/PASE/Thread/OCW listed as not implemented.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS and debug APK generated.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java README.md docs/implementation-status.md
git commit -m "feat: integrate permissions and Inbox observation"
```

---

## Self-Review

- Spec coverage: Covers runtime permission prerequisites for BLE work and the research note that the app can poll/observe openHAB Inbox after user entry.
- Scope: This does not implement CameraX scanning or real BLE/Thread commissioning.
- No placeholders: All classes, commands, and expected outcomes are specified.
