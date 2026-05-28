# Implementation Plan: AGENTS.md and Project Skills

Date: 2026-05-28
Spec: `docs/superpowers/specs/2026-05-28-agents-and-skills-design.md`

## Goal

Implement durable AI-agent guidance for this repository by adding a concise root `AGENTS.md` and project-specific skill source files that capture Android Matter commissioning, connectedhomeip artifact, ADB, IPv6/mDNS, and status-doc workflows.

## Scope

Create these repo files:

- `AGENTS.md`
- `.codex/skills/openhab-android-matter-adb-debugging/SKILL.md`
- `.codex/skills/openhab-android-matter-connectedhomeip-artifacts/SKILL.md`
- `.codex/skills/openhab-android-matter-commissioning-flow/SKILL.md`
- `.codex/skills/openhab-android-matter-mdns-ipv6-troubleshooting/SKILL.md`
- `.codex/skills/openhab-android-matter-status-docs/SKILL.md`

The `.codex/skills` files are repo-owned source copies. If active Codex discovery requires user-local installation under `C:\Users\reyhard\.codex\skills`, do that as a separate explicit step after the repo files are reviewed, because it writes outside the repository.

## Non-Goals

- Do not implement app behavior changes.
- Do not modify connectedhomeip artifacts.
- Do not create a dedicated openHAB handoff skill; that behavior is intentionally not frozen.
- Do not commit `.artifacts/`, local backup files, or unrelated dirty source changes.
- Do not include full setup codes, Thread datasets, REST tokens, or fabric material.

## Step 1: Add Root AGENTS.md

Create `AGENTS.md` with these sections:

1. `Project Reality`
   - State that this is an Android companion app for openHAB Matter pairing.
   - State that real connectedhomeip commissioning is validated on one Android phone and one Thread device.
   - State that fake controller is test/offline only.
   - Require real Thread commissioning and OCW to fail closed when connectedhomeip is unavailable.
   - Note that current OCW-code openHAB pairing is validated but handoff behavior is an active design area.

2. `Read First`
   - Link to `docs/implementation-status.md`.
   - Link to `docs/open-commissioning-window-workflow.md`.
   - Link to `docs/research.md`.
   - Link to `README.md`, with caveat that status/workflow docs are more current on conflicts.

3. `Build, Test, Install`
   - Include PowerShell commands for `ANDROID_HOME`, unit tests, assemble, artifact-aware assemble, `adb devices`, and APK install.
   - Mention known serial `62311e26` only as historical context; require verification with `adb devices`.

4. `Operational Rules`
   - Dirty-worktree safety.
   - Do not commit `.artifacts/` or local backups unless requested.
   - Treat connectedhomeip artifacts as external local dependencies.
   - Redaction rules for sensitive values.
   - Do not replace connectedhomeip with Play Services-only behavior without explicit design approval.
   - Preserve native readiness gate.

5. `Matter Rules`
   - Use manual OCW code directly.
   - Do not derive manual code from QR unless explicitly implementing that feature.
   - Compare phone logs with mDNS/Avahi before diagnosing openHAB pairing failures.
   - IPv6 reachability is required; IPv4 OTBR checks are not enough.

6. `Project Skills`
   - Add routing table from task to project-local skill file.

## Step 2: Add ADB Debugging Skill

Create `.codex/skills/openhab-android-matter-adb-debugging/SKILL.md`.

Required front matter:

```yaml
---
name: openhab-android-matter-adb-debugging
description: Use when debugging Android phone behavior, app crashes, commissioning stalls, logcat output, or verifying node/IP/OCW state from ADB logs in openhab-android-matter.
---
```

Required content:

- Start by checking attached devices with `adb devices`.
- Prefer explicit `-s <serial>` for all ADB commands.
- Include app package `org.openhab.matter.companion`.
- Include useful logcat filters for app plus Matter tags.
- Explain how to extract node id, IPv6 endpoint, OCW success, manual code, QR code, and CHIP status errors.
- Include redaction rule for sensitive values.
- Require evidence before saying commissioning or OCW succeeded.

## Step 3: Add connectedhomeip Artifacts Skill

Create `.codex/skills/openhab-android-matter-connectedhomeip-artifacts/SKILL.md`.

Required front matter:

```yaml
---
name: openhab-android-matter-connectedhomeip-artifacts
description: Use when building, updating, packaging, validating, or diagnosing connectedhomeip Android controller artifacts for openhab-android-matter.
---
```

Required content:

- Expected artifact layout: jars and ABI-specific native libs.
- Gradle properties for artifact directory and ABI selection.
- Placeholder files are invalid.
- Runtime readiness expectations.
- Distinguish legacy JNI stub from real connectedhomeip Java/JNI controller artifacts.
- If connectedhomeip checkout is stale, fetch/update before building artifacts.

## Step 4: Add Commissioning Flow Skill

Create `.codex/skills/openhab-android-matter-commissioning-flow/SKILL.md`.

Required front matter:

```yaml
---
name: openhab-android-matter-commissioning-flow
description: Use when modifying Thread commissioning, BLE scanning, attestation, bootstrap state, connectedhomeip controller state, or OpenCommissioningWindow behavior in openhab-android-matter.
---
```

Required content:

- Summarize current Android-as-local-Matter-controller flow.
- State fake controller is test-only and must not be fallback for real flows.
- Link to `docs/open-commissioning-window-workflow.md`.
- Include source map for main classes.
- Mention concrete OCW callback class requirement due JNI CheckJNI proxy issue.
- Include current OCW/device-pointer timeout parameters.
- Include failure-mode checklist.

## Step 5: Add mDNS/IPv6 Troubleshooting Skill

Create `.codex/skills/openhab-android-matter-mdns-ipv6-troubleshooting/SKILL.md`.

Required front matter:

```yaml
---
name: openhab-android-matter-mdns-ipv6-troubleshooting
description: Use when troubleshooting Thread IPv6 routing, OTBR reachability, OpenWRT/Raspberry Pi Avahi, stale _matterc._udp records, or mismatched Matter device IPs.
---
```

Required content:

- Compare phone log IP/node with `avahi-browse -rt _matterc._udp`.
- Ping current and stale IPv6 addresses from relevant hosts.
- Restart Avahi on Raspberry Pi/OpenWRT when stale records persist.
- Check IPv6 RA/routes before blaming app logic.
- Warn that interface names, prefixes, and link-local next hops are environment-specific.

## Step 6: Add Status Docs Skill

Create `.codex/skills/openhab-android-matter-status-docs/SKILL.md`.

Required front matter:

```yaml
---
name: openhab-android-matter-status-docs
description: Use when updating openhab-android-matter project status, workflow, or research docs after implementation changes, hardware validation, or debugging findings.
---
```

Required content:

- Update `docs/implementation-status.md` when real capability changes.
- Update `docs/open-commissioning-window-workflow.md` when flow/classes/parameters/failure paths change.
- Check `README.md` for stale claims after major changes.
- Keep docs factual and concise.
- Do not permanently document sensitive values.

## Step 7: Verify Content

Run these checks:

```powershell
rg -n "TBD|TODO|FIXME|<serial>|<artifact-dir>" AGENTS.md .codex\skills
rg -n "349|MT:|dataset active|password|token|fd88:" AGENTS.md .codex\skills
Get-ChildItem .codex\skills -Recurse -Filter SKILL.md
```

Expected result:

- No placeholders except intentional command placeholders if they are explained.
- No full setup codes, Thread datasets, REST tokens, or fabric material.
- Exactly five `SKILL.md` files under `.codex/skills`.

## Step 8: Git Hygiene

Before committing:

```powershell
git status --short
git diff -- AGENTS.md .codex\skills
git add AGENTS.md .codex\skills
git diff --cached --name-only
```

Only these paths should be staged:

- `AGENTS.md`
- `.codex/skills/openhab-android-matter-adb-debugging/SKILL.md`
- `.codex/skills/openhab-android-matter-connectedhomeip-artifacts/SKILL.md`
- `.codex/skills/openhab-android-matter-commissioning-flow/SKILL.md`
- `.codex/skills/openhab-android-matter-mdns-ipv6-troubleshooting/SKILL.md`
- `.codex/skills/openhab-android-matter-status-docs/SKILL.md`

Commit message:

```text
docs: add agent guidance and project skills
```

## Step 9: Optional User-Local Skill Installation

After the repo commit, ask whether to install/copy the repo skill files into `C:\Users\reyhard\.codex\skills` so future Codex sessions discover them automatically.

Do not perform this step without explicit approval because it writes outside the repository.
