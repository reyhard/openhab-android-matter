# Matter Setup UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved first-run, main Add Matter device, Settings, and Thread Network editor UI refresh while preserving connectedhomeip fail-closed behavior.

**Architecture:** Keep `MatterSetupViewModel` as the state owner, but separate navigation stages for welcome, required setup, main add-device, settings, token replacement, Thread editor, devices, and troubleshooting. Keep Compose screen files focused by adding small new screen/component files rather than growing `OpenHabSetupScreen.kt`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android resources, existing Java/Kotlin config repositories, JUnit, Compose UI tests, Gradle.

---

## File Structure

- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`: add UI stages for welcome/settings/thread editor/token editor.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`: add navigation and token/edit actions.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`: add factory states for welcome, main add-device, settings, thread editor, token editor.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt`: route reset/settings/navigation states and remove BackToMainMenu from settings-adjacent state.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigation.kt`: implement system-back mappings for settings, editor, devices, troubleshooting, manual code, and scanner entry.
- Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfigCompleteness.kt`: pure helper for deciding whether persisted config is complete enough to skip first-run setup.
- Create `app/src/main/kotlin/org/openhab/matter/companion/setup/FirstRunSettingsValidator.kt`: pure validation facade for openHAB status, Thread dataset status, and OTBR status.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt`: default first-run URL, route new actions, validate full first-run settings before saving, mask saved token in UI state.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/components/MatterSetupScaffold.kt`: shared top logo/back/settings chrome and simple card helpers.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/WelcomeScreen.kt`: Get started screen.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt`: main Add Matter device screen with cog, scan-guide image, QR icon button, manual entry.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/SettingsScreen.kt`: organized settings page.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/ThreadNetworkEditorScreen.kt`: focused Thread editor.
- Create `app/src/main/kotlin/org/openhab/matter/companion/ui/ChangeTokenScreen.kt`: controlled token replacement screen.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt`: simplify into first-run required setup form only.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/ManualCodeScreen.kt`: remove visible Back button and rely on system back.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceListScreen.kt`: remove Back buttons.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt`: remove bottom navigation buttons that duplicate system back unless they are real recovery actions.
- Modify `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`: route new stages to new screens.
- Add resources under `app/src/main/res/drawable/`: `openhab_setup_hero.png`, `matter_scan_guide.png`, and `openhab_icon.xml` or `openhab_icon.png`.
- Modify tests under `app/src/test/kotlin/org/openhab/matter/companion/setup/` and `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`.

---

### Task 1: Navigation State And Back Behavior

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigation.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfigCompleteness.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducerTest.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigationTest.kt`

- [ ] **Step 1: Write failing reducer tests**

Add these tests to `MatterSetupStateReducerTest.kt`:

```kotlin
@Test
fun resetReturnsWelcomeWhenConfigurationIsIncomplete() {
    val state = MatterSetupStateReducer.reset(
        setupComplete = false,
        openHabUrl = "http://openhab:8080"
    )

    assertEquals(MatterSetupStage.Welcome, state.stage)
    assertEquals("Set up Matter with openHAB", state.title)
    assertEquals(MatterSetupAction.GetStarted, state.primaryAction)
    assertEquals("Get started", state.primaryActionLabel)
}

@Test
fun getStartedReturnsRequiredSetupWithDefaultOpenHabAddress() {
    val state = MatterSetupStateReducer.requiredSetup("")

    assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
    assertEquals("Connect to openHAB", state.title)
    assertEquals(MatterSetupAction.TestSettings, state.primaryAction)
    assertEquals("Test settings", state.primaryActionLabel)
    assertEquals("http://openhab:8080", state.openHabUrlFallback)
    assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
}

@Test
fun settingsStateUsesDedicatedSettingsStage() {
    val state = MatterSetupStateReducer.settings()

    assertEquals(MatterSetupStage.Settings, state.stage)
    assertEquals("Settings", state.title)
    assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
}

@Test
fun phoneDeviceListDoesNotExposeBackToMainMenu() {
    val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true)

    assertEquals(MatterSetupStage.PhoneDeviceList, state.stage)
    assertFalse(state.secondaryActions.contains(MatterSetupAction.BackToMainMenu))
}
```

Replace the existing `resetReturnsOpenHabSetupWhenOpenHabIsNotConfigured`, `editSettingsReturnsOpenHabSetupWithCurrentUrlPreserved`, `openHabSetupReadyKeepsUserInSettingsWithBackAndTroubleshootingActions`, and `phoneDeviceListUsesDedicatedStageAndBackActions` assertions so they expect the new state names and no visible `BackToMainMenu`.

- [ ] **Step 2: Write failing back-navigation tests**

Replace `MatterSetupBackNavigationTest.kt` with:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatterSetupBackNavigationTest {
    @Test
    fun welcomeBackIsNotIntercepted() {
        val state = MatterSetupStateReducer.reset(
            setupComplete = false,
            openHabUrl = "http://openhab:8080"
        )

        assertNull(MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun settingsBackReturnsToMainScreen() {
        val state = MatterSetupStateReducer.settings()

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun threadEditorBackReturnsToSettings() {
        val state = MatterSetupStateReducer.threadNetworkEditor()

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun tokenEditorBackReturnsToSettings() {
        val state = MatterSetupStateReducer.changeToken()

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun phoneDevicesBackReturnsToSettings() {
        val state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true)

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun troubleshootingFromSettingsBackReturnsToSettings() {
        val state = MatterSetupStateReducer.advancedTroubleshooting(MatterSetupStateReducer.settings())

        assertEquals(MatterSetupAction.BackToSettings, MatterSetupBackNavigation.systemBackAction(state))
    }

    @Test
    fun manualCodeBackReturnsToMainScreen() {
        val state = MatterSetupStateReducer.manualCodeEntry()

        assertEquals(MatterSetupAction.BackToMainMenu, MatterSetupBackNavigation.systemBackAction(state))
    }
}
```

- [ ] **Step 3: Run reducer/navigation tests and confirm failure**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupStateReducerTest" --tests "*MatterSetupBackNavigationTest"
```

Expected: compile failures for missing `Welcome`, `Settings`, `ThreadNetworkEditor`, `ChangeToken`, `GetStarted`, `TestSettings`, `ChangeToken`, `EditThreadNetwork`, `openHabUrlFallback`, and new reducer signatures.

- [ ] **Step 4: Add stages and actions**

Update `MatterSetupStage.kt`:

```kotlin
package org.openhab.matter.companion.setup

enum class MatterSetupStage {
    Welcome,
    NeedsOpenHabSetup,
    OpenHabSetupChecking,
    ReadyToScan,
    ScanningQr,
    QrScanned,
    EnteringManualCode,
    ReadinessChecking,
    NeedsPairingMode,
    CommissioningToPhone,
    OpeningCommissioningWindow,
    CommissioningWindowOpen,
    SendingCodeToOpenHab,
    WatchingOpenHabInbox,
    SuccessInboxDetected,
    Failed,
    Settings,
    ChangeToken,
    ThreadNetworkEditor,
    PhoneDeviceList,
    AdvancedTroubleshooting
}
```

Update `MatterSetupAction.kt`:

```kotlin
package org.openhab.matter.companion.setup

sealed interface MatterSetupAction {
    data object GetStarted : MatterSetupAction
    data object SaveOpenHab : MatterSetupAction
    data object TestOpenHab : MatterSetupAction
    data object TestSettings : MatterSetupAction
    data object EditSettings : MatterSetupAction
    data object ChangeToken : MatterSetupAction
    data object SaveChangedToken : MatterSetupAction
    data object EditThreadNetwork : MatterSetupAction
    data object CheckThreadDataset : MatterSetupAction
    data object SaveThreadSettings : MatterSetupAction
    data object DetectThreadBorderRouters : MatterSetupAction
    data class SelectThreadBorderRouter(val endpoint: String) : MatterSetupAction
    data object BackToMainMenu : MatterSetupAction
    data object BackToSettings : MatterSetupAction
    data object ShowPhoneDevices : MatterSetupAction
    data object StartScan : MatterSetupAction
    data object EnterCodeManually : MatterSetupAction
    data object SubmitManualCode : MatterSetupAction
    data object ConfirmPairingMode : MatterSetupAction
    data object Retry : MatterSetupAction
    data object OpenCommissioningWindowAgain : MatterSetupAction
    data object BrowseMatterServices : MatterSetupAction
    data object CheckIpv6Reachability : MatterSetupAction
    data object ShowTroubleshooting : MatterSetupAction
    data object ForgetFromPhone : MatterSetupAction
    data object AddAnotherDevice : MatterSetupAction
}
```

- [ ] **Step 5: Add config completeness helper**

Create `MatterSetupConfigCompleteness.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.openhab.matter.companion.config.AppConfig

object MatterSetupConfigCompleteness {
    const val DefaultOpenHabUrl = "http://openhab:8080"

    fun isComplete(config: AppConfig): Boolean {
        return config.openHabBaseUrl().isNotBlank() &&
            config.openHabApiToken().isNotBlank() &&
            config.threadDataset().isNotBlank() &&
            config.otbrBaseUrl().isNotBlank() &&
            !config.openHabApiTokenUnreadable() &&
            !config.threadDatasetUnreadable()
    }
}
```

- [ ] **Step 6: Extend UI state with fallback URL**

Modify the `MatterSetupUiState` data class constructor:

```kotlin
data class MatterSetupUiState(
    val stage: MatterSetupStage,
    val title: String,
    val message: String,
    val steps: List<MatterSetupStep> = emptyList(),
    val activeDetail: String? = null,
    val countdownSeconds: Int? = null,
    val primaryAction: MatterSetupAction? = null,
    val primaryActionLabel: String = "",
    val primaryActionEnabled: Boolean = true,
    val secondaryActions: List<MatterSetupAction> = emptyList(),
    val failure: MatterSetupFailure? = null,
    val diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty(),
    val openHabUrlFallback: String = ""
)
```

Add or replace companion factories:

```kotlin
companion object {
    fun initial(setupComplete: Boolean): MatterSetupUiState {
        return if (setupComplete) {
            addMatterDevice()
        } else {
            welcome()
        }
    }

    fun welcome(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.Welcome,
            title = "Set up Matter with openHAB",
            message = "A guided local setup for adding Matter devices to your openHAB home.",
            primaryAction = MatterSetupAction.GetStarted,
            primaryActionLabel = "Get started"
        )
    }

    fun addMatterDevice(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ReadyToScan,
            title = "Add Matter device",
            message = "Scan the device QR code or enter the setup code manually.",
            primaryAction = MatterSetupAction.StartScan,
            primaryActionLabel = "Scan code",
            secondaryActions = listOf(MatterSetupAction.EditSettings)
        )
    }

    // Keep the existing progress(), failed(), progressMessage(), and progressSteps() functions unchanged.
}
```

- [ ] **Step 7: Update reducer**

Replace `MatterSetupStateReducer` with:

```kotlin
package org.openhab.matter.companion.setup

object MatterSetupStateReducer {
    fun reset(setupComplete: Boolean, openHabUrl: String): MatterSetupUiState {
        return if (setupComplete) {
            MatterSetupUiState.addMatterDevice()
        } else {
            MatterSetupUiState.welcome()
        }
    }

    fun requiredSetup(openHabUrl: String): MatterSetupUiState {
        val effectiveUrl = openHabUrl.ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
        return MatterSetupUiState(
            stage = MatterSetupStage.NeedsOpenHabSetup,
            title = "Connect to openHAB",
            message = "Connect to your openHAB home and Thread network before adding Matter devices.",
            primaryAction = MatterSetupAction.TestSettings,
            primaryActionLabel = "Test settings",
            primaryActionEnabled = effectiveUrl.trim().isNotBlank(),
            secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting),
            openHabUrlFallback = effectiveUrl
        )
    }

    fun settings(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.Settings,
            title = "Settings",
            message = "Manage openHAB, Thread network, and local phone staging for Matter setup."
        )
    }

    fun changeToken(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ChangeToken,
            title = "Change token",
            message = "Enter a new openHAB access token and test it before saving.",
            primaryAction = MatterSetupAction.SaveChangedToken,
            primaryActionLabel = "Save token"
        )
    }

    fun threadNetworkEditor(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ThreadNetworkEditor,
            title = "Thread network",
            message = "Used to commission Matter devices to your Thread network before handing them to openHAB.",
            primaryAction = MatterSetupAction.SaveThreadSettings,
            primaryActionLabel = "Save"
        )
    }

    fun manualCodeEntry(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.EnteringManualCode,
            title = "Enter setup code",
            message = "Type the 11-digit Matter setup code printed on the device or box.",
            primaryAction = MatterSetupAction.SubmitManualCode,
            primaryActionLabel = "Continue"
        )
    }

    fun phoneDeviceList(hasDevices: Boolean, message: String = ""): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.PhoneDeviceList,
            title = "Devices on this phone",
            message = message.ifBlank {
                if (hasDevices) {
                    "Matter devices staged by this app for openHAB handoff."
                } else {
                    "No staged Matter devices are stored on this phone."
                }
            },
            primaryAction = MatterSetupAction.BackToSettings,
            primaryActionLabel = "Back to settings"
        )
    }

    fun openHabSetupChecking(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.OpenHabSetupChecking,
            title = "Checking settings",
            message = "Checking openHAB, Thread dataset, and Border Router settings.",
            primaryAction = MatterSetupAction.TestSettings,
            primaryActionLabel = "Checking...",
            primaryActionEnabled = false
        )
    }

    fun openHabSetupNotReady(
        openHabUrl: String,
        message: String,
        failure: MatterSetupFailure? = null,
        diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty()
    ): MatterSetupUiState {
        return requiredSetup(openHabUrl).copy(
            message = message.ifBlank { "Settings are not ready yet. Check the details in troubleshooting." },
            failure = failure,
            diagnostics = diagnostics
        )
    }

    fun advancedTroubleshooting(current: MatterSetupUiState): MatterSetupUiState {
        val primaryAction = when (current.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking,
            MatterSetupStage.Settings,
            MatterSetupStage.ChangeToken,
            MatterSetupStage.ThreadNetworkEditor -> MatterSetupAction.BackToSettings
            else -> MatterSetupAction.Retry
        }
        return MatterSetupUiState(
            stage = MatterSetupStage.AdvancedTroubleshooting,
            title = "Advanced troubleshooting",
            message = current.failure?.message ?: "Review setup diagnostics before trying again.",
            primaryAction = primaryAction,
            primaryActionLabel = if (primaryAction == MatterSetupAction.BackToSettings) {
                "Back to settings"
            } else {
                "Back to setup"
            },
            failure = current.failure,
            diagnostics = current.diagnostics
        )
    }
}
```

- [ ] **Step 8: Update back navigation**

Replace `MatterSetupBackNavigation.kt` with:

```kotlin
package org.openhab.matter.companion.setup

object MatterSetupBackNavigation {
    fun systemBackAction(state: MatterSetupUiState): MatterSetupAction? {
        return when (state.stage) {
            MatterSetupStage.Settings,
            MatterSetupStage.EnteringManualCode,
            MatterSetupStage.ScanningQr,
            MatterSetupStage.NeedsPairingMode -> MatterSetupAction.BackToMainMenu

            MatterSetupStage.ChangeToken,
            MatterSetupStage.ThreadNetworkEditor,
            MatterSetupStage.PhoneDeviceList -> MatterSetupAction.BackToSettings

            MatterSetupStage.AdvancedTroubleshooting -> state.primaryAction

            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking,
            MatterSetupStage.Welcome,
            MatterSetupStage.ReadyToScan,
            MatterSetupStage.QrScanned,
            MatterSetupStage.ReadinessChecking,
            MatterSetupStage.CommissioningToPhone,
            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen,
            MatterSetupStage.SendingCodeToOpenHab,
            MatterSetupStage.WatchingOpenHabInbox,
            MatterSetupStage.SuccessInboxDetected,
            MatterSetupStage.Failed -> null
        }
    }
}
```

- [ ] **Step 9: Run reducer/navigation tests and verify pass**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupStateReducerTest" --tests "*MatterSetupBackNavigationTest"
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducer.kt app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigation.kt app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfigCompleteness.kt app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupStateReducerTest.kt app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupBackNavigationTest.kt
git commit -m "feat: define refreshed setup navigation"
```

---

### Task 2: First-Run Settings Validation And Token Replacement State

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/FirstRunSettingsValidator.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/FirstRunSettingsValidatorTest.kt`

- [ ] **Step 1: Write validator tests**

Create `FirstRunSettingsValidatorTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.otbr.OtbrStatus

class FirstRunSettingsValidatorTest {
    @Test
    fun validInputsPass() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertTrue(result.ready)
        assertEquals(emptyList<String>(), result.warnings)
    }

    @Test
    fun missingTokenFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("openHAB access token is required"))
    }

    @Test
    fun invalidDatasetFailsWithoutEchoingInput() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "not-a-dataset",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("Thread dataset is not valid."))
        assertFalse(result.details.joinToString(" ").contains("not-a-dataset"))
    }

    @Test
    fun openHabMatterControllerFailureFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1",
            openHabStatus = OpenHabStatus(false, true, false, "Matter controller offline", "thing offline"),
            otbrStatus = OtbrStatus(true, "accepted", "")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("Matter controller offline"))
    }

    @Test
    fun invalidOtbrFails() {
        val result = FirstRunSettingsValidator.validate(
            openHabUrl = "http://openhab:8080",
            token = "ohab.token",
            dataset = "hex:0E080000000000010000",
            otbrBaseUrl = "bad host",
            openHabStatus = OpenHabStatus(true, true, true, "ready", ""),
            otbrStatus = OtbrStatus(false, "OTBR address is invalid", "bad host")
        )

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("OTBR address is invalid"))
    }
}
```

- [ ] **Step 2: Run validator test and confirm failure**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*FirstRunSettingsValidatorTest"
```

Expected: FAIL because `FirstRunSettingsValidator` does not exist.

- [ ] **Step 3: Implement validator**

Create `FirstRunSettingsValidator.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.otbr.OtbrStatus

data class FirstRunSettingsValidationResult(
    val ready: Boolean,
    val details: List<String>,
    val warnings: List<String>
)

object FirstRunSettingsValidator {
    fun validate(
        openHabUrl: String,
        token: String,
        dataset: String,
        otbrBaseUrl: String,
        openHabStatus: OpenHabStatus,
        otbrStatus: OtbrStatus
    ): FirstRunSettingsValidationResult {
        val datasetValidation = ThreadDatasetSettingsValidator.validate(dataset)
        val warnings = buildList {
            if (openHabUrl.isBlank()) {
                add("openHAB address is required")
            }
            if (token.isBlank()) {
                add("openHAB access token is required")
            }
            if (!openHabStatus.restReachable()) {
                add(openHabStatus.message().orEmpty().ifBlank { "openHAB REST API is not reachable" })
            }
            if (!openHabStatus.matterControllerReady()) {
                add(openHabStatus.message().orEmpty().ifBlank { "openHAB Matter controller is not ready" })
            }
            if (datasetValidation.status != ThreadDatasetSettingsStatus.Valid) {
                add(datasetValidation.title)
            }
            if (otbrBaseUrl.isBlank()) {
                add("Thread Border Router address is required")
            }
            if (!otbrStatus.reachable()) {
                add(otbrStatus.message().orEmpty().ifBlank { "Thread Border Router is not reachable" })
            }
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        val details = listOf(
            "openHAB REST reachable=${openHabStatus.restReachable()}",
            "openHAB Matter controller ready=${openHabStatus.matterControllerReady()}",
            datasetValidation.title,
            "Thread Border Router reachable=${otbrStatus.reachable()}",
            openHabStatus.details().orEmpty(),
            otbrStatus.details().orEmpty()
        ).map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { detail ->
                detail.replace(dataset, "<redacted>")
                    .replace(token, "<redacted>")
                    .replace(otbrBaseUrl, otbrBaseUrl.toLogSafeUrl())
                    .replace(openHabUrl, openHabUrl.toLogSafeUrl())
            }
            .distinct()

        return FirstRunSettingsValidationResult(
            ready = warnings.isEmpty() && openHabStatus.online(),
            details = details,
            warnings = warnings
        )
    }
}
```

- [ ] **Step 4: Run validator test and verify pass**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*FirstRunSettingsValidatorTest"
```

Expected: PASS.

- [ ] **Step 5: Update ViewModel initialization and first-run route**

In `MatterSetupViewModel.kt`, import `HttpOtbrClient`, `FirstRunSettingsValidator`, and `MatterSetupConfigCompleteness`. Add:

```kotlin
private val otbrClient by lazy { HttpOtbrClient() }
private var setupComplete = false
```

In `init`, replace the current openHAB-only setup logic:

```kotlin
val config = configRepository.load()
openHabUrl = config.openHabBaseUrl().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
token = ""
threadDataset = config.threadDataset()
otbrBaseUrl = config.otbrBaseUrl()
attestationBypassEnabled = config.attestationBypassEnabled()
threadSettingsMessage = ThreadDatasetSettingsValidator.validate(
    threadDataset,
    config.threadDatasetUnreadable()
).title
setupComplete = MatterSetupConfigCompleteness.isComplete(config)
uiState = MatterSetupStateReducer.reset(setupComplete, openHabUrl)
```

- [ ] **Step 6: Update ViewModel action routing**

In `handleAction`, add/replace these cases:

```kotlin
MatterSetupAction.GetStarted -> {
    if (openHabUrl.isBlank()) {
        openHabUrl = MatterSetupConfigCompleteness.DefaultOpenHabUrl
    }
    uiState = MatterSetupStateReducer.requiredSetup(openHabUrl)
}

MatterSetupAction.TestSettings -> {
    startFirstRunSettingsCheck()
}

MatterSetupAction.EditSettings -> {
    uiState = MatterSetupStateReducer.settings()
}

MatterSetupAction.ChangeToken -> {
    token = ""
    uiState = MatterSetupStateReducer.changeToken()
}

MatterSetupAction.SaveChangedToken -> {
    startChangedTokenCheck()
}

MatterSetupAction.EditThreadNetwork -> {
    uiState = MatterSetupStateReducer.threadNetworkEditor()
}

MatterSetupAction.BackToSettings -> {
    restorePersistedConfig(maskToken = true)
    uiState = MatterSetupStateReducer.settings()
}

MatterSetupAction.BackToMainMenu -> {
    scannedPayload = ""
    manualSetupCode = ""
    restorePersistedConfig(maskToken = true)
    uiState = MatterSetupUiState.addMatterDevice()
}

MatterSetupAction.EnterCodeManually -> {
    uiState = MatterSetupStateReducer.manualCodeEntry()
}
```

Keep existing cases for QR workflow, troubleshooting, devices, router detection, and forget/retry.

- [ ] **Step 7: Implement first-run settings validation in ViewModel**

Add this method to `MatterSetupViewModel.kt`:

```kotlin
private fun startFirstRunSettingsCheck() {
    val baseUrl = openHabUrl.trim().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
    val apiToken = token.trim()
    val datasetInput = threadDataset.trim()
    val otbrTarget = otbrBaseUrl.trim()
    if (!executionGate.tryStart()) {
        return
    }

    uiState = MatterSetupStateReducer.openHabSetupChecking()
    workerThread = Thread({
        try {
            val existingConfig = configRepository.load()
            val openHabStatus = openHabClient.checkReadiness(baseUrl, apiToken)
            val otbrStatus = otbrClient.checkReadiness(otbrTarget)
            val validation = FirstRunSettingsValidator.validate(
                openHabUrl = baseUrl,
                token = apiToken,
                dataset = datasetInput,
                otbrBaseUrl = otbrTarget,
                openHabStatus = openHabStatus,
                otbrStatus = otbrStatus
            )
            val sanitizer = SetupFailureSanitizer(existingConfig, baseUrl, apiToken)
            if (validation.ready) {
                val parsedDataset = ThreadDataset.parse(datasetInput).chipToolValue()
                configRepository.save(
                    AppConfig(
                        parsedDataset,
                        existingConfig.setupPayload(),
                        baseUrl,
                        apiToken,
                        otbrTarget,
                        false,
                        existingConfig.setupPayloadUnreadable(),
                        false,
                        attestationBypassEnabled
                    )
                )
                postState {
                    openHabUrl = baseUrl
                    token = ""
                    threadDataset = parsedDataset
                    otbrBaseUrl = otbrTarget
                    setupComplete = true
                    threadSettingsMessage = "Thread dataset looks valid."
                    uiState = MatterSetupUiState.addMatterDevice()
                }
            } else {
                val failure = MatterSetupFailure(
                    step = MatterSetupStage.OpenHabSetupChecking,
                    message = "Settings are not ready yet",
                    details = validation.details.map(sanitizer::sanitize).joinToString("; ")
                )
                emitState(
                    MatterSetupStateReducer.openHabSetupNotReady(
                        baseUrl,
                        validation.warnings.map(sanitizer::sanitize).joinToString(". "),
                        failure,
                        MatterSetupDiagnosticsSummary(
                            checks = validation.details.map(sanitizer::sanitize),
                            warnings = validation.warnings.map(sanitizer::sanitize),
                            details = validation.details.map(sanitizer::sanitize)
                        )
                    )
                )
            }
        } catch (error: Exception) {
            val existingConfig = runCatching { configRepository.load() }.getOrDefault(AppConfig("", ""))
            emitOpenHabSetupError(error, existingConfig, baseUrl, apiToken)
        } finally {
            executionGate.finish()
        }
    }, "first-run-settings-check")
    workerThread?.start()
}
```

- [ ] **Step 8: Implement changed-token validation**

Add this method:

```kotlin
private fun startChangedTokenCheck() {
    val baseUrl = openHabUrl.trim().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
    val apiToken = token.trim()
    if (apiToken.isBlank()) {
        uiState = MatterSetupStateReducer.changeToken().copy(
            message = "Enter an openHAB access token before saving."
        )
        return
    }
    if (!executionGate.tryStart()) {
        return
    }

    uiState = MatterSetupStateReducer.changeToken().copy(
        primaryActionEnabled = false,
        primaryActionLabel = "Checking..."
    )
    workerThread = Thread({
        try {
            val existingConfig = configRepository.load()
            val status = openHabClient.checkReadiness(baseUrl, apiToken)
            if (status.restReachable() && status.matterControllerReady()) {
                configRepository.save(
                    AppConfig(
                        existingConfig.threadDataset(),
                        existingConfig.setupPayload(),
                        baseUrl,
                        apiToken,
                        existingConfig.otbrBaseUrl(),
                        existingConfig.threadDatasetUnreadable(),
                        existingConfig.setupPayloadUnreadable(),
                        false,
                        existingConfig.attestationBypassEnabled()
                    )
                )
                postState {
                    token = ""
                    openHabUrl = baseUrl
                    uiState = MatterSetupStateReducer.settings()
                }
            } else {
                val sanitizer = SetupFailureSanitizer(existingConfig, baseUrl, apiToken)
                postState {
                    uiState = MatterSetupStateReducer.changeToken().copy(
                        message = sanitizer.sanitize(status.message().orEmpty().ifBlank {
                            "openHAB did not accept the token or Matter is not ready."
                        })
                    )
                }
            }
        } finally {
            executionGate.finish()
        }
    }, "openhab-token-change-check")
    workerThread?.start()
}
```

- [ ] **Step 9: Update restore/load config helpers**

Change `restorePersistedConfig()` signature to:

```kotlin
private fun restorePersistedConfig(maskToken: Boolean = false) {
    val config = configRepository.load()
    openHabUrl = config.openHabBaseUrl().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
    token = if (maskToken) "" else config.openHabApiToken()
    threadDataset = config.threadDataset()
    otbrBaseUrl = config.otbrBaseUrl()
    attestationBypassEnabled = config.attestationBypassEnabled()
    setupComplete = MatterSetupConfigCompleteness.isComplete(config)
    threadSettingsMessage = ThreadDatasetSettingsValidator.validate(
        threadDataset,
        config.threadDatasetUnreadable()
    ).title
}
```

Change `loadMatterSetupConfig()` so it uses persisted token if the editable token field is blank:

```kotlin
private fun loadMatterSetupConfig(): MatterSetupConfig {
    val config = configRepository.load()
    return MatterSetupConfig(
        openHabBaseUrl = openHabUrl.ifBlank { config.openHabBaseUrl() },
        openHabApiToken = token.ifBlank { config.openHabApiToken() },
        threadDataset = config.threadDataset(),
        otbrBaseUrl = config.otbrBaseUrl(),
        attestationBypassEnabled = config.attestationBypassEnabled()
    )
}
```

- [ ] **Step 10: Run unit tests and address only the expected API rename errors**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: unit tests compile. If failures mention old reducer calls such as `reset(openHabConfigured = ...)`, update only those call sites to `reset(setupComplete = ...)`. Some Compose android tests may still fail until UI tasks are complete.

- [ ] **Step 11: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/setup/FirstRunSettingsValidator.kt app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupViewModel.kt app/src/test/kotlin/org/openhab/matter/companion/setup/FirstRunSettingsValidatorTest.kt
git commit -m "feat: validate first-run Matter settings"
```

---

### Task 3: Android Assets And Shared Compose Components

**Files:**
- Create: `app/src/main/res/drawable/openhab_setup_hero.png`
- Create: `app/src/main/res/drawable/matter_scan_guide.png`
- Create: `app/src/main/res/drawable/openhab_icon.png`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/components/MatterSetupScaffold.kt`
- Test: `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

- [ ] **Step 1: Add temporary UI test expectation for image-backed screens**

Add this helper assertion later-used by screen tests:

```kotlin
private fun assertOpenHabLogoAndMatterVisualsCanRender() {
    composeRule.onNodeWithText("Set up Matter with openHAB").assertIsDisplayed()
}
```

This step intentionally compiles now but will be used in Task 4.

- [ ] **Step 2: Copy assets into Android resources**

Run:

```powershell
New-Item -ItemType Directory -Force app\src\main\res\drawable | Out-Null
Copy-Item docs\interface\image_1.png app\src\main\res\drawable\openhab_setup_hero.png -Force
Copy-Item docs\interface\image_2.png app\src\main\res\drawable\matter_scan_guide.png -Force
```

Convert `docs/interface/openhab-icon.svg` to a PNG if Android vector import rejects the SVG. Use this PowerShell/.NET conversion only if ImageMagick is not available:

```powershell
Add-Type -AssemblyName System.Drawing
$bitmap = New-Object System.Drawing.Bitmap 96,96
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Transparent)
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(90, 96, 102)), 7
$orangePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(229, 83, 0)), 7
$graphics.DrawArc($pen, 18, 18, 60, 60, 35, 290)
$graphics.DrawLine($orangePen, 30, 58, 48, 40)
$graphics.DrawLine($orangePen, 48, 40, 66, 58)
$graphics.Dispose()
$bitmap.Save((Resolve-Path app\src\main\res\drawable).Path + '\openhab_icon.png', [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
```

- [ ] **Step 3: Create shared Compose components**

Create `MatterSetupScaffold.kt`:

```kotlin
package org.openhab.matter.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R

@Composable
fun MatterSetupScaffold(
    title: String,
    message: String,
    showBack: Boolean = false,
    showSettings: Boolean = false,
    onBack: () -> Unit = {},
    onSettings: () -> Unit = {},
    content: @Composable Column.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.openhab_icon),
                contentDescription = "openHAB",
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (showSettings) {
                    IconButton(onClick = onSettings) {
                        Text("⚙", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
fun SettingsCard(content: @Composable Column.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
}
```

- [ ] **Step 4: Run resource compile**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: PASS. If `openhab_icon.png` is missing or invalid, the error will reference `R.drawable.openhab_icon`; create/fix that file before continuing.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/res/drawable/openhab_setup_hero.png app/src/main/res/drawable/matter_scan_guide.png app/src/main/res/drawable/openhab_icon.png app/src/main/kotlin/org/openhab/matter/companion/ui/components/MatterSetupScaffold.kt
git commit -m "feat: add setup UI assets and chrome"
```

---

### Task 4: Welcome And Add Matter Device Screens

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/WelcomeScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`
- Test: `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

- [ ] **Step 1: Update Compose UI tests for welcome and main screen**

Replace `firstRunSetupScreenShowsOpenHabSettings` with:

```kotlin
@Test
fun firstRunShowsWelcomeScreen() {
    render(MatterSetupStateReducer.reset(setupComplete = false, openHabUrl = "http://openhab:8080"))

    composeRule.onNodeWithText("Set up Matter with openHAB").assertIsDisplayed()
    composeRule.onNodeWithText("Get started").assertIsDisplayed()
    composeRule.onNodeWithText("Easy and guided").assertIsDisplayed()
}
```

Replace `readyToScanScreenShowsQrManualAndSettingsActions` with:

```kotlin
@Test
fun addMatterDeviceScreenShowsScanManualAndSettingsCog() {
    render(MatterSetupUiState.addMatterDevice())

    composeRule.onNodeWithText("Add Matter device").assertIsDisplayed()
    composeRule.onNodeWithText("Scan code").assertIsDisplayed()
    composeRule.onNodeWithText("Enter code manually").assertIsDisplayed()
    composeRule.onNodeWithText("⚙").assertIsDisplayed()
    composeRule.onNodeWithText("You can enter the 11-digit setup code instead.").assertIsDisplayed()
}
```

Add:

```kotlin
@Test
fun getStartedDispatchesAction() {
    val actions = mutableListOf<MatterSetupAction>()
    render(MatterSetupStateReducer.reset(setupComplete = false, openHabUrl = "http://openhab:8080"), onAction = actions::add)

    composeRule.onNodeWithText("Get started").performClick()

    assertTrue(actions.contains(MatterSetupAction.GetStarted))
}

@Test
fun settingsCogDispatchesEditSettings() {
    val actions = mutableListOf<MatterSetupAction>()
    render(MatterSetupUiState.addMatterDevice(), onAction = actions::add)

    composeRule.onNodeWithText("⚙").performClick()

    assertTrue(actions.contains(MatterSetupAction.EditSettings))
}
```

- [ ] **Step 2: Run UI tests and confirm failure**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: FAIL because `WelcomeScreen` and refreshed scan screen are not implemented.

- [ ] **Step 3: Create WelcomeScreen**

Create `WelcomeScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun WelcomeScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = "A guided local setup for adding Matter devices to your openHAB home."
    ) {
        Image(
            painter = painterResource(R.drawable.openhab_setup_hero),
            contentDescription = "Matter device setup with openHAB",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(topStart = 120.dp, topEnd = 120.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
        )
        Spacer(Modifier.height(18.dp))
        BenefitCard("Easy and guided", "Clear steps to get your device connected quickly.")
        Spacer(Modifier.height(10.dp))
        BenefitCard("Private and local", "Your devices stay in your home.")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.GetStarted) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Get started" })
        }
    }
}

@Composable
private fun BenefitCard(title: String, body: String) {
    SettingsCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text = body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

- [ ] **Step 4: Refresh ScanDeviceScreen**

Replace `ScanDeviceScreen.kt` with:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun ScanDeviceScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title.ifBlank { "Add Matter device" },
        message = state.message.ifBlank { "Scan the device QR code or enter the setup code manually." },
        showSettings = true,
        onSettings = { onAction(MatterSetupAction.EditSettings) }
    ) {
        Image(
            painter = painterResource(R.drawable.matter_scan_guide),
            contentDescription = "Matter QR code scanning guide",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Spacer(Modifier.height(18.dp))
        SettingsCard {
            ReadinessRow("✓", "openHAB connected")
            ReadinessRow("✓", "Thread network ready")
            ReadinessRow("✓", "Bluetooth and location ready")
            ReadinessRow("Manual", "You can enter the 11-digit setup code instead.")
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onAction(MatterSetupAction.StartScan) },
            modifier = Modifier.fillMaxWidth()
        ) {
            QrCodeIcon(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(12.dp))
            Text("Scan code")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.EnterCodeManually) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter code manually")
        }
    }
}

@Composable
private fun ReadinessRow(prefix: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prefix, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun QrCodeIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val cell = size.width / 5f
        val squares = listOf(
            0 to 0, 1 to 0, 0 to 1,
            3 to 0, 4 to 0, 4 to 1,
            0 to 3, 0 to 4, 1 to 4,
            2 to 2, 3 to 3, 4 to 4
        )
        squares.forEach { (x, y) ->
            drawRect(
                color = color,
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell * 0.72f, cell * 0.72f)
            )
        }
    }
}
```

- [ ] **Step 5: Route Welcome stage**

Update `MatterSetupApp.kt` `when`:

```kotlin
MatterSetupStage.Welcome -> WelcomeScreen(
    state = state,
    onAction = onAction
)
```

Keep `ReadyToScan`, `ScanningQr`, and `QrScanned` routed to `ScanDeviceScreen`.

- [ ] **Step 6: Run UI tests for welcome/main screen**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: welcome and add-device tests PASS. Settings-related UI tests may still fail until Task 5.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/ui/WelcomeScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt
git commit -m "feat: refresh welcome and add-device screens"
```

---

### Task 5: Required Setup, Settings, Token, And Thread Editor Screens

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/SettingsScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/ChangeTokenScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/ThreadNetworkEditorScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`
- Test: `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

- [ ] **Step 1: Add UI tests for setup/settings/thread/token screens**

Add tests:

```kotlin
@Test
fun requiredSetupScreenShowsAllRequiredFields() {
    render(MatterSetupStateReducer.requiredSetup("http://openhab:8080"))

    composeRule.onNodeWithText("Connect to openHAB").assertIsDisplayed()
    composeRule.onNodeWithText("openHAB address").assertIsDisplayed()
    composeRule.onNodeWithText("Address of your openHAB instance.").assertIsDisplayed()
    composeRule.onNodeWithText("Access token").assertIsDisplayed()
    composeRule.onNodeWithText("Active Operational Dataset").assertIsDisplayed()
    composeRule.onNodeWithText("Thread Border Router address").assertIsDisplayed()
    composeRule.onNodeWithText("Detect border router").assertIsDisplayed()
    composeRule.onNodeWithText("Test settings").assertIsDisplayed()
}

@Test
fun settingsScreenShowsOrganizedSections() {
    render(MatterSetupStateReducer.settings())

    composeRule.onNodeWithText("openHAB connection").assertIsDisplayed()
    composeRule.onNodeWithText("Access token").assertIsDisplayed()
    composeRule.onNodeWithText("Set").assertIsDisplayed()
    composeRule.onNodeWithText("Change token").assertIsDisplayed()
    composeRule.onNodeWithText("Thread network").assertIsDisplayed()
    composeRule.onNodeWithText("Edit").assertIsDisplayed()
    composeRule.onNodeWithText("This phone").assertIsDisplayed()
    composeRule.onNodeWithText("Advanced").assertIsDisplayed()
}

@Test
fun settingsEditThreadDispatchesAction() {
    val actions = mutableListOf<MatterSetupAction>()
    render(MatterSetupStateReducer.settings(), onAction = actions::add)

    composeRule.onNodeWithText("Edit").performClick()

    assertTrue(actions.contains(MatterSetupAction.EditThreadNetwork))
}

@Test
fun threadNetworkEditorShowsDatasetRouterAndSave() {
    render(MatterSetupStateReducer.threadNetworkEditor())

    composeRule.onNodeWithText("Thread network").assertIsDisplayed()
    composeRule.onNodeWithText("Active Operational Dataset").assertIsDisplayed()
    composeRule.onNodeWithText("Thread Border Router address").assertIsDisplayed()
    composeRule.onNodeWithText("Detect border router").assertIsDisplayed()
    composeRule.onNodeWithText("Save").assertIsDisplayed()
}

@Test
fun changeTokenScreenShowsTokenReplacementControls() {
    render(MatterSetupStateReducer.changeToken())

    composeRule.onNodeWithText("Change token").assertIsDisplayed()
    composeRule.onNodeWithText("Access token").assertIsDisplayed()
    composeRule.onNodeWithText("Save token").assertIsDisplayed()
}
```

- [ ] **Step 2: Run UI tests and confirm failure**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: FAIL because new screens are missing or old setup screen text remains.

- [ ] **Step 3: Replace OpenHabSetupScreen with required setup UI**

Keep the existing function signature. Replace the body with a `MatterSetupScaffold` containing all fields, the revealable token/dataset controls using `OutlinedTextField` trailing icons with text `👁`, the `Detect border router` button, selected router list, and a `Test settings` button:

```kotlin
MatterSetupScaffold(
    title = state.title,
    message = state.message,
    showBack = false
) {
    OutlinedTextField(
        value = openHabUrl.ifBlank { state.openHabUrlFallback },
        onValueChange = onUrlChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("openHAB address") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    Text("Address of your openHAB instance.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = token,
        onValueChange = onTokenChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Access token") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { Text("👁") }
    )
    Text("Create one in openHAB under Profile / API tokens.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(
        value = threadDataset,
        onValueChange = onThreadDatasetChange,
        modifier = Modifier.fillMaxWidth().height(132.dp),
        label = { Text("Active Operational Dataset") },
        visualTransformation = PasswordVisualTransformation(),
        trailingIcon = { Text("👁") }
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = otbrBaseUrl,
        onValueChange = onOtbrBaseUrlChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Thread Border Router address") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        enabled = !threadBorderRouterDiscoveryInProgress,
        onClick = { onAction(MatterSetupAction.DetectThreadBorderRouters) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (threadBorderRouterDiscoveryInProgress) "Detecting..." else "Detect border router")
    }
    threadBorderRouters.forEach { router ->
        TextButton(
            onClick = { onAction(MatterSetupAction.SelectThreadBorderRouter(router.endpoint)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${router.displayName} - ${router.endpoint}")
        }
    }
    if (threadSettingsMessage.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(threadSettingsMessage, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(18.dp))
    Button(
        enabled = state.primaryActionEnabled,
        onClick = { onAction(MatterSetupAction.TestSettings) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(state.primaryActionLabel.ifBlank { "Test settings" })
    }
}
```

Keep imports minimal and remove obsolete `Back to main menu`, `Devices on this phone`, and inline advanced Thread buttons from this first-run setup screen.

- [ ] **Step 4: Create SettingsScreen**

Create `SettingsScreen.kt` with sections and actions exactly as tested:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun SettingsScreen(
    state: MatterSetupUiState,
    openHabUrl: String,
    tokenSet: Boolean,
    threadDatasetSet: Boolean,
    otbrBaseUrl: String,
    phoneDeviceCount: Int,
    attestationBypassEnabled: Boolean,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(title = state.title, message = state.message, showBack = true, onBack = {
        onAction(MatterSetupAction.BackToMainMenu)
    }) {
        SectionLabel("openHAB connection")
        SettingsCard {
            SettingsRow("openHAB address", openHabUrl, "Ready")
            SettingsRow("Access token", "Stored securely. Token is hidden after saving.", if (tokenSet) "Set" else "Missing")
            RowButtons("Test", "Change token", { onAction(MatterSetupAction.TestOpenHab) }, { onAction(MatterSetupAction.ChangeToken) })
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel("Thread network")
        SettingsCard {
            SettingsRow("Active Operational Dataset", if (threadDatasetSet) "Stored encrypted for Thread commissioning." else "Missing", if (threadDatasetSet) "Valid" else "Missing")
            SettingsRow("Thread Border Router", otbrBaseUrl.ifBlank { "Not set" }, if (otbrBaseUrl.isBlank()) "Missing" else "Set")
            RowButtons("Edit", "Detect router", { onAction(MatterSetupAction.EditThreadNetwork) }, { onAction(MatterSetupAction.DetectThreadBorderRouters) })
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel("This phone")
        SettingsCard {
            SettingsRow("Devices on this phone", "View staged Matter devices and retry openHAB handoff.", "$phoneDeviceCount staged")
            RowButtons("View devices", "Troubleshoot", { onAction(MatterSetupAction.ShowPhoneDevices) }, { onAction(MatterSetupAction.ShowTroubleshooting) })
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel("Advanced")
        SettingsCard {
            SettingsRow("Developer attestation bypass", "For development devices only.", if (attestationBypassEnabled) "On" else "Off")
            SettingsRow("Network diagnostics", "mDNS, IPv6, and openHAB readiness checks.", "")
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (status.isNotBlank()) {
            Text(status, color = if (status == "Set" || status == "Ready" || status == "Valid") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowButtons(
    left: String,
    right: String,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onLeft, modifier = Modifier.weight(1f)) { Text(left) }
        Spacer(Modifier.padding(5.dp))
        OutlinedButton(onClick = onRight, modifier = Modifier.weight(1f)) { Text(right) }
    }
}
```

- [ ] **Step 5: Create ChangeTokenScreen**

Create `ChangeTokenScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold

@Composable
fun ChangeTokenScreen(
    state: MatterSetupUiState,
    token: String,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(title = state.title, message = state.message, showBack = true, onBack = {
        onAction(MatterSetupAction.BackToSettings)
    }) {
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { Text("👁") }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = token.isNotBlank() && state.primaryActionEnabled,
            onClick = { onAction(MatterSetupAction.SaveChangedToken) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Save token" })
        }
    }
}
```

- [ ] **Step 6: Create ThreadNetworkEditorScreen**

Create `ThreadNetworkEditorScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun ThreadNetworkEditorScreen(
    state: MatterSetupUiState,
    threadDataset: String,
    otbrBaseUrl: String,
    threadSettingsMessage: String,
    threadBorderRouters: List<ThreadBorderRouterRecord>,
    threadBorderRouterDiscoveryInProgress: Boolean,
    onThreadDatasetChange: (String) -> Unit,
    onOtbrBaseUrlChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToSettings) }
    ) {
        SectionLabel("Network credentials")
        SettingsCard {
            OutlinedTextField(
                value = threadDataset,
                onValueChange = onThreadDatasetChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                label = { Text("Active Operational Dataset") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = { Text("👁") }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Paste the Active Operational Dataset from your Thread Border Router. It is stored encrypted and hidden after saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (threadSettingsMessage.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = threadSettingsMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.CheckThreadDataset) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check dataset")
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Border Router")
        SettingsCard {
            OutlinedTextField(
                value = otbrBaseUrl,
                onValueChange = onOtbrBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Thread Border Router address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Used for diagnostics and readiness checks. IPv6 Thread reachability is required for openHAB pairing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                enabled = !threadBorderRouterDiscoveryInProgress,
                onClick = { onAction(MatterSetupAction.DetectThreadBorderRouters) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (threadBorderRouterDiscoveryInProgress) "Detecting..." else "Detect border router")
            }
            threadBorderRouters.forEach { router ->
                TextButton(
                    onClick = { onAction(MatterSetupAction.SelectThreadBorderRouter(router.endpoint)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${router.displayName} - ${router.endpoint}")
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.BackToSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onAction(MatterSetupAction.SaveThreadSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Save" })
        }
    }
}
```

- [ ] **Step 7: Route new settings screens in MatterSetupApp**

Add cases:

```kotlin
MatterSetupStage.Settings -> SettingsScreen(
    state = state,
    openHabUrl = openHabUrl,
    tokenSet = token.isBlank(),
    threadDatasetSet = threadDataset.isNotBlank(),
    otbrBaseUrl = otbrBaseUrl,
    phoneDeviceCount = phoneDevices.size,
    attestationBypassEnabled = attestationBypassEnabled,
    onAction = onAction
)

MatterSetupStage.ChangeToken -> ChangeTokenScreen(
    state = state,
    token = token,
    onTokenChange = onTokenChange,
    onAction = onAction
)

MatterSetupStage.ThreadNetworkEditor -> ThreadNetworkEditorScreen(
    state = state,
    threadDataset = threadDataset,
    otbrBaseUrl = otbrBaseUrl,
    threadSettingsMessage = threadSettingsMessage,
    threadBorderRouters = threadBorderRouters,
    threadBorderRouterDiscoveryInProgress = threadBorderRouterDiscoveryInProgress,
    onThreadDatasetChange = onThreadDatasetChange,
    onOtbrBaseUrlChange = onOtbrBaseUrlChange,
    onAction = onAction
)
```

- [ ] **Step 8: Run UI tests and verify pass**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: PASS for setup/settings/thread/token tests. Fix import or duplicate text issues before continuing.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/SettingsScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/ChangeTokenScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/ThreadNetworkEditorScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt
git commit -m "feat: add organized setup settings screens"
```

---

### Task 6: Remove Visible Back-To-Main Buttons And Final Verification

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/ManualCodeScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceListScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt`
- Test: `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

- [ ] **Step 1: Update UI tests for absent visible back buttons**

Import:

```kotlin
import androidx.compose.ui.test.assertDoesNotExist
```

Update manual, phone devices, and troubleshooting tests:

```kotlin
composeRule.onNodeWithText("Back to main menu").assertDoesNotExist()
composeRule.onNodeWithText("Back").assertDoesNotExist()
```

Keep `pressBack()` tests to confirm system back dispatches the right action.

- [ ] **Step 2: Run UI tests and confirm failure**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: FAIL because old visible back buttons still exist.

- [ ] **Step 3: Remove ManualCodeScreen visible Back button**

Delete the `OutlinedButton` that dispatches `MatterSetupAction.BackToMainMenu` from `ManualCodeScreen.kt`. Keep the `Continue` button unchanged.

- [ ] **Step 4: Remove PhoneDeviceListScreen visible back buttons**

Delete the `OutlinedButton` blocks labeled `Back to settings` and `Back to main menu` from `PhoneDeviceListScreen.kt`. Keep staged-device actions unchanged.

- [ ] **Step 5: Remove AdvancedTroubleshootingScreen duplicate navigation**

Remove the top and bottom buttons whose only purpose is `Back to setup` or `Back to settings`. Keep real diagnostic actions:

```kotlin
OutlinedButton(onClick = { onAction(MatterSetupAction.BrowseMatterServices) }) { Text("Browse Matter services") }
OutlinedButton(onClick = { onAction(MatterSetupAction.CheckIpv6Reachability) }) { Text("Check IPv6 reachability") }
OutlinedButton(onClick = { onAction(MatterSetupAction.ShowPhoneDevices) }) { Text("Devices on this phone") }
```

- [ ] **Step 6: Run full unit and assemble verification**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 7: Run Compose UI verification**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: PASS on an attached emulator/device. If no device is attached, record that this verification was not run and run `:app:testDebugUnitTest :app:assembleDebug` instead.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/kotlin/org/openhab/matter/companion/ui/ManualCodeScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/PhoneDeviceListScreen.kt app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt
git commit -m "feat: rely on system back in setup subpages"
```

---

## Final Verification

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: PASS.

If a device or emulator is attached, also run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.openhab.matter.companion.ui.MatterSetupAppTest
```

Expected: PASS.

Manual smoke checklist:

- Fresh install shows Get started.
- Get started opens required setup with `http://openhab:8080`.
- Test settings saves only when openHAB, token, dataset, and Border Router checks pass.
- Success lands on Add Matter device.
- Add Matter device shows scan image, cog icon, `Scan code`, and `Enter code manually`.
- Cog opens organized Settings.
- Settings `Change token` never displays the stored token.
- Settings Thread `Edit` opens focused Thread editor.
- Android system back returns from Settings, Thread editor, Change token, Devices on this phone, Advanced troubleshooting, and manual code entry.
- Real commissioning still fails closed when connectedhomeip readiness is unavailable.
