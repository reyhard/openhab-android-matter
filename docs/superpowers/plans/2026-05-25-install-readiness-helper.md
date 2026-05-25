# Install Readiness Helper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a repeatable Windows PowerShell helper that builds the debug APK, verifies the artifact, checks ADB devices, and installs only when a usable device/emulator is attached.

**Architecture:** Keep the helper outside the Android app in `scripts/` so it can run from a developer shell without changing runtime behavior. Add a no-dependency PowerShell self-test that creates fake `adb` command shims and uses `-SkipBuild` to verify no-device, single-device install, and multi-device-without-serial behavior.

**Tech Stack:** PowerShell 7-compatible script, Android Gradle wrapper, ADB command-line interface, no Pester dependency.

---

### Task 1: Script Self-Test

**Files:**
- Create: `scripts/test_install_debug.ps1`
- Create: `scripts/install_debug.ps1`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Write failing self-test**

Create `scripts/test_install_debug.ps1` with helper functions that create a temporary fake project root, fake APK file, and fake `adb.ps1`. The tests must run:

```powershell
& $script -ProjectRoot $projectRoot -SkipBuild -AdbPath $adbNoDevices -ApkPath $apk
# Expected exit code 2, output contains "No connected Android devices"

& $script -ProjectRoot $projectRoot -SkipBuild -AdbPath $adbOneDevice -ApkPath $apk
# Expected exit code 0, fake adb log contains "install -r"

& $script -ProjectRoot $projectRoot -SkipBuild -AdbPath $adbTwoDevices -ApkPath $apk
# Expected exit code 4, output contains "Multiple Android devices"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1`

Expected: FAIL because `scripts/install_debug.ps1` does not exist.

- [ ] **Step 3: Implement helper script**

Create `scripts/install_debug.ps1` with parameters:

```powershell
param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string] $GradlePath = "",
    [string] $AdbPath = "adb",
    [string] $ApkPath = "",
    [string] $Serial = "",
    [switch] $SkipBuild
)
```

Behavior:
- Build unless `-SkipBuild` is set: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`.
- Require `app\build\outputs\apk\debug\app-debug.apk` to exist and have non-zero length.
- Run `adb devices`, parse only rows whose second column is `device`.
- Exit `2` when no usable devices are attached.
- Exit `4` when multiple usable devices are attached and no `-Serial` is supplied.
- Run `adb install -r <apk>` for one device, or `adb -s <serial> install -r <apk>` when `-Serial` is supplied.
- Exit non-zero if Gradle, ADB listing, or install fails.

- [ ] **Step 4: Run test to verify it passes**

Run: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1`

Expected: PASS.

- [ ] **Step 5: Update docs and verify**

Document the helper in `README.md` and add an implemented status bullet. Then run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -SkipBuild
```

Expected: Gradle PASS, self-test PASS, helper exits `2` in the current environment if no device/emulator is attached.
