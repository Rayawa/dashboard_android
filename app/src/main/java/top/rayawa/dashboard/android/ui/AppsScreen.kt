package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.rayawa.dashboard.android.data.AppType
import top.rayawa.dashboard.android.data.AdvancedCondition
import top.rayawa.dashboard.android.data.AppsPage
import top.rayawa.dashboard.android.data.DashboardApi
import top.rayawa.dashboard.android.data.DownloadGrowth
import top.rayawa.dashboard.android.data.FilterField
import top.rayawa.dashboard.android.data.MarketApp
import top.rayawa.dashboard.android.data.RatingDistribution
import top.rayawa.dashboard.android.data.SdkDistribution
import top.rayawa.dashboard.android.data.SortField
import top.rayawa.dashboard.android.data.SearchOperator

private enum class AppsMode(val label: String) { OVERVIEW("数据概况"), LIST("应用列表") }

@Composable
fun AppsScreen(refreshKey: Int, openDetail: (MarketApp) -> Unit, modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(AppsMode.OVERVIEW) }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppsMode.entries.forEach { item ->
                FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item.label) })
            }
        }
        if (mode == AppsMode.OVERVIEW) MarketInsights(refreshKey, Modifier.weight(1f))
        else MarketList(refreshKey, openDetail, Modifier.weight(1f))
    }
}

@Composable
private fun MarketInsights(refreshKey: Int, modifier: Modifier = Modifier) {
    var rating by remember { mutableStateOf<RatingDistribution?>(null) }
    var minSdk by remember { mutableStateOf<List<SdkDistribution>>(emptyList()) }
    var targetSdk by remember { mutableStateOf<List<SdkDistribution>>(emptyList()) }
    var growth by remember { mutableStateOf<List<DownloadGrowth>>(emptyList()) }
    var period by remember { mutableIntStateOf(7) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey, period, retryKey) {
        loading = true
        error = null
        runCatching {
            coroutineScope {
                val ratingRequest = async { DashboardApi.ratingDistribution() }
                val minRequest = async { DashboardApi.sdkDistribution(false) }
                val targetRequest = async { DashboardApi.sdkDistribution(true) }
                val growthRequest = async { DashboardApi.downloadGrowth(period) }
                rating = ratingRequest.await()
                minSdk = minRequest.await()
                targetSdk = targetRequest.await()
                growth = growthRequest.await()
            }
        }.onFailure { error = it.message }
        loading = false
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("市场数据概况", style = MaterialTheme.typography.headlineSmall)
            Text("评分、SDK 与下载增长趋势", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (loading && rating == null) item { LoadingPane(text = "正在加载统计数据…") }
        error?.let { item { ErrorPane(it, retry = { retryKey++ }) } }
        rating?.let { data ->
            item {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Text("评分分布", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFF44336))
                        DonutChart(data.values, colors, Modifier.weight(1f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            data.values.forEachIndexed { index, value ->
                                Text("${5 - index} 星  ${formatCount(value)}", color = colors[index], style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        if (minSdk.isNotEmpty()) item { SdkCard("最低 SDK 分布", minSdk) }
        if (targetSdk.isNotEmpty()) item { SdkCard("目标 SDK 分布", targetSdk) }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("下载增长榜", style = MaterialTheme.typography.titleLarge)
                        Text("按快照周期对比下载量增量", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AssistChip(
                        onClick = { period = when (period) { 1 -> 7; 7 -> 30; else -> 1 } },
                        label = { Text("${period}天") },
                    )
                }
                Spacer(Modifier.height(10.dp))
                growth.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", modifier = Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(item.name, maxLines = 1)
                            Text("${item.priorDate.take(10)} → ${item.currentDate.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("+${formatCount(item.increment)}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun SdkCard(title: String, values: List<SdkDistribution>) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        BarChart(
            labels = values.take(10).map { "API ${it.sdk}" },
            values = values.take(10).map { it.count },
        )
    }
}

@Composable
private fun MarketList(refreshKey: Int, openDetail: (MarketApp) -> Unit, modifier: Modifier = Modifier) {
    var pageData by remember { mutableStateOf<AppsPage?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var pageSize by remember { mutableIntStateOf(20) }
    var appType by remember { mutableStateOf(AppType.ALL) }
    var filterField by remember { mutableStateOf(FilterField.NAME) }
    var filterText by remember { mutableStateOf("") }
    var appliedFilter by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(SortField.UPDATED) }
    var descending by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadToken by remember { mutableIntStateOf(0) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var advancedEnabled by remember { mutableStateOf(false) }
    var useAnd by remember { mutableStateOf(true) }
    var firstCondition by remember { mutableStateOf(AdvancedCondition(FilterField.NAME, SearchOperator.CONTAINS)) }
    var secondCondition by remember { mutableStateOf(AdvancedCondition(FilterField.DEVELOPER, SearchOperator.CONTAINS)) }

    LaunchedEffect(refreshKey, page, pageSize, appType, appliedFilter, filterField, sort, descending,
        loadToken, advancedEnabled, useAnd, firstCondition, secondCondition) {
        loading = true
        error = null
        runCatching {
            if (advancedEnabled) {
                DashboardApi.appsQuery(page, pageSize, sort, descending, useAnd, listOf(firstCondition, secondCondition))
            } else {
                DashboardApi.appsPage(page, pageSize, sort, descending, filterField, appliedFilter, type = appType)
            }
        }.onSuccess { pageData = it }.onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("筛选内容") },
                    placeholder = { Text("应用名称、包名、App ID 或开发者") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { appType = appType.next(); page = 1 }, label = { Text("类型：${appType.label}") })
                    AssistChip(onClick = { filterField = filterField.next() }, label = { Text("字段：${filterField.label}") })
                    AssistChip(onClick = { sort = sort.next(); page = 1 }, label = { Text("排序：${sort.label}") })
                    AssistChip(onClick = { descending = !descending; page = 1 }, label = { Text(if (descending) "降序" else "升序") })
                    AssistChip(onClick = { pageSize = when (pageSize) { 20 -> 50; 50 -> 100; else -> 20 }; page = 1 }, label = { Text("每页 $pageSize") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { appliedFilter = filterText.trim(); advancedEnabled = false; page = 1 }) { Text("筛选") }
                    OutlinedButton(onClick = { filterText = ""; appliedFilter = ""; appType = AppType.ALL; advancedEnabled = false; page = 1 }) { Text("清除") }
                }
                AssistChip(
                    onClick = { advancedExpanded = !advancedExpanded },
                    label = { Text(if (advancedExpanded) "收起高级组合搜索" else "高级组合搜索") },
                )
                if (advancedExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = useAnd, onClick = { useAnd = true }, label = { Text("同时满足 AND") })
                        FilterChip(selected = !useAnd, onClick = { useAnd = false }, label = { Text("任一满足 OR") })
                    }
                    AdvancedConditionEditor(firstCondition) { firstCondition = it }
                    AdvancedConditionEditor(secondCondition) { secondCondition = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { advancedEnabled = true; appliedFilter = ""; appType = AppType.ALL; page = 1 }) { Text("应用高级条件") }
                        OutlinedButton(onClick = { advancedEnabled = false; page = 1 }) { Text("停用") }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("共 ${formatCount(pageData?.totalCount ?: 0)} 条应用数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loading) Text("加载中…", color = MaterialTheme.colorScheme.primary)
            }
        }
        error?.let { item { ErrorPane(it, retry = { loadToken++ }) } }
        items(pageData?.apps.orEmpty(), key = { it.appId }) { app -> AppRow(app, { openDetail(app) }) }
        item {
            val totalPages = pageData?.totalPages ?: 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { page-- }, enabled = page > 1 && !loading) { Text("上一页") }
                Text("第 $page / $totalPages 页")
                Button(onClick = { page++ }, enabled = page < totalPages && !loading) { Text("下一页") }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

private fun AppType.next() = AppType.entries[(ordinal + 1) % AppType.entries.size]
private fun FilterField.next() = FilterField.entries[(ordinal + 1) % FilterField.entries.size]
private fun SortField.next() = SortField.entries[(ordinal + 1) % SortField.entries.size]
private fun SearchOperator.next() = SearchOperator.entries[(ordinal + 1) % SearchOperator.entries.size]

@Composable
private fun AdvancedConditionEditor(condition: AdvancedCondition, onChange: (AdvancedCondition) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onChange(condition.copy(field = condition.field.next())) },
                label = { Text(condition.field.label) },
            )
            AssistChip(
                onClick = { onChange(condition.copy(operator = condition.operator.next())) },
                label = { Text(condition.operator.label) },
            )
        }
        if (condition.operator != SearchOperator.NOT_EMPTY) {
            OutlinedTextField(
                value = condition.value,
                onValueChange = { onChange(condition.copy(value = it)) },
                label = { Text("条件值") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
