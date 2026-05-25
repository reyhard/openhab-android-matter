# Connectedhomeip Controller Callback Reflection Plan

Goal: add reflection-backed building blocks for connectedhomeip controller callbacks so the gateway can later use real commissioning completion, attestation continuation, and connected-device pointer acquisition without compile-time CHIP dependencies.

## Scope

- Extend `ConnectedHomeIpReflectionCommandFactory` to locate and invoke:
  - `ChipDeviceController.setCompletionListener(CompletionListener)`
  - `ChipDeviceController.setDeviceAttestationDelegate(int, DeviceAttestationDelegate)`
  - `ChipDeviceController.continueCommissioning(long, boolean)`
  - `ChipDeviceController.getConnectedDevicePointer(long, GetConnectedDeviceCallback)`
  - `ChipDeviceController.releaseConnectedDevicePointer(long)`
- Add dynamic proxy bridges for:
  - commissioning completion listener -> `MatterCommissioningResult` / exception / timeout
  - attestation delegate -> calls `continueCommissioning(devicePtr, bypassFlag)`
  - connected device callback -> `ConnectedHomeIpDevicePointer` with release action
- Keep constructors compatible for existing tests.
- Update docs with exact remaining Android runtime work.

## Verification

- Add TDD tests for each proxy and reflected method.
- Run targeted controller callback tests first.
- Run full `:app:testDebugUnitTest :app:assembleDebug --offline` before commit.
