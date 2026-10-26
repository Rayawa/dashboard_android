package top.rayawa.dashboard.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DashboardAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkDashboardPalette else LightDashboardPalette
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = palette.blue, onPrimary = if (darkTheme) palette.surfaceAlt else Color.White,
        primaryContainer = palette.blue.copy(alpha = 0.12f), onPrimaryContainer = palette.blue,
        secondary = palette.cyan, onSecondary = if (darkTheme) palette.surfaceAlt else Color.White,
        secondaryContainer = palette.cyan.copy(alpha = 0.12f), onSecondaryContainer = palette.cyan,
        tertiary = palette.green, onTertiary = if (darkTheme) palette.surfaceAlt else Color.White,
        tertiaryContainer = palette.successBackground, onTertiaryContainer = palette.green,
        background = palette.background, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.surfaceAlt, onSurfaceVariant = palette.secondaryText,
        surfaceTint = Color.Transparent, surfaceDim = palette.surfaceAlt, surfaceBright = palette.surface,
        surfaceContainerLowest = palette.surface, surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surfaceAlt, surfaceContainerHigh = palette.track,
        surfaceContainerHighest = palette.track,
        outline = palette.secondaryText, outlineVariant = palette.border,
        error = palette.red, onError = if (darkTheme) palette.surfaceAlt else Color.White,
        errorContainer = palette.red.copy(alpha = 0.12f), onErrorContainer = palette.red,
        inverseSurface = if (darkTheme) LightDashboardPalette.text else DarkDashboardPalette.text,
        inverseOnSurface = if (darkTheme) LightDashboardPalette.surface else DarkDashboardPalette.surface,
        inversePrimary = if (darkTheme) LightDashboardPalette.blue else DarkDashboardPalette.blue,
    )
    CompositionLocalProvider(LocalDashboardPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}
