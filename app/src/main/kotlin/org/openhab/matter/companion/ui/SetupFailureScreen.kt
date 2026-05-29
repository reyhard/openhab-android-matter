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
fun SetupFailureScreen(
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
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(text = state.message)
        val details = state.failure?.details.orEmpty()
        if (details.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val suggestions = state.failure?.suggestions.orEmpty()
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Try this next",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            suggestions.forEach { suggestion ->
                Text(
                    text = "- $suggestion",
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.Retry) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Try again" })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.ShowTroubleshooting) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show troubleshooting")
        }
    }
}
