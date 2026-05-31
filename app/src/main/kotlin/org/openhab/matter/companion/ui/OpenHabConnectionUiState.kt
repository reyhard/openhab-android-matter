package org.openhab.matter.companion.ui

import org.openhab.matter.companion.openhab.OpenHabStatus

enum class OpenHabConnectionStateKind {
    Unknown,
    Checking,
    Connected,
    MissingToken,
    AddressError,
    TokenError,
    ControllerError
}

data class OpenHabConnectionUiState(
    val kind: OpenHabConnectionStateKind,
    val title: String,
    val message: String,
    val statusLabel: String,
    val ready: Boolean
) {
    companion object {
        fun notChecked(tokenSet: Boolean): OpenHabConnectionUiState {
            return if (tokenSet) {
                OpenHabConnectionUiState(
                    kind = OpenHabConnectionStateKind.Unknown,
                    title = "openHAB connection not checked",
                    message = "Test the connection to verify the address and token.",
                    statusLabel = "Not checked",
                    ready = false
                )
            } else {
                missingToken()
            }
        }

        fun checking(): OpenHabConnectionUiState {
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.Checking,
                title = "Checking openHAB",
                message = "Verifying the address and access token.",
                statusLabel = "Checking",
                ready = false
            )
        }

        fun connected(): OpenHabConnectionUiState {
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.Connected,
                title = "Connected to openHAB",
                message = "We were able to reach your openHAB home.",
                statusLabel = "Ready",
                ready = true
            )
        }

        fun missingToken(): OpenHabConnectionUiState {
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.MissingToken,
                title = "Add access token",
                message = "Paste an openHAB API token before testing the connection.",
                statusLabel = "Missing token",
                ready = false
            )
        }

        fun addressError(): OpenHabConnectionUiState {
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.AddressError,
                title = "Check openHAB address",
                message = "We could not reach openHAB. Check the address and network.",
                statusLabel = "Address error",
                ready = false
            )
        }

        fun tokenError(): OpenHabConnectionUiState {
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.TokenError,
                title = "Check access token",
                message = "openHAB rejected the access token. Paste a valid API token.",
                statusLabel = "Token error",
                ready = false
            )
        }

        fun fromStatus(status: OpenHabStatus): OpenHabConnectionUiState {
            if (status.online() && status.restReachable() && status.matterControllerReady()) {
                return connected()
            }
            if (status.message().orEmpty().contains("access token", ignoreCase = true)) {
                return tokenError()
            }
            if (!status.restReachable()) {
                return addressError()
            }
            return OpenHabConnectionUiState(
                kind = OpenHabConnectionStateKind.ControllerError,
                title = "Check Matter controller",
                message = status.message().orEmpty().ifBlank {
                    "openHAB is reachable, but the Matter controller is not ready."
                },
                statusLabel = "Needs attention",
                ready = false
            )
        }
    }
}
