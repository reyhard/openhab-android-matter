# Optional Hardware Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the debug APK installable on devices and emulators that do not expose camera or location hardware features.

**Architecture:** Add explicit optional `uses-feature` declarations for hardware implied by runtime permissions. Add a small offline PowerShell badging smoke test that uses Android SDK `aapt dump badging` to reject implied required camera/location features in the built APK.

**Tech Stack:** Android manifest, PowerShell 7, Android SDK `aapt`.

---

### Task 1: Mark Camera And Location Hardware Optional

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `scripts/test_apk_badging.ps1`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [x] **Step 1: Add failing APK badging smoke test**

Create `scripts/test_apk_badging.ps1` with parameters:

```powershell
param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $AaptPath = "",
    [string] $ApkPath = ""
)
```

The script must:

- Resolve the project root.
- Default the APK path to `app\build\outputs\apk\debug\app-debug.apk`.
- Resolve `aapt.exe` from `$AaptPath`, `local.properties` `sdk.dir`, `$env:ANDROID_HOME`, or `$env:ANDROID_SDK_ROOT`.
- Run `aapt dump badging <apk>`.
- Fail if output contains:

```text
uses-implied-feature: name='android.hardware.camera'
uses-implied-feature: name='android.hardware.location'
```

- Pass only when output contains optional feature lines for:

```text
uses-feature-not-required: name='android.hardware.camera'
uses-feature-not-required: name='android.hardware.location'
```

- [x] **Step 2: Verify RED**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_apk_badging.ps1
```

Expected: FAIL against the current APK because `CAMERA` and `ACCESS_FINE_LOCATION` imply required hardware features.

- [x] **Step 3: Update manifest**

Add these declarations directly after permissions in `app/src/main/AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.location" android:required="false" />
```

- [x] **Step 4: Rebuild and verify GREEN**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --offline
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_apk_badging.ps1
```

Expected: Gradle exits `0`, and the badging smoke test prints `APK badging smoke-test passed.`

- [x] **Step 5: Update docs**

Update docs to mention that the APK marks camera/location hardware optional, so camera scan and BLE/location-adjacent flows can fall back to manual input or runtime checks instead of blocking installation on headless emulators.

- [x] **Step 6: Full verification**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_apk_badging.ps1
git diff --check
```

Expected: all commands exit `0`; the badging test runs after the final APK assembly; `git diff --check` may print existing CRLF warnings only.

- [x] **Step 7: Commit**

Run:

```powershell
git add app/src/main/AndroidManifest.xml scripts/test_apk_badging.ps1 README.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-optional-hardware-features.md
git commit -m "fix: mark hardware features optional"
```

---

## Self-Review

- Spec coverage: This improves installability of the requested APK without requiring physical hardware.
- Placeholder scan: No TODO/TBD/fill-in steps remain.
- Type consistency: Feature names and badging strings match Android `aapt dump badging` output.
