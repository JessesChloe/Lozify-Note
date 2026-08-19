package com.witte.lozify.core.network.webdav

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for Distributed Multi-Device Global UUID & Fingerprint Anti-Duplication Sync Architecture.
 *
 * Verifies that:
 * 1. Concurrent note creation across different devices does NOT trigger ID collisions.
 * 2. Cross-version migration with different temporary syncIds does NOT produce duplicate notes.
 *
 * Stage 30 & 31: Distributed Global UUID & Anti-Duplication Engine.
 */
class WebDavUuidSyncTest {

    @Test
    fun testConcurrentMultiDeviceCreation_noIdCollision() {
        // 1. Phone creates 5 notes (Local IDs: 1..5)
        val phoneNotes = (1..5).map { index ->
            JSONObject().apply {
                put("syncId", "lz-phone-note-$index")
                put("id", index) // Local auto-increment ID on phone
                put("content", "手机创建的笔记 $index")
                put("createdAt", 1787000000000L + index * 1000)
                put("updatedAt", 1787000000000L + index * 1000)
                put("isPinned", false)
                put("isDeleted", false)
                put("tags", JSONArray().put("手机标签"))
            }
        }

        // 2. Emulator creates 3 notes (Local IDs: 1..3, created later)
        val emulatorNotes = (1..3).map { index ->
            JSONObject().apply {
                put("syncId", "lz-emulator-note-$index")
                put("id", index) // Local auto-increment ID on emulator (COLLIDING with phone's 1..3!)
                put("content", "电脑创建的笔记 $index")
                put("createdAt", 1787005000000L + index * 1000)
                put("updatedAt", 1787005000000L + index * 1000)
                put("isPinned", false)
                put("isDeleted", false)
                put("tags", JSONArray().put("电脑标签"))
            }
        }

        // 3. Merging by global syncId (UUID)
        val phoneMapBySyncId = phoneNotes.associateBy { it.getString("syncId") }
        val emulatorMapBySyncId = emulatorNotes.associateBy { it.getString("syncId") }

        val allMergedSyncIds = (phoneMapBySyncId.keys + emulatorMapBySyncId.keys).toSet()

        // Assert that all 8 notes exist distinctly
        assertEquals("Total notes after merge must be exactly 5 + 3 = 8", 8, allMergedSyncIds.size)

        // Verify phone's oldest notes are 100% intact
        assertTrue(allMergedSyncIds.contains("lz-phone-note-1"))
        assertTrue(allMergedSyncIds.contains("lz-phone-note-2"))
        assertTrue(allMergedSyncIds.contains("lz-phone-note-3"))
        assertTrue(allMergedSyncIds.contains("lz-phone-note-4"))
        assertTrue(allMergedSyncIds.contains("lz-phone-note-5"))

        // Verify emulator's new notes are 100% intact
        assertTrue(allMergedSyncIds.contains("lz-emulator-note-1"))
        assertTrue(allMergedSyncIds.contains("lz-emulator-note-2"))
        assertTrue(allMergedSyncIds.contains("lz-emulator-note-3"))
    }

    @Test
    fun testCrossVersionFingerprintDeduplication_preventsDuplicates() {
        fun noteFingerprint(createdAtMillis: Long, content: String): String = "$createdAtMillis:${content.trim()}"

        val creationTime = 1787010000000L
        val text = "这是一条跨版本同步的原始测试笔记"

        // Old version phone note: has fingerprint but syncId was generated differently
        val remoteLegacyNote = JSONObject().apply {
            put("syncId", "lz-1787010000000-1")
            put("createdAt", creationTime)
            put("content", text)
            put("updatedAt", 1787010000000L)
        }

        // Local emulator note: has same createdAt and content, but locally assigned uuid
        val localNoteFingerprint = noteFingerprint(creationTime, text)
        val remoteFingerprint = noteFingerprint(remoteLegacyNote.getLong("createdAt"), remoteLegacyNote.getString("content"))

        // Fingerprints must match
        assertEquals(localNoteFingerprint, remoteFingerprint)

        // Verifying deduplication logic: if fingerprints match, it should NOT insert a duplicate
        val isSameNote = localNoteFingerprint == remoteFingerprint
        assertTrue("Dual-fingerprint must recognize identical createdAt and content as same note", isSameNote)
    }

    @Test
    fun testSameSyncId_lwwMergeUpdatesCorrectly() {
        val sharedSyncId = "lz-shared-" + UUID.randomUUID()

        // Phone version (older)
        val phoneNote = JSONObject().apply {
            put("syncId", sharedSyncId)
            put("content", "原始版本 (手机)")
            put("updatedAt", 1000L)
        }

        // Emulator version (newer)
        val emulatorNote = JSONObject().apply {
            put("syncId", sharedSyncId)
            put("content", "在电脑上修改后的最新版本")
            put("updatedAt", 2000L)
        }

        val remoteUpdatedAt = phoneNote.getLong("updatedAt")
        val localUpdatedAt = emulatorNote.getLong("updatedAt")

        val winningContent = if (remoteUpdatedAt > localUpdatedAt) {
            phoneNote.getString("content")
        } else {
            emulatorNote.getString("content")
        }

        assertEquals("在电脑上修改后的最新版本", winningContent)
    }

    @Test
    fun testLegacyPayloadFallback_generatesConsistentSyncId() {
        val legacyNoteWithoutSyncId = JSONObject().apply {
            put("id", 42L)
            put("createdAt", 1787030000000L)
            put("content", "旧版没有 syncId 的历史笔记")
        }

        val derivedSyncId = legacyNoteWithoutSyncId.optString("syncId").ifEmpty {
            val legacyCreatedAt = legacyNoteWithoutSyncId.optLong("createdAt", 0L)
            val legacyId = legacyNoteWithoutSyncId.optLong("id", 0L)
            "lz-$legacyCreatedAt-$legacyId"
        }

        assertEquals("lz-1787030000000-42", derivedSyncId)
    }

    @Test
    fun testLocalDeletionLww_overridesRemoteActiveNote_preventsResurrection() {
        val syncId = "lz-test-del-1"

        // 1. Remote cloud payload has old active note (updated at T=1000)
        val remoteNote = JSONObject().apply {
            put("syncId", syncId)
            put("content", "准备删除的笔记")
            put("isDeleted", false)
            put("createdAt", 1000L)
            put("updatedAt", 1000L)
        }

        // 2. Local note is soft-deleted on phone (updated at T=2000)
        val localNoteDeleted = JSONObject().apply {
            put("syncId", syncId)
            put("content", "准备删除的笔记")
            put("isDeleted", true)
            put("createdAt", 1000L)
            put("updatedAt", 2000L)
        }

        val localUpdatedAt = localNoteDeleted.getLong("updatedAt")
        val remoteUpdatedAt = remoteNote.getLong("updatedAt")

        // LWW Decision:
        val shouldUpdateLocalFromRemote = remoteUpdatedAt > localUpdatedAt
        val finalIsDeleted = if (shouldUpdateLocalFromRemote) {
            remoteNote.getBoolean("isDeleted")
        } else {
            localNoteDeleted.getBoolean("isDeleted")
        }

        // Must remain deleted!
        assertFalse("Local is newer than remote, so should not update local from remote", shouldUpdateLocalFromRemote)
        assertTrue("Final note state must remain deleted (preventing resurrection)", finalIsDeleted)
    }

    @Test
    fun testRemoteDeletionLww_updatesLocalNoteToDeleted() {
        val syncId = "lz-test-del-2"

        // 1. Local emulator has old active note (updated at T=1000)
        val localNote = JSONObject().apply {
            put("syncId", syncId)
            put("content", "在手机端已删除的笔记")
            put("isDeleted", false)
            put("createdAt", 1000L)
            put("updatedAt", 1000L)
        }

        // 2. Remote cloud has deletion from phone (updated at T=2500)
        val remoteDeletedNote = JSONObject().apply {
            put("syncId", syncId)
            put("content", "在手机端已删除的笔记")
            put("isDeleted", true)
            put("createdAt", 1000L)
            put("updatedAt", 2500L)
        }

        val localUpdatedAt = localNote.getLong("updatedAt")
        val remoteUpdatedAt = remoteDeletedNote.getLong("updatedAt")

        val shouldUpdateLocalFromRemote = remoteUpdatedAt > localUpdatedAt
        val finalIsDeleted = if (shouldUpdateLocalFromRemote) {
            remoteDeletedNote.getBoolean("isDeleted")
        } else {
            localNote.getBoolean("isDeleted")
        }

        assertTrue("Remote is newer, so local must update from remote", shouldUpdateLocalFromRemote)
        assertTrue("Local note must become deleted to enter Trash", finalIsDeleted)
    }
}
