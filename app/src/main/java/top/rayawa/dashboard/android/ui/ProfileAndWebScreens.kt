package top.rayawa.dashboard.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import top.rayawa.dashboard.android.data.DashboardApi

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SStationScreen(modifier: Modifier = Modifier) {
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler(canGoBack) { webView?.goBack() }

    Column(modifier.fillMaxSize()) {
        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        error?.let { message ->
            DashboardCard(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("S站暂时无法连接", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { error = null; webView?.reload() }, modifier = Modifier.padding(top = 8.dp)) { Text("重新加载") }
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.setSupportZoom(true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            canGoBack = view.canGoBack()
                            error = null
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, value: WebResourceError) {
                            if (request.isForMainFrame) error = value.description.toString()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                            canGoBack = view.canGoBack()
                        }
                    }
                    loadUrl(DashboardApi.SITE_URL)
                }
            },
        )
        Text(
            "S站内容由 Dashboard 项目网站提供；应用数据浏览与搜索请使用原生“首页”和“详情”页面。",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DisposableEffect(Unit) { onDispose { webView?.destroy(); webView = null } }
}

@Composable
fun ProfileScreen(showNotice: () -> Unit, openDetailByPackage: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dashboard_settings", 0) }
    var username by remember { mutableStateOf(prefs.getString("username", "").orEmpty()) }
    var editName by remember { mutableStateOf(false) }
    var haptics by remember { mutableStateOf(prefs.getBoolean("haptics", true)) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableLongStateOf(DashboardApi.cacheSizeBytes()) }
    var showLinks by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▦", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Text("Dashboard应用看板", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("原生 Android 客户端", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("设置", style = MaterialTheme.typography.titleLarge)
            DashboardCard(Modifier.fillMaxWidth()) {
                SettingRow("称呼", username.ifBlank { "未设置" }, onClick = { editName = true })
                HorizontalDivider()
                SwitchRow("触感反馈", "点击主要控件时振动", haptics) {
                    haptics = it; prefs.edit { putBoolean("haptics", it) }
                }
                HorizontalDivider()
                SettingRow("清除缓存", "API 与网页缓存 ${formatBytes(cacheSize)}") { confirmClear = true }
            }
        }
        item {
            Text("帮助与项目", style = MaterialTheme.typography.titleLarge)
            DashboardCard(Modifier.fillMaxWidth()) {
                SettingRow("使用教程", "搜索、筛选与详情") {
                    infoDialog = "使用教程" to "1. 首页展示市场总览、同步状态、排行榜与最近收录。\n\n2. 详情页的数据概况可查看评分、SDK 与下载增长；应用列表支持类型、字段、排序、分页和 AND/OR 高级组合搜索。\n\n3. 顶部搜索支持名称、包名、App ID 与应用市场链接，也可提交新应用或更新请求。\n\n4. 应用详情支持查看评分、截图、下载与评分趋势，并可打开应用市场、隐私政策或分享。\n\n5. 网络失败时会回退到本地 API 缓存，可在设置中清理。"
                }
                HorizontalDivider()
                SettingRow("重要提示与隐私", "查看数据来源与隐私说明", showNotice)
                HorizontalDivider()
                SettingRow("隐私政策", "本机数据与网络访问说明") {
                    infoDialog = "隐私政策" to "本应用不要求登录，不收集通讯录、位置、相册或麦克风数据。称呼、搜索历史和界面设置保存在本机。\n\n浏览市场信息时会向 Dashboard 后端请求公开数据并缓存响应；加载图标与截图时会访问华为应用图片服务器。提交应用时，应用标识、平台标识与用户填写的称呼会发送到 Dashboard 后端。\n\n你可以随时在设置中清除 API 缓存和网页数据。"
                }
                HorizontalDivider()
                SettingRow("更新日志", "Android 1.0.0") {
                    infoDialog = "更新日志" to "V1.0.0 · 2026-09-21\n\n• 首个 Android 原生版本\n• 对齐 S站、首页、详情与我的四个入口\n• 支持市场概览、同步状态、排行榜、数据图表与下载增长\n• 支持列表筛选、排序、分页和高级组合搜索\n• 支持原生搜索、投稿、应用详情、截图和趋势图\n• 支持离线 API 缓存、深链、响应式导航与 Material 3 动态配色"
                }
                HorizontalDivider()
                SettingRow("联系我们", "邮箱与官方交流群") {
                    infoDialog = "联系我们" to "主要负责人：shenjack\n邮箱：3695888@qq.com\n\n应用开发：清霁·Rayawa\n邮箱：rayawa.work@outlook.com\n\nT站网页负责人：tianxiu2b2t\n邮箱：administrator@ttb-network.top\n\nHarmony Dashboard 官方交流群：757273833"
                }
                HorizontalDivider()
                SettingRow("项目主页", "dashboard.rayawa.top") { openExternal(context, "https://dashboard.rayawa.top/") }
                HorizontalDivider()
                SettingRow("订阅 MeoW 频道", "项目动态与社区内容") { openExternal(context, "https://www.chuckfang.com/MeoW/appLinking?channelId=8bbba7f53bf9458284a41d04f295450b") }
                HorizontalDivider()
                SettingRow("友情链接", "HarmonyOS 生态项目") {
                    showLinks = true
                }
                HorizontalDivider()
                SettingRow("联系与赞助", "反馈问题或支持项目") { openExternal(context, "https://afdian.com/a/shenjack") }
            }
        }
        item {
            Text("关于", style = MaterialTheme.typography.titleLarge)
            DashboardCard(Modifier.fillMaxWidth()) {
                SettingRow("版本", "1.0.0 (1)") {
                    infoDialog = "开发者信息" to "Designed & Developed by Ray Chen (Rayawa)\nAndroid 原生版本使用 Kotlin 与 Jetpack Compose 构建。"
                }
                HorizontalDivider()
                SettingRow("后端 API", "ddns.shenjack.top:10003") { openExternal(context, "${DashboardApi.SITE_URL}docs") }
                HorizontalDivider()
                SettingRow("京ICP备2025153453号", "本应用备案域名：rayawa.top") { openExternal(context, "https://beian.miit.gov.cn/#/Integrated/index") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Copyright © 2026. All rights reserved.", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(84.dp))
        }
    }

    if (editName) AlertDialog(
        onDismissRequest = { editName = false },
        title = { Text("设置称呼") },
        text = { OutlinedTextField(username, { username = it.take(30) }, label = { Text("你的称呼") }, singleLine = true) },
        confirmButton = { Button(onClick = { prefs.edit { putString("username", username.trim()) }; editName = false }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { editName = false }) { Text("取消") } },
    )
    infoDialog?.let { info -> AlertDialog(
        onDismissRequest = { infoDialog = null },
        title = { Text(info.first) },
        text = { LazyColumn { item { Text(info.second) } } },
        confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("知道了") } },
    ) }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清除缓存") },
        text = { Text("将清除 API 离线缓存、S站网页存储与 Cookie。称呼和设置会保留。") },
        confirmButton = {
            Button(onClick = {
                DashboardApi.clearCache()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                cacheSize = 0
                confirmClear = false
            }) { Text("继续") }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
    )
    if (showLinks) AlertDialog(
        onDismissRequest = { showLinks = false },
        title = { Text("友情链接") },
        text = {
            LazyColumn {
                item { Text("项目站点", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                item { SettingRow("项目网站首页", "dashboard.rayawa.top") { openExternal(context, "https://dashboard.rayawa.top/") } }
                item { SettingRow("S站", DashboardApi.SITE_URL) { openExternal(context, DashboardApi.SITE_URL) } }
                item { SettingRow("Egui", "shenjack.top:10003/egui") { openExternal(context, "${DashboardApi.PUBLIC_SITE_URL}egui/") } }
                item { SettingRow("T站", "hmos.txit.top/dashboard") { openExternal(context, "https://hmos.txit.top/dashboard") } }
                item { Text("友好应用", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                item { SettingRow("迹得", "cn.biandangroup.getrace") { showLinks = false; openDetailByPackage("cn.biandangroup.getrace") } }
                item { SettingRow("记得订阅", "com.xxtstudio.accountDL") { showLinks = false; openDetailByPackage("com.xxtstudio.accountDL") } }
                item { SettingRow("记得订阅（元服务）", "com.atomicservice.6917600885144283966") { showLinks = false; openDetailByPackage("com.atomicservice.6917600885144283966") } }
                item { SettingRow("纸语", "com.my.papertalk") { showLinks = false; openDetailByPackage("com.my.papertalk") } }
                item { Text("生态站点", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                item { SettingRow("Hap资源站", "sydxky.cn") { openExternal(context, "https://sydxky.cn/") } }
                item { SettingRow("应用荟萃", "home.dztap.com") { openExternal(context, "https://home.dztap.com/index") } }
                item { SettingRow("Open Store", "next.betahub.tech") { openExternal(context, "https://next.betahub.tech/") } }
                item { SettingRow("NEXT Store", "next.vcck.cn") { openExternal(context, "https://next.vcck.cn/#/") } }
                item { SettingRow("方舟网", "fangzhou.club") { openExternal(context, "https://fangzhou.club/") } }
                item { SettingRow("AppGallery统计数据", "appgallery.info") { openExternal(context, "https://appgallery.info/index.html") } }
            }
        },
        confirmButton = { TextButton(onClick = { showLinks = false }) { Text("关闭") } },
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun ImportantNoticeDialog(required: Boolean, onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!required) onDismiss() },
        title = { Text("重要提示") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Dashboard 应用看板是用于浏览公开 HarmonyOS 应用市场信息的第三方数据工具。") }
                item { Text("应用名称、图标、版本、开发者、评分、下载量和截图等内容来自公开接口，可能存在延迟或误差，不构成商业、投资或安全建议。") }
                item { Text("客户端会访问 Dashboard 后端与应用图片服务器；搜索记录、称呼和显示设置仅保存在本机。提交应用时会将所填应用标识和称呼发送至服务端。") }
                item { Text("打开华为应用市场、隐私政策、项目主页等链接时，将由系统浏览器或对应应用处理。") }
                item { Text("继续使用即表示你已阅读并理解上述说明。") }
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text(if (required) "我已阅读并同意" else "知道了") } },
        dismissButton = if (required) null else ({ TextButton(onClick = onDismiss) { Text("关闭") } }),
    )
}

private fun openExternal(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
