package org.openhab.matter.companion.setup

import org.openhab.matter.companion.config.AppConfig

object MatterSetupConfigCompleteness {
    const val DefaultOpenHabUrl = "http://openhab:8080"

    fun isComplete(config: AppConfig): Boolean {
        return config.openHabBaseUrl().isNotBlank() &&
            config.openHabApiToken().isNotBlank() &&
            config.threadDataset().isNotBlank() &&
            config.otbrBaseUrl().isNotBlank() &&
            !config.openHabApiTokenUnreadable() &&
            !config.threadDatasetUnreadable()
    }
}
