package org.openhab.matter.companion.setup

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadDatasetSettingsValidatorTest {
    @Test
    fun missingDatasetAsksUserToPasteActiveOperationalDataset() {
        val result = ThreadDatasetSettingsValidator.validate("")

        assertEquals(ThreadDatasetSettingsStatus.Missing, result.status)
        assertEquals("Thread dataset is missing.", result.title)
    }

    @Test
    fun invalidDatasetDoesNotExposeInput() {
        val result = ThreadDatasetSettingsValidator.validate("not-a-dataset")

        assertEquals(ThreadDatasetSettingsStatus.Invalid, result.status)
        assertEquals("Thread dataset is not valid.", result.title)
        assertEquals(false, result.message.contains("not-a-dataset"))
    }

    @Test
    fun validDatasetAcceptsHexPrefix() {
        val result = ThreadDatasetSettingsValidator.validate("hex:0E080000000000010000")

        assertEquals(ThreadDatasetSettingsStatus.Valid, result.status)
        assertEquals("Thread dataset looks valid.", result.title)
    }

    @Test
    fun unreadableStoredDatasetRequiresManualEntry() {
        val result = ThreadDatasetSettingsValidator.validate(
            "hex:0E080000000000010000",
            storedDatasetUnreadable = true
        )

        assertEquals(ThreadDatasetSettingsStatus.Unreadable, result.status)
        assertEquals("Stored Thread dataset cannot be read.", result.title)
    }
}
