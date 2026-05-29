package org.openhab.matter.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
