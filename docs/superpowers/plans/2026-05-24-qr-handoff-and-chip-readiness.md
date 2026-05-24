# QR Handoff And CHIP Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an offline-buildable QR scan handoff path and make the native CHIP controller seam observable/configurable without crashing when the native library is not bundled.

**Architecture:** Use Android's external scanner intent contract as a dependency-free bridge for QR capture; parsed result ingestion remains in existing Java Matter payload code. Refactor `ChipMatterController` so loading the native library is explicit and testable, then expose readiness status in the UI. Native CameraX decoding and real connectedhomeip commissioning remain not implemented until dependencies/native libraries are available.

**Tech Stack:** Java 17, Android `Intent`, legacy `startActivityForResult` for dependency-free compatibility, JNI `System.loadLibrary`, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/qr/QrScanContract.java` - constants for common external QR scanner intent action/extras.
- `app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java` - creates the external scan intent and extracts scan result text.
- `app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java` - tests intent action/extras and result extraction.
- `app/src/main/java/org/openhab/matter/companion/controller/NativeLibraryLoader.java` - small native library loading interface.
- `app/src/main/java/org/openhab/matter/companion/controller/SystemNativeLibraryLoader.java` - production `System.loadLibrary` implementation.
- `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerConfig.java` - config model for native library name and attestation bypass flag.
- `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerStatus.java` - readiness result model.
- `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java` - remove static library load, add explicit readiness and configuration.
- `app/src/test/java/org/openhab/matter/companion/controller/ChipMatterControllerReadinessTest.java` - tests readiness without native library.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - add scan and native CHIP readiness buttons.
- `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java` - add UI strings.
- `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java` - add presentation tests.
- `README.md` and `docs/implementation-status.md` - update status accurately.

---

### Task 18: External QR Scanner Handoff

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/qr/QrScanContract.java`
- Create: `app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java`
- Test: `app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java`

- [ ] **Step 1: Write failing QR scan intent tests**

Create tests proving:

```java
@Test
public void createsQrScannerIntent() {
    Intent intent = QrScanIntentFactory.createScanIntent();

    assertEquals("com.google.zxing.client.android.SCAN", intent.getAction());
    assertEquals("QR_CODE_MODE", intent.getStringExtra("SCAN_MODE"));
    assertEquals(false, intent.getBooleanExtra("SAVE_HISTORY", true));
}

@Test
public void extractsScanResultFromActivityData() {
    Intent data = new Intent();
    data.putExtra("SCAN_RESULT", "MT:Y.K9042C00KA0648G00");

    assertEquals("MT:Y.K9042C00KA0648G00", QrScanIntentFactory.extractResult(data));
}

@Test
public void missingScanResultReturnsEmptyString() {
    assertEquals("", QrScanIntentFactory.extractResult(new Intent()));
    assertEquals("", QrScanIntentFactory.extractResult(null));
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.qr.QrScanIntentFactoryTest
```

Expected: FAIL because `QrScanIntentFactory` does not exist.

- [ ] **Step 3: Implement QR scan contract**

`QrScanContract`:

```java
package org.openhab.matter.companion.qr;

public final class QrScanContract {
    public static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String EXTRA_SCAN_MODE = "SCAN_MODE";
    public static final String EXTRA_SAVE_HISTORY = "SAVE_HISTORY";
    public static final String EXTRA_SCAN_RESULT = "SCAN_RESULT";
    public static final String QR_CODE_MODE = "QR_CODE_MODE";

    private QrScanContract() {
    }
}
```

`QrScanIntentFactory`:

```java
package org.openhab.matter.companion.qr;

import android.content.Intent;

public final class QrScanIntentFactory {
    private QrScanIntentFactory() {
    }

    public static Intent createScanIntent() {
        Intent intent = new Intent(QrScanContract.ACTION_SCAN);
        intent.putExtra(QrScanContract.EXTRA_SCAN_MODE, QrScanContract.QR_CODE_MODE);
        intent.putExtra(QrScanContract.EXTRA_SAVE_HISTORY, false);
        return intent;
    }

    public static String extractResult(Intent data) {
        if (data == null) {
            return "";
        }
        String result = data.getStringExtra(QrScanContract.EXTRA_SCAN_RESULT);
        return result == null ? "" : result.trim();
    }
}
```

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/qr app/src/test/java/org/openhab/matter/companion/qr
git commit -m "feat: add QR scanner handoff contract"
```

---

### Task 19: Native CHIP Controller Readiness

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/NativeLibraryLoader.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/SystemNativeLibraryLoader.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerConfig.java`
- Create: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterControllerStatus.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ChipMatterController.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ChipMatterControllerReadinessTest.java`

- [ ] **Step 1: Write failing readiness tests**

Create tests proving:

```java
@Test
public void reportsNativeLibraryMissingWithoutThrowingFromConstructor() {
    NativeLibraryLoader loader = name -> {
        throw new UnsatisfiedLinkError("missing " + name);
    };

    ChipMatterController controller = new ChipMatterController(loader, ChipMatterControllerConfig.defaultConfig());
    ChipMatterControllerStatus status = controller.readiness();

    assertFalse(status.ready());
    assertEquals("openhab_matter_chip", status.libraryName());
    assertTrue(status.message().contains("missing openhab_matter_chip"));
}

@Test
public void reportsReadyWhenNativeLibraryLoads() {
    ChipMatterController controller = new ChipMatterController(name -> { }, new ChipMatterControllerConfig(
            "custom_chip",
            true));

    ChipMatterControllerStatus status = controller.readiness();

    assertTrue(status.ready());
    assertEquals("custom_chip", status.libraryName());
    assertTrue(status.attestationBypassEnabled());
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.controller.ChipMatterControllerReadinessTest
```

Expected: FAIL because the readiness classes and constructors do not exist.

- [ ] **Step 3: Implement readiness model and loader**

`NativeLibraryLoader`:

```java
package org.openhab.matter.companion.controller;

public interface NativeLibraryLoader {
    void load(String libraryName);
}
```

`SystemNativeLibraryLoader`:

```java
package org.openhab.matter.companion.controller;

public final class SystemNativeLibraryLoader implements NativeLibraryLoader {
    @Override
    public void load(String libraryName) {
        System.loadLibrary(libraryName);
    }
}
```

`ChipMatterControllerConfig`:

```java
package org.openhab.matter.companion.controller;

public final class ChipMatterControllerConfig {
    private static final String DEFAULT_LIBRARY = "openhab_matter_chip";
    private final String nativeLibraryName;
    private final boolean attestationBypassEnabled;

    public ChipMatterControllerConfig(String nativeLibraryName, boolean attestationBypassEnabled) {
        this.nativeLibraryName = nativeLibraryName == null || nativeLibraryName.trim().isEmpty()
                ? DEFAULT_LIBRARY : nativeLibraryName.trim();
        this.attestationBypassEnabled = attestationBypassEnabled;
    }

    public static ChipMatterControllerConfig defaultConfig() {
        return new ChipMatterControllerConfig(DEFAULT_LIBRARY, false);
    }

    public String nativeLibraryName() {
        return nativeLibraryName;
    }

    public boolean attestationBypassEnabled() {
        return attestationBypassEnabled;
    }
}
```

`ChipMatterControllerStatus`:

```java
package org.openhab.matter.companion.controller;

public final class ChipMatterControllerStatus {
    private final boolean ready;
    private final String libraryName;
    private final boolean attestationBypassEnabled;
    private final String message;

    public ChipMatterControllerStatus(boolean ready, String libraryName, boolean attestationBypassEnabled, String message) {
        this.ready = ready;
        this.libraryName = libraryName == null ? "" : libraryName;
        this.attestationBypassEnabled = attestationBypassEnabled;
        this.message = message == null ? "" : message;
    }

    public boolean ready() { return ready; }
    public String libraryName() { return libraryName; }
    public boolean attestationBypassEnabled() { return attestationBypassEnabled; }
    public String message() { return message; }
}
```

- [ ] **Step 4: Refactor `ChipMatterController`**

Requirements:

- Remove the static `System.loadLibrary(...)` block.
- Add default constructor using `new SystemNativeLibraryLoader()` and `ChipMatterControllerConfig.defaultConfig()`.
- Add package-visible constructor `ChipMatterController(NativeLibraryLoader loader, ChipMatterControllerConfig config)`.
- Add `ChipMatterControllerStatus readiness()`.
- Load the native library lazily once in `readiness()` and before each native call.
- If loading fails, `readiness()` returns not-ready status; controller methods throw `IllegalStateException` with the status message before native calls.

- [ ] **Step 5: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller
git commit -m "feat: add CHIP controller readiness diagnostics"
```

---

### Task 20: UI And Docs Integration

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Add presentation tests**

Add tests proving:

```java
@Test
public void describesExternalQrScannerMissing() {
    assertEquals(
            "No external QR scanner app is installed. Paste the Matter code manually or install a scanner that supports ZXing scan intents.",
            MainActivityPresentation.externalQrScannerMissing());
}

@Test
public void describesNativeChipReadiness() {
    ChipMatterControllerStatus status = new ChipMatterControllerStatus(false, "openhab_matter_chip", false, "missing");

    assertEquals(
            "Native CHIP controller not ready: missing",
            MainActivityPresentation.nativeChipReadiness(status));
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected: FAIL because the helper methods do not exist.

- [ ] **Step 3: Wire QR scan button and result ingestion**

Modify `MainActivity`:

- Add request code `QR_SCAN_REQUEST = 3001`.
- Add button `Scan Matter QR with installed scanner`.
- On click, call `startActivityForResult(QrScanIntentFactory.createScanIntent(), QR_SCAN_REQUEST)`.
- Catch `ActivityNotFoundException` and append `MainActivityPresentation.externalQrScannerMissing()`.
- Override `onActivityResult`; for `QR_SCAN_REQUEST` and `RESULT_OK`, extract result, set `payloadInput`, set `state.setupPayload`, and append `Scanned Matter QR payload and populated setup payload input.`
- If scan result is empty, append `QR scanner returned no Matter payload.`

- [ ] **Step 4: Wire CHIP readiness button**

Modify `MainActivity`:

- Add field `private final ChipMatterController chipMatterController = new ChipMatterController();`
- Add button `Check native CHIP controller`.
- On click, call `chipMatterController.readiness()` and append `MainActivityPresentation.nativeChipReadiness(status)`.

- [ ] **Step 5: Implement presentation helpers**

`MainActivityPresentation.externalQrScannerMissing()` returns exactly the string in Step 1.

`MainActivityPresentation.nativeChipReadiness(ChipMatterControllerStatus status)` returns:

- `Native CHIP controller ready: <libraryName>` when ready.
- `Native CHIP controller not ready: <message>` when not ready.

- [ ] **Step 6: Update docs**

In `README.md`:

- Add implemented bullet: `External QR scanner handoff can populate the Matter setup payload field when a compatible scanner app is installed.`
- Add implemented bullet: `Native CHIP controller readiness diagnostics report whether the JNI library is available.`
- Keep not-implemented wording for `native CameraX QR scanning`.

In `docs/implementation-status.md`:

- Add implemented bullet for external QR scanner handoff.
- Add implemented bullet for CHIP readiness diagnostics.
- Change not implemented `Camera QR scanning` to `Native CameraX QR scanning and in-app QR decoding.`
- Keep real BLE/PASE/Thread/OCW connectedhomeip commissioning not implemented.

- [ ] **Step 7: Verify**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 8: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java README.md docs/implementation-status.md
git commit -m "feat: integrate QR handoff and CHIP readiness UI"
```

---

## Self-Review

- Spec coverage: This plan advances QR capture UX without network dependencies and makes the native CHIP seam safer and observable. It does not claim native CameraX QR decoding or real connectedhomeip commissioning.
- Placeholder scan: No placeholder markers or unspecified commands are present.
- Type consistency: QR classes use `QrScanContract` and `QrScanIntentFactory`; CHIP diagnostics consistently use `ChipMatterControllerConfig`, `NativeLibraryLoader`, and `ChipMatterControllerStatus`.
