package top.rayawa.dashboard.android.data

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DashboardApi {
    const val SITE_URL = "https://ddns.shenjack.top:10003/"
    const val PUBLIC_SITE_URL = "https://shenjack.top:10003/"
    private const val API_URL = "${SITE_URL}api/v0/"
    private const val CACHE_PREFS = "dashboard_api_cache"
    private const val USER_AGENT = "top.rayawa.dashboard.android/1.0 Android"
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")

    private fun cacheKey(path: String, body: String?) = "${if (body == null) "GET" else "POST"}:$path:${body.orEmpty()}"

    private fun cached(key: String): String? =
        if (::appContext.isInitialized) appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            .getString(key, null) else null

    private fun saveCache(key: String, value: String) {
        if (::appContext.isInitialized) {
            appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
                .edit { putString(key, value) }
        }
    }

    private fun request(path: String, body: String? = null): JSONObject {
        val key = cacheKey(path, body)
        try {
            val connection = URI(API_URL + path).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = if (body == null) "GET" else "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "$USER_AGENT (${Build.MODEL}; API ${Build.VERSION.SDK_INT})")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream.bufferedReader().use(BufferedReader::readText)
                if (code !in 200..299) {
                    throw IllegalStateException(JSONObject(text).optString("message", "服务器返回 $code"))
                }
                if (text.length > 12_000_000) throw IllegalStateException("服务器响应过大")
                saveCache(key, text)
                return JSONObject(text)
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            val fallback = cached(key)
            if (fallback != null) return JSONObject(fallback)
            throw IllegalStateException(error.message ?: "网络连接失败，请稍后重试", error)
        }
    }

    suspend fun marketInfo(): MarketInfo = withContext(Dispatchers.IO) {
        val envelope = request("market_info")
        val data = envelope.getJSONObject("data")
        val count = data.getJSONObject("app_count")
        val status = data.optJSONObject("sync_status") ?: JSONObject()
        MarketInfo(
            counts = MarketCounts(
                apps = count.long("apps"),
                atomicServices = count.long("atomic_services"),
                maxDownloadCount = count.long("max_download_count"),
                total = count.long("total"),
            ),
            crateVersion = data.text("crate_version", "-"),
            developerCount = data.long("developer_count"),
            substanceCount = data.long("substance_count"),
            pageSizeMax = data.int("page_size_max", 100),
            syncStatus = status.toSyncStatus(),
            timestamp = envelope.text("timestamp"),
        )
    }

    suspend fun appsPage(
        page: Int = 1,
        pageSize: Int = 20,
        sort: SortField = SortField.UPDATED,
        descending: Boolean = true,
        searchField: FilterField? = null,
        searchValue: String = "",
        exact: Boolean = false,
        type: AppType = AppType.ALL,
    ): AppsPage = withContext(Dispatchers.IO) {
        val params = mutableListOf(
            "page_size=${pageSize.coerceIn(1, 100)}",
            "detail=true",
            "sort=${encode(sort.apiName)}",
            "desc=$descending",
            "exclude_atomic=${type == AppType.APP}",
        )
        when {
            type == AppType.ATOMIC -> {
                params += "search_key=pkg_name"
                params += "search_value=${encode("com.atomicservice")}" 
                params += "search_exact=false"
            }
            searchField != null && searchValue.isNotBlank() -> {
                params += "search_key=${encode(searchField.apiName)}"
                params += "search_value=${encode(searchValue.trim())}"
                params += "search_exact=$exact"
            }
        }
        parseAppsPage(request("apps/list/${page.coerceAtLeast(1)}?${params.joinToString("&")}"))
    }

    suspend fun search(query: String): List<MarketApp> = withContext(Dispatchers.IO) {
        val normalized = parseAppIdentity(query)
        if (normalized.first != null || normalized.second != null) {
            return@withContext try {
                listOf(appDetail(normalized.first, normalized.second).app)
            } catch (_: Exception) {
                emptyList()
            }
        }
        appsPage(
            pageSize = 30,
            sort = SortField.UPDATED,
            searchField = FilterField.NAME,
            searchValue = query,
        ).apps
    }

    suspend fun appsQuery(
        page: Int,
        pageSize: Int,
        sort: SortField,
        descending: Boolean,
        useAnd: Boolean,
        conditions: List<AdvancedCondition>,
    ): AppsPage = withContext(Dispatchers.IO) {
        val valid = conditions.filter { it.operator == SearchOperator.NOT_EMPTY || it.value.isNotBlank() }
        require(valid.isNotEmpty()) { "请至少填写一个有效的高级搜索条件" }
        val body = JSONObject().put(if (useAnd) "and" else "or", JSONArray().apply {
            valid.forEach { condition ->
                put(JSONObject().apply {
                    put("key", condition.field.apiName)
                    put("op", condition.operator.apiName)
                    if (condition.operator != SearchOperator.NOT_EMPTY) put("value", condition.value.trim())
                })
            }
        })
        val path = "apps/query?page=${page.coerceAtLeast(1)}&page_size=${pageSize.coerceIn(1, 100)}" +
            "&detail=true&sort=${encode(sort.apiName)}&desc=$descending"
        parseAppsPage(request(path, body.toString()))
    }

    fun cacheSizeBytes(): Long {
        if (!::appContext.isInitialized) return 0
        val values = appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).all.values
        return values.sumOf { (it as? String)?.toByteArray(StandardCharsets.UTF_8)?.size?.toLong() ?: 0L }
    }

    fun clearCache() {
        if (::appContext.isInitialized) appContext.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    suspend fun appDetail(appId: String? = null, packageName: String? = null): AppDetail = coroutineScope {
        val path = when {
            !appId.isNullOrBlank() -> "apps/app_id/${encode(appId)}?use_cache=true"
            !packageName.isNullOrBlank() -> "apps/pkg_name/${encode(packageName)}?use_cache=true"
            else -> throw IllegalArgumentException("缺少应用标识")
        }
        val detail = withContext(Dispatchers.IO) {
            val data = request(path).getJSONObject("data")
            val full = data.optJSONObject("full_info") ?: data
            full.toMarketApp()
        }
        val metrics = async { runCatching { appMetrics(detail.packageName) }.getOrDefault(emptyList()) }
        val ratings = async { runCatching { ratingHistory(detail.packageName) }.getOrDefault(emptyList()) }
        AppDetail(detail, metrics.await(), ratings.await())
    }

    suspend fun submitApp(value: String, user: String): MarketApp = withContext(Dispatchers.IO) {
        val identity = parseAppIdentity(value)
        if (identity.first == null && identity.second == null) throw IllegalArgumentException("请输入有效的 App ID、包名或应用市场链接")
        val body = JSONObject().apply {
            identity.first?.let { put("app_id", it) }
            identity.second?.let { put("pkg_name", it) }
            put("comment", JSONObject().put("platform", "dashboard-android/1.0").put("user", user.trim()))
        }
        val data = request("submit", body.toString()).getJSONObject("data")
        (data.optJSONObject("full_info") ?: data).toMarketApp()
    }

    suspend fun dailyMaxDownloads(): List<DailyMaxDownload> = withContext(Dispatchers.IO) {
        request("rankings/max_download").getJSONArray("data").objects().map {
            DailyMaxDownload(
                it.text("app_id"), it.text("app_name", "未知应用"), it.text("pkg_name"),
                it.long("download_count"), it.text("report_date")
            )
        }.sortedByDescending { it.reportDate }.take(10)
    }

    suspend fun developerRanking(): List<DeveloperRanking> = withContext(Dispatchers.IO) {
        request("rankings/developers?limit=10&page=1").getJSONArray("data").arrays().mapNotNull {
            if (it.length() < 3) null else DeveloperRanking(it.optString(0), it.optString(1, "未知开发者"), it.optLong(2))
        }
    }

    suspend fun ratingDistribution(): RatingDistribution = withContext(Dispatchers.IO) {
        val data = request("charts/rating").getJSONObject("data")
        RatingDistribution(data.long("star_1"), data.long("star_2"), data.long("star_3"), data.long("star_4"), data.long("star_5"))
    }

    suspend fun sdkDistribution(target: Boolean): List<SdkDistribution> = withContext(Dispatchers.IO) {
        request("charts/${if (target) "target_sdk" else "min_sdk"}").getJSONArray("data").arrays().mapNotNull {
            if (it.length() < 2) null else SdkDistribution(it.optInt(0), it.optLong(1))
        }.sortedByDescending { it.count }
    }

    suspend fun downloadGrowth(days: Int = 7): List<DownloadGrowth> = withContext(Dispatchers.IO) {
        request("rankings/download_increase?days=$days&limit=10&page=1").getJSONArray("data").objects().map {
            DownloadGrowth(
                it.text("app_id"), it.text("name", "未知应用"), it.text("pkg_name"),
                it.text("current_period_date"), it.text("prior_period_date"),
                it.long("current_download_count"), it.long("prior_download_count"), it.long("download_increment")
            )
        }
    }

    private suspend fun appMetrics(packageName: String): List<AppMetric> = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext emptyList()
        request("apps/metrics/${encode(packageName)}").getJSONArray("data").objects().map {
            AppMetric(
                it.text("version"), it.long("version_code"), it.long("size_bytes"),
                it.long("download_count"), it.text("created_at")
            )
        }.sortedBy { it.reportDate }
    }

    private suspend fun ratingHistory(packageName: String): List<RatingHistory> = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext emptyList()
        request("rankings/rate_history?pkg_name=${encode(packageName)}").getJSONArray("data").objects().mapNotNull {
            val average = it.doubleOrNull("cumulative_avg_rating_high_prec")
                ?: it.doubleOrNull("cumulative_avg_rating_origin")
            average?.let { value -> RatingHistory(it.text("report_date"), value, it.long("cumulative_total_count")) }
        }.sortedBy { it.reportDate }
    }

    private fun parseAppsPage(envelope: JSONObject): AppsPage {
        val data = envelope.getJSONObject("data")
        return AppsPage(
            apps = data.getJSONArray("data").objects().map { it.toMarketApp() },
            page = data.int("page", 1),
            pageSize = data.int("page_size", 20),
            totalCount = data.long("total_count"),
            totalPages = data.int("total_pages", 1).coerceAtLeast(1),
        )
    }

    fun parseAppIdentity(raw: String): Pair<String?, String?> {
        val value = raw.trim()
        if (value.isBlank()) return null to null
        val fromUrl = runCatching {
            val uri = URI(value)
            uri.rawQuery?.split("&")?.firstOrNull { it.startsWith("id=") }?.substringAfter("id=")
        }.getOrNull()
        val candidate = fromUrl ?: value
        return when {
            candidate.matches(Regex("C?[0-9]{8,}")) -> candidate to null
            candidate.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_-]+)+")) -> null to candidate
            else -> null to null
        }
    }

    private fun JSONObject.toSyncStatus(): SyncStatus {
        fun duration(key: String): SyncDuration? {
            val value = opt(key) ?: return null
            return when (value) {
                is Number -> SyncDuration(value.toLong())
                is JSONObject -> SyncDuration(value.long("secs"), value.long("nanos"))
                else -> null
            }
        }
        return SyncStatus(
            isSyncingAll = optBoolean("is_syncing_all"),
            totalProcessed = long("total_processed"), totalInserted = long("total_inserted"),
            totalSkipped = long("total_skipped"), totalFailed = long("total_failed"),
            progress = optJSONArray("progress")?.longs().orEmpty(),
            elapsed = duration("elapsed_time"), estimated = duration("estimated_total_time"),
            nextSync = duration("next_sync_countdown"),
        )
    }

    private fun JSONObject.toMarketApp(): MarketApp {
        val shots = optJSONArray("new_screen_shots")?.objects()?.map {
            Screenshot(it.text("url"), it.text("resolution"), it.int("rotated"))
        }.orEmpty().ifEmpty {
            optJSONArray("screen_shots")?.strings()?.map { Screenshot(it) }.orEmpty()
        }
        return MarketApp(
            appId = text("app_id"), packageName = text("pkg_name"), name = text("name", "未知应用"),
            iconUrl = text("icon_url"), developerName = text("developer_name", "未知开发者"),
            description = text("description"), briefDescription = text("brief_desc"),
            kindName = text("kind_name"), kindTypeName = text("kind_type_name"), tagName = text("tag_name"),
            version = text("version"), versionCode = long("version_code"), downloadCount = long("download_count"),
            sizeBytes = long("size_bytes"), minSdk = int("minsdk"), targetSdk = int("target_sdk"),
            compileSdk = int("compile_sdk_version"), averageRating = doubleOrNull("average_rating"),
            fullAverageRating = doubleOrNull("full_average_rating"), totalRatingCount = long("total_star_rating_count"),
            starCounts = listOf(long("star_1_rating_count"), long("star_2_rating_count"), long("star_3_rating_count"), long("star_4_rating_count"), long("star_5_rating_count")),
            listedAt = text("listed_at"), updatedAt = text("updated_at"), metricsCreatedAt = text("metrics_created_at"),
            releaseDate = long("release_date"), supplier = text("supplier"), privacyUrl = text("privacy_url"),
            newFeatures = text("new_features").ifBlank { text("upgrade_msg") }, screenshots = shots,
            releaseCountries = optJSONArray("release_countries")?.strings().orEmpty(),
            mainDeviceCodes = optJSONArray("main_device_codes")?.strings().orEmpty(),
        )
    }

    private fun JSONObject.text(key: String, fallback: String = ""): String {
        val value = opt(key)
        return if (value == null || value == JSONObject.NULL) fallback else value.toString()
    }
    private fun JSONObject.long(key: String, fallback: Long = 0): Long = when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> value.toDoubleOrNull()?.toLong() ?: fallback
        else -> fallback
    }
    private fun JSONObject.int(key: String, fallback: Int = 0): Int = long(key, fallback.toLong()).toInt()
    private fun JSONObject.doubleOrNull(key: String): Double? = when (val value = opt(key)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONArray.arrays(): List<JSONArray> = (0 until length()).mapNotNull { optJSONArray(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { opt(it)?.takeUnless { v -> v == JSONObject.NULL }?.toString() }
    private fun JSONArray.longs(): List<Long> = (0 until length()).map { optLong(it) }
}
