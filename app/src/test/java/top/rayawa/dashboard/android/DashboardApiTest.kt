package top.rayawa.dashboard.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import top.rayawa.dashboard.android.data.DashboardApi

class DashboardApiTest {
    @Test
    fun parsesAppId() {
        val result = DashboardApi.parseAppIdentity("C5765880207854244859")
        assertEquals("C5765880207854244859", result.first)
        assertNull(result.second)
    }

    @Test
    fun parsesPackageNameAndMarketLink() {
        assertEquals(
            "com.tencent.wechat",
            DashboardApi.parseAppIdentity("com.tencent.wechat").second,
        )
        assertEquals(
            "com.tencent.wechat",
            DashboardApi.parseAppIdentity("https://appgallery.huawei.com/app/detail?id=com.tencent.wechat").second,
        )
    }

    @Test
    fun rejectsFreeTextAsIdentity() {
        val result = DashboardApi.parseAppIdentity("微信")
        assertNull(result.first)
        assertNull(result.second)
    }
}
