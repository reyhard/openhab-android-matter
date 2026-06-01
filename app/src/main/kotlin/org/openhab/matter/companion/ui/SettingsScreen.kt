package org.openhab.matter.companion.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun SettingsScreen(
    state: MatterSetupUiState,
    openHabUrl: String,
    tokenSet: Boolean,
    openHabConnectionState: OpenHabConnectionUiState,
    threadNetworkState: ThreadNetworkUiState,
    otbrBaseUrl: String,
    phoneDeviceCount: Int,
    attestationBypassEnabled: Boolean,
    onAttestationBypassChange: (Boolean) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToMainMenu) }
    ) {
        SectionLabel("openHAB connection")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "openHAB address",
                value = openHabUrl.ifBlank { "Not set" },
                status = if (openHabConnectionState.kind == OpenHabConnectionStateKind.TokenError) {
                    "Address set"
                } else {
                    openHabConnectionState.statusLabel
                },
                onClick = { onAction(MatterSetupAction.EditOpenHabAddress) }
            )
            SettingsRow(
                title = "Access token",
                value = if (tokenSet) {
                    if (openHabConnectionState.kind == OpenHabConnectionStateKind.TokenError) {
                        openHabConnectionState.message
                    } else {
                        "Stored securely"
                    }
                } else {
                    "Not set"
                },
                status = when (openHabConnectionState.kind) {
                    OpenHabConnectionStateKind.TokenError,
                    OpenHabConnectionStateKind.MissingToken -> openHabConnectionState.statusLabel
                    else -> if (tokenSet) "Set" else "Missing"
                },
                onClick = { onAction(MatterSetupAction.EditOpenHabAddress) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("Thread network")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "Thread network settings",
                value = if (threadNetworkState.kind == ThreadNetworkStateKind.DatasetError) {
                    threadNetworkState.message
                } else if (threadNetworkState.kind == ThreadNetworkStateKind.MissingBorderRouter ||
                    threadNetworkState.kind == ThreadNetworkStateKind.BorderRouterError) {
                    threadNetworkState.message
                } else if (threadNetworkState.ready || threadNetworkState.kind == ThreadNetworkStateKind.Unknown ||
                    threadNetworkState.kind == ThreadNetworkStateKind.Checking) {
                    "Dataset stored. Border router: ${otbrBaseUrl.ifBlank { "not set" }}"
                } else {
                    "Dataset not set"
                },
                status = threadNetworkState.statusLabel,
                onClick = { onAction(MatterSetupAction.EditThreadNetwork) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("This phone")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "Devices on this phone",
                value = "$phoneDeviceCount staged",
                onClick = { onAction(MatterSetupAction.ShowPhoneDevices) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("Advanced")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsSwitchRow(
                title = "Attestation bypass",
                value = if (attestationBypassEnabled) "Enabled" else "Disabled",
                checked = attestationBypassEnabled,
                onCheckedChange = onAttestationBypassChange
            )
            SettingsRow(
                title = "Advanced troubleshooting",
                value = "Setup checks and network tools",
                onClick = { onAction(MatterSetupAction.ShowTroubleshooting) }
            )
            SettingsRow(
                title = "About",
                value = "Version, license, and notices",
                onClick = { onAction(MatterSetupAction.ShowAbout) }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    status: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (status != null) {
            StatusPill(status)
        }
        Text(
            text = ">",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    val positive = text in setOf("Set", "Ready", "Valid")
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (positive) Color(0xFFDFF5E3) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (positive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
