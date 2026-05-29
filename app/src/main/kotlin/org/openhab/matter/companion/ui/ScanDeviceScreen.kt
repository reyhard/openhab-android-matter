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
fun ScanDeviceScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = state.title.ifBlank { "Scan your device code" },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.message.ifBlank {
                "Point your camera at the Matter QR code on the device or box."
            }
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.StartScan) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR code")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.EditSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }
    }
}
