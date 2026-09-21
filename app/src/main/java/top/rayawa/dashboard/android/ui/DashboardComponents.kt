package top.rayawa.dashboard.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.text.DecimalFormat
import kotlin.math.max

private val imageCache = object : LruCache<String, Bitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

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
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
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
    }
    val sized = if (size != null) modifier.size(size) else modifier
    Box(
        modifier = sized.clip(RoundedCornerShape(if (size != null) size / 4 else 16.dp))
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
        } else if (url.isNotBlank()) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Text("APP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        OutlinedButton(onClick = retry) { Text("重试") }
    }
}

@Composable
fun BarChart(
    labels: List<String>,
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val maxValue = max(1L, values.maxOrNull() ?: 1L)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.zip(values).forEach { (label, value) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(formatCount(value), style = MaterialTheme.typography.labelMedium)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(
                        Modifier.fillMaxWidth((value.toFloat() / maxValue).coerceIn(0f, 1f))
                            .height(8.dp).background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        if (values.isEmpty()) return@Canvas
        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val minValue = values.minOrNull() ?: 0f
        val range = ((values.maxOrNull() ?: 1f) - minValue).takeIf { it > 0f } ?: 1f
        val stepX = if (values.size == 1) 0f else size.width / (values.size - 1)
        values.zipWithNext().forEachIndexed { index, pair ->
            val start = Offset(index * stepX, size.height - ((pair.first - minValue) / range * size.height))
            val end = Offset((index + 1) * stepX, size.height - ((pair.second - minValue) / range * size.height))
            drawLine(color, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        values.forEachIndexed { index, value ->
            drawCircle(color, radius = 5f, center = Offset(index * stepX, size.height - ((value - minValue) / range * size.height)))
        }
    }
}

@Composable
fun DonutChart(values: List<Long>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = values.sum().coerceAtLeast(1)
    Canvas(modifier = modifier.size(180.dp)) {
        var start = -90f
        values.forEachIndexed { index, value ->
            val sweep = value.toFloat() / total * 360f
            drawArc(
                color = colors[index % colors.size], startAngle = start, sweepAngle = sweep,
                useCenter = false, topLeft = Offset(12f, 12f),
                size = Size(size.width - 24f, size.height - 24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 34f, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}
