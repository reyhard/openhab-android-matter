package org.openhab.matter.companion.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.openhab.matter.companion.config.AppConfig
import org.openhab.matter.companion.config.SharedPreferencesAppConfigRepository
import org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactory
import org.openhab.matter.companion.controller.FakeMatterController
import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.MatterBootstrapStateRepository
import org.openhab.matter.companion.controller.MatterDeviceDetails
import org.openhab.matter.companion.controller.NativeChipControllerSession
import org.openhab.matter.companion.controller.SharedPreferencesMatterBootstrapStateRepository
import org.openhab.matter.companion.diagnostics.AndroidReadinessProbe
import org.openhab.matter.companion.diagnostics.AndroidMatterMdnsBrowser
import org.openhab.matter.companion.diagnostics.AndroidThreadBorderRouterBrowser
import org.openhab.matter.companion.diagnostics.DefaultIpv6ReachabilityProbe
import org.openhab.matter.companion.diagnostics.MatterMdnsRecord
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.domain.MatterSetupPayloadParser
import org.openhab.matter.companion.domain.ThreadDataset
import org.openhab.matter.companion.openhab.HttpOpenHabClient
import org.openhab.matter.companion.openhab.HttpOpenHabInboxClient
import org.openhab.matter.companion.openhab.HttpOpenHabMatterDiscoveryClient
import org.openhab.matter.companion.openhab.OpenHabInboxStatus
import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.otbr.HttpOtbrClient
import org.openhab.matter.companion.setup.AndroidMatterSetupPorts
import org.openhab.matter.companion.setup.FirstRunSettingsValidator
import org.openhab.matter.companion.setup.MatterDeviceDetailFormatter
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupConfig
import org.openhab.matter.companion.setup.MatterSetupConfigCompleteness
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsContext
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupDeviceIdentity
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupPorts
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.MatterSetupWorkflow
import org.openhab.matter.companion.setup.PhoneMatterDevice
import org.openhab.matter.companion.setup.PhoneMatterDeviceDetails
import org.openhab.matter.companion.setup.ThreadDatasetSettingsStatus
import org.openhab.matter.companion.setup.ThreadDatasetSettingsValidator
import org.openhab.matter.companion.setup.WorkflowExecutionGate
import org.openhab.matter.companion.setup.phoneMatterDeviceDetailsFromControllerDetails
import org.openhab.matter.companion.setup.sanitizeLogUrls
import org.openhab.matter.companion.setup.toLogSafeUrl

internal fun resolveEffectiveSetupToken(editableToken: String, persistedToken: String): String {
    return editableToken.trim().ifBlank { persistedToken }
}

private const val TAG = "MatterSetupViewModel"

internal fun threadSettingsConfigForSave(
    existingConfig: AppConfig,
    safeDataset: String,
    openHabBaseUrl: String,
    otbrBaseUrl: String,
    attestationBypassEnabled: Boolean
): AppConfig {
    return AppConfig(
        safeDataset,
        existingConfig.setupPayload(),
        openHabBaseUrl,
        existingConfig.openHabApiToken(),
        otbrBaseUrl,
        false,
        existingConfig.setupPayloadUnreadable(),
        existingConfig.openHabApiTokenUnreadable(),
        attestationBypassEnabled
    )
}

internal fun openHabConnectionConfigForSave(
    existingConfig: AppConfig,
    openHabBaseUrl: String,
    editableToken: String
): AppConfig {
    val trimmedToken = editableToken.trim()
    return AppConfig(
        existingConfig.threadDataset(),
        existingConfig.setupPayload(),
        openHabBaseUrl,
        trimmedToken.ifBlank { existingConfig.openHabApiToken() },
        existingConfig.otbrBaseUrl(),
        existingConfig.threadDatasetUnreadable(),
        existingConfig.setupPayloadUnreadable(),
        if (trimmedToken.isBlank()) existingConfig.openHabApiTokenUnreadable() else false,
        existingConfig.attestationBypassEnabled()
    )
}

internal fun shouldRefreshOpenHabConnectionAfterAction(action: MatterSetupAction): Boolean {
    return action in setOf(
        MatterSetupAction.AddAnotherDevice,
        MatterSetupAction.BackToMainMenu,
        MatterSetupAction.BackToSettings,
        MatterSetupAction.EditSettings,
        MatterSetupAction.SaveOpenHabAddress,
        MatterSetupAction.SaveChangedToken
    )
}

internal fun resolvePhoneDeviceReturnAction(state: MatterSetupUiState): MatterSetupAction {
    return when (state.stage) {
        MatterSetupStage.AdvancedTroubleshooting,
        MatterSetupStage.PhoneDeviceList,
        MatterSetupStage.PhoneDeviceDetails -> state.primaryAction ?: MatterSetupAction.BackToSettings

        else -> MatterSetupAction.BackToSettings
    }
}

internal fun resolvePhoneDeviceDetailsSelection(
    nodeId: Long?,
    phoneDevices: List<PhoneMatterDevice>
): PhoneMatterDevice? {
    return nodeId?.let { requestedNodeId ->
        phoneDevices.firstOrNull { it.nodeId == requestedNodeId }
    } ?: phoneDevices.singleOrNull()
}

internal fun phoneDeviceDetailsFromControllerDetails(
    details: MatterDeviceDetails,
    device: PhoneMatterDevice
): PhoneMatterDeviceDetails {
    return phoneMatterDeviceDetailsFromControllerDetails(details, device)
}

internal fun mergeMatterDeviceDetails(
    existing: MatterDeviceDetails,
    update: MatterDeviceDetails
): MatterDeviceDetails {
    return MatterDeviceDetails.Builder()
        .vendorName(update.vendorName().ifBlank { existing.vendorName() })
        .productName(update.productName().ifBlank { existing.productName() })
        .softwareVersionString(update.softwareVersionString().ifBlank { existing.softwareVersionString() })
        .hardwareVersionString(update.hardwareVersionString().ifBlank { existing.hardwareVersionString() })
        .partNumber(update.partNumber().ifBlank { existing.partNumber() })
        .batteryPercentRemaining(update.batteryPercentRemaining() ?: existing.batteryPercentRemaining())
        .batteryQuantity(update.batteryQuantity() ?: existing.batteryQuantity())
        .batteryDesignation(update.batteryDesignation().ifBlank { existing.batteryDesignation() })
        .threadNetworkName(update.threadNetworkName().ifBlank { existing.threadNetworkName() })
        .threadChannel(update.threadChannel() ?: existing.threadChannel())
        .ipv6Address(update.ipv6Address().ifBlank { existing.ipv6Address() })
        .otaUpdatePossible(update.otaUpdatePossible() ?: existing.otaUpdatePossible())
        .build()
}

internal fun phoneDeviceDetailsFetchStarted(state: MatterSetupUiState): MatterSetupUiState {
    return state.copy(
        phoneDeviceDetailsFetching = true,
        phoneDeviceDetailsMessage = ""
    )
}

internal fun phoneDeviceDetailsFetchSucceeded(
    state: MatterSetupUiState,
    update: PhoneMatterDeviceDetails
): MatterSetupUiState {
    return state.copy(
        phoneDeviceDetails = state.phoneDeviceDetails.merge(update),
        phoneDeviceDetailsFetching = false,
        phoneDeviceDetailsMessage = "Device data refreshed"
    )
}

internal fun phoneDeviceDetailsFetchFailed(state: MatterSetupUiState): MatterSetupUiState {
    return state.copy(
        phoneDeviceDetailsFetching = false,
        phoneDeviceDetailsMessage = "Could not fetch data from device"
    )
}

internal fun isCurrentPhoneDeviceDetailsRequest(
    state: MatterSetupUiState,
    selectedDevice: PhoneMatterDevice?,
    requestNodeId: Long
): Boolean {
    return state.stage == MatterSetupStage.PhoneDeviceDetails &&
        selectedDevice?.nodeId == requestNodeId
}

class MatterSetupViewModel @JvmOverloads constructor(
    application: Application,
    private val bootstrapStateRepositoryOverride: MatterBootstrapStateRepository? = null,
    private val controllerSessionOverride: NativeChipControllerSession? = null,
    initialize: Boolean = true
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(MatterSetupUiState.initial(openHabConfigured = false))
        private set
    var openHabUrl by mutableStateOf("")
        private set
    var token by mutableStateOf("")
        private set
    var openHabTokenStored by mutableStateOf(false)
        private set
    var threadDataset by mutableStateOf("")
        private set
    var otbrBaseUrl by mutableStateOf("")
        private set
    var attestationBypassEnabled by mutableStateOf(false)
        private set
    var threadSettingsMessage by mutableStateOf("")
        private set
    var threadNetworkState by mutableStateOf(ThreadNetworkUiState.notChecked("", ""))
        private set
    var openHabConnectionState by mutableStateOf(OpenHabConnectionUiState.notChecked(tokenSet = false))
        private set
    var threadBorderRouters by mutableStateOf<List<ThreadBorderRouterRecord>>(emptyList())
        private set
    var threadBorderRouterDiscoveryInProgress by mutableStateOf(false)
        private set
    var phoneDevices by mutableStateOf<List<PhoneMatterDevice>>(emptyList())
        private set
    var scanReadiness by mutableStateOf(ScanReadinessUiState.ready())
        private set
    var ipv6DiagnosticAddress by mutableStateOf("")
        private set
    var manualSetupCode by mutableStateOf("")
        private set

    private var scannedPayload = ""
    private val appContext = application.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executionGate = WorkflowExecutionGate()
    private var workerThread: Thread? = null
    private var selectedPhoneDevice: PhoneMatterDevice? = null

    private val configRepository by lazy { SharedPreferencesAppConfigRepository(appContext) }
    private val bootstrapStateRepository by lazy {
        bootstrapStateRepositoryOverride ?: SharedPreferencesMatterBootstrapStateRepository(appContext)
    }
    private val openHabClient by lazy { HttpOpenHabClient() }
    private val otbrClient by lazy { HttpOtbrClient() }
    private val openHabMatterDiscoveryClient by lazy { HttpOpenHabMatterDiscoveryClient() }
    private val openHabInboxClient by lazy { HttpOpenHabInboxClient() }
    private val fakeMatterController by lazy { FakeMatterController() }
    private val readinessProbe by lazy { AndroidReadinessProbe(appContext) }
    private val matterMdnsBrowser by lazy { AndroidMatterMdnsBrowser(appContext) }
    private val ipv6ReachabilityProbe by lazy { DefaultIpv6ReachabilityProbe() }
    private val threadBorderRouterBrowser by lazy { AndroidThreadBorderRouterBrowser(appContext) }
    private var openHabConfigured = false
    private val controllerSession by lazy {
        controllerSessionOverride ?: newNativeControllerSession(loadMatterSetupConfig().attestationBypassEnabled)
    }

    init {
        if (initialize) {
            restorePersistedConfig(maskToken = true)
            refreshScanReadiness()
            refreshOpenHabConnectionStatus()
            refreshThreadNetworkStatus()
            uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
        }
    }

    fun onOpenHabUrlChange(value: String) {
        openHabUrl = value
        openHabConnectionState = OpenHabConnectionUiState.notChecked(openHabTokenStored || token.trim().isNotBlank())
        if (uiState.stage == MatterSetupStage.NeedsOpenHabSetup) {
            val failure = uiState.failure
            uiState = if (failure == null) {
                MatterSetupStateReducer.requiredSetup(value)
            } else {
                MatterSetupStateReducer.openHabSetupNotReady(
                    value,
                    uiState.message,
                    failure,
                    uiState.diagnostics
                )
            }
        }
    }

    fun onTokenChange(value: String) {
        token = value
        openHabConnectionState = OpenHabConnectionUiState.notChecked(openHabTokenStored || value.trim().isNotBlank())
    }

    fun onThreadDatasetChange(value: String) {
        threadDataset = value
        threadNetworkState = ThreadNetworkUiState.notChecked(threadDataset, otbrBaseUrl)
    }

    fun onOtbrBaseUrlChange(value: String) {
        otbrBaseUrl = value
        threadNetworkState = ThreadNetworkUiState.notChecked(threadDataset, otbrBaseUrl)
    }

    fun onAttestationBypassChange(value: Boolean) {
        attestationBypassEnabled = value
        saveAttestationBypass(value)
    }

    fun onIpv6DiagnosticAddressChange(value: String) {
        ipv6DiagnosticAddress = value
    }

    fun onManualSetupCodeChange(value: String) {
        manualSetupCode = value
    }

    fun handleAction(action: MatterSetupAction) {
        when (action) {
            MatterSetupAction.StartScan -> {
                refreshScanReadiness()
                uiState = scanningState()
            }

            MatterSetupAction.OpenBluetoothSettings,
            MatterSetupAction.OpenLocationSettings,
            MatterSetupAction.RequestSetupPermissions -> {
                refreshScanReadiness()
            }

            MatterSetupAction.OpenOpenHabInbox -> {
                // Handled by MatterSetupActivity because opening a browser is an Android UI side effect.
            }

            MatterSetupAction.ConfirmPairingMode -> {
                if (scannedPayload.isBlank()) {
                    uiState = MatterSetupUiState.failed(
                        MatterSetupFailure(
                            step = MatterSetupStage.QrScanned,
                            message = "Scan a Matter QR code before starting setup."
                        ),
                        MatterSetupDiagnosticsSummary.empty()
                    )
                } else {
                    startRealWorkflow(scannedPayload)
                }
            }

            MatterSetupAction.Retry,
            MatterSetupAction.AddAnotherDevice -> {
                scannedPayload = ""
                manualSetupCode = ""
                uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
                refreshOpenHabConnectionAfterNavigation(action)
                refreshThreadNetworkStatus()
            }

            MatterSetupAction.BackToMainMenu -> {
                scannedPayload = ""
                manualSetupCode = ""
                restorePersistedConfig(maskToken = true)
                uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
                refreshOpenHabConnectionAfterNavigation(action)
                refreshThreadNetworkStatus()
            }

            MatterSetupAction.BackToSettings -> {
                restorePersistedConfig(maskToken = true)
                refreshPhoneDevices()
                uiState = MatterSetupStateReducer.settings()
                refreshOpenHabConnectionAfterNavigation(action)
                refreshThreadNetworkStatus()
            }

            MatterSetupAction.BackToRequiredSetup -> {
                uiState = MatterSetupStateReducer.requiredSetup(openHabUrl)
            }

            MatterSetupAction.SaveOpenHab,
            MatterSetupAction.TestSettings -> {
                startFirstRunSettingsCheck()
            }

            MatterSetupAction.TestOpenHab -> {
                startOpenHabConnectionTest()
            }

            MatterSetupAction.GetStarted -> {
                openHabUrl = effectiveOpenHabUrl()
                uiState = MatterSetupStateReducer.requiredSetup(openHabUrl)
            }

            MatterSetupAction.EditSettings -> {
                restorePersistedConfig(maskToken = true)
                refreshPhoneDevices()
                uiState = MatterSetupStateReducer.settings()
                refreshOpenHabConnectionAfterNavigation(action)
                refreshThreadNetworkStatus()
            }

            MatterSetupAction.EditOpenHabAddress -> {
                prepareOpenHabConnectionEditor()
            }

            MatterSetupAction.SaveOpenHabAddress -> {
                saveOpenHabConnection()
                refreshOpenHabConnectionAfterNavigation(action)
            }

            MatterSetupAction.ChangeToken -> {
                prepareOpenHabConnectionEditor()
            }

            MatterSetupAction.SaveChangedToken -> {
                saveOpenHabConnection()
                refreshOpenHabConnectionAfterNavigation(action)
            }

            MatterSetupAction.EditThreadNetwork -> {
                uiState = MatterSetupStateReducer.threadNetworkEditor()
            }

            MatterSetupAction.ShowPhoneDevices -> {
                val returnAction = phoneDeviceReturnAction()
                refreshPhoneDevices()
                if (phoneDevices.isEmpty()) {
                    selectedPhoneDevice = null
                }
                uiState = MatterSetupStateReducer.phoneDeviceList(
                    hasDevices = phoneDevices.isNotEmpty(),
                    returnAction = returnAction
                )
            }

            is MatterSetupAction.ShowPhoneDeviceDetails -> {
                showPhoneDeviceDetails(action.nodeId)
            }

            MatterSetupAction.FetchPhoneDeviceDetails -> {
                fetchPhoneDeviceDetails()
            }

            MatterSetupAction.AcknowledgePhoneDeviceDetailsMessage -> {
                acknowledgePhoneDeviceDetailsMessage()
            }

            MatterSetupAction.CheckThreadDataset -> {
                refreshThreadNetworkStatus()
            }

            MatterSetupAction.SaveThreadSettings -> {
                saveThreadSettings()
            }

            MatterSetupAction.DetectThreadBorderRouters -> {
                detectThreadBorderRouters()
            }

            is MatterSetupAction.SelectThreadBorderRouter -> {
                otbrBaseUrl = action.endpoint
                threadSettingsMessage = "Selected Thread Border Router: ${action.endpoint}"
                threadNetworkState = ThreadNetworkUiState.notChecked(threadDataset, otbrBaseUrl)
            }

            MatterSetupAction.EnterCodeManually -> {
                uiState = MatterSetupStateReducer.manualCodeEntry()
            }

            MatterSetupAction.SubmitManualCode -> {
                onQrPayloadScanned(manualSetupCode)
            }

            MatterSetupAction.ShowTroubleshooting -> {
                uiState = MatterSetupStateReducer.advancedTroubleshooting(uiState)
            }

            MatterSetupAction.BrowseMatterServices -> {
                browseMatterServices()
            }

            MatterSetupAction.CheckIpv6Reachability -> {
                checkIpv6Reachability()
            }

            MatterSetupAction.OpenCommissioningWindowAgain -> {
                startOpenCommissioningWindowAgain()
            }

            MatterSetupAction.ForgetFromPhone -> {
                forgetStagedDeviceFromPhone()
            }
        }
    }

    fun onQrPayloadScanned(payload: String) {
        if (payload.isBlank()) {
            uiState = MatterSetupUiState.failed(
                MatterSetupFailure(
                    step = MatterSetupStage.ScanningQr,
                    message = "The QR code was not a valid Matter setup code."
                ),
                MatterSetupDiagnosticsSummary.empty()
            )
            return
        }

        scannedPayload = payload
        uiState = MatterSetupUiState(
            stage = MatterSetupStage.NeedsPairingMode,
            title = "Put device in pairing mode",
            message = "Make sure the Matter device is powered on and ready before continuing.",
            primaryAction = MatterSetupAction.ConfirmPairingMode,
            primaryActionLabel = "Device is ready"
        )
    }

    override fun onCleared() {
        executionGate.close()
        workerThread?.interrupt()
        workerThread = null
        super.onCleared()
    }

    private fun startFirstRunSettingsCheck() {
        val baseUrl = effectiveOpenHabUrl()
        val datasetInput = threadDataset
        val otbrTarget = otbrBaseUrl.trim()
        val existingConfig = configRepository.load()
        val apiToken = resolveEffectiveSetupToken(token, existingConfig.openHabApiToken())
        if (!executionGate.tryStart()) {
            return
        }

        uiState = MatterSetupStateReducer.openHabSetupChecking()
        workerThread = Thread({
            try {
                val openHabStatus = openHabClient.checkReadiness(baseUrl, apiToken)
                val otbrStatus = otbrClient.checkReadiness(otbrTarget)
                postThreadNetworkResult(ThreadNetworkUiState.fromInputs(datasetInput, otbrTarget, otbrStatus))
                val validation = FirstRunSettingsValidator.validate(
                    openHabUrl = baseUrl,
                    token = apiToken,
                    dataset = datasetInput,
                    otbrBaseUrl = otbrTarget,
                    openHabStatus = openHabStatus,
                    otbrStatus = otbrStatus
                )
                if (validation.ready) {
                    val safeDataset = ThreadDataset.parse(datasetInput).chipToolValue()
                    configRepository.save(
                        AppConfig(
                            safeDataset,
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
                    controllerSession.syncAttestationBypass(attestationBypassEnabled)
                postState {
                    openHabUrl = baseUrl
                    token = ""
                    openHabTokenStored = true
                    openHabConnectionState = OpenHabConnectionUiState.connected()
                    threadDataset = safeDataset
                    otbrBaseUrl = otbrTarget
                    openHabConfigured = true
                        threadSettingsMessage = ThreadDatasetSettingsValidator.validate(safeDataset).title
                        uiState = MatterSetupUiState.addMatterDevice()
                    }
                } else {
                    postOpenHabConnectionTestResult(OpenHabConnectionUiState.fromStatus(openHabStatus))
                    emitFirstRunSettingsFailure(baseUrl, existingConfig, apiToken, validation.warnings, validation.details)
                }
            } catch (error: Exception) {
                val existingConfig = runCatching { configRepository.load() }.getOrDefault(AppConfig("", ""))
                postOpenHabConnectionTestResult(OpenHabConnectionUiState.addressError())
                emitOpenHabSetupError(error, existingConfig, baseUrl, apiToken)
            } finally {
                executionGate.finish()
            }
        }, "first-run-settings-check")
        workerThread?.start()
    }

    private fun startChangedTokenCheck() {
        val baseUrl = effectiveOpenHabUrl()
        val apiToken = token.trim()
        if (apiToken.isBlank()) {
            val warning = "openHAB access token is required"
            uiState = MatterSetupStateReducer.changeToken().copy(
                message = warning,
                diagnostics = MatterSetupDiagnosticsSummary(
                    checks = emptyList(),
                    warnings = listOf(warning),
                    details = emptyList()
                )
            )
            return
        }
        if (!executionGate.tryStart()) {
            return
        }

        uiState = MatterSetupStateReducer.changeTokenChecking()
        workerThread = Thread({
            try {
                val existingConfig = configRepository.load()
                val status = openHabClient.checkReadiness(baseUrl, apiToken)
                if (status.online() && status.restReachable() && status.matterControllerReady()) {
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
                        openHabUrl = baseUrl
                        token = ""
                        openHabTokenStored = true
                        openHabConfigured = MatterSetupConfigCompleteness.isComplete(configRepository.load())
                        uiState = MatterSetupStateReducer.settings()
                    }
                } else {
                    emitChangedTokenFailure(status, existingConfig, baseUrl, apiToken)
                }
            } catch (error: Exception) {
                val existingConfig = runCatching { configRepository.load() }.getOrDefault(AppConfig("", ""))
                emitChangedTokenError(error, existingConfig, baseUrl, apiToken)
            } finally {
                executionGate.finish()
            }
        }, "changed-token-check")
        workerThread?.start()
    }

    private fun effectiveOpenHabUrl(): String {
        return openHabUrl.trim().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
    }

    private fun updateThreadDatasetValidationMessage(): Boolean {
        val validation = ThreadDatasetSettingsValidator.validate(threadDataset)
        threadSettingsMessage = "${validation.title} ${validation.message}"
        return validation.status == ThreadDatasetSettingsStatus.Valid
    }

    private fun prepareOpenHabConnectionEditor() {
        token = ""
        uiState = MatterSetupStateReducer.openHabAddressEditor()
    }

    private fun saveOpenHabConnection() {
        val baseUrl = effectiveOpenHabUrl()
        val existingConfig = configRepository.load()
        val updatedConfig = openHabConnectionConfigForSave(
            existingConfig = existingConfig,
            openHabBaseUrl = baseUrl,
            editableToken = token
        )
        configRepository.save(updatedConfig)
        openHabUrl = baseUrl
        token = ""
        openHabTokenStored = updatedConfig.openHabApiToken().isNotBlank()
        openHabConnectionState = OpenHabConnectionUiState.notChecked(openHabTokenStored)
        openHabConfigured = MatterSetupConfigCompleteness.isComplete(configRepository.load())
        uiState = MatterSetupStateReducer.settings()
    }

    private fun startOpenHabConnectionTest() {
        refreshOpenHabConnectionStatus()
    }

    private fun refreshOpenHabConnectionStatus() {
        val baseUrl = effectiveOpenHabUrl()
        val existingConfig = configRepository.load()
        val apiToken = resolveEffectiveSetupToken(token, existingConfig.openHabApiToken())
        if (apiToken.isBlank()) {
            openHabConnectionState = OpenHabConnectionUiState.missingToken()
            return
        }
        if (!executionGate.tryStart()) {
            return
        }

        openHabConnectionState = OpenHabConnectionUiState.checking()
        workerThread = Thread({
            try {
                val status = openHabClient.checkReadiness(baseUrl, apiToken)
                postOpenHabConnectionTestResult(OpenHabConnectionUiState.fromStatus(status))
            } catch (error: Exception) {
                postOpenHabConnectionTestResult(OpenHabConnectionUiState.addressError())
            } finally {
                executionGate.finish()
            }
        }, "openhab-connection-test")
        workerThread?.start()
    }

    private fun refreshOpenHabConnectionAfterNavigation(action: MatterSetupAction) {
        if (shouldRefreshOpenHabConnectionAfterAction(action)) {
            refreshOpenHabConnectionStatus()
        }
    }

    private fun postOpenHabConnectionTestResult(state: OpenHabConnectionUiState) {
        mainHandler.post {
            openHabConnectionState = state
        }
    }

    private fun saveAttestationBypass(value: Boolean) {
        val existingConfig = configRepository.load()
        configRepository.save(
            AppConfig(
                existingConfig.threadDataset(),
                existingConfig.setupPayload(),
                existingConfig.openHabBaseUrl(),
                existingConfig.openHabApiToken(),
                existingConfig.otbrBaseUrl(),
                existingConfig.threadDatasetUnreadable(),
                existingConfig.setupPayloadUnreadable(),
                existingConfig.openHabApiTokenUnreadable(),
                value
            )
        )
        controllerSession.syncAttestationBypass(value)
        openHabConfigured = MatterSetupConfigCompleteness.isComplete(configRepository.load())
    }

    private fun saveThreadSettings() {
        val parsedDataset = runCatching { ThreadDataset.parse(threadDataset) }.getOrElse {
            updateThreadDatasetValidationMessage()
            return
        }
        val existingConfig = configRepository.load()
        val safeDataset = parsedDataset.chipToolValue()
        configRepository.save(
            threadSettingsConfigForSave(
                existingConfig = existingConfig,
                safeDataset = safeDataset,
                openHabBaseUrl = effectiveOpenHabUrl(),
                otbrBaseUrl = otbrBaseUrl.trim(),
                attestationBypassEnabled = attestationBypassEnabled
            )
        )
        threadDataset = safeDataset
        controllerSession.syncAttestationBypass(attestationBypassEnabled)
        openHabConfigured = MatterSetupConfigCompleteness.isComplete(configRepository.load())
        threadSettingsMessage = "Saved Thread settings. Dataset value is stored encrypted and not shown in logs."
        refreshThreadNetworkStatus()
    }

    private fun refreshThreadNetworkStatus() {
        val datasetInput = threadDataset
        val otbrTarget = otbrBaseUrl.trim()
        val initialState = ThreadNetworkUiState.notChecked(datasetInput, otbrTarget)
        if (initialState.kind != ThreadNetworkStateKind.Unknown) {
            threadNetworkState = initialState
            threadSettingsMessage = "${initialState.title} ${initialState.message}"
            return
        }

        threadNetworkState = ThreadNetworkUiState.checking()
        threadSettingsMessage = "Checking Thread dataset and Border Router address."
        Thread({
            val status = otbrClient.checkReadiness(otbrTarget)
            postThreadNetworkResult(ThreadNetworkUiState.fromInputs(datasetInput, otbrTarget, status))
        }, "thread-network-check").start()
    }

    private fun postThreadNetworkResult(state: ThreadNetworkUiState) {
        mainHandler.post {
            if (executionGate.canEmit()) {
                threadNetworkState = state
                threadSettingsMessage = "${state.title} ${state.message}"
            }
        }
    }

    private fun detectThreadBorderRouters() {
        if (!executionGate.tryStart()) {
            return
        }
        threadBorderRouterDiscoveryInProgress = true
        threadSettingsMessage = "Looking for Thread Border Routers on the current network..."
        workerThread = Thread({
            try {
                val records = threadBorderRouterBrowser.browse(3_000L)
                postState {
                    threadBorderRouters = records
                    threadSettingsMessage = if (records.isEmpty()) {
                        "No Thread Border Routers were detected. Check Wi-Fi, VPN, mDNS, and that the Border Router is on this network."
                    } else {
                        "Detected ${records.size} Thread Border Router${if (records.size == 1) "" else "s"}. Tap one to use it as the diagnostic target."
                    }
                    threadBorderRouterDiscoveryInProgress = false
                }
            } finally {
                postState {
                    threadBorderRouterDiscoveryInProgress = false
                }
                executionGate.finish()
            }
        }, "thread-border-router-discovery")
        workerThread?.start()
    }

    private fun refreshPhoneDevices() {
        phoneDevices = PhoneMatterDevice.fromBootstrapState(bootstrapStateRepository.load())
            ?.let(::listOf)
            .orEmpty()
    }

    private fun showPhoneDeviceDetails(nodeId: Long?) {
        val returnAction = phoneDeviceReturnAction()
        val device = resolvePhoneDeviceDetailsSelection(nodeId, phoneDevices) ?: run {
            selectedPhoneDevice = null
            uiState = MatterSetupStateReducer.phoneDeviceList(
                hasDevices = phoneDevices.isNotEmpty(),
                returnAction = returnAction
            )
            return
        }
        selectedPhoneDevice = device
        uiState = MatterSetupStateReducer.phoneDeviceDetails(
            device = device,
            returnAction = returnAction
        )
    }

    private fun fetchPhoneDeviceDetails() {
        Log.i(
            TAG,
            "Phone device details fetch requested stage=${uiState.stage}, selectedNode=${selectedPhoneDevice?.displayNodeId}"
        )
        if (uiState.stage != MatterSetupStage.PhoneDeviceDetails) {
            Log.i(TAG, "Phone device details fetch ignored outside details stage")
            return
        }
        val requestNodeId = selectedPhoneDevice?.nodeId ?: run {
            Log.w(TAG, "Phone device details fetch failed before native call: no selected node")
            uiState = phoneDeviceDetailsFetchFailed(uiState)
            return
        }
        val bootstrapState = bootstrapStateRepository.load()
        val device = PhoneMatterDevice.fromBootstrapState(bootstrapState) ?: run {
            Log.w(TAG, "Phone device details fetch failed before native call: no stored bootstrap device")
            uiState = phoneDeviceDetailsFetchFailed(uiState)
            return
        }
        val controllerState = bootstrapState.controllerState()
        if (!device.stateReadable ||
            device.nodeId != requestNodeId
        ) {
            Log.w(
                TAG,
                "Phone device details fetch failed before native call: " +
                    "stateReadable=${device.stateReadable}, " +
                    "controllerStateStored=${device.controllerStateStored}, " +
                    "storedNode=${device.displayNodeId}, requestedNode=${MatterDeviceDetailFormatter.nodeId(requestNodeId)}"
            )
            uiState = phoneDeviceDetailsFetchFailed(uiState)
            return
        }
        if (!executionGate.tryStart()) {
            Log.i(TAG, "Phone device details fetch ignored because another workflow is running")
            return
        }
        uiState = phoneDeviceDetailsFetchStarted(uiState)
        workerThread = Thread({
            try {
                val selection = controllerSession.selectNativeIfReady()
                Log.i(
                    TAG,
                    "Phone device details native selection: native=${selection.nativeSelected()}, message=${selection.message()}"
                )
                if (!selection.nativeSelected()) {
                    throw IllegalStateException("connectedhomeip is not ready for device metadata.")
                }
                val details = selection.controller().readDeviceDetails(
                    requestNodeId,
                    controllerState,
                    { step ->
                        Log.i(
                            TAG,
                            "Phone device details native progress: complete=${step.complete()}, message=${step.message()}"
                        )
                    }
                )
                Log.i(TAG, "Phone device details native read returned empty=${details.isEmpty}")
                if (details.isEmpty) {
                    throw IllegalStateException("No Matter device details were returned.")
                }
                val update = phoneDeviceDetailsFromControllerDetails(details, device)
                persistFetchedPhoneDeviceDetails(requestNodeId, details)
                postState {
                    if (isCurrentPhoneDeviceDetailsRequest(uiState, selectedPhoneDevice, requestNodeId)) {
                        uiState = phoneDeviceDetailsFetchSucceeded(uiState, update)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Phone device details fetch failed", error)
                postState {
                    if (isCurrentPhoneDeviceDetailsRequest(uiState, selectedPhoneDevice, requestNodeId)) {
                        uiState = phoneDeviceDetailsFetchFailed(uiState)
                    }
                }
            } finally {
                executionGate.finish()
            }
        }, "matter-device-details-fetch")
        workerThread?.start()
    }

    private fun persistFetchedPhoneDeviceDetails(requestNodeId: Long, details: MatterDeviceDetails) {
        val currentState = bootstrapStateRepository.load()
        if (currentState.stateUnreadable() || currentState.bootstrapNodeId() != requestNodeId) {
            Log.w(
                TAG,
                "Phone device details persistence skipped: " +
                    "stateUnreadable=${currentState.stateUnreadable()}, " +
                    "storedNode=${MatterDeviceDetailFormatter.nodeId(currentState.bootstrapNodeId())}, " +
                    "requestedNode=${MatterDeviceDetailFormatter.nodeId(requestNodeId)}"
            )
            return
        }
        val mergedDetails = mergeMatterDeviceDetails(currentState.deviceDetails(), details)
        bootstrapStateRepository.save(
            MatterBootstrapState(
                currentState.bootstrapNodeId(),
                currentState.controllerState(),
                false,
                mergedDetails.vendorName().ifBlank { currentState.vendorName() },
                mergedDetails.productName().ifBlank { currentState.productName() },
                mergedDetails
            )
        )
    }

    private fun acknowledgePhoneDeviceDetailsMessage() {
        uiState = uiState.copy(phoneDeviceDetailsMessage = "")
    }

    fun refreshScanReadiness() {
        val bluetooth = readinessProbe.bluetoothDiagnostic()
        val location = readinessProbe.locationServicesDiagnostic()
        val missingPermissionDiagnostics = readinessProbe.permissionDiagnostics()
            .filter { !it.ok }
        val permissionsReady = missingPermissionDiagnostics.isEmpty()
        scanReadiness = ScanReadinessUiState(
            bluetoothReady = bluetooth.ok,
            locationReady = location.ok,
            permissionsReady = permissionsReady,
            bluetoothMessage = if (bluetooth.ok) "" else bluetooth.message,
            locationMessage = if (location.ok) "" else location.message,
            permissionsMessage = if (permissionsReady) {
                ""
            } else {
                "Grant required permissions: ${missingPermissionDiagnostics.joinToString { it.name }}."
            },
            bluetoothHelperAvailable = !bluetooth.ok && bluetooth.message.contains("Turn on", ignoreCase = true),
            locationHelperAvailable = !location.ok && location.message.contains("Turn on", ignoreCase = true),
            permissionsHelperAvailable = !permissionsReady ||
                bluetooth.message.contains("permission", ignoreCase = true) ||
                location.message.contains("permission", ignoreCase = true)
        )
    }

    private fun browseMatterServices() {
        if (!executionGate.tryStart()) {
            return
        }

        appendAdvancedDiagnostics(
            checks = listOf("Matter mDNS browse started from this phone.")
        )
        workerThread = Thread({
            val update = try {
                val records = matterMdnsBrowser.browseMatterCommissionable(3_000L) +
                    matterMdnsBrowser.browseMatterOperational(3_000L)
                val details = formatMatterMdnsRecords(records)
                val warnings = if (records.isEmpty()) {
                    listOf(
                        "No Matter mDNS services were discovered from this phone. The pairing window may have expired, mDNS may be blocked, or this phone may be on the wrong network."
                    )
                } else {
                    emptyList()
                }
                {
                    appendAdvancedDiagnostics(
                        checks = listOf("Matter mDNS services discovered=${records.size}"),
                        warnings = warnings,
                        details = details
                    )
                }
            } catch (error: Exception) {
                {
                    appendAdvancedDiagnostics(
                        warnings = listOf("Matter mDNS browse failed."),
                        details = listOf(error.message.orEmpty().ifBlank { error.javaClass.simpleName })
                    )
                }
            }
            mainHandler.post {
                if (executionGate.canEmit()) {
                    update()
                }
                executionGate.finish()
            }
        }, "matter-mdns-browse")
        workerThread?.start()
    }

    private fun checkIpv6Reachability() {
        val address = ipv6DiagnosticAddress.trim()
        if (address.isBlank()) {
            appendAdvancedDiagnostics(
                warnings = listOf("Enter a device IPv6 address before running the reachability check.")
            )
            return
        }
        if (!DefaultIpv6ReachabilityProbe.isIpv6Literal(address)) {
            appendAdvancedDiagnostics(
                warnings = listOf("Enter an IPv6 literal such as fd00::1234. IPv4 addresses and hostnames do not verify Thread IPv6 reachability.")
            )
            return
        }
        if (!executionGate.tryStart()) {
            return
        }

        appendAdvancedDiagnostics(
            checks = listOf("IPv6 reachability check started for $address.")
        )
        workerThread = Thread({
            val update = try {
                val reachable = ipv6ReachabilityProbe.isReachable(address, 2_000)
                val details = listOf(
                    "IPv6 reachability to $address: ${if (reachable) "reachable" else "not verified"}",
                    "Android reachability is best-effort. A failed check can mean ICMP is blocked or unavailable, not necessarily that the Matter device is offline."
                )
                val warnings = if (reachable) {
                    emptyList()
                } else {
                    listOf("This phone could not verify IPv6 reachability to $address.")
                }
                {
                    appendAdvancedDiagnostics(
                        checks = listOf("IPv6 reachability verified=$reachable"),
                        warnings = warnings,
                        details = details
                    )
                }
            } catch (error: Exception) {
                {
                    appendAdvancedDiagnostics(
                        warnings = listOf("IPv6 reachability check failed."),
                        details = listOf(error.message.orEmpty().ifBlank { error.javaClass.simpleName })
                    )
                }
            }
            mainHandler.post {
                if (executionGate.canEmit()) {
                    update()
                }
                executionGate.finish()
            }
        }, "matter-ipv6-reachability")
        workerThread?.start()
    }

    private fun formatMatterMdnsRecords(records: List<MatterMdnsRecord>): List<String> {
        if (records.isEmpty()) {
            return listOf(MatterMdnsRecord.phoneDiscoveryLimitation)
        }

        return records.flatMap { record ->
            buildList {
                add("${record.serviceType} ${record.instanceName} at ${record.host.ifBlank { "unknown host" }}:${record.port}")
                add("IPv6 addresses: ${record.ipv6Addresses.ifEmpty { listOf("none resolved") }.joinToString()}")
                if (record.txt.isNotEmpty()) {
                    add("TXT keys: ${record.txt.keys.sorted().joinToString()}")
                }
                add(record.interpretationTitle)
            }
        } + MatterMdnsRecord.phoneDiscoveryLimitation
    }

    private fun appendAdvancedDiagnostics(
        checks: List<String> = emptyList(),
        warnings: List<String> = emptyList(),
        details: List<String> = emptyList()
    ) {
        val current = uiState.diagnostics
        uiState = uiState.copy(
            diagnostics = MatterSetupDiagnosticsSummary(
                checks = (current.checks + checks).dedupeNonBlank(),
                warnings = (current.warnings + warnings).dedupeNonBlank(),
                details = (current.details + details).dedupeNonBlank()
            )
        )
    }

    private fun List<String>.dedupeNonBlank(): List<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun emitOpenHabSetupFailure(
        message: String,
        status: OpenHabStatus,
        config: AppConfig,
        baseUrl: String,
        apiToken: String
    ) {
        val sanitizer = SetupFailureSanitizer(config, baseUrl, apiToken)
        val details = listOf(
            "openHAB REST reachable=${status.restReachable()}",
            "openHAB Matter controller ready=${status.matterControllerReady()}",
            status.message().orEmpty(),
            status.details().orEmpty()
        ).sanitizeWith(sanitizer).joinToString("; ")
        val failure = MatterSetupFailure(
            step = MatterSetupStage.OpenHabSetupChecking,
            message = message,
            details = details
        )
        emitState(
            MatterSetupStateReducer.openHabSetupNotReady(
                baseUrl,
                listOf(message, details)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(". "),
                failure,
                MatterSetupDiagnosticsSummary(
                    checks = listOf(
                        "openHAB URL configured=true",
                        "openHAB REST reachable=${status.restReachable()}",
                        "openHAB Matter controller ready=${status.matterControllerReady()}"
                    ),
                    warnings = listOf(status.message().orEmpty()).sanitizeWith(sanitizer),
                    details = listOf(status.details().orEmpty()).sanitizeWith(sanitizer)
                )
            )
        )
    }

    private fun emitOpenHabSetupError(
        error: Exception,
        config: AppConfig,
        baseUrl: String,
        apiToken: String
    ) {
        val sanitizer = SetupFailureSanitizer(config, baseUrl, apiToken)
        val detail = sanitizer.sanitize(error.message.orEmpty()).ifBlank { error.javaClass.simpleName }
        val failure = MatterSetupFailure(
            step = MatterSetupStage.OpenHabSetupChecking,
            message = "openHAB connection check failed",
            details = detail
        )
        emitState(
            MatterSetupStateReducer.openHabSetupNotReady(
                baseUrl,
                listOf("openHAB connection check failed", detail)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(". "),
                failure,
                MatterSetupDiagnosticsSummary(
                    checks = listOf("openHAB URL configured=true"),
                    warnings = listOf("openHAB readiness check failed"),
                    details = listOf(detail)
                )
            )
        )
    }

    private fun emitFirstRunSettingsFailure(
        baseUrl: String,
        config: AppConfig,
        apiToken: String,
        warnings: List<String>,
        details: List<String>
    ) {
        val sanitizer = SetupFailureSanitizer(config, baseUrl, apiToken)
        val safeWarnings = warnings.sanitizeWith(sanitizer)
        val safeDetails = details.sanitizeWith(sanitizer)
        val message = safeWarnings.firstOrNull() ?: "Settings are not ready yet"
        val failure = MatterSetupFailure(
            step = MatterSetupStage.OpenHabSetupChecking,
            message = message,
            details = safeDetails.joinToString("; ")
        )
        emitState(
            MatterSetupStateReducer.openHabSetupNotReady(
                baseUrl,
                safeWarnings.ifEmpty { listOf(message) }.joinToString(". "),
                failure,
                MatterSetupDiagnosticsSummary(
                    checks = safeDetails.filter {
                        it.contains("configured=") || it.contains("reachable=") || it.contains("ready=") ||
                            it.contains("valid=")
                    },
                    warnings = safeWarnings,
                    details = safeDetails
                )
            )
        )
    }

    private fun emitChangedTokenFailure(
        status: OpenHabStatus,
        config: AppConfig,
        baseUrl: String,
        apiToken: String
    ) {
        val sanitizer = SetupFailureSanitizer(config, baseUrl, apiToken)
        val warning = status.message().orEmpty().ifBlank { "openHAB token validation failed" }
        val details = listOf(
            "openHAB REST reachable=${status.restReachable()}",
            "openHAB Matter controller ready=${status.matterControllerReady()}",
            warning,
            status.details().orEmpty()
        ).sanitizeWith(sanitizer)
        val safeWarning = sanitizer.sanitize(warning)
        val failure = MatterSetupFailure(
            step = MatterSetupStage.ChangeToken,
            message = safeWarning,
            details = details.joinToString("; ")
        )
        emitState(
            MatterSetupStateReducer.changeToken().copy(
                message = safeWarning,
                failure = failure,
                diagnostics = MatterSetupDiagnosticsSummary(
                    checks = details.filter { it.contains("reachable=") || it.contains("ready=") },
                    warnings = listOf(safeWarning),
                    details = details
                )
            )
        )
    }

    private fun emitChangedTokenError(
        error: Exception,
        config: AppConfig,
        baseUrl: String,
        apiToken: String
    ) {
        val sanitizer = SetupFailureSanitizer(config, baseUrl, apiToken)
        val detail = sanitizer.sanitize(error.message.orEmpty()).ifBlank { error.javaClass.simpleName }
        val failure = MatterSetupFailure(
            step = MatterSetupStage.ChangeToken,
            message = "openHAB token validation failed",
            details = detail
        )
        emitState(
            MatterSetupStateReducer.changeToken().copy(
                message = "openHAB token validation failed",
                failure = failure,
                diagnostics = MatterSetupDiagnosticsSummary(
                    checks = emptyList(),
                    warnings = listOf("openHAB token validation failed"),
                    details = listOf(detail)
                )
            )
        )
    }

    private fun List<String>.sanitizeWith(sanitizer: SetupFailureSanitizer): List<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(sanitizer::sanitize)
            .distinct()
    }

    private fun scanningState(): MatterSetupUiState {
        return MatterSetupUiState(
            stage = MatterSetupStage.ScanningQr,
            title = "Scan Matter QR code",
            message = "Point the camera at the Matter QR code on the device or box.",
            primaryAction = MatterSetupAction.StartScan,
            primaryActionLabel = "Scan QR code"
        )
    }

    private fun restorePersistedConfig(maskToken: Boolean = false) {
        val config = configRepository.load()
        openHabUrl = config.openHabBaseUrl().trim().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
        token = if (maskToken) "" else config.openHabApiToken()
        openHabTokenStored = config.openHabApiToken().isNotBlank()
        openHabConnectionState = OpenHabConnectionUiState.notChecked(openHabTokenStored)
        threadDataset = config.threadDataset()
        otbrBaseUrl = config.otbrBaseUrl()
        attestationBypassEnabled = config.attestationBypassEnabled()
        openHabConfigured = MatterSetupConfigCompleteness.isComplete(config)
        threadSettingsMessage = ThreadDatasetSettingsValidator.validate(
            threadDataset,
            config.threadDatasetUnreadable()
        ).title
        threadNetworkState = ThreadNetworkUiState.notChecked(threadDataset, otbrBaseUrl)
    }

    private fun phoneDeviceReturnAction(): MatterSetupAction {
        return resolvePhoneDeviceReturnAction(uiState)
    }

    private fun startRealWorkflow(setupPayload: String) {
        if (!executionGate.tryStart()) {
            return
        }
        workerThread = Thread({
            try {
                MatterSetupWorkflow(createRealPorts()) { state ->
                    emitState(state)
                }.startAutomatedSetup(setupPayload)
            } finally {
                executionGate.finish()
            }
        }, "matter-automated-setup")
        workerThread?.start()
    }

    private fun startOpenCommissioningWindowAgain() {
        if (!executionGate.tryStart()) {
            return
        }
        workerThread = Thread({
            var activeStage = MatterSetupStage.OpeningCommissioningWindow
            val existingConfig = runCatching { configRepository.load() }.getOrDefault(AppConfig("", ""))
            val sanitizer = SetupFailureSanitizer(existingConfig, openHabUrl, token)
            val workflowConfig = loadMatterSetupConfig()
            val diagnosticsContext = MatterSetupDiagnosticsContext(
                openHabBaseUrl = workflowConfig.openHabBaseUrl.toLogSafeUrl(),
                otbrBaseUrl = workflowConfig.otbrBaseUrl.toLogSafeUrl(),
                attestationBypassEnabled = workflowConfig.attestationBypassEnabled
            )
            try {
                val bootstrap = bootstrapStateRepository.load()
                if (bootstrap.stateUnreadable()) {
                    throw IllegalStateException(
                        "Stored Matter bootstrap controller state could not be read. Re-run Thread commissioning."
                    )
                }
                if (bootstrap.bootstrapNodeId() < 0L) {
                    throw IllegalStateException("No staged Matter device is stored on this phone.")
                }

                emitState(MatterSetupUiState.progress(activeStage))
                val ports = createRealPorts()
                val window = ports.openCommissioningWindow(bootstrap.bootstrapNodeId(), bootstrap.controllerState())
                val windowIdentity = MatterSetupDeviceIdentity(window.vendorName, window.productName)
                if (window.manualCode.isBlank()) {
                    emitWorkflowFailure(
                        activeStage,
                        "OpenCommissioningWindow did not return a manual setup code",
                        "Blank manual setup code returned by Matter controller",
                        diagnosticsContext,
                        sanitizer
                    )
                    return@Thread
                }

                activeStage = MatterSetupStage.CommissioningWindowOpen
                emitState(MatterSetupUiState.progress(activeStage, window.timeoutSeconds, deviceIdentity = windowIdentity))

                activeStage = MatterSetupStage.SendingCodeToOpenHab
                emitState(MatterSetupUiState.progress(activeStage, window.timeoutSeconds, deviceIdentity = windowIdentity))
                val baselineInbox = ports.readOpenHabInbox(workflowConfig)
                val scan = ports.sendCodeToOpenHab(window.manualCode, workflowConfig)
                if (!scan.started) {
                    emitWorkflowFailure(
                        activeStage,
                        "openHAB could not start pairing",
                        scan.details,
                        diagnosticsContext,
                        sanitizer
                    )
                    return@Thread
                }

                activeStage = MatterSetupStage.WatchingOpenHabInbox
                emitState(MatterSetupUiState.progress(activeStage, scan.timeoutSeconds, deviceIdentity = windowIdentity))
                val inbox = ports.waitForOpenHabInbox(
                    workflowConfig,
                    scan.timeoutSeconds,
                    baselineInbox.matterEntryIds
                )
                if (!inbox.matterEntryDetected) {
                    emitWorkflowFailure(
                        activeStage,
                        "openHAB did not report the device yet",
                        inbox.details,
                        diagnosticsContext,
                        sanitizer
                    )
                    return@Thread
                }

                emitState(
                    MatterSetupUiState(
                        stage = MatterSetupStage.SuccessInboxDetected,
                        title = "Device found by openHAB",
                        message = "openHAB reported a Matter Inbox entry for this device.",
                        primaryAction = MatterSetupAction.AddAnotherDevice,
                        primaryActionLabel = "Add another device"
                    )
                )
            } catch (error: Exception) {
                emitWorkflowFailure(
                    activeStage,
                    "Setup could not open the pairing window again",
                    error.message.orEmpty(),
                    diagnosticsContext,
                    sanitizer
                )
            } finally {
                executionGate.finish()
            }
        }, "matter-open-commissioning-window-again")
        workerThread?.start()
    }

    private fun forgetStagedDeviceFromPhone() {
        val returnAction = uiState.primaryAction ?: MatterSetupAction.BackToSettings
        runCatching { bootstrapStateRepository.clear() }
            .onSuccess {
                refreshPhoneDevices()
                selectedPhoneDevice = null
                uiState = MatterSetupStateReducer.phoneDeviceList(
                    hasDevices = false,
                    message = "Stored Matter staging data was removed from this app. This does not factory reset the device or remove it from other ecosystems.",
                    returnAction = returnAction
                )
            }
            .onFailure { error ->
                uiState = MatterSetupUiState.failed(
                    MatterSetupFailure(
                        step = MatterSetupStage.AdvancedTroubleshooting,
                        message = "Staged device could not be removed from this phone.",
                        details = error.message.orEmpty()
                    ),
                    MatterSetupDiagnosticsSummary.empty()
                )
            }
    }

    private fun emitWorkflowFailure(
        step: MatterSetupStage,
        message: String,
        details: String,
        diagnosticsContext: MatterSetupDiagnosticsContext,
        sanitizer: SetupFailureSanitizer
    ) {
        val failure = MatterSetupFailure(
            step = step,
            message = message,
            details = sanitizer.sanitize(details)
        )
        emitState(MatterSetupUiState.failed(failure, runBasicDiagnostics(failure, diagnosticsContext)))
    }

    private fun emitState(state: MatterSetupUiState) {
        postState {
            uiState = state
        }
    }

    private fun postState(update: () -> Unit) {
        if (!executionGate.canEmit()) {
            return
        }
        mainHandler.post {
            if (executionGate.canEmit()) {
                update()
            }
        }
    }

    private fun createRealPorts(): AndroidMatterSetupPorts {
        return AndroidMatterSetupPorts(
            configLoader = ::loadMatterSetupConfig,
            readinessChecker = { baseUrl, apiToken ->
                openHabClient.checkReadiness(baseUrl, apiToken)
            },
            matterRunner = object : AndroidMatterSetupPorts.MatterRunner {
                override fun commissionToPhone(
                    setupPayload: String,
                    config: MatterSetupConfig,
                    progress: (String) -> Unit
                ): MatterSetupPorts.CommissionResult {
                    val dataset = ThreadDataset.parse(config.threadDataset)
                    val payload = MatterSetupPayloadParser.parse(setupPayload)
                    val selection = controllerSession.selectNativeIfReady()
                    if (!selection.nativeSelected()) {
                        throw IllegalStateException("connectedhomeip is not ready for real commissioning.")
                    }

                    val result = selection.controller().commissionBleThread(
                        dataset,
                        payload,
                        bootstrapStateRepository.load().controllerState(),
                        { step -> progress(step.message()) }
                    )
                    bootstrapStateRepository.save(
                        MatterBootstrapState(
                            result.nodeId(),
                            result.controllerState(),
                            false,
                            result.vendorName(),
                            result.productName()
                        )
                    )
                    return MatterSetupPorts.CommissionResult(
                        result.nodeId(),
                        result.controllerState(),
                        result.vendorName(),
                        result.productName()
                    )
                }

                override fun openCommissioningWindow(
                    nodeId: Long,
                    controllerState: String
                ): MatterSetupPorts.OpenWindowResult {
                    val selection = controllerSession.selectNativeIfReady()
                    if (!selection.nativeSelected()) {
                        throw IllegalStateException("connectedhomeip is not ready for Open Commissioning Window.")
                    }

                    val result = selection.controller().openCommissioningWindow(
                        nodeId,
                        300,
                        3840,
                        controllerState,
                        null
                    )
                    val currentState = bootstrapStateRepository.load()
                    bootstrapStateRepository.save(
                        MatterBootstrapState(
                            nodeId,
                            result.controllerState(),
                            false,
                            currentState.vendorName(),
                            currentState.productName()
                        )
                    )
                    return MatterSetupPorts.OpenWindowResult(
                        manualCode = result.manualCode(),
                        qrCode = result.qrCode(),
                        controllerState = result.controllerState(),
                        timeoutSeconds = 300,
                        vendorName = currentState.vendorName(),
                        productName = currentState.productName()
                    )
                }
            },
            discoveryScan = { baseUrl, manualCode, apiToken ->
                openHabMatterDiscoveryClient.startMatterScan(baseUrl, manualCode, apiToken)
            },
            inboxWaiter = { baseUrl, apiToken, timeoutSeconds ->
                waitForOpenHabInbox(baseUrl, apiToken, timeoutSeconds)
            },
            diagnosticsRunner = ::runBasicDiagnostics,
            readinessDiagnostics = {
                listOf(readinessProbe.bluetoothDiagnostic(), readinessProbe.locationServicesDiagnostic()) +
                    readinessProbe.permissionDiagnostics()
            },
            networkTransportSummary = readinessProbe::networkTransportSummary,
            inboxReader = { baseUrl, apiToken ->
                readOpenHabInbox(baseUrl, apiToken)
            },
            baselineAwareInboxWaiter = { baseUrl, apiToken, timeoutSeconds, baselineMatterEntryIds ->
                waitForOpenHabInbox(baseUrl, apiToken, timeoutSeconds, baselineMatterEntryIds)
            }
        )
    }

    private fun loadMatterSetupConfig(): MatterSetupConfig {
        val config = configRepository.load()
        return MatterSetupConfig(
            openHabBaseUrl = openHabUrl.trim().ifBlank {
                config.openHabBaseUrl().trim().ifBlank { MatterSetupConfigCompleteness.DefaultOpenHabUrl }
            },
            openHabApiToken = resolveEffectiveSetupToken(token, config.openHabApiToken()),
            threadDataset = config.threadDataset(),
            otbrBaseUrl = config.otbrBaseUrl(),
            attestationBypassEnabled = config.attestationBypassEnabled()
        )
    }

    private fun waitForOpenHabInbox(
        baseUrl: String,
        apiToken: String,
        timeoutSeconds: Int,
        baselineMatterEntryIds: Set<String> = emptySet()
    ): MatterSetupPorts.InboxResult {
        val deadlineMillis = System.currentTimeMillis() + (timeoutSeconds.coerceAtLeast(30) * 1000L)
        var last = OpenHabInboxStatus(false, false, "openHAB Inbox was not checked", "")
        var detected = false
        do {
            last = runCatching {
                openHabInboxClient.checkInbox(baseUrl, apiToken)
            }.getOrElse { error ->
                OpenHabInboxStatus(false, false, "openHAB Inbox check failed", error.message.orEmpty())
            }
            detected = matterEntryDetectedAfterBaseline(last, baselineMatterEntryIds)
            if (detected) {
                break
            }
            val remainingMillis = deadlineMillis - System.currentTimeMillis()
            if (remainingMillis <= 0L) {
                break
            }
            try {
                Thread.sleep(remainingMillis.coerceAtMost(2_000L))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        } while (true)

        val details = listOf(last.message().orEmpty(), last.details().orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(": ")
        val baselineDetails = if (!detected && baselineMatterEntryIds.isNotEmpty() && last.matterEntryDetected()) {
            listOf(details, "Only pre-existing Matter Inbox entries were detected.")
                .filter { it.isNotBlank() }
                .joinToString(" ")
        } else {
            details
        }
        return MatterSetupPorts.InboxResult(detected, baselineDetails, last.matterEntryIds())
    }

    private fun readOpenHabInbox(
        baseUrl: String,
        apiToken: String
    ): MatterSetupPorts.InboxResult {
        val status = runCatching {
            openHabInboxClient.checkInbox(baseUrl, apiToken)
        }.getOrElse { error ->
            OpenHabInboxStatus(false, false, "openHAB Inbox check failed", error.message.orEmpty())
        }
        val details = listOf(status.message().orEmpty(), status.details().orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(": ")
        return MatterSetupPorts.InboxResult(status.matterEntryDetected(), details, status.matterEntryIds())
    }

    private fun matterEntryDetectedAfterBaseline(
        status: OpenHabInboxStatus,
        baselineMatterEntryIds: Set<String>
    ): Boolean {
        if (baselineMatterEntryIds.isEmpty()) {
            return status.matterEntryDetected()
        }
        return (status.matterEntryIds() - baselineMatterEntryIds).isNotEmpty()
    }

    private fun runBasicDiagnostics(
        failure: MatterSetupFailure,
        context: MatterSetupDiagnosticsContext
    ): MatterSetupDiagnosticsSummary {
        return MatterSetupDiagnosticsSummary(
            checks = listOf(
                "openHAB URL configured=${context.openHabBaseUrl.isNotBlank()}",
                "attestation bypass enabled=${context.attestationBypassEnabled}"
            ),
            warnings = listOf(failure.message).filter { it.isNotBlank() },
            details = listOf(
                "Failed at ${failure.step}",
                failure.details.ifBlank { "No additional details." }
            )
        )
    }

    private fun newNativeControllerSession(attestationBypassEnabled: Boolean): NativeChipControllerSession {
        val connectedHomeIpFactory = ConnectedHomeIpMatterControllerFactory(appContext)
        return NativeChipControllerSession(
            fakeMatterController,
            attestationBypassEnabled,
            connectedHomeIpFactory::create
        )
    }

    private class SetupFailureSanitizer(
        config: AppConfig,
        openHabBaseUrl: String,
        openHabApiToken: String
    ) {
        private val replacements = linkedMapOf<String, String>()

        init {
            add(openHabBaseUrl, openHabBaseUrl.toLogSafeUrl())
            add(config.openHabBaseUrl(), config.openHabBaseUrl().toLogSafeUrl())
            add(config.otbrBaseUrl(), config.otbrBaseUrl().toLogSafeUrl())
            add(openHabApiToken)
            add(config.openHabApiToken())
            add(config.threadDataset())
            add(config.setupPayload())
        }

        fun sanitize(value: String): String {
            val codeRedacted = value
                .sanitizeLogUrls()
                .replace(MatterQrPayloadRegex, "<redacted>")
                .replace(ManualSetupCodeRegex, "<redacted>")
            return replacements.entries
                .sortedByDescending { it.key.length }
                .fold(codeRedacted) { sanitized, entry ->
                    sanitized.replace(entry.key, entry.value)
                }
        }

        private fun add(value: String, replacement: String = "<redacted>") {
            if (value.isNotBlank()) {
                replacements[value] = replacement
            }
        }
    }

    private companion object {
        val ManualSetupCodeRegex = Regex("""\b\d{11}(?:\d{10})?\b""")
        val MatterQrPayloadRegex = Regex("""MT:\S+""")
    }
}
