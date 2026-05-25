# Connectedhomeip Android BLE Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Android BLE connection provider that scans for Matter BLE advertisements by discriminator, connects with GATT, forwards GATT callbacks to connectedhomeip's `AndroidBleManager`, registers the connection id, and returns a cleanup-safe `ConnectedHomeIpBleConnection`.

**Architecture:** Keep the provider behind the existing `ConnectedHomeIpBleConnectionProvider` seam. Split Android BLE details into injectable wrappers so unit tests can verify service-data filter construction, timeout/error paths, callback forwarding, connection id registration, and cleanup without a real BLE radio. The provider is not wired into `MainActivity` until connectedhomeip artifacts and device testing are available.

**Tech Stack:** Android Java, Bluetooth LE APIs, reflection-backed connectedhomeip platform provider, JUnit/Robolectric unit tests, Gradle offline verification.

---

## Files

- Create: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpAndroidBleConnectionProvider.java`
- Create: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpAndroidBleConnectionProviderTest.java`
- Modify: `docs/chip-jni-integration.md`
- Modify: `docs/implementation-status.md`

## Task 1: Matter BLE Advertisement Filter

- [ ] **Step 1: Write failing tests**

Create tests that verify full discriminator service-data bytes for discriminator `3840` are `[0x00, 0x00, 0x0F]`, mask is `[0xFF, 0xFF, 0xFF]`, and service UUID is `0000FFF6-0000-1000-8000-00805F9B34FB`.

- [ ] **Step 2: Run targeted test**

Run: `./gradlew.bat :app:testDebugUnitTest --offline --tests "*ConnectedHomeIpAndroidBleConnectionProviderTest"`
Expected: compile failure because provider does not exist.

- [ ] **Step 3: Implement minimal filter helper**

Implement immutable `MatterBleScanFilter` nested value with UUID, serviceData, and serviceDataMask, plus `matterScanFilter(int discriminator)`.

- [ ] **Step 4: Verify targeted test passes**

Run the same targeted Gradle command.
Expected: PASS.

## Task 2: Scan/Connect/Register Flow

- [ ] **Step 1: Write failing tests**

Add fake scanner, connector, and connectedhomeip BLE manager wrappers. Verify `connect(3840)` starts scan with the Matter filter, stops scan after finding a device, connects GATT, calls `addConnection(gatt)`, calls `setBleCallback(...)`, and returns `ConnectedHomeIpBleConnection(gatt, connId, cleanup)`.

- [ ] **Step 2: Run targeted test**

Run targeted provider tests.
Expected: FAIL on missing flow implementation.

- [ ] **Step 3: Implement provider flow**

Add constructor-injected interfaces for BLE scanner, GATT connector, connectedhomeip BLE manager, and clock/waiter. Implement timeout-safe scan/connect flow without UI threading assumptions.

- [ ] **Step 4: Verify targeted test passes**

Run targeted provider tests.
Expected: PASS.

## Task 3: Cleanup and Error Paths

- [ ] **Step 1: Write failing tests**

Cover scan timeout, null connection id `0`, connector failure, and cleanup calling `removeConnection(connId)` and `BluetoothGatt.close()`.

- [ ] **Step 2: Run targeted test**

Run targeted provider tests.
Expected: FAIL on missing error/cleanup handling.

- [ ] **Step 3: Implement cleanup and failures**

Make failures deterministic, stop scans on all paths, close GATT on registration failure, and use `ConnectedHomeIpBleConnection` close action for registered connections.

- [ ] **Step 4: Verify targeted test passes**

Run targeted provider tests.
Expected: PASS.

## Task 4: Docs and Full Verification

- [ ] **Step 1: Update docs**

Update implementation status and CHIP JNI integration docs to show BLE scan/connect/provider exists but real device verification is pending.

- [ ] **Step 2: Run full verification**

Run: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Sidecar review and commit**

Request a subagent review for BLE provider correctness, fix findings, rerun full verification, then commit with message `feat: add connectedhomeip Android BLE provider`.
