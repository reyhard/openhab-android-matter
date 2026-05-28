# AGENTS.md and Project Skills Design

Date: 2026-05-28

## Goal

Create durable AI-agent guidance for this repository so future sessions do not rediscover the same Matter, connectedhomeip, Android, ADB, IPv6, and openHAB constraints. The guidance is for AI agents only, not general contributor onboarding.

The design uses a thin root `AGENTS.md` plus focused local skills. `AGENTS.md` should carry stable project rules and routing hints. Skills should carry detailed operational workflows that are invoked only when relevant.

## Context

This repository is an Android companion app for openHAB Matter pairing. Current project docs show that the app can build and install a debug APK, package connectedhomeip Android controller artifacts, commission a real Thread Matter device from Android, restore the commissioned node id, open an OpenCommissioningWindow, and support openHAB pairing after stale mDNS/Avahi records are cleared.

The important current docs are:

- `docs/implementation-status.md`
- `docs/open-commissioning-window-workflow.md`
- `docs/research.md`
- `README.md`

When these conflict, `docs/implementation-status.md` and workflow docs should be treated as more current than `README.md`.

## Chosen Approach

Use a thin `AGENTS.md` plus focused local skills.

Reasons:

- The root instruction file stays readable and high-signal.
- Detailed, task-specific debugging knowledge is loaded only when needed.
- Skills can evolve independently as the app's commissioning and handoff behavior changes.
- Future agents get a clear routing table instead of a long always-on troubleshooting dump.

Rejected alternatives:

- Large `AGENTS.md` only: too much detail would be always loaded and likely ignored or become stale.
- Docs only: useful for humans, but weaker at steering agent behavior than task-triggered skills.

## Root AGENTS.md Design

`AGENTS.md` should be short, directive, and repo-specific.

### Project Reality

The file should state:

- This repo is an Android companion app for openHAB Matter pairing.
- Real connectedhomeip commissioning has been validated on one Android device and one Thread device.
- The fake Matter controller is for deterministic tests and offline diagnostics only.
- Thread commissioning and OpenCommissioningWindow flows must fail closed when connectedhomeip is unavailable; agents must not silently route real commissioning through simulation.
- The current validated openHAB path uses OpenCommissioningWindow codes, but openHAB handoff behavior is an active design area and should not be treated as permanent architecture unless the task explicitly depends on it.

### Read First

The file should route agents to read:

- `docs/implementation-status.md` for current capability status.
- `docs/open-commissioning-window-workflow.md` for OCW control flow and source map.
- `docs/research.md` for architectural background.
- `README.md` for build/install basics, with the caveat that it may lag status docs.

### Default Commands

The file should include command patterns, not long explanations:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PopenhabMatterChipControllerArtifactsDir=<artifact-dir> -PopenhabMatterChipControllerAbis=arm64-v8a
D:\Tools\Android\SDK\platform-tools\adb.exe devices
D:\Tools\Android\SDK\platform-tools\adb.exe -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

The known phone serial from this development session is `62311e26`, but agents must verify attached devices before using it.

### Operational Rules

The file should include these rules:

- Do not revert user changes or unrelated dirty worktree files.
- Do not commit `.artifacts/` or local backup files unless explicitly requested.
- Treat connectedhomeip artifacts as local external dependencies.
- Do not expose REST tokens, Thread datasets, setup PINs, fabric material, or full QR/manual codes in permanent docs unless explicitly requested.
- Short-lived debug summaries may include node ids, IPv6 addresses, and codes when necessary to solve the active issue.
- Do not replace the connectedhomeip commissioning path with a Play Services-only implementation without explicit design approval.
- Preserve the native readiness gate: real commissioning should stop when connectedhomeip is not ready.

### Matter-Specific Rules

The file should include:

- Manual setup code returned by OpenCommissioningWindow should be used directly; do not derive it from QR unless the current task explicitly implements QR-to-manual conversion.
- mDNS/Avahi can expose stale `_matterc._udp` records, so agents must compare phone log IP/node information with openHAB/Raspberry Pi discovery output before diagnosing pairing failures.
- IPv6 reachability is required for openHAB Matter pairing. Do not assume IPv4 OTBR reachability is sufficient.

### Skill Routing Table

The file should map common tasks to local skills:

| Task | Skill |
| --- | --- |
| App crash, stalled commissioning, phone log verification | `openhab-android-matter-adb-debugging` |
| connectedhomeip build, artifact packaging, runtime readiness | `openhab-android-matter-connectedhomeip-artifacts` |
| Thread commissioning, BLE, attestation, bootstrap state, OCW internals | `openhab-android-matter-commissioning-flow` |
| IPv6, OTBR, OpenWRT, Avahi, stale `_matterc._udp` records | `openhab-android-matter-mdns-ipv6-troubleshooting` |
| Updating status/workflow docs after real-device findings | `openhab-android-matter-status-docs` |

## Skill Designs

### openhab-android-matter-adb-debugging

Use when the app crashes, commissioning stalls, the user asks for phone logs, or phone behavior must be verified against UI output.

The skill should include:

- ADB discovery and serial selection commands.
- Logcat commands filtered for this app and Matter stack tags such as `CHIP`, `CTL`, `DMG`, `BLE`, and `DIS`.
- How to identify current node id, IPv6 endpoint, connected-device pointer acquisition, OCW success, manual code, QR code, and CHIP status errors.
- How to distinguish UI delay from real failure by watching progression logs.
- A rule not to claim success until logs show the relevant callback or openHAB confirmation.
- Redaction guidance for tokens, datasets, setup codes, and fabric material.

### openhab-android-matter-connectedhomeip-artifacts

Use when building, updating, packaging, or validating connectedhomeip Android controller artifacts.

The skill should include:

- Expected artifact layout: controller jars plus ABI-specific `libCHIPController.so` and `libc++_shared.so`.
- Required Gradle properties, especially `-PopenhabMatterChipControllerArtifactsDir=...` and ABI selection.
- Reminder that placeholder jars/libs must not be accepted as valid artifacts.
- Runtime readiness expectations: class lookup, JNI load, AndroidChipPlatform initialization, BLE callback access, and controller construction.
- Guidance to fetch/update connectedhomeip before rebuilding when the local checkout is stale.
- Clear separation between the legacy `libopenhab_matter_chip.so` stub and the real connectedhomeip Java/JNI controller artifacts.

### openhab-android-matter-commissioning-flow

Use when modifying Thread commissioning, BLE scanning, attestation, bootstrap state, controller state, or OpenCommissioningWindow behavior.

The skill should include:

- Summary of the current flow: Android acts as a local Matter controller, commissions the Thread device, stores bootstrap node/controller state, then opens OCW when requested.
- Fake controller is test-only and must not be used for real Thread commissioning or OCW fallback.
- Source map pointing to `MainActivity`, `MatterController`, `ConnectedHomeIpMatterController`, `ConnectedHomeIpReflectionGateway`, `ConnectedHomeIpReflectionCommandFactory`, bootstrap state classes, and OCW callback/result classes.
- Known JNI issue: dynamic proxy OCW callback can fail CheckJNI return-type validation; prefer concrete callback class when packaged.
- Parameters that matter today: OCW timeout, discriminator, iteration count, callback wait timeout, and device-pointer wait timeout.
- Failure-mode checklist: missing artifacts, BLE scan mismatch, GATT retryable errors, attestation handling, ICD registration, operational discovery timeout, pointer acquisition timeout, OCW callback error or timeout.

### openhab-android-matter-mdns-ipv6-troubleshooting

Use when IP address, Avahi, OpenWRT, OTBR, Thread routing, or stale `_matterc._udp` discovery is involved.

The skill should include:

- Compare phone logs with `avahi-browse -rt _matterc._udp` output.
- Verify whether the IP in phone logs matches the IP openHAB/Raspberry Pi sees.
- Ping current and stale IPv6 addresses from relevant hosts.
- Restart Avahi on Raspberry Pi and OpenWRT when stale records persist.
- Check IPv6 route/RA behavior before blaming app logic.
- State that link-local next hops, Thread prefixes, and interface names are environment-specific and must be verified from current commands.
- Explain that IPv6 reachability and current mDNS are both required for openHAB pairing to work reliably.

### openhab-android-matter-status-docs

Use when updating docs after implementation changes, hardware validation, or debugging findings.

The skill should include:

- Update `docs/implementation-status.md` whenever real capability status changes.
- Update `docs/open-commissioning-window-workflow.md` when OCW flow, parameters, source map, or failure paths change.
- Check `README.md` for stale claims after major changes; either update it or ensure newer docs are clearly authoritative.
- Do not permanently document sensitive values such as full setup codes, Thread datasets, REST tokens, or fabric material.
- Prefer concise factual status over session narrative.

## Non-Goals

This design does not define a dedicated openHAB handoff skill. Handoff behavior may change, so durable guidance should not freeze the current manual-code flow as permanent architecture.

This design does not implement the files. It only specifies the intended structure and content for later implementation.

## Verification Strategy For Implementation

When implementing this design later:

- Confirm `AGENTS.md` exists at repo root and is concise.
- Confirm each skill has a `SKILL.md` with a clear trigger description and operational workflow.
- Confirm skill names in `AGENTS.md` match actual directory names.
- Confirm no sensitive session-specific setup codes, Thread datasets, REST tokens, or fabric material are written.
- Confirm `git diff --cached --name-only` includes only intended docs/skill files before committing.
