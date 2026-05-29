package org.openhab.matter.companion.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.PhoneMatterDevice

class MatterSetupAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstRunSetupScreenShowsOpenHabSettings() {
        render(MatterSetupUiState.initial(openHabConfigured = false))

        composeRule.onNodeWithText("Connect to openHAB").assertIsDisplayed()
        composeRule.onNodeWithText("openHAB address").assertIsDisplayed()
        composeRule.onNodeWithText("Access token").assertIsDisplayed()
        composeRule.onNodeWithText("Test connection").assertIsDisplayed()
        composeRule.onNodeWithText("Devices on this phone").assertIsDisplayed()
    }

    @Test
    fun readyToScanScreenShowsQrAndSettingsActions() {
        render(MatterSetupUiState.initial(openHabConfigured = true))

        composeRule.onNodeWithText("Add Matter device").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR code").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun progressScreenShowsStepsCurrentDetailAndCountdown() {
        render(
            MatterSetupUiState.progress(
                MatterSetupStage.CommissioningWindowOpen,
                countdownSeconds = 300,
                activeDetail = "Waiting for device on the Thread network"
            )
        )

        composeRule.onNodeWithText("Setting up your device").assertIsDisplayed()
        composeRule.onNodeWithText("Opening pairing window").assertIsDisplayed()
        composeRule.onNodeWithText("Waiting for device on the Thread network").assertIsDisplayed()
        composeRule.onNodeWithText("Pairing window open for 5:00").assertIsDisplayed()
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
    fun phoneDeviceListShowsEmptyState() {
        render(MatterSetupStateReducer.phoneDeviceList(hasDevices = false))

        composeRule.onNodeWithText("No staged Matter devices are stored on this phone.").assertIsDisplayed()
        composeRule.onNodeWithText("Back to settings").assertIsDisplayed()
    }

    private fun render(
        state: MatterSetupUiState,
        phoneDevices: List<PhoneMatterDevice> = emptyList()
    ) {
        composeRule.setContent {
            MatterSetupApp(
                state = state,
                openHabUrl = "http://openhab.local:8080",
                token = "",
                threadDataset = "",
                otbrBaseUrl = "",
                attestationBypassEnabled = false,
                threadSettingsMessage = "",
                threadBorderRouters = emptyList(),
                threadBorderRouterDiscoveryInProgress = false,
                phoneDevices = phoneDevices,
                ipv6DiagnosticAddress = "",
                onOpenHabUrlChange = {},
                onTokenChange = {},
                onThreadDatasetChange = {},
                onOtbrBaseUrlChange = {},
                onAttestationBypassChange = {},
                onIpv6DiagnosticAddressChange = {},
                onAction = {}
            )
        }
    }
}
