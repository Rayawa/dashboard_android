package top.rayawa.dashboard.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URI
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.rayawa.dashboard.android.ui.DashboardOutlinedButton as OutlinedButton
import top.rayawa.dashboard.android.ui.theme.LocalDashboardPalette

private val imageCache = object : LruCache<String, Bitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

internal val DashboardPagePadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 24.dp)

fun formatCount(value: Long): String = when {
    value >= 100_000_000 -> DecimalFormat("0.##亿").format(value / 100_000_000.0)
    value >= 10_000 -> DecimalFormat("0.##万").format(value / 10_000.0)
    else -> DecimalFormat("#,###").format(value)
}

fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024 * 1024 -> DecimalFormat("0.## GB").format(value / (1024.0 * 1024 * 1024))
    value >= 1024L * 1024 -> DecimalFormat("0.## MB").format(value / (1024.0 * 1024))
    value >= 1024 -> DecimalFormat("0.## KB").format(value / 1024.0)
    else -> "$value B"
}

fun compactDate(value: String): String = value.replace('T', ' ').take(16).ifBlank { "-" }

fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = safe % 3600 / 60
    val secs = safe % 60
    return buildString {
        if (hours > 0) append("${hours}小时")
        if (minutes > 0 || hours > 0) append("${minutes}分")
        append("${secs}秒")
    }
}

@Composable
fun ResourceIcon(
    @DrawableRes drawable: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = tint?.let(ColorFilter::tint),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val clickModifier = if (onClick != null) {
        Modifier.clip(shape).clickable(role = Role.Button, onClick = rememberHapticAction(onClick))
    } else Modifier
    Card(
        modifier = modifier.then(clickModifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape,
        border = BorderStroke(1.dp, LocalDashboardPalette.current.border),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var bitmap by remember(url) { mutableStateOf(imageCache.get(url)) }
    var loading by remember(url) { mutableStateOf(url.isNotBlank() && bitmap == null) }
    LaunchedEffect(url) {
        if (url.isBlank() || bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", "Dashboard-Android/1.0")
                connection.inputStream.use(BitmapFactory::decodeStream).also { connection.disconnect() }
            }.getOrNull()?.also { imageCache.put(url, it) }
        }
        loading = false
    }
    val sized = if (size != null) modifier.size(size) else modifier
    Box(
        modifier = sized.clip(RoundedCornerShape(if (size != null) {
            if (size >= 80.dp) size * 0.22f else size / 4
        } else 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val value = bitmap
        if (value != null) {
            Image(
                bitmap = value.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        } else if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            ResourceIcon(
                top.rayawa.dashboard.android.R.drawable.harmony_start_icon,
                contentDescription, Modifier.matchParentSize(), tint = null,
            )
        }
    }
}

@Composable
fun LoadingPane(modifier: Modifier = Modifier, text: String = "正在加载…") {
    Column(
        modifier = modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorPane(message: String, retry: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth()) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(6.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = retry) {
            ResourceIcon(
                top.rayawa.dashboard.android.R.drawable.harmony_refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text("重试")
        }
    }
}
