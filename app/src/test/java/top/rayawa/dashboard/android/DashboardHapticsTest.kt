package top.rayawa.dashboard.android

import org.junit.Assert.assertEquals
import org.junit.Test
import top.rayawa.dashboard.android.ui.DashboardHaptics

class DashboardHapticsTest {
    @Test
    fun enabledInteractionEmitsOneFeedback() {
        var feedbackCount = 0
        val haptics = DashboardHaptics(isEnabled = { true }, emitFeedback = { feedbackCount++ })
        haptics.press()
        assertEquals(1, feedbackCount)
    }

    @Test
    fun disabledInteractionDoesNotRequestDeviceFeedback() {
        var feedbackCount = 0
        val haptics = DashboardHaptics(isEnabled = { false }, emitFeedback = { feedbackCount++ })
        repeat(3) { haptics.press() }
        assertEquals(0, feedbackCount)
    }

    @Test
    fun existingControllerReadsSettingChangesAtInteractionTime() {
        var enabled = true
        var feedbackCount = 0
        val haptics = DashboardHaptics(isEnabled = { enabled }, emitFeedback = { feedbackCount++ })
        haptics.press()
        enabled = false
        haptics.press()
        assertEquals(1, feedbackCount)
        enabled = true
        haptics.press()
        assertEquals(2, feedbackCount)
    }
}
