# openHAB Matter Android Production Continuation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the installable MVP closer to the real app in `docs/research.md` by decoding Matter `MT:` QR payloads locally and adding openHAB readiness diagnostics.

**Architecture:** Keep production-risky Matter commissioning behind `MatterController`, but remove avoidable fake-only UX by parsing QR payloads in Java before commissioning. Add an independent openHAB diagnostics package using `HttpURLConnection` so the UI can validate base URL reachability without AndroidX or new dependencies.

**Tech Stack:** Java 17, Android SDK 36/target 35, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/domain/Base38.java` - Matter Base38 decoder for QR payload bytes.
- `app/src/main/java/org/openhab/matter/companion/domain/BitReader.java` - LSB-first bit reader for QR payload fields.
- `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayload.java` - extend model with numeric vendor/product IDs, commissioning flow, and discovery bitmask.
- `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayloadParser.java` - parse `MT:` payloads instead of marking them unsupported.
- `app/src/test/java/org/openhab/matter/companion/domain/MatterQrPayloadParserTest.java` - QR parsing tests using known Project CHIP payload `MT:Y.K9042C00KA0648G00`.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabClient.java` - small REST client interface.
- `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabClient.java` - production `HttpURLConnection` implementation.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabStatus.java` - readiness result object.
- `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabClient.java` - deterministic fake for UI and JVM tests.
- `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabClientTest.java` - fake diagnostics tests.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - integrate QR decode copy and openHAB readiness button.
- `README.md` and `docs/implementation-status.md` - update status from "CHIP QR payload decoding not implemented" to implemented.

---

### Task 7: Matter MT QR Payload Parsing

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/domain/Base38.java`
- Create: `app/src/main/java/org/openhab/matter/companion/domain/BitReader.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayload.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/domain/MatterSetupPayloadParser.java`
- Test: `app/src/test/java/org/openhab/matter/companion/domain/MatterQrPayloadParserTest.java`

- [ ] **Step 1: Write failing QR parser tests**

Create `app/src/test/java/org/openhab/matter/companion/domain/MatterQrPayloadParserTest.java`:

```java
package org.openhab.matter.companion.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class MatterQrPayloadParserTest {
    @Test
    public void parsesKnownProjectChipQrPayload() {
        MatterSetupPayload payload = MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G00");

        assertEquals("MT:Y.K9042C00KA0648G00", payload.rawPayload());
        assertEquals(20202021L, payload.pin());
        assertEquals(3840, payload.discriminator());
        assertEquals(65521, payload.vendorId());
        assertEquals(32768, payload.productId());
        assertEquals(0, payload.commissioningFlow());
        assertEquals(2, payload.discoveryCapabilities());
        assertFalse(payload.requiresChipParser());
    }

    @Test
    public void rejectsQrPayloadWithWrongPrefix() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("XX:Y.K9042C00KA0648G00"));
    }

    @Test
    public void rejectsQrPayloadWithInvalidBase38Character() {
        assertThrows(IllegalArgumentException.class, () -> MatterSetupPayloadParser.parse("MT:Y.K9042C00KA0648G0_"));
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: FAIL because QR payload still reports `requiresChipParser=true` or missing QR parser classes.

- [ ] **Step 3: Implement Base38 and bit decoding**

Create `Base38` to decode Matter Base38 groups using alphabet `0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-.`, little-endian numeric groups, and group sizes 5 chars -> 3 bytes, 4 chars -> 2 bytes, 2 chars -> 1 byte.

Create `BitReader` to read unsigned values LSB-first from decoded bytes.

- [ ] **Step 4: Extend payload model and parser**

Add `vendorId`, `productId`, `commissioningFlow`, and `discoveryCapabilities` to `MatterSetupPayload`.

Parse QR fields in this order: version 3 bits, vendor ID 16, product ID 16, commissioning flow 2, discovery capabilities 8, long discriminator 12, setup PIN 27, padding 4.

- [ ] **Step 5: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/domain app/src/test/java/org/openhab/matter/companion/domain
git commit -m "feat: decode Matter QR setup payloads"
```

---

### Task 8: openHAB Readiness Diagnostics

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabClient.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabStatus.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/FakeOpenHabClient.java`
- Test: `app/src/test/java/org/openhab/matter/companion/openhab/FakeOpenHabClientTest.java`

- [ ] **Step 1: Write failing fake-client tests**

Create `FakeOpenHabClientTest` proving healthy and unhealthy status messages.

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: FAIL because openHAB client classes do not exist.

- [ ] **Step 3: Implement status model and clients**

`OpenHabStatus` should expose `online()`, `message()`, and `details()`. `OpenHabClient` should expose `OpenHabStatus checkReadiness(String baseUrl) throws Exception`.

`HttpOpenHabClient` should call `/rest/` with 3 second connect/read timeouts and return online for HTTP 2xx or 3xx. It should return offline status for malformed URLs, connection errors, or non-success HTTP responses.

`FakeOpenHabClient` should return deterministic online/offline values for JVM tests and UI demo mode.

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
git commit -m "feat: add openHAB readiness diagnostics"
```

---

### Task 9: UI Integration for QR Decode and openHAB Diagnostics

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/ui/AppState.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Add UI controls**

Add an openHAB base URL text field and a "Check openHAB readiness" button. Change the `MT:` handling path to show decoded PIN/discriminator/vendor/product metadata without telling the user a CHIP parser is required.

- [ ] **Step 2: Keep secrets out of logs**

Do not echo Thread dataset. Do not echo setup PIN except as decoded setup metadata explicitly needed for commissioning; never include it in error messages.

- [ ] **Step 3: Verify build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS and APK generated.

- [ ] **Step 4: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/ui/AppState.java README.md docs/implementation-status.md
git commit -m "feat: integrate QR decoding and openHAB diagnostics"
```

---

## Self-Review

- Spec coverage: This plan directly addresses two missing items listed in `docs/implementation-status.md`: CHIP QR payload decoding for `MT:` payloads and openHAB REST health checks.
- Scope: Real BLE/PASE/Thread commissioning remains blocked on connectedhomeip JNI integration and is not claimed here.
- Source note: QR field layout is based on Project CHIP `SetupPayload.h`; Base38 behavior is based on Project CHIP setup payload decoding.
