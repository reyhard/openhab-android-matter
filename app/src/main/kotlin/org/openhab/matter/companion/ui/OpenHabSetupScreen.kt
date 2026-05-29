package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.diagnostics.ThreadBorderRouterRecord
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun OpenHabSetupScreen(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    threadDataset: String,
    otbrBaseUrl: String,
    attestationBypassEnabled: Boolean,
    threadSettingsMessage: String,
    threadBorderRouters: List<ThreadBorderRouterRecord>,
    threadBorderRouterDiscoveryInProgress: Boolean,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onThreadDatasetChange: (String) -> Unit,
    onOtbrBaseUrlChange: (String) -> Unit,
    onAttestationBypassChange: (Boolean) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    val effectiveOpenHabUrl = openHabUrl.ifBlank { state.openHabUrlFallback }
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
        if (MatterSetupAction.BackToMainMenu in state.secondaryActions) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.BackToMainMenu) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to main menu")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.ShowPhoneDevices) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Devices on this phone")
        }
        if (MatterSetupAction.ShowTroubleshooting in state.secondaryActions) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.ShowTroubleshooting) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Advanced troubleshooting")
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "openHAB",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = effectiveOpenHabUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("openHAB address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = state.primaryActionEnabled,
            onClick = { state.primaryAction?.let(onAction) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Continue" })
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Thread network",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Use manual mode when the app cannot read the dataset from your Thread Border Router."
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = threadDataset,
            onValueChange = onThreadDatasetChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            label = { Text("Active Operational Dataset") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = otbrBaseUrl,
            onValueChange = onOtbrBaseUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Thread Border Router address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.layout.Row {
            Checkbox(
                checked = attestationBypassEnabled,
                onCheckedChange = onAttestationBypassChange
            )
            Text(
                text = "Developer attestation bypass",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (threadSettingsMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = threadSettingsMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.CheckThreadDataset) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check dataset")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.SaveThreadSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Thread settings")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            enabled = !threadBorderRouterDiscoveryInProgress,
            onClick = { onAction(MatterSetupAction.DetectThreadBorderRouters) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (threadBorderRouterDiscoveryInProgress) "Detecting..." else "Detect border routers")
        }
        threadBorderRouters.forEach { router ->
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    onAction(MatterSetupAction.SelectThreadBorderRouter(router.endpoint))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${router.displayName} - ${router.endpoint}")
            }
        }
    }
}
