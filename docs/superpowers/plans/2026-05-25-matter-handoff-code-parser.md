# Matter Handoff Code Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Accept Matter QR and Verhoeff-validated manual setup/multi-admin codes for openHAB Scan Input handoff while keeping Thread commissioning strict about requiring a long discriminator.

**Architecture:** Add a small handoff-specific parser that validates codes suitable for openHAB's Scan Input without reusing the stricter Thread commissioning parser for manual numeric codes. Wire scan result extraction and Wi-Fi/multi-admin handoff validation through this parser, while leaving `MatterSetupPayloadParser.parse("34970112332")` as a failure for Thread commissioning.

**Tech Stack:** Java domain parser, Android activity wiring, JUnit/Robolectric tests.

---

### Task 1: Add Handoff Parser And Scanner Acceptance

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/domain/MatterHandoffCodeParser.java`
- Create: `app/src/test/java/org/openhab/matter/companion/domain/MatterHandoffCodeParserTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java`
- Modify: `app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [x] **Step 1: Write failing parser tests**

Add tests proving:

```java
assertEquals("MT:Y.K9042C00KA0648G00",
        MatterHandoffCodeParser.parseForOpenHabScanInput("  MT:Y.K9042C00KA0648G00  "));
assertEquals("34970112332",
        MatterHandoffCodeParser.parseForOpenHabScanInput("3497-0112-332"));
assertEquals("34970112332",
        MatterHandoffCodeParser.parseForOpenHabScanInput("34970112332"));
assertEquals("641295075300001000017",
        MatterHandoffCodeParser.parseForOpenHabScanInput("6412-9507-5300-0010-0001-7"));
assertThrows(IllegalArgumentException.class,
        () -> MatterHandoffCodeParser.parseForOpenHabScanInput("34970112333"));
assertThrows(IllegalArgumentException.class,
        () -> MatterHandoffCodeParser.parseForOpenHabScanInput("23276800006"));
assertThrows(IllegalArgumentException.class,
        () -> MatterHandoffCodeParser.parseForOpenHabScanInput("641295075399999999991"));
assertThrows(IllegalArgumentException.class,
        () -> MatterHandoffCodeParser.parseForOpenHabScanInput("pin=20202021;disc=3840"));
assertThrows(IllegalArgumentException.class,
        () -> MatterHandoffCodeParser.parseForOpenHabScanInput("https://example.test"));
```

- [x] **Step 2: Write failing scanner tests**

Extend `QrScanIntentFactoryTest`:

```java
assertEquals("34970112332",
        QrScanIntentFactory.extractMatterSetupPayloadText("  3497-0112-332  "));
```

Keep the existing unsupported URL rejection.

- [x] **Step 3: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*MatterHandoffCodeParserTest" --tests "*QrScanIntentFactoryTest"
```

Expected: FAIL because `MatterHandoffCodeParser` does not exist and scanner extraction still relies only on `MatterSetupPayloadParser`.

- [x] **Step 4: Implement handoff parser**

Implement `MatterHandoffCodeParser.parseForOpenHabScanInput(String input)`:

- Trim input.
- If input starts with `MT:`, validate it with `MatterSetupPayloadParser.parse(input)` and return the trimmed QR payload.
- For manual code input, remove ASCII hyphen and whitespace characters.
- Accept only exactly 11 or 21 digits after normalization.
- Validate the Matter manual-code Verhoeff check digit.
- Reject manual codes that decode to invalid setup PIN values.
- Reject 21-digit manual codes with invalid vendor/product ranges.
- Reject explicit `pin=...;disc=...` fields and unrelated URLs/text.

- [x] **Step 5: Wire scanner extraction**

In `QrScanIntentFactory.extractMatterSetupPayloadText`, call `MatterHandoffCodeParser.parseForOpenHabScanInput(scanResult)` instead of `MatterSetupPayloadParser.parse(scanResult)` and return the normalized handoff code.

- [x] **Step 6: Validate Wi-Fi/multi-admin handoff input**

In `MainActivity.showWifiInstructions()`, validate `payloadInput` with `MatterHandoffCodeParser.parseForOpenHabScanInput`. Store the normalized result in `state.setupPayload` and update the input field when normalization changes. If invalid, append:

```text
Enter a Matter QR payload or 11-digit manual setup/multi-admin code for openHAB Scan Input.
```

- [x] **Step 7: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests "*MatterHandoffCodeParserTest" --tests "*QrScanIntentFactoryTest"
```

Expected: PASS.

- [x] **Step 8: Full verification**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
git diff --check
```

Expected: all commands exit `0`; `git diff --check` may print existing CRLF warnings only.

- [x] **Step 9: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/domain/MatterHandoffCodeParser.java app/src/test/java/org/openhab/matter/companion/domain/MatterHandoffCodeParserTest.java app/src/main/java/org/openhab/matter/companion/qr/QrScanIntentFactory.java app/src/test/java/org/openhab/matter/companion/qr/QrScanIntentFactoryTest.java app/src/main/java/org/openhab/matter/companion/MainActivity.java README.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-matter-handoff-code-parser.md
git commit -m "feat: accept Matter handoff codes"
```

---

## Self-Review

- Spec coverage: This advances the Wi-Fi/multi-admin handoff flow from `docs/research.md` without weakening Thread commissioning validation.
- Placeholder scan: No TODO/TBD/fill-in steps remain.
- Type consistency: Parser method and normalized return value are used consistently by QR extraction and handoff UI validation.
