package top.rayawa.dashboard.android

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.rayawa.dashboard.android.ui.DashboardAssistChip
import top.rayawa.dashboard.android.ui.DashboardButton
import top.rayawa.dashboard.android.ui.DashboardCard
import top.rayawa.dashboard.android.ui.DashboardHaptics
import top.rayawa.dashboard.android.ui.LocalDashboardHaptics
import top.rayawa.dashboard.android.ui.SwitchRow
import top.rayawa.dashboard.android.ui.theme.DashboardAndroidTheme

class DashboardControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun buttonsCardsAndChipsShareLivePreferenceWithoutChangingActions() {
        var enabled = true
        var feedbackCount = 0
        var actionCount = 0
        val haptics = DashboardHaptics({ enabled }, { feedbackCount++ })
        compose.setContent {
            DashboardAndroidTheme {
                CompositionLocalProvider(LocalDashboardHaptics provides haptics) {
                    Column {
                        DashboardButton(onClick = { actionCount++ }) { Text("按钮") }
                        DashboardCard(onClick = { actionCount++ }) { Text("应用卡片") }
                        DashboardAssistChip(onClick = { actionCount++ }, label = { Text("筛选") })
                        DashboardButton(onClick = { actionCount++ }, enabled = false) { Text("禁用") }
                    }
                }
            }
        }
        compose.onNodeWithText("按钮").performClick()
        compose.runOnIdle {
            assertEquals(1, feedbackCount)
            enabled = false
        }
        compose.onNodeWithText("应用卡片").performClick()
        compose.runOnIdle {
            assertEquals(1, feedbackCount)
            assertEquals(2, actionCount)
            enabled = true
        }
        compose.onNodeWithText("筛选").performClick()
        compose.onNodeWithText("禁用").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(2, feedbackCount)
            assertEquals(3, actionCount)
        }
    }

    @Test
    fun feedbackSettingRowCanEnableFromItsLabelAndDisableSilently() {
        val enabled = mutableStateOf(false)
        var feedbackCount = 0
        val haptics = DashboardHaptics({ enabled.value }, { feedbackCount++ })
        compose.setContent {
            DashboardAndroidTheme {
                CompositionLocalProvider(LocalDashboardHaptics provides haptics) {
                    SwitchRow("触感反馈", "点击主要控件时振动", checked = enabled.value) { enabled.value = it }
                }
            }
        }
        compose.onNodeWithText("触感反馈").performClick()
        compose.runOnIdle {
            assertEquals(true, enabled.value)
            assertEquals(1, feedbackCount)
        }
        compose.onNodeWithText("触感反馈").performClick()
        compose.runOnIdle {
            assertEquals(false, enabled.value)
            assertEquals(1, feedbackCount)
        }
    }
}
