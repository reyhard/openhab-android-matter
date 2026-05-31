# Matter Handoff Flow Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the redesigned Matter handoff screens: diagnostic **Devices on this phone**, copyable **Device details**, and explicit richer Matter metadata fetch.

**Architecture:** Keep Compose as a pure UI layer. The ViewModel owns navigation, selected device state, fetch state, and action dispatch. Matter cluster reads stay behind the controller/gateway metadata seam, returning partial optional details that are merged into UI rows with stable fallbacks.

**Tech Stack:** Android, Kotlin, Java, Jetpack Compose Material 3, connectedhomeip reflection bridge, JUnit, Compose UI tests.

---

## File Structure

- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`
  - Add actions for details navigation and metadata fetch.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`
  - Add `PhoneDeviceDetails`.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`
  - Add fetch status/message fields for the details screen.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt`
  - Add details-state factory and system-back return behavior support.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigation.kt`
  - Return from details to the phone-device list.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/PhoneMatterDevice.kt`
  - Normalize staged-device fallbacks, diagnostics labels, selected-device detail merge, and node formatting.
- Create `app/src/main/kotlin/org/openhab/matter/companion/setup/PhoneMatterDeviceDetails.kt`
  - Hold optional fetched details and merge behavior.
- Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterDeviceDetailFormatter.kt`
  - Format battery, IPv6, Thread network, OTA, and fallback display values.
- Create `app/src/main/java/org/openhab/matter/companion/controller/MatterDeviceDetails.java`
  - Java controller-layer DTO for optional rich metadata.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java`
  - Add `readDeviceDetails(long nodeId, String controllerState, ProgressListener listener)`.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java`
  - Implement deterministic offline details for tests only.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerGateway.java`
  - Add `readDeviceDetails(long nodeId)`.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterController.java`
  - Add readiness-gated `readDeviceDetails`.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpDeviceMetadataReader.java`
  - Replace vendor/product-only method with richer details method while preserving vendor/product read use.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionDeviceMetadataReader.java`
  - Read BasicInformation strings and best-effort optional clusters using reflection callbacks.
- Modify `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGateway.java`
  - Delegate explicit metadata fetch and keep commissioning vendor/product read best-effort.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt`
  - Track selected phone device, route details/fetch actions, invoke controller selection, and show fetch/copy messages.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`
  - Route new details stage.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceListScreen.kt`
  - Replace the existing plain list with Material cards, expandable diagnostics, and icon actions.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceDetailsScreen.kt`
  - Render header, detail rows, fetch button, copy behavior, and bottom actions.
- Add vector drawables under `app/src/main/res/drawable/`
  - `ic_material_expand_more.xml`, `ic_material_expand_less.xml`, `ic_material_info.xml`, `ic_material_delete.xml`, `ic_material_open_in_new.xml`, `ic_material_cloud_download.xml`, `ic_material_label.xml`, `ic_material_store.xml`, `ic_material_inventory.xml`, `ic_material_settings.xml`, `ic_material_numbers.xml`, `ic_material_battery.xml`, `ic_material_wifi.xml`, and `ic_material_public.xml`.
- Modify `app/src/test/kotlin/org/openhab/matter/companion/setup/PhoneMatterDeviceTest.kt`
  - Cover fallbacks, node formatting, details merge.
- Create `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterDeviceDetailFormatterTest.kt`
  - Cover formatters.
- Modify controller tests under `app/src/test/java/org/openhab/matter/companion/controller/`
  - Cover explicit details fetch and fail-closed readiness.
- Modify `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`
  - Cover redesigned list and details UI.

---

### Task 1: UI Models, Actions, Stages, And Formatters

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigation.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/PhoneMatterDevice.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/PhoneMatterDeviceDetails.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterDeviceDetailFormatter.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/PhoneMatterDeviceTest.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterDeviceDetailFormatterTest.kt`

- [ ] **Step 1: Write failing formatter and model tests**

Add tests for stable fallbacks, merge behavior, and display formatting:

```kotlin
@Test
fun stagedDeviceUsesProductAndVendorFallbacksSeparately() {
    val device = PhoneMatterDevice(nodeId = 0x4D2, controllerStateStored = false, stateReadable = true)

    assertEquals("Unknown Matter device", device.displayProductName)
    assertEquals("Unknown vendor", device.displayVendorName)
    assertEquals("0x4D2", device.displayNodeId)
    assertEquals("missing", device.displayControllerState)
    assertEquals("yes", device.displayStateReadable)
}

@Test
fun fetchedDetailsMergeDoesNotClearExistingValues() {
    val original = PhoneMatterDeviceDetails(
        deviceName = "BILRESA scroll wheel",
        vendor = "IKEA of Sweden",
        product = "BILRESA scroll wheel",
        nodeId = "0x165BC267A7E344D0",
        firmwareVersion = "1.8.7"
    )
    val merged = original.merge(PhoneMatterDeviceDetails(hardwareVersion = "P2.0"))

    assertEquals("1.8.7", merged.firmwareVersion)
    assertEquals("P2.0", merged.hardwareVersion)
    assertEquals("BILRESA scroll wheel", merged.deviceName)
}

@Test
fun batteryHalfPercentFormatsWithTypeWhenPresent() {
    assertEquals("52% · 2×AAA", MatterDeviceDetailFormatter.battery(104, 2, "AAA"))
    assertEquals("52%", MatterDeviceDetailFormatter.battery(104, null, ""))
    assertEquals("Unknown", MatterDeviceDetailFormatter.battery(null, 2, "AAA"))
}

@Test
fun threadAndOtaFormattersUseStableFallbacks() {
    assertEquals("OpenThread · Channel 25", MatterDeviceDetailFormatter.threadNetwork("OpenThread", 25))
    assertEquals("OpenThread", MatterDeviceDetailFormatter.threadNetwork("OpenThread", null))
    assertEquals("Channel 25", MatterDeviceDetailFormatter.threadNetwork("", 25))
    assertEquals("Unknown", MatterDeviceDetailFormatter.threadNetwork("", null))
    assertEquals("Possible", MatterDeviceDetailFormatter.otaUpdate(true))
    assertEquals("Not available", MatterDeviceDetailFormatter.otaUpdate(false))
    assertEquals("Unknown", MatterDeviceDetailFormatter.otaUpdate(null))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "org.openhab.matter.companion.setup.PhoneMatterDeviceTest" --tests "org.openhab.matter.companion.setup.MatterDeviceDetailFormatterTest"
```

Expected: compilation fails because the new properties/classes do not exist.

- [ ] **Step 3: Implement models and formatters**

Add:

```kotlin
data class PhoneMatterDeviceDetails(
    val deviceName: String = "",
    val vendor: String = "",
    val product: String = "",
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val partNumber: String = "",
    val nodeId: String = "",
    val battery: String = "",
    val threadNetwork: String = "",
    val ipv6Address: String = "",
    val otaUpdate: String = ""
) {
    fun merge(update: PhoneMatterDeviceDetails): PhoneMatterDeviceDetails = copy(
        deviceName = update.deviceName.ifBlank { deviceName },
        vendor = update.vendor.ifBlank { vendor },
        product = update.product.ifBlank { product },
        firmwareVersion = update.firmwareVersion.ifBlank { firmwareVersion },
        hardwareVersion = update.hardwareVersion.ifBlank { hardwareVersion },
        partNumber = update.partNumber.ifBlank { partNumber },
        nodeId = update.nodeId.ifBlank { nodeId },
        battery = update.battery.ifBlank { battery },
        threadNetwork = update.threadNetwork.ifBlank { threadNetwork },
        ipv6Address = update.ipv6Address.ifBlank { ipv6Address },
        otaUpdate = update.otaUpdate.ifBlank { otaUpdate }
    )
}
```

Add formatter object with:

```kotlin
object MatterDeviceDetailFormatter {
    const val UNKNOWN = "Unknown"
    const val UNKNOWN_PRODUCT = "Unknown Matter device"
    const val UNKNOWN_VENDOR = "Unknown vendor"

    fun nodeId(nodeId: Long?): String =
        nodeId?.let { "0x${java.lang.Long.toUnsignedString(it, 16).uppercase()}" } ?: UNKNOWN

    fun display(value: String, fallback: String = UNKNOWN): String =
        value.trim().ifBlank { fallback }

    fun battery(halfPercent: Int?, quantity: Int?, designation: String): String {
        if (halfPercent == null) return UNKNOWN
        val percent = "${halfPercent / 2}%"
        val type = designation.trim()
        return if (quantity != null && quantity > 0 && type.isNotBlank()) {
            "$percent · ${quantity}×$type"
        } else {
            percent
        }
    }

    fun threadNetwork(name: String, channel: Int?): String {
        val parts = listOf(
            name.trim().takeIf { it.isNotBlank() },
            channel?.let { "Channel $it" }
        ).filterNotNull()
        return parts.joinToString(" · ").ifBlank { UNKNOWN }
    }

    fun otaUpdate(updatePossible: Boolean?): String = when (updatePossible) {
        true -> "Possible"
        false -> "Not available"
        null -> UNKNOWN
    }
}
```

Update `PhoneMatterDevice` to expose `displayProductName`, `displayVendorName`, `displayControllerState`, `displayStateReadable`, and `initialDetails()`.

Add `FetchState` fields to `MatterSetupUiState` as simple strings/booleans:

```kotlin
val phoneDeviceDetails: PhoneMatterDeviceDetails = PhoneMatterDeviceDetails(),
val phoneDeviceDetailsFetching: Boolean = false,
val phoneDeviceDetailsMessage: String = ""
```

Add actions:

```kotlin
data class ShowPhoneDeviceDetails(val nodeId: Long?) : MatterSetupAction
data object FetchPhoneDeviceDetails : MatterSetupAction
```

Add stage:

```kotlin
PhoneDeviceDetails,
```

Add reducer:

```kotlin
fun phoneDeviceDetails(device: PhoneMatterDevice, fetching: Boolean = false, message: String = "") =
    MatterSetupUiState(
        stage = MatterSetupStage.PhoneDeviceDetails,
        title = "Device details",
        message = "Helpful information for advanced setup and troubleshooting.",
        phoneDeviceDetails = device.initialDetails(),
        phoneDeviceDetailsFetching = fetching,
        phoneDeviceDetailsMessage = message,
        primaryAction = MatterSetupAction.BackToSettings,
        primaryActionLabel = "Advanced"
    )
```

Update back navigation so `PhoneDeviceDetails` returns `ShowPhoneDevices`.

- [ ] **Step 4: Run tests to verify they pass**

Run the same Gradle test command. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/setup app/src/test/kotlin/org/openhab/matter/companion/setup
git commit -m "Add phone device details UI model"
```

---

### Task 2: Controller Metadata Fetch Seam

**Files:**
- Create: `app/src/main/java/org/openhab/matter/companion/controller/MatterDeviceDetails.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/MatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/FakeMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpControllerGateway.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterController.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpDeviceMetadataReader.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGateway.java`
- Modify: `app/src/main/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionDeviceMetadataReader.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpMatterControllerTest.java`
- Test: `app/src/test/java/org/openhab/matter/companion/controller/ConnectedHomeIpReflectionGatewayTest.java`

- [ ] **Step 1: Write failing controller tests**

Add tests that verify explicit fetch delegates through the readiness-gated connectedhomeip path:

```java
@Test
public void readDeviceDetailsRequiresReadyArtifactsAndDelegatesToGateway() throws Exception {
    CapturingGateway gateway = new CapturingGateway();
    gateway.details = new MatterDeviceDetails.Builder()
            .vendorName("IKEA of Sweden")
            .productName("BILRESA scroll wheel")
            .softwareVersionString("1.8.7")
            .build();
    ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
            readyArtifacts(),
            gateway,
            false);

    MatterDeviceDetails details = controller.readDeviceDetails(0x165BC267A7E344D0L, "controller-state", ignored -> { });

    assertEquals(0x165BC267A7E344D0L, gateway.detailsNodeId);
    assertEquals("IKEA of Sweden", details.vendorName());
    assertEquals("BILRESA scroll wheel", details.productName());
    assertEquals("1.8.7", details.softwareVersionString());
}

@Test
public void readDeviceDetailsRejectsMissingArtifactsBeforeCallingGateway() {
    CapturingGateway gateway = new CapturingGateway();
    ConnectedHomeIpMatterController controller = new ConnectedHomeIpMatterController(
            missingArtifacts(),
            gateway,
            false);

    IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> controller.readDeviceDetails(1234L, "state", ignored -> { }));

    assertEquals("Missing connectedhomeip controller class: chip.devicecontroller.ChipDeviceController", error.getMessage());
    assertEquals(0, gateway.callCount);
}
```

Update `ConnectedHomeIpReflectionGatewayTest` fake metadata readers so commissioning still calls best-effort vendor/product, and add:

```java
@Test
public void readDeviceDetailsDelegatesToMetadataReader() throws Exception {
    FakeChipDeviceController controller = new FakeChipDeviceController();
    CapturingMetadataReader metadataReader = new CapturingMetadataReader(
            new MatterDeviceDetails.Builder().vendorName("IKEA of Sweden").productName("BILRESA scroll wheel").build());
    ConnectedHomeIpReflectionGateway gateway = new ConnectedHomeIpReflectionGateway(
            () -> controller,
            unusedBleProvider(),
            () -> 1L,
            unusedMonitor(),
            unusedAttestationHandler(),
            unusedPointerProvider(),
            fakeCommandFactory(),
            metadataReader,
            1000L);

    MatterDeviceDetails details = gateway.readDeviceDetails(0x165BC267A7E344D0L);

    assertSame(controller, metadataReader.controller);
    assertEquals(0x165BC267A7E344D0L, metadataReader.nodeId);
    assertEquals("IKEA of Sweden", details.vendorName());
}
```

- [ ] **Step 2: Run controller tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerTest" --tests "org.openhab.matter.companion.controller.ConnectedHomeIpReflectionGatewayTest"
```

Expected: compilation fails until the new DTO and methods are added.

- [ ] **Step 3: Implement controller DTO and seams**

Create an immutable `MatterDeviceDetails` Java DTO with a nested `Builder`, string getters for BasicInformation values, `Integer batteryPercentRemaining()`, `Integer batteryQuantity()`, `String batteryDesignation()`, `String threadNetworkName()`, `Integer threadChannel()`, `String ipv6Address()`, and `Boolean otaUpdatePossible()`.

Add `MatterController.readDeviceDetails(...)`; implement:

- `FakeMatterController`: return `MatterDeviceDetails.empty()`.
- `ConnectedHomeIpMatterController`: call `requireArtifactsReady()`, emit start/finish progress, delegate to `gateway.readDeviceDetails(nodeId)`.
- `ConnectedHomeIpControllerGateway`: add `MatterDeviceDetails readDeviceDetails(long nodeId) throws Exception`.
- `ConnectedHomeIpDeviceMetadataReader`: add `MatterDeviceDetails readDeviceDetails(Object controller, long nodeId) throws Exception` and make `none()` return `MatterDeviceDetails.empty()`.
- `ConnectedHomeIpReflectionGateway`: use `metadataReader.readDeviceDetails(...)`; for commissioning, call the same method and convert only vendor/product into `MatterDeviceMetadata`.

- [ ] **Step 4: Implement best-effort reflection metadata reads**

Update `ConnectedHomeIpReflectionDeviceMetadataReader`:

- Keep BasicInformation endpoint `0`.
- Read string attributes by reflected methods:
  - `readVendorNameAttribute`
  - `readProductNameAttribute`
  - `readSoftwareVersionStringAttribute`
  - `readHardwareVersionStringAttribute`
  - `readPartNumberAttribute`
- Attempt optional cluster constructors by class name:
  - `chip.devicecontroller.ChipClusters$PowerSourceCluster`
  - `chip.devicecontroller.ChipClusters$GeneralDiagnosticsCluster`
  - `chip.devicecontroller.ChipClusters$ThreadNetworkDiagnosticsCluster`
  - `chip.devicecontroller.ChipClusters$OtaSoftwareUpdateRequestorCluster`
- Use helper callbacks that latch on `onSuccess`/`onError`, return empty/null on timeout, and emit diagnostics instead of throwing per-attribute failures.
- If a cluster class or method is absent in packaged artifacts, emit a diagnostic and continue.
- For GeneralDiagnostics IPv6, accept either a callback result that can be converted to a display string or no value. Do not invent addresses.

- [ ] **Step 5: Run controller tests to verify they pass**

Run the same controller test command. Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/org/openhab/matter/companion/controller app/src/test/java/org/openhab/matter/companion/controller
git commit -m "Add explicit Matter device details fetch seam"
```

---

### Task 3: ViewModel Navigation And Fetch State

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigationTest.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducerTest.kt`

- [ ] **Step 1: Write failing reducer/back-navigation tests**

Add:

```kotlin
@Test
fun phoneDeviceDetailsBackReturnsToPhoneDeviceList() {
    val state = MatterSetupStateReducer.phoneDeviceDetails(
        PhoneMatterDevice(nodeId = 1234L, controllerStateStored = true, stateReadable = true)
    )

    assertEquals(MatterSetupAction.ShowPhoneDevices, MatterSetupBackNavigation.systemBackAction(state))
}

@Test
fun phoneDeviceDetailsInitializesFromStagedDevice() {
    val state = MatterSetupStateReducer.phoneDeviceDetails(
        PhoneMatterDevice(
            nodeId = 0x165BC267A7E344D0L,
            controllerStateStored = false,
            stateReadable = true,
            vendorName = "IKEA of Sweden",
            productName = "BILRESA scroll wheel"
        )
    )

    assertEquals(MatterSetupStage.PhoneDeviceDetails, state.stage)
    assertEquals("BILRESA scroll wheel", state.phoneDeviceDetails.deviceName)
    assertEquals("IKEA of Sweden", state.phoneDeviceDetails.vendor)
    assertEquals("0x165BC267A7E344D0", state.phoneDeviceDetails.nodeId)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "org.openhab.matter.companion.setup.MatterSetupBackNavigationTest" --tests "org.openhab.matter.companion.setup.MatterSetupStateReducerTest"
```

Expected: FAIL until details state/back support exists.

- [ ] **Step 3: Implement ViewModel detail navigation**

In `MatterSetupViewModel`:

- Add `private var selectedPhoneDevice: PhoneMatterDevice? = null`.
- On `ShowPhoneDevices`, refresh devices and clear selected device if the list is empty.
- On `ShowPhoneDeviceDetails(nodeId)`, find by node id, falling back to the first staged device when only one exists, set `selectedPhoneDevice`, and set `uiState = MatterSetupStateReducer.phoneDeviceDetails(device)`.
- On `FetchPhoneDeviceDetails`, call a new private `fetchPhoneDeviceDetails()`.

The fetch method should:

```kotlin
private fun fetchPhoneDeviceDetails() {
    val device = selectedPhoneDevice ?: phoneDevices.firstOrNull() ?: return
    if (device.nodeId == null || !device.stateReadable) {
        uiState = uiState.copy(phoneDeviceDetailsMessage = "Could not fetch data from device")
        return
    }
    if (!executionGate.tryStart()) return
    uiState = uiState.copy(phoneDeviceDetailsFetching = true, phoneDeviceDetailsMessage = "")
    workerThread = Thread({
        try {
            val selection = controllerSession.selectNativeIfReady()
            if (!selection.nativeSelected()) {
                throw IllegalStateException("connectedhomeip is not ready for device metadata.")
            }
            val details = selection.controller().readDeviceDetails(
                device.nodeId,
                bootstrapStateRepository.load().controllerState(),
                { _ -> }
            )
            val update = PhoneMatterDeviceDetails.fromControllerDetails(details, device)
            postState {
                uiState = uiState.copy(
                    phoneDeviceDetails = uiState.phoneDeviceDetails.merge(update),
                    phoneDeviceDetailsFetching = false,
                    phoneDeviceDetailsMessage = "Device data refreshed"
                )
            }
        } catch (error: Exception) {
            postState {
                uiState = uiState.copy(
                    phoneDeviceDetailsFetching = false,
                    phoneDeviceDetailsMessage = "Could not fetch data from device"
                )
            }
        } finally {
            executionGate.finish()
        }
    }, "matter-device-details-fetch")
    workerThread?.start()
}
```

Use Kotlin lambda syntax for the Java `ProgressListener`: `{ _ -> }`.

- [ ] **Step 4: Run reducer/navigation tests**

Run the same tests. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt app/src/main/kotlin/org/openhab/matter/companion/setup app/src/test/kotlin/org/openhab/matter/companion/setup
git commit -m "Wire phone device details navigation"
```

---

### Task 4: Compose Screens, Icons, Copy, And Clipboard

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceListScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceDetailsScreen.kt`
- Add: `app/src/main/res/drawable/ic_material_expand_more.xml`
- Add: `app/src/main/res/drawable/ic_material_expand_less.xml`
- Add: `app/src/main/res/drawable/ic_material_info.xml`
- Add: `app/src/main/res/drawable/ic_material_delete.xml`
- Add: `app/src/main/res/drawable/ic_material_open_in_new.xml`
- Add: `app/src/main/res/drawable/ic_material_cloud_download.xml`
- Add: `app/src/main/res/drawable/ic_material_label.xml`
- Add: `app/src/main/res/drawable/ic_material_store.xml`
- Add: `app/src/main/res/drawable/ic_material_inventory.xml`
- Add: `app/src/main/res/drawable/ic_material_numbers.xml`
- Add: `app/src/main/res/drawable/ic_material_battery.xml`
- Add: `app/src/main/res/drawable/ic_material_wifi.xml`
- Add: `app/src/main/res/drawable/ic_material_public.xml`
- Test: `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

- [ ] **Step 1: Write failing Compose tests for list and details**

Update list tests to assert:

```kotlin
composeRule.onNodeWithText("Devices on this phone").assertIsDisplayed()
composeRule.onNodeWithText("Matter devices staged by this app for openHAB handoff.").assertIsDisplayed()
composeRule.onNodeWithText("BILRESA scroll wheel").assertIsDisplayed()
composeRule.onNodeWithText("IKEA of Sweden").assertIsDisplayed()
composeRule.onAllNodesWithText("Send code to openHAB").assertCountEquals(0)
composeRule.onAllNodesWithText("Send command to openHAB").assertCountEquals(0)
composeRule.onAllNodesWithText("Node:").assertCountEquals(0)
composeRule.onNodeWithContentDescription("Show diagnostics").performClick()
composeRule.onNodeWithText("Node").assertIsDisplayed()
composeRule.onNodeWithText("Controller state").assertIsDisplayed()
composeRule.onNodeWithText("State readable").assertIsDisplayed()
composeRule.onNodeWithText("Debug information").assertIsDisplayed()
composeRule.onNodeWithText("Open commissioning window").assertIsDisplayed()
composeRule.onNodeWithText("Details").assertIsDisplayed()
composeRule.onNodeWithText("Forget from this phone").assertIsDisplayed()
```

Add details tests:

```kotlin
@Test
fun phoneDeviceDetailsShowsHeaderRowsFetchAndActions() {
    render(
        state = MatterSetupStateReducer.phoneDeviceDetails(
            PhoneMatterDevice(
                nodeId = 0x165BC267A7E344D0L,
                controllerStateStored = false,
                stateReadable = true,
                vendorName = "IKEA of Sweden",
                productName = "BILRESA scroll wheel"
            )
        )
    )

    composeRule.onNodeWithText("Advanced").assertIsDisplayed()
    composeRule.onNodeWithText("Device details").assertIsDisplayed()
    composeRule.onNodeWithText("Helpful information for advanced setup and troubleshooting.").assertIsDisplayed()
    composeRule.onNodeWithText("Not yet added to openHAB").assertIsDisplayed()
    composeRule.onNodeWithText("Device name").assertIsDisplayed()
    composeRule.onNodeWithText("Vendor").assertIsDisplayed()
    composeRule.onNodeWithText("IKEA of Sweden").assertIsDisplayed()
    composeRule.onNodeWithText("IPv6 address").assertIsDisplayed()
    composeRule.onNodeWithText("Unknown").assertExists()
    composeRule.onNodeWithText("Fetch additional data from device").assertIsDisplayed()
    composeRule.onAllNodesWithText("Fetched from Matter clusters").assertCountEquals(0)
    composeRule.onAllNodesWithText("Open commissioning window").assertCountEquals(1)
    composeRule.onAllNodesWithText("Forget from this phone").assertCountEquals(1)
}
```

- [ ] **Step 2: Run android tests to verify they fail**

Run on an available emulator/device if configured:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest --tests "org.openhab.matter.companion.ui.MatterSetupAppTest"
```

If no Android test device is available, run `:app:testDebugUnitTest` after implementation and report the androidTest gap.

- [ ] **Step 3: Implement icons**

Add simple Material-style vector drawables with `android:fillColor="?attr/colorControlNormal"` where possible. Use existing `ic_material_arrow_back` and `ic_material_settings` as style references.

- [ ] **Step 4: Implement redesigned list screen**

Use `Card`, `IconButton`, `OutlinedButton`, `TextButton`/clickable action cells, `rememberSaveable(device.nodeId) { mutableStateOf(false) }`, and `painterResource(...)`.

Keep diagnostics hidden until expanded. Use content descriptions:

- `Show diagnostics`
- `Hide diagnostics`
- `Open commissioning window`
- `View details`
- `Forget from this phone`

Dispatch:

```kotlin
onAction(MatterSetupAction.OpenCommissioningWindowAgain)
onAction(MatterSetupAction.ShowPhoneDeviceDetails(device.nodeId))
onAction(MatterSetupAction.ForgetFromPhone)
```

- [ ] **Step 5: Implement details screen**

Create `PhoneDeviceDetailsScreen` with:

- `MatterSetupScaffold(title = "Device details", message = ..., showBack = true, onBack = { onAction(MatterSetupAction.ShowPhoneDevices) })`
- Breadcrumb/link text `Advanced` near top.
- Header card with product name and beige status pill.
- Rows in spec order.
- `LocalClipboardManager.current.setText(AnnotatedString(row.copyValue))` on row click.
- Snackbar or local message text for `Copied <label>`; prefer `SnackbarHost` if already convenient.
- Outlined fetch button dispatching `FetchPhoneDeviceDetails`.
- Primary/secondary bottom buttons dispatching `OpenCommissioningWindowAgain` and `ForgetFromPhone`.

- [ ] **Step 6: Route details stage**

In `MatterSetupApp`, add:

```kotlin
MatterSetupStage.PhoneDeviceDetails -> PhoneDeviceDetailsScreen(
    state = state,
    onAction = onAction
)
```

- [ ] **Step 7: Run tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

Run android tests if a device is attached:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe devices
.\gradlew.bat :app:connectedDebugAndroidTest --tests "org.openhab.matter.companion.ui.MatterSetupAppTest"
```

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/ui app/src/main/res/drawable app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt
git commit -m "Redesign phone device handoff screens"
```

---

### Task 5: End-To-End Verification, Cleanup, And Documentation Check

**Files:**
- Modify only files needed to fix issues found by verification.
- Optionally modify `docs/implementation-status.md` if the implementation materially changes current capability wording around the phone-device details screen.

- [ ] **Step 1: Run full unit tests**

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build debug APK**

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run targeted android UI tests if a device is available**

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe devices
```

If a device or emulator is listed:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest --tests "org.openhab.matter.companion.ui.MatterSetupAppTest"
```

If no device is listed, record that androidTest verification could not be run.

- [ ] **Step 4: Inspect changed files**

```powershell
git status --short
git diff --stat HEAD
```

Expected: only implementation files changed since the previous task commits.

- [ ] **Step 5: Commit verification fixes or docs update**

If no files changed, skip this commit. If fixes/docs were needed:

```powershell
git add app/src/main docs/implementation-status.md
git commit -m "Polish Matter handoff flow redesign"
```

---

## Required Final Review

After all tasks are complete:

- Run a final code review against `docs/superpowers/specs/2026-05-31-matter-handoff-flow-redesign-design.md`.
- Confirm no product images/placeholders were introduced.
- Confirm **Devices on this phone** shows only staging diagnostics plus actions.
- Confirm **Device details** owns the full mockup row set and explicit fetch.
- Confirm connectedhomeip fetch/commissioning still fails closed when native readiness is unavailable.
- Confirm tests and build results are reported in the final answer.
