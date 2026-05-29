package org.openhab.matter.companion.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.openhab.matter.companion.setup.MatterSetupAction
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
    fun firstRunShowsWelcomeScreen() {
        render(MatterSetupStateReducer.reset(setupComplete = false, openHabUrl = "http://openhab:8080"))

        composeRule.onNodeWithText("Set up Matter with openHAB").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
        composeRule.onNodeWithText("Easy and guided").assertIsDisplayed()
    }

    @Test
    fun requiredSetupScreenShowsAllRequiredFields() {
        render(
            state = MatterSetupStateReducer.requiredSetup("http://openhab:8080"),
            openHabUrl = "http://openhab:8080"
        )

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
    fun requiredSetupTroubleshootingDispatchesAction() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupStateReducer.requiredSetup("http://openhab:8080"),
            openHabUrl = "http://openhab:8080",
            onAction = actions::add
        )

        composeRule.onNodeWithText("Advanced troubleshooting").performClick()

        assertTrue(actions.contains(MatterSetupAction.ShowTroubleshooting))
    }

    @Test
    fun addMatterDeviceScreenShowsScanManualAndSettingsCog() {
        render(MatterSetupUiState.addMatterDevice())

        composeRule.onNodeWithText("Add Matter device").assertIsDisplayed()
        composeRule.onNodeWithText("Scan code").assertIsDisplayed()
        composeRule.onNodeWithText("Enter code manually").assertIsDisplayed()
        composeRule.onNodeWithText("⚙").assertIsDisplayed()
        composeRule.onNodeWithText("You can enter the 11-digit setup code instead.").assertIsDisplayed()
    }

    @Test
    fun getStartedDispatchesAction() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupStateReducer.reset(setupComplete = false, openHabUrl = "http://openhab:8080"),
            onAction = actions::add
        )

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
    fun settingsScreenShowsOrganizedSections() {
        render(
            state = MatterSetupStateReducer.settings(),
            openHabTokenStored = true
        )

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
    fun settingsScreenShowsMissingWhenEditableTokenBlankWithoutStoredToken() {
        render(
            state = MatterSetupStateReducer.settings(),
            openHabTokenStored = false,
            threadDataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1"
        )

        composeRule.onNodeWithText("Access token").assertIsDisplayed()
        composeRule.onNodeWithText("Missing").assertIsDisplayed()
        composeRule.onNodeWithText("Not set").assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsSetWhenStoredTokenIsHiddenFromEditableField() {
        render(
            state = MatterSetupStateReducer.settings(),
            token = "",
            openHabTokenStored = true
        )

        composeRule.onNodeWithText("Access token").assertIsDisplayed()
        composeRule.onNodeWithText("Set").assertIsDisplayed()
        composeRule.onNodeWithText("Stored securely").assertIsDisplayed()
    }

    @Test
    fun settingsEditThreadDispatchesAction() {
        val actions = mutableListOf<MatterSetupAction>()
        render(
            state = MatterSetupStateReducer.settings(),
            onAction = actions::add
        )

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

    @Test
    fun changeTokenCheckingDoesNotExposeThreadFields() {
        render(
            state = MatterSetupStateReducer.changeTokenChecking(),
            token = "new.secret",
            threadDataset = "hex:0E080000000000010000",
            otbrBaseUrl = "fd00::1"
        )

        composeRule.onNodeWithText("Change token").assertIsDisplayed()
        composeRule.onNodeWithText("Checking...").assertIsDisplayed()
        composeRule.onAllNodesWithText("Active Operational Dataset").assertCountEquals(0)
        composeRule.onAllNodesWithText("Thread Border Router address").assertCountEquals(0)
    }

    @Test
    fun changeTokenScreenCanToggleTokenRevealControl() {
        render(
            state = MatterSetupStateReducer.changeToken(),
            token = "new.secret"
        )

        composeRule.onNodeWithText("Show").performClick()

        composeRule.onNodeWithText("Hide").assertIsDisplayed()
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
        token: String = "",
        openHabTokenStored: Boolean = false,
        threadDataset: String = "",
        otbrBaseUrl: String = "",
        onAction: (MatterSetupAction) -> Unit = {}
    ) {
        composeRule.setContent {
            MatterSetupApp(
                state = state,
                openHabUrl = openHabUrl,
                token = token,
                openHabTokenStored = openHabTokenStored,
                threadDataset = threadDataset,
                otbrBaseUrl = otbrBaseUrl,
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

    private fun assertOpenHabLogoAndMatterVisualsCanRender() {
        composeRule.onNodeWithText("Set up Matter with openHAB").assertIsDisplayed()
    }
}
