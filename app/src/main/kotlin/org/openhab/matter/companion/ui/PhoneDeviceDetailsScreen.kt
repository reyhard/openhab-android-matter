package org.openhab.matter.companion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterDeviceDetailFormatter
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.setup.PhoneMatterDeviceDetails
import org.openhab.matter.companion.ui.components.MatterSetupScaffold

@Composable
fun PhoneDeviceDetailsScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    val details = state.phoneDeviceDetails
    var copyFeedback by remember { mutableStateOf("") }
    MatterSetupScaffold(
        title = "Device details",
        message = state.message.ifBlank { "Helpful information for advanced setup and troubleshooting." },
        showBack = true,
        onBack = { onAction(MatterSetupAction.ShowPhoneDevices) }
    ) {
        Text(
            text = "Advanced",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onAction(MatterSetupAction.ShowPhoneDevices) }
        )
        Spacer(Modifier.height(16.dp))
        DeviceHeaderCard(details)
        Spacer(Modifier.height(16.dp))
        DeviceDetailsCard(
            details = details,
            onCopied = { label ->
                copyFeedback = "Copied $label"
            }
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.FetchPhoneDeviceDetails) },
            enabled = !state.phoneDeviceDetailsFetching,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            if (state.phoneDeviceDetailsFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_material_cloud_download),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(if (state.phoneDeviceDetailsFetching) "Fetching..." else "Fetch additional data from device")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Reads more information directly from Matter clusters.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.phoneDeviceDetailsMessage in setOf("Could not fetch data from device", "Device data refreshed")) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.phoneDeviceDetailsMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.phoneDeviceDetailsMessage == "Could not fetch data from device") {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        if (copyFeedback.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = copyFeedback,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.OpenCommissioningWindowAgain) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_material_open_in_new),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Open commissioning window")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.ForgetFromPhone) },
            modifier = Modifier.fillMaxWidth()
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

@Composable
private fun DeviceHeaderCard(details: PhoneMatterDeviceDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = MatterDeviceDetailFormatter.display(
                    details.deviceName,
                    MatterDeviceDetailFormatter.UNKNOWN_PRODUCT
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFFF4EAD8),
                contentColor = Color(0xFF6B4E16)
            ) {
                Text(
                    text = "Not yet added to openHAB",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailsCard(
    details: PhoneMatterDeviceDetails,
    onCopied: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            val rows = detailRows(details)
            rows.forEachIndexed { index, row ->
                DeviceDetailRow(row = row, onCopied = onCopied)
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceDetailRow(
    row: DeviceDetailItem,
    onCopied: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboard.setText(AnnotatedString(row.value))
                onCopied(row.label)
            }
            .semantics { contentDescription = "Copy ${row.label}" }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(row.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class DeviceDetailItem(
    val label: String,
    val value: String,
    val icon: Int
)

private fun detailRows(details: PhoneMatterDeviceDetails): List<DeviceDetailItem> {
    return listOf(
        DeviceDetailItem(
            label = "Device name",
            value = display(details.deviceName, MatterDeviceDetailFormatter.UNKNOWN_PRODUCT),
            icon = R.drawable.ic_material_label
        ),
        DeviceDetailItem(
            label = "Vendor",
            value = display(details.vendor, MatterDeviceDetailFormatter.UNKNOWN_VENDOR),
            icon = R.drawable.ic_material_store
        ),
        DeviceDetailItem(
            label = "Product",
            value = display(details.product),
            icon = R.drawable.ic_material_inventory
        ),
        DeviceDetailItem(
            label = "Firmware version",
            value = display(details.firmwareVersion),
            icon = R.drawable.ic_material_label
        ),
        DeviceDetailItem(
            label = "Hardware version",
            value = display(details.hardwareVersion),
            icon = R.drawable.ic_material_settings
        ),
        DeviceDetailItem(
            label = "Part number",
            value = display(details.partNumber),
            icon = R.drawable.ic_material_label
        ),
        DeviceDetailItem(
            label = "Node ID",
            value = display(details.nodeId),
            icon = R.drawable.ic_material_numbers
        ),
        DeviceDetailItem(
            label = "Battery",
            value = display(details.battery),
            icon = R.drawable.ic_material_battery
        ),
        DeviceDetailItem(
            label = "Thread network",
            value = display(details.threadNetwork),
            icon = R.drawable.ic_material_wifi
        ),
        DeviceDetailItem(
            label = "IPv6 address",
            value = display(details.ipv6Address),
            icon = R.drawable.ic_material_public
        ),
        DeviceDetailItem(
            label = "OTA update",
            value = display(details.otaUpdate),
            icon = R.drawable.ic_material_cloud_download
        )
    )
}

private fun display(
    value: String,
    fallback: String = MatterDeviceDetailFormatter.UNKNOWN
): String {
    return MatterDeviceDetailFormatter.display(value, fallback)
}
