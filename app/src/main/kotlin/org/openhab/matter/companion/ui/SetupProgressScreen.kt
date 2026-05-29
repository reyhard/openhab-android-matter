package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.openhab.matter.companion.setup.CommissioningWindowCountdown
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.SetupStepList

@Composable
fun SetupProgressScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    val countdownSeconds = state.countdownSeconds
    var displayedCountdownSeconds by remember(countdownSeconds) {
        mutableIntStateOf(countdownSeconds ?: 0)
    }
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds != null) {
            displayedCountdownSeconds = countdownSeconds
            while (displayedCountdownSeconds > 0) {
                delay(1_000L)
                displayedCountdownSeconds -= 1
            }
        }
    }

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
        countdownSeconds?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = CommissioningWindowCountdown.displayText(displayedCountdownSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        SetupStepList(steps = state.steps)
        if (MatterSetupAction.ShowTroubleshooting in state.secondaryActions) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { onAction(MatterSetupAction.ShowTroubleshooting) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show troubleshooting")
            }
        }
    }
}
