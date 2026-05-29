package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun AdvancedTroubleshootingScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    val primaryAction = state.primaryAction ?: MatterSetupAction.Retry
    val primaryActionLabel = state.primaryActionLabel.ifBlank { "Back to setup" }
    val failure = state.failure
    val diagnostics = state.diagnostics
    val guidance = buildRecoveryGuidance(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = state.title.ifBlank { "Advanced troubleshooting" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Review the captured checks before retrying setup.",
            style = MaterialTheme.typography.bodyLarge
        )
        if (failure != null) {
            Spacer(Modifier.height(16.dp))
            TroubleshootingSection(title = "Failure") {
                Text(text = failure.message.ifBlank { state.message })
                if (failure.details.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = failure.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        TroubleshootingSection(title = "Checks") {
            TroubleshootingBullets(
                items = diagnostics.checks,
                emptyText = "No diagnostic checks were captured. Review the recovery guidance below after confirming the basics."
            )
        }
        Spacer(Modifier.height(16.dp))
        TroubleshootingSection(title = "Warnings") {
            TroubleshootingBullets(
                items = diagnostics.warnings,
                emptyText = "No warnings were captured."
            )
        }
        Spacer(Modifier.height(16.dp))
        TroubleshootingSection(title = "Details") {
            TroubleshootingBullets(
                items = diagnostics.details,
                emptyText = "No low-level diagnostic details were captured."
            )
        }
        Spacer(Modifier.height(16.dp))
        TroubleshootingSection(title = "Recovery guidance") {
            TroubleshootingBullets(items = guidance)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(primaryAction) },
            enabled = state.primaryActionEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(primaryActionLabel)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.OpenCommissioningWindowAgain) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open pairing window again")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.ForgetFromPhone) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Forget from this phone")
        }
    }
}

@Composable
private fun TroubleshootingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    content()
}

@Composable
private fun TroubleshootingBullets(
    items: List<String>,
    emptyText: String? = null
) {
    val visibleItems = items.filter { it.isNotBlank() }
    if (visibleItems.isEmpty()) {
        if (emptyText != null) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    visibleItems.forEach { item ->
        Text(
            text = "- $item",
            modifier = Modifier.padding(vertical = 3.dp)
        )
    }
}

private fun buildRecoveryGuidance(state: MatterSetupUiState): List<String> {
    return (
        state.failure?.suggestions.orEmpty() + listOf(
            "Confirm IPv6 routing works between openHAB, the OTBR or Thread border router, and the Matter device.",
            "Check that openHAB or its host can see current _matterc._udp mDNS records through Avahi, and clear stale records if needed.",
            "Disable VPNs that isolate local traffic, stay on the correct Wi-Fi, and keep Bluetooth and location enabled for commissioning.",
            "Confirm the device is still in pairing mode before retrying.",
            "If the openHAB pairing window expired, retry setup to request a fresh commissioning window before sending the code."
        )
    ).distinct()
}
