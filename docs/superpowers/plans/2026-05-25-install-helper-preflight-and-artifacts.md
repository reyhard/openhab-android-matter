# Install Helper Preflight And Artifact Flags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the Windows install helper so it can verify package readiness without a device and can build the exact APK variant that includes supplied connectedhomeip or native prebuilt artifacts.

**Architecture:** Keep `scripts/install_debug.ps1` as a thin wrapper around the existing Gradle validation/build tasks and ADB install flow. Add explicit Gradle property forwarding and a `-PreflightOnly` stop point after APK verification so artifact/prebuilt builds can be checked before any device is attached.

**Tech Stack:** PowerShell 7, Android Gradle wrapper, ADB.

---

### Task 1: Helper Preflight And Artifact Arguments

**Files:**
- Modify: `scripts/install_debug.ps1`
- Modify: `scripts/test_install_debug.ps1`
- Modify: `README.md`
- Modify: `docs/implementation-status.md`

- [x] **Step 1: Write failing helper self-tests**

Extend `scripts/test_install_debug.ps1` with a fake Gradle script that records arguments and creates the fake APK. Add tests:

```powershell
$result = Invoke-InstallHelperWithGradle -ProjectRoot $project.Root -GradlePath $gradle.Path -AdbPath $adb.Path -PreflightOnly
Assert-Equal 0 $result.ExitCode "Preflight helper exit code mismatch."
Assert-Contains "Preflight complete" $result.Output "Preflight helper output mismatch."
Assert-Equal $false (Test-Path -LiteralPath $adb.Log) "Preflight should not call adb."
Assert-Contains ":app:testDebugUnitTest :app:assembleDebug --offline" (Get-Content -LiteralPath $gradle.Log -Raw) "Preflight did not run Gradle build."

$result = Invoke-InstallHelperWithGradle -ProjectRoot $project.Root -GradlePath $gradle.Path -AdbPath $adb.Path -PreflightOnly -ChipControllerArtifactsDir "C:\tmp\chip-artifacts" -ChipControllerAbis "arm64-v8a"
Assert-Contains "-PopenhabMatterChipControllerArtifactsDir=C:\tmp\chip-artifacts" (Get-Content -LiteralPath $gradle.Log -Raw) "Missing controller artifacts Gradle arg."
Assert-Contains "-PopenhabMatterChipControllerAbis=arm64-v8a" (Get-Content -LiteralPath $gradle.Log -Raw) "Missing controller ABI Gradle arg."

$result = Invoke-InstallHelperWithGradle -ProjectRoot $project.Root -GradlePath $gradle.Path -AdbPath $adb.Path -PreflightOnly -ChipNativeMode "prebuilt" -ChipPrebuiltDir "C:\tmp\native"
Assert-Contains "-PopenhabMatterChipNativeMode=prebuilt" (Get-Content -LiteralPath $gradle.Log -Raw) "Missing native mode Gradle arg."
Assert-Contains "-PopenhabMatterChipPrebuiltDir=C:\tmp\native" (Get-Content -LiteralPath $gradle.Log -Raw) "Missing native prebuilt Gradle arg."
```

- [x] **Step 2: Verify RED**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1
```

Expected: FAIL because `-PreflightOnly` and chip artifact/native parameters are not supported.

- [x] **Step 3: Implement helper flags**

Add parameters:

```powershell
[string] $ChipControllerArtifactsDir = "",
[string] $ChipControllerAbis = "",
[string] $ChipNativeMode = "",
[string] $ChipPrebuiltDir = "",
[switch] $PreflightOnly
```

Build args start as:

```powershell
$gradleArgs = @(":app:testDebugUnitTest", ":app:assembleDebug", "--offline")
```

Append non-blank properties:

```powershell
-PopenhabMatterChipControllerArtifactsDir=<dir>
-PopenhabMatterChipControllerAbis=<abis>
-PopenhabMatterChipNativeMode=<mode>
-PopenhabMatterChipPrebuiltDir=<dir>
```

After APK existence/length verification, if `-PreflightOnly` is set, print:

```text
Preflight complete. Debug APK is ready; install step was skipped.
```

and exit `0` before any ADB command.

- [x] **Step 4: Verify helper tests**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1
```

Expected: PASS.

- [x] **Step 5: Update docs and full verification**

Document:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly -ChipControllerArtifactsDir <artifact-dir>
```

Then run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_install_debug.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
```

Expected: helper self-test PASS, preflight PASS, Gradle PASS.

- [x] **Step 6: Commit**

Run:

```powershell
git add scripts/install_debug.ps1 scripts/test_install_debug.ps1 README.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-install-helper-preflight-and-artifacts.md
git commit -m "tools: add install helper preflight mode"
```

---

## Self-Review

- Spec coverage: This improves the install-ready path by allowing artifact-specific APK builds to be verified before hardware is attached.
- Placeholder scan: No TODO/TBD/fill-in steps remain.
- Type consistency: Uses existing Gradle property names and existing install helper script naming.
