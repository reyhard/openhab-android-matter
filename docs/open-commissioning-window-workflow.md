# Open Commissioning Window Workflow

This documents the current Android app flow for opening a Matter commissioning window on a device that the phone has already commissioned as the bootstrap controller.

## Summary

The app no longer opens an OpenCommissioningWindow through the simulated controller. The user must first run Thread commissioning successfully so the app has a bootstrap Matter node id and connectedhomeip controller state. When the user taps **Open commissioning window** in the legacy UI, the app reloads the persisted bootstrap state, selects the connectedhomeip Java controller only if packaged artifacts and runtime preflight are ready, asks connectedhomeip for a connected-device pointer, invokes `openPairingWindowWithPINCallback`, waits for the callback, then displays the temporary manual setup code and QR code when one is returned. When an openHAB base URL is configured, the app automatically posts the returned manual setup code to openHAB Matter discovery scan input and then observes the openHAB Inbox for a Matter entry. If no openHAB URL is configured, the app keeps the manual Scan Input instructions.

The Compose automated setup flow treats OpenCommissioningWindow as an internal step after QR scan, pairing-mode confirmation, and BLE Thread commissioning to the phone. When connectedhomeip returns the manual setup code, the app submits that code to openHAB Matter discovery scan input, starts watching the openHAB Inbox, and reports v1 success only when a Matter Inbox entry is detected. The temporary 300-second pairing window is shown as a countdown in the user-facing progress UI.

Important current parameters:

| Parameter | Current value | Where set |
| --- | ---: | --- |
| Window timeout | `300` seconds | `MainActivity.runOpenCommissioningWindow()`, `MatterSetupViewModel` |
| Discriminator | `3840` | `MainActivity.runOpenCommissioningWindow()`, `MatterSetupViewModel` |
| Enhanced commissioning iteration | `1000` | `ConnectedHomeIpMatterController` |
| Setup PIN passed to CHIP API | `null` | `ConnectedHomeIpReflectionGateway` |
| Device-pointer wait timeout | `300_000` ms | `ConnectedHomeIpMatterControllerFactory` |
| OCW callback wait timeout | `300_000` ms | `ConnectedHomeIpReflectionGateway` |

## Compose Automated Setup Flow

```mermaid
flowchart TD
    A[Open app] --> B{openHAB configured?}
    B -- No --> B1[Enter openHAB URL/token and run readiness check]
    B1 --> B2{openHAB ready?}
    B2 -- No --> B3[Show sanitized failure and troubleshooting]
    B2 -- Yes --> C[Scan Matter QR code]
    B -- Yes --> C
    C --> D[Confirm device is in pairing mode]
    D --> E[Check openHAB readiness]
    E --> F{Ready?}
    F -- No --> F1[Show failure diagnostics]
    F -- Yes --> G[Commission device to phone over BLE Thread]
    G --> H[Open CommissioningWindow through connectedhomeip]
    H --> I[Show 300-second countdown]
    I --> J[Send returned manual code to openHAB discovery scan]
    J --> K[Poll openHAB Inbox]
    K --> L{Matter Inbox entry detected?}
    L -- Yes --> M[Show success]
    L -- No --> N[Show troubleshooting guidance]
```

The Compose path is owned by `MatterSetupViewModel`. It uses `MatterSetupWorkflow` and `AndroidMatterSetupPorts` to keep connectedhomeip, REST, storage, and diagnostics outside Compose UI code. `WorkflowExecutionGate` prevents duplicate workflow starts from repeated taps, and the ViewModel suppresses state emissions after it is cleared.

## High-Level Flow

```mermaid
flowchart TD
    A[User taps Open commissioning window] --> B[Load persisted Matter bootstrap state]
    B --> C{Bootstrap state unreadable?}
    C -- Yes --> C1[Fail closed, clear in-memory node id, show re-run commissioning message]
    C -- No --> D[Resolve bootstrap node id]
    D --> E{Node id available?}
    E -- No --> E1[Clear persisted bootstrap state and ask user to run Thread commissioning first]
    E -- Yes --> F[Log target node id]
    F --> G[Select native controller if ready]
    G --> H{connectedhomeip selected?}
    H -- No --> H1[Stop; do not use fake controller]
    H -- Yes --> I[Call MatterController.openCommissioningWindow]
    I --> J[Acquire connected-device pointer for node id]
    J --> K[Invoke openPairingWindowWithPINCallback]
    K --> L{Command started?}
    L -- No --> L1[Release pointer and report failure]
    L -- Yes --> M[Wait for OpenCommissioningCallback]
    M --> N{Callback result}
    N -- onSuccess --> O[Save returned controller state]
    O --> P[Show temporary manual code and QR if returned]
    P --> S{openHAB URL configured?}
    S -- No --> Q[Show openHAB Scan Input instructions]
    S -- Yes --> T[POST Matter discovery scan input with manual code]
    T --> U[Observe openHAB Inbox SSE or poll Inbox]
    U --> V{Matter Inbox entry detected?}
    V -- Yes --> W[Report openHAB Matter Inbox entry detected]
    V -- No --> X[Report scan started but no Inbox entry yet]
    N -- onError/timeout/blank codes --> N1[Report operation failure]
    N1 --> R[Release pointer]
    Q --> R
    W --> R
    X --> R
```

## App/UI Sequence

```mermaid
sequenceDiagram
    actor User
    participant Activity as MainActivity
    participant Store as MatterBootstrapStateRepository
    participant Session as NativeChipControllerSession
    participant Selector as MatterControllerSelector
    participant Controller as ConnectedHomeIpMatterController
    participant UI as App log/QR display

    User->>Activity: Tap "Open commissioning window"
    Activity->>Store: load()
    Store-->>Activity: MatterBootstrapState
    alt encrypted bootstrap state unreadable
        Activity->>UI: Show bootstrap-state-unreadable message
    else no bootstrap node id
        Activity->>Store: clear()
        Activity->>UI: Ask user to run Thread commissioning first
    else bootstrap node id available
        Activity->>UI: Log target node id
        Activity->>Session: selectNativeIfReady()
        Session->>Selector: select(fallback, connectedhomeip candidate, preferNative=true)
        Selector-->>Session: MatterControllerSelection
        Session-->>Activity: selection
        Activity->>UI: Log controller selection
        alt native controller not selected
            Activity->>UI: Stop after readiness/preflight message
        else native controller selected
            Activity->>Controller: openCommissioningWindow(nodeId, 300, 3840, controllerState)
            Controller-->>Activity: MatterOpenCommissioningWindowResult
            Activity->>Store: save(nodeId, result.controllerState)
            Activity->>UI: Render temporary QR/manual code
            alt openHAB URL configured
                Activity->>UI: Start Matter discovery scan input and observe Inbox
            else no openHAB URL configured
                Activity->>UI: Show openHAB Scan Input instructions
            end
        end
    end
```

## connectedhomeip Command Sequence

```mermaid
sequenceDiagram
    participant C as ConnectedHomeIpMatterController
    participant G as ConnectedHomeIpReflectionGateway
    participant Provider as ConnectedHomeIpPlatformControllerProvider
    participant Ptr as ConnectedHomeIpReflectionDevicePointerProvider
    participant Factory as ConnectedHomeIpReflectionCommandFactory
    participant CHIP as ChipDeviceController
    participant CB as OpenCommissioningCallback bridge

    C->>C: requireArtifactsReady()
    C->>G: openCommissioningWindow(request)
    G->>Provider: controller()
    Provider-->>G: ChipDeviceController
    G->>Ptr: acquire(controller, nodeId)
    Ptr->>Factory: newGetConnectedDeviceCallback(callback)
    Ptr->>Factory: invokeGetConnectedDevicePointer(controller, nodeId, callback)
    Factory->>CHIP: getConnectedDevicePointer(nodeId, callback)
    CHIP-->>Ptr: onDeviceConnected(devicePtr)
    Ptr-->>G: ConnectedHomeIpDevicePointer
    G->>Factory: newOpenCommissioningWindowCallback(controllerState)
    Factory-->>G: concrete callback or proxy callback
    G->>Factory: invokeOpenPairingWindowWithPinCallback(controller, devicePtr, request, null, callback)
    Factory->>CHIP: openPairingWindowWithPINCallback(devicePtr, 300, 1000, 3840, null, callback)
    CHIP-->>Factory: boolean started
    alt started
        CHIP-->>CB: onSuccess(deviceId, manualPairingCode, qrCode)
        CB-->>G: MatterOpenCommissioningWindowResult
        G-->>C: result
    else not started
        G-->>C: IllegalStateException
    end
    G->>Ptr: close()
    Ptr->>Factory: invokeReleaseConnectedDevicePointer(controller, devicePtr)
    Factory->>CHIP: releaseConnectedDevicePointer(devicePtr)
```

## Readiness Gate

```mermaid
flowchart TD
    A[selectNativeIfReady] --> B[Create or reuse connectedhomeip candidate]
    B --> C[Check packaged connectedhomeip artifacts]
    C --> D{Artifacts ready?}
    D -- No --> D1[Return fallback controller with nativeSelected=false]
    D -- Yes --> E[Runtime preflight]
    E --> F[Build/load ChipDeviceController and AndroidChipPlatform]
    F --> G[Check BLE provider runtime readiness]
    G --> H{Runtime ready?}
    H -- No --> H1[Return fallback controller with nativeSelected=false]
    H -- Yes --> I[Return connectedhomeip controller with nativeSelected=true]
```

The calling UI checks `nativeSelected()`. If it is false, the OCW workflow stops after logging the selection message. This is intentional: OpenCommissioningWindow does not silently fall back to `FakeMatterController`.

## Failure Paths

```mermaid
flowchart TD
    A[Open commissioning window] --> B{Bootstrap state readable?}
    B -- No --> B1[Fail closed; user must re-run Thread commissioning]
    B -- Yes --> C{Bootstrap node id exists?}
    C -- No --> C1[Clear bootstrap state; user must run Thread commissioning]
    C -- Yes --> D{Native controller selected?}
    D -- No --> D1[Stop with connectedhomeip readiness/preflight message]
    D -- Yes --> E{Connected-device pointer acquired?}
    E -- No --> E1[Operation failure: pointer callback failure or timeout]
    E -- Yes --> F{openPairingWindowWithPINCallback returns true?}
    F -- No --> F1[Operation failure: command did not start]
    F -- Yes --> G{Callback completes?}
    G -- Timeout --> G1[Operation failure: callback timed out]
    G -- onError --> G2[Operation failure: CHIP status reported]
    G -- onSuccess blank manual and QR --> G3[Operation failure: blank setup codes]
    G -- onSuccess code returned --> H[Save state and display code/QR]
```

- In the Compose automated flow, if the openHAB scan starts but no Inbox entry is detected before timeout, the app shows recovery guidance for IPv6 routing, OTBR reachability, mDNS/Avahi visibility, stale Matter records, and retrying setup to request a fresh commissioning window.
- The Compose advanced troubleshooting screen is guidance-only for OCW retry and forget-from-phone cleanup today. It does not expose one-tap OCW retry or device removal until those actions are wired to real operations.

## Source Map

| Area | Main classes |
| --- | --- |
| UI entry point and result display | `MainActivity.runOpenCommissioningWindow()`, `MainActivity.showTemporaryQrCode()` |
| Compose automated setup entry point | `MatterSetupActivity`, `MatterSetupViewModel`, `MatterSetupApp` |
| Compose workflow state and ports | `MatterSetupWorkflow`, `MatterSetupStateReducer`, `WorkflowExecutionGate`, `AndroidMatterSetupPorts` |
| Bootstrap state resolution | `MatterBootstrapStateRepository`, `MatterBootstrapStateResolver`, `MatterBootstrapState` |
| Native-controller gate | `NativeChipControllerSession`, `MatterControllerSelector`, `ConnectedHomeIpMatterControllerFactory` |
| connectedhomeip controller facade | `ConnectedHomeIpMatterController` |
| CHIP command orchestration | `ConnectedHomeIpReflectionGateway` |
| Connected-device pointer lifecycle | `ConnectedHomeIpReflectionDevicePointerProvider`, `ConnectedHomeIpConnectedDeviceCallback`, `ConnectedHomeIpDevicePointer` |
| Reflected CHIP APIs | `ConnectedHomeIpReflectionCommandFactory` |
| OCW callback/result mapping | `ConnectedHomeIpOpenCommissioningWindowCallback`, `MatterOpenCommissioningWindowResult` |
| openHAB scan handoff and instructions | `OpenHabMatterDiscoveryClient`, `HttpOpenHabMatterDiscoveryClient`, `OpenHabInstructions` |
