package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openhab.matter.companion.config.AppConfig

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
}
