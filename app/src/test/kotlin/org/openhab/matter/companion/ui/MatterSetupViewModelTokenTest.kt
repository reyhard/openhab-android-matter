package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openhab.matter.companion.config.AppConfig
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupStateReducer
import org.openhab.matter.companion.setup.PhoneMatterDevice

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
}
