package com.witte.lozify.core.update

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UpdateManager parsing and version comparison logic.
 *
 * Stage 28: Online Version Detection & Push System.
 */
class UpdateManagerTest {

    private lateinit var updateManager: UpdateManager

    @Before
    fun setUp() {
        updateManager = UpdateManager()
    }

    @Test
    fun testParseUpdateJson_newerVersionCode_triggersUpdate() {
        val rawJson = """
            {
                "versionCode": 3,
                "versionName": "1.3.0",
                "releaseDate": "2026-08-20",
                "changelog": "✨ 1. 新增 WebDAV 端到端防审查加密\n⚡ 2. 瀑布流列表 120Hz 丝滑",
                "downloadUrl": "https://gitee.com/witte/Lozify/releases/download/v1.3.0/app-debug.apk",
                "githubDownloadUrl": "https://github.com/witte/Lozify/releases/download/v1.3.0/app-debug.apk",
                "isForceUpdate": false
            }
        """.trimIndent()

        val currentVersionCode = 2 // Current is v1.2.0 (versionCode 2)
        val updateInfo = updateManager.parseUpdateJson(rawJson, currentVersionCode)

        assertNotNull(updateInfo)
        assertEquals(3, updateInfo?.versionCode)
        assertEquals("1.3.0", updateInfo?.versionName)
        assertEquals("2026-08-20", updateInfo?.releaseDate)
        assertTrue(updateInfo?.changelog?.contains("WebDAV") == true)
        assertEquals("https://gitee.com/witte/Lozify/releases/download/v1.3.0/app-debug.apk", updateInfo?.downloadUrl)
        assertEquals("https://github.com/witte/Lozify/releases/download/v1.3.0/app-debug.apk", updateInfo?.githubDownloadUrl)
        assertFalse(updateInfo?.isForceUpdate ?: true)
    }

    @Test
    fun testParseUpdateJson_sameOrOlderVersion_returnsNull() {
        val rawJson = """
            {
                "versionCode": 2,
                "versionName": "1.2.0",
                "releaseDate": "2026-08-19",
                "changelog": "当前版本已是最新",
                "downloadUrl": "https://gitee.com/witte/Lozify/releases/download/v1.2.0/app-debug.apk",
                "isForceUpdate": false
            }
        """.trimIndent()

        // Same version (2 vs 2) -> No update
        val updateInfoSame = updateManager.parseUpdateJson(rawJson, currentVersionCode = 2)
        assertNull(updateInfoSame)

        // Older remote version (2 vs 3) -> No update
        val updateInfoOlder = updateManager.parseUpdateJson(rawJson, currentVersionCode = 3)
        assertNull(updateInfoOlder)
    }

    @Test
    fun testParseUpdateJson_forceUpdateFlag_parsedProperly() {
        val rawJson = """
            {
                "versionCode": 10,
                "versionName": "2.0.0",
                "releaseDate": "2026-09-01",
                "changelog": "重大架构升级，必须更新",
                "downloadUrl": "https://gitee.com/witte/Lozify/releases/download/v2.0.0/app-debug.apk",
                "isForceUpdate": true
            }
        """.trimIndent()

        val updateInfo = updateManager.parseUpdateJson(rawJson, currentVersionCode = 2)
        assertNotNull(updateInfo)
        assertTrue(updateInfo?.isForceUpdate == true)
    }
}
