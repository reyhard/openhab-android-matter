# openHAB Inbox Node Match Implementation Plan

**Goal:** Detect a fresh Matter entry in a large openHAB Inbox without mistaking ignored entries for a successful discovery.

**Background:** A real 67-entry, 20,077-byte Inbox placed the new Matter entry beyond the client's 16 KiB cutoff. Eight older ignored entries were inside the cutoff, producing a misleading pre-existing-entry failure. The automated workflow also compares Matter thingUIDs against a baseline. The same physical device has different node IDs on the phone's and openHAB's fabrics, so node-ID equality cannot reliably identify it across fabrics.

**Architecture:** Read bounded JSON to EOF and parse complete top-level entries without a new dependency. Preserve legacy Inbox status APIs. Poll for non-ignored Matter entries, preferring labels confirmed by commissioning metadata; node-ID equality is optional corroboration, not a requirement or independent success signal. Remove baseline snapshots.

**Tech Stack:** Java, Kotlin, Android, plain JUnit, local `OneShotHttpServer`.

---

## Files Touched

- Modify: `app/src/main/java/org/openhab/matter/companion/openhab/HttpOpenHabInboxClient.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/openhab/OpenHabInboxStatus.java`
- Create if useful: a package-private Inbox JSON scanner / entry model in the same Java package.
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupPorts.kt`, `AndroidMatterSetupPorts.kt`, `MatterSetupWorkflow.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt`
- Test: `app/src/test/java/org/openhab/matter/companion/openhab/HttpOpenHabInboxClientTest.java`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflowTest.kt`, `AndroidMatterSetupPortsTest.kt`, and relevant Inbox polling tests.

---

### Task 1: Parse Complete Inbox Entries

- [ ] **Step 1: Write failing HTTP client tests.** With `OneShotHttpServer`, return a roughly 20 KiB array of 67 entries with eight old Matter entries and the target at index 62, beyond 16 KiB. Assert that the target and its `thingUID`, `properties.nodeId`, `label`, and `flag` are parsed, and that a different node does not match.
- [ ] **Step 2: Cover parser boundaries.** Test a complete entry followed by a truncated trailing object, missing `properties.nodeId` with a matching thingUID suffix, escaped strings, reordered fields, nested unrelated values, whitespace, and non-Matter entries. An incomplete object must not create a false match or throw.
- [ ] **Step 3: Implement bounded reading and scanning.** Raise `MAX_RESPONSE_BYTES` to `8 * 1024 * 1024`; read to EOF within that bound. Detect an exceeded bound explicitly and report a truncated/incomplete response rather than silently treating it as complete. Replace regex and substring detection with a dependency-free scanner of the top-level JSON array; handle JSON escapes and nesting, retain complete entries preceding a truncated tail, and avoid using text inside unrelated fields as an entry.
- [ ] **Step 4: Extend status additively.** Expose immutable parsed entries and Matter node IDs plus `containsNodeId(long)` in `OpenHabInboxStatus`. Keep existing constructors, `matterEntryDetected()`, and `matterEntryIds()` working for `MainActivity` and fake clients; derive IDs from parsed Matter entries without synthetic regex/hash matches.
- [ ] **Step 5: Implement exact node matching.** For Matter entries, compare decimal `properties.nodeId`, a whole `thingUID` suffix after `:`, or hexadecimal node ID representation; support `0x` and hex case consistently without substring matches. Preserve unsigned 64-bit text using unsigned conversion or `BigInteger`, never signed decimal parsing that rejects values above `Long.MAX_VALUE`.
- [ ] **Step 6: Verify.** Run `:app:testDebugUnitTest --tests "org.openhab.matter.companion.openhab.HttpOpenHabInboxClientTest"`; expect the new and existing HTTP tests to pass.

---

### Task 2: Pass Discovery Context Through Workflow And Ports

- [ ] **Step 1: Write failing workflow and port tests.** Assert available commissioning vendor and product names reach the Inbox waiter, an already-present `NEW` entry succeeds, and only ignored entries fail. Verify differing phone and openHAB node IDs do not prevent success.
- [ ] **Step 2: Change the port contract.** Pass optional vendor/product and, if available, node ID as discovery context; carry whether the selected entry has a confirmed label in `InboxResult`. Remove `baselineAwareInboxWaiter`, `inboxReader`, and unused baseline APIs; update fakes and constructor callers.
- [ ] **Step 3: Update automated setup.** Remove the pre-scan Inbox snapshot entirely. After scan submission, pass `commission.vendorName` and `commission.productName` to the wait; missing metadata must not block discovery. An entry already `NEW` before the scan is still eligible.
- [ ] **Step 4: Update the other caller.** Migrate the staged-device OCW retry in `MatterSetupViewModel` to the same contract, using available window metadata and bootstrap node ID only as optional context. Remove stale overloads and references after all callers compile.
- [ ] **Step 5: Verify.** Run the workflow and Android ports JUnit classes via `:app:testDebugUnitTest --tests`; expect metadata forwarding, cross-fabric IDs, already-present `NEW`, and ignored-only tests to pass.

---

### Task 3: Poll For Fresh Matter Entries And Report Accuracy

- [ ] **Step 1: Add selection and polling tests.** Replay the real 67-entry, 20,077-byte response exactly, with the `NEW` target at byte 18,683, through `OneShotHttpServer`; assert detection. Test all Matter entries `IGNORED`, missing flag, non-Matter `NEW`, truncated trailing object, a later `NEW` entry, unreachable Inbox, and `matterEntryIds()` legacy compatibility.
- [ ] **Step 2: Define deterministic selection.** Only entries whose `thingUID` starts with `matter:` and whose explicit flag is not `IGNORED` qualify; treat a missing or blank flag as ineligible to avoid false positives. When both vendor and product are available, prefer eligible entries whose label contains both case-insensitively. If none matches, accept any eligible entry as **unconfirmed**, including an unrelated `NEW` entry; test this explicitly and do not claim it is the commissioned device. Missing metadata also yields unconfirmed selection.
- [ ] **Step 3: Implement polling.** Use the selection rule on every response until an eligible entry or timeout; delete `matterEntryDetectedAfterBaseline`. A node-ID match may provide additional positive context only for an eligible entry, never override an ignored/missing flag or serve as sole success criterion. Test unsigned `10051033818082827676` and mismatched fabric IDs. Preserve timeout, interruption, and sanitized diagnostics.
- [ ] **Step 4: Update messaging.** A label-confirmed entry on the first poll may be reported as “openHAB already knows this device”; do not claim the scan created it. For unconfirmed entries, report that a fresh Matter Inbox entry was found but its identity could not be confirmed. Reserve the existing no-fresh-entry failure text for genuinely no eligible entry; distinguish Inbox errors.
- [ ] **Step 5: Verify.** Run the HTTP and polling-related unit tests; confirm the exact replay, ignored-only, unrelated `NEW`, truncated-tail, and compatibility cases pass.

---

### Task 4: Full Verification And Review

- [ ] **Step 1: Run all unit tests.**

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

- [ ] **Step 2: Review boundaries.** Confirm no new JSON dependency, `matterEntryIds()` compatibility, no connectedhomeip fail-closed gate changes, and no sensitive setup codes or fabric material in fixtures or logs.
- [ ] **Step 3: Review identity limitations.** Phone node `0x3FE921A7A8C9DA6C` and openHAB node `0x8B7C72014043DD9C` refer to the same real device on different fabrics. Confirm node-ID equality is never required or sufficient, unsigned parsing is covered, and an unrelated fresh entry is reported as unconfirmed rather than positively identified.

## Acceptance Criteria

- Exact replay of the 67-entry, 20,077-byte response finds its `NEW` target at byte 18,683; complete entries survive a truncated trailing object without false positives.
- Only explicit non-`IGNORED` Matter entries qualify; missing flags and ignored-only payloads do not. Label-confirmed entries are preferred; unrelated `NEW` entries are accepted only as unconfirmed discoveries.
- An already-present `NEW` entry succeeds without a baseline or a claim that this scan created it. Node-ID matching is optional corroboration only, and unsigned IDs are preserved.
- Legacy Inbox consumers compile and behave as before; the connectedhomeip readiness gate remains fail-closed.
- The full unit-test command passes, or any environmental blocker is reported explicitly.
