package top.rayawa.dashboard.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.rayawa.dashboard.android.R
import top.rayawa.dashboard.android.data.MarketCounts
import top.rayawa.dashboard.android.data.MarketInfo
import top.rayawa.dashboard.android.data.SyncStatus
import top.rayawa.dashboard.android.ui.theme.DashboardAndroidTheme

// Real components with deterministic local data; no requests or preferences needed for previews.
@Preview(name = "首页 · 明亮", widthDp = 390, heightDp = 840, locale = "zh")
@Preview(name = "首页 · 深色", widthDp = 390, heightDp = 840, locale = "zh", uiMode = 0x20)
@Composable
private fun HomePreview() {
    DashboardAndroidTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("下午好", style = MaterialTheme.typography.headlineMedium)
                Text("探索 HarmonyOS 应用生态的最新动态。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val market = MarketInfo(
                    counts = MarketCounts(apps = 12680, atomicServices = 2480, maxDownloadCount = 123456789),
                    developerCount = 8462, substanceCount = 386,
                    timestamp = "2026-10-11T12:30:00", crateVersion = "3.1.0",
                    syncStatus = SyncStatus(totalProcessed = 15160, totalInserted = 86, totalSkipped = 15074),
                )
                MarketOverview(market)
                SyncCard(market)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(R.drawable.dashboard_station, R.drawable.dashboard_home, R.drawable.harmony_api, R.drawable.harmony_user).forEach {
                        ResourceIcon(it, null, Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "分布图 · 明亮", widthDp = 390, locale = "zh")
@Preview(name = "分布图 · 深色", widthDp = 390, locale = "zh", uiMode = 0x20)
@Preview(name = "分布图 · 窄屏大字", widthDp = 320, fontScale = 1.5f, locale = "zh")
@Preview(name = "分布图 · 平板", widthDp = 840, locale = "zh")
@Composable
private fun DistributionPreview() {
    DashboardAndroidTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardCard(Modifier.padding(14.dp).fillMaxWidth()) {
                Text("应用评分分布", style = MaterialTheme.typography.titleLarge)
                DistributionChart(listOf(
                    DistributionEntry("5 星", 7850, 0), DistributionEntry("4 星", 1640, 1),
                    DistributionEntry("3 星", 820, 2), DistributionEntry("2 星", 450, 3),
                    DistributionEntry("1 星与无评分", 1960, 4),
                ), Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Preview(name = "趋势图 · 明亮", widthDp = 390, locale = "zh")
@Preview(name = "趋势图 · 深色", widthDp = 390, locale = "zh", uiMode = 0x20)
@Preview(name = "趋势图 · 窄屏", widthDp = 320, locale = "zh")
@Composable
private fun TrendPreview() {
    DashboardAndroidTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardCard(Modifier.padding(14.dp).fillMaxWidth()) {
                Text("下载量趋势", style = MaterialTheme.typography.titleLarge)
                LineChart(listOf(11000.0, 12300.0, 12500.0, 16200.0, 18100.0, 19200.0, 25000.0),
                    labels = (1..7).map { "2026-10-0$it" }, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Preview(name = "我的 · 边距", widthDp = 390, heightDp = 720, locale = "zh")
@Preview(name = "我的 · 深色边距", widthDp = 390, heightDp = 720, locale = "zh", uiMode = 0x20)
@Preview(name = "我的 · 窄屏大字", widthDp = 320, heightDp = 900, fontScale = 1.5f, locale = "zh")
@Composable
private fun ProfileSpacingPreview() {
    DashboardAndroidTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(DashboardPagePadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileIdentity()
                ProfileSection("设置") {
                    SettingRow("称呼", "Rayawa", R.drawable.harmony_user) {}
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchRow("触感反馈", "点击主要控件时振动", true, R.drawable.dashboard_vibration) {}
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SettingRow("清除缓存", "API 与网页缓存 12 MB", R.drawable.harmony_trash) {}
                }
                ProfileSection("帮助与项目") {
                    SettingRow("使用教程", "搜索、筛选与详情", R.drawable.harmony_tutorial) {}
                }
            }
        }
    }
}

@Preview(name = "同步进度 · 明亮", widthDp = 390, locale = "zh")
@Preview(name = "同步进度 · 深色", widthDp = 390, locale = "zh", uiMode = 0x20)
@Composable
private fun SyncProgressPreview() {
    DashboardAndroidTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(DashboardPagePadding)) {
                SyncCard(MarketInfo(syncStatus = SyncStatus(
                    isSyncingAll = true, progress = listOf(6500, 10000), totalProcessed = 6500,
                    totalInserted = 100, totalSkipped = 6400,
                )))
            }
        }
    }
}
