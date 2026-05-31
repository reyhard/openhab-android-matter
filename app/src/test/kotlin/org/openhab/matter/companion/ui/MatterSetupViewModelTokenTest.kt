package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openhab.matter.companion.config.AppConfig
import org.openhab.matter.companion.controller.MatterDeviceDetails
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.PhoneMatterDevice
import org.openhab.matter.companion.setup.PhoneMatterDeviceDetails

class MatterSetupViewModelTokenTest {
    @Test
    fun firstRunSettingsUsePersistedTokenWhenEditableTokenIsBlank() {
        val result = resolveEffectiveSetupToken(
            editableToken = "",
            persistedToken = "persisted.token"
        )

        assertEquals("persisted.token", result)
    }

    @Test
    fun firstRunSettingsPreferEditedTokenWhenPresent() {
        val result = resolveEffectiveSetupToken(
            editableToken = "  edited.token  ",
            persistedToken = "persisted.token"
        )

        assertEquals("edited.token", result)
    }

    @Test
    fun threadSettingsSavePreservesPersistedTokenAndUnreadableState() {
        val existingConfig = AppConfig(
            "hex:old",
            "MT:old",
            "http://openhab:8080",
            "persisted.token",
            "fd00::1",
            false,
            false,
            true,
            false
        )

        val result = threadSettingsConfigForSave(
            existingConfig = existingConfig,
            safeDataset = "hex:0E080000000000010000",
            openHabBaseUrl = "http://openhab:8080",
            otbrBaseUrl = "fd00::2",
            attestationBypassEnabled = true
        )

        assertEquals("persisted.token", result.openHabApiToken())
        assertEquals(true, result.openHabApiTokenUnreadable())
    }

    @Test
    fun openHabConnectionSavePreservesPersistedTokenWhenEditableTokenIsBlank() {
        val existingConfig = AppConfig(
            "hex:old",
            "MT:old",
            "http://old-openhab:8080",
            "persisted.token",
            "fd00::1",
            false,
            false,
            true,
            false
        )

        val result = openHabConnectionConfigForSave(
            existingConfig = existingConfig,
            openHabBaseUrl = "http://new-openhab:8080",
            editableToken = "   "
        )

        assertEquals("http://new-openhab:8080", result.openHabBaseUrl())
        assertEquals("persisted.token", result.openHabApiToken())
        assertEquals(true, result.openHabApiTokenUnreadable())
    }

    @Test
    fun openHabConnectionSaveStoresEditedTokenAndClearsUnreadableState() {
        val existingConfig = AppConfig(
            "hex:old",
            "MT:old",
            "http://old-openhab:8080",
            "persisted.token",
            "fd00::1",
            false,
            false,
            true,
            false
        )

        val result = openHabConnectionConfigForSave(
            existingConfig = existingConfig,
            openHabBaseUrl = "http://new-openhab:8080",
            editableToken = "  edited.token  "
        )

        assertEquals("http://new-openhab:8080", result.openHabBaseUrl())
        assertEquals("edited.token", result.openHabApiToken())
        assertEquals(false, result.openHabApiTokenUnreadable())
    }

    @Test
    fun phoneDeviceDetailsReturnActionUsesStoredDetailsContext() {
        val state = MatterSetupStateReducer.phoneDeviceDetails(
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true
            ),
            returnAction = MatterSetupAction.BackToRequiredSetup
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, resolvePhoneDeviceReturnAction(state))
    }

    @Test
    fun phoneDeviceDetailsNavigationPreservesListReturnAction() {
        val state = MatterSetupStateReducer.phoneDeviceList(
            hasDevices = true,
            returnAction = MatterSetupAction.BackToRequiredSetup
        )

        assertEquals(MatterSetupAction.BackToRequiredSetup, resolvePhoneDeviceReturnAction(state))
    }

    @Test
    fun detailsNavigationSelectsMatchingNodeId() {
        val selected = resolvePhoneDeviceDetailsSelection(
            nodeId = 0x2,
            phoneDevices = listOf(
                PhoneMatterDevice(0x1, controllerStateStored = true, stateReadable = true),
                PhoneMatterDevice(0x2, controllerStateStored = true, stateReadable = true)
            )
        )

        assertEquals(0x2L, selected?.nodeId)
    }

    @Test
    fun detailsNavigationFallsBackOnlyWhenExactlyOneDeviceIsStaged() {
        val onlyDevice = PhoneMatterDevice(0x4D2, controllerStateStored = true, stateReadable = true)

        assertEquals(onlyDevice, resolvePhoneDeviceDetailsSelection(null, listOf(onlyDevice)))
        assertNull(
            resolvePhoneDeviceDetailsSelection(
                nodeId = null,
                phoneDevices = listOf(
                    onlyDevice,
                    PhoneMatterDevice(0x162E, controllerStateStored = true, stateReadable = true)
                )
            )
        )
    }

    @Test
    fun controllerDetailsMapToPhoneDeviceDetailsWithStagedFallbacks() {
        val update = phoneDeviceDetailsFromControllerDetails(
            details = MatterDeviceDetails.Builder()
                .vendorName("")
                .productName("Fetched product")
                .softwareVersionString("1.8.7")
                .hardwareVersionString("P2.0")
                .partNumber("E2201")
                .batteryPercentRemaining(104)
                .batteryQuantity(2)
                .batteryDesignation("AAA")
                .threadNetworkName("OpenThread")
                .threadChannel(25)
                .ipv6Address("fd00::1234")
                .otaUpdatePossible(true)
                .build(),
            device = PhoneMatterDevice(
                nodeId = 0x165BC267A7E344D0L,
                controllerStateStored = true,
                stateReadable = true,
                vendorName = "Staged vendor",
                productName = "Staged product"
            )
        )

        assertEquals("Fetched product", update.deviceName)
        assertEquals("Staged vendor", update.vendor)
        assertEquals("Fetched product", update.product)
        assertEquals("1.8.7", update.firmwareVersion)
        assertEquals("P2.0", update.hardwareVersion)
        assertEquals("E2201", update.partNumber)
        assertEquals("0x165BC267A7E344D0", update.nodeId)
        assertEquals("52% · 2×AAA", update.battery)
        assertEquals("OpenThread · Channel 25", update.threadNetwork)
        assertEquals("fd00::1234", update.ipv6Address)
        assertEquals("Possible", update.otaUpdate)
    }

    @Test
    fun controllerDetailsLeaveAbsentUpdateValuesBlankForMerge() {
        val update = phoneDeviceDetailsFromControllerDetails(
            details = MatterDeviceDetails.empty(),
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true,
                vendorName = "Staged vendor",
                productName = "Staged product"
            )
        )

        assertEquals("Staged product", update.deviceName)
        assertEquals("Staged vendor", update.vendor)
        assertEquals("Staged product", update.product)
        assertEquals("0x4D2", update.nodeId)
        assertEquals("", update.firmwareVersion)
        assertEquals("", update.hardwareVersion)
        assertEquals("", update.partNumber)
        assertEquals("", update.battery)
        assertEquals("", update.threadNetwork)
        assertEquals("", update.ipv6Address)
        assertEquals("", update.otaUpdate)
    }

    @Test
    fun controllerDetailsDoNotWriteUnknownIdentityWhenFetchedAndStagedValuesAreAbsent() {
        val update = phoneDeviceDetailsFromControllerDetails(
            details = MatterDeviceDetails.empty(),
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true
            )
        )

        assertEquals("", update.deviceName)
        assertEquals("", update.vendor)
        assertEquals("", update.product)
        assertEquals("0x4D2", update.nodeId)
    }

    @Test
    fun successfulFetchMergesDetailsAndClearsFetching() {
        val state = MatterSetupStateReducer.phoneDeviceDetails(
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true,
                vendorName = "Staged vendor",
                productName = "Staged product"
            ),
            fetching = true
        )

        val refreshed = phoneDeviceDetailsFetchSucceeded(
            state,
            PhoneMatterDeviceDetails(firmwareVersion = "1.8.7")
        )

        assertEquals("Staged vendor", refreshed.phoneDeviceDetails.vendor)
        assertEquals("1.8.7", refreshed.phoneDeviceDetails.firmwareVersion)
        assertEquals(false, refreshed.phoneDeviceDetailsFetching)
        assertEquals("Device data refreshed", refreshed.phoneDeviceDetailsMessage)
    }

    @Test
    fun failedFetchKeepsDetailsAndClearsFetching() {
        val state = MatterSetupStateReducer.phoneDeviceDetails(
            device = PhoneMatterDevice(
                nodeId = 0x4D2,
                controllerStateStored = true,
                stateReadable = true,
                vendorName = "Staged vendor",
                productName = "Staged product"
            ),
            fetching = true
        ).copy(phoneDeviceDetails = PhoneMatterDeviceDetails(firmwareVersion = "1.8.7"))

        val failed = phoneDeviceDetailsFetchFailed(state)

        assertEquals("1.8.7", failed.phoneDeviceDetails.firmwareVersion)
        assertEquals(false, failed.phoneDeviceDetailsFetching)
        assertEquals("Could not fetch data from device", failed.phoneDeviceDetailsMessage)
    }
}
