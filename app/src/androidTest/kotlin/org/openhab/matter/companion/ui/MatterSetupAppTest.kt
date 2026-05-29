package org.openhab.matter.companion.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupConfigCompleteness
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.MatterSetupDeviceIdentity
import org.openhab.matter.companion.setup.PhoneMatterDevice

class MatterSetupAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstRunSetupScreenShowsWelcome() {
        render(MatterSetupUiState.initial(openHabConfigured = false))

        composeRule.onNodeWithText("Set up Matter with openHAB").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun requiredSetupScreenShowsOpenHabSettingsWithDefaultAddress() {
        render(
            state = MatterSetupStateReducer.requiredSetup(""),
            openHabUrl = ""
        )

        composeRule.onNodeWithText("Connect to openHAB").assertIsDisplayed()
        composeRule.onNodeWithText("openHAB address").assertIsDisplayed()
        composeRule.onNodeWithText(MatterSetupConfigCompleteness.DefaultOpenHabUrl).assertIsDisplayed()
        composeRule.onNodeWithText("Access token").assertIsDisplayed()
        composeRule.onNodeWithText("Test settings").assertIsDisplayed()
        composeRule.onNodeWithText("Devices on this phone").assertIsDisplayed()
    }

    @Test
    fun readyToScanScreenShowsQrManualAndSettingsActions() {
        render(MatterSetupUiState.initial(openHabConfigured = true))

        composeRule.onNodeWithText("Add Matter device").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR code").assertIsDisplayed()
        composeRule.onNodeWithText("Enter code manually").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun readyToScanManualCodeButtonDispatchesAction() {
        val actions = mutableListOf<MatterSetupAction>()
        render(MatterSetupUiState.initial(openHabConfigured = true), onAction = actions::add)

        composeRule.onNodeWithText("Enter code manually").performClick()

        assertTrue(actions.contains(MatterSetupAction.EnterCodeManually))
    }

    @Test
    fun systemBackFromSettingsReturnsToMainMenu() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupStateReducer.settings(),
            onAction = actions::add
        )

        pressBack()

        assertTrue(actions.contains(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun manualCodeScreenShowsCodeEntryActions() {
        render(
            MatterSetupUiState(
                stage = MatterSetupStage.EnteringManualCode,
                title = "Enter setup code",
                message = "Type the Matter setup code printed on the device or box.",
                primaryAction = MatterSetupAction.SubmitManualCode,
                primaryActionLabel = "Continue",
                secondaryActions = listOf(MatterSetupAction.BackToMainMenu)
            )
        )

        composeRule.onNodeWithText("Enter setup code").assertIsDisplayed()
        composeRule.onNodeWithText("Pairing code").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun manualCodeContinueDispatchesSubmitAction() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupUiState(
                stage = MatterSetupStage.EnteringManualCode,
                title = "Enter setup code",
                message = "Type the Matter setup code printed on the device or box.",
                primaryAction = MatterSetupAction.SubmitManualCode,
                primaryActionLabel = "Continue",
                secondaryActions = listOf(MatterSetupAction.BackToMainMenu)
            ),
            manualSetupCode = "pin=20202021;disc=3840",
            onAction = actions::add
        )

        composeRule.onNodeWithText("Continue").performClick()

        assertTrue(actions.contains(MatterSetupAction.SubmitManualCode))
    }

    @Test
    fun progressScreenShowsStepsCurrentDetailAndCountdown() {
        render(
            MatterSetupUiState.progress(
                MatterSetupStage.CommissioningWindowOpen,
                countdownSeconds = 300,
                activeDetail = "Waiting for device on the Thread network",
                deviceIdentity = MatterSetupDeviceIdentity(vendorName = "Aqara", productName = "U200")
            )
        )

        composeRule.onNodeWithText("Setting up your device").assertIsDisplayed()
        composeRule.onNodeWithText("Opening pairing window").assertIsDisplayed()
        composeRule.onNodeWithText("Waiting for device on the Thread network").assertIsDisplayed()
        composeRule.onNodeWithText("Pairing window open for 5:00").assertIsDisplayed()
        composeRule.onNodeWithText("Vendor").assertIsDisplayed()
        composeRule.onNodeWithText("Aqara").assertIsDisplayed()
        composeRule.onNodeWithText("Product").assertIsDisplayed()
        composeRule.onNodeWithText("U200").assertIsDisplayed()
    }

    @Test
    fun failureScreenShowsDiagnosticsEntryPoint() {
        render(
            MatterSetupUiState.failed(
                MatterSetupFailure(
                    step = MatterSetupStage.WatchingOpenHabInbox,
                    message = "openHAB did not report the device yet",
                    details = "No Matter Inbox entry before timeout"
                ),
                MatterSetupDiagnosticsSummary.empty()
            )
        )

        composeRule.onNodeWithText("Setup needs attention").assertIsDisplayed()
        composeRule.onNodeWithText("Show troubleshooting").assertIsDisplayed()
    }

    @Test
    fun advancedTroubleshootingShowsRecoveryActions() {
        render(
            MatterSetupUiState(
                stage = MatterSetupStage.AdvancedTroubleshooting,
                title = "Advanced troubleshooting",
                message = "Review setup diagnostics before trying again.",
                primaryAction = MatterSetupAction.Retry,
                primaryActionLabel = "Back to setup",
                diagnostics = MatterSetupDiagnosticsSummary(
                    checks = listOf("openHAB URL configured=true"),
                    warnings = listOf("openHAB did not report the device yet"),
                    details = listOf("Failed at WatchingOpenHabInbox")
                )
            )
        )

        composeRule.onNodeWithText("Advanced troubleshooting").assertIsDisplayed()
        composeRule.onNodeWithText("Back to setup").assertIsDisplayed()
        composeRule.onNodeWithText("Devices on this phone").assertIsDisplayed()
        composeRule.onNodeWithText("Browse Matter services").assertIsDisplayed()
        composeRule.onNodeWithText("Device IPv6 address").assertIsDisplayed()
        composeRule.onNodeWithText("Check IPv6 reachability").assertIsDisplayed()
    }

    @Test
    fun phoneDeviceListShowsStoredDeviceActions() {
        render(
            state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true),
            phoneDevices = listOf(
                PhoneMatterDevice(
                    nodeId = 1234L,
                    controllerStateStored = true,
                    stateReadable = true
                )
            )
        )

        composeRule.onNodeWithText("Devices on this phone").assertIsDisplayed()
        composeRule.onNodeWithText("0x4D2").assertIsDisplayed()
        composeRule.onNodeWithText("Open pairing window again").assertIsDisplayed()
        composeRule.onNodeWithText("Forget from this phone").assertIsDisplayed()
        composeRule.onNodeWithText("Back to settings").assertIsDisplayed()
    }

    @Test
    fun phoneDeviceListExplainsWhenPairingWindowRetryIsUnavailable() {
        render(
            state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true),
            phoneDevices = listOf(
                PhoneMatterDevice(
                    nodeId = 1234L,
                    controllerStateStored = true,
                    stateReadable = false
                )
            )
        )

        composeRule.onNodeWithText("Stored state is unreadable").assertIsDisplayed()
        composeRule.onNodeWithText("Pairing window retry needs a readable stored node and controller state.")
            .assertIsDisplayed()
    }

    @Test
    fun phoneDeviceListAllowsDebugPairingWindowAttemptWhenControllerStateIsMissing() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupStateReducer.phoneDeviceList(hasDevices = true),
            phoneDevices = listOf(
                PhoneMatterDevice(
                    nodeId = 1234L,
                    controllerStateStored = false,
                    stateReadable = true
                )
            ),
            onAction = actions::add
        )

        composeRule.onNodeWithText("Controller state is missing").assertIsDisplayed()
        composeRule.onNodeWithText("Open pairing window again").performClick()

        assertTrue(actions.contains(MatterSetupAction.OpenCommissioningWindowAgain))
    }

    @Test
    fun phoneDeviceListShowsEmptyState() {
        render(MatterSetupStateReducer.phoneDeviceList(hasDevices = false))

        composeRule.onNodeWithText("No staged Matter devices are stored on this phone.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to settings").assertIsDisplayed()
    }

    private fun render(
        state: MatterSetupUiState,
        phoneDevices: List<PhoneMatterDevice> = emptyList(),
        manualSetupCode: String = "",
        openHabUrl: String = "http://openhab.local:8080",
        onAction: (MatterSetupAction) -> Unit = {}
    ) {
        composeRule.setContent {
            MatterSetupApp(
                state = state,
                openHabUrl = openHabUrl,
                token = "",
                threadDataset = "",
                otbrBaseUrl = "",
                attestationBypassEnabled = false,
                threadSettingsMessage = "",
                threadBorderRouters = emptyList(),
                threadBorderRouterDiscoveryInProgress = false,
                phoneDevices = phoneDevices,
                ipv6DiagnosticAddress = "",
                manualSetupCode = manualSetupCode,
                onOpenHabUrlChange = {},
                onTokenChange = {},
                onThreadDatasetChange = {},
                onOtbrBaseUrlChange = {},
                onAttestationBypassChange = {},
                onIpv6DiagnosticAddressChange = {},
                onManualSetupCodeChange = {},
                onAction = onAction
            )
        }
    }
}
