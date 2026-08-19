package com.witte.lozify.core.common

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RichTextUtils formatting, stripping, mention extraction and search highlighting.
 */
class RichTextUtilsTest {

    @Test
    fun testStripFormatting_removesAllMarkdownMarkers() {
        val input = "**加粗** 和 __下划线__ 以及 ==高亮== 和 - [ ] 待办 和 - [x] 完成"
        val expected = "加粗 和 下划线 以及 高亮 和 待办 和 完成"
        val actual = RichTextUtils.stripFormatting(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testStripFormatting_removesMentions() {
        val input = "参考笔记 @[项目规划方案](note:101) 中的结论"
        val expected = "参考笔记 项目规划方案 中的结论"
        val actual = RichTextUtils.stripFormatting(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testExtractMentionsFromContent_extractsMultipleMentions() {
        val input = "有关内容请参阅 @[架构设计](note:12) 以及后续的 @[发布清单](note:34)。"
        val mentions = RichTextUtils.extractMentionsFromContent(input)

        assertEquals(2, mentions.size)
        assertEquals(12L, mentions[0].first)
        assertEquals("架构设计", mentions[0].second)
        assertEquals(34L, mentions[1].first)
        assertEquals("发布清单", mentions[1].second)
    }

    @Test
    fun testCountFormatting_returnsAccurateCounts() {
        val input = """
            **粗体1** **粗体2**
            __下划线__
            ==高亮1== ==高亮2== ==高亮3==
            @[引用](note:1)
            - [ ] 待办1
            - [x] 完成1
        """.trimIndent()

        val counts = RichTextUtils.countFormatting(input)
        assertEquals(2, counts[RichTextUtils.FormatType.BOLD])
        assertEquals(1, counts[RichTextUtils.FormatType.UNDERLINE])
        assertEquals(3, counts[RichTextUtils.FormatType.HIGHLIGHT])
        assertEquals(1, counts[RichTextUtils.FormatType.MENTION])
        assertEquals(1, counts[RichTextUtils.FormatType.CHECKBOX_UNCHECKED])
        assertEquals(1, counts[RichTextUtils.FormatType.CHECKBOX_CHECKED])
    }

    @Test
    fun testInsertFormatting_wrapsSelectedText() {
        val original = "Hello World"
        // Wrap "World" (start: 6, end: 11) with Bold
        val result = RichTextUtils.insertFormatting(original, 6, 11, RichTextUtils.FormatType.BOLD)
        assertEquals("Hello **World**", result)
    }

    @Test
    fun testInsertFormatting_emptyCursorInsertsPair() {
        val original = "Hello "
        val result = RichTextUtils.insertFormatting(original, 6, 6, RichTextUtils.FormatType.HIGHLIGHT)
        assertEquals("Hello ====", result)
    }

    @Test
    fun testParseRichText_extractsTagsAndCheckboxSymbols() {
        val input = "今日计划 #工作 #学习\n- [ ] 写代码\n- [x] 读书"
        val parsed = RichTextUtils.parseRichText(input)

        assertEquals(listOf("工作", "学习"), parsed.tags)
        assertTrue(parsed.annotatedString.text.contains("☐ 写代码"))
        assertTrue(parsed.annotatedString.text.contains("☑ 读书"))
    }

    @Test
    fun testParseRichText_convertsListPrefixToBullets() {
        val input = "今日备忘:\n- 第一项任务\n- 第二项任务"
        val parsed = RichTextUtils.parseRichText(input)

        assertTrue(parsed.annotatedString.text.contains("• 第一项任务"))
        assertTrue(parsed.annotatedString.text.contains("• 第二项任务"))
    }

    @Test
    fun testParseRichText_lruCacheReturnsSameInstance() {
        RichTextUtils.clearCache()
        val input = "缓存测试笔记 #测试内容 **加粗** @[关联](note:88)"
        val firstResult = RichTextUtils.parseRichText(input)
        val secondResult = RichTextUtils.parseRichText(input)

        // Verifying cache hit returns identical cached object instance
        assertSame(firstResult, secondResult)

        RichTextUtils.clearCache()
        val thirdResult = RichTextUtils.parseRichText(input)
        assertEquals(firstResult.tags, thirdResult.tags)
        assertEquals(firstResult.mentions, thirdResult.mentions)
        assertEquals(firstResult.annotatedString.text, thirdResult.annotatedString.text)
    }
}
