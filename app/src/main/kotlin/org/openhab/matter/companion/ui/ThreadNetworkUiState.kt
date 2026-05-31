package org.openhab.matter.companion.ui

import org.openhab.matter.companion.otbr.OtbrStatus
import org.openhab.matter.companion.setup.ThreadDatasetSettingsStatus
import org.openhab.matter.companion.setup.ThreadDatasetSettingsValidator

enum class ThreadNetworkStateKind {
    Unknown,
    Checking,
    Ready,
    DatasetError,
    MissingBorderRouter,
    BorderRouterError
}

data class ThreadNetworkUiState(
    val kind: ThreadNetworkStateKind,
    val title: String,
    val message: String,
    val statusLabel: String,
    val ready: Boolean
) {
    companion object {
        fun notChecked(dataset: String, otbrBaseUrl: String): ThreadNetworkUiState {
            val datasetValidation = ThreadDatasetSettingsValidator.validate(dataset)
            if (datasetValidation.status != ThreadDatasetSettingsStatus.Valid) {
                return datasetError(datasetValidation.title, datasetValidation.message)
            }
            if (otbrBaseUrl.isBlank()) {
                return missingBorderRouter()
            }
            return ThreadNetworkUiState(
                kind = ThreadNetworkStateKind.Unknown,
                title = "Thread network not checked",
                message = "Check the Thread dataset and Border Router address before pairing.",
                statusLabel = "Not checked",
                ready = false
            )
        }

        fun checking(): ThreadNetworkUiState {
            return ThreadNetworkUiState(
                kind = ThreadNetworkStateKind.Checking,
                title = "Checking Thread network",
                message = "Verifying the dataset and Border Router address.",
                statusLabel = "Checking",
                ready = false
            )
        }

        fun fromInputs(dataset: String, otbrBaseUrl: String, otbrStatus: OtbrStatus): ThreadNetworkUiState {
            val datasetValidation = ThreadDatasetSettingsValidator.validate(dataset)
            if (datasetValidation.status != ThreadDatasetSettingsStatus.Valid) {
                return datasetError(datasetValidation.title, datasetValidation.message)
            }
            if (otbrBaseUrl.isBlank()) {
                return missingBorderRouter()
            }
            if (!otbrStatus.reachable()) {
                return ThreadNetworkUiState(
                    kind = ThreadNetworkStateKind.BorderRouterError,
                    title = "Check Thread Border Router",
                    message = "Thread Border Router address is not reachable.",
                    statusLabel = "Router error",
                    ready = false
                )
            }
            return ThreadNetworkUiState(
                kind = ThreadNetworkStateKind.Ready,
                title = "Thread network ready",
                message = "Thread dataset and Border Router address are ready.",
                statusLabel = "Ready",
                ready = true
            )
        }

        private fun datasetError(title: String, message: String): ThreadNetworkUiState {
            return ThreadNetworkUiState(
                kind = ThreadNetworkStateKind.DatasetError,
                title = title,
                message = message,
                statusLabel = "Dataset error",
                ready = false
            )
        }

        private fun missingBorderRouter(): ThreadNetworkUiState {
            return ThreadNetworkUiState(
                kind = ThreadNetworkStateKind.MissingBorderRouter,
                title = "Thread Border Router missing",
                message = "Enter a Thread Border Router address before pairing.",
                statusLabel = "Missing router",
                ready = false
            )
        }
    }
}
