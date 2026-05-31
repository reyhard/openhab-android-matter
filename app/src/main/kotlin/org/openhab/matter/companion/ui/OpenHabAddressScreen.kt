package org.openhab.matter.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun OpenHabAddressScreen(
    state: MatterSetupUiState,
    openHabUrl: String,
    token: String,
    tokenSet: Boolean,
    openHabConnectionState: OpenHabConnectionUiState,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onAction: (MatterSetupAction) -> Unit
) {
    var tokenVisible by remember { mutableStateOf(false) }
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToSettings) }
    ) {
        SettingsCard {
            OutlinedTextField(
                value = openHabUrl,
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
                supportingText = {
                    Text(
                        if (tokenSet) {
                            "Leave blank to keep the stored token."
                        } else {
                            "Create one in openHAB under Profile / API tokens."
                        }
                    )
                },
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
            OpenHabConnectionStatusCard(openHabConnectionState, onAction)
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
                onClick = { onAction(MatterSetupAction.SaveOpenHabAddress) },
                modifier = Modifier.weight(1f)
            ) {
                Text(state.primaryActionLabel.ifBlank { "Save" })
            }
        }
    }
}

@Composable
private fun OpenHabConnectionStatusCard(
    state: OpenHabConnectionUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    val colors = connectionColors(state.kind)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                ConnectionStateIcon(state.kind)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                enabled = state.kind != OpenHabConnectionStateKind.Checking,
                onClick = { onAction(MatterSetupAction.TestOpenHab) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.kind == OpenHabConnectionStateKind.Checking) "Testing..." else "Test connection")
            }
        }
    }
}

@Composable
private fun ConnectionStateIcon(kind: OpenHabConnectionStateKind) {
    val colors = connectionColors(kind)
    Box(
        modifier = Modifier.size(34.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            drawCircle(color = colors.iconBackground)
            if (kind == OpenHabConnectionStateKind.Connected) {
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.28f, size.height * 0.52f),
                    end = Offset(size.width * 0.43f, size.height * 0.68f),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.43f, size.height * 0.68f),
                    end = Offset(size.width * 0.74f, size.height * 0.32f),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.50f, size.height * 0.24f),
                    end = Offset(size.width * 0.50f, size.height * 0.58f),
                    strokeWidth = 3.0.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8.dp.toPx(),
                    center = Offset(size.width * 0.50f, size.height * 0.75f)
                )
            }
        }
    }
}

private data class ConnectionColors(
    val background: Color,
    val iconBackground: Color
)

private fun connectionColors(kind: OpenHabConnectionStateKind): ConnectionColors {
    return when (kind) {
        OpenHabConnectionStateKind.Connected -> ConnectionColors(Color(0xFFF6F9F3), Color(0xFF2E9E44))
        OpenHabConnectionStateKind.Checking,
        OpenHabConnectionStateKind.Unknown -> ConnectionColors(Color(0xFFF7F9FE), Color(0xFF1769D2))
        else -> ConnectionColors(Color(0xFFFFF8F4), Color(0xFFB3261E))
    }
}
