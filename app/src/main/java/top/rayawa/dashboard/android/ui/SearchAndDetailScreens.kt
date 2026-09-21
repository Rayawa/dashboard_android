package top.rayawa.dashboard.android.ui

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import top.rayawa.dashboard.android.data.AppDetail
import top.rayawa.dashboard.android.data.DashboardApi
import top.rayawa.dashboard.android.data.MarketApp
import top.rayawa.dashboard.android.data.SortField

@Composable
fun SearchScreen(openDetail: (MarketApp) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dashboard_settings", 0) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MarketApp>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<MarketApp>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(prefs.getString("search_history", "").orEmpty().split('\n').filter(String::isNotBlank)) }
    var showSubmit by remember { mutableStateOf(false) }

    fun search(value: String = query) {
        val text = value.trim()
        if (text.isBlank()) return
        query = text
        loading = true
        error = null
        scope.launch {
            runCatching { DashboardApi.search(text) }
                .onSuccess {
                    results = it
                    history = (listOf(text) + history.filterNot { old -> old == text }).take(10)
                    prefs.edit { putString("search_history", history.joinToString("\n")) }
                    if (it.isEmpty()) error = "没有找到匹配的应用"
                }
                .onFailure { error = it.message ?: "搜索失败" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        recommendations = runCatching { DashboardApi.appsPage(pageSize = 8, sort = SortField.LISTED).apps }.getOrDefault(emptyList())
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索应用") },
                placeholder = { Text("名称、包名、App ID 或应用市场链接") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { search() }, enabled = !loading && query.isNotBlank()) { Text(if (loading) "搜索中…" else "搜索") }
                OutlinedButton(onClick = { showSubmit = true }) { Text("提交收录") }
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (results.isNotEmpty()) {
            item { Text("搜索结果（${results.size}）", style = MaterialTheme.typography.titleLarge) }
            items(results, key = { it.appId }) { AppRow(it, { openDetail(it) }) }
        } else if (!loading) {
            if (history.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索历史", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            history = emptyList()
                            prefs.edit { remove("search_history") }
                        }) { Text("删除全部") }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.forEach { item -> AssistChip(onClick = { search(item) }, label = { Text(item) }) }
                    }
                }
            }
            if (recommendations.isNotEmpty()) {
                item { Text("搜索推荐", style = MaterialTheme.typography.titleLarge) }
                items(recommendations, key = { it.appId }) { AppRow(it, { openDetail(it) }) }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }

    if (showSubmit) SubmitDialog(
        initial = query,
        onDismiss = { showSubmit = false },
        onSubmitted = { showSubmit = false; openDetail(it) },
    )
}

@Composable
private fun SubmitDialog(initial: String, onDismiss: () -> Unit, onSubmitted: (MarketApp) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(initial) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交应用收录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("输入 App ID、包名或华为应用市场详情链接。提交后服务端会获取并更新应用信息。")
                OutlinedTextField(value, { value = it }, label = { Text("应用标识") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = value.isNotBlank() && !loading,
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val user = context.getSharedPreferences("dashboard_settings", 0).getString("username", "").orEmpty()
                        runCatching { DashboardApi.submitApp(value, user) }
                            .onSuccess(onSubmitted)
                            .onFailure { error = it.message ?: "提交失败" }
                        loading = false
                    }
                },
            ) { Text(if (loading) "提交中…" else "提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun AppDetailScreen(
    appId: String?,
    packageName: String?,
    preview: MarketApp?,
    refreshKey: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var detail by remember(appId, packageName) { mutableStateOf(preview?.let { AppDetail(it) }) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(appId, packageName, refreshKey, retryKey) {
        loading = true
        error = null
        runCatching { DashboardApi.appDetail(appId, packageName) }
            .onSuccess { detail = it }
            .onFailure { error = it.message ?: "详情加载失败" }
        loading = false
    }

    val data = detail
    if (data == null && loading) {
        LoadingPane(modifier = modifier, text = "正在加载应用详情…")
        return
    }
    if (data == null) {
        ErrorPane(error ?: "应用详情不可用", retry = { retryKey++ }, modifier)
        return
    }
    val app = data.app
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let { item { Text("刷新失败，当前显示已有数据：$it", color = MaterialTheme.colorScheme.error) } }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RemoteImage(app.iconUrl, "${app.name}图标", size = 100.dp)
                Text(app.name, style = MaterialTheme.typography.headlineMedium)
                Text(app.developerName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openUrl(context, "https://appgallery.huawei.com/app/detail?id=${app.appId}") }) { Text("应用市场") }
                    if (app.privacyUrl.isNotBlank()) OutlinedButton(onClick = { openUrl(context, app.privacyUrl) }) { Text("隐私政策") }
                    OutlinedButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "${app.name}\n${DashboardApi.PUBLIC_SITE_URL}dashboard?app_id=${app.appId}")
                        context.startActivity(Intent.createChooser(share, "分享应用"))
                    }) { Text("分享") }
                }
            }
        }
        item { AppInformation(app) }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Text("评分分布", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                BarChart((5 downTo 1).map { "$it 星" }, app.starCounts.reversed(), color = MaterialTheme.colorScheme.tertiary)
                Text("综合评分 ${app.fullAverageRating ?: app.averageRating ?: 0.0} · 共 ${formatCount(app.totalRatingCount)} 条", modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (app.description.isNotBlank() || app.newFeatures.isNotBlank()) {
            item {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Text("应用介绍", style = MaterialTheme.typography.titleLarge)
                    Text(app.description.ifBlank { "暂无介绍" }, modifier = Modifier.padding(top = 8.dp))
                    if (app.newFeatures.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text("新版特性", style = MaterialTheme.typography.titleMedium)
                        Text(app.newFeatures, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        if (app.screenshots.isNotEmpty()) {
            item {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Text("应用截图", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(app.screenshots, key = { it.url }) { shot ->
                            RemoteImage(
                                shot.url, "${app.name}应用截图",
                                Modifier.width(if (shot.rotated == 1) 360.dp else 210.dp).height(360.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }
        }
        if (data.metrics.isNotEmpty()) {
            item {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Text("下载量趋势", style = MaterialTheme.typography.titleLarge)
                    Text("${compactDate(data.metrics.first().reportDate)} — ${compactDate(data.metrics.last().reportDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LineChart(data.metrics.map { it.downloadCount.toFloat() }, Modifier.padding(top = 12.dp))
                }
            }
        }
        if (data.ratings.isNotEmpty()) {
            item {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Text("评分趋势", style = MaterialTheme.typography.titleLarge)
                    LineChart(data.ratings.map { it.average.toFloat() }, Modifier.padding(top = 12.dp), MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun AppInformation(app: MarketApp) {
    val fields = listOf(
        "App ID" to app.appId,
        "包名" to app.packageName,
        "版本" to "${app.version} (${app.versionCode})",
        "分类" to listOf(app.kindTypeName, app.kindName, app.tagName).filter(String::isNotBlank).joinToString(" · "),
        "大小" to formatBytes(app.sizeBytes),
        "下载量" to formatCount(app.downloadCount),
        "SDK" to "最低 ${app.minSdk} · 目标 ${app.targetSdk} · 编译 ${app.compileSdk}",
        "上架时间" to compactDate(app.listedAt),
        "更新时间" to compactDate(app.updatedAt),
        "发行地区" to app.releaseCountries.joinToString().ifBlank { "-" },
    )
    DashboardCard(Modifier.fillMaxWidth()) {
        fields.forEachIndexed { index, field ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(field.first, Modifier.width(82.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(field.second.ifBlank { "-" }, Modifier.weight(1f))
            }
            if (index < fields.lastIndex) HorizontalDivider()
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
