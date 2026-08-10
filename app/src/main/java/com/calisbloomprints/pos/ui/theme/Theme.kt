package com.calisbloomprints.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB84F72),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E5),
    onPrimaryContainer = Color(0xFF5A112A),
    secondary = Color(0xFF557D58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEED7),
    onSecondaryContainer = Color(0xFF15381D),
    tertiary = Color(0xFF8C6BB1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4FF),
    onTertiaryContainer = Color(0xFF351D54),
    background = Color(0xFFFFF8FA),
    onBackground = Color(0xFF26191E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF26191E),
    surfaceVariant = Color(0xFFFFE8EF),
    onSurfaceVariant = Color(0xFF5B444D),
    outline = Color(0xFFD0A1AF),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5F1230),
    primaryContainer = Color(0xFF7F2947),
    onPrimaryContainer = Color(0xFFFFD8E5),
    secondary = Color(0xFFBFE2B9),
    onSecondary = Color(0xFF12371A),
    secondaryContainer = Color(0xFF2E5634),
    onSecondaryContainer = Color(0xFFDCEED7),
    tertiary = Color(0xFFD9BEFF),
    onTertiary = Color(0xFF3A205D),
    tertiaryContainer = Color(0xFF573C7A),
    onTertiaryContainer = Color(0xFFF0E4FF),
    background = Color(0xFF1B1417),
    onBackground = Color(0xFFF3E7EB),
    surface = Color(0xFF241C20),
    onSurface = Color(0xFFF3E7EB),
    surfaceVariant = Color(0xFF49343C),
    onSurfaceVariant = Color(0xFFE0C5CF),
    outline = Color(0xFFA77E8C),
    error = Color(0xFFFFB4AB),
)

@Composable
fun CalisBloomprintsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
