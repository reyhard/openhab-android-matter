# openHAB Matter Controller Readiness Plan

## Goal

Warn before commissioning when the configured openHAB instance is reachable but does not expose an online Matter controller thing.

## Current Behavior

- The Android app checks only `<base>/rest/`.
- Any 2xx/3xx REST response is reported as online readiness.
- This can hide a missing or offline Matter controller until later commissioning steps fail.

## Desired Behavior

- Validate the openHAB base URL as before.
- Check `<base>/rest/` first.
- If REST is not reachable, keep the existing REST failure behavior.
- If REST is reachable, fetch `<base>/rest/things`.
- Report ready only when a Matter controller thing is present and `ONLINE`.
- Report not ready when the Matter controller is missing, offline, or cannot be verified.
- Preserve enough state in `OpenHabStatus` for UI/tests to distinguish REST reachability from Matter controller readiness.

## TDD Steps

1. Add `HttpOpenHabClientTest` with a route-based local HTTP server.
2. RED: assert online readiness for REST plus an online `matter:controller` thing.
3. RED: assert not-ready status when `/rest/things` has no Matter controller.
4. RED: assert not-ready status when the Matter controller is present but `OFFLINE`.
5. RED: preserve REST HTTP error behavior.
6. Implement the smallest status model and HTTP client changes to pass.
7. Update fake client tests only as needed for the expanded status contract.
8. Update README and implementation status docs.

## Verification

- `.\gradlew.bat :app:testDebugUnitTest --offline`
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`
- `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\install_debug.ps1 -PreflightOnly`
- `git diff --check`
- Subagent spec and code-quality reviews before commit.
