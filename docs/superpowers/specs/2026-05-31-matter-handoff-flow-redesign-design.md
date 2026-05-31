# Matter Handoff Flow Redesign Design

## Context

The app stages Matter devices on the Android connectedhomeip fabric so they can be handed off to openHAB through OpenCommissioningWindow. The existing Compose **Devices on this phone** submenu is a view of this app's persisted staging state, not a full fabric inventory. This design redesigns that staging view and adds a polished **Device details** screen while preserving the existing commissioning-window, forget, navigation, staged storage, and openHAB handoff behavior.

The UI target is the supplied mockup pair:

- **Devices on this phone**: staged-device card with expandable diagnostics and icon actions.
- **Device details**: advanced details page with a header card, details rows, explicit fetch button, copyable values, and bottom actions.

## Goals

- Redesign **Devices on this phone** into a polished Material-style list of staged devices.
- Add a **Device details** screen that shows the mockup-style details rows.
- Keep product images out of both screens.
- Keep staged-device business logic intact.
- Fetch only product name and vendor name by default from the current staging/commissioning metadata path.
- Fetch deeper Matter cluster data only after the user taps **Fetch additional data from device**.
- Show optional fetched values when available and use clear fallbacks when attributes are absent or unreadable.
- Keep long values such as node IDs and IPv6 addresses readable and copyable.
- Preserve and extend accessibility labels for icons, row copy actions, and destructive actions.

## Non-Goals

- Do not implement product image fetching.
- Do not show product image placeholders.
- Do not turn **Devices on this phone** into a full connectedhomeip fabric inventory.
- Do not auto-fetch deep cluster data when opening the details screen.
- Do not add a separate fetched-data preview section or fetched-data chips.
- Do not change the connectedhomeip fail-closed readiness gate.
- Do not change openHAB handoff architecture or silently route real commissioning through `FakeMatterController`.

## Screen 1: Devices On This Phone

The screen uses the existing Compose app scaffold/top row and Material 3 styling. It keeps system back behavior and does not add visible back-to-main-menu buttons.

Copy:

- Page title: `Devices on this phone`
- Intro text: `Matter devices staged by this app for openHAB handoff.`
- Section title: `Stored Matter staging`
- Section description: `This list only shows devices staged by this app on the Android fabric for openHAB handoff.`

Each staged device renders as one rounded Material-style card with subtle elevation or tonal surface treatment. The card header contains:

- Product name as the primary title.
- Vendor name directly below.
- A chevron expand/collapse icon button in the top-right.

Fallbacks:

- Product: `Unknown Matter device`
- Vendor: `Unknown vendor`

The list card intentionally does not show:

- Product image.
- Product image placeholder.
- Three-dot overflow menu.
- Status/state pill in the header.
- `Send code to openHAB` or `Send command to openHAB`.
- Firmware, battery, Thread, IPv6, OTA, or any deep fetched metadata.

### Expandable Diagnostics

Diagnostics are local UI state per device. Collapsed cards hide diagnostics. Expanded cards show only staging diagnostics:

- `Node`: formatted node id or `Unknown`
- `Controller state`: `stored`, `missing`, or `Unknown`
- `State readable`: `yes`, `no`, or `Unknown`
- A compact pale-blue information box with an info icon when a debug warning is relevant.

For the current missing-controller-state case, the debug box text is:

`Debug attempt: controller state is missing, so connectedhomeip may still fail to open the commissioning window.`

### Card Actions

The bottom of each card keeps only these actions:

- `Open commissioning window`
- `Details`
- `Forget from this phone`

Each action uses an icon:

- Open commissioning window: pairing/window-style icon, or the closest available drawable.
- Details: info/details icon.
- Forget from this phone: delete/trash icon, colored red for the destructive action.

On wider phones the actions may be equal-width cells with dividers, matching the mockup direction. On small phones the layout must avoid text clipping; it may wrap or stack if required. Existing behavior is preserved:

- `Open commissioning window` dispatches the existing commissioning-window retry/handoff path.
- `Details` opens the new **Device details** screen for that staged device.
- `Forget from this phone` dispatches the existing staged-state cleanup path.

## Screen 2: Device Details

The details screen is the only screen that presents the mockup-style device metadata rows. It initializes from local staged data and updates when explicit fetch succeeds.

Copy:

- Breadcrumb/link: `Advanced`
- Page title: `Device details`
- Subtitle: `Helpful information for advanced setup and troubleshooting.`

### Header Card

The header is a simple rounded white card. It does not show an image or placeholder. It displays:

- Product/device name prominently.
- Soft beige status pill: `Not yet added to openHAB`

The product/device name uses the product fallback `Unknown Matter device` when no product name is available.

### Details Card

The main details card contains icon + label + value rows in this order:

- `Device name`
- `Vendor`
- `Product`
- `Firmware version`
- `Hardware version`
- `Part number`
- `Node ID`
- `Battery`
- `Thread network`
- `IPv6 address`
- `OTA update`

Default values before explicit fetch:

- Device name: staged product name, or `Unknown Matter device`.
- Vendor: staged vendor name, or `Unknown vendor`.
- Product: staged product name, or `Unknown`.
- Node ID: staged node id, or `Unknown`.
- All deeper fields: `Unknown`.

After fetch, available values are merged into this same details card. Missing values remain `Unknown`; fetched failures do not clear previous values.

Long values may wrap, shrink, or visually ellipsize, but the full underlying value must remain available to copy. Rows should use comfortable spacing and stay readable on small Android phones.

### Copy Interaction

Every data row is clickable. Tapping a row copies only the row value, not the label, to the clipboard. Examples:

- Tapping `Vendor` copies `IKEA of Sweden`.
- Tapping `Node ID` copies the full node id.
- Tapping `IPv6 address` copies the full IPv6 address.

After copying, show lightweight feedback such as:

- `Copied`
- or `Copied IPv6 address`

Rows expose accessibility labels/hints such as:

- `Copy Vendor`
- `Copy Node ID`
- `Copy IPv6 address`

### Fetch Additional Data

Below the details card and above the bottom actions, show an outlined blue Material-style button:

`Fetch additional data from device`

The button uses a cloud download icon if available. If the Compose Material icon dependency is not present, use the closest existing cloud/download drawable or add a small local vector drawable. Helper text below the button:

`Reads more information directly from Matter clusters.`

While fetching:

- Disable the button.
- Show a loading state such as a spinner or `Fetching...`.

On success:

- Merge returned values into the main details rows.
- Keep the details card as the single source of displayed device data.
- Optionally update helper text to indicate data was refreshed.

On failure:

- Keep existing local values visible.
- Show snackbar/toast: `Could not fetch data from device`.
- Do not clear previously available data.

Do not show:

- A separate `Fetched from Matter clusters` preview section.
- Fetched-data chips below the fetch button.

### Bottom Actions

The bottom of the details screen keeps only:

- Primary filled blue button: `Open commissioning window`
- Secondary outlined blue button: `Forget from this phone`

Both keep existing behavior.

## Data Model And Controller Flow

The staged-device list continues to derive from `MatterBootstrapState` through `PhoneMatterDevice`. The model should be extended conservatively so it can represent:

- node id
- controller-state stored/missing
- state-readable flag
- staged/default vendor name
- staged/default product name
- details/fetch state for the selected device
- optional fetched attributes for the details screen

Compose must not read Matter clusters directly. The ViewModel owns navigation, selected-device details state, fetch state, and action dispatch. It calls a single controller-layer metadata reader for the explicit fetch.

The existing `ConnectedHomeIpDeviceMetadataReader` seam currently reads BasicInformation vendor/product. It should be expanded or replaced by a richer details reader that can read optional attributes from:

- BasicInformation
- PowerSource
- NetworkCommissioning where useful
- GeneralDiagnostics
- ThreadNetworkDiagnostics
- OtaSoftwareUpdateRequestor

Default/staged identity should still be available without pressing the details fetch button. Deeper fields must only be fetched on the details screen after user action.

If individual cluster reads fail, the reader should return partial data where possible. A total fetch failure should surface as a fetch error while preserving the existing values.

## Matter / CHIP Mapping

BasicInformation:

- `vendorName` -> `Vendor`
- `productName` -> `Product` and `Device name` fallback
- `softwareVersionString` -> `Firmware version`
- `hardwareVersionString` -> `Hardware version`
- `partNumber` -> `Part number`

PowerSource:

- `batPercentRemaining` -> battery percentage. Matter reports half-percent units, so `104` displays as `52%`.
- `batQuantity` plus `batReplacementDescription` or `batCommonDesignation` -> battery type, for example `2×AAA`.

NetworkCommissioning:

- Connected network/interface data may augment network status where available.

GeneralDiagnostics:

- `networkInterfaces[].iPv6Addresses` -> IPv6 address rows.
- If multiple IPv6 addresses are available, display the most useful stable address first.

ThreadNetworkDiagnostics:

- `networkName` -> Thread network name.
- `channel` -> Thread channel.
- Deeper fields such as routing role, PAN IDs, and active faults are out of scope for this task.

OtaSoftwareUpdateRequestor:

- `updatePossible = true` -> `Possible`
- `updatePossible = false` -> `Not available`
- `updateState` may be used later for more detailed OTA status.

## Formatting Helpers

Add or extend shared helpers for:

- product/vendor/node/controller-state fallbacks
- node id formatting as uppercase unsigned hexadecimal with `0x` prefix
- battery half-percent conversion
- battery quantity/type display
- IPv6 byte-array formatting
- preferred IPv6 address selection
- Thread network display such as `OpenThread · Channel 25`
- OTA display

Fallbacks must be stable and user-facing:

- Product: `Unknown Matter device`
- Vendor: `Unknown vendor`
- Node: `Unknown`
- Controller state: `Unknown`
- State readable: `Unknown`
- Optional fetched details: `Unknown`

## Navigation

Add a distinct details navigation state/stage for **Device details**. The list card `Details` action selects the staged device and opens the details stage. Android system back from details should return to **Devices on this phone**. The `Advanced` breadcrumb/link should also return to the advanced/settings context used by the existing flow, without introducing a visible back-to-main-menu button.

## Error Handling

The existing connectedhomeip readiness gate remains authoritative. If connectedhomeip artifacts/runtime are unavailable, real commissioning and live fetch paths fail closed rather than using simulation.

Fetch errors are localized to the details screen:

- Existing staged/default values remain visible.
- The UI shows `Could not fetch data from device`.
- Partial data should still be merged when the reader can return it safely.

Sensitive values remain protected. The redesign must not permanently document or log REST tokens, Thread datasets, fabric material, setup PINs, or full QR/manual setup codes.

## Accessibility

Required accessibility coverage:

- Chevron button: `Show diagnostics` / `Hide diagnostics`.
- List actions: descriptive labels for opening commissioning window, viewing details, and forgetting from phone.
- Delete/forget action must be announced as destructive where practical.
- Details rows: `Copy <label>` semantics.
- Fetch button: label plus loading state.
- Icons should be decorative only when the text already fully labels the action; otherwise they need content descriptions.

## Testing

### Devices On This Phone Compose Tests

- Page title and intro copy render.
- Product image is not rendered.
- Product image placeholder is not rendered.
- Three-dot menu is not rendered.
- Send-to-openHAB button is not rendered.
- Product and vendor are visible.
- Diagnostics are hidden when collapsed.
- Diagnostics are visible when expanded.
- Debug information box renders for the missing-controller-state case.
- Action icons/labels are visible.
- `Open commissioning window` invokes the existing action.
- `Details` opens/selects the details action.
- `Forget from this phone` invokes the existing forget action.

### Device Details Compose Tests

- Product image is not rendered.
- Header shows product name and `Not yet added to openHAB`.
- Details rows render in the expected order.
- Default rows use staged product/vendor/node values and `Unknown` for unfetched fields.
- IPv6 address row is visible when data is available.
- Fetch button is visible with a cloud/download icon or equivalent.
- Fetched-chip preview section is not rendered.
- Tapping a field row copies the correct value.
- Fetch success updates the main details card.
- Fetch failure keeps existing values and shows `Could not fetch data from device`.
- `Open commissioning window` and `Forget from this phone` actions still dispatch existing behavior.

### Unit Tests

- `PhoneMatterDevice` fallbacks and node formatting.
- Details formatter fallbacks.
- Battery half-percent conversion.
- Battery type formatting with and without quantity/designation.
- IPv6 byte-array formatting.
- Preferred IPv6 address selection.
- Thread network display.
- OTA update display.
- Partial fetch merge preserves previous values when new values are missing.

### Manual Verification

- Small Android phone layout does not clip action labels or long values.
- Commissioning-window retry still reaches the existing OpenCommissioningWindow path.
- Forget still clears only this app's staged state.
- Details fetch succeeds partially when only some clusters are available.
- Details fetch failure keeps local values visible.
