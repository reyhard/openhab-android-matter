package org.openhab.matter.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
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
    scanReadiness: ScanReadinessUiState,
    openHabConnectionState: OpenHabConnectionUiState,
    threadNetworkState: ThreadNetworkUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title.ifBlank { "Add Matter device" },
        message = state.message.ifBlank { "Scan the Matter QR code on the device or box." },
        showSettings = true,
        centerText = true,
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
        ReadinessGuideCard(scanReadiness, openHabConnectionState, threadNetworkState, onAction)
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
private fun ReadinessGuideCard(
    scanReadiness: ScanReadinessUiState,
    openHabConnectionState: OpenHabConnectionUiState,
    threadNetworkState: ThreadNetworkUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    SettingsCard {
        SectionLabel("Ready to pair")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadinessRow(openHabConnectionState.title, ready = openHabConnectionState.ready)
            ReadinessRow(threadNetworkState.title, ready = threadNetworkState.ready)
            ReadinessRow("Bluetooth and location ready", ready = scanReadiness.ready)
        }
        if (!openHabConnectionState.ready || !threadNetworkState.ready || !scanReadiness.ready) {
            ReadinessDetails(scanReadiness, openHabConnectionState, threadNetworkState, onAction)
        }
    }
}

@Composable
private fun ReadinessRow(
    text: String,
    ready: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ready) {
            ReadyCheckIcon()
        } else {
            AttentionIcon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReadinessDetails(
    scanReadiness: ScanReadinessUiState,
    openHabConnectionState: OpenHabConnectionUiState,
    threadNetworkState: ThreadNetworkUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!openHabConnectionState.ready) {
            Text(
                text = openHabConnectionState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HelperButton("Fix openHAB settings") {
                onAction(MatterSetupAction.EditSettings)
            }
        }
        if (!threadNetworkState.ready) {
            Text(
                text = threadNetworkState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HelperButton("Fix Thread settings") {
                onAction(MatterSetupAction.EditSettings)
            }
        }
        listOf(
            scanReadiness.bluetoothMessage,
            scanReadiness.locationMessage,
            scanReadiness.permissionsMessage
        ).filter { it.isNotBlank() }
            .forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        if (scanReadiness.bluetoothHelperAvailable) {
            HelperButton("Turn on Bluetooth") {
                onAction(MatterSetupAction.OpenBluetoothSettings)
            }
        }
        if (scanReadiness.locationHelperAvailable) {
            HelperButton("Open location settings") {
                onAction(MatterSetupAction.OpenLocationSettings)
            }
        }
        if (scanReadiness.permissionsHelperAvailable) {
            HelperButton("Grant permissions") {
                onAction(MatterSetupAction.RequestSetupPermissions)
            }
        }
    }
}

@Composable
private fun HelperButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(text)
    }
}

@Composable
private fun ReadyCheckIcon() {
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawRoundRect(
                color = Color(0xFFE7F0FE),
                size = Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
            drawLine(
                color = Color(0xFF1976D2),
                start = Offset(size.width * 0.25f, size.height * 0.52f),
                end = Offset(size.width * 0.43f, size.height * 0.70f),
                strokeWidth = stroke.width,
                cap = stroke.cap
            )
            drawLine(
                color = Color(0xFF1976D2),
                start = Offset(size.width * 0.43f, size.height * 0.70f),
                end = Offset(size.width * 0.76f, size.height * 0.30f),
                strokeWidth = stroke.width,
                cap = stroke.cap
            )
        }
    }
}

@Composable
private fun AttentionIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawRoundRect(
            color = Color(0xFFFFF3D8),
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawLine(
            color = Color(0xFFB26A00),
            start = Offset(size.width * 0.50f, size.height * 0.24f),
            end = Offset(size.width * 0.50f, size.height * 0.60f),
            strokeWidth = 2.4.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFB26A00),
            radius = 1.6.dp.toPx(),
            center = Offset(size.width * 0.50f, size.height * 0.76f)
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
