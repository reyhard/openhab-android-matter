package org.openhab.matter.companion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState

@Composable
fun SetupSuccessScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onAction(MatterSetupAction.BackToMainMenu) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_material_arrow_back),
                    contentDescription = "Back to main menu"
                )
            }
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.openhab_icon),
                contentDescription = "openHAB",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.title.ifBlank { "Device found by openHAB" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message.ifBlank { "openHAB reported a Matter Inbox entry for this device" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(36.dp))
            Image(
                painter = painterResource(R.drawable.openhab_inbox_success),
                contentDescription = "Device found in openHAB Inbox",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.OpenOpenHabInbox) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text("Open openHAB Inbox")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onAction(MatterSetupAction.AddAnotherDevice) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(state.primaryActionLabel.ifBlank { "Add another device" })
        }
    }
}
