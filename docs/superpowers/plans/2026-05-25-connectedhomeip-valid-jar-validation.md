# Connectedhomeip Valid Jar Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reject corrupt, non-ZIP, or class-incomplete connectedhomeip controller JAR artifacts before producing an artifact-specific debug APK.

**Architecture:** Keep the validation in the existing Gradle `verifyConnectedHomeIpControllerArtifacts` task so every artifact-specific build and install-helper preflight uses the same gate. Extend the PowerShell smoke helper to create valid synthetic JARs for the positive case, a corrupt non-empty JAR for one negative case, and valid JARs missing required runtime class entries for another negative case.

**Tech Stack:** Gradle Groovy DSL, Java `ZipFile`, PowerShell 7 smoke tests.

---

### Task 1: Validate Connectedhomeip Controller JAR Format And Required Classes

**Files:**
- Modify: `app/build.gradle`
- Modify: `scripts/test_connectedhomeip_artifacts.ps1`
- Modify: `docs/implementation-status.md`
- Modify: `README.md`

- [x] **Step 1: Write failing smoke coverage for corrupt non-empty JARs**

Update `scripts/test_connectedhomeip_artifacts.ps1` so the positive artifact layout writes valid ZIP/JAR files with the class entries probed by `ConnectedHomeIpControllerArtifacts`, a new negative layout writes a non-empty text file as `CHIPController.jar`, and a second negative layout writes valid JARs without the required runtime class entries.

Add assertions:

```powershell
$invalidRoot = Join-Path $tmpRoot "chip-controller-artifacts-invalid-jar"
New-ArtifactLayout -Root $invalidRoot -JarMode "InvalidControllerJar"
$invalid = Invoke-GradleArtifactVerification -ArtifactRoot $invalidRoot
Assert-True ($invalid.ExitCode -ne 0) "Expected invalid synthetic artifacts to fail validation."
Assert-True ($invalid.Output.Contains("Invalid connectedhomeip controller jar")) ("Expected invalid-jar failure message. Output:`n" + $invalid.Output)

$missingClassRoot = Join-Path $tmpRoot "chip-controller-artifacts-missing-class"
New-ArtifactLayout -Root $missingClassRoot -JarMode "MissingRequiredClass"
$missingClass = Invoke-GradleArtifactVerification -ArtifactRoot $missingClassRoot
Assert-True ($missingClass.ExitCode -ne 0) "Expected artifacts missing required controller classes to fail validation."
Assert-True ($missingClass.Output.Contains("Missing connectedhomeip controller class entry")) ("Expected missing-class failure message. Output:`n" + $missingClass.Output)
```

- [x] **Step 2: Verify RED**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
```

Expected: FAIL because the current Gradle task only rejects missing/empty JAR files, not corrupt non-empty JARs or JARs missing required runtime class entries.

- [x] **Step 3: Implement Gradle JAR and class-entry validation**

In `app/build.gradle`, import `java.util.zip.ZipFile`, open each required controller JAR during `verifyConnectedHomeIpControllerArtifacts`, and collect entries across all required JARs.

Expected implementation shape:

```groovy
import java.util.zip.ZipFile

private static void verifyJarFile(File jar) {
    try {
        new ZipFile(jar).withCloseable {
        }
    } catch (IOException e) {
        throw new GradleException("Invalid connectedhomeip controller jar: " + jar, e)
    }
}
```

Call `verifyJarFile(jar)` after the existing non-empty check, then reject missing class entries matching the runtime readiness contract.

- [x] **Step 4: Verify smoke helper**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
```

Expected: PASS with `connectedhomeip artifact validation smoke-test passed.`

- [x] **Step 5: Update docs**

Update docs to say connectedhomeip controller artifacts require valid non-empty JAR files containing required runtime class entries plus ABI-specific native libraries, not just non-empty JAR paths.

- [x] **Step 6: Full verification**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
git diff --check
```

Expected: all commands exit `0`; `git diff --check` may print existing CRLF warnings only.

- [x] **Step 7: Commit**

Run:

```powershell
git add app/build.gradle scripts/test_connectedhomeip_artifacts.ps1 README.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-connectedhomeip-valid-jar-validation.md
git commit -m "build: reject invalid connectedhomeip jars"
```

---

## Self-Review

- Spec coverage: This closes an offline artifact-packaging gap before real connectedhomeip artifacts are used for installable APK builds.
- Placeholder scan: No TODO/TBD/fill-in steps remain.
- Type consistency: The plan uses the existing Gradle property and smoke-helper naming.
