# Real Hardware Install Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the remaining work required to install the APK on a real Android device and validate the `docs/research.md` Matter-over-Thread flow end to end with openHAB.

**Architecture:** Keep the simulated controller as the safe fallback, but validate the existing connectedhomeip Java-controller path with real artifacts and hardware. Treat every hardware-dependent result as evidence gathered through scripts, app logs, openHAB state, and OTBR state rather than assuming green JVM tests prove production readiness.

**Tech Stack:** Android Gradle Plugin 8.11.1, Java 17, Android SDK 36, connectedhomeip Android CHIPTool-style artifacts, PowerShell install helper, ADB, openHAB REST API, OTBR CLI or web endpoint.

---

## File Structure

- `docs/completion-audit.md` - current evidence and requirement-by-requirement completion status.
- `docs/chip-jni-integration.md` - artifact layout, Java controller seam, and hardware acceptance criteria.
- `README.md` - operator-facing build, artifact packaging, and install instructions.
- `scripts/install_debug.ps1` - build/test/APK verification and ADB install helper.
- `scripts/test_connectedhomeip_artifacts.ps1` - synthetic validation of connectedhomeip artifact gate behavior.
- `app/build.gradle` - Gradle artifact packaging and verification gates.
- `app/src/main/java/org/openhab/matter/companion/controller/` - connectedhomeip controller integration and diagnostics.
- `app/src/main/java/org/openhab/matter/companion/openhab/` - openHAB readiness, Inbox polling, and SSE clients.
- `app/src/main/java/org/openhab/matter/companion/otbr/` - OTBR connectivity client.

---

### Task 1: Produce Real connectedhomeip Android Controller Artifacts

**Files:**
- External input: connectedhomeip build output
- Verify: `docs/chip-jni-integration.md`
- Verify: `README.md`

- [ ] **Step 1: Build or obtain CHIPTool-style artifacts**

Produce a directory with this exact layout:

```text
$ArtifactDir/CHIPController.jar
$ArtifactDir/CHIPInteractionModel.jar
$ArtifactDir/CHIPClusterID.jar
$ArtifactDir/CHIPClusters.jar
$ArtifactDir/AndroidPlatform.jar
$ArtifactDir/OnboardingPayload.jar
$ArtifactDir/libMatterTlv.jar
$ArtifactDir/libMatterJson.jar
$ArtifactDir/jniLibs/arm64-v8a/libCHIPController.so
$ArtifactDir/jniLibs/arm64-v8a/libc++_shared.so
$ArtifactDir/jniLibs/armeabi-v7a/libCHIPController.so
$ArtifactDir/jniLibs/armeabi-v7a/libc++_shared.so
$ArtifactDir/jniLibs/x86/libCHIPController.so
$ArtifactDir/jniLibs/x86/libc++_shared.so
$ArtifactDir/jniLibs/x86_64/libCHIPController.so
$ArtifactDir/jniLibs/x86_64/libc++_shared.so
```

- [ ] **Step 2: Reject placeholders before packaging**

Run:

```powershell
Get-ChildItem $ArtifactDir -Recurse -File | Select-Object FullName,Length
```

Expected:

```text
Every listed jar and native library exists and has Length greater than 0.
```

- [ ] **Step 3: Build with real artifacts**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=$ArtifactDir"
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 4: Commit only documentation updates if artifact instructions changed**

Do not commit generated jars, `.so` files, Gradle build outputs, or local artifact directories. If build evidence changes required operator instructions, update docs and commit:

```powershell
git add README.md docs/chip-jni-integration.md docs/completion-audit.md
git commit -m "docs: capture connectedhomeip artifact readiness"
```

---

### Task 2: Install APK On One Real Android Device

**Files:**
- Verify: `scripts/install_debug.ps1`
- Evidence: command output from the install helper

- [ ] **Step 1: Connect one Android device**

Enable USB debugging and verify exactly one ready device:

```powershell
adb devices
```

Expected:

```text
List of devices attached
$Serial	device
```

- [ ] **Step 2: Install artifact-enabled APK**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -ChipControllerArtifactsDir $ArtifactDir
```

Expected:

```text
Debug APK ready: $ApkPath\app-debug.apk ($ApkSize bytes)
Installing on $Serial ...
Success
```

- [ ] **Step 3: Record install evidence**

Update `docs/completion-audit.md` with:

```markdown
- Real-device install verified on the actual device model / Android the actual Android version using connectedhomeip artifacts from the actual connectedhomeip artifact source.
```

Commit:

```powershell
git add docs/completion-audit.md
git commit -m "docs: record real device install evidence"
```

---

### Task 3: Validate Runtime Controller Selection With Real Artifacts

**Files:**
- Runtime: Android app
- Verify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerFactory.java`
- Verify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpFabricRestoreProbe.java`

- [ ] **Step 1: Launch the app and check controller artifacts**

In the app, tap:

```text
Check connectedhomeip controller
```

Expected log:

```text
connectedhomeip Android controller artifacts ready: CHIPController.
```

- [ ] **Step 2: Select connectedhomeip controller**

Tap:

```text
Use connectedhomeip controller if ready
```

Expected log:

```text
Matter controller selection: Using native Matter controller: the selected connectedhomeip candidate name
```

- [ ] **Step 3: Record failures with sanitized details**

If selection fails, copy only sanitized app log text. Do not copy Thread datasets, Matter setup PINs, QR payloads, or opaque controller state into docs.

---

### Task 4: Validate BLE Thread Commissioning Against A Real Device

**Files:**
- Runtime: Android app
- Runtime: OTBR CLI or OTBR diagnostic endpoint
- Verify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Prepare inputs**

Collect:

```text
Thread Active Operational Dataset from OTBR: hex:$ThreadDatasetHex
Matter setup payload from device QR: MT:$MatterQrPayload
openHAB base URL: http://$OpenHabHost:8080
OTBR base URL or diagnostic endpoint: http://$OtbrHost
```

Do not store these values in repository files.

- [ ] **Step 2: Save app configuration**

In the app:

```text
Paste Thread dataset
Paste OTBR base URL
Paste openHAB base URL
Tap Save dataset, OTBR URL, and openHAB URL
```

Expected log:

```text
Saved Thread dataset in encrypted app storage, saved OTBR base URL, saved openHAB base URL, and saved developer attestation bypass: on or off. Setup payloads and PINs are not saved.
```

- [ ] **Step 3: Check prerequisites**

In the app, tap:

```text
Check OTBR connectivity
Check openHAB readiness
Check commissioning permissions
```

Expected logs:

```text
OTBR connectivity: endpoint reachable.
openHAB Matter controller is ready
Runtime commissioning permissions already granted: the granted permission list
```

- [ ] **Step 4: Commission via BLE Thread**

Put the Matter-over-Thread device into pairing mode, then tap:

```text
Run Thread commissioning
```

Expected app behavior:

```text
BLE scan starts for the setup discriminator.
PASE pairing completes.
Attestation continues or developer bypass is explicitly used.
Thread dataset is provisioned without being printed.
Commissioning returns a bootstrap node id.
```

- [ ] **Step 5: Verify OTBR membership**

Use OTBR CLI or the available OTBR diagnostic interface to confirm the device joined the Thread network.

Expected evidence:

```text
The commissioned device appears as a child/router/end-device on the target Thread network.
```

- [ ] **Step 6: Record sanitized result**

Update `docs/completion-audit.md` with device model, Android version, connectedhomeip artifact source, OTBR model, and pass/fail result. Do not include setup payloads, PINs, datasets, or fabric material.

---

### Task 5: Validate Fabric Restore And OpenCommissioningWindow

**Files:**
- Runtime: Android app
- Verify: `docs/chip-jni-integration.md`

- [ ] **Step 1: Restart the app**

Fully close and reopen the app after successful Thread commissioning.

- [ ] **Step 2: Verify fabric restore**

Tap:

```text
Check connectedhomeip fabric restore
```

Expected log:

```text
connectedhomeip fabric restore ready for node $NodeId.
```

- [ ] **Step 3: Open commissioning window**

Tap:

```text
Open commissioning window
```

Expected app behavior:

```text
connectedhomeip acquires a connected-device pointer.
OpenCommissioningWindow command starts.
App displays a temporary manual pairing code or QR payload.
```

- [ ] **Step 4: Record result**

Update `docs/completion-audit.md` with pass/fail evidence for fabric restore and OpenCommissioningWindow. Do not copy the temporary code into repository files.

---

### Task 6: Validate openHAB Secondary Pairing

**Files:**
- Runtime: openHAB Main UI
- Runtime: Android app
- Runtime: openHAB Inbox REST/SSE

- [ ] **Step 1: Enter temporary code in openHAB**

In openHAB Main UI:

```text
Settings > Things > + > Matter > Scan Input
```

Enter the temporary setup code shown by the Android app.

- [ ] **Step 2: Watch Inbox from Android**

In the app, tap:

```text
Watch openHAB Inbox SSE
```

Expected app log:

```text
openHAB Inbox SSE: Matter Inbox entry detected.
```

- [ ] **Step 3: Confirm openHAB Thing**

In openHAB, add the Inbox entry as a Thing and verify the Thing remains online.

Expected evidence:

```text
Matter device appears in openHAB Inbox and can be added as an online Thing.
```

- [ ] **Step 4: Record final hardware validation**

Update `docs/completion-audit.md`:

```markdown
- End-to-end Matter-over-Thread validation passed on the actual validation date: Android BLE Thread commissioning, OCW, openHAB Scan Input pairing, and Inbox detection all succeeded.
```

Commit:

```powershell
git add docs/completion-audit.md docs/implementation-status.md README.md
git commit -m "docs: record end-to-end matter hardware validation"
```

---

### Task 7: Decide Bootstrap Fabric Persistence Policy

**Files:**
- Modify if policy changes: `docs/chip-jni-integration.md`
- Modify if policy changes: `docs/implementation-status.md`
- Modify if policy changes: `README.md`
- Modify if implementation changes: `app/src/main/java/org/openhab/matter/companion/controller/`

- [ ] **Step 1: Choose persistence policy from hardware evidence**

Pick one policy:

```text
Ephemeral bootstrap fabric: the phone only commissions and opens OCW during the setup session.
Persistent bootstrap fabric: the phone must restore its connectedhomeip fabric after app restart to reopen windows later.
```

- [ ] **Step 2: If ephemeral, document the limitation**

Update docs to state:

```text
The phone bootstrap fabric is used for setup only. Reopening commissioning windows after app reinstall or lost controller state requires re-commissioning the device or another controller.
```

- [ ] **Step 3: If persistent, verify encrypted state restore**

Run the hardware sequence twice with app restart between commissioning and OCW. Expected:

```text
connectedhomeip fabric restore ready for node $NodeId.
OpenCommissioningWindow succeeds after restart.
```

- [ ] **Step 4: Commit the policy**

Run:

```powershell
git add README.md docs/chip-jni-integration.md docs/implementation-status.md docs/completion-audit.md app/src/main/java/org/openhab/matter/companion/controller
git commit -m "docs: define bootstrap fabric persistence policy"
```

---

### Task 8: Add Troubleshooting UX Only After Design Approval

**Files:**
- Design: `docs/superpowers/specs/YYYY-MM-DD-openhab-thread-troubleshooting-design.md`
- Plan: `docs/superpowers/plans/YYYY-MM-DD-openhab-thread-troubleshooting.md`
- Likely modify: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Likely modify: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Likely test: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`

- [ ] **Step 1: Use brainstorming before implementation**

Follow `superpowers:brainstorming` because this is user-facing UX behavior. Present the design and get approval before editing app code.

- [ ] **Step 2: Minimum approved scope**

The troubleshooting guide should cover only:

```text
openHAB Matter binding controller Thing must be online.
openHAB host needs IPv6 reachability to the Thread network through OTBR.
mDNS/Avahi/service discovery must work between openHAB and the Matter device.
The user enters the Android-generated temporary code in openHAB Matter Scan Input.
If Inbox detection fails, use app readiness, Inbox, and SSE checks to narrow the issue.
```

- [ ] **Step 3: Verify no secrets are logged**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --tests org.openhab.matter.companion.MainActivityPresentationTest
```

Expected:

```text
BUILD SUCCESSFUL
```

---

### Task 9: Decide Whether To Display A Temporary QR Code

**Files:**
- Design if implemented: `docs/superpowers/specs/YYYY-MM-DD-temporary-code-qr-display-design.md`
- Plan if implemented: `docs/superpowers/plans/YYYY-MM-DD-temporary-code-qr-display.md`
- Likely modify if implemented: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Likely modify if implemented: `app/src/main/java/org/openhab/matter/companion/MainActivityPresentation.java`
- Likely test if implemented: `app/src/test/java/org/openhab/matter/companion/MainActivityPresentationTest.java`

- [ ] **Step 1: Decide scope from hardware flow**

After OpenCommissioningWindow works on hardware, decide whether manual code display is sufficient:

```text
Manual-only: keep the current text temporary code and openHAB Scan Input instructions.
QR display: render the returned OCW QR payload in-app so another controller can scan it.
```

- [ ] **Step 2: If QR display is required, use brainstorming before implementation**

Follow `superpowers:brainstorming` because this is new visible UX. The approved design must specify:

```text
Whether the QR is generated from connectedhomeip's returned qrCode or from the manual pairing code.
Whether the QR is shown on the same output panel or in a modal.
How the app prevents the temporary code or QR payload from being saved in logs or persisted state.
```

- [ ] **Step 3: Preserve the existing safe default if manual-only is accepted**

Update docs to state:

```text
The app displays the temporary manual code and openHAB Scan Input instructions. It does not render a QR code in the MVP.
```

Commit:

```powershell
git add README.md docs/implementation-status.md docs/completion-audit.md
git commit -m "docs: define temporary code display scope"
```

---

## Final Verification Gate

Before marking the app complete, run or collect evidence for all applicable commands:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline "-PopenhabMatterChipControllerArtifactsDir=$ArtifactDir"
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -ChipControllerArtifactsDir $ArtifactDir
git diff --check
```

Then verify all hardware evidence:

```text
Connected Android device install succeeds.
connectedhomeip controller selection succeeds.
BLE Thread commissioning succeeds against a real device.
OTBR confirms the device joined the Thread network.
Fabric restore policy is verified or documented.
OpenCommissioningWindow returns a temporary code.
openHAB Scan Input accepts the code.
openHAB Inbox detects the device.
The openHAB Matter Thing is online after adding it.
```

## Self-Review

- Spec coverage: The plan covers every remaining unproven item in `docs/completion-audit.md` and `docs/implementation-status.md`.
- Placeholder scan: No `TBD`, `TODO`, "implement later", or unspecified command remains. Hardware-dependent values are explicit operator inputs because they cannot be known from the repository.
- Type consistency: The plan uses existing script names, Gradle properties, and app button labels from the current codebase.
