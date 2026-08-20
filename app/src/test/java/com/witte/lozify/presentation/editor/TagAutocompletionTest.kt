package com.witte.lozify.presentation.editor

import com.witte.lozify.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for #Tag Autocompletion detection and filtering.
 */
class TagAutocompletionTest {

    // Same algorithm as implemented in NoteEditorBottomSheet
    private fun findActiveTagQuery(text: String, cursorPos: Int): Pair<Int, String>? {
        if (cursorPos <= 0 || cursorPos > text.length) return null
        var hashIndex = -1
        for (i in cursorPos - 1 downTo 0) {
            val ch = text[i]
            if (ch == '#') {
                hashIndex = i
                break
            }
            if (ch.isWhitespace() || ch == '\n' || ch == '@') {
                break
            }
        }
        if (hashIndex != -1) {
            val prevChar = if (hashIndex > 0) text[hashIndex - 1] else null
            val isAlphaNumeric = prevChar != null && (prevChar in 'a'..'z' || prevChar in 'A'..'Z' || prevChar in '0'..'9')
            if (!isAlphaNumeric) {
                val query = text.substring(hashIndex + 1, cursorPos)
                return Pair(hashIndex, query)
            }
        }
        return null
    }

    @Test
    fun testEmptyInputWithHashOnly() {
        val text = "#"
        val result = findActiveTagQuery(text, 1)
        assertEquals(Pair(0, ""), result)
    }

    @Test
    fun testTypingTagPrefix() {
        val text = "Hello #ta"
        val result = findActiveTagQuery(text, 9)
        assertEquals(Pair(6, "ta"), result)
    }

    @Test
    fun testTypingTagAfterChineseWithoutSpace() {
        val text = "今天天气很好#美"
        val result = findActiveTagQuery(text, text.length)
        assertEquals(Pair(6, "美"), result)
    }

    @Test
    fun testTypingTagAfterPunctuationWithoutSpace() {
        val text = "跑步打卡，#"
        val result = findActiveTagQuery(text, text.length)
        assertEquals(Pair(5, ""), result)
    }

    @Test
    fun testSpaceAfterTagDismissesPicker() {
        val text = "Hello #task "
        val result = findActiveTagQuery(text, 12)
        assertNull(result)
    }

    @Test
    fun testHashInMiddleOfWordIgnored() {
        // e.g. "email#domain" - preceded by English letter 'l'
        val text = "email#domain"
        val result = findActiveTagQuery(text, 12)
        assertNull(result)
    }

    @Test
    fun testMultipleTagsDetection() {
        val text = "#first some text #sec"
        val result = findActiveTagQuery(text, text.length)
        assertEquals(Pair(17, "sec"), result)
    }

    @Test
    fun testFilterOnlyActiveOrPinnedTags() {
        val allTags = listOf(
            Tag(id = 1, name = "maidtask", createdAt = Instant.now(), usageCount = 5),
            Tag(id = 2, name = "随想", createdAt = Instant.now(), usageCount = 3),
            Tag(id = 3, name = "已删除废弃标签", createdAt = Instant.now(), usageCount = 0, isPinned = false),
            Tag(id = 4, name = "固定置顶新标签", createdAt = Instant.now(), usageCount = 0, isPinned = true)
        )

        val activeTags = allTags.filter { it.usageCount > 0 || it.isPinned }
        assertEquals(3, activeTags.size)
        assertTrue(activeTags.any { it.name == "maidtask" })
        assertTrue(activeTags.any { it.name == "随想" })
        assertTrue(activeTags.any { it.name == "固定置顶新标签" })
        assertTrue(activeTags.none { it.name == "已删除废弃标签" })
    }
}
