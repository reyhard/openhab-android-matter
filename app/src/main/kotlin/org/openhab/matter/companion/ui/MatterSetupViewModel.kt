package org.openhab.matter.companion.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.openhab.matter.companion.config.AppConfig
import org.openhab.matter.companion.config.SharedPreferencesAppConfigRepository
import org.openhab.matter.companion.controller.ConnectedHomeIpMatterControllerFactory
import org.openhab.matter.companion.controller.FakeMatterController
import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.NativeChipControllerSession
import org.openhab.matter.companion.controller.SharedPreferencesMatterBootstrapStateRepository
import org.openhab.matter.companion.diagnostics.AndroidReadinessProbe
import org.openhab.matter.companion.diagnostics.AndroidThreadBorderRouterBrowser
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.domain.MatterSetupPayloadParser
import org.openhab.matter.companion.domain.ThreadDataset
import org.openhab.matter.companion.openhab.HttpOpenHabClient
import org.openhab.matter.companion.openhab.HttpOpenHabInboxClient
import org.openhab.matter.companion.openhab.HttpOpenHabMatterDiscoveryClient
import org.openhab.matter.companion.openhab.OpenHabInboxStatus
import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.setup.AndroidMatterSetupPorts
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupConfig
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsContext
import org.openhab.matter.companion.setup.MatterSetupDiagnosticsSummary
import org.openhab.matter.companion.setup.MatterSetupFailure
import org.openhab.matter.companion.setup.MatterSetupPorts
import org.openhab.matter.companion.setup.MatterSetupStage
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.MatterSetupWorkflow
import org.openhab.matter.companion.setup.PhoneMatterDevice
import org.openhab.matter.companion.setup.ThreadDatasetSettingsStatus
import org.openhab.matter.companion.setup.ThreadDatasetSettingsValidator
import org.openhab.matter.companion.setup.WorkflowExecutionGate
import org.openhab.matter.companion.setup.sanitizeLogUrls
import org.openhab.matter.companion.setup.toLogSafeUrl

class MatterSetupViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(MatterSetupUiState.initial(openHabConfigured = false))
        private set
    var openHabUrl by mutableStateOf("")
        private set
    var token by mutableStateOf("")
        private set
    var threadDataset by mutableStateOf("")
        private set
    var otbrBaseUrl by mutableStateOf("")
        private set
    var attestationBypassEnabled by mutableStateOf(false)
        private set
    var threadSettingsMessage by mutableStateOf("")
        private set
    var threadBorderRouters by mutableStateOf<List<ThreadBorderRouterRecord>>(emptyList())
        private set
    var threadBorderRouterDiscoveryInProgress by mutableStateOf(false)
        private set
    var phoneDevices by mutableStateOf<List<PhoneMatterDevice>>(emptyList())
        private set

    private var scannedPayload = ""
    private val appContext = application.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executionGate = WorkflowExecutionGate()
    private var workerThread: Thread? = null

    private val configRepository by lazy { SharedPreferencesAppConfigRepository(appContext) }
    private val bootstrapStateRepository by lazy { SharedPreferencesMatterBootstrapStateRepository(appContext) }
    private val openHabClient by lazy { HttpOpenHabClient() }
    private val openHabMatterDiscoveryClient by lazy { HttpOpenHabMatterDiscoveryClient() }
    private val openHabInboxClient by lazy { HttpOpenHabInboxClient() }
    private val fakeMatterController by lazy { FakeMatterController() }
    private val readinessProbe by lazy { AndroidReadinessProbe(appContext) }
    private val threadBorderRouterBrowser by lazy { AndroidThreadBorderRouterBrowser(appContext) }
    private var openHabConfigured = false
    private val controllerSession by lazy {
        newNativeControllerSession(loadMatterSetupConfig().attestationBypassEnabled)
    }

    init {
        val config = configRepository.load()
        openHabUrl = config.openHabBaseUrl()
        token = config.openHabApiToken()
        threadDataset = config.threadDataset()
        otbrBaseUrl = config.otbrBaseUrl()
        attestationBypassEnabled = config.attestationBypassEnabled()
        threadSettingsMessage = ThreadDatasetSettingsValidator.validate(
            threadDataset,
            config.threadDatasetUnreadable()
        ).title
        openHabConfigured = config.openHabBaseUrl().isNotBlank()
        uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
    }

    fun onOpenHabUrlChange(value: String) {
        openHabUrl = value
        if (uiState.stage == MatterSetupStage.NeedsOpenHabSetup) {
            val failure = uiState.failure
            uiState = if (failure == null) {
                MatterSetupStateReducer.openHabSetup(value, showBackToMainMenu = openHabConfigured)
            } else {
                MatterSetupStateReducer.openHabSetupNotReady(
                    value,
                    uiState.message,
                    failure,
                    uiState.diagnostics,
                    showBackToMainMenu = openHabConfigured
                )
            }
        }
    }

    fun onTokenChange(value: String) {
        token = value
    }

    fun onThreadDatasetChange(value: String) {
        threadDataset = value
    }

    fun onOtbrBaseUrlChange(value: String) {
        otbrBaseUrl = value
    }

    fun onAttestationBypassChange(value: Boolean) {
        attestationBypassEnabled = value
    }

    fun handleAction(action: MatterSetupAction) {
        when (action) {
            MatterSetupAction.StartScan -> {
                uiState = scanningState()
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
                uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
            }

            MatterSetupAction.BackToMainMenu -> {
                scannedPayload = ""
                restorePersistedConfig()
                uiState = MatterSetupStateReducer.reset(openHabConfigured, openHabUrl)
            }

            MatterSetupAction.BackToSettings -> {
                uiState = MatterSetupStateReducer.editSettings(openHabUrl)
            }

            MatterSetupAction.SaveOpenHab,
            MatterSetupAction.TestOpenHab -> {
                startOpenHabSetupCheck()
            }

            MatterSetupAction.EditSettings -> {
                uiState = MatterSetupStateReducer.editSettings(openHabUrl)
            }

            MatterSetupAction.ShowPhoneDevices -> {
                refreshPhoneDevices()
                uiState = MatterSetupStateReducer.phoneDeviceList(phoneDevices.isNotEmpty())
            }

            MatterSetupAction.CheckThreadDataset -> {
                updateThreadDatasetValidationMessage()
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
            }

            MatterSetupAction.ShowTroubleshooting,
            MatterSetupAction.EnterCodeManually -> {
                uiState = MatterSetupStateReducer.advancedTroubleshooting(uiState)
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

    private fun startOpenHabSetupCheck() {
        val baseUrl = openHabUrl.trim()
        val apiToken = token
        if (baseUrl.isBlank()) {
            uiState = MatterSetupStateReducer.openHabSetup(openHabUrl)
            return
        }
        if (!executionGate.tryStart()) {
            return
        }

        uiState = MatterSetupStateReducer.openHabSetupChecking()
        workerThread = Thread({
            try {
                val existingConfig = configRepository.load()
                val status = openHabClient.checkReadiness(baseUrl, apiToken)
                if (status.online() && status.restReachable() && status.matterControllerReady()) {
                    saveOpenHabConfig(existingConfig, baseUrl, apiToken)
                    postState {
                        openHabUrl = baseUrl
                        token = apiToken
                        openHabConfigured = true
                        uiState = MatterSetupStateReducer.openHabSetupReady(baseUrl)
                    }
                } else {
                    emitOpenHabSetupFailure(
                        "openHAB is not ready yet",
                        status,
                        existingConfig,
                        baseUrl,
                        apiToken
                    )
                }
            } catch (error: Exception) {
                val existingConfig = runCatching { configRepository.load() }.getOrDefault(AppConfig("", ""))
                emitOpenHabSetupError(error, existingConfig, baseUrl, apiToken)
            } finally {
                executionGate.finish()
            }
        }, "openhab-setup-check")
        workerThread?.start()
    }

    private fun saveOpenHabConfig(existingConfig: AppConfig, baseUrl: String, apiToken: String) {
        configRepository.save(
            AppConfig(
                existingConfig.threadDataset(),
                existingConfig.setupPayload(),
                baseUrl,
                apiToken,
                existingConfig.otbrBaseUrl(),
                existingConfig.threadDatasetUnreadable(),
                existingConfig.setupPayloadUnreadable(),
                existingConfig.openHabApiTokenUnreadable(),
                existingConfig.attestationBypassEnabled()
            )
        )
    }

    private fun updateThreadDatasetValidationMessage(): Boolean {
        val validation = ThreadDatasetSettingsValidator.validate(threadDataset)
        threadSettingsMessage = "${validation.title} ${validation.message}"
        return validation.status == ThreadDatasetSettingsStatus.Valid
    }

    private fun saveThreadSettings() {
        val parsedDataset = runCatching { ThreadDataset.parse(threadDataset) }.getOrElse {
            updateThreadDatasetValidationMessage()
            return
        }
        val existingConfig = configRepository.load()
        val safeDataset = parsedDataset.chipToolValue()
        configRepository.save(
            AppConfig(
                safeDataset,
                existingConfig.setupPayload(),
                openHabUrl,
                token,
                otbrBaseUrl.trim(),
                false,
                existingConfig.setupPayloadUnreadable(),
                existingConfig.openHabApiTokenUnreadable(),
                attestationBypassEnabled
            )
        )
        threadDataset = safeDataset
        controllerSession.syncAttestationBypass(attestationBypassEnabled)
        threadSettingsMessage = "Saved Thread settings. Dataset value is stored encrypted and not shown in logs."
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
                ),
                showBackToMainMenu = openHabConfigured
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
                ),
                showBackToMainMenu = openHabConfigured
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

    private fun restorePersistedConfig() {
        val config = configRepository.load()
        openHabUrl = config.openHabBaseUrl()
        token = config.openHabApiToken()
        threadDataset = config.threadDataset()
        otbrBaseUrl = config.otbrBaseUrl()
        attestationBypassEnabled = config.attestationBypassEnabled()
        openHabConfigured = config.openHabBaseUrl().isNotBlank()
        threadSettingsMessage = ThreadDatasetSettingsValidator.validate(
            threadDataset,
            config.threadDatasetUnreadable()
        ).title
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
                if (bootstrap.bootstrapNodeId() < 0L || bootstrap.controllerState().isBlank()) {
                    throw IllegalStateException("No staged Matter device is stored on this phone.")
                }

                emitState(MatterSetupUiState.progress(activeStage))
                val ports = createRealPorts()
                val window = ports.openCommissioningWindow(bootstrap.bootstrapNodeId(), bootstrap.controllerState())
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
                emitState(MatterSetupUiState.progress(activeStage, window.timeoutSeconds))

                activeStage = MatterSetupStage.SendingCodeToOpenHab
                emitState(MatterSetupUiState.progress(activeStage, window.timeoutSeconds))
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
                emitState(MatterSetupUiState.progress(activeStage, scan.timeoutSeconds))
                val inbox = ports.waitForOpenHabInbox(workflowConfig, scan.timeoutSeconds)
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
        runCatching { bootstrapStateRepository.clear() }
            .onSuccess {
                refreshPhoneDevices()
                uiState = MatterSetupStateReducer.phoneDeviceList(
                    hasDevices = false,
                    message = "Stored Matter staging data was removed from this app. This does not factory reset the device or remove it from other ecosystems."
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
                    bootstrapStateRepository.save(MatterBootstrapState(result.nodeId(), result.controllerState(), false))
                    return MatterSetupPorts.CommissionResult(result.nodeId(), result.controllerState())
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
                    bootstrapStateRepository.save(MatterBootstrapState(nodeId, result.controllerState(), false))
                    return MatterSetupPorts.OpenWindowResult(
                        manualCode = result.manualCode(),
                        qrCode = result.qrCode(),
                        controllerState = result.controllerState(),
                        timeoutSeconds = 300
                    )
                }
            },
            discoveryScan = { baseUrl, manualCode, apiToken ->
                openHabMatterDiscoveryClient.startMatterScan(baseUrl, manualCode, apiToken)
            },
            inboxWaiter = ::waitForOpenHabInbox,
            diagnosticsRunner = ::runBasicDiagnostics,
            readinessDiagnostics = {
                listOf(readinessProbe.bluetoothDiagnostic(), readinessProbe.locationServicesDiagnostic()) +
                    readinessProbe.permissionDiagnostics()
            },
            networkTransportSummary = readinessProbe::networkTransportSummary
        )
    }

    private fun loadMatterSetupConfig(): MatterSetupConfig {
        val config = configRepository.load()
        return MatterSetupConfig(
            openHabBaseUrl = openHabUrl,
            openHabApiToken = token,
            threadDataset = config.threadDataset(),
            otbrBaseUrl = config.otbrBaseUrl(),
            attestationBypassEnabled = config.attestationBypassEnabled()
        )
    }

    private fun waitForOpenHabInbox(
        baseUrl: String,
        apiToken: String,
        timeoutSeconds: Int
    ): MatterSetupPorts.InboxResult {
        val deadlineMillis = System.currentTimeMillis() + (timeoutSeconds.coerceAtLeast(30) * 1000L)
        var last = OpenHabInboxStatus(false, false, "openHAB Inbox was not checked", "")
        do {
            last = runCatching {
                openHabInboxClient.checkInbox(baseUrl, apiToken)
            }.getOrElse { error ->
                OpenHabInboxStatus(false, false, "openHAB Inbox check failed", error.message.orEmpty())
            }
            if (last.matterEntryDetected()) {
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
        return MatterSetupPorts.InboxResult(last.matterEntryDetected(), details)
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
