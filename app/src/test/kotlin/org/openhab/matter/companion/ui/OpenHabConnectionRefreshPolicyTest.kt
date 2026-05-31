package org.openhab.matter.companion.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openhab.matter.companion.setup.MatterSetupAction

class OpenHabConnectionRefreshPolicyTest {
    @Test
    fun refreshesWhenReturningToAddMatterDeviceFromSettings() {
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.BackToMainMenu))
    }

    @Test
    fun refreshesWhenStartingAnotherDevice() {
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.AddAnotherDevice))
    }

    @Test
    fun refreshesWhenOpeningSettings() {
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.EditSettings))
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.BackToSettings))
    }

    @Test
    fun refreshesAfterSavingOpenHabConnectionSettings() {
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.SaveOpenHabAddress))
        assertTrue(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.SaveChangedToken))
    }

    @Test
    fun doesNotRefreshForUnrelatedActions() {
        assertFalse(shouldRefreshOpenHabConnectionAfterAction(MatterSetupAction.StartScan))
    }
}
