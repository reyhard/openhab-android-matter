# Compose Automated Matter Setup Design

## Goal

Replace the current developer-oriented single-screen app flow with a user-friendly automated Matter setup workflow for openHAB.

The v1 success condition is: the app commissions a Matter-over-Thread device to the phone, opens an OpenCommissioningWindow, submits the returned manual setup code to openHAB Matter discovery scan input, and detects a Matter entry in the openHAB Inbox.

The design should keep a path toward future integration into the official openHAB Android app. The implementation should therefore be native Android, Kotlin-first for new workflow/UI code, and keep Matter/openHAB operations outside the Compose presentation layer.

## Context

The current app already has the important underlying capabilities:

- Native QR scanning and setup-payload parsing.
- openHAB readiness and Inbox observation.
- connectedhomeip readiness gating.
- Real BLE Thread commissioning through connectedhomeip artifacts when available.
- OpenCommissioningWindow support with returned manual code and QR display.
- Existing auto openHAB Matter discovery scan design in `docs/superpowers/specs/2026-05-28-openhab-auto-matter-scan-design.md`.

The current UI is still built programmatically in `MainActivity` and exposes most controls, inputs, and logs on one screen. The new design should make the happy path feel like a normal smart-home setup wizard while preserving advanced diagnostics for recovery.

Generated visual references live in `docs/interface/Interface_1.png` through `docs/interface/Interface_7.png`. They are directional mockups, not exact implementation requirements.

## Scope

In scope:

- Add Jetpack Compose as the primary UI for the automated setup flow.
- Add Kotlin workflow/state classes that orchestrate existing Java services.
- Preserve existing Java domain/controller/openHAB code where practical.
- Build the main flow around real operations, not a demo-only shell.
- Show progress updates for every major step.
- Show a visible countdown while the commissioning window is open.
- Run automatic basic diagnostics when the flow fails.
- Add advanced troubleshooting surfaces for logs, pending staged devices, manual OCW retry, mDNS discovery checks, IPv6 reachability checks, and forget-from-phone. Pending-device details may be limited to data the app already knows or can safely query through connectedhomeip in v1.

Out of scope for v1:

- Flutter or other cross-platform UI frameworks.
- Rewriting connectedhomeip commissioning logic.
- Replacing openHAB's Matter binding behavior.
- Auto-approving Inbox entries into openHAB Things.
- Claiming success only when a Thing is created and online.
- Deriving a manual setup code from QR payloads.
- Showing raw Thread datasets, REST tokens, full setup QR/manual codes, or fabric material in persistent logs.

## Recommended Approach

Use a workflow-first Compose migration.

New Kotlin workflow classes should own setup state, transitions, progress, errors, diagnostics, and user actions. Compose should render immutable UI state and dispatch actions. Compose must not directly call connectedhomeip, REST clients, encrypted storage, or low-level Android diagnostics.

This keeps the valuable feature logic reusable if the official openHAB Android app later adopts the workflow but wants a different presentation style.

## Architecture

```text
Compose UI
  MatterSetupScreen
  OpenHabSetupScreen
  ScanDeviceScreen
  SetupProgressScreen
  SetupFailureScreen
  AdvancedTroubleshootingScreen
  PendingDeviceDetailsScreen

Kotlin workflow layer
  MatterSetupWorkflow
  MatterSetupUiState
  MatterSetupAction
  MatterSetupStep
  MatterSetupError
  MatterSetupDiagnosticsSummary
  MatterSetupProgressEvent

Java/domain layer
  MatterSetupPayloadParser
  ThreadDataset
  MatterController / ConnectedHomeIpMatterController
  MatterBootstrapStateRepository
  NativeChipControllerSession
  openHAB readiness, discovery scan, Inbox clients
  QR scanner activity and result extraction
```

The workflow layer should wrap existing operations behind small interfaces where needed. These interfaces can initially delegate to the existing Java classes and later be swapped or moved into the official openHAB Android app.

## Main User Flow

First launch starts with openHAB setup if no valid openHAB configuration exists. The user enters the openHAB base URL and optional token, then the app tests readiness.

After openHAB setup, the primary action is "Add Matter device".

```text
Scan QR
-> Check readiness
-> Prompt user to put device in pairing mode
-> Commission device to this phone
-> Open commissioning window
-> Show commissioning-window countdown
-> Send setup code to openHAB
-> Watch openHAB Inbox
-> Succeed when a Matter Inbox entry is detected
```

The success screen should say "Device found by openHAB" or equivalent. It must not claim the device is fully added as a Thing or online in v1.

## Workflow State Machine

```text
NeedsOpenHabSetup
OpenHabSetupChecking
ReadyToScan
ScanningQr
QrScanned
ReadinessChecking
NeedsPairingMode
CommissioningToPhone
OpeningCommissioningWindow
CommissioningWindowOpen
SendingCodeToOpenHab
WatchingOpenHabInbox
SuccessInboxDetected
Failed
AdvancedTroubleshooting
```

Each state should carry:

- Primary title.
- Short user-facing message.
- Current step list with status.
- Optional progress percentage when meaningful.
- Optional countdown seconds for OCW.
- Primary and secondary actions.
- Sanitized technical details for advanced views.

## Progress Presentation

The progress screen should show stable steps:

```text
Checking setup
Connecting to device
Adding device to this phone
Opening pairing window
Sending setup code to openHAB
Waiting for openHAB
```

Step labels are user-facing. Internal details such as `getConnectedDevicePointer`, CASE, PASE, node ids, and raw CHIP status should only appear in advanced logs.

The OCW countdown should start when the window is successfully opened and use the configured timeout, currently 300 seconds. The UI should show remaining time with text such as:

```text
Pairing window open for 4:32
```

If the countdown expires before openHAB Inbox detection, the failure recovery screen should suggest opening the pairing window again.

## Readiness Checks

Before commissioning, the workflow should run readiness checks and prompt for remediable problems.

Bluetooth:

- If Bluetooth is disabled, prompt the user to enable it before BLE commissioning.
- If Bluetooth LE is unavailable, block BLE commissioning.

Permissions:

- Camera permission for QR scanning.
- Bluetooth/Nearby devices permissions required by the Android version.
- Location permission and location services where Android BLE scanning requires them.
- Explain location as "needed to find nearby Matter devices".

Network:

- Detect active network transports through Android connectivity APIs.
- Prefer Wi-Fi for local openHAB, OTBR, mDNS, and IPv6 diagnostics.
- Warn when the phone appears to be on mobile data only.
- Warn when VPN is active because it can block local discovery, openHAB access, or IPv6 routes.

openHAB:

- Check `/rest/`.
- Check that a Matter controller Thing is online when available from `/rest/things`.
- Check `/rest/inbox` access when token/config allows it.

connectedhomeip:

- Keep the existing native readiness gate.
- Real commissioning and OCW must stop if connectedhomeip is not ready.
- Do not fall back to `FakeMatterController` for real commissioning.

Device pairing mode:

- After QR scan, show a short pairing-mode instruction screen.
- During BLE scan, show "Looking for this device nearby".
- On scan timeout, suggest pairing mode, distance, power, Bluetooth, and factory reset only if the device was previously paired.

## Error Handling

When a step fails, the workflow should automatically run basic diagnostics and then show a recovery screen.

The recovery screen should include:

- What failed.
- Likely causes.
- Checks the app ran.
- Recommended next action.
- A "Show troubleshooting" action for advanced tools.

Failure-specific guidance:

```text
Invalid QR or manual code
- Rescan or enter code manually.

BLE scan timeout
- Put the device in pairing mode.
- Keep the phone near the device.
- Check Bluetooth and required permissions.
- Retry before the device leaves pairing mode.

Commissioning failed after BLE connection
- Bias suggestions toward Thread network, IPv6 routing, OTBR, and mDNS.
- Suggest checking the Thread dataset and OTBR only after the user-facing network hints.

OpenCommissioningWindow failed
- Retry opening the pairing window.
- If bootstrap state is missing or unreadable, rerun commissioning.

openHAB scan rejected
- Check openHAB URL, token, Matter binding, and Matter controller Thing.

Inbox not detected before timeout
- Explain that openHAB accepted the code but did not report a Matter Inbox entry.
- Suggest IPv6 routing, OTBR reachability, mDNS/Avahi, stale Matter records, and openHAB logs.
- Offer Open Commissioning Window retry if the device is still staged on the phone.

Too many administrators suspected
- Explain that Matter devices can be shared with only a limited number of systems.
- Offer advanced "Forget from this phone" after confirmation.
```

## Diagnostics

Automatic basic diagnostics should run after a failure:

```text
openHAB readiness
Matter controller readiness
Inbox endpoint access
Runtime permissions
Bluetooth enabled/available
Network transport summary
VPN active/inactive
OTBR diagnostic URL or host reachability when configured
Device IPv6 reachability when a current IP is known
```

Advanced diagnostics should be available from failure screens and an advanced settings area.

### IPv6 Reachability

The UI should call this a reachability check, not a guaranteed ping.

Android cannot always rely on ICMP or a system `ping6` binary. The diagnostic should try practical checks and report limitations:

```text
1. Java `InetAddress` reachability for IPv6 addresses.
2. Socket/TCP probe only when a relevant host and port are known.
3. Fallback: show last known IPv6 and explain that the phone could not verify reachability directly.
```

### mDNS Discovery

Add a Matter discovery diagnostic using Android NSD/DNS-SD where feasible.

Browse:

```text
_matterc._udp
_matter._tcp
```

Show discovered records:

- Service type.
- Instance name.
- Resolved host.
- IPv6 addresses, if available.
- Port.
- TXT fields, where available and not sensitive.
- First seen and last seen.

User-facing interpretation:

```text
Phone can see the device
This phone discovered a Matter service for this device. If openHAB still cannot find it, check mDNS/Avahi and IPv6 routing on the openHAB side.

Phone cannot see the device
No Matter service was discovered from this phone. The pairing window may have expired, the device may not be reachable, or mDNS may be blocked on this network.
```

The UI must state the limitation:

```text
This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup.
```

## Advanced Device Management

Advanced management is a recovery tool, not the app's front door.

The app should show devices staged on this phone but not yet confirmed in openHAB. For each device, expose:

- Open commissioning window.
- Send code to openHAB.
- Details.
- Forget from this phone.

Details may include available values. The implementation should show unknown/unavailable for fields that connectedhomeip or the device does not expose yet:

- Device name or label.
- Vendor and product.
- Firmware version.
- Node id.
- Last known IPv6 address.
- Last seen.
- Commissioning window status/countdown.
- openHAB handoff status.

"Forget from this phone" should clearly say it removes the app's controller fabric and frees one Matter administrator slot. It must not imply factory reset or removal from other ecosystems.

## Security And Privacy

- Do not log REST tokens, Thread datasets, setup PINs, full QR/manual codes, or fabric material.
- Short-lived UI may display the temporary manual setup code when needed for handoff, but persistent logs should not store it.
- Advanced logs should be sanitized by default.
- Device node ids and IPv6 addresses may appear in advanced diagnostics because they are needed to troubleshoot the active issue.
- The connectedhomeip readiness gate must continue to fail closed.

## Testing

Unit tests should cover the workflow state machine independent of Compose:

- First-run openHAB setup required when no config exists.
- Successful state progression through Inbox-detected success.
- OCW countdown starts from the returned/configured timeout.
- Countdown expiry produces a recovery action.
- Each major failure maps to the expected recovery suggestions.
- Automatic diagnostics are requested after failure.
- Sensitive values are not included in progress, diagnostics summaries, or logs.

Compose UI tests should cover:

- First-run setup screen.
- Ready-to-scan screen.
- Progress step rendering.
- Countdown rendering.
- Failure screen with diagnostics summary.
- Advanced troubleshooting entry points.

Existing Java tests should remain for parsers, clients, connectedhomeip readiness, and openHAB scan behavior.

Verification should include:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Hardware validation should confirm:

- Bluetooth disabled state prompts correctly.
- VPN/mobile-data warning appears when applicable.
- BLE scan timeout recovery text helps put the device in pairing mode.
- Thread commissioning still uses connectedhomeip and fails closed when unavailable.
- OCW countdown matches the actual 300-second window.
- openHAB scan submission uses the returned manual code.
- Inbox detection drives v1 success.
- mDNS diagnostics report what the phone sees without claiming openHAB sees the same records.
