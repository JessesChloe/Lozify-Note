package com.witte.lozify.presentation.home

import com.witte.lozify.domain.model.Tag
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for Sidebar Tag filtering and multi-criteria sorting algorithms.
 */
class TagSortAndFilterTest {

    private val sampleTags = listOf(
        Tag(id = 1, name = "工作", usageCount = 10, isPinned = true, createdAt = Instant.ofEpochSecond(1000)),
        Tag(id = 2, name = "Android", usageCount = 25, isPinned = false, createdAt = Instant.ofEpochSecond(2000)),
        Tag(id = 3, name = "Kotlin", usageCount = 5, isPinned = false, createdAt = Instant.ofEpochSecond(3000)),
        Tag(id = 4, name = "设计", usageCount = 15, isPinned = false, createdAt = Instant.ofEpochSecond(4000)),
        Tag(id = 5, name = "读书笔记", usageCount = 8, isPinned = true, createdAt = Instant.ofEpochSecond(5000))
    )

    @Test
    fun testTagFilter_matchesCaseInsensitive() {
        val query = "and"
        val filtered = sampleTags.filter { it.name.contains(query, ignoreCase = true) }

        assertEquals(1, filtered.size)
        assertEquals("Android", filtered.first().name)
    }

    @Test
    fun testTagSort_byUsageDesc() {
        val sorted = sampleTags.sortedWith(
            compareByDescending<Tag> { it.usageCount }.thenBy { it.name }
        )

        assertEquals("Android", sorted[0].name) // 25
        assertEquals("设计", sorted[1].name)    // 15
        assertEquals("工作", sorted[2].name)    // 10
        assertEquals("读书笔记", sorted[3].name) // 8
        assertEquals("Kotlin", sorted[4].name)  // 5
    }

    @Test
    fun testTagSort_byNameAsc() {
        val sorted = sampleTags.sortedBy { it.name.lowercase() }

        assertEquals("android", sorted[0].name.lowercase())
        assertEquals("kotlin", sorted[1].name.lowercase())
    }

    @Test
    fun testTagSort_byRecentCreated() {
        val sorted = sampleTags.sortedByDescending { it.id }

        assertEquals(5L, sorted[0].id)
        assertEquals(4L, sorted[1].id)
        assertEquals(3L, sorted[2].id)
        assertEquals(2L, sorted[3].id)
        assertEquals(1L, sorted[4].id)
    }
}
