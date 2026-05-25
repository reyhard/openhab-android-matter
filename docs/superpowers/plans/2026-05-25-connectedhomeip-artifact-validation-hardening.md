# connectedhomeip Artifact Validation Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the connectedhomeip artifact packaging gate reject empty placeholder files and provide a repeatable smoke test for the required artifact layout.

**Architecture:** Keep artifact validation in Gradle because `preBuild` already depends on `verifyConnectedHomeIpControllerArtifacts`. Add a PowerShell smoke helper under `scripts/` that creates synthetic positive/negative layouts and runs the Gradle verifier, matching the existing Windows helper style.

**Tech Stack:** Android Gradle Plugin/Groovy, PowerShell 7, Windows-friendly paths, offline Gradle verification.

---

### Task 1: Non-Empty Artifact Validation

**Files:**
- Modify: `app/build.gradle`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

- [ ] **Step 1: Verify RED with an empty synthetic artifact**

Create a temporary artifact directory containing all required jar and native library file names, but leave at least `CHIPController.jar` empty. Run:

```powershell
.\gradlew.bat :app:verifyConnectedHomeIpControllerArtifacts --offline "-PopenhabMatterChipControllerArtifactsDir=<empty-artifact-dir>"
```

Expected before implementation: PASS, proving the current gate is too weak.

- [ ] **Step 2: Implement non-empty checks**

In `app/build.gradle`, when validating each required jar/native library, require both `isFile()` and `length() > 0`. Error text should include:

```text
Empty connectedhomeip controller jar:
Empty connectedhomeip controller native library:
```

- [ ] **Step 3: Verify GREEN negative case**

Run the same Gradle command with the empty artifact file.

Expected after implementation: FAIL with `Empty connectedhomeip controller jar:`.

### Task 2: Artifact Validation Smoke Helper

**Files:**
- Create: `scripts/test_connectedhomeip_artifacts.ps1`
- Modify: `README.md`

- [ ] **Step 1: Add smoke helper**

Create `scripts/test_connectedhomeip_artifacts.ps1` that:
- Resolves repo root and `gradlew.bat`.
- Creates `build/tmp/chip-controller-artifacts-smoke`.
- Creates all required jars/native library files with non-empty placeholder content.
- Creates `build/tmp/chip-controller-artifacts-empty` with one empty required jar.
- Runs `:app:verifyConnectedHomeIpControllerArtifacts --offline` against the positive layout and expects exit `0`.
- Runs the same task against the empty layout and expects non-zero output containing `Empty connectedhomeip controller jar`.
- Prints `connectedhomeip artifact validation smoke-test passed.` on success.

- [ ] **Step 2: Verify helper**

Run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
```

Expected: PASS with `connectedhomeip artifact validation smoke-test passed.`

- [ ] **Step 3: Update README**

Document the smoke helper near the connectedhomeip artifact packaging section.

- [ ] **Step 4: Full verification and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\test_connectedhomeip_artifacts.ps1
git diff --check
git add app/build.gradle scripts/test_connectedhomeip_artifacts.ps1 README.md docs/chip-jni-integration.md docs/implementation-status.md docs/superpowers/plans/2026-05-25-connectedhomeip-artifact-validation-hardening.md
git commit -m "build: reject empty connectedhomeip artifacts"
```

Expected: Gradle PASS, smoke helper PASS, whitespace check clean.

---

## Self-Review

- Spec coverage: This improves the official connectedhomeip artifact gate needed before real Android BLE Thread commissioning can be installed and tested.
- Placeholder scan: No TBD/TODO/fill-in steps remain.
- Type consistency: Uses existing Gradle property `openhabMatterChipControllerArtifactsDir` and task `verifyConnectedHomeIpControllerArtifacts`.
