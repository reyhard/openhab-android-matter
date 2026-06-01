package org.openhab.matter.companion.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold
import org.openhab.matter.companion.ui.components.SectionLabel
import org.openhab.matter.companion.ui.components.SettingsCard

@Composable
fun AboutScreen(
    state: MatterSetupUiState,
    legalContent: AboutLegalContent,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = state.message,
        showBack = true,
        onBack = { onAction(MatterSetupAction.BackToSettings) }
    ) {
        SettingsCard {
            Text(
                text = "openHAB Matter helper",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Version ${legalContent.versionName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        LegalTextSection(
            title = "App license",
            text = legalContent.appLicense
        )
        Spacer(Modifier.height(24.dp))
        LegalTextSection(
            title = "Third-party notices",
            text = legalContent.thirdPartyNotices
        )
        Spacer(Modifier.height(24.dp))
        LegalTextSection(
            title = "Third-party licenses",
            text = legalContent.thirdPartyLicenses
        )
    }
}

@Composable
private fun LegalTextSection(
    title: String,
    text: String
) {
    SectionLabel(title)
    Spacer(Modifier.height(8.dp))
    SettingsCard {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
