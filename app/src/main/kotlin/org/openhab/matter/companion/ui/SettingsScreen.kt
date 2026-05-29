package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    threadDatasetSet: Boolean,
    otbrBaseUrl: String,
    phoneDeviceCount: Int,
    attestationBypassEnabled: Boolean,
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
                title = "Address",
                value = openHabUrl.ifBlank { "Not set" },
                status = "Ready"
            )
            SettingsRow(
                title = "Access token",
                value = if (tokenSet) "Stored securely" else "Not set",
                status = if (tokenSet) "Set" else "Missing",
                actionLabel = "Change token",
                onActionClick = { onAction(MatterSetupAction.ChangeToken) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("Thread network")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "Active Operational Dataset",
                value = if (threadDatasetSet) "Stored securely" else "Not set",
                status = if (threadDatasetSet) "Valid" else "Missing",
                actionLabel = "Edit",
                onActionClick = { onAction(MatterSetupAction.EditThreadNetwork) }
            )
            SettingsRow(
                title = "Border router",
                value = otbrBaseUrl.ifBlank { "Not set" },
                actionLabel = "Detect router",
                onActionClick = { onAction(MatterSetupAction.EditThreadNetwork) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("This phone")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "Staged devices",
                value = phoneDeviceCount.toString(),
                actionLabel = "Devices",
                onActionClick = { onAction(MatterSetupAction.ShowPhoneDevices) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SectionLabel("Advanced")
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingsRow(
                title = "Attestation bypass",
                value = if (attestationBypassEnabled) "Enabled" else "Disabled"
            )
            SettingsRow(
                title = "Diagnostics",
                value = "Setup checks and network tools",
                actionLabel = "Troubleshooting",
                onActionClick = { onAction(MatterSetupAction.ShowTroubleshooting) }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    status: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        if (actionLabel != null && onActionClick != null) {
            OutlinedButton(onClick = onActionClick) {
                Text(actionLabel)
            }
        }
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
