package top.rayawa.dashboard.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Matches dashboard-HarmonyOS/entry/src/main/resources/{base,dark}/element/color.json. */
@Immutable
data class DashboardPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val navigation: Color,
    val text: Color,
    val secondaryText: Color,
    val border: Color,
    val grid: Color,
    val track: Color,
    val blue: Color,
    val cyan: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val purple: Color,
    val successBackground: Color,
    val warningBackground: Color,
    val extraSeries: List<Color>,
) {
    val chartColors: List<Color> get() = listOf(blue, cyan, green, orange, red, purple) + extraSeries
}

val LightDashboardPalette = DashboardPalette(
    background = Color(0xFFF6F9FF), surface = Color.White, surfaceAlt = Color(0xFFF0F4FA),
    navigation = Color(0xFFEEF4FE), text = Color(0xFF172033), secondaryText = Color(0xFF5F6B7A),
    border = Color(0xFFD8E4F2), grid = Color(0xFFD8E0EB), track = Color(0xFFEAF0F8),
    blue = Color(0xFF1E6FCD), cyan = Color(0xFF007D8A), green = Color(0xFF00875A),
    orange = Color(0xFFB54708), red = Color(0xFFCF2634), purple = Color(0xFF7048E8),
    successBackground = Color(0xFFE8F7EF), warningBackground = Color(0xFFFFF4DE),
    extraSeries = listOf(0xFFD43C9B, 0xFF5C6BC0, 0xFF8A5A00, 0xFF00838F, 0xFF7B1FA2,
        0xFF2E7D32, 0xFFC62828, 0xFFEF6C00, 0xFF1565C0, 0xFF6D4C41).map { Color(it) },
)

val DarkDashboardPalette = DashboardPalette(
    background = Color(0xFF1A2B3C), surface = Color(0xFF182231), surfaceAlt = Color(0xFF101927),
    navigation = Color(0xFF0B131F), text = Color(0xFFF1F4F8), secondaryText = Color(0xFFA8B4C3),
    border = Color(0xFF273546), grid = Color(0xFF344358), track = Color(0xFF233046),
    blue = Color(0xFF4A9CE2), cyan = Color(0xFF55C7D3), green = Color(0xFF52C999),
    orange = Color(0xFFFFB75E), red = Color(0xFFFF8792), purple = Color(0xFFB69AFF),
    successBackground = Color(0xFF15392D), warningBackground = Color(0xFF3E3017),
    extraSeries = listOf(0xFFFF77C8, 0xFF9FA8FF, 0xFFE0A45B, 0xFF62D7E2, 0xFFD29BFF,
        0xFF7DDD83, 0xFFFF8A80, 0xFFFFAB66, 0xFF78AEFF, 0xFFC7A29A).map { Color(it) },
)

val LocalDashboardPalette = staticCompositionLocalOf { LightDashboardPalette }
