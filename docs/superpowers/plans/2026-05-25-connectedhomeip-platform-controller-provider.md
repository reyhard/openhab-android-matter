# Connectedhomeip Platform Controller Provider Plan

Goal: add an offline-testable provider that constructs the connectedhomeip Android platform and `ChipDeviceController` through reflection, without compile-time CHIP dependencies.

## Scope

- Load `ChipDeviceController.loadJni()` before platform construction.
- Construct Android platform dependencies through reflection:
  - `AndroidBleManager(context)`
  - `AndroidNfcCommissioningManager()`
  - `PreferencesKeyValueStoreManager(context)`
  - `PreferencesConfigurationManager(context)`
  - `NsdManagerServiceResolver(context)`
  - `NsdManagerServiceBrowser(context)`
  - `ChipMdnsCallbackImpl()`
  - `DiagnosticDataProviderImpl(context)`
- Construct `AndroidChipPlatform(...)` with those dependencies.
- Build `ControllerParams.newBuilder().setControllerVendorId(0xFFF4).setEnableServerInteractions(true).build()`.
- Construct and cache `ChipDeviceController(ControllerParams)`.
- Keep UI selection unchanged until BLE provider and platform provider can be wired safely.

## Verification

- Add fake-class unit tests proving call order, parameter mapping, and controller caching.
- Run targeted provider tests and full debug unit/build verification.
