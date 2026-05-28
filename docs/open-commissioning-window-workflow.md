# Open Commissioning Window Workflow

This documents the current Android app flow for opening a Matter commissioning window on a device that the phone has already commissioned as the bootstrap controller.

## Summary

The app no longer opens an OpenCommissioningWindow through the simulated controller. The user must first run Thread commissioning successfully so the app has a bootstrap Matter node id and connectedhomeip controller state. When the user taps **Open commissioning window**, the app reloads the persisted bootstrap state, selects the connectedhomeip Java controller only if packaged artifacts and runtime preflight are ready, asks connectedhomeip for a connected-device pointer, invokes `openPairingWindowWithPINCallback`, waits for the callback, then displays the temporary manual setup code and QR code when one is returned.

Important current parameters:

| Parameter | Current value | Where set |
| --- | ---: | --- |
| Window timeout | `300` seconds | `MainActivity.runOpenCommissioningWindow()` |
| Discriminator | `3840` | `MainActivity.runOpenCommissioningWindow()` |
| Enhanced commissioning iteration | `1000` | `ConnectedHomeIpMatterController` |
| Setup PIN passed to CHIP API | `null` | `ConnectedHomeIpReflectionGateway` |
| Device-pointer wait timeout | `300_000` ms | `ConnectedHomeIpMatterControllerFactory` |
| OCW callback wait timeout | `300_000` ms | `ConnectedHomeIpReflectionGateway` |

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
    P --> Q[Show openHAB Scan Input instructions]
    N -- onError/timeout/blank codes --> N1[Report operation failure]
    N1 --> R[Release pointer]
    Q --> R
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
            Activity->>UI: Show openHAB Scan Input instructions
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

## Source Map

| Area | Main classes |
| --- | --- |
| UI entry point and result display | `MainActivity.runOpenCommissioningWindow()`, `MainActivity.showTemporaryQrCode()` |
| Bootstrap state resolution | `MatterBootstrapStateRepository`, `MatterBootstrapStateResolver`, `MatterBootstrapState` |
| Native-controller gate | `NativeChipControllerSession`, `MatterControllerSelector`, `ConnectedHomeIpMatterControllerFactory` |
| connectedhomeip controller facade | `ConnectedHomeIpMatterController` |
| CHIP command orchestration | `ConnectedHomeIpReflectionGateway` |
| Connected-device pointer lifecycle | `ConnectedHomeIpReflectionDevicePointerProvider`, `ConnectedHomeIpConnectedDeviceCallback`, `ConnectedHomeIpDevicePointer` |
| Reflected CHIP APIs | `ConnectedHomeIpReflectionCommandFactory` |
| OCW callback/result mapping | `ConnectedHomeIpOpenCommissioningWindowCallback`, `MatterOpenCommissioningWindowResult` |
| openHAB user instructions | `OpenHabInstructions` |
