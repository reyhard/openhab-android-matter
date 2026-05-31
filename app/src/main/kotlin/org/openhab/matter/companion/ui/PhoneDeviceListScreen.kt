package org.openhab.matter.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.PhoneMatterDevice
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel

@Composable
fun PhoneDeviceListScreen(
    state: MatterSetupUiState,
    devices: List<PhoneMatterDevice>,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title.ifBlank { "Devices on this phone" },
        message = state.message.ifBlank { "Matter devices staged by this app for openHAB handoff." },
        showBack = true,
        onBack = { onAction(state.primaryAction ?: MatterSetupAction.BackToSettings) }
    ) {
        SectionLabel("Stored Matter staging")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This list only shows devices staged by this app on the Android fabric for openHAB handoff.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text(
                text = "No staged Matter devices are stored on this phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                devices.forEach { device ->
                    PhoneMatterDeviceCard(device = device, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun PhoneMatterDeviceCard(
    device: PhoneMatterDevice,
    onAction: (MatterSetupAction) -> Unit
) {
    var expanded by rememberSaveable(device.nodeId, device.displayNodeId) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.displayProductName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = device.displayVendorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        painter = painterResource(
                            if (expanded) {
                                R.drawable.ic_material_expand_less
                            } else {
                                R.drawable.ic_material_expand_more
                            }
                        ),
                        contentDescription = if (expanded) "Hide diagnostics" else "Show diagnostics"
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                PhoneMatterDiagnostics(device)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAction(MatterSetupAction.OpenCommissioningWindowAgain) },
                    enabled = device.canAttemptCommissioningWindowForDebug,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Open commissioning window" }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_material_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Open commissioning window")
                }
                OutlinedButton(
                    onClick = { onAction(MatterSetupAction.ShowPhoneDeviceDetails(device.nodeId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "View details" }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_material_info),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Details")
                }
                OutlinedButton(
                    onClick = { onAction(MatterSetupAction.ForgetFromPhone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Forget from this phone" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_material_delete),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Forget from this phone")
                }
            }
        }
    }
}

@Composable
private fun PhoneMatterDiagnostics(device: PhoneMatterDevice) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiagnosticRow(label = "Node", value = device.displayNodeId)
        DiagnosticRow(label = "Controller state", value = device.displayControllerState)
        DiagnosticRow(label = "State readable", value = device.displayStateReadable)
        if (!device.controllerStateStored) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFEAF4FF),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_material_info),
                    contentDescription = null,
                    tint = Color(0xFF0B57D0),
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Debug information",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0B57D0)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Debug attempt: controller state is missing, so connectedhomeip may still fail to open the commissioning window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF174EA6)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.1f)
        )
    }
}
