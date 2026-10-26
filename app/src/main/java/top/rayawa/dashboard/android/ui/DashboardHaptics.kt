package top.rayawa.dashboard.android.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/** Reads the setting at interaction time, including callbacks retained by pointerInput. */
class DashboardHaptics(
    private val isEnabled: () -> Boolean,
    private val emitFeedback: () -> Unit,
) {
    fun press() {
        if (isEnabled()) emitFeedback()
    }
}

val LocalDashboardHaptics = staticCompositionLocalOf {
    DashboardHaptics(isEnabled = { false }, emitFeedback = {})
}

@Composable
fun DashboardHapticsProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val nativeHaptics = LocalHapticFeedback.current
    val preferences = remember(context) { context.getSharedPreferences("dashboard_settings", 0) }
    val haptics = remember(preferences, view) {
        DashboardHaptics(
            isEnabled = { preferences.getBoolean("haptics", true) },
            emitFeedback = {
                // View feedback honors the device's touch feedback setting and needs no permission.
                // Some vendors do not implement CONTEXT_CLICK; VIRTUAL_KEY is the native fallback.
                if (!view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            },
        )
    }
    val gatedNativeHaptics = remember(preferences, nativeHaptics) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (preferences.getBoolean("haptics", true)) {
                    nativeHaptics.performHapticFeedback(hapticFeedbackType)
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalDashboardHaptics provides haptics,
        LocalHapticFeedback provides gatedNativeHaptics,
        content = content,
    )
}

@Composable
fun rememberHapticAction(action: () -> Unit): () -> Unit {
    val haptics = LocalDashboardHaptics.current
    val currentAction = rememberUpdatedState(action)
    return remember(haptics) {
        {
            haptics.press()
            currentAction.value()
        }
    }
}
