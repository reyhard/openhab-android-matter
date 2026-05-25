# Connectedhomeip Reflection Gateway Plan

Goal: wire the reflected connectedhomeip command factory into a concrete `ConnectedHomeIpControllerGateway` while keeping Android runtime dependencies injectable until BLE scan/connect and device pointer APIs are implemented.

## Scope

- Add a reflection-backed gateway that invokes `pairDeviceThroughBLE(...)` and `openPairingWindowWithPINCallback(...)` through `ConnectedHomeIpReflectionCommandFactory`.
- Introduce narrow seams for the still-platform-specific pieces:
  - controller object provider
  - BLE connection provider returning `BluetoothGatt` plus connectedhomeip connection id
  - Matter node-id allocator
  - commissioning completion monitor
  - connected-device pointer provider/releaser for OCW
- Keep `ConnectedHomeIpMatterController` and UI selection behavior unchanged for now.
- Do not claim full production commissioning until the providers are backed by real Android BLE, connectedhomeip callbacks, attestation handling, and pointer acquisition.

## Tasks

1. Add failing tests for `ConnectedHomeIpReflectionGateway`:
   - BLE Thread commissioning builds Thread `CommissionParameters`, acquires BLE by discriminator, allocates a node id, invokes `pairDeviceThroughBLE(...)`, waits for completion, closes BLE, and returns the completed node id/state.
   - BLE acquisition is closed when command invocation fails.
   - OCW acquires a device pointer, invokes `openPairingWindowWithPINCallback(...)`, awaits the callback result, and releases the pointer.
   - OCW releases the pointer when invocation or callback await fails.
2. Implement small interfaces/value objects for the injected runtime seams.
3. Implement `ConnectedHomeIpReflectionGateway` using the existing reflection factory and callback bridge.
4. Update docs to show that command orchestration now exists but Android provider implementations remain.
5. Verify with targeted unit tests, then full `:app:testDebugUnitTest :app:assembleDebug --offline`.
6. Request sidecar review and fix findings before committing.
