package org.openhab.matter.companion.setup

import org.openhab.matter.companion.domain.ThreadDataset

enum class ThreadDatasetSettingsStatus {
    Missing,
    Invalid,
    Valid,
    Unreadable
}

data class ThreadDatasetSettingsValidation(
    val status: ThreadDatasetSettingsStatus,
    val title: String,
    val message: String
)

object ThreadDatasetSettingsValidator {
    fun validate(
        datasetInput: String,
        storedDatasetUnreadable: Boolean = false
    ): ThreadDatasetSettingsValidation {
        if (storedDatasetUnreadable) {
            return ThreadDatasetSettingsValidation(
                status = ThreadDatasetSettingsStatus.Unreadable,
                title = "Stored Thread dataset cannot be read.",
                message = "Paste and save the Active Operational Dataset again."
            )
        }
        if (datasetInput.isBlank()) {
            return ThreadDatasetSettingsValidation(
                status = ThreadDatasetSettingsStatus.Missing,
                title = "Thread dataset is missing.",
                message = "Paste the Active Operational Dataset from your Thread Border Router before commissioning."
            )
        }
        return runCatching {
            ThreadDataset.parse(datasetInput)
        }.fold(
            onSuccess = {
                ThreadDatasetSettingsValidation(
                    status = ThreadDatasetSettingsStatus.Valid,
                    title = "Thread dataset looks valid.",
                    message = "The dataset format is valid and can be used for Thread commissioning."
                )
            },
            onFailure = {
                ThreadDatasetSettingsValidation(
                    status = ThreadDatasetSettingsStatus.Invalid,
                    title = "Thread dataset is not valid.",
                    message = "Use the Active Operational Dataset as raw hex or with a hex: prefix."
                )
            }
        )
    }
}
