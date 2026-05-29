package org.openhab.matter.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun ScanDeviceScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title.ifBlank { "Add Matter device" },
        message = state.message.ifBlank { "Scan the device QR code or enter the setup code manually." },
        showSettings = true,
        onSettings = { onAction(MatterSetupAction.EditSettings) }
    ) {
        Image(
            painter = painterResource(R.drawable.matter_scan_guide),
            contentDescription = "Matter scan guide",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(24.dp))
        ReadinessGuideCard()
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.StartScan) },
            modifier = Modifier.fillMaxWidth()
        ) {
            QrCodeIcon()
            Spacer(Modifier.width(12.dp))
            Text(state.primaryActionLabel.ifBlank { "Scan code" })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.EnterCodeManually) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter code manually")
        }
    }
}

@Composable
private fun ReadinessGuideCard() {
    SettingsCard {
        SectionLabel("Ready to pair")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadinessRow("openHAB connected")
            ReadinessRow("Thread network ready")
            ReadinessRow("Bluetooth and location ready")
        }
        Text(
            text = "You can enter the 11-digit setup code instead.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadinessRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ready",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun QrCodeIcon() {
    val color = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = Modifier.size(20.dp)) {
        val cell = size.width / 5f
        val squares = listOf(
            0 to 0, 1 to 0, 0 to 1,
            3 to 0, 4 to 0, 4 to 1,
            0 to 3, 0 to 4, 1 to 4,
            2 to 2, 3 to 3, 4 to 4
        )
        squares.forEach { (x, y) ->
            drawRect(
                color = color,
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell * 0.72f, cell * 0.72f)
            )
        }
    }
}
