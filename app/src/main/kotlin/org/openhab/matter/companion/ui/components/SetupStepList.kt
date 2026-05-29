package org.openhab.matter.companion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupStep
import org.openhab.matter.companion.setup.MatterSetupStepStatus

@Composable
fun SetupStepList(
    steps: List<MatterSetupStep>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = statusGlyph(step.status),
                    color = statusColor(step.status),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.label,
                        fontWeight = if (step.status == MatterSetupStepStatus.Active) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                    Text(
                        text = statusLabel(step.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(step.status)
                    )
                    if (step.detail.isNotBlank()) {
                        Text(
                            text = step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: MatterSetupStepStatus): Color = when (status) {
    MatterSetupStepStatus.Complete -> MaterialTheme.colorScheme.secondary
    MatterSetupStepStatus.Active -> MaterialTheme.colorScheme.primary
    MatterSetupStepStatus.Failed -> MaterialTheme.colorScheme.error
    MatterSetupStepStatus.Pending -> MaterialTheme.colorScheme.outline
}

private fun statusGlyph(status: MatterSetupStepStatus): String = when (status) {
    MatterSetupStepStatus.Complete -> "✓"
    MatterSetupStepStatus.Active -> "●"
    MatterSetupStepStatus.Failed -> "!"
    MatterSetupStepStatus.Pending -> "○"
}

private fun statusLabel(status: MatterSetupStepStatus): String = when (status) {
    MatterSetupStepStatus.Complete -> "Complete"
    MatterSetupStepStatus.Active -> "Active"
    MatterSetupStepStatus.Failed -> "Failed"
    MatterSetupStepStatus.Pending -> "Pending"
}
