# Compose Automated Matter Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Kotlin/Jetpack Compose automated Matter setup flow that scans a device QR code, commissions the device to the phone, opens an OpenCommissioningWindow, sends the returned code to openHAB, and succeeds when a Matter Inbox entry is detected.

**Architecture:** Add a Kotlin workflow/state layer above the existing Java Matter/openHAB services, then render that state with Compose. Keep connectedhomeip, REST, storage, and diagnostics outside Compose so the workflow can later move toward official openHAB Android integration.

**Tech Stack:** Android Java 17, Kotlin 2.1.21, Jetpack Compose with Compose Compiler Gradle plugin, Material 3, existing Java connectedhomeip/openHAB clients, JUnit 4, Robolectric.

---

## Scope And File Map

Specification: `docs/superpowers/specs/2026-05-28-compose-automated-matter-setup-design.md`

Existing design dependency: `docs/superpowers/specs/2026-05-28-openhab-auto-matter-scan-design.md`

Before implementation, inspect dirty files:

```powershell
git status --short
```

Work with user changes. Do not revert unrelated dirty files. Stage only files touched by each task.

Create Kotlin setup workflow package:

- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt` - finite workflow stages.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStepStatus.kt` - step status enum.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStep.kt` - user-facing progress step.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt` - immutable UI state rendered by Compose.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt` - UI actions sent into workflow/activity.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupFailure.kt` - sanitized failure model.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupDiagnosticsSummary.kt` - automatic diagnostics summary.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfig.kt` - sanitized workflow config.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupPorts.kt` - ports that adapt existing Java services.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflow.kt` - pure workflow state machine.
- `app/src/main/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdown.kt` - countdown math.
- `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflowTest.kt`
- `app/src/test/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdownTest.kt`

Create Android adapters:

- `app/src/main/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPorts.kt` - adapter from workflow ports to existing Java services.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/ReadinessDiagnostic.kt` - diagnostic result model.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidReadinessProbe.kt` - Bluetooth, permission, network, VPN, and location-service checks.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummary.kt` - pure transport summary.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecord.kt` - Matter DNS-SD result model.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsBrowser.kt` - browse abstraction for `_matterc._udp` and `_matter._tcp`.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidMatterMdnsBrowser.kt` - Android NSD implementation.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/Ipv6ReachabilityProbe.kt` - reachability abstraction.
- `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbe.kt` - Java reachability implementation.
- `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummaryTest.kt`
- `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecordTest.kt`
- `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbeTest.kt`

Create Compose UI:

- `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupActivity.kt` - new main Activity using Compose.
- `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt` - top-level Compose entry.
- `app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/PairingModeScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupProgressScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupFailureScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupSuccessScreen.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/components/SetupStepList.kt`
- `app/src/main/kotlin/org/openhab/matter/companion/ui/theme/MatterSetupTheme.kt`
- `app/src/androidTest/kotlin/org/openhab/matter/companion/ui/MatterSetupAppTest.kt`

Modify:

- `build.gradle` - add Kotlin Android and Compose compiler plugins.
- `app/build.gradle` - apply Kotlin/Compose plugins and dependencies.
- `app/src/main/AndroidManifest.xml` - point launcher activity to `MatterSetupActivity`.
- `app/src/main/java/org/openhab/matter/companion/MainActivity.java` - keep legacy class in place but stop using it as launcher.
- `docs/implementation-status.md` - update after implementation and verification.
- `docs/open-commissioning-window-workflow.md` - update after implementation and verification.

---

### Task 1: Kotlin And Compose Build Foundation

**Files:**
- Modify: `build.gradle`
- Modify: `app/build.gradle`
- Create: `app/src/test/kotlin/org/openhab/matter/companion/setup/KotlinBuildSmokeTest.kt`

- [ ] **Step 1: Add a failing Kotlin test**

Create `app/src/test/kotlin/org/openhab/matter/companion/setup/KotlinBuildSmokeTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinBuildSmokeTest {
    @Test
    fun kotlinTestsRun() {
        assertEquals("openHAB", "open" + "HAB")
    }
}
```

- [ ] **Step 2: Run the Kotlin test to verify the project does not compile Kotlin yet**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*KotlinBuildSmokeTest"
```

Expected: FAIL because the project has no Kotlin Android plugin/source support yet, or because the Kotlin test is ignored/not compiled.

- [ ] **Step 3: Add Kotlin and Compose plugins**

Modify root `build.gradle` to:

```groovy
plugins {
    id "com.android.application" version "8.11.1" apply false
    id "org.jetbrains.kotlin.android" version "2.1.21" apply false
    id "org.jetbrains.kotlin.plugin.compose" version "2.1.21" apply false
}
```

Modify `app/build.gradle` plugin block to:

```groovy
plugins {
    id "com.android.application"
    id "org.jetbrains.kotlin.android"
    id "org.jetbrains.kotlin.plugin.compose"
}
```

Inside `android { ... }`, after `compileOptions`, add:

```groovy
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose true
    }
```

In `dependencies { ... }`, add:

```groovy
    def compose_bom_version = "2025.05.00"
    implementation platform("androidx.compose:compose-bom:$compose_bom_version")
    implementation "androidx.activity:activity-compose:1.10.1"
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation "androidx.compose.material3:material3"
    debugImplementation "androidx.compose.ui:ui-tooling"

    testImplementation "org.jetbrains.kotlin:kotlin-test-junit:2.1.21"

    androidTestImplementation platform("androidx.compose:compose-bom:$compose_bom_version")
    androidTestImplementation "androidx.compose.ui:ui-test-junit4"
    androidTestImplementation "androidx.test.ext:junit:1.2.1"
    androidTestImplementation "androidx.test.espresso:espresso-core:3.6.1"
    debugImplementation "androidx.compose.ui:ui-test-manifest"
```

Change `defaultConfig.testInstrumentationRunner` to:

```groovy
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
```

- [ ] **Step 4: Run the Kotlin smoke test**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*KotlinBuildSmokeTest"
```

Expected: PASS.

- [ ] **Step 5: Run assemble to verify Compose dependencies resolve**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```powershell
git add build.gradle app\build.gradle app\src\test\kotlin\org\openhab\matter\companion\setup\KotlinBuildSmokeTest.kt
git commit -m "Add Kotlin and Compose build support"
```

---

### Task 2: Workflow State Model

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStepStatus.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStep.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupFailure.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupDiagnosticsSummary.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupUiStateTest.kt`

- [ ] **Step 1: Write the failing state model tests**

Create `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupUiStateTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupUiStateTest {
    @Test
    fun initialStateRequiresOpenHabSetupWhenNotConfigured() {
        val state = MatterSetupUiState.initial(openHabConfigured = false)

        assertEquals(MatterSetupStage.NeedsOpenHabSetup, state.stage)
        assertEquals("Connect to openHAB", state.title)
        assertFalse(state.primaryActionEnabled)
        assertNull(state.countdownSeconds)
    }

    @Test
    fun progressStateContainsStableStepLabels() {
        val state = MatterSetupUiState.progress(MatterSetupStage.OpeningCommissioningWindow, countdownSeconds = 300)

        assertEquals("Setting up your device", state.title)
        assertEquals(6, state.steps.size)
        assertEquals("Checking setup", state.steps[0].label)
        assertEquals("Opening pairing window", state.steps[3].label)
        assertEquals(MatterSetupStepStatus.Active, state.steps[3].status)
        assertEquals(300, state.countdownSeconds)
    }

    @Test
    fun failureStateDoesNotExposeSensitiveValues() {
        val failure = MatterSetupFailure(
            step = MatterSetupStage.SendingCodeToOpenHab,
            message = "openHAB could not start pairing",
            details = "HTTP 401 for redacted request"
        )

        val state = MatterSetupUiState.failed(failure, MatterSetupDiagnosticsSummary.empty())

        assertEquals(MatterSetupStage.Failed, state.stage)
        assertTrue(state.secondaryActions.contains(MatterSetupAction.ShowTroubleshooting))
        assertFalse(state.toString().contains("34970112332"))
        assertFalse(state.toString().contains("ohab_"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupUiStateTest"
```

Expected: FAIL because the setup model classes do not exist.

- [ ] **Step 3: Add workflow stage enum**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStage.kt`:

```kotlin
package org.openhab.matter.companion.setup

enum class MatterSetupStage {
    NeedsOpenHabSetup,
    OpenHabSetupChecking,
    ReadyToScan,
    ScanningQr,
    QrScanned,
    ReadinessChecking,
    NeedsPairingMode,
    CommissioningToPhone,
    OpeningCommissioningWindow,
    CommissioningWindowOpen,
    SendingCodeToOpenHab,
    WatchingOpenHabInbox,
    SuccessInboxDetected,
    Failed,
    AdvancedTroubleshooting
}
```

- [ ] **Step 4: Add step status and step model**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStepStatus.kt`:

```kotlin
package org.openhab.matter.companion.setup

enum class MatterSetupStepStatus {
    Pending,
    Active,
    Complete,
    Failed
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupStep.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class MatterSetupStep(
    val label: String,
    val status: MatterSetupStepStatus,
    val detail: String = ""
)
```

- [ ] **Step 5: Add failure and diagnostics models**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupFailure.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class MatterSetupFailure(
    val step: MatterSetupStage,
    val message: String,
    val details: String = "",
    val suggestions: List<String> = defaultSuggestions(step)
) {
    companion object {
        fun defaultSuggestions(step: MatterSetupStage): List<String> = when (step) {
            MatterSetupStage.ScanningQr,
            MatterSetupStage.QrScanned -> listOf("Scan the Matter QR code again.", "Enter the setup code manually.")

            MatterSetupStage.CommissioningToPhone -> listOf(
                "Keep the phone near the device.",
                "Confirm the device is still in pairing mode.",
                "Check Thread network and IPv6 routing if BLE already connected."
            )

            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen -> listOf(
                "Open the pairing window again.",
                "Run setup again if this phone no longer has the device staged."
            )

            MatterSetupStage.SendingCodeToOpenHab -> listOf(
                "Check the openHAB address and access token.",
                "Check that the openHAB Matter controller Thing is online."
            )

            MatterSetupStage.WatchingOpenHabInbox -> listOf(
                "Check IPv6 routing between openHAB and the Thread network.",
                "Check mDNS or Avahi on the openHAB side.",
                "Open the pairing window again if the countdown expired."
            )

            else -> listOf("Try again.", "Open troubleshooting for more checks.")
        }
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupDiagnosticsSummary.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class MatterSetupDiagnosticsSummary(
    val checks: List<String>,
    val warnings: List<String>,
    val details: List<String>
) {
    companion object {
        fun empty(): MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary(
            checks = emptyList(),
            warnings = emptyList(),
            details = emptyList()
        )
    }
}
```

- [ ] **Step 6: Add action model**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupAction.kt`:

```kotlin
package org.openhab.matter.companion.setup

sealed interface MatterSetupAction {
    data object SaveOpenHab : MatterSetupAction
    data object TestOpenHab : MatterSetupAction
    data object StartScan : MatterSetupAction
    data object EnterCodeManually : MatterSetupAction
    data object ConfirmPairingMode : MatterSetupAction
    data object Retry : MatterSetupAction
    data object OpenCommissioningWindowAgain : MatterSetupAction
    data object ShowTroubleshooting : MatterSetupAction
    data object ForgetFromPhone : MatterSetupAction
    data object AddAnotherDevice : MatterSetupAction
}
```

- [ ] **Step 7: Add UI state model**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupUiState.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class MatterSetupUiState(
    val stage: MatterSetupStage,
    val title: String,
    val message: String,
    val steps: List<MatterSetupStep> = emptyList(),
    val countdownSeconds: Int? = null,
    val primaryAction: MatterSetupAction? = null,
    val primaryActionLabel: String = "",
    val primaryActionEnabled: Boolean = true,
    val secondaryActions: List<MatterSetupAction> = emptyList(),
    val failure: MatterSetupFailure? = null,
    val diagnostics: MatterSetupDiagnosticsSummary = MatterSetupDiagnosticsSummary.empty()
) {
    companion object {
        fun initial(openHabConfigured: Boolean): MatterSetupUiState {
            return if (openHabConfigured) {
                MatterSetupUiState(
                    stage = MatterSetupStage.ReadyToScan,
                    title = "Add Matter device",
                    message = "Scan the Matter QR code to add the device to openHAB.",
                    primaryAction = MatterSetupAction.StartScan,
                    primaryActionLabel = "Scan QR code"
                )
            } else {
                MatterSetupUiState(
                    stage = MatterSetupStage.NeedsOpenHabSetup,
                    title = "Connect to openHAB",
                    message = "Connect to your openHAB home before adding Matter devices.",
                    primaryAction = MatterSetupAction.TestOpenHab,
                    primaryActionLabel = "Test connection",
                    primaryActionEnabled = false
                )
            }
        }

        fun progress(stage: MatterSetupStage, countdownSeconds: Int? = null): MatterSetupUiState {
            return MatterSetupUiState(
                stage = stage,
                title = "Setting up your device",
                message = progressMessage(stage),
                steps = progressSteps(stage),
                countdownSeconds = countdownSeconds,
                secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting)
            )
        }

        fun failed(
            failure: MatterSetupFailure,
            diagnostics: MatterSetupDiagnosticsSummary
        ): MatterSetupUiState {
            return MatterSetupUiState(
                stage = MatterSetupStage.Failed,
                title = "Setup needs attention",
                message = failure.message,
                failure = failure,
                diagnostics = diagnostics,
                primaryAction = MatterSetupAction.Retry,
                primaryActionLabel = "Try again",
                secondaryActions = listOf(MatterSetupAction.ShowTroubleshooting)
            )
        }

        private fun progressMessage(stage: MatterSetupStage): String = when (stage) {
            MatterSetupStage.ReadinessChecking -> "Checking your phone, network, and openHAB."
            MatterSetupStage.CommissioningToPhone -> "Adding the device to this phone first."
            MatterSetupStage.OpeningCommissioningWindow -> "Preparing the device for openHAB."
            MatterSetupStage.CommissioningWindowOpen -> "The device is ready for openHAB."
            MatterSetupStage.SendingCodeToOpenHab -> "Sending the setup code to openHAB."
            MatterSetupStage.WatchingOpenHabInbox -> "Waiting for openHAB to find the device."
            else -> "This usually takes less than a minute."
        }

        private fun progressSteps(activeStage: MatterSetupStage): List<MatterSetupStep> {
            val stages = listOf(
                MatterSetupStage.ReadinessChecking to "Checking setup",
                MatterSetupStage.CommissioningToPhone to "Connecting to device",
                MatterSetupStage.CommissioningToPhone to "Adding device to this phone",
                MatterSetupStage.OpeningCommissioningWindow to "Opening pairing window",
                MatterSetupStage.SendingCodeToOpenHab to "Sending setup code to openHAB",
                MatterSetupStage.WatchingOpenHabInbox to "Waiting for openHAB"
            )
            val activeIndex = stages.indexOfFirst { it.first == activeStage }.coerceAtLeast(0)
            return stages.mapIndexed { index, (_, label) ->
                MatterSetupStep(
                    label = label,
                    status = when {
                        index < activeIndex -> MatterSetupStepStatus.Complete
                        index == activeIndex -> MatterSetupStepStatus.Active
                        else -> MatterSetupStepStatus.Pending
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 8: Run tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupUiStateTest"
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\setup app\src\test\kotlin\org\openhab\matter\companion\setup\MatterSetupUiStateTest.kt
git commit -m "Add Matter setup workflow state model"
```

---

### Task 3: Countdown Model

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdown.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdownTest.kt`

- [ ] **Step 1: Write failing countdown tests**

Create `app/src/test/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdownTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class CommissioningWindowCountdownTest {
    @Test
    fun remainingSecondsCountsDownFromTimeout() {
        val countdown = CommissioningWindowCountdown(openedAtMillis = 1_000L, timeoutSeconds = 300)

        assertEquals(300, countdown.remainingSeconds(nowMillis = 1_000L))
        assertEquals(299, countdown.remainingSeconds(nowMillis = 1_001L))
        assertEquals(240, countdown.remainingSeconds(nowMillis = 61_000L))
    }

    @Test
    fun remainingSecondsNeverGoesNegative() {
        val countdown = CommissioningWindowCountdown(openedAtMillis = 1_000L, timeoutSeconds = 300)

        assertEquals(0, countdown.remainingSeconds(nowMillis = 302_000L))
    }

    @Test
    fun displayTextFormatsMinutesAndSeconds() {
        assertEquals("Pairing window open for 5:00", CommissioningWindowCountdown.displayText(300))
        assertEquals("Pairing window open for 0:09", CommissioningWindowCountdown.displayText(9))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*CommissioningWindowCountdownTest"
```

Expected: FAIL because `CommissioningWindowCountdown` does not exist.

- [ ] **Step 3: Add countdown implementation**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/CommissioningWindowCountdown.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class CommissioningWindowCountdown(
    val openedAtMillis: Long,
    val timeoutSeconds: Int
) {
    fun remainingSeconds(nowMillis: Long): Int {
        val elapsedMillis = (nowMillis - openedAtMillis).coerceAtLeast(0L)
        val elapsedSeconds = elapsedMillis / 1_000L
        return (timeoutSeconds - elapsedSeconds).coerceAtLeast(0L).toInt()
    }

    companion object {
        fun displayText(remainingSeconds: Int): String {
            val safeSeconds = remainingSeconds.coerceAtLeast(0)
            val minutes = safeSeconds / 60
            val seconds = safeSeconds % 60
            return "Pairing window open for $minutes:${seconds.toString().padStart(2, '0')}"
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*CommissioningWindowCountdownTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\setup\CommissioningWindowCountdown.kt app\src\test\kotlin\org\openhab\matter\companion\setup\CommissioningWindowCountdownTest.kt
git commit -m "Add commissioning window countdown model"
```

---

### Task 4: Workflow Ports And Happy Path State Machine

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfig.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupPorts.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflow.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflowTest.kt`

- [ ] **Step 1: Write failing happy path workflow test**

Create `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflowTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatterSetupWorkflowTest {
    @Test
    fun automatedFlowSucceedsWhenInboxEntryIsDetected() {
        val ports = FakeMatterSetupPorts()
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        assertEquals(MatterSetupStage.SuccessInboxDetected, states.last().stage)
        assertEquals("Device found by openHAB", states.last().title)
        assertTrue(ports.commissionCalled)
        assertTrue(ports.openWindowCalled)
        assertTrue(ports.scanCalled)
        assertTrue(ports.inboxCalled)
        assertFalse(states.joinToString("\n").contains("34970112332"))
    }

    private class FakeMatterSetupPorts : MatterSetupPorts {
        var commissionCalled = false
        var openWindowCalled = false
        var scanCalled = false
        var inboxCalled = false

        override fun loadConfig(): MatterSetupConfig = MatterSetupConfig(
            openHabBaseUrl = "http://openhab.local:8080",
            openHabApiToken = "ohab_secret",
            threadDataset = "hex:001122",
            otbrBaseUrl = "http://otbr.local",
            attestationBypassEnabled = false
        )

        override fun checkReadiness(config: MatterSetupConfig): MatterSetupPorts.ReadinessResult {
            return MatterSetupPorts.ReadinessResult(ready = true, details = listOf("openHAB connected"))
        }

        override fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): MatterSetupPorts.CommissionResult {
            commissionCalled = true
            return MatterSetupPorts.CommissionResult(nodeId = 1234L, controllerState = "controller-state")
        }

        override fun openCommissioningWindow(nodeId: Long, controllerState: String): MatterSetupPorts.OpenWindowResult {
            openWindowCalled = true
            return MatterSetupPorts.OpenWindowResult(
                manualCode = "34970112332",
                qrCode = "",
                controllerState = "controller-state-2",
                timeoutSeconds = 300
            )
        }

        override fun sendCodeToOpenHab(manualCode: String, config: MatterSetupConfig): MatterSetupPorts.OpenHabScanResult {
            scanCalled = true
            return MatterSetupPorts.OpenHabScanResult(started = true, timeoutSeconds = 120, details = "scan accepted")
        }

        override fun waitForOpenHabInbox(config: MatterSetupConfig, timeoutSeconds: Int): MatterSetupPorts.InboxResult {
            inboxCalled = true
            return MatterSetupPorts.InboxResult(matterEntryDetected = true, details = "Matter Inbox entry detected")
        }

        override fun runDiagnostics(failure: MatterSetupFailure, config: MatterSetupConfig): MatterSetupDiagnosticsSummary {
            return MatterSetupDiagnosticsSummary.empty()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupWorkflowTest"
```

Expected: FAIL because `MatterSetupConfig`, `MatterSetupPorts`, and `MatterSetupWorkflow` do not exist.

- [ ] **Step 3: Add config model**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupConfig.kt`:

```kotlin
package org.openhab.matter.companion.setup

data class MatterSetupConfig(
    val openHabBaseUrl: String,
    val openHabApiToken: String,
    val threadDataset: String,
    val otbrBaseUrl: String,
    val attestationBypassEnabled: Boolean
) {
    val openHabConfigured: Boolean
        get() = openHabBaseUrl.isNotBlank()
}
```

- [ ] **Step 4: Add ports contract**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupPorts.kt`:

```kotlin
package org.openhab.matter.companion.setup

interface MatterSetupPorts {
    fun loadConfig(): MatterSetupConfig

    fun checkReadiness(config: MatterSetupConfig): ReadinessResult

    fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): CommissionResult

    fun openCommissioningWindow(nodeId: Long, controllerState: String): OpenWindowResult

    fun sendCodeToOpenHab(manualCode: String, config: MatterSetupConfig): OpenHabScanResult

    fun waitForOpenHabInbox(config: MatterSetupConfig, timeoutSeconds: Int): InboxResult

    fun runDiagnostics(failure: MatterSetupFailure, config: MatterSetupConfig): MatterSetupDiagnosticsSummary

    data class ReadinessResult(
        val ready: Boolean,
        val details: List<String>,
        val warnings: List<String> = emptyList()
    )

    data class CommissionResult(
        val nodeId: Long,
        val controllerState: String
    )

    data class OpenWindowResult(
        val manualCode: String,
        val qrCode: String,
        val controllerState: String,
        val timeoutSeconds: Int
    )

    data class OpenHabScanResult(
        val started: Boolean,
        val timeoutSeconds: Int,
        val details: String
    )

    data class InboxResult(
        val matterEntryDetected: Boolean,
        val details: String
    )
}
```

- [ ] **Step 5: Add workflow implementation**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflow.kt`:

```kotlin
package org.openhab.matter.companion.setup

class MatterSetupWorkflow(
    private val ports: MatterSetupPorts,
    private val emit: (MatterSetupUiState) -> Unit
) {
    fun startAutomatedSetup(setupPayload: String) {
        val config = ports.loadConfig()
        if (!config.openHabConfigured) {
            emit(MatterSetupUiState.initial(openHabConfigured = false))
            return
        }

        runCatching {
            emit(MatterSetupUiState.progress(MatterSetupStage.ReadinessChecking))
            val readiness = ports.checkReadiness(config)
            if (!readiness.ready) {
                fail(MatterSetupStage.ReadinessChecking, "Setup is not ready yet", readiness.warnings.joinToString("; "), config)
                return
            }

            emit(MatterSetupUiState.progress(MatterSetupStage.CommissioningToPhone))
            val commission = ports.commissionToPhone(setupPayload, config)

            emit(MatterSetupUiState.progress(MatterSetupStage.OpeningCommissioningWindow))
            val window = ports.openCommissioningWindow(commission.nodeId, commission.controllerState)
            emit(MatterSetupUiState.progress(MatterSetupStage.CommissioningWindowOpen, window.timeoutSeconds))

            emit(MatterSetupUiState.progress(MatterSetupStage.SendingCodeToOpenHab, window.timeoutSeconds))
            val scan = ports.sendCodeToOpenHab(window.manualCode, config)
            if (!scan.started) {
                fail(MatterSetupStage.SendingCodeToOpenHab, "openHAB could not start pairing", scan.details, config)
                return
            }

            emit(MatterSetupUiState.progress(MatterSetupStage.WatchingOpenHabInbox, window.timeoutSeconds))
            val inbox = ports.waitForOpenHabInbox(config, scan.timeoutSeconds)
            if (!inbox.matterEntryDetected) {
                fail(MatterSetupStage.WatchingOpenHabInbox, "openHAB did not report the device yet", inbox.details, config)
                return
            }

            emit(
                MatterSetupUiState(
                    stage = MatterSetupStage.SuccessInboxDetected,
                    title = "Device found by openHAB",
                    message = "openHAB reported a Matter Inbox entry for this device.",
                    primaryAction = MatterSetupAction.AddAnotherDevice,
                    primaryActionLabel = "Add another device"
                )
            )
        }.onFailure { error ->
            fail(MatterSetupStage.Failed, "Setup could not finish", error.message.orEmpty(), config)
        }
    }

    private fun fail(stage: MatterSetupStage, message: String, details: String, config: MatterSetupConfig) {
        val failure = MatterSetupFailure(step = stage, message = message, details = details)
        emit(MatterSetupUiState.failed(failure, ports.runDiagnostics(failure, config)))
    }
}
```

- [ ] **Step 6: Run workflow test**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupWorkflowTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\setup app\src\test\kotlin\org\openhab\matter\companion\setup\MatterSetupWorkflowTest.kt
git commit -m "Add automated Matter setup workflow"
```

---

### Task 5: Workflow Failure And Diagnostics Triggering

**Files:**
- Modify: `app/src/test/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflowTest.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/MatterSetupWorkflow.kt`

- [ ] **Step 1: Add failing workflow failure tests**

Append to `MatterSetupWorkflowTest`:

```kotlin
    @Test
    fun openHabScanFailureRunsDiagnosticsAndShowsRecovery() {
        val ports = FakeMatterSetupPorts().apply {
            scanStarted = false
            scanDetails = "HTTP 401"
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        val finalState = states.last()
        assertEquals(MatterSetupStage.Failed, finalState.stage)
        assertEquals(MatterSetupStage.SendingCodeToOpenHab, finalState.failure!!.step)
        assertTrue(ports.diagnosticsCalled)
        assertTrue(finalState.failure.suggestions.contains("Check the openHAB address and access token."))
        assertFalse(finalState.toString().contains("ohab_secret"))
        assertFalse(finalState.toString().contains("34970112332"))
    }

    @Test
    fun inboxTimeoutSuggestsIpv6AndMdns() {
        val ports = FakeMatterSetupPorts().apply {
            inboxDetected = false
            inboxDetails = "No Matter Inbox entry before timeout"
        }
        val states = mutableListOf<MatterSetupUiState>()
        val workflow = MatterSetupWorkflow(ports) { states.add(it) }

        workflow.startAutomatedSetup("MT:TEST")

        val suggestions = states.last().failure!!.suggestions.joinToString("\n")
        assertTrue(suggestions.contains("IPv6 routing"))
        assertTrue(suggestions.contains("mDNS"))
        assertTrue(ports.diagnosticsCalled)
    }
```

Update `FakeMatterSetupPorts` in the same test file:

```kotlin
        var scanStarted = true
        var scanDetails = "scan accepted"
        var inboxDetected = true
        var inboxDetails = "Matter Inbox entry detected"
        var diagnosticsCalled = false
```

Change `sendCodeToOpenHab` in the fake:

```kotlin
            return MatterSetupPorts.OpenHabScanResult(started = scanStarted, timeoutSeconds = 120, details = scanDetails)
```

Change `waitForOpenHabInbox` in the fake:

```kotlin
            return MatterSetupPorts.InboxResult(matterEntryDetected = inboxDetected, details = inboxDetails)
```

Change `runDiagnostics` in the fake:

```kotlin
            diagnosticsCalled = true
            return MatterSetupDiagnosticsSummary(
                checks = listOf("openHAB readiness checked"),
                warnings = emptyList(),
                details = listOf("Diagnostics completed")
            )
```

- [ ] **Step 2: Run tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterSetupWorkflowTest"
```

Expected: PASS. If this fails because `scanStarted`, `inboxDetected`, or `diagnosticsCalled` are missing, update the fake exactly as shown in Step 1.

- [ ] **Step 3: Commit**

```powershell
git add app\src\test\kotlin\org\openhab\matter\companion\setup\MatterSetupWorkflowTest.kt app\src\main\kotlin\org\openhab\matter\companion\setup\MatterSetupWorkflow.kt
git commit -m "Cover Matter setup workflow recovery states"
```

---

### Task 6: Android Readiness Diagnostics

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/ReadinessDiagnostic.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummary.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidReadinessProbe.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummaryTest.kt`

- [ ] **Step 1: Write failing transport summary tests**

Create `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummaryTest.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTransportSummaryTest {
    @Test
    fun wifiWithoutVpnIsPreferredForLocalSetup() {
        val summary = NetworkTransportSummary(wifi = true, cellular = false, vpn = false)

        assertTrue(summary.localNetworkPreferred)
        assertEquals(emptyList<String>(), summary.warnings)
    }

    @Test
    fun cellularOnlyWarnsUser() {
        val summary = NetworkTransportSummary(wifi = false, cellular = true, vpn = false)

        assertEquals(listOf("Connect to Wi-Fi before adding local Matter devices."), summary.warnings)
    }

    @Test
    fun vpnWarnsUser() {
        val summary = NetworkTransportSummary(wifi = true, cellular = false, vpn = true)

        assertEquals(listOf("VPN is active and may block openHAB, mDNS, or IPv6 routing."), summary.warnings)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*NetworkTransportSummaryTest"
```

Expected: FAIL because diagnostics classes do not exist.

- [ ] **Step 3: Add diagnostic models**

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/ReadinessDiagnostic.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

data class ReadinessDiagnostic(
    val name: String,
    val ok: Boolean,
    val message: String
)
```

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/NetworkTransportSummary.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

data class NetworkTransportSummary(
    val wifi: Boolean,
    val cellular: Boolean,
    val vpn: Boolean
) {
    val localNetworkPreferred: Boolean
        get() = wifi && !vpn

    val warnings: List<String>
        get() = buildList {
            if (!wifi && cellular) {
                add("Connect to Wi-Fi before adding local Matter devices.")
            }
            if (vpn) {
                add("VPN is active and may block openHAB, mDNS, or IPv6 routing.")
            }
        }
}
```

- [ ] **Step 4: Add Android readiness probe**

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidReadinessProbe.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class AndroidReadinessProbe(private val context: Context) {
    fun bluetoothDiagnostic(): ReadinessDiagnostic {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return ReadinessDiagnostic("Bluetooth", ok = false, message = "Bluetooth is not available on this device.")
        return ReadinessDiagnostic(
            name = "Bluetooth",
            ok = adapter.isEnabled,
            message = if (adapter.isEnabled) "Bluetooth is enabled." else "Enable Bluetooth to find nearby Matter devices."
        )
    }

    fun locationServicesDiagnostic(): ReadinessDiagnostic {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return ReadinessDiagnostic(
            name = "Location services",
            ok = enabled,
            message = if (enabled) {
                "Location services are enabled."
            } else {
                "Enable location services so Android can scan for nearby Bluetooth devices."
            }
        )
    }

    fun permissionDiagnostics(): List<ReadinessDiagnostic> {
        val permissions = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= 23) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        return permissions.map { permission ->
            val granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            ReadinessDiagnostic(
                name = permission.substringAfterLast('.'),
                ok = granted,
                message = if (granted) "$permission granted." else "$permission is needed for setup."
            )
        }
    }

    fun networkTransportSummary(): NetworkTransportSummary {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return NetworkTransportSummary(wifi = false, cellular = false, vpn = false)
        val capabilities = connectivity.getNetworkCapabilities(network)
            ?: return NetworkTransportSummary(wifi = false, cellular = false, vpn = false)
        return NetworkTransportSummary(
            wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            vpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        )
    }
}
```

- [ ] **Step 5: Run diagnostics tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*NetworkTransportSummaryTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\diagnostics app\src\test\kotlin\org\openhab\matter\companion\diagnostics\NetworkTransportSummaryTest.kt
git commit -m "Add Android setup readiness diagnostics"
```

---

### Task 7: mDNS And IPv6 Diagnostic Models

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecord.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsBrowser.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidMatterMdnsBrowser.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/Ipv6ReachabilityProbe.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbe.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecordTest.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbeTest.kt`

- [ ] **Step 1: Write failing mDNS and IPv6 tests**

Create `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecordTest.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class MatterMdnsRecordTest {
    @Test
    fun phoneDiscoveryLimitationTextIsExplicit() {
        assertEquals(
            "This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup.",
            MatterMdnsRecord.phoneDiscoveryLimitation
        )
    }

    @Test
    fun summaryShowsPhoneCanSeeDevice() {
        val record = MatterMdnsRecord(
            serviceType = "_matterc._udp",
            instanceName = "Test Device",
            host = "test.local",
            ipv6Addresses = listOf("fd00::1234"),
            port = 5540,
            txt = mapOf("D" to "3840")
        )

        assertEquals("Phone can see the device", record.interpretationTitle)
    }
}
```

Create `app/src/test/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbeTest.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultIpv6ReachabilityProbeTest {
    @Test
    fun detectsIpv6Literal() {
        assertTrue(DefaultIpv6ReachabilityProbe.isIpv6Literal("fd00::1234"))
        assertFalse(DefaultIpv6ReachabilityProbe.isIpv6Literal("192.168.1.10"))
        assertFalse(DefaultIpv6ReachabilityProbe.isIpv6Literal("openhab.local"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterMdnsRecordTest" --tests "*DefaultIpv6ReachabilityProbeTest"
```

Expected: FAIL because the diagnostic classes do not exist.

- [ ] **Step 3: Add mDNS record model and browser interface**

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsRecord.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

data class MatterMdnsRecord(
    val serviceType: String,
    val instanceName: String,
    val host: String,
    val ipv6Addresses: List<String>,
    val port: Int,
    val txt: Map<String, String>
) {
    val interpretationTitle: String
        get() = if (ipv6Addresses.isEmpty()) "Phone found a Matter service" else "Phone can see the device"

    companion object {
        const val phoneDiscoveryLimitation: String =
            "This check shows discovery from this phone. openHAB may see a different result depending on its network, Avahi, router, and IPv6 setup."
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/MatterMdnsBrowser.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

interface MatterMdnsBrowser {
    fun browseMatterCommissionable(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>

    fun browseMatterOperational(timeoutMillis: Long = 5_000L): List<MatterMdnsRecord>
}
```

- [ ] **Step 4: Add Android NSD mDNS browser skeleton**

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/AndroidMatterMdnsBrowser.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.Inet6Address
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidMatterMdnsBrowser(context: Context) : MatterMdnsBrowser {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    override fun browseMatterCommissionable(timeoutMillis: Long): List<MatterMdnsRecord> {
        return browse("_matterc._udp", timeoutMillis)
    }

    override fun browseMatterOperational(timeoutMillis: Long): List<MatterMdnsRecord> {
        return browse("_matter._tcp", timeoutMillis)
    }

    private fun browse(serviceType: String, timeoutMillis: Long): List<MatterMdnsRecord> {
        val records = mutableListOf<MatterMdnsRecord>()
        val latch = CountDownLatch(1)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolve(serviceType, serviceInfo, records)
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                latch.countDown()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
        runCatching { nsdManager.stopServiceDiscovery(listener) }
        return records.toList()
    }

    private fun resolve(serviceType: String, serviceInfo: NsdServiceInfo, records: MutableList<MatterMdnsRecord>) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val host = resolved.host
                val ipv6 = if (host is Inet6Address) listOf(host.hostAddress.orEmpty()) else emptyList()
                records.add(
                    MatterMdnsRecord(
                        serviceType = serviceType,
                        instanceName = resolved.serviceName.orEmpty(),
                        host = host?.hostName.orEmpty(),
                        ipv6Addresses = ipv6,
                        port = resolved.port,
                        txt = emptyMap()
                    )
                )
            }
        })
    }
}
```

- [ ] **Step 5: Add IPv6 reachability probe**

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/Ipv6ReachabilityProbe.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

interface Ipv6ReachabilityProbe {
    fun isReachable(ipv6Address: String, timeoutMillis: Int = 2_000): Boolean
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/diagnostics/DefaultIpv6ReachabilityProbe.kt`:

```kotlin
package org.openhab.matter.companion.diagnostics

import java.net.Inet6Address
import java.net.InetAddress

class DefaultIpv6ReachabilityProbe : Ipv6ReachabilityProbe {
    override fun isReachable(ipv6Address: String, timeoutMillis: Int): Boolean {
        if (!isIpv6Literal(ipv6Address)) {
            return false
        }
        return runCatching {
            InetAddress.getByName(ipv6Address).isReachable(timeoutMillis)
        }.getOrDefault(false)
    }

    companion object {
        fun isIpv6Literal(value: String): Boolean {
            return runCatching { InetAddress.getByName(value) }
                .getOrNull() is Inet6Address && value.contains(":")
        }
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*MatterMdnsRecordTest" --tests "*DefaultIpv6ReachabilityProbeTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\diagnostics app\src\test\kotlin\org\openhab\matter\companion\diagnostics
git commit -m "Add Matter mDNS and IPv6 diagnostic models"
```

---

### Task 8: Real Workflow Ports Adapter

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPorts.kt`
- Test: `app/src/test/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPortsTest.kt`

- [ ] **Step 1: Write failing adapter tests with fake Java-facing dependencies**

Create `app/src/test/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPortsTest.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus
import org.openhab.matter.companion.openhab.OpenHabStatus

class AndroidMatterSetupPortsTest {
    @Test
    fun openHabScanFailureDoesNotExposeManualCode() {
        val ports = AndroidMatterSetupPorts(
            configLoader = { testConfig() },
            readinessChecker = { _, _ -> OpenHabStatus(true, true, true, "openHAB ready", "") },
            matterRunner = FakeMatterRunner(),
            discoveryScan = { _, code, _ ->
                OpenHabMatterDiscoveryScanStatus(false, false, "scan failed", "Rejected request without code $code", 0)
            },
            inboxWaiter = { _, _, _ -> MatterSetupPorts.InboxResult(false, "not used") },
            diagnosticsRunner = { _, _ -> MatterSetupDiagnosticsSummary.empty() }
        )

        val result = ports.sendCodeToOpenHab("34970112332", testConfig())

        assertFalse(result.started)
        assertFalse(result.details.contains("34970112332"))
    }

    @Test
    fun readinessFailsWhenOpenHabMatterControllerIsOffline() {
        val ports = AndroidMatterSetupPorts(
            configLoader = { testConfig() },
            readinessChecker = { _, _ -> OpenHabStatus(false, true, false, "Matter controller offline", "offline") },
            matterRunner = FakeMatterRunner(),
            discoveryScan = { _, _, _ -> OpenHabMatterDiscoveryScanStatus(true, true, "started", "", 120) },
            inboxWaiter = { _, _, _ -> MatterSetupPorts.InboxResult(true, "detected") },
            diagnosticsRunner = { _, _ -> MatterSetupDiagnosticsSummary.empty() }
        )

        val result = ports.checkReadiness(testConfig())

        assertFalse(result.ready)
        assertTrue(result.warnings.contains("Matter controller offline"))
    }

    private fun testConfig(): MatterSetupConfig = MatterSetupConfig(
        openHabBaseUrl = "http://openhab.local:8080",
        openHabApiToken = "ohab_secret",
        threadDataset = "hex:001122",
        otbrBaseUrl = "http://otbr.local",
        attestationBypassEnabled = false
    )

    private class FakeMatterRunner : AndroidMatterSetupPorts.MatterRunner {
        override fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): MatterSetupPorts.CommissionResult {
            return MatterSetupPorts.CommissionResult(1L, "state")
        }

        override fun openCommissioningWindow(nodeId: Long, controllerState: String): MatterSetupPorts.OpenWindowResult {
            return MatterSetupPorts.OpenWindowResult("34970112332", "", "state-2", 300)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*AndroidMatterSetupPortsTest"
```

Expected: FAIL because `AndroidMatterSetupPorts` does not exist.

- [ ] **Step 3: Add adapter implementation**

Create `app/src/main/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPorts.kt`:

```kotlin
package org.openhab.matter.companion.setup

import org.openhab.matter.companion.openhab.OpenHabMatterDiscoveryScanStatus
import org.openhab.matter.companion.openhab.OpenHabStatus

class AndroidMatterSetupPorts(
    private val configLoader: () -> MatterSetupConfig,
    private val readinessChecker: (String, String) -> OpenHabStatus,
    private val matterRunner: MatterRunner,
    private val discoveryScan: (String, String, String) -> OpenHabMatterDiscoveryScanStatus,
    private val inboxWaiter: (String, String, Int) -> MatterSetupPorts.InboxResult,
    private val diagnosticsRunner: (MatterSetupFailure, MatterSetupConfig) -> MatterSetupDiagnosticsSummary
) : MatterSetupPorts {
    interface MatterRunner {
        fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): MatterSetupPorts.CommissionResult

        fun openCommissioningWindow(nodeId: Long, controllerState: String): MatterSetupPorts.OpenWindowResult
    }

    override fun loadConfig(): MatterSetupConfig = configLoader()

    override fun checkReadiness(config: MatterSetupConfig): MatterSetupPorts.ReadinessResult {
        val status = readinessChecker(config.openHabBaseUrl, config.openHabApiToken)
        return MatterSetupPorts.ReadinessResult(
            ready = status.online() && status.restReachable() && status.matterControllerReady(),
            details = listOf(status.message()).filter { it.isNotBlank() },
            warnings = if (status.matterControllerReady()) emptyList() else listOf(status.message())
        )
    }

    override fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): MatterSetupPorts.CommissionResult {
        return matterRunner.commissionToPhone(setupPayload, config)
    }

    override fun openCommissioningWindow(nodeId: Long, controllerState: String): MatterSetupPorts.OpenWindowResult {
        return matterRunner.openCommissioningWindow(nodeId, controllerState)
    }

    override fun sendCodeToOpenHab(manualCode: String, config: MatterSetupConfig): MatterSetupPorts.OpenHabScanResult {
        val status = discoveryScan(config.openHabBaseUrl, manualCode, config.openHabApiToken)
        return MatterSetupPorts.OpenHabScanResult(
            started = status.started(),
            timeoutSeconds = status.timeoutSeconds(),
            details = sanitize(status.details(), manualCode, config.openHabApiToken)
        )
    }

    override fun waitForOpenHabInbox(config: MatterSetupConfig, timeoutSeconds: Int): MatterSetupPorts.InboxResult {
        return inboxWaiter(config.openHabBaseUrl, config.openHabApiToken, timeoutSeconds)
    }

    override fun runDiagnostics(failure: MatterSetupFailure, config: MatterSetupConfig): MatterSetupDiagnosticsSummary {
        return diagnosticsRunner(failure, config)
    }

    private fun sanitize(value: String?, manualCode: String, token: String): String {
        var sanitized = value.orEmpty()
        if (manualCode.isNotBlank()) {
            sanitized = sanitized.replace(manualCode, "[setup code redacted]")
        }
        if (token.isNotBlank()) {
            sanitized = sanitized.replace(token, "[token redacted]")
        }
        return sanitized
    }
}
```

- [ ] **Step 4: Run adapter tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest --tests "*AndroidMatterSetupPortsTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\setup\AndroidMatterSetupPorts.kt app\src\test\kotlin\org\openhab\matter\companion\setup\AndroidMatterSetupPortsTest.kt
git commit -m "Add Android adapter for Matter setup workflow"
```

---

### Task 9: Compose Screen Components

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/theme/MatterSetupTheme.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/components/SetupStepList.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/PairingModeScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupProgressScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupFailureScreen.kt`
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupSuccessScreen.kt`

- [ ] **Step 1: Add theme**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/theme/MatterSetupTheme.kt`:

```kotlin
package org.openhab.matter.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF1769D2),
    secondary = Color(0xFF4F6F52),
    tertiary = Color(0xFFC75B12),
    background = Color(0xFFFAF8F4),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E)
)

@Composable
fun MatterSetupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
```

- [ ] **Step 2: Add setup step list**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/components/SetupStepList.kt`:

```kotlin
package org.openhab.matter.companion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupStep
import org.openhab.matter.companion.setup.MatterSetupStepStatus

@Composable
fun SetupStepList(steps: List<MatterSetupStep>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEach { step ->
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(statusGlyph(step.status), color = statusColor(step.status))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(step.label, fontWeight = if (step.status == MatterSetupStepStatus.Active) FontWeight.Bold else FontWeight.Normal)
                    if (step.detail.isNotBlank()) {
                        Text(step.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: MatterSetupStepStatus) = when (status) {
    MatterSetupStepStatus.Complete -> MaterialTheme.colorScheme.secondary
    MatterSetupStepStatus.Active -> MaterialTheme.colorScheme.primary
    MatterSetupStepStatus.Failed -> MaterialTheme.colorScheme.error
    MatterSetupStepStatus.Pending -> MaterialTheme.colorScheme.outline
}

private fun statusGlyph(status: MatterSetupStepStatus): String = when (status) {
    MatterSetupStepStatus.Complete -> "✓"
    MatterSetupStepStatus.Active -> "●"
    MatterSetupStepStatus.Failed -> "!"
    MatterSetupStepStatus.Pending -> "○"
}
```

- [ ] **Step 3: Add screen composables**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/OpenHabSetupScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun OpenHabSetupScreen(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(state.title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(state.message)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(openHabUrl, onUrlChange, label = { Text("openHAB address") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(token, onTokenChange, label = { Text("Access token") })
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = state.primaryActionEnabled,
            onClick = { state.primaryAction?.let(onAction) }
        ) {
            Text(state.primaryActionLabel)
        }
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/ScanDeviceScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun ScanDeviceScreen(state: MatterSetupUiState, onAction: (MatterSetupAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Scan your device code", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Point your camera at the Matter QR code on the device or box.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(MatterSetupAction.StartScan) }) {
            Text("Scan QR code")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onAction(MatterSetupAction.EnterCodeManually) }) {
            Text("Enter code manually")
        }
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/PairingModeScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction

@Composable
fun PairingModeScreen(onAction: (MatterSetupAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Put device in pairing mode", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Make sure the device is powered on and ready to be added.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(MatterSetupAction.ConfirmPairingMode) }) {
            Text("Device is ready")
        }
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupProgressScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.CommissioningWindowCountdown
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.SetupStepList

@Composable
fun SetupProgressScreen(state: MatterSetupUiState) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(state.title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(state.message)
        state.countdownSeconds?.let {
            Spacer(Modifier.height(12.dp))
            Text(CommissioningWindowCountdown.displayText(it))
        }
        Spacer(Modifier.height(24.dp))
        SetupStepList(state.steps)
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupFailureScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun SetupFailureScreen(state: MatterSetupUiState, onAction: (MatterSetupAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(state.title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(state.message)
        Spacer(Modifier.height(16.dp))
        state.failure?.suggestions.orEmpty().forEach {
            Text("• $it")
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(MatterSetupAction.Retry) }) {
            Text("Try again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onAction(MatterSetupAction.ShowTroubleshooting) }) {
            Text("Show troubleshooting")
        }
    }
}
```

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/SetupSuccessScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun SetupSuccessScreen(state: MatterSetupUiState, onAction: (MatterSetupAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(state.title, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(state.message)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(MatterSetupAction.AddAnotherDevice) }) {
            Text("Add another device")
        }
    }
}
```

- [ ] **Step 4: Add top-level app switch**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.runtime.Composable
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.theme.MatterSetupTheme

@Composable
fun MatterSetupApp(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    onOpenHabUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupTheme {
        when (state.stage) {
            MatterSetupStage.NeedsOpenHabSetup,
            MatterSetupStage.OpenHabSetupChecking -> OpenHabSetupScreen(
                state = state,
                openHabUrl = openHabUrl,
                token = token,
                onUrlChange = onOpenHabUrlChange,
                onTokenChange = onTokenChange,
                onAction = onAction
            )

            MatterSetupStage.ReadyToScan,
            MatterSetupStage.ScanningQr,
            MatterSetupStage.QrScanned -> ScanDeviceScreen(state, onAction)

            MatterSetupStage.NeedsPairingMode -> PairingModeScreen(onAction)

            MatterSetupStage.ReadinessChecking,
            MatterSetupStage.CommissioningToPhone,
            MatterSetupStage.OpeningCommissioningWindow,
            MatterSetupStage.CommissioningWindowOpen,
            MatterSetupStage.SendingCodeToOpenHab,
            MatterSetupStage.WatchingOpenHabInbox -> SetupProgressScreen(state)

            MatterSetupStage.SuccessInboxDetected -> SetupSuccessScreen(state, onAction)

            MatterSetupStage.Failed,
            MatterSetupStage.AdvancedTroubleshooting -> SetupFailureScreen(state, onAction)
        }
    }
}
```

- [ ] **Step 5: Run assemble**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\ui
git commit -m "Add Compose Matter setup screens"
```

---

### Task 10: Compose Activity And QR Launch Wiring

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add Compose Activity**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupActivity.kt`:

```kotlin
package org.openhab.matter.companion.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.openhab.matter.companion.qr.InAppQrScannerActivity
import org.openhab.matter.companion.qr.QrScanIntentFactory
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupUiState

class MatterSetupActivity : ComponentActivity() {
    private var uiState by mutableStateOf(MatterSetupUiState.initial(openHabConfigured = false))
    private var openHabUrl by mutableStateOf("")
    private var token by mutableStateOf("")
    private var scannedPayload by mutableStateOf("")

    private val qrScanner = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }
        val payload = QrScanIntentFactory.extractMatterSetupPayload(result.data)
        if (payload.isNullOrBlank()) {
            uiState = MatterSetupUiState.failed(
                failure = org.openhab.matter.companion.setup.MatterSetupFailure(
                    step = MatterSetupStage.ScanningQr,
                    message = "QR scanner did not return a Matter code."
                ),
                diagnostics = org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary.empty()
            )
            return@registerForActivityResult
        }
        scannedPayload = payload
        uiState = MatterSetupUiState(
            stage = MatterSetupStage.NeedsPairingMode,
            title = "Put device in pairing mode",
            message = "Make sure the device is powered on and ready to be added.",
            primaryAction = MatterSetupAction.ConfirmPairingMode,
            primaryActionLabel = "Device is ready"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MatterSetupApp(
                state = uiState,
                openHabUrl = openHabUrl,
                token = token,
                onOpenHabUrlChange = {
                    openHabUrl = it
                    uiState = MatterSetupUiState.initial(openHabConfigured = it.isNotBlank())
                },
                onTokenChange = { token = it },
                onAction = ::handleAction
            )
        }
    }

    private fun handleAction(action: MatterSetupAction) {
        when (action) {
            MatterSetupAction.StartScan -> {
                qrScanner.launch(Intent(this, InAppQrScannerActivity::class.java))
            }
            MatterSetupAction.ConfirmPairingMode -> {
                uiState = MatterSetupUiState.progress(MatterSetupStage.ReadinessChecking)
            }
            MatterSetupAction.Retry -> {
                uiState = MatterSetupUiState.initial(openHabConfigured = openHabUrl.isNotBlank())
            }
            MatterSetupAction.AddAnotherDevice -> {
                scannedPayload = ""
                uiState = MatterSetupUiState.initial(openHabConfigured = openHabUrl.isNotBlank())
            }
            MatterSetupAction.ShowTroubleshooting,
            MatterSetupAction.EnterCodeManually,
            MatterSetupAction.SaveOpenHab,
            MatterSetupAction.TestOpenHab,
            MatterSetupAction.OpenCommissioningWindowAgain,
            MatterSetupAction.ForgetFromPhone -> {
                uiState = uiState.copy(
                    stage = MatterSetupStage.AdvancedTroubleshooting,
                    title = "Troubleshooting",
                    message = "Advanced setup tools will appear here as they are connected."
                )
            }
        }
    }
}
```

- [ ] **Step 2: Change launcher activity**

Modify `app/src/main/AndroidManifest.xml`. Replace the launcher activity class name:

```xml
<activity
    android:name=".ui.MatterSetupActivity"
    android:exported="true">
```

Keep the existing intent filters unchanged.

- [ ] **Step 3: Run assemble**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\ui\MatterSetupActivity.kt app\src\main\AndroidManifest.xml
git commit -m "Launch Compose Matter setup activity"
```

---

### Task 11: Wire Real Automated Workflow In Activity

**Files:**
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupActivity.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/setup/AndroidMatterSetupPorts.kt`

- [ ] **Step 1: Add real-service imports and fields**

In `MatterSetupActivity`, add imports for existing Java classes:

```kotlin
import android.view.WindowManager
import org.openhab.matter.companion.config.SharedPreferencesAppConfigRepository
import org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactory
import org.openhab.matter.companion.controller.FakeMatterController
import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.NativeChipControllerSession
import org.openhab.matter.companion.controller.SharedPreferencesMatterBootstrapStateRepository
import org.openhab.matter.companion.domain.MatterSetupPayloadParser
import org.openhab.matter.companion.domain.ThreadDataset
import org.openhab.matter.companion.openhab.HttpOpenHabClient
import org.openhab.matter.companion.openhab.HttpOpenHabInboxClient
import org.openhab.matter.companion.openhab.HttpOpenHabMatterDiscoveryClient
```

Inside `MatterSetupActivity`, add fields:

```kotlin
    private val fakeMatterController = FakeMatterController()
    private val configRepository by lazy { SharedPreferencesAppConfigRepository(this) }
    private val bootstrapStateRepository by lazy { SharedPreferencesMatterBootstrapStateRepository(this) }
```

In `onCreate`, before `setContent`, add:

```kotlin
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val persistedConfig = configRepository.load()
        openHabUrl = persistedConfig.openHabBaseUrl()
        token = persistedConfig.openHabApiToken()
        uiState = MatterSetupUiState.initial(openHabConfigured = openHabUrl.isNotBlank())
```

- [ ] **Step 2: Add workflow launcher**

In `MatterSetupActivity`, add:

```kotlin
private fun startRealWorkflow(setupPayload: String) {
    val ports = createRealPorts()
    Thread {
        MatterSetupWorkflow(ports) { state ->
            runOnUiThread { uiState = state }
        }.startAutomatedSetup(setupPayload)
    }.start()
}
```

- [ ] **Step 3: Implement `createRealPorts()` using existing Java services**

In `MatterSetupActivity`, add:

```kotlin
private fun createRealPorts(): AndroidMatterSetupPorts {
    val openHabClient = HttpOpenHabClient()
    val discoveryClient = HttpOpenHabMatterDiscoveryClient()
    val inboxClient = HttpOpenHabInboxClient()

    return AndroidMatterSetupPorts(
        configLoader = {
            val config = configRepository.load()
            MatterSetupConfig(
                openHabBaseUrl = if (openHabUrl.isNotBlank()) openHabUrl else config.openHabBaseUrl(),
                openHabApiToken = if (token.isNotBlank()) token else config.openHabApiToken(),
                threadDataset = config.threadDataset(),
                otbrBaseUrl = config.otbrBaseUrl(),
                attestationBypassEnabled = config.attestationBypassEnabled()
            )
        },
        readinessChecker = { baseUrl, apiToken ->
            openHabClient.checkReadiness(baseUrl, apiToken)
        },
        matterRunner = createMatterRunner(),
        discoveryScan = { baseUrl, manualCode, apiToken ->
            discoveryClient.startMatterScan(baseUrl, manualCode, apiToken)
        },
        inboxWaiter = { baseUrl, apiToken, timeoutSeconds ->
            val deadline = System.currentTimeMillis() + timeoutSeconds.coerceAtLeast(30) * 1_000L
            var last = inboxClient.checkInbox(baseUrl, apiToken)
            while (!last.matterEntryDetected() && System.currentTimeMillis() < deadline) {
                Thread.sleep(2_000L)
                last = inboxClient.checkInbox(baseUrl, apiToken)
            }
            MatterSetupPorts.InboxResult(last.matterEntryDetected(), last.details().orEmpty())
        },
        diagnosticsRunner = { failure, _ ->
            MatterSetupDiagnosticsSummary(
                checks = listOf("openHAB readiness checked", "Inbox checked"),
                warnings = failure.suggestions,
                details = listOf(failure.details).filter { it.isNotBlank() }
            )
        }
    )
}
```

- [ ] **Step 4: Implement native session factory and `MatterRunner`**

In `MatterSetupActivity`, add:

```kotlin
private fun createNativeSession(attestationBypassEnabled: Boolean): NativeChipControllerSession {
    val connectedHomeIpFactory = ConnectedHomeIpMatterControllerFactory(this)
    return NativeChipControllerSession(
        fakeMatterController,
        attestationBypassEnabled,
        connectedHomeIpFactory::create
    )
}

private fun createMatterRunner(): AndroidMatterSetupPorts.MatterRunner {
    return object : AndroidMatterSetupPorts.MatterRunner {
        override fun commissionToPhone(setupPayload: String, config: MatterSetupConfig): MatterSetupPorts.CommissionResult {
            val dataset = ThreadDataset.parse(config.threadDataset)
            val payload = MatterSetupPayloadParser.parse(setupPayload)
            val session = createNativeSession(config.attestationBypassEnabled)
            val selection = session.selectNativeIfReady()
            if (!selection.nativeSelected()) {
                throw IllegalStateException("connectedhomeip is not ready for real commissioning.")
            }
            val existingState = bootstrapStateRepository.load()
            val result = selection.controller().commissionBleThread(
                dataset,
                payload,
                existingState.controllerState(),
                null
            )
            bootstrapStateRepository.save(MatterBootstrapState(result.nodeId(), result.controllerState(), false))
            return MatterSetupPorts.CommissionResult(result.nodeId(), result.controllerState())
        }

        override fun openCommissioningWindow(nodeId: Long, controllerState: String): MatterSetupPorts.OpenWindowResult {
            val config = configRepository.load()
            val session = createNativeSession(config.attestationBypassEnabled())
            val selection = session.selectNativeIfReady()
            if (!selection.nativeSelected()) {
                throw IllegalStateException("connectedhomeip is not ready for Open Commissioning Window.")
            }
            val result = selection.controller().openCommissioningWindow(nodeId, 300, 3840, controllerState, null)
            bootstrapStateRepository.save(MatterBootstrapState(nodeId, result.controllerState(), false))
            return MatterSetupPorts.OpenWindowResult(
                manualCode = result.temporaryCode(),
                qrCode = result.qrCode(),
                controllerState = result.controllerState(),
                timeoutSeconds = 300
            )
        }
    }
}
```

This mirrors the existing Java factory pattern:

```java
ConnectedHomeIpMatterControllerFactory connectedHomeIpFactory = new ConnectedHomeIpMatterControllerFactory(this);
return new NativeChipControllerSession(fakeMatterController, attestationBypassEnabled, connectedHomeIpFactory::create);
```

The final code must compile and must not use `FakeMatterController` when native is not ready. `FakeMatterController` is only the fallback candidate inside `NativeChipControllerSession`; the explicit `nativeSelected()` checks above preserve fail-closed real commissioning behavior.

- [ ] **Step 5: Start real workflow after pairing mode confirmation**

In `handleAction`, change `ConfirmPairingMode` handling to:

```kotlin
MatterSetupAction.ConfirmPairingMode -> {
    if (scannedPayload.isBlank()) {
        uiState = MatterSetupUiState.failed(
            MatterSetupFailure(MatterSetupStage.QrScanned, "Scan the Matter QR code again."),
            MatterSetupDiagnosticsSummary.empty()
        )
    } else {
        startRealWorkflow(scannedPayload)
    }
}
```

- [ ] **Step 6: Run focused compile**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run unit tests and assemble**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\ui\MatterSetupActivity.kt app\src\main\kotlin\org\openhab\matter\companion\setup\AndroidMatterSetupPorts.kt
git commit -m "Wire automated Matter setup workflow to Android services"
```

---

### Task 12: Advanced Troubleshooting Screen

**Files:**
- Create: `app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt`
- Modify: `app/src/main/kotlin/org/openhab/matter/companion/ui/MatterSetupApp.kt`

- [ ] **Step 1: Add advanced troubleshooting screen**

Create `app/src/main/kotlin/org/openhab/matter/companion/ui/AdvancedTroubleshootingScreen.kt`:

```kotlin
package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun AdvancedTroubleshootingScreen(state: MatterSetupUiState, onAction: (MatterSetupAction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Troubleshooting", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("These checks help diagnose openHAB pairing problems.")
        Spacer(Modifier.height(16.dp))
        state.diagnostics.checks.forEach { Text("✓ $it") }
        state.diagnostics.warnings.forEach { Text("! $it") }
        state.diagnostics.details.forEach { Text(it) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(MatterSetupAction.OpenCommissioningWindowAgain) }) {
            Text("Open pairing window again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onAction(MatterSetupAction.ForgetFromPhone) }) {
            Text("Forget from this phone")
        }
    }
}
```

- [ ] **Step 2: Route advanced state to troubleshooting screen**

Modify `MatterSetupApp.kt` so the final branch becomes:

```kotlin
            MatterSetupStage.Failed -> SetupFailureScreen(state, onAction)

            MatterSetupStage.AdvancedTroubleshooting -> AdvancedTroubleshootingScreen(state, onAction)
```

- [ ] **Step 3: Run assemble**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```powershell
git add app\src\main\kotlin\org\openhab\matter\companion\ui\AdvancedTroubleshootingScreen.kt app\src\main\kotlin\org\openhab\matter\companion\ui\MatterSetupApp.kt
git commit -m "Add advanced Matter setup troubleshooting screen"
```

---

### Task 13: Documentation Updates

**Files:**
- Modify: `docs/implementation-status.md`
- Modify: `docs/open-commissioning-window-workflow.md`

- [ ] **Step 1: Update implementation status**

In `docs/implementation-status.md`, under `## Implemented`, add:

```markdown
- Kotlin/Jetpack Compose automated Matter setup UI guides users through openHAB setup, QR scan, pairing-mode confirmation, Thread commissioning, OpenCommissioningWindow, openHAB scan submission, and Inbox detection.
- Automated setup workflow exposes user-facing progress states and a commissioning-window countdown while preserving the connectedhomeip fail-closed readiness gate.
- Failure recovery runs basic diagnostics and links to advanced troubleshooting for openHAB readiness, network/VPN hints, mDNS discovery limitations, IPv6 reachability checks, OCW retry, and forget-from-phone recovery.
```

If any of these statements are not fully true after implementation, move the incomplete statement to `## Not Implemented Yet` and describe the remaining gap exactly.

- [ ] **Step 2: Update OCW workflow**

In `docs/open-commissioning-window-workflow.md`, update the summary paragraph to include:

```markdown
The Compose automated setup flow now treats OpenCommissioningWindow as an internal step. When connectedhomeip returns the manual setup code, the app submits that code to openHAB Matter discovery scan input, starts watching openHAB Inbox, and reports v1 success only when a Matter Inbox entry is detected. The temporary 300-second pairing window is shown as a countdown in the user-facing progress UI.
```

In the failure paths section, add:

```markdown
- If the openHAB scan starts but no Inbox entry is detected before timeout, the app shows recovery guidance for IPv6 routing, OTBR reachability, mDNS/Avahi, stale Matter records, and OCW retry.
```

- [ ] **Step 3: Run doc diff review**

Run:

```powershell
git diff -- docs\implementation-status.md docs\open-commissioning-window-workflow.md
```

Expected: Diff only contains truthful status/workflow updates for implemented behavior.

- [ ] **Step 4: Commit**

```powershell
git add docs\implementation-status.md docs\open-commissioning-window-workflow.md
git commit -m "Document Compose automated Matter setup flow"
```

---

### Task 14: Final Verification

**Files:**
- No file changes expected unless verification finds a bug.

- [ ] **Step 1: Run all unit tests**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build debug APK**

Run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL and `app\build\outputs\apk\debug\app-debug.apk` exists.

- [ ] **Step 3: Optional real-artifact build**

If connectedhomeip artifacts are available, run:

```powershell
$env:ANDROID_HOME='D:\Tools\Android\SDK'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug -PopenhabMatterChipControllerArtifactsDir=<artifact-dir> -PopenhabMatterChipControllerAbis=arm64-v8a
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual device validation**

Run:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe devices
```

Expected: at least one device is attached and authorized.

Install:

```powershell
D:\Tools\Android\SDK\platform-tools\adb.exe -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: install succeeds.

Validate on device:

```text
1. Launch app.
2. Confirm Compose openHAB setup screen appears.
3. Disable Bluetooth and confirm the flow prompts for Bluetooth instead of failing with a raw error.
4. Enable Bluetooth and scan a Matter QR code.
5. Confirm pairing-mode screen appears before BLE commissioning.
6. Run real Thread commissioning.
7. Confirm OCW countdown starts at 300 seconds.
8. Confirm setup code is submitted to openHAB.
9. Confirm success appears only after openHAB Inbox detects a Matter entry.
10. If Inbox does not detect the device, confirm failure recovery mentions IPv6, OTBR, mDNS/Avahi, stale records, and OCW retry.
```

- [ ] **Step 5: Confirm secrets are not logged**

Run:

```powershell
git grep -n "34970112332\|ohab_\|hex:" -- app\src docs
```

Expected: No newly added persistent log strings expose real setup codes, REST tokens, or Thread datasets. Test fixtures may contain dummy values only.

- [ ] **Step 6: Final status**

Run:

```powershell
git status --short
```

Expected: clean except for user-owned unrelated files that were already dirty before this plan was executed.
