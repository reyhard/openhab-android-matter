package org.openhab.matter.companion.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openhab.matter.companion.R
import org.openhab.matter.companion.setup.MatterSetupAction
import org.openhab.matter.companion.setup.MatterSetupUiState
import org.openhab.matter.companion.ui.components.MatterSetupScaffold

@Composable
fun WelcomeScreen(
    state: MatterSetupUiState,
    onAction: (MatterSetupAction) -> Unit
) {
    MatterSetupScaffold(
        title = state.title,
        message = "A guided local setup for adding Matter devices to your openHAB home."
    ) {
        Image(
            painter = painterResource(R.drawable.openhab_setup_hero),
            contentDescription = "openHAB Matter setup",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(24.dp))
        BenefitCard(
            title = "Easy and guided",
            message = "Clear steps to get you set up quickly.",
            iconRes = R.drawable.ic_welcome_easy,
            iconTint = Color(0xFFEF7B18),
            iconBackground = Color(0xFFFFF3E7)
        )
        Spacer(Modifier.height(12.dp))
        BenefitCard(
            title = "Private and local",
            message = "Your devices stay in your home.",
            iconRes = R.drawable.ic_welcome_private,
            iconTint = Color(0xFF1F8B46),
            iconBackground = Color(0xFFEAF6EE)
        )
        Spacer(Modifier.height(12.dp))
        BenefitCard(
            title = "One home, everything together",
            message = "Matter devices work beautifully with openHAB.",
            iconRes = R.drawable.ic_welcome_home,
            iconTint = Color(0xFF5B56B3),
            iconBackground = Color(0xFFF0EEFF)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onAction(MatterSetupAction.GetStarted) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(state.primaryActionLabel.ifBlank { "Get started" })
        }
    }
}

@Composable
private fun BenefitCard(
    title: String,
    message: String,
    iconRes: Int,
    iconTint: Color,
    iconBackground: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
