package com.witte.lozify.core.network.webdav

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WebDAV Fast-Path & Zero-Traffic Sync Decision Logic.
 *
 * Stage 29: Pull-to-Sync & Fast-Path Optimization.
 */
class WebDavFastPathTest {

    @Test
    fun testFastPath_bothSidesUpToDate_canSkipFullSync() {
        val lastSyncTimestamp = 1787030000000L
        val localMaxUpdatedAt = 1787030000000L
        val localNoteCount = 42

        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("lastSyncTime", 1787030000000L)
            put("noteCount", 42)
            put("device", "Xiaomi 14")
        }

        val remoteLastSyncTime = manifestJson.getLong("lastSyncTime")
        val remoteNoteCount = manifestJson.getInt("noteCount")

        val canFastPath = remoteLastSyncTime <= lastSyncTimestamp &&
                localMaxUpdatedAt <= lastSyncTimestamp &&
                remoteNoteCount == localNoteCount

        assertTrue("Should take Fast-Path when neither side has any new changes", canFastPath)
    }

    @Test
    fun testFastPath_localHasNewerEdit_mustPerformSync() {
        val lastSyncTimestamp = 1787030000000L
        val localMaxUpdatedAt = 1787035000000L // 5000ms after last sync
        val localNoteCount = 42

        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("lastSyncTime", 1787030000000L)
            put("noteCount", 42)
        }

        val remoteLastSyncTime = manifestJson.getLong("lastSyncTime")
        val remoteNoteCount = manifestJson.getInt("noteCount")

        val canFastPath = remoteLastSyncTime <= lastSyncTimestamp &&
                localMaxUpdatedAt <= lastSyncTimestamp &&
                remoteNoteCount == localNoteCount

        assertFalse("Cannot fast-path when local has newer edits", canFastPath)
    }

    @Test
    fun testFastPath_remoteHasNewerSync_mustPerformSync() {
        val lastSyncTimestamp = 1787030000000L
        val localMaxUpdatedAt = 1787030000000L
        val localNoteCount = 42

        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("lastSyncTime", 1787038000000L) // Remote updated by another device
            put("noteCount", 43)
        }

        val remoteLastSyncTime = manifestJson.getLong("lastSyncTime")
        val remoteNoteCount = manifestJson.getInt("noteCount")

        val canFastPath = remoteLastSyncTime <= lastSyncTimestamp &&
                localMaxUpdatedAt <= lastSyncTimestamp &&
                remoteNoteCount == localNoteCount

        assertFalse("Cannot fast-path when remote has been updated by another device", canFastPath)
    }

    @Test
    fun testSyncResult_isAlreadyUpToDate_flagIntegrity() {
        val fastPathResult = SyncResult(
            isSuccess = true,
            isAlreadyUpToDate = true
        )

        assertTrue(fastPathResult.isSuccess)
        assertTrue(fastPathResult.isAlreadyUpToDate)
        assertEquals(0, fastPathResult.uploadedNotes)
        assertEquals(0, fastPathResult.downloadedNotes)
    }
}
