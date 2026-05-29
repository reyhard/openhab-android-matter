# Matter Setup UI Refresh Design

## Context

The app already has a Compose-based Matter setup flow, persisted openHAB settings, encrypted token and Thread dataset storage, Thread Border Router discovery, advanced troubleshooting, and a Devices on this phone submenu. This design refreshes the presentation and navigation so the app matches the supplied mockups more closely and gives first-run users a clear required setup path before they can add a Matter device.

Reference mockups created during design:

- `.superpowers/brainstorm/manual-ui-design-1780077600/setup-flow-standalone.html`
- `.superpowers/brainstorm/manual-ui-design-1780077600/settings-page-standalone.html`
- `.superpowers/brainstorm/manual-ui-design-1780077600/thread-network-editor-standalone.html`

## Goals

- Show a polished Get started screen on first launch.
- Require openHAB address, access token, Active Operational Dataset, and Thread Border Router address before the first setup can be saved.
- Validate first-run settings before saving them.
- Move successful first-run setup to a main Add Matter device screen, not directly into camera scanning.
- Provide both QR scanning and manual 11-digit setup-code entry from the main screen.
- Use the supplied plug/openHAB image in the welcome screen and the supplied QR scan-guide image in the Add Matter device screen.
- Move settings behind a cog icon in the top-right of the main screen.
- Remove visible Back to main menu buttons from subpages and rely on Android system back.
- Improve Settings organization so common connection tasks are separate from Thread editing, staged-device handling, and advanced diagnostics.

## Non-Goals

- Do not change the connectedhomeip commissioning architecture.
- Do not silently route real commissioning through the fake controller.
- Do not redesign openHAB handoff behavior beyond the UI and validation flow described here.
- Do not expose stored tokens, Thread datasets, QR codes, manual setup codes, or fabric material in permanent docs or logs.

## First-Run Flow

On a fresh install or when no required configuration is available, the app starts at a welcome screen:

- title: `Set up Matter with openHAB`
- openHAB logo near the top
- supplied plug/openHAB visual asset
- short benefit rows using user-facing language only
- primary button: `Get started`

Tapping Get started opens the required setup screen. This screen collects:

- openHAB address, defaulting to `http://openhab:8080`
- access token
- Active Operational Dataset
- Thread Border Router address

The openHAB address helper text should explain the field: `Address of your openHAB instance.` The access-token helper should tell users where to create a token in openHAB, using plain product wording such as `Create one in openHAB under Profile / API tokens.`

The access token and Thread dataset fields are masked by default while typing and have reveal icons. After the token is saved, the Settings UI must not display or prefill the stored token value. Token replacement happens through an explicit `Change token` action.

The Thread Border Router row includes `Detect border router`, which runs the existing Thread Border Router discovery and lets the user select a detected endpoint.

The primary setup action is `Test settings`. On first run, settings are saved only after all required checks pass:

- openHAB REST is reachable
- the token is accepted by openHAB
- the openHAB Matter controller is online
- the Thread dataset parses as a valid Active Operational Dataset
- the Thread Border Router address is usable, or it was selected from discovery

If validation fails, the screen remains in setup mode and shows actionable, sanitized failure details. It must not save partial first-run settings as if setup were complete.

After validation succeeds, the app navigates to the main Add Matter device screen.

## Main Add Matter Device Screen

The main screen is the normal post-setup landing page. It has:

- openHAB logo near the top
- settings cog icon in the top-right
- title: `Add Matter device`
- supplied QR scan-guide image
- readiness summary rows for openHAB, Thread network, and Bluetooth/location readiness
- primary button: `Scan code`, with a QR-style icon next to the text
- secondary button: `Enter code manually`

Supporting copy can say the user can scan the device QR code or enter the setup code manually. The manual path should focus on the 11-digit Matter manual setup code in user-facing copy. The existing parser may continue accepting other supported formats where already implemented, but the simplified UI copy should not lead with those variants.

The Add Matter device screen is the guide screen. Tapping Scan code from that screen opens the camera scanner, instead of dropping users directly into a minimal full-screen camera with little context on app launch. The camera screen itself can remain focused on scanning.

The main screen should not display `Pairing window open`. Once pairing starts, progress belongs in the setup progress flow, where a new state is shown.

## Settings Entry And Navigation

Settings are accessed from the cog icon in the top-right of the main Add Matter device screen. Existing text buttons that say `Back to main menu` should be removed from settings-adjacent pages.

Android system back should work from:

- Settings
- Thread Network editor
- Devices on this phone
- Advanced troubleshooting
- Manual code entry
- QR scan guide / scan entry

The system back behavior should return to the prior logical screen and should not trap users in Advanced troubleshooting or Devices on this phone.

## Settings Page

Settings should be organized into task-based sections.

### openHAB Connection

Shows:

- openHAB address
- access token status
- `Test`
- `Change token`

The access token status pill should say `Set` when a token exists, with a green success-style background. Stored token values remain hidden and are not prefilled into editable text fields.

`Change token` opens a controlled token replacement state where the user can type a new token, reveal it while typing, test it, and save only if accepted.

### Thread Network

Shows:

- Active Operational Dataset status
- Thread Border Router address/status
- `Edit`
- `Detect router`

The Settings card stays compact. `Edit` opens a focused Thread Network editor rather than expanding all fields inline on the main Settings page.

### This Phone

Shows:

- `Devices on this phone`
- staged-device count or status
- action to view staged devices
- action to enter troubleshooting if handoff fails

This section represents this app's persisted staging state, not a full connectedhomeip fabric inventory.

### Advanced

Shows lower-priority controls:

- Developer attestation bypass
- Network diagnostics entry point

Advanced controls remain accessible, but they should not dominate first-run setup or the main settings page.

## Thread Network Editor

The Thread Network editor opens from Settings > Thread network > Edit. It contains:

- title: `Thread network`
- short explanation that these values are used to commission Matter devices to Thread before openHAB handoff
- masked/revealable Active Operational Dataset field
- dataset validation status
- Thread Border Router address field
- `Detect border router`
- detected router list with `Select`
- `Cancel`
- `Save`

Saving requires the dataset to parse correctly. Border Router discovery can populate the address field, but discovery does not infer or replace the Thread dataset.

## Devices On This Phone

The existing Devices on this phone feature remains a focused view of app-staged bootstrap state. It should use system back rather than visible Back to main menu buttons.

Actions remain:

- retry OpenCommissioningWindow for the staged device when possible
- continue openHAB handoff where supported
- forget the staged device from this app

Copy must not imply that this is a complete connectedhomeip fabric inventory.

## Advanced Troubleshooting

Advanced troubleshooting remains available from Settings and failure states. It should keep the existing phone-side Matter mDNS, IPv6 reachability, readiness, and recovery guidance. It should use system back for returning to the previous screen.

## Assets

Use supplied bitmap assets:

- Welcome/Get started: `docs/interface/image_1.png`
- Add Matter device scan guide: `docs/interface/image_2.png`

Use the existing openHAB logo asset where practical:

- `docs/interface/openhab-icon.svg`

Implementation may need to copy these assets into Android drawable resources in an Android-compatible format. The source design assets should remain under `docs/interface/`.

## State And Data Flow

The ViewModel remains the owner of persisted configuration and setup navigation state. It should distinguish:

- first-run setup required
- first-run setup validation in progress
- configured main Add Matter device screen
- settings view
- Thread Network editor
- token replacement view
- Devices on this phone
- Advanced troubleshooting

Configuration should be saved through the existing repository only after the relevant validation passes:

- first-run full validation before marking setup complete
- token replacement validation before replacing the stored token
- Thread settings validation before replacing Thread dataset and Border Router settings

Sensitive values should continue using encrypted app-private storage.

## Error Handling

Validation failures should be shown near the relevant section with clear action text. Diagnostics must remain sanitized:

- do not display full tokens
- do not display full Thread datasets in logs or permanent docs
- do not display full QR/manual codes in persistent diagnostics
- sanitize openHAB URLs where existing logging already does so

If connectedhomeip artifacts or runtime readiness are unavailable, real commissioning must fail closed as it does today. The UI refresh must not weaken that readiness gate.

## Testing

Unit coverage should verify:

- first-run setup requires all required fields
- default openHAB URL is `http://openhab:8080`
- first-run validation saves only after all checks pass
- failed validation does not mark setup complete
- saved token is represented as `Set`, not displayed
- `Change token` validates before replacing a stored token
- Thread editor save requires valid dataset input
- Thread Border Router detection can populate the router field
- system back returns from Settings, Thread editor, Devices on this phone, and Advanced troubleshooting

Compose UI tests should verify:

- welcome screen appears on first launch
- Get started opens setup
- successful Test settings lands on Add Matter device
- Add Matter device screen shows `Scan code`, QR icon, `Enter code manually`, and the scan-guide visual
- settings cog opens Settings
- Settings sections render in the intended order
- Thread Network Edit opens the editor
- Back to main menu buttons are absent from the updated screens

Manual/device verification should include:

- scanning path still launches the QR scanner
- manual 11-digit code path still starts the expected pairing flow
- settings validation works against a real openHAB instance
- Thread Border Router discovery still lists `_meshcop._udp` results when available
