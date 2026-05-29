package org.openhab.matter.companion.setup

import org.openhab.matter.companion.openhab.OpenHabStatus
import org.openhab.matter.companion.otbr.OtbrStatus

data class FirstRunSettingsValidationResult(
    val ready: Boolean,
    val details: List<String>,
    val warnings: List<String>
)

object FirstRunSettingsValidator {
    fun validate(
        openHabUrl: String,
        token: String,
        dataset: String,
        otbrBaseUrl: String,
        openHabStatus: OpenHabStatus,
        otbrStatus: OtbrStatus
    ): FirstRunSettingsValidationResult {
        val datasetValidation = ThreadDatasetSettingsValidator.validate(dataset)
        val warnings = buildList {
            if (openHabUrl.isBlank()) {
                add("openHAB address is required")
            }
            if (token.isBlank()) {
                add("openHAB access token is required")
            }
            if (!openHabStatus.restReachable()) {
                add(openHabStatus.message().orEmpty().ifBlank { "openHAB REST API is not reachable" })
            }
            if (!openHabStatus.matterControllerReady()) {
                add(openHabStatus.message().orEmpty().ifBlank { "openHAB Matter controller is not ready" })
            }
            if (datasetValidation.status != ThreadDatasetSettingsStatus.Valid) {
                add(datasetValidation.title)
            }
            if (otbrBaseUrl.isBlank()) {
                add("Thread Border Router address is required")
            } else if (!otbrStatus.reachable()) {
                add(otbrStatus.message().ifBlank { "Thread Border Router is not reachable" })
            }
        }
            .map { it.redact(token, dataset) }
            .dedupeNonBlank()

        val details = buildList {
            add("openHAB URL configured=${openHabUrl.isNotBlank()}")
            add("openHAB token configured=${token.isNotBlank()}")
            add("openHAB REST reachable=${openHabStatus.restReachable()}")
            add("openHAB Matter controller ready=${openHabStatus.matterControllerReady()}")
            add("Thread dataset valid=${datasetValidation.status == ThreadDatasetSettingsStatus.Valid}")
            add("Thread Border Router configured=${otbrBaseUrl.isNotBlank()}")
            add("Thread Border Router reachable=${otbrStatus.reachable()}")
            add(openHabStatus.message().orEmpty())
            add(openHabStatus.details().orEmpty())
            add(otbrStatus.message())
            add(otbrStatus.details())
        }
            .map { it.redact(token, dataset) }
            .dedupeNonBlank()

        return FirstRunSettingsValidationResult(
            ready = warnings.isEmpty() && openHabStatus.online(),
            details = details,
            warnings = warnings
        )
    }

    private fun List<String>.dedupeNonBlank(): List<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun String.redact(token: String, dataset: String): String {
        return listOf(token, dataset)
            .filter { it.isNotBlank() }
            .fold(sanitizeLogUrls()) { sanitized, secret ->
                sanitized.replace(secret, "<redacted>")
            }
    }
}
