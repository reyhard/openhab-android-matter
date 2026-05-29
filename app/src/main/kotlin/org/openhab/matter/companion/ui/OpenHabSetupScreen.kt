package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

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
    var tokenVisible by remember { mutableStateOf(false) }
    var threadDatasetVisible by remember { mutableStateOf(false) }
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = false
    ) {
        SettingsCard {
            SectionLabel("openHAB connection")
            OutlinedTextField(
                value = effectiveOpenHabUrl,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("openHAB address") },
                supportingText = { Text("Address of your openHAB instance.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access token") },
                supportingText = { Text("Create one in openHAB under Profile / API tokens.") },
                singleLine = true,
                visualTransformation = if (tokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { tokenVisible = !tokenVisible }) {
                        Text(if (tokenVisible) "Hide" else "Show")
                    }
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            SectionLabel("Thread network")
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
                Text(threadSettingsMessage)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = state.primaryActionEnabled,
            onClick = { onAction(MatterSetupAction.TestSettings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Test settings" })
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
    }
}
