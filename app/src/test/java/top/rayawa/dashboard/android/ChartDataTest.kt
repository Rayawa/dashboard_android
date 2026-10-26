package top.rayawa.dashboard.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.rayawa.dashboard.android.data.SdkDistribution
import top.rayawa.dashboard.android.ui.chartMaximum
import top.rayawa.dashboard.android.ui.chartPercent
import top.rayawa.dashboard.android.ui.sdkEntries

class ChartDataTest {
    @Test
    fun collapsedSdkPreservesEveryCountAndExpandsAllCategories() {
        val values = (1..12).map { SdkDistribution(it, it * 100L) }
        val labels = values.map { it.sdk }.reversed()
        val collapsed = sdkEntries(values, labels, false)
        assertEquals(8, collapsed.size)
        assertEquals("其他", collapsed.last().label)
        assertEquals(values.sumOf { it.count }, collapsed.sumOf { it.value })
        assertEquals(values.sumOf { it.count }, sdkEntries(values, labels, true).sumOf { it.value })
        assertEquals(12, sdkEntries(values, labels, true).size)
    }

    @Test
    fun sdkColorsRemainStableAcrossDifferentMinimumAndTargetDistributions() {
        val shared = listOf(22, 21, 20, 19, 18)
        val minimum = sdkEntries(listOf(SdkDistribution(20, 40), SdkDistribution(18, 60)), shared, false)
        val target = sdkEntries(listOf(SdkDistribution(22, 50), SdkDistribution(20, 50)), shared, false)
        assertEquals(minimum.first { it.label == "API 20" }.colorIndex, target.first { it.label == "API 20" }.colorIndex)
        assertEquals("API 22", target.first().label)
    }

    @Test
    fun emptyFlatSinglePointAndLargeSeriesHaveUsableAxes() {
        assertEquals(1.0, chartMaximum(emptyList()), 0.0)
        assertEquals(1.0, chartMaximum(listOf(0.0, 0.0)), 0.0)
        assertTrue(chartMaximum(listOf(42.0, 42.0)) > 42.0)
        assertTrue(chartMaximum(listOf(100_000_001.0)) > 100_000_001.0)
        assertEquals(5.0, chartMaximum(listOf(4.9), fixedMaximum = 5.0), 0.0)
        assertEquals(1.0, chartMaximum(listOf(Double.NaN, Double.POSITIVE_INFINITY)), 0.0)
    }

    @Test
    fun percentagesHandleEmptyAndLargeTotals() {
        assertEquals("0.0%", chartPercent(0, 0))
        assertEquals("25.0%", chartPercent(1_000_000_000, 4_000_000_000))
    }
}
