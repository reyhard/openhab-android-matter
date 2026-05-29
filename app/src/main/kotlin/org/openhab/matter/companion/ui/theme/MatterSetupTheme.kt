package org.openhab.matter.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MatterSetupColorScheme = lightColorScheme(
    primary = Color(0xFF1769D2),
    secondary = Color(0xFF4F6F52),
    tertiary = Color(0xFFC75B12),
    background = Color(0xFFFAF8F4),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E)
)

@Composable
fun MatterSetupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MatterSetupColorScheme,
        content = content
    )
}
