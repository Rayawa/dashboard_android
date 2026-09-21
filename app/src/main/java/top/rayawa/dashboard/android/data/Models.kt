package top.rayawa.dashboard.android.data

data class MarketCounts(
    val apps: Long = 0,
    val atomicServices: Long = 0,
    val maxDownloadCount: Long = 0,
    val total: Long = 0,
)

data class SyncDuration(val seconds: Long = 0, val nanos: Long = 0)

data class SyncStatus(
    val isSyncingAll: Boolean = false,
    val totalProcessed: Long = 0,
    val totalInserted: Long = 0,
    val totalSkipped: Long = 0,
    val totalFailed: Long = 0,
    val progress: List<Long> = emptyList(),
    val elapsed: SyncDuration? = null,
    val estimated: SyncDuration? = null,
    val nextSync: SyncDuration? = null,
)

data class MarketInfo(
    val counts: MarketCounts = MarketCounts(),
    val crateVersion: String = "-",
    val developerCount: Long = 0,
    val substanceCount: Long = 0,
    val pageSizeMax: Int = 100,
    val syncStatus: SyncStatus = SyncStatus(),
    val timestamp: String = "",
)

data class Screenshot(val url: String, val resolution: String = "", val rotated: Int = 0)

data class MarketApp(
    val appId: String = "",
    val packageName: String = "",
    val name: String = "未知应用",
    val iconUrl: String = "",
    val developerName: String = "未知开发者",
    val description: String = "",
    val briefDescription: String = "",
    val kindName: String = "",
    val kindTypeName: String = "",
    val tagName: String = "",
    val version: String = "",
    val versionCode: Long = 0,
    val downloadCount: Long = 0,
    val sizeBytes: Long = 0,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val compileSdk: Int = 0,
    val averageRating: Double? = null,
    val fullAverageRating: Double? = null,
    val totalRatingCount: Long = 0,
    val starCounts: List<Long> = List(5) { 0 },
    val listedAt: String = "",
    val updatedAt: String = "",
    val metricsCreatedAt: String = "",
    val releaseDate: Long = 0,
    val supplier: String = "",
    val privacyUrl: String = "",
    val newFeatures: String = "",
    val screenshots: List<Screenshot> = emptyList(),
    val releaseCountries: List<String> = emptyList(),
    val mainDeviceCodes: List<String> = emptyList(),
) {
    val isAtomicService: Boolean get() = packageName.startsWith("com.atomicservice")
}

data class AppsPage(
    val apps: List<MarketApp> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Long = 0,
    val totalPages: Int = 1,
)

data class DailyMaxDownload(
    val appId: String,
    val appName: String,
    val packageName: String,
    val downloadCount: Long,
    val reportDate: String,
)

data class DeveloperRanking(val id: String, val name: String, val appCount: Long)

data class RatingDistribution(
    val star1: Long = 0,
    val star2: Long = 0,
    val star3: Long = 0,
    val star4: Long = 0,
    val star5: Long = 0,
) {
    val values: List<Long> get() = listOf(star5, star4, star3, star2, star1)
}

data class SdkDistribution(val sdk: Int, val count: Long)

data class DownloadGrowth(
    val appId: String,
    val name: String,
    val packageName: String,
    val currentDate: String,
    val priorDate: String,
    val currentDownloads: Long,
    val priorDownloads: Long,
    val increment: Long,
)

data class AppMetric(
    val version: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val downloadCount: Long,
    val reportDate: String,
)

data class RatingHistory(
    val reportDate: String,
    val average: Double,
    val totalCount: Long,
)

data class AppDetail(
    val app: MarketApp,
    val metrics: List<AppMetric> = emptyList(),
    val ratings: List<RatingHistory> = emptyList(),
)

enum class AppType(val label: String) {
    ALL("全部"), APP("应用"), ATOMIC("元服务")
}

enum class SortField(val label: String, val apiName: String) {
    UPDATED("更新时间", "updated_at"),
    LISTED("上架时间", "listed_at"),
    DOWNLOADS("下载量", "download_count"),
    RATINGS("评分数量", "total_star_rating_count"),
    SIZE("应用大小", "size_bytes"),
}

enum class FilterField(val label: String, val apiName: String) {
    NAME("应用名称", "name"),
    PACKAGE("包名", "pkg_name"),
    APP_ID("App ID", "app_id"),
    CATEGORY("分类", "kind_name"),
    TAG("标签", "tag_name"),
    DEVELOPER("开发者", "developer_name"),
}

enum class SearchOperator(val label: String, val apiName: String) {
    EQUAL("等于", "eq"),
    NOT_EQUAL("不等于", "ne"),
    CONTAINS("包含", "i_like"),
    NOT_CONTAINS("不包含", "not_i_like"),
    GREATER("大于", "gt"),
    LESS("小于", "lt"),
    NOT_EMPTY("不为空", "is_not_null"),
}

data class AdvancedCondition(
    val field: FilterField,
    val operator: SearchOperator,
    val value: String = "",
)
