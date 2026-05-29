package org.openhab.matter.companion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.openhab.matter.companion.setup.CommissioningWindowCountdown
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupDeviceIdentity
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.SetupStepList

@Composable
fun SetupProgressScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    val countdownSeconds = state.countdownSeconds
    var displayedCountdownSeconds by remember(countdownSeconds) {
        mutableIntStateOf(countdownSeconds ?: 0)
    }
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds != null) {
            displayedCountdownSeconds = countdownSeconds
            while (displayedCountdownSeconds > 0) {
                delay(1_000L)
                displayedCountdownSeconds -= 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(text = state.message)
        val countdownText = countdownSeconds?.let {
            CommissioningWindowCountdown.displayText(displayedCountdownSeconds)
        }
        val deviceIdentity = state.deviceIdentity
        if (deviceIdentity != null && countdownText != null) {
            Spacer(Modifier.height(12.dp))
            PairingWindowDeviceCard(
                countdownText = countdownText,
                deviceIdentity = deviceIdentity
            )
        } else if (countdownText != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = countdownText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        SetupStepList(steps = state.steps)
        if (MatterSetupAction.ShowTroubleshooting in state.secondaryActions) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.ShowTroubleshooting) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show troubleshooting")
            }
        }
    }
}

@Composable
private fun PairingWindowDeviceCard(
    countdownText: String,
    deviceIdentity: MatterSetupDeviceIdentity
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = countdownText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            if (deviceIdentity.vendorName.isNotBlank()) {
                DeviceIdentityRow(label = "Vendor", value = deviceIdentity.vendorName)
            }
            if (deviceIdentity.vendorName.isNotBlank() && deviceIdentity.productName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
            }
            if (deviceIdentity.productName.isNotBlank()) {
                DeviceIdentityRow(label = "Product", value = deviceIdentity.productName)
            }
        }
    }
}

@Composable
private fun DeviceIdentityRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(76.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
