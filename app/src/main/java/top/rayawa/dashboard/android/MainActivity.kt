package top.rayawa.dashboard.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import top.rayawa.dashboard.android.data.DashboardApi
import top.rayawa.dashboard.android.data.MarketApp
import top.rayawa.dashboard.android.ui.AppDetailScreen
import top.rayawa.dashboard.android.ui.AppsScreen
import top.rayawa.dashboard.android.ui.HomeScreen
import top.rayawa.dashboard.android.ui.ImportantNoticeDialog
import top.rayawa.dashboard.android.ui.ProfileScreen
import top.rayawa.dashboard.android.ui.SStationScreen
import top.rayawa.dashboard.android.ui.SearchScreen
import top.rayawa.dashboard.android.ui.theme.DashboardAndroidTheme

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DashboardApi.initialize(this)
        deepLink.value = intent?.data
        enableEdgeToEdge()
        setContent { DashboardAndroidTheme { DashboardApp(deepLink.value) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.data
    }
}

private enum class MainTab(val title: String, val symbol: String) {
    S_STATION("S站", "◎"), HOME("首页", "⌂"), APPS("详情", "▦"), PROFILE("我的", "♙")
}

private sealed interface AppRoute {
    data object Search : AppRoute
    data class Detail(val appId: String?, val packageName: String?, val preview: MarketApp? = null) : AppRoute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardApp(incomingLink: Uri?) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("dashboard_settings", 0) }
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var route by remember { mutableStateOf<AppRoute?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showNotice by remember { mutableStateOf(!prefs.getBoolean("notice_accepted_v1", false)) }

    fun openDetail(app: MarketApp) {
        route = AppRoute.Detail(app.appId, app.packageName, app)
    }
    fun openDetailById(appId: String, packageName: String) {
        route = AppRoute.Detail(appId.ifBlank { null }, packageName.ifBlank { null })
    }

    LaunchedEffect(incomingLink) {
        if (incomingLink == null) return@LaunchedEffect
        val appId = incomingLink.getQueryParameter("app_id") ?: incomingLink.getQueryParameter("id")
        val packageName = incomingLink.getQueryParameter("pkg_name")
        if (!appId.isNullOrBlank() || !packageName.isNullOrBlank()) {
            selectedTab = MainTab.APPS
            route = AppRoute.Detail(appId, packageName)
        }
    }
    BackHandler(route != null) { route = null }

    val title = when (val current = route) {
        AppRoute.Search -> "搜索应用"
        is AppRoute.Detail -> current.preview?.name ?: "应用详情"
        null -> selectedTab.title
    }

    @Composable
    fun Page(modifier: Modifier) {
        when (val current = route) {
            AppRoute.Search -> SearchScreen(::openDetail, modifier)
            is AppRoute.Detail -> AppDetailScreen(current.appId, current.packageName, current.preview, refreshKey, modifier)
            null -> when (selectedTab) {
                MainTab.S_STATION -> SStationScreen(modifier)
                MainTab.HOME -> HomeScreen(refreshKey, ::openDetail, ::openDetailById, modifier)
                MainTab.APPS -> AppsScreen(refreshKey, ::openDetail, modifier)
                MainTab.PROFILE -> ProfileScreen(
                    showNotice = { showNotice = true },
                    openDetailByPackage = { openDetailById("", it) },
                    modifier = modifier,
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (route != null) TextButton(onClick = { route = null }) { Text("‹ 返回") }
                    },
                    actions = {
                        if (route == null && selectedTab != MainTab.S_STATION) {
                            TextButton(onClick = { route = AppRoute.Search }) { Text("搜索") }
                        }
                        if (selectedTab != MainTab.PROFILE && route != AppRoute.Search) {
                            TextButton(onClick = { refreshKey++ }) { Text("刷新") }
                        }
                    },
                )
            },
            bottomBar = {
                if (!wide && route == null) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    if (prefs.getBoolean("haptics", true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                icon = { Text(tab.symbol, style = MaterialTheme.typography.titleLarge) },
                                label = { Text(tab.title) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (wide && route == null) {
                Row(Modifier.fillMaxSize().padding(padding)) {
                    NavigationRail {
                        MainTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = {
                                    selectedTab = tab
                                    if (prefs.getBoolean("haptics", true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                icon = { Text(tab.symbol, style = MaterialTheme.typography.titleLarge) },
                                label = { Text(tab.title) },
                            )
                        }
                    }
                    Page(Modifier.weight(1f))
                }
            } else {
                Page(Modifier.fillMaxSize().padding(padding))
            }
        }
    }

    if (showNotice) ImportantNoticeDialog(
        required = !prefs.getBoolean("notice_accepted_v1", false),
        onAccept = {
            prefs.edit { putBoolean("notice_accepted_v1", true) }
            showNotice = false
        },
        onDismiss = { showNotice = false },
    )
}
