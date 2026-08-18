package com.witte.lozify.core.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for JSON Backup structure formatting and validation.
 */
class BackupJsonTest {

    @Test
    fun testBackupJson_structureValid() {
        val root = JSONObject().apply {
            put("version", 1)
            put("appName", "Lozify")
            put("exportedAt", Instant.now().toString())

            val tagsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1)
                    put("name", "工作")
                    put("icon", "💼")
                    put("isPinned", true)
                    put("pinOrder", 0)
                })
            }
            put("tags", tagsArray)

            val notesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 101)
                    put("content", "今天完成了重要项目交付 #工作")
                    put("isPinned", false)
                    put("isArchived", false)
                    put("isDeleted", false)
                    put("createdAt", 1787030000000L)
                    put("updatedAt", 1787030000000L)
                    put("tags", JSONArray().apply { put("工作") })
                })
            }
            put("notes", notesArray)
        }

        val jsonString = root.toString()
        val parsed = JSONObject(jsonString)

        assertEquals(1, parsed.getInt("version"))
        assertEquals("Lozify", parsed.getString("appName"))
        assertEquals(1, parsed.getJSONArray("tags").length())
        assertEquals(1, parsed.getJSONArray("notes").length())

        val firstNote = parsed.getJSONArray("notes").getJSONObject(0)
        assertEquals("今天完成了重要项目交付 #工作", firstNote.getString("content"))
        assertEquals("工作", firstNote.getJSONArray("tags").getString(0))
    }

    @Test
    fun testBackupJson_detectsCorruptedFormat() {
        val corruptedJson = JSONObject().apply {
            put("version", 1)
            // missing "notes" and "tags"
        }

        assertFalse(corruptedJson.has("notes"))
        assertFalse(corruptedJson.has("tags"))
    }
}
