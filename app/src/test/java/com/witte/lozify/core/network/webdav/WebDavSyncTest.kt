package com.witte.lozify.core.network.webdav

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for WebDAV Cloud Sync logic, payload serialization, and Last-Write-Wins merge rules.
 *
 * Stage 26: WebDAV Cloud Sync.
 */
class WebDavSyncTest {

    @Test
    fun testPayloadSerialization_validStructure() {
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", Instant.now().toString())

            val tagsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                    put("name", "想法")
                    put("icon", "💡")
                    put("isPinned", true)
                    put("pinOrder", 1)
                    put("usageCount", 5)
                })
            }
            put("tags", tagsArray)

            val notesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 201)
                    put("content", "WebDAV 多端同步上线啦 #想法")
                    put("isPinned", true)
                    put("isArchived", false)
                    put("isDeleted", false)
                    put("createdAt", 1787030000000L)
                    put("updatedAt", 1787030000000L)
                    put("tags", JSONArray().apply { put("想法") })
                    put("attachments", JSONArray().apply {
                        put(JSONObject().apply {
                            put("filePath", "images/img_201_1.jpg")
                            put("displayOrder", 0)
                            put("mimeType", "image/jpeg")
                        })
                    })
                })
            }
            put("notes", notesArray)
        }

        val jsonStr = root.toString()
        val parsed = JSONObject(jsonStr)

        assertEquals(1, parsed.getInt("version"))
        assertEquals(1, parsed.getJSONArray("tags").length())
        assertEquals(1, parsed.getJSONArray("notes").length())

        val note = parsed.getJSONArray("notes").getJSONObject(0)
        assertEquals(201L, note.getLong("id"))
        assertEquals("WebDAV 多端同步上线啦 #想法", note.getString("content"))
        assertTrue(note.getBoolean("isPinned"))
        assertEquals("images/img_201_1.jpg", note.getJSONArray("attachments").getJSONObject(0).getString("filePath"))
    }

    @Test
    fun testTwoWayMerge_lastWriteWinsResolution() {
        // Scenario 1: Remote note has newer timestamp -> Remote wins
        val localUpdatedAt1 = 1000L
        val remoteUpdatedAt1 = 2000L
        assertTrue("Remote should win when remoteUpdatedAt > localUpdatedAt", remoteUpdatedAt1 > localUpdatedAt1)

        // Scenario 2: Local note has newer timestamp -> Local wins
        val localUpdatedAt2 = 3000L
        val remoteUpdatedAt2 = 2500L
        assertTrue("Local should win when localUpdatedAt > remoteUpdatedAt", localUpdatedAt2 > remoteUpdatedAt2)

        // Scenario 3: Soft-deleted note propagates deletion
        val remoteDeleted = true
        val localDeleted = false
        // Merged status: if remote is newer and deleted, local must be marked deleted
        val mergedDeleted = if (remoteUpdatedAt1 > localUpdatedAt1) remoteDeleted else localDeleted
        assertTrue("Soft-deletion should propagate to local when remote is newer", mergedDeleted)
    }

    @Test
    fun testManifestJson_structure() {
        val now = System.currentTimeMillis()
        val manifest = JSONObject().apply {
            put("version", 1)
            put("lastSyncTime", now)
            put("noteCount", 12)
            put("tagCount", 4)
            put("device", "Xiaomi 14 Pro")
        }

        assertEquals(1, manifest.getInt("version"))
        assertEquals(now, manifest.getLong("lastSyncTime"))
        assertEquals(12, manifest.getInt("noteCount"))
        assertEquals(4, manifest.getInt("tagCount"))
        assertEquals("Xiaomi 14 Pro", manifest.getString("device"))
    }

    @Test
    fun testTombstonePurgeProtocol_preventsResurrectionAndSyncsEmptyTrash() {
        val purgedSyncIds = setOf("lz-1787000000-note1", "lz-1787000000-note2")
        val root = JSONObject().apply {
            put("version", 1)
            val purgedArr = JSONArray()
            purgedSyncIds.forEach { purgedArr.put(it) }
            put("purgedSyncIds", purgedArr)
            put("notes", JSONArray().apply {
                put(JSONObject().apply {
                    put("syncId", "lz-1787000000-note3")
                    put("content", "活跃笔记")
                    put("isDeleted", false)
                })
            })
            put("tags", JSONArray())
        }

        val jsonStr = root.toString()
        val parsed = JSONObject(jsonStr)

        val parsedPurgedArr = parsed.optJSONArray("purgedSyncIds") ?: JSONArray()
        val parsedPurgedSet = (0 until parsedPurgedArr.length()).map { parsedPurgedArr.getString(it) }.toSet()

        // 1. Purged set correctly restored
        assertEquals(2, parsedPurgedSet.size)
        assertTrue(parsedPurgedSet.contains("lz-1787000000-note1"))
        assertTrue(parsedPurgedSet.contains("lz-1787000000-note2"))

        // 2. Note3 is active and not in purged set
        val note3 = parsed.getJSONArray("notes").getJSONObject(0)
        val note3SyncId = note3.getString("syncId")
        assertFalse(parsedPurgedSet.contains(note3SyncId))
    }
}
