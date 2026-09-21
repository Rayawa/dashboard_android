package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.rayawa.dashboard.android.data.DailyMaxDownload
import top.rayawa.dashboard.android.data.DashboardApi
import top.rayawa.dashboard.android.data.DeveloperRanking
import top.rayawa.dashboard.android.data.MarketApp
import top.rayawa.dashboard.android.data.MarketInfo
import top.rayawa.dashboard.android.data.SortField
import top.rayawa.dashboard.android.R
import java.time.LocalTime

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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun MarketOverview(info: MarketInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardCard(Modifier.fillMaxWidth()) {
            Text("应用总数", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatCount(info.counts.apps + info.counts.atomicServices), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("应用", info.counts.apps, Modifier.weight(1f))
                Stat("元服务", info.counts.atomicServices, Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardCard(Modifier.weight(1f)) { Stat("开发者", info.developerCount) }
            DashboardCard(Modifier.weight(1f)) { Stat("专题", info.substanceCount) }
        }
        DashboardCard(Modifier.fillMaxWidth()) {
            Stat("最高装机量", info.counts.maxDownloadCount)
        }
    }
}

@Composable
private fun Stat(label: String, value: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Text(formatCount(value), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SyncCard(info: MarketInfo) {
    val status = info.syncStatus
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("同步信息", style = MaterialTheme.typography.titleMedium)
            Text(
                if (status.isSyncingAll) "全量同步中" else "服务正常",
                color = if (status.isSyncingAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
        }
        if (status.progress.size >= 2 && status.progress[1] > 0) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progress[0].toFloat() / status.progress[1] },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("进度 ${formatCount(status.progress[0])} / ${formatCount(status.progress[1])}", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text("已处理 ${formatCount(status.totalProcessed)} · 新增 ${formatCount(status.totalInserted)} · 跳过 ${formatCount(status.totalSkipped)}")
        Text("失败 ${formatCount(status.totalFailed)}", color = if (status.totalFailed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        status.elapsed?.let { Text("已用时 ${formatDuration(it.seconds)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        status.nextSync?.let { Text("下次同步约 ${formatDuration(it.seconds)} 后", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("最后更新：${compactDate(info.timestamp)} · 后端 ${info.crateVersion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RankingRow(title: String, subtitle: String, value: String, onClick: (() -> Unit)? = null) {
    DashboardCard(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            Text(value, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AppRow(app: MarketApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(app.iconUrl, "${app.name}图标", size = 58.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f, false))
                    if (app.isAtomicService) {
                        Spacer(Modifier.width(6.dp))
                        Text("元服务", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(app.developerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
