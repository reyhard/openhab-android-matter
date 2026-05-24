Android Companion App for openHAB Matter Pairing - Research Report
(Updated with "BLE-on-phone Thread provisioning + openHAB UI code entry" flow)
=========================================================================

## Executive Summary

An Android companion app that helps non-technical users commission Matter devices (Thread and Wi-Fi) and attach them to openHAB is technically feasible, with a particularly strong path where the phone performs BLE + Thread provisioning and opens a commissioning window, and the user then types the temporary code into the existing openHAB Matter UI.

Key updates compared to the initial plan:

- For Matter-over-Thread, the recommended flow is now: Android app embeds a minimal Matter controller (CHIP) to perform `ble-thread`-style commissioning using the Thread dataset from your OTBR, then calls the `OpenCommissioningWindow` command to generate a secondary pairing code that the user enters into openHAB's existing Matter "Scan Input" dialog.
- openHAB's Matter binding and UI remain unchanged for pairing; they simply act as the second administrator (multi-admin) using the code produced by the phone, mirroring what you currently do manually with `chip-tool`.

The primary recommended architecture is still Option C, but extended with a concrete pattern:

- Wi-Fi + multi-admin: Android acts as a commissioning assistant and code scanner, handing setup codes or multi-admin codes to openHAB over REST or via user copy-paste.
- Thread: Android acts as a temporary commissioning controller (BLE + Thread dataset + OpenCommissioningWindow), then the user finishes pairing in openHAB UI using the generated code.

Biggest blockers remain: Thread credential management and OTBR setup, Google/Apple Thread network lock-in, and the effort of embedding and maintaining the Matter controller stack on Android, though this is constrained to a small command surface (BLE Thread commissioning + OCW).

## Feasibility Assessment

The updated architecture keeps openHAB as the steady-state controller but moves the painful, proximity-dependent BLE work from a Raspberry Pi + `chip-tool` to the phone.

- Matter over Wi-Fi: Direct pairing to openHAB via setup codes (with or without Play Services Commissioning API) remains feasible if IPv6 is correctly configured; the Android app can either send setup codes to openHAB via REST or leave the final code entry to the user.
- Matter over Thread:
  - Your current successful Aqara U200 flow (OTBR on ESP32-C6 + RCP + `chip-tool pairing ble-thread` + `open-commissioning-window` + manual code entry in openHAB) is already a valid pattern and proven to work.
  - Porting that logic to Android using the Matter controller library is technically feasible (Nordic/NXP "chip-tool for Android" variants show similar functionality), though it requires JNI and careful handling of attestation and network access.
- openHAB:
  - The Matter binding supports pairing via temporary setup codes entered in the existing "Scan Input" field; that path does not require new REST APIs and can stay as-is.
  - IPv6, mDNS and OTBR networking remain prerequisites; your notes on Avahi, IPv6 RA settings, and RCP quirks remain relevant and can be turned into app-side guidance.

Therefore, the updated plan is feasible with medium-high engineering effort on the Android side and moderate changes (if any) on openHAB's side.

## Current State of Android Matter APIs

This section remains largely as before; the main addition is the clear distinction between two ways of doing BLE + Thread on Android:

- Using Google's Commissioning API / Home Mobile SDK, which is optimized for commissioning into the Google fabric and multi-admin sharing.
- Using an embedded Matter controller library (CHIP) to implement `chip-tool`-equivalent commands (`ble-thread`, `open-commissioning-window`) locally on the device, independent of Google's fabric.

Given your goal (attach to openHAB, not necessarily to Google), the updated plan uses:

- Play Services Commissioning API mostly as an optional helper for Wi-Fi multi-admin flows.
- An embedded controller (CHIP) as the primary engine for the Thread path, with code surface limited to:
  - Setup payload parsing.
  - BLE scanning, PASE, and attestation.
  - Provisioning Thread Active Operational Dataset.
  - Opening a commissioning window (OCW) and retrieving a secondary setup code.

All previous details about versions, permissions (Nearby devices, Wi-Fi, Bluetooth), and Play Services dependencies remain valid.

## Current State of openHAB Matter Support

No fundamental change here, but the updated plan leans into existing UI flows:

- The Matter binding supports adding devices by entering a setup code via the "Scan Input" field in the "Add a new Thing: matter" dialog; this is exactly how you added the Aqara lock after running `open-commissioning-window` on the Pi.
- The binding already expects multi-admin flows, where another controller (`chip-tool`, vendor app, other ecosystem) opens a commissioning window and openHAB is added as a secondary controller.

The main design choice is not to modify openHAB for this variant:

- You keep the full manual pairing flow as-is and only change how the temporary code is generated (phone instead of Pi/`chip-tool`).

## Matter over Wi-Fi Commissioning Flow

This section is unchanged in essence:

- Commissioning over Wi-Fi still follows the standard PASE over BLE -> send SSID/password -> device joins Wi-Fi -> controller uses CASE over IPv6.
- Android may use Play Services Commissioning API, an embedded controller, or vendor apps to bring the device onto Wi-Fi.

The openHAB-focused flows remain:

- Android scans the code and either:
  - Calls an openHAB REST endpoint to start pairing with that code (Option C original).
  - Or simply displays the code and tells user to paste it into openHAB's Scan Input (same pattern as Thread multi-admin).

## Matter over Thread Commissioning Flow

Here we integrate your desired "BLE on phone + existing openHAB UI" pattern explicitly.

### 6.1 Standard spec flow

As before, Matter over Thread commissioning is:

- BLE advertisement -> PASE over BLE using setup PIN -> attestation -> Thread Active Operational Dataset provisioning -> device joins Thread via OTBR -> CASE over IP.

### 6.2 Your current chip-tool + OTBR flow (baseline)

You already implemented:

- Set up OTBR (ESP32-C6) + RCP (ESP32-H2) and obtain Thread Active Operational Dataset (`dataset active -x`).
- Run on Raspberry Pi:
  - `chip-tool payload parse-setup-payload ...` to get PIN + long discriminator.
  - `chip-tool pairing ble-thread <NODE_ID> hex:<DATASET_HEX> <PIN> <DISCRIMINATOR>` to commission the lock to Thread network (with potential attestation bypass).
  - `chip-tool pairing open-commissioning-window <NODE_ID> 1 300 1000 <DISCRIMINATOR>` to open commissioning window and output a temporary setup code + QR.
- In openHAB UI, enter that temporary code in Matter binding Scan Input -> device appears in Inbox -> add Thing.

### 6.3 Updated Android flow: "Phone = chip-tool, openHAB UI unchanged"

On Android, the plan is:

- Thread dataset:
  - App stores the Active Operational Dataset (`hex:<DATASET_HEX>`) once, copied from OTBR or fetched via a simple local service.
- Setup payload parsing:
  - App scans the device QR, uses the embedded CHIP stack's payload parser (or own code) to extract:
    - 8-digit PIN.
    - Long discriminator (handles your note that 11-digit manual code is not enough).
- BLE + Thread commissioning (to first fabric):
  - Using the CHIP Android controller library, run the equivalent of `pairing ble-thread`:
    - BLE scan filtered by discriminator.
    - PASE using PIN.
    - Attestation with either a proper PAA store or a "developer mode / bypass attestation" flag matching `--bypass-attestation-verifier true`.
    - Provision Thread Active Operational Dataset to join OTBR network.
- Open commissioning window (multi-admin):
  - Invoke `OpenCommissioningWindow` on that node, with parameters analogous to `open-commissioning-window` (`1 300 1000 <discriminator>`).
  - Read the returned temporary setup code/QR content.
- User copies code to openHAB:
  - App shows the temporary code and optionally a QR generated from the content.
  - User goes to openHAB UI -> Matter binding -> enters that code in Scan Input and completes pairing exactly as today.

In other words, the phone entirely replaces `chip-tool` while openHAB's binding behavior stays identical.

## Home Assistant Architecture and Lessons

No major changes, but there is a strong analogy now:

- Your Android app is effectively a custom mobile Matter controller in the same spirit as Nordic's sample "CHIP Tool for Android" app that commissions Wi-Fi/Thread devices using an embedded controller stack.
- Home Assistant still provides the best reference for:
  - Server-side Matter controller (Matter Server) + mobile commissioning UX.
  - Thread integration and OTBR management, showing how credential management might evolve later on openHAB's side.

## Other Ecosystem Comparison

The comparison table stands; the updated insight is:

- Your flow is closest to "CHIP Tool + openHAB" but with CHIP Tool moved onto Android.
- This mirrors ecosystems where:
  - Mobile app is the commissioner (SmartThings, Google Home, Apple Home).
  - The hub/server (openHAB) joins later via multi-admin using a code the mobile app generates.

## Recommended Architecture

### Option C (Updated) - Android commissioning assistant + mini controller for Thread

This is now the primary recommendation, with two sub-paths:

- C1 (Wi-Fi and generic multi-admin):
  - Android app:
    - Scan QR/manual codes.
    - Either send codes to openHAB via REST (`/rest/matter/pair`) or show them for manual entry in openHAB UI.
  - openHAB:
    - Matter binding controller does IP-level commissioning (Wi-Fi already provisioned).
- C2 (Thread, BLE on phone):
  - Android app:
    - Embed CHIP controller.
    - Use stored OTBR Thread dataset to perform BLE->Thread commissioning (equivalent to `pairing ble-thread`).
    - Call `OpenCommissioningWindow` to generate secondary pairing code.
    - Display that code to the user.
  - openHAB:
    - User types that code into Matter binding Scan Input; binding adds device to its fabric as secondary controller.

This option:

- Minimizes openHAB changes (can be done without any binding modifications).
- Puts BLE/Thread proximity operations exactly where they belong (on the phone, next to the lock).
- Matches your already proven manual path, keeping troubleshooting reasoning intact.

## Sequence Diagrams

### 10.1 Matter over Wi-Fi pairing to openHAB (Option C1, unchanged)

Same as in the original report: Android scans code and either sends it to openHAB via REST or shows it for manual entry; openHAB Matter controller completes commissioning and creates an Inbox entry; app optionally observes via SSE.

### 10.2 Matter over Thread pairing with BLE on phone (Option C2)

Actors: User, Android App, OTBR, Thread Network, Matter Device, openHAB UI, openHAB Matter Controller

- Preconditions: OTBR + RCP configured and running; Thread dataset obtained once (`dataset active -x`) and stored in Android app.
- User opens Android app -> "Add Thread Matter device."
- App asks user to put device into pairing mode; user presses lock's Set button.
- App scans device's QR, parses setup payload to get PIN + long discriminator.
- App's embedded CHIP controller performs BLE->Thread commissioning:
  - BLE scan filtered by discriminator.
  - PASE using PIN; attestation with PAA store or "bypass" flag.
  - Provision Thread dataset to join OTBR's Thread network.
- Device joins Thread network and becomes reachable over IPv6 via OTBR.
- App invokes `OpenCommissioningWindow` cluster command on the device (first fabric), specifying timeout and discriminator.
- Controller stack returns a new temporary setup code/QR representing a new commissioning window.
- App displays: "Temporary Matter pairing code: XXXX-XXXX-XXX. Open openHAB -> Matter -> Scan and enter this code."
- User goes to openHAB Main UI -> Things -> + -> Matter -> enters code in Scan Input and starts scan.
- openHAB Matter controller uses this code to add itself as secondary admin; device appears in Inbox; user adds it as a Thing.

### 10.3 Multi-admin from other ecosystems (unchanged)

Still: user requests "share / add another app" in Google/Apple/SmartThings -> obtains code/QR -> Android app helps capture and forward to openHAB, or user types it directly into openHAB UI.

## Android App Design

Updated to include an embedded Matter controller module.

### 11.1 Modules

- `core:matter-setup` - QR parsing, payload models, UI utilities.
- `core:matter-controller` - JNI wrapper and Kotlin layer around CHIP Controller:
  - `commissionBleThread(datasetHex, pin, discriminator)`
  - `openCommissioningWindow(nodeId, timeout, discriminator)`
  - Built based on `connectedhomeip` and examples from Nordic/NXP Android chip-tool.
- `core:openhab` - REST client, SSE.
- `feature:thread-commission` - UI flow for Thread devices.
- `feature:wifi-commission` - UI flow for Wi-Fi/Multi-admin (simpler).

### 11.2 Key screens (Thread path)

- Thread Dataset Setup:
  - Text field to paste Thread dataset hex string; "Test connectivity" button that attempts to reach OTBR and maybe validates dataset format.
- Scan Device Code:
  - CameraX QR scanner; fallback manual code entry; parsing shows PIN, discriminator, vendor/product.
- BLE + Thread Commissioning:
  - Progress UI with logs:
    - "Scanning via BLE for device with discriminator X"
    - "PASE session established"
    - "Attestation: OK / bypassed"
    - "Provisioning Thread dataset"
    - "Joined Thread network successfully"
- Open Commissioning Window:
  - Button: "Open commissioning window for openHAB"
  - Progress step; on success, display code and QR.
- Help screen with your troubleshooting notes:
  - Avahi/mDNS requirements.
  - IPv6 `accept_ra_rt_info_max_plen` hack.
  - Reminder that if device doesn't appear in openHAB Inbox, openHAB's IPv6 or Matter binding may still be misconfigured.

## openHAB Integration Design

For this variant, there's no hard dependency on new openHAB APIs. The integration plan stays:

- Encourage openHAB users to ensure:
  - Matter binding installed and controller Thing online.
  - IPv6 and mDNS working on openHAB host (Avahi, RA fixes, router supporting mDNS across segments).
- Optionally add soft integration:
  - App can connect to openHAB REST to check Matter controller state and network config, then warn early if things look broken.
  - App can poll Inbox or listen to SSE after the user enters the temporary code, so it can show "openHAB has detected this device" confirmation.

The more REST-driven "Android sends setup code directly to openHAB" path from the original report can still be implemented in parallel for Wi-Fi and vendor-commissioned devices.

## Security and Privacy

The only major change is that the phone now temporarily holds a Matter fabric for the first commissioning step (Thread path):

- The embedded controller maintains the initial fabric keys and credentials; it should:
  - Keep them in secure storage (if you need to reconnect), or
  - Treat the phone's fabric as ephemeral: after OCW and multi-admin to openHAB, you can choose to never interact with that device again from the phone.

Given you only need the phone as a bootstrap commissioner and not as a long-term controller, a safe pattern is:

- Use a fixed bootstrap fabric on the phone, and only ever use it to:
  - Join devices to Thread + open a commissioning window.
  - Let openHAB add itself as persistent controller.

No device control or state storage happens on the phone; openHAB remains the canonical controller.

## MVP Scope

Revised MVP:

- Must-have:
  - Thread dataset configuration screen (paste hex) and basic OTBR connectivity check.
  - QR/manual code parsing for Matter setup payload (PIN + long discriminator).
  - Embedded controller implementation of `pairing ble-thread` equivalent from Android, using BLE to commission one known Thread lock to your OTBR.
  - Embedded controller implementation of `open-commissioning-window` equivalent, returning a temporary setup code.
  - Simple UI that displays the temporary code and instructs user to enter it in openHAB -> Matter -> Scan Input.
- Nice-to-have:
  - openHAB REST health check and "Did your lock appear in Inbox?" SSE integration.
  - Wi-Fi flow and generic multi-admin flows from other ecosystems.

## Codex Execution Plan

Updates to the earlier phase plan to reflect the embedded controller and BLE-Thread/OCW steps.

### Phase 4 (revised) - Android Matter commissioning experiment (Controller on Android)

- Goal: Get a minimal CHIP controller running on Android that can talk to a Thread device via BLE and Thread dataset.
- Tasks:
  - Build or integrate the CHIP Android controller library, using Nordic/NXP sample chip-tool for Android as reference.
  - Implement a simple console-like test screen that:
    - Reads a hard-coded Thread dataset hex string.
    - Asks for PIN and discriminator.
    - Calls `commissionBleThread(dataset, pin, discriminator)` and shows raw log output.
- Acceptance: For one known lock, you can replicate `chip-tool pairing ble-thread` behavior from Android and see the device join your OTBR network (OTBR CLI shows it as a child).

### Phase 5 (revised) - OpenCommissioningWindow from Android

- Goal: Replicate `chip-tool pairing open-commissioning-window` from Android.
- Tasks:
  - Add a controller function `openCommissioningWindow(nodeId, timeout, discriminator)` that sends the OCW command to the device you just commissioned.
  - Parse the OCW response to get temporary setup code/QR payload.
  - Display the code.
- Acceptance: When you run OCW from Android and then manually enter the code in openHAB's Scan Input, the lock appears in the openHAB Inbox as it used to when you ran OCW from the Pi.

Other phases (0-3, 6-10) remain similar to the original report, but now emphasize that for Thread devices the commissioning engine is on the phone.

## Suggested Codex Prompts

Additions specific to your updated flow:

- "Using the `chip_tool_guide` docs, implement an Android JNI wrapper for the CHIP controller functions needed to replicate `pairing ble-thread` and `open-commissioning-window` in Kotlin."
- "Given this Thread dataset hex string and Matter setup payload, write Kotlin code that uses the CHIP controller to commission a device over BLE to the Thread network from Android."
- "Implement a Compose screen that runs `openCommissioningWindow` on a given `nodeId` and then displays the temporary setup code and QR for the user to type into openHAB."

## Risks, Blockers, and Unknowns

Updated key risks:

- Embedding CHIP on Android:
  - Maintaining a custom JNI build of `connectedhomeip` is non-trivial; Nordic/NXP and others show it's possible, but you must track Matter spec and security updates.
- Attestation handling:
  - Production-grade flows should not rely on `--bypass-attestation-verifier`; on Android you'll need to either ship a PAA trust store or keep a developer mode toggle, similar to your current workaround.
- Thread/OTBR brittleness:
  - All earlier IPv6/mDNS/RA pitfalls remain; your app can help surface them (e.g., test mDNS to lock, test IPv6), but it cannot fully hide them.
- openHAB UI coupling:
  - You rely on openHAB continuing to support manual entry of Matter codes in the Matter binding UI; if this changes in future versions, you may need to add a REST integration path.

## Reference Library

Same library as before, with special emphasis on:

- CHIP Tool docs and OCW examples.
- OTBR dataset handling (`dataset active -x`).
- openHAB Matter binding README and forum examples showing code entry in Scan Input.
- Nordic/NXP Android chip-tool references.

This updated report keeps the original structure but folds in your desired BLE-on-phone + keep openHAB UI as-is Thread flow: Android acts as a portable chip-tool (BLE + Thread + OCW), and openHAB continues to be the place where you type the temporary code and manage the Thing.
