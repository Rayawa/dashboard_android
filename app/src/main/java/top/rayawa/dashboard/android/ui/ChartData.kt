package top.rayawa.dashboard.android.ui

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import top.rayawa.dashboard.android.data.SdkDistribution

data class DistributionEntry(val label: String, val value: Long, val colorIndex: Int)

/** The same SDK keeps the same color in both charts; collapsed categories retain all counts. */
fun sdkEntries(values: List<SdkDistribution>, sharedSdks: List<Int>, expanded: Boolean): List<DistributionEntry> {
    val counts = values.groupBy { it.sdk }.mapValues { (_, rows) -> rows.sumOf { it.count.coerceAtLeast(0) } }
    val ordered = sharedSdks.distinct().sortedDescending().filter { it in counts }
    val visible = if (expanded) ordered else ordered.take(7)
    val entries = visible.map { sdk -> DistributionEntry("API $sdk", counts.getValue(sdk), sharedSdks.indexOf(sdk)) }
    return if (visible.size < ordered.size) {
        entries + DistributionEntry("其他", ordered.drop(7).sumOf { counts.getValue(it) }, -1)
    } else entries
}

/** Start at zero, with headroom even for flat, zero or single-point series. */
fun chartMaximum(values: List<Double>, fixedMaximum: Double? = null): Double {
    if (fixedMaximum != null && fixedMaximum.isFinite() && fixedMaximum > 0) return fixedMaximum
    val maximum = values.filter { it.isFinite() && it >= 0 }.maxOrNull() ?: 0.0
    if (maximum == 0.0) return 1.0
    val padded = maximum * 1.08
    val magnitude = 10.0.pow(floor(log10(padded)))
    return ceil(padded / magnitude * 4.0) / 4.0 * magnitude
}

fun chartPercent(value: Long, total: Long): String =
    if (total <= 0) "0.0%" else java.lang.String.format(java.util.Locale.ROOT, "%.1f%%", value.toDouble() / total * 100)
