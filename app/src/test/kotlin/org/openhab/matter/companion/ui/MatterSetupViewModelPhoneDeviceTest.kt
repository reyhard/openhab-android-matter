package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.openhab.matter.companion.controller.ChipMatterControllerStatus
import org.openhab.matter.companion.controller.MatterBootstrapState
import org.openhab.matter.companion.controller.MatterBootstrapStateRepository
import org.openhab.matter.companion.controller.MatterCommissioningResult
import org.openhab.matter.companion.controller.MatterController
import org.openhab.matter.companion.controller.MatterControllerCandidate
import org.openhab.matter.companion.controller.MatterDeviceDetails
import org.openhab.matter.companion.controller.MatterOpenCommissioningWindowResult
import org.openhab.matter.companion.controller.NativeChipControllerSession
import org.openhab.matter.companion.domain.CommissioningStep
import org.openhab.matter.companion.domain.MatterSetupPayload
import org.openhab.matter.companion.domain.ThreadDataset
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStage
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class MatterSetupViewModelPhoneDeviceTest {
    @Test
    fun showPhoneDevicesThenShowPhoneDeviceDetailsSelectsDetailsFromStagedDevice() {
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "controller-state",
                false,
                "Staged vendor",
                "Staged product"
            )
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))

        assertEquals(MatterSetupStage.PhoneDeviceDetails, viewModel.uiState.stage)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.deviceName)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.product)
        assertEquals("0x4D2", viewModel.uiState.phoneDeviceDetails.nodeId)
    }

    @Test
    fun fetchPhoneDeviceDetailsFailsClosedWhenControllerStateIsMissing() {
        val nativeController = RecordingNativeController(
            details = MatterDeviceDetails.Builder()
                .softwareVersionString("1.8.7")
                .build()
        )
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "",
                false,
                "Staged vendor",
                "Staged product"
            ),
            nativeController = nativeController
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)
        drainMainThread()

        assertEquals(MatterSetupStage.PhoneDeviceDetails, viewModel.uiState.stage)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.deviceName)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("", viewModel.uiState.phoneDeviceDetails.firmwareVersion)
        assertFalse(viewModel.uiState.phoneDeviceDetailsFetching)
        assertEquals("Could not fetch data from device", viewModel.uiState.phoneDeviceDetailsMessage)
        assertEquals(0, nativeController.readDeviceDetailsCalls)
    }

    @Test
    fun acknowledgePhoneDeviceDetailsMessageClearsMessageWithoutDroppingDetails() {
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "",
                false,
                "Staged vendor",
                "Staged product"
            )
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)
        drainMainThread()
        viewModel.handleAction(MatterSetupAction.AcknowledgePhoneDeviceDetailsMessage)

        assertEquals(MatterSetupStage.PhoneDeviceDetails, viewModel.uiState.stage)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.deviceName)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("0x4D2", viewModel.uiState.phoneDeviceDetails.nodeId)
        assertEquals("", viewModel.uiState.phoneDeviceDetailsMessage)
    }

    @Test
    fun fetchPhoneDeviceDetailsFailsClosedWhenBootstrapNodeChangedBeforeFetch() {
        val nativeController = RecordingNativeController(
            details = MatterDeviceDetails.Builder()
                .softwareVersionString("1.8.7")
                .build()
        )
        val bootstrapStateRepository = FakeBootstrapStateRepository(
            MatterBootstrapState(
                0x4D2,
                "controller-state",
                false,
                "Staged vendor",
                "Staged product"
            )
        )
        val viewModel = viewModelWith(
            bootstrapStateRepository = bootstrapStateRepository,
            nativeController = nativeController
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        bootstrapStateRepository.save(
            MatterBootstrapState(
                0x162E,
                "new-controller-state",
                false,
                "Other vendor",
                "Other product"
            )
        )
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)
        drainMainThread()

        assertEquals(MatterSetupStage.PhoneDeviceDetails, viewModel.uiState.stage)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.deviceName)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("", viewModel.uiState.phoneDeviceDetails.firmwareVersion)
        assertFalse(viewModel.uiState.phoneDeviceDetailsFetching)
        assertEquals("Could not fetch data from device", viewModel.uiState.phoneDeviceDetailsMessage)
        assertEquals(0, nativeController.readDeviceDetailsCalls)
    }

    @Test
    fun fetchPhoneDeviceDetailsMergesNativeDetailsWhenControllerStateIsStoredAndNativeIsReady() {
        val nativeController = RecordingNativeController(
            details = MatterDeviceDetails.Builder()
                .softwareVersionString("1.8.7")
                .hardwareVersionString("P2.0")
                .threadNetworkName("OpenThread")
                .threadChannel(25)
                .build()
        )
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "controller-state",
                false,
                "Staged vendor",
                "Staged product"
            ),
            nativeController = nativeController
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)

        waitForFetchToFinish(viewModel)

        assertEquals(1, nativeController.readDeviceDetailsCalls)
        assertEquals(0x4D2L, nativeController.lastNodeId)
        assertEquals("controller-state", nativeController.lastControllerState)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.product)
        assertEquals("1.8.7", viewModel.uiState.phoneDeviceDetails.firmwareVersion)
        assertEquals("P2.0", viewModel.uiState.phoneDeviceDetails.hardwareVersion)
        assertEquals("OpenThread · Channel 25", viewModel.uiState.phoneDeviceDetails.threadNetwork)
        assertEquals("Device data refreshed", viewModel.uiState.phoneDeviceDetailsMessage)
    }

    @Test
    fun fetchPhoneDeviceDetailsReportsFailureWhenNativeDetailsAreEmpty() {
        val nativeController = RecordingNativeController(details = MatterDeviceDetails.empty())
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "controller-state",
                false,
                "Staged vendor",
                "Staged product"
            ),
            nativeController = nativeController
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)

        waitForFetchToFinish(viewModel)

        assertEquals(1, nativeController.readDeviceDetailsCalls)
        assertEquals("Staged vendor", viewModel.uiState.phoneDeviceDetails.vendor)
        assertEquals("Staged product", viewModel.uiState.phoneDeviceDetails.product)
        assertEquals("", viewModel.uiState.phoneDeviceDetails.firmwareVersion)
        assertEquals("Could not fetch data from device", viewModel.uiState.phoneDeviceDetailsMessage)
    }

    @Test
    fun fetchPhoneDeviceDetailsDropsCompletionAfterLeavingDetailsStage() {
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val nativeController = RecordingNativeController(
            details = MatterDeviceDetails.Builder()
                .softwareVersionString("1.8.7")
                .build(),
            beforeReadReturns = {
                readStarted.countDown()
                assertTrue(
                    "Timed out waiting to release device details read",
                    releaseRead.await(5, TimeUnit.SECONDS)
                )
            }
        )
        val viewModel = viewModelWith(
            bootstrapState = MatterBootstrapState(
                0x4D2,
                "controller-state",
                false,
                "Staged vendor",
                "Staged product"
            ),
            nativeController = nativeController
        )

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        viewModel.handleAction(MatterSetupAction.ShowPhoneDeviceDetails(0x4D2))
        viewModel.handleAction(MatterSetupAction.FetchPhoneDeviceDetails)
        assertTrue("Timed out waiting for device details read", readStarted.await(5, TimeUnit.SECONDS))

        viewModel.handleAction(MatterSetupAction.ShowPhoneDevices)
        releaseRead.countDown()
        waitForFetchToFinish(viewModel)

        assertEquals(MatterSetupStage.PhoneDeviceList, viewModel.uiState.stage)
        assertEquals("", viewModel.uiState.phoneDeviceDetails.firmwareVersion)
        assertEquals("", viewModel.uiState.phoneDeviceDetailsMessage)
    }

    private fun viewModelWith(
        bootstrapState: MatterBootstrapState,
        nativeController: RecordingNativeController = RecordingNativeController()
    ): MatterSetupViewModel {
        return viewModelWith(FakeBootstrapStateRepository(bootstrapState), nativeController)
    }

    private fun viewModelWith(
        bootstrapStateRepository: MatterBootstrapStateRepository,
        nativeController: RecordingNativeController = RecordingNativeController()
    ): MatterSetupViewModel {
        return MatterSetupViewModel(
            application = RuntimeEnvironment.getApplication(),
            bootstrapStateRepositoryOverride = bootstrapStateRepository,
            controllerSessionOverride = NativeChipControllerSession(
                ThrowingMatterController,
                false
            ) { nativeController },
            initialize = false
        )
    }

    private fun waitForFetchToFinish(viewModel: MatterSetupViewModel) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        do {
            drainMainThread()
            if (!viewModel.uiState.phoneDeviceDetailsFetching &&
                viewModel.uiState.phoneDeviceDetailsMessage.isNotBlank()
            ) {
                return
            }
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
        drainMainThread()
        assertFalse("Device details fetch did not finish", viewModel.uiState.phoneDeviceDetailsFetching)
    }

    private fun drainMainThread() {
        shadowOf(RuntimeEnvironment.getApplication().mainLooper).idle()
    }

    private class FakeBootstrapStateRepository(
        private var state: MatterBootstrapState
    ) : MatterBootstrapStateRepository {
        override fun load(): MatterBootstrapState = state

        override fun save(state: MatterBootstrapState) {
            this.state = state
        }

        override fun clear() {
            state = MatterBootstrapState.empty()
        }
    }

    private class RecordingNativeController(
        private val details: MatterDeviceDetails = MatterDeviceDetails.empty(),
        private val ready: Boolean = true,
        private val beforeReadReturns: () -> Unit = {}
    ) : MatterControllerCandidate {
        var readDeviceDetailsCalls = 0
            private set
        var lastNodeId: Long? = null
            private set
        var lastControllerState: String? = null
            private set

        override fun readiness(): ChipMatterControllerStatus {
            return ChipMatterControllerStatus(ready, "test-native", false, "test")
        }

        override fun commissionBleThread(
            dataset: ThreadDataset,
            payload: MatterSetupPayload,
            controllerState: String,
            listener: MatterController.ProgressListener
        ): MatterCommissioningResult {
            throw AssertionError("commissionBleThread should not be called")
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            timeoutSeconds: Int,
            discriminator: Int,
            controllerState: String,
            listener: MatterController.ProgressListener
        ): MatterOpenCommissioningWindowResult {
            throw AssertionError("openCommissioningWindow should not be called")
        }

        override fun readDeviceDetails(
            nodeId: Long,
            controllerState: String,
            listener: MatterController.ProgressListener
        ): MatterDeviceDetails {
            readDeviceDetailsCalls += 1
            lastNodeId = nodeId
            lastControllerState = controllerState
            listener.onProgress(CommissioningStep("Reading device details", false))
            beforeReadReturns()
            return details
        }
    }

    private object ThrowingMatterController : MatterController {
        override fun commissionBleThread(
            dataset: ThreadDataset,
            payload: MatterSetupPayload,
            controllerState: String,
            listener: MatterController.ProgressListener
        ): MatterCommissioningResult {
            throw AssertionError("Fallback controller should not be called")
        }

        override fun openCommissioningWindow(
            nodeId: Long,
            timeoutSeconds: Int,
            discriminator: Int,
            controllerState: String,
            listener: MatterController.ProgressListener
        ): MatterOpenCommissioningWindowResult {
            throw AssertionError("Fallback controller should not be called")
        }
    }
}
