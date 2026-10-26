package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.rayawa.dashboard.android.R
import top.rayawa.dashboard.android.data.DailyMaxDownload
import top.rayawa.dashboard.android.data.DashboardApi
import top.rayawa.dashboard.android.data.DeveloperRanking
import top.rayawa.dashboard.android.data.MarketApp
import top.rayawa.dashboard.android.data.MarketInfo
import top.rayawa.dashboard.android.data.SortField
import top.rayawa.dashboard.android.ui.theme.LocalDashboardPalette

@Composable
fun HomeScreen(
    refreshKey: Int,
    openDetail: (MarketApp) -> Unit,
    openDetailById: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val username = remember { context.getSharedPreferences("dashboard_settings", 0).getString("username", "").orEmpty() }
    var market by remember { mutableStateOf<MarketInfo?>(null) }
    var recent by remember { mutableStateOf<List<MarketApp>>(emptyList()) }
    var daily by remember { mutableStateOf<List<DailyMaxDownload>>(emptyList()) }
    var developers by remember { mutableStateOf<List<DeveloperRanking>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey, retryKey) {
        loading = true
        error = null
        runCatching {
            coroutineScope {
                val marketRequest = async { DashboardApi.marketInfo() }
                val recentRequest = async { DashboardApi.appsPage(pageSize = 12, sort = SortField.LISTED).apps }
                val dailyRequest = async { DashboardApi.dailyMaxDownloads() }
                val developerRequest = async { DashboardApi.developerRanking() }
                market = marketRequest.await()
                recent = runCatching { recentRequest.await() }.getOrDefault(emptyList())
                daily = runCatching { dailyRequest.await() }.getOrDefault(emptyList())
                developers = runCatching { developerRequest.await() }.getOrDefault(emptyList())
            }
        }.onFailure { error = it.message ?: "网络连接失败" }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = DashboardPagePadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            val greeting = when (LocalTime.now().hour) {
                in 6..11 -> "上午好"
                in 12..17 -> "下午好"
                else -> "晚上好"
            }
            Text(
                if (username.isBlank()) greeting else "$greeting，$username",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "欢迎来到 Dashboard 应用看板，探索 HarmonyOS 应用生态的最新动态。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (loading && market == null) item { LoadingPane(text = "正在获取最新市场状态…") }
        error?.let { message -> if (market == null) item { ErrorPane(message, retry = { retryKey++ }) } }
        market?.let { info ->
            item { MarketOverview(info) }
            item { SyncCard(info) }
        }
        if (daily.isNotEmpty()) {
            item { Text("每日最高下载量应用", style = MaterialTheme.typography.titleLarge) }
            items(daily, key = { "${it.appId}-${it.reportDate}" }) { app ->
                RankingRow(
                    title = app.appName,
                    subtitle = app.reportDate,
                    value = formatCount(app.downloadCount),
                    onClick = { openDetailById(app.appId, app.packageName) },
                )
            }
        }
        if (developers.isNotEmpty()) {
            item { Text("开发者应用数量排行", style = MaterialTheme.typography.titleLarge) }
            items(developers, key = { it.id }) { developer ->
                RankingRow(developer.name, developer.id, "${formatCount(developer.appCount)} 款")
            }
        }
        if (recent.isNotEmpty()) {
            item { Text("最近收录", style = MaterialTheme.typography.titleLarge) }
            items(recent, key = { it.appId }) { app -> AppRow(app = app, onClick = { openDetail(app) }) }
        }
    }
}

@Composable
internal fun MarketOverview(info: MarketInfo) {
    val palette = LocalDashboardPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardCard(Modifier.fillMaxWidth()) {
            Text("应用总数", style = MaterialTheme.typography.titleMedium)
            Text(formatCount(info.counts.apps + info.counts.atomicServices), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("应用", info.counts.apps, Modifier.weight(1f))
                Stat("元服务", info.counts.atomicServices, Modifier.weight(1f), palette.cyan)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardCard(Modifier.weight(1f)) { Stat("开发者", info.developerCount, color = palette.purple) }
            DashboardCard(Modifier.weight(1f)) { Stat("专题", info.substanceCount, color = palette.orange) }
        }
        DashboardCard(Modifier.fillMaxWidth()) {
            Stat("最高装机量", info.counts.maxDownloadCount)
        }
    }
}

@Composable
private fun Stat(label: String, value: Long, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Text(formatCount(value), style = MaterialTheme.typography.headlineSmall, color = color)
    }
}

@Composable
internal fun SyncCard(info: MarketInfo) {
    val status = info.syncStatus
    val palette = LocalDashboardPalette.current
    DashboardCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("同步信息", style = MaterialTheme.typography.titleLarge)
            Text(
                if (status.isSyncingAll) "全量同步中" else "服务正常",
                style = MaterialTheme.typography.labelMedium,
                color = if (status.isSyncingAll) palette.orange else palette.green,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (status.isSyncingAll) palette.warningBackground else palette.successBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (status.isSyncingAll && status.progress.size >= 2 && status.progress[1] > 0) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { (status.progress[0].toFloat() / status.progress[1]).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = palette.blue, trackColor = palette.track, strokeCap = StrokeCap.Round,
                    gapSize = 0.dp, drawStopIndicator = {},
                )
                Text("进度 ${formatCount(status.progress[0])} / ${formatCount(status.progress[1])}",
                    style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            }
        }
        Spacer(Modifier.height(12.dp))
        val fields = listOf(
            "已处理" to formatCount(status.totalProcessed), "新增" to formatCount(status.totalInserted),
            "跳过" to formatCount(status.totalSkipped), "失败" to formatCount(status.totalFailed),
            "已用时" to (status.elapsed?.let { formatDuration(it.seconds) } ?: "—"),
            "预计总耗时" to (status.estimated?.let { formatDuration(it.seconds) } ?: "—"),
        )
        fields.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label, value) ->
                    Text("$label：$value", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = if (label == "失败" && status.totalFailed > 0) palette.red else palette.secondaryText)
                }
            }
        }
        status.nextSync?.let {
            Text("下次同步约 ${formatDuration(it.seconds)} 后", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Text("最后更新：${compactDate(info.timestamp)}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
        Text("后端版本：${info.crateVersion}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
    }
}

@Composable
private fun RankingRow(title: String, subtitle: String, value: String, onClick: (() -> Unit)? = null) {
    DashboardCard(
        Modifier.fillMaxWidth(), onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Text(value, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AppRow(app: MarketApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(app.iconUrl, "${app.name}图标", size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (app.isAtomicService) {
                        Spacer(Modifier.width(6.dp))
                        Text("元服务", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(app.developerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${app.kindName.ifBlank { app.kindTypeName }} · ★ ${app.fullAverageRating ?: app.averageRating ?: 0.0} · ${formatBytes(app.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text("收录于 ${compactDate(app.listedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            ResourceIcon(
                R.drawable.harmony_right,
                contentDescription = "打开${app.name}详情",
                modifier = Modifier.width(20.dp).height(20.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
