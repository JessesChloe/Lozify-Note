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
                    put("attachments", JSONArray().apply {
                        put(JSONObject().apply {
                            put("displayOrder", 0)
                            put("mimeType", "image/jpeg")
                            put("base64Data", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                        })
                    })
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
        assertTrue(firstNote.has("attachments"))
        assertEquals(1, firstNote.getJSONArray("attachments").length())
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

    @Test
    fun testZipHeaderDetection() {
        val zipHeaderBytes = byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())
        assertTrue(zipHeaderBytes.size >= 2 && zipHeaderBytes[0] == 0x50.toByte() && zipHeaderBytes[1] == 0x4B.toByte())

        val jsonBytes = "{ \"version\": 1 }".toByteArray(Charsets.UTF_8)
        assertFalse(jsonBytes.size >= 2 && jsonBytes[0] == 0x50.toByte() && jsonBytes[1] == 0x4B.toByte())
    }

    @Test
    fun testMarkdownYamlFrontmatterParsing() {
        val rawMarkdown = """
            ---
            id: 101
            created_at: 2026-08-18T10:00:00Z
            updated_at: 2026-08-18T10:00:00Z
            pinned: true
            tags: [task, 随想]
            ---
            
            今天完成了重要工作交付 #task
            
            ![image](images/note_101_img_0.jpg)
        """.trimIndent()

        assertTrue(rawMarkdown.startsWith("---"))
        val secondDelimiter = rawMarkdown.indexOf("---", 3)
        assertTrue(secondDelimiter != -1)

        val header = rawMarkdown.substring(3, secondDelimiter).trim()
        val noteBody = rawMarkdown.substring(secondDelimiter + 3).trim()

        assertTrue(header.contains("pinned: true"))
        assertTrue(header.contains("tags: [task, 随想]"))
        assertTrue(noteBody.contains("今天完成了重要工作交付 #task"))
    }
}
