---
name: openhab-android-matter-commissioning-flow
description: Use when modifying Thread commissioning, BLE scanning, attestation, bootstrap state, connectedhomeip controller state, or OpenCommissioningWindow behavior in openhab-android-matter.
---

# Commissioning Flow

Use this skill when editing the real Matter flow: BLE Thread commissioning, attestation, bootstrap state, connectedhomeip controller state, device-pointer acquisition, or OpenCommissioningWindow.

## Current Flow

- Android acts as a local Matter controller.
- The app commissions the Thread device with connectedhomeip using BLE and the Thread dataset.
- The app stores bootstrap node/controller state.
- Later, the app restores that node and opens an OpenCommissioningWindow through connectedhomeip.
- The fake controller is test-only and must not be used as fallback for real Thread commissioning or OCW.

Read `docs/open-commissioning-window-workflow.md` before changing OCW behavior.

## Source Map

- UI entry point: `app/src/main/java/org/openhab/matter/companion/MainActivity.java`
- Controller abstraction: `MatterController`
- connectedhomeip facade: `ConnectedHomeIpMatterController`
- Reflection gateway: `ConnectedHomeIpReflectionGateway`
- Reflection command factory: `ConnectedHomeIpReflectionCommandFactory`
- Bootstrap state: `MatterBootstrapStateRepository`, `MatterBootstrapStateResolver`, `MatterBootstrapState`
- Device pointer lifecycle: `ConnectedHomeIpReflectionDevicePointerProvider`, `ConnectedHomeIpConnectedDeviceCallback`, `ConnectedHomeIpDevicePointer`
- OCW callback/result: `ConnectedHomeIpOpenCommissioningWindowCallback`, `MatterOpenCommissioningWindowResult`

## Invariants

- Keep the native readiness gate before real commissioning.
- Preserve fail-closed behavior when connectedhomeip is unavailable.
- Do not hide real failures behind simulation.
- Use the manual setup code returned by OCW directly.
- Do not derive manual code from QR unless the task explicitly asks for that feature.
- Prefer the concrete OCW callback class when packaged. A dynamic proxy callback can fail Android CheckJNI return-type validation.

## Current Parameters To Check

- OCW timeout: see `MainActivity.runOpenCommissioningWindow()`.
- OCW discriminator: see `MainActivity.runOpenCommissioningWindow()`.
- Enhanced commissioning iteration count: see `ConnectedHomeIpMatterController`.
- Device-pointer wait timeout: see `ConnectedHomeIpMatterControllerFactory`.
- OCW callback wait timeout: see `ConnectedHomeIpReflectionGateway`.

## Failure Checklist

- Missing or invalid connectedhomeip artifacts.
- Runtime readiness/preflight failure.
- BLE scan filter mismatch or no advertisement for discriminator.
- Retryable GATT failures such as `62` or `133`.
- Attestation handling or bypass mismatch.
- ICD registration issues.
- Operational discovery timeout after BLE Thread commissioning.
- Device-pointer acquisition timeout/failure.
- OCW command did not start.
- OCW callback error, timeout, blank code, or JNI callback crash.

