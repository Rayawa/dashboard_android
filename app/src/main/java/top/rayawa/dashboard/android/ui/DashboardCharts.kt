package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import top.rayawa.dashboard.android.ui.DashboardTextButton as TextButton
import top.rayawa.dashboard.android.ui.theme.LocalDashboardPalette

@Composable
fun DistributionChart(entries: List<DistributionEntry>, modifier: Modifier = Modifier) {
    val palette = LocalDashboardPalette.current
    val haptics = LocalDashboardHaptics.current
    val total = entries.sumOf { it.value.coerceAtLeast(0) }
    val fontScale = LocalDensity.current.fontScale
    var selectedLabel by remember(entries) { androidx.compose.runtime.mutableStateOf(entries.firstOrNull()?.label) }
    val selected = entries.indexOfFirst { it.label == selectedLabel }
    fun entryColor(index: Int): Color = entries[index].colorIndex.let {
        if (it < 0) palette.secondaryText else palette.chartColors[it % palette.chartColors.size]
    }
    if (total == 0L) {
        Text("暂无数据", modifier.padding(vertical = 30.dp), color = palette.secondaryText)
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 500.dp
        val legendColumns = if (!horizontal && maxWidth >= 280.dp && fontScale <= 1.2f) 2 else 1
        @Composable
        fun Pie(modifier: Modifier) {
            Canvas(modifier.aspectRatio(1f).semantics {
                contentDescription = "分布饼图，共${formatCount(total)}，点击扇区查看数量和占比"
            }.pointerInput(entries) {
                detectTapGestures { point ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (hypot(point.x - center.x, point.y - center.y) <= minOf(size.width, size.height) / 2f - 10.dp.toPx()) {
                        val angle = ((atan2(point.y - center.y, point.x - center.x) * 180 / PI + 450) % 360).toFloat()
                        var end = 0f
                        val index = entries.indexOfFirst {
                            end += it.value.coerceAtLeast(0).toFloat() / total * 360f
                            angle < end
                        }
                        if (index >= 0 && selectedLabel != entries[index].label) {
                            haptics.press()
                            selectedLabel = entries[index].label
                        }
                    }
                }
            }) {
                val inset = 10.dp.toPx()
                val bounds = Size(size.width - inset * 2, size.height - inset * 2)
                var start = -90f
                entries.forEachIndexed { index, entry ->
                    val sweep = (entry.value.coerceAtLeast(0).toDouble() / total * 360).toFloat()
                    if (sweep > 0f) {
                        drawArc(entryColor(index), start, sweep, true, Offset(inset, inset), bounds)
                        drawArc(palette.surface, start, sweep, true, Offset(inset, inset), bounds,
                            style = Stroke(1.5.dp.toPx()))
                        if (index == selected) drawArc(entryColor(index), start, sweep, false,
                            Offset(inset - 4.dp.toPx(), inset - 4.dp.toPx()),
                            Size(bounds.width + 8.dp.toPx(), bounds.height + 8.dp.toPx()),
                            style = Stroke(2.dp.toPx()))
                    }
                    start += sweep
                }
            }
        }
        @Composable
        fun Legend(modifier: Modifier) {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                entries.withIndex().chunked(legendColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (index, entry) ->
                            val color = entryColor(index)
                            Surface(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .selectable(selected == index, role = Role.RadioButton, onClick = {
                                        if (selectedLabel != entry.label) {
                                            haptics.press()
                                            selectedLabel = entry.label
                                        }
                                    }),
                                color = if (selected == index) palette.surfaceAlt else palette.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected == index) color.copy(alpha = 0.5f) else palette.border),
                            ) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.label, style = MaterialTheme.typography.labelLarge)
                                        Text("${formatCount(entry.value)} · ${chartPercent(entry.value, total)}",
                                            style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                                    }
                                }
                            }
                        }
                        if (row.size < legendColumns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Pie(Modifier.weight(1f))
                Legend(Modifier.weight(1f))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pie(Modifier.width(200.dp))
                Legend(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun BarChart(
    labels: List<String>, values: List<Long>, modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val maximum = (values.maxOrNull() ?: 0L).coerceAtLeast(1)
    val total = values.sumOf { it.coerceAtLeast(0) }
    val palette = LocalDashboardPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.zip(values).forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, Modifier.width(34.dp), style = MaterialTheme.typography.labelMedium, color = palette.secondaryText)
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.track)) {
                    Box(Modifier.fillMaxWidth((value.toDouble() / maximum).toFloat().coerceIn(0f, 1f))
                        .height(8.dp).background(color))
                }
                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.End) {
                    Text(formatCount(value), style = MaterialTheme.typography.labelMedium)
                    Text(chartPercent(value, total), style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                }
            }
        }
    }
}

@Composable
fun LineChart(
    values: List<Double>, modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    labels: List<String> = emptyList(), unit: String = "次", fixedMaximum: Double? = null,
) {
    val palette = LocalDashboardPalette.current
    val haptics = LocalDashboardHaptics.current
    val measurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = palette.secondaryText, fontSize = 10.sp)
    val safeValues = remember(values) { values.map { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 } }
    var selected by remember(values) { mutableIntStateOf(values.lastIndex.coerceAtLeast(0)) }
    val maximum = chartMaximum(safeValues, fixedMaximum)
    fun formatValue(value: Double): String = if (unit == "分") "%.1f".format(java.util.Locale.ROOT, value) else formatCount(value.toLong())
    if (values.isEmpty()) {
        Text("暂无趋势数据", modifier.padding(vertical = 30.dp), color = palette.secondaryText)
        return
    }
    val tickLayouts = (0..4).map { measurer.measure(formatValue(maximum * (4 - it) / 4), textStyle) }
    val axisWidth = tickLayouts.maxOf { it.size.width }.toFloat()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(230.dp).semantics {
            contentDescription = "${labels.getOrNull(selected)?.take(10).orEmpty()}，${formatValue(safeValues[selected])}$unit"
        }.pointerInput(values, axisWidth) {
            detectTapGestures { point ->
                val left = axisWidth + 10.dp.toPx()
                val width = (size.width - left - 10.dp.toPx()).coerceAtLeast(1f)
                val next = (((point.x - left) / width).coerceIn(0f, 1f) * values.lastIndex).roundToInt()
                if (selected != next) {
                    haptics.press()
                    selected = next
                }
            }
        }) {
            val left = axisWidth + 10.dp.toPx()
            val right = size.width - 10.dp.toPx()
            val top = 16.dp.toPx()
            val bottom = size.height - 28.dp.toPx()
            val width = (right - left).coerceAtLeast(1f)
            val height = bottom - top
            val dashed = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx()))
            for (row in 0..4) {
                val y = top + height * row / 4
                drawLine(palette.grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx(), pathEffect = dashed)
                val tick = tickLayouts[row]
                drawText(tick, topLeft = Offset(left - tick.size.width - 8.dp.toPx(), y - tick.size.height / 2))
            }
            val points = safeValues.mapIndexed { index, value ->
                Offset(if (values.size == 1) left + width / 2 else left + width * index / values.lastIndex,
                    bottom - (value / maximum).toFloat().coerceIn(0f, 1f) * height)
            }
            // Horizontal controls stay between adjacent points, so the curve never overshoots counts or ratings.
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.zipWithNext().forEach { (a, b) ->
                    val middle = (a.x + b.x) / 2
                    cubicTo(middle, a.y, middle, b.y, b.x, b.y)
                }
            }
            if (points.size > 1) {
                val fill = Path().apply {
                    addPath(path); lineTo(points.last().x, bottom); lineTo(points.first().x, bottom); close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), top, bottom))
                drawPath(path, color, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
            }
            val focus = points[selected]
            drawLine(palette.secondaryText.copy(alpha = 0.5f), Offset(focus.x, top), Offset(focus.x, bottom), pathEffect = dashed)
            points.forEachIndexed { index, point ->
                if (points.size <= 40 || index == selected) drawCircle(color, 2.5.dp.toPx(), point)
            }
            drawCircle(color.copy(alpha = 0.2f), 8.dp.toPx(), focus)
            drawCircle(palette.surface, 5.dp.toPx(), focus)
            drawCircle(color, 3.5.dp.toPx(), focus)
            val indexes = if (values.size > 2 && width >= 220.dp.toPx()) listOf(0, values.lastIndex / 2, values.lastIndex) else listOf(0, values.lastIndex).distinct()
            indexes.forEach { index ->
                val date = labels.getOrNull(index)?.take(10)?.takeLast(5) ?: "${index + 1}"
                val layout = measurer.measure(date, textStyle)
                val x = (points[index].x - layout.size.width / 2).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(x, bottom + 10.dp.toPx()))
            }
        }
        Surface(color = palette.surfaceAlt, shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(labels.getOrNull(selected)?.take(10) ?: "第 ${selected + 1} 个数据点", style = MaterialTheme.typography.labelMedium, color = palette.secondaryText)
                Text("${formatValue(safeValues[selected])} $unit", style = MaterialTheme.typography.titleMedium, color = color)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { selected-- }, enabled = selected > 0) { Text("上一条") }
            Text("点击图表查看数据", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
            TextButton(onClick = { selected++ }, enabled = selected < values.lastIndex) { Text("下一条") }
        }
    }
}
