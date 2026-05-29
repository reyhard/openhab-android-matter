package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun ChangeTokenScreen(
    state: MatterSetupUiState,
    token: String,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToSettings) }
    ) {
        SettingsCard {
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access token") },
                supportingText = { Text("Create one in openHAB under Profile / API tokens.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = {}) {
                        Text("Show")
                    }
                }
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = state.primaryActionEnabled,
            onClick = { onAction(MatterSetupAction.SaveChangedToken) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Save token" })
        }
    }
}
