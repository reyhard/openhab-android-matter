---
name: openhab-android-matter-adb-debugging
description: Use when debugging Android phone behavior, app crashes, commissioning stalls, logcat output, or verifying node/IP/OCW state from ADB logs in openhab-android-matter.
---

# openHAB Android Matter ADB Debugging

Use this skill when the phone is the source of truth: app crash, stalled commissioning, missing UI progress, suspected connectedhomeip failure, or a request to verify node/IP/OCW state from ADB logs.

## Rules

- Start by checking attached devices. Do not assume a serial.
- Use explicit `-s <serial>` for all ADB commands once the target is known.
- Package name: `org.openhab.matter.companion`.
- Do not claim commissioning or OCW succeeded until logs show the relevant success callback or openHAB confirmation.
- Redact REST tokens, Thread datasets, setup PINs, fabric material, and full setup codes in permanent docs. Short-lived debugging summaries may include them only when needed for the active issue.

## Commands

```powershell
$adb='D:\Tools\Android\SDK\platform-tools\adb.exe'
& $adb devices
& $adb -s <serial> logcat -c
& $adb -s <serial> shell pidof org.openhab.matter.companion
& $adb -s <serial> logcat --pid <pid>
```

Useful broad filter:

```powershell
& $adb -s <serial> logcat -v time | Select-String -Pattern 'org.openhab.matter.companion|CHIP|CTL|DMG|BLE|DIS|AndroidRuntime|FATAL EXCEPTION'
```

If the app crashed:

```powershell
& $adb -s <serial> logcat -d -v time | Select-String -Pattern 'FATAL EXCEPTION|AndroidRuntime|CheckJNI|org.openhab.matter.companion|chip.devicecontroller'
```

## What To Extract

Look for:

- Current commissioned node id, usually logged as a hex value.
- Operational endpoint, typically `UDP:[<ipv6>]:5540`.
- BLE scan start, discriminator match, GATT connect, MTU, and retryable GATT failures.
- Completion listener success/failure for Thread commissioning.
- Device pointer acquisition for the node id.
- `OpenCommissioningWindow` start and callback result.
- Manual pairing code and QR code returned by connectedhomeip.
- CHIP status/error values and the stage where failure occurred.

## Interpretation

- UI silence can be a progress-reporting bug, not a commissioning failure. Check whether CHIP logs continue moving.
- Operational discovery failures after BLE success usually point to IPv6, Thread routing, mDNS, or fabric restore state.
- A CheckJNI crash around `OpenCommissioningCallback` suggests the concrete callback bridge is missing or incompatible.
- If phone logs show a current IPv6 address but openHAB/Avahi shows a different address, treat discovery/cache state as suspect before changing app logic.

