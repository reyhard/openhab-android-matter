# Encrypted Storage And SSE Inbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace plaintext Thread dataset persistence with an Android Keystore-backed encrypted storage seam and add openHAB SSE Inbox observation so the app can confirm Matter Inbox discovery after the user enters the temporary setup code.

**Architecture:** Keep encryption isolated behind a small `SecretCodec` boundary so JVM tests can verify AES-GCM behavior while Android production uses `AndroidKeyStore`. Add an SSE client beside the existing polling Inbox client, with a pure Java SSE parser that can be tested without network access. Wire both features into `MainActivity` only after the independently testable pieces are green.

**Tech Stack:** Java 17, Android Keystore, AES/GCM/NoPadding, `HttpURLConnection`, Server-Sent Events text parsing, JUnit 4.13.2, no new external dependencies.

---

## File Structure

- `app/src/main/java/org/openhab/matter/companion/config/SecretCodec.java` - small encode/decode interface for sensitive config values.
- `app/src/main/java/org/openhab/matter/companion/config/AesGcmSecretCodec.java` - JVM-testable AES-GCM implementation using an injected `SecretKey`.
- `app/src/main/java/org/openhab/matter/companion/config/AndroidKeystoreSecretCodec.java` - Android production codec backed by `AndroidKeyStore`.
- `app/src/main/java/org/openhab/matter/companion/config/SecureAppConfigMapper.java` - encrypts/decrypts sensitive fields and supports plaintext migration reads.
- `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java` - uses `SecureAppConfigMapper`; saves Thread dataset encrypted.
- `app/src/test/java/org/openhab/matter/companion/config/AesGcmSecretCodecTest.java` - round-trip and tamper tests.
- `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java` - encrypted save value and plaintext migration tests.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxEvent.java` - SSE event model.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxEventListener.java` - event callback interface.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxSseParser.java` - pure parser for SSE events.
- `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxSseClient.java` - blocking `/rest/events?topics=openhab/inbox/*` client.
- `app/src/test/java/org/openhab/matter/companion/openhab/OpenHabInboxSseParserTest.java` - parser behavior tests.
- `app/src/test/java/org/openhab/matter/companion/openhab/OpenHabInboxSseClientTest.java` - small local HTTP server test for URL and callback behavior.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - constructs encrypted repository and adds SSE observation action.
- `README.md` and `docs/implementation-status.md` - update feature status and remaining limitations.

---

### Task 15: Encrypted Thread Dataset Storage

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/config/SecretCodec.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/AesGcmSecretCodec.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/AndroidKeystoreSecretCodec.java`
- Create: `app/src/main/java/org/openhab/matter/companion/config/SecureAppConfigMapper.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/config/SharedPreferencesAppConfigRepository.java`
- Test: `app/src/test/java/org/openhab/matter/companion/config/AesGcmSecretCodecTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/config/SecureAppConfigMapperTest.java`

- [ ] **Step 1: Write failing AES-GCM codec tests**

Create `AesGcmSecretCodecTest` with these exact behaviors:

```java
@Test
public void encodedValueDoesNotContainPlaintextAndCanBeDecoded() throws Exception {
    SecretKey key = new SecretKeySpec(new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16
    }, "AES");
    SecretCodec codec = new AesGcmSecretCodec(key, new SecureRandom(new byte[] {9, 8, 7, 6}));

    String encoded = codec.encode("hex:00112233445566778899aabbccddeeff");

    assertTrue(encoded.startsWith("enc:v1:"));
    assertFalse(encoded.contains("00112233445566778899"));
    assertEquals("hex:00112233445566778899aabbccddeeff", codec.decode(encoded));
}

@Test(expected = GeneralSecurityException.class)
public void tamperedValueCannotBeDecoded() throws Exception {
    SecretKey key = new SecretKeySpec(new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16
    }, "AES");
    SecretCodec codec = new AesGcmSecretCodec(key, new SecureRandom(new byte[] {1, 2, 3, 4}));

    String encoded = codec.encode("hex:00112233445566778899aabbccddeeff");
    codec.decode(encoded.substring(0, encoded.length() - 2) + "AA");
}
```

- [ ] **Step 2: Write failing mapper tests**

Create `SecureAppConfigMapperTest` proving encrypted save values and plaintext migration:

```java
@Test
public void encodesThreadDatasetButLeavesOpenHabUrlPlaintext() throws Exception {
    SecretCodec codec = new FixedSecretCodec();
    SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

    SecureAppConfigMapper.StoredConfig stored = mapper.toStoredValues(
            new AppConfig("hex:001122", "http://openhab.local:8080"));

    assertEquals("encoded(hex:001122)", stored.threadDataset());
    assertEquals("http://openhab.local:8080", stored.openHabBaseUrl());
}

@Test
public void readsLegacyPlaintextThreadDatasetForMigration() throws Exception {
    SecretCodec codec = new FixedSecretCodec();
    SecureAppConfigMapper mapper = new SecureAppConfigMapper(codec);

    AppConfig config = mapper.fromStoredValues("hex:legacy", "http://openhab.local:8080");

    assertEquals("hex:legacy", config.threadDataset());
    assertEquals("http://openhab.local:8080", config.openHabBaseUrl());
}
```

Use this test helper:

```java
private static final class FixedSecretCodec implements SecretCodec {
    @Override
    public String encode(String plaintext) {
        return "encoded(" + plaintext + ")";
    }

    @Override
    public String decode(String encoded) {
        return encoded.substring("encoded(".length(), encoded.length() - 1);
    }
}
```

- [ ] **Step 3: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.config.AesGcmSecretCodecTest --tests org.openhab.matter.companion.config.SecureAppConfigMapperTest
```

Expected: FAIL because the new classes do not exist.

- [ ] **Step 4: Implement encryption classes**

`SecretCodec`:

```java
package org.openhab.matter.companion.config;

public interface SecretCodec {
    String ENCRYPTED_PREFIX = "enc:v1:";

    String encode(String plaintext) throws Exception;

    String decode(String encoded) throws Exception;
}
```

`AesGcmSecretCodec` requirements:

- Use `AES/GCM/NoPadding`.
- Use 12-byte IV.
- Use 128-bit GCM tag.
- Encode as `enc:v1:<base64(iv)>:<base64(ciphertext)>`.
- Return `""` for null or empty plaintext.
- Throw `GeneralSecurityException` when encrypted payload is malformed or authentication fails.

`AndroidKeystoreSecretCodec` requirements:

- Alias: `openhab_matter_thread_dataset`.
- Key store: `AndroidKeyStore`.
- Key generator algorithm: `KeyProperties.KEY_ALGORITHM_AES`.
- Purposes: encrypt and decrypt.
- Block mode: `KeyProperties.BLOCK_MODE_GCM`.
- Padding: `KeyProperties.ENCRYPTION_PADDING_NONE`.
- Randomized encryption required.
- Delegate actual encode/decode to `AesGcmSecretCodec`.

`SecureAppConfigMapper` requirements:

- `toStoredValues(AppConfig)` encrypts only `threadDataset`.
- `toStoredValues(AppConfig)` stores `openHabBaseUrl` unchanged.
- `fromStoredValues(String, String)` decodes `threadDataset` only when it starts with `SecretCodec.ENCRYPTED_PREFIX`.
- `fromStoredValues(String, String)` returns legacy plaintext Thread dataset unchanged when no encrypted prefix is present.
- If encrypted decode fails, return an empty Thread dataset and preserve openHAB URL rather than crashing app startup.

- [ ] **Step 5: Wire repository to mapper**

Modify `SharedPreferencesAppConfigRepository`:

- Default constructor creates `new SecureAppConfigMapper(new AndroidKeystoreSecretCodec())`.
- Add package-visible constructor for tests or future Android instrumentation:

```java
SharedPreferencesAppConfigRepository(SharedPreferences preferences, SecureAppConfigMapper mapper)
```

- `load()` calls `mapper.fromStoredValues(preferences.getString(KEY_THREAD_DATASET, ""), preferences.getString(KEY_OPENHAB_BASE_URL, ""))`.
- `save()` calls `mapper.toStoredValues(config)` and writes stored values.

- [ ] **Step 6: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/config app/src/test/java/org/openhab/matter/companion/config
git commit -m "feat: encrypt Thread dataset config"
```

---

### Task 16: openHAB Inbox SSE Observation

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxEvent.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxEventListener.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxSseParser.java`
- Create: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxSseClient.java`
- Test: `app/src/test/java/org/openhab/matter/companion/openhab/OpenHabInboxSseParserTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/openhab/OpenHabInboxSseClientTest.java`

- [ ] **Step 1: Write failing parser tests**

Create `OpenHabInboxSseParserTest` with these behaviors:

```java
@Test
public void parsesMatterInboxEventFromSseBlock() {
    OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
            "event: message\n"
                    + "data: {\"topic\":\"openhab/inbox/matter:node:abc/added\",\"payload\":{\"thingUID\":\"matter:node:abc\"}}\n"
                    + "\n");

    assertTrue(event.matterEntryDetected());
    assertEquals("openhab/inbox/matter:node:abc/added", event.topic());
}

@Test
public void parsesMultiLineDataEvent() {
    OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
            "data: {\"topic\":\"openhab/inbox/other/added\",\n"
                    + "data: \"payload\":{\"thingUID\":\"matter:node:abc\"}}\n"
                    + "\n");

    assertTrue(event.matterEntryDetected());
}

@Test
public void ignoresNonMatterInboxEvent() {
    OpenHabInboxEvent event = OpenHabInboxSseParser.parse(
            "data: {\"topic\":\"openhab/items/Switch/state\",\"payload\":\"ON\"}\n\n");

    assertFalse(event.matterEntryDetected());
}
```

- [ ] **Step 2: Write failing local HTTP SSE client test**

Create `OpenHabInboxSseClientTest` with a one-shot `ServerSocket` server, matching the existing style in `HttpOpenHabInboxClientTest`. It must prove:

- Client requests `/rest/events?topics=openhab/inbox/*`.
- Client invokes listener when the SSE data contains a Matter Inbox entry.
- Client exits after the listener returns `false`.

Use listener shape:

```java
OpenHabInboxEventListener listener = event -> {
    received.add(event);
    return false;
};
```

- [ ] **Step 3: Verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.openhab.OpenHabInboxSseParserTest --tests org.openhab.matter.companion.openhab.OpenHabInboxSseClientTest
```

Expected: FAIL because the SSE classes do not exist.

- [ ] **Step 4: Implement SSE event model and parser**

`OpenHabInboxEvent` exposes:

```java
public String topic()
public String rawData()
public boolean matterEntryDetected()
```

`OpenHabInboxEventListener` exposes:

```java
boolean onEvent(OpenHabInboxEvent event);
```

`OpenHabInboxSseParser.parse(String block)` requirements:

- Collect all `data:` lines in order, stripping only the optional single leading space after `data:`.
- Join multiline data using `\n`.
- Extract topic with a small deterministic scan for `"topic":"..."`.
- Set `matterEntryDetected` when raw data contains `matter:` or `"bindingId":"matter"`.
- Return an event with empty topic and raw data when the block has no data lines.

- [ ] **Step 5: Implement SSE HTTP client**

`OpenHabInboxSseClient.observe(String baseUrl, OpenHabInboxEventListener listener)` requirements:

- Normalize URL to `<baseUrl>/rest/events?topics=openhab/inbox/*`.
- Use `HttpURLConnection`.
- Set method `GET`.
- Set `Accept: text/event-stream`.
- Use connect timeout 3000 ms and read timeout 15000 ms.
- Treat non-2xx response as `IOException`.
- Read line-by-line until EOF or listener returns `false`.
- Accumulate lines until a blank line, then parse and deliver one event.
- Disconnect in `finally`.

- [ ] **Step 6: Verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/openhab app/src/test/java/org/openhab/matter/companion/openhab
git commit -m "feat: add openHAB Inbox SSE observation"
```

---

### Task 17: UI And Docs Integration

**Files:**
- Modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Add SSE observation action**

Add a button labeled `Watch openHAB Inbox SSE`.

The click handler must:

- Read `openHabInput`.
- If empty, append `Enter an openHAB base URL first.`
- Start a background thread named `openhab-inbox-sse`.
- Append `Watching openHAB Inbox SSE at <safe-url> ...`.
- Call `new OpenHabInboxSseClient().observe(baseUrl, event -> { ... })`.
- In the listener, post to UI thread:
  - `openHAB Inbox SSE: Matter Inbox entry detected.` when `event.matterEntryDetected()` is true.
  - `openHAB Inbox SSE: event received but no Matter Inbox entry detected yet.` otherwise.
- Return `false` from the listener after a Matter entry is detected; return `true` for non-Matter events.
- On exception, post `openHAB Inbox SSE observation failed: <safe details>`.

- [ ] **Step 2: Update encrypted storage UI wording**

Change save confirmation from:

```text
Saved Thread dataset and openHAB base URL. Setup payloads and PINs are not saved.
```

to:

```text
Saved Thread dataset in encrypted app storage and saved openHAB base URL. Setup payloads and PINs are not saved.
```

- [ ] **Step 3: Update docs**

In `README.md`, add current MVP bullets:

- `Encrypted app-private storage for the OTBR Thread dataset using Android Keystore-backed AES-GCM.`
- `openHAB Inbox SSE observation via /rest/events?topics=openhab/inbox/*.`

In `docs/implementation-status.md`:

- Move encrypted Thread dataset storage into Implemented.
- Move openHAB SSE streaming of Inbox changes into Implemented.
- Keep real BLE scanning, PASE, attestation, Thread provisioning, OCW, connectedhomeip JNI, and Camera QR scanning in Not Implemented Yet.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: PASS and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/org/openhab/matter/companion/MainActivity.java README.md docs/implementation-status.md
git commit -m "feat: integrate encrypted storage and SSE observation"
```

---

## Self-Review

- Spec coverage: This plan advances two explicit remaining gaps from `docs/research.md`: secure Thread credential handling and openHAB SSE Inbox confirmation. It does not claim to implement Camera QR scanning or the connectedhomeip/CHIP JNI controller.
- Placeholder scan: No placeholder markers or unspecified test commands are present.
- Type consistency: The storage boundary consistently uses `SecretCodec`, `AesGcmSecretCodec`, `AndroidKeystoreSecretCodec`, and `SecureAppConfigMapper`; the SSE boundary consistently uses `OpenHabInboxEvent`, `OpenHabInboxEventListener`, `OpenHabInboxSseParser`, and `OpenHabInboxSseClient`.
