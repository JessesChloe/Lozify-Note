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

    @Test
    fun testParseRichText_tagDirectlyAfterEnglish() {
        val input = "project#backend and task#urgent"
        val parsed = RichTextUtils.parseRichText(input)

        assertEquals(listOf("backend", "urgent"), parsed.tags)
    }

    @Test
    fun testParseRichText_urlAnchorPreservedNotExtractedAsTag() {
        val input = "查看文档 https://developer.android.com/guide#activity 详情"
        val parsed = RichTextUtils.parseRichText(input)

        // The #activity inside the URL must NOT be extracted as a tag
        assertTrue(parsed.tags.isEmpty())
    }

    @Test
    fun testGetCleanSummary_stripsMentionsAndMarkdown() {
        val input = "节点2@[节点1](note:22) #工作 **加粗** ==高亮=="
        val summary = RichTextUtils.getCleanSummary(input, 30)

        // Mentions are completely stripped to preserve pure body text
        assertEquals("节点2 #工作 加粗 高亮", summary)
    }

    @Test
    fun testGetCleanSummary_handlesNestedMentionsGracefully() {
        val input = "节点3@[节点2@[节点1](note:22)](note:23)"
        val summary = RichTextUtils.getCleanSummary(input, 50)

        // Must NOT contain any brackets, parentheses, or raw note: tokens, and keeps pure body
        assertFalse(summary.contains("["))
        assertFalse(summary.contains("]"))
        assertFalse(summary.contains("note:"))
        assertEquals("节点3", summary)
    }

    @Test
    fun testGetCleanSummary_pureMentionFallback() {
        val input = "@[会议纪要](note:10)"
        val summary = RichTextUtils.getCleanSummary(input, 50)

        assertEquals("会议纪要", summary)
    }

    @Test
    fun testGetCleanSummary_multilineAndLengthLimit() {
        val input = "第一行内容\n第二行内容非常非常长超过设定的最大限制"
        val summary = RichTextUtils.getCleanSummary(input, 10)

        assertEquals("第一行内容 第二行内...", summary)
    }

    @Test
    fun testApplyToggleableFormatting_firstClickInsertsQuadMarkers() {
        // 1st click on empty text: "" with cursor at 0
        val result = RichTextUtils.applyToggleableFormatting("", 0, 0, RichTextUtils.FormatType.BOLD)

        assertEquals("****", result.newText)
        assertEquals(2, result.newCursorStart)
    }

    @Test
    fun testApplyToggleableFormatting_secondClickTogglesOffEmptyMarkers() {
        // 2nd click when cursor is inside empty bold: "**|**" (cursor at 2)
        val result = RichTextUtils.applyToggleableFormatting("****", 2, 2, RichTextUtils.FormatType.BOLD)

        // Should cleanly toggle off and remove the empty "****", NOT create "************"
        assertEquals("", result.newText)
        assertEquals(0, result.newCursorStart)
    }

    @Test
    fun testApplyToggleableFormatting_clickInsideFormattedTextJumpsOut() {
        // Cursor at end of bold content: "**Hello|**" (cursor at 7)
        val result = RichTextUtils.applyToggleableFormatting("**Hello**", 7, 7, RichTextUtils.FormatType.BOLD)

        // Should jump past "**" to "**Hello**|" (cursor at 9)
        assertEquals("**Hello**", result.newText)
        assertEquals(9, result.newCursorStart)
    }

    @Test
    fun testApplyToggleableFormatting_wrapAndUnwrapSelection() {
        // 1. Wrap unformatted selected text "Hello"
        val wrapResult = RichTextUtils.applyToggleableFormatting("Hello world", 0, 5, RichTextUtils.FormatType.BOLD)
        assertEquals("**Hello** world", wrapResult.newText)
        assertEquals(0, wrapResult.newCursorStart)
        assertEquals(9, wrapResult.newCursorEnd)

        // 2. Unwrap already formatted selected text "**Hello**"
        val unwrapResult = RichTextUtils.applyToggleableFormatting("**Hello** world", 0, 9, RichTextUtils.FormatType.BOLD)
        assertEquals("Hello world", unwrapResult.newText)
        assertEquals(0, unwrapResult.newCursorStart)
        assertEquals(5, unwrapResult.newCursorEnd)
    }
}

