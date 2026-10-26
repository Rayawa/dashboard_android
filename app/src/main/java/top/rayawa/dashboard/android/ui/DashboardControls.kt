package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Route activation through one preference-aware feedback path; disabled controls retain native behavior.
@Composable
fun DashboardButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick = rememberHapticAction(onClick), modifier = modifier, enabled = enabled, content = content,
)

@Composable
fun DashboardOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick = rememberHapticAction(onClick), modifier = modifier, enabled = enabled, content = content,
)

@Composable
fun DashboardTextButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.TextButton(
    onClick = rememberHapticAction(onClick), modifier = modifier, enabled = enabled,
    contentPadding = contentPadding, content = content,
)

@Composable
fun DashboardIconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit,
) = androidx.compose.material3.IconButton(
    onClick = rememberHapticAction(onClick), modifier = modifier, enabled = enabled, content = content,
)

@Composable
fun DashboardAssistChip(
    onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) = androidx.compose.material3.AssistChip(
    onClick = rememberHapticAction(onClick), label = label, modifier = modifier, enabled = enabled,
)

@Composable
fun DashboardFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) = androidx.compose.material3.FilterChip(
    selected = selected, onClick = rememberHapticAction(onClick), label = label,
    modifier = modifier, enabled = enabled,
)
