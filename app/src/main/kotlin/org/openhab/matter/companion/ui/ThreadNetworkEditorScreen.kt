package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun ThreadNetworkEditorScreen(
    state: MatterSetupUiState,
    threadDataset: String,
    otbrBaseUrl: String,
    threadSettingsMessage: String,
    threadBorderRouters: List<ThreadBorderRouterRecord>,
    threadBorderRouterDiscoveryInProgress: Boolean,
    onThreadDatasetChange: (String) -> Unit,
    onOtbrBaseUrlChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    var threadDatasetVisible by remember { mutableStateOf(false) }
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToSettings) }
    ) {
        SettingsCard {
            OutlinedTextField(
                value = threadDataset,
                onValueChange = onThreadDatasetChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                label = { Text("Active Operational Dataset") },
                visualTransformation = if (threadDatasetVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    TextButton(onClick = { threadDatasetVisible = !threadDatasetVisible }) {
                        Text(if (threadDatasetVisible) "Hide" else "Show")
                    }
                }
            )
            OutlinedTextField(
                value = otbrBaseUrl,
                onValueChange = onOtbrBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Thread Border Router address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedButton(
                enabled = !threadBorderRouterDiscoveryInProgress,
                onClick = { onAction(MatterSetupAction.DetectThreadBorderRouters) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (threadBorderRouterDiscoveryInProgress) "Detecting..." else "Detect border router")
            }
            threadBorderRouters.forEach { router ->
                TextButton(
                    onClick = { onAction(MatterSetupAction.SelectThreadBorderRouter(router.endpoint)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${router.displayName} - ${router.endpoint}")
                }
            }
            if (threadSettingsMessage.isNotBlank()) {
                Text(
                    text = threadSettingsMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.CheckThreadDataset) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check dataset")
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.BackToSettings) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                enabled = state.primaryActionEnabled,
                onClick = { onAction(MatterSetupAction.SaveThreadSettings) },
                modifier = Modifier.weight(1f)
            ) {
                Text(state.primaryActionLabel.ifBlank { "Save" })
            }
        }
    }
}
