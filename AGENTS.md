# Agent Guidance

This file is for AI agents working in this repository. Keep it short, operational, and consistent with the current project docs.

## Project Reality

- This repo is an Android companion app for openHAB Matter pairing.
- Real connectedhomeip commissioning has been validated on one Android phone and one Thread Matter device.
- `FakeMatterController` is for deterministic tests and offline diagnostics only.
- Thread commissioning and OpenCommissioningWindow must fail closed when connectedhomeip artifacts/runtime are unavailable. Do not silently route real commissioning through simulation.
- The current validated openHAB path uses OpenCommissioningWindow codes, but openHAB handoff behavior is still an active design area. Do not freeze that behavior as permanent architecture unless the current task explicitly requires it.

## Read First

- `docs/implementation-status.md` is the current capability/status reference.
- `docs/open-commissioning-window-workflow.md` describes OCW control flow, parameters, source map, and failure paths.
- `docs/research.md` gives architectural background.
- `README.md` has build/install basics, but may lag the status and workflow docs.

When docs conflict, prefer `docs/implementation-status.md` and workflow docs over `README.md`.

## Build, Test, Install

Use PowerShell from the repo root:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PopenhabMatterChipControllerArtifactsDir=<artifact-dir> -PopenhabMatterChipControllerAbis=arm64-v8a
D:\Tools\Android\SDK\platform-tools\adb.exe devices
D:\Tools\Android\SDK\platform-tools\adb.exe -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

Device serial `62311e26` was used during development, but always verify current attached devices with `adb devices` before using a serial.

## Operational Rules

- Do not revert user changes or unrelated dirty worktree files.
- Do not commit `.artifacts/` or local backup files unless explicitly requested.
- Treat connectedhomeip artifacts as local external dependencies.
- Do not expose REST tokens, Thread datasets, setup PINs, fabric material, or full QR/manual codes in permanent docs unless explicitly requested.
- Short-lived debug summaries may include node ids, IPv6 addresses, and setup codes when necessary for the active issue.
- Do not replace the connectedhomeip commissioning path with Play Services-only behavior without explicit design approval.
- Preserve the native readiness gate: real commissioning should stop when connectedhomeip is not ready.

## Commit and Release Notes

- Use concise, changelog-friendly commit subjects: `type(scope): summary`.
- Prefer these types: `feat`, `fix`, `matter`, `chip`, `ci`, `build`, `release`, `docs`, `test`, `chore`.
- Use scopes when they clarify the release note, for example `matter`, `chip`, `commissioning`, `ocw`, `thread`, `ble`, `ui`, `release`.
- Mark incompatible behavior with `!` after the type or scope, and include `BREAKING CHANGE:` in the commit body when more detail is needed.
- Add `[skip changelog]` or `changelog: skip` for commits that should not appear in release notes.
- Keep the first line user-facing where possible; generated releases group commits from `scripts/generate_changelog.mjs`.
- Release tags should normally be `v<versionName>` from `app/build.gradle`, for example `v0.1.0`.

## Matter Rules

- Use the manual setup code returned by OpenCommissioningWindow directly.
- Do not derive a manual setup code from QR unless the current task explicitly implements that feature.
- Before diagnosing openHAB pairing failure, compare phone log node/IP data with mDNS/Avahi output from the openHAB/Raspberry Pi side.
- IPv6 reachability is required for openHAB Matter pairing. IPv4 OTBR reachability is not sufficient.

## Project Skills

Repo-owned skill source files live under `.codex/skills/`.

| Task | Skill |
| --- | --- |
| App crash, stalled commissioning, phone log verification | `.codex/skills/openhab-android-matter-adb-debugging/SKILL.md` |
| connectedhomeip build, artifact packaging, runtime readiness | `.codex/skills/openhab-android-matter-connectedhomeip-artifacts/SKILL.md` |
| Thread commissioning, BLE, attestation, bootstrap state, OCW internals | `.codex/skills/openhab-android-matter-commissioning-flow/SKILL.md` |
| IPv6, OTBR, OpenWRT, Avahi, stale `_matterc._udp` records | `.codex/skills/openhab-android-matter-mdns-ipv6-troubleshooting/SKILL.md` |
| Updating status/workflow docs after real-device findings | `.codex/skills/openhab-android-matter-status-docs/SKILL.md` |
