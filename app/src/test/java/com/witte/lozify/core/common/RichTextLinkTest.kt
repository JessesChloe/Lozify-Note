package com.witte.lozify.core.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Stage 33: URL link recognition, LinkBlue styling, punctuation trimming,
 * and tag isolation.
 */
class RichTextLinkTest {

    @Test
    fun testStandardHttpAndHttpsUrls_recognizedAndStyledAsLinkBlue() {
        val content = "欢迎访问 https://github.com/JessesChloe/Lozify-Note 以及 http://lozify.app"
        val parsed = RichTextUtils.parseRichText(content)

        val urlAnnotations = parsed.annotatedString.getStringAnnotations("URL", 0, parsed.annotatedString.length)
        assertEquals("Should extract exactly 2 URLs", 2, urlAnnotations.size)
        assertEquals("https://github.com/JessesChloe/Lozify-Note", urlAnnotations[0].item)
        assertEquals("http://lozify.app", urlAnnotations[1].item)

        // Verify getUrlAtOffset
        val offsetInsideFirstUrl = urlAnnotations[0].start + 5
        val retrievedUrl = RichTextUtils.getUrlAtOffset(parsed.annotatedString, offsetInsideFirstUrl)
        assertEquals("https://github.com/JessesChloe/Lozify-Note", retrievedUrl)

        // Verify offset outside URL returns null
        val offsetOutside = 0
        assertNull(RichTextUtils.getUrlAtOffset(parsed.annotatedString, offsetOutside))
    }

    @Test
    fun testWwwUrls_recognizedCorrectly() {
        val content = "请查看 www.bilibili.com/video/BV12345 视频"
        val parsed = RichTextUtils.parseRichText(content)

        val urlAnnotations = parsed.annotatedString.getStringAnnotations("URL", 0, parsed.annotatedString.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("www.bilibili.com/video/BV12345", urlAnnotations[0].item)
    }

    @Test
    fun testChinesePunctuationTrimming_removesTrailingPunctuation() {
        val content = "项目源码在 https://gitee.com/muxsans/lozify-note。欢迎体验！以及 (https://example.com/api), 敬请期待！"
        val parsed = RichTextUtils.parseRichText(content)

        val urlAnnotations = parsed.annotatedString.getStringAnnotations("URL", 0, parsed.annotatedString.length)
        assertEquals(2, urlAnnotations.size)

        // Must not contain trailing '。'
        assertEquals("https://gitee.com/muxsans/lozify-note", urlAnnotations[0].item)
        assertFalse(urlAnnotations[0].item.endsWith("。"))

        // Must not contain trailing ')' or ','
        assertEquals("https://example.com/api", urlAnnotations[1].item)
        assertFalse(urlAnnotations[1].item.endsWith(")"))
        assertFalse(urlAnnotations[1].item.endsWith(","))
    }

    @Test
    fun testUrlAnchorVsTagIsolation_doesNotConflict() {
        val content = "文档在 https://example.com/docs#intro 查阅，关联 #开发心得 标签"
        val parsed = RichTextUtils.parseRichText(content)

        // Verify URL is intact with #intro
        val urlAnnotations = parsed.annotatedString.getStringAnnotations("URL", 0, parsed.annotatedString.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://example.com/docs#intro", urlAnnotations[0].item)

        // Verify tags list only contains "开发心得", and does NOT extract "intro" as a tag
        assertEquals(1, parsed.tags.size)
        assertEquals("开发心得", parsed.tags[0])
    }

    @Test
    fun testMarkdownVisualTransformation_appliesLinkBlueToUrls() {
        val transformation = MarkdownVisualTransformation()
        val raw = AnnotatedString("访问 https://lozify.org 获取最新版本")
        val transformed = transformation.filter(raw)

        val spanStyles = transformed.text.spanStyles
        val hasLinkBlue = spanStyles.any {
            it.item.color == RichTextUtils.LinkBlue && it.item.textDecoration == TextDecoration.Underline
        }

        assertTrue("VisualTransformation must style URL with LinkBlue and Underline", hasLinkBlue)
    }
}
