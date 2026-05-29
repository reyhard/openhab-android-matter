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
import org.openhab.matter.companion.setup.PhoneMatterDevice

@Composable
fun PhoneDeviceListScreen(
    state: MatterSetupUiState,
    devices: List<PhoneMatterDevice>,
    onAction: (MatterSetupAction) -> Unit
) {
    val hasCommissioningWindowDevice = devices.any { it.canOpenCommissioningWindow }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = state.title.ifBlank { "Devices on this phone" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(text = state.message)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.BackToSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to settings")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.BackToMainMenu) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to main menu")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Stored Matter staging",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This list only shows devices staged by this app on the Android fabric for openHAB handoff."
        )
        Spacer(Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text(
                text = "No staged Matter devices are stored on this phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            devices.forEach { device ->
                PhoneMatterDeviceSummary(device)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = hasCommissioningWindowDevice,
                onClick = { onAction(MatterSetupAction.OpenCommissioningWindowAgain) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open pairing window again")
            }
            if (!hasCommissioningWindowDevice) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pairing window retry needs a readable stored node and controller state.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.ForgetFromPhone) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Forget from this phone")
            }
        }
    }
}

@Composable
private fun PhoneMatterDeviceSummary(device: PhoneMatterDevice) {
    Text(
        text = device.displayNodeId,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
    Text(text = device.status)
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Controller state: ${if (device.controllerStateStored) "stored" else "missing"}",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "State readable: ${if (device.stateReadable) "yes" else "no"}",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
