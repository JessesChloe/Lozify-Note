package com.witte.lozify.core.common

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * RichTextUtils - Utility for parsing and rendering rich text formatting.
 *
 * Stage 13 UI Refactor (Flomo Visual Alignment):
 * 1. Inline Tag Badges: Tags (#tag) are rendered as inline composable capsules using appendInlineContent.
 * 2. Relation Blocks: Mentions (@[text](note:id)) are stripped from body text and extracted for bottom relation cards.
 * 3. Markdown Formatting: Bold (**text**), Underline (__text__), Highlight (==text==), Checkbox (- [ ] / - [x]).
 */
object RichTextUtils {

    private val HighlightYellow = Color(0xFFFFF3C4)
    private val CheckboxGreen = Color(0xFF00C853)
    private val CheckboxGray = Color(0xFF9CA3AF)

    /**
     * Data class holding parsed rich text result.
     *
     * @param annotatedString Formatted text with inline content placeholders
     * @param tags Extracted unique tag names without # prefix
     * @param mentions Extracted mentions for bottom relation cards
     */
    data class ParsedRichText(
        val annotatedString: AnnotatedString,
        val tags: List<String>,
        val mentions: List<ExtractedMention>
    )

    /**
     * Data class representing an extracted mention relation.
     *
     * @param noteId ID of the referenced note
     * @param mentionText Display title/summary of the referenced note
     */
    data class ExtractedMention(
        val noteId: Long,
        val mentionText: String
    )

    /**
     * Calculate appropriate badge width in SP for an inline tag.
     */
    fun calculateTagBadgeWidth(tagName: String): TextUnit {
        val charWidthSum = tagName.sumOf { if (it.code > 127) 13.0 else 7.5 }
        val totalWidth = 8.0 + charWidthSum + 12.0
        return totalWidth.sp
    }

    /**
     * Parse rich text content into ParsedRichText.
     *
     * - Checkbox markers (- [ ] / - [x]) replaced with Unicode.
     * - Mentions (@[text](note:id)) stripped from body and collected into mentions list.
     * - Tags (#tagName) converted to inline content (appendInlineContent) and collected into tags list.
     * - Bold (**), Underline (__), Highlight (==) mapped to SpanStyles.
     */
    fun parseRichText(
        content: String,
        tagColor: Color = Color(0xFF1A73E8),
        onTagClick: ((String) -> Unit)? = null
    ): ParsedRichText {
        var processedContent = content

        // Step 1: Replace checkbox markers with Unicode symbols
        val checkboxPattern = Regex("""^- \[([ x])\]""", RegexOption.MULTILINE)
        processedContent = processedContent.replace(checkboxPattern) { matchResult ->
            when (matchResult.groupValues[1]) {
                "x" -> "☑"  // Checked
                else -> "☐" // Unchecked
            }
        }

        // Step 2: Extract and strip all @[text](note:id) mentions from body text
        val extractedMentions = mutableListOf<ExtractedMention>()
        val mentionRegex = Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:(\d+)\)""")
        mentionRegex.findAll(processedContent).forEach { match ->
            val text = match.groupValues[1]
            val noteId = match.groupValues[2].toLongOrNull()
            if (noteId != null) {
                extractedMentions.add(ExtractedMention(noteId, text))
            }
        }

        // Replace mentions first so body text is clean
        val contentWithoutMentions = processedContent.replace(mentionRegex, "").trimEnd()

        val extractedTags = mutableListOf<String>()
        val builder = AnnotatedString.Builder()
        val markerPositions = mutableListOf<MarkerPosition>()

        // Find bold markers ((?s) enables DOTALL mode)
        Regex("""\*\*(?s)(.+?)\*\*""").findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.BOLD))
        }

        // Find underline markers ((?s) enables DOTALL mode)
        Regex("""__(?s)(.+?)__""").findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.UNDERLINE))
        }

        // Find highlight markers ((?s) enables DOTALL mode)
        Regex("""==(?s)(.+?)==""").findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.HIGHLIGHT))
        }

        // Find tags (#tagName) - Supports Chinese characters directly preceding # without space, while preventing URL anchors (page#sec)
        val tagRegex = Regex("""(?<![a-zA-Z0-9])#[a-zA-Z0-9\u4e00-\u9fa5_]+""")
        tagRegex.findAll(contentWithoutMentions).forEach { match ->
            val tagName = match.value.substring(1) // remove #
            extractedTags.add(tagName)
            markerPositions.add(MarkerPosition(
                start = match.range.first,
                end = match.range.last + 1,
                type = MarkerType.TAG,
                tagName = tagName
            ))
        }

        // Sort structural markers by start position
        val structuralMarkers = markerPositions
            .filter { it.type == MarkerType.OPEN || it.type == MarkerType.CLOSE || it.type == MarkerType.TAG }
            .sortedBy { it.start }

        val positionMap = mutableMapOf<Int, Int>() // original pos -> builder length
        var cursor = 0

        while (cursor < contentWithoutMentions.length) {
            positionMap[cursor] = builder.length

            val marker = structuralMarkers.firstOrNull { it.start == cursor }
            if (marker != null) {
                when (marker.type) {
                    MarkerType.OPEN, MarkerType.CLOSE -> {
                        for (k in cursor until marker.end) {
                            positionMap[k] = builder.length
                        }
                        cursor = marker.end
                    }
                    MarkerType.TAG -> {
                        val tagName = marker.tagName ?: ""
                        val tagStartIndex = builder.length
                        builder.appendInlineContent(id = "tag_$tagName", alternateText = "\uFFFD")
                        val tagEndIndex = builder.length
                        // Fallback SpanStyle for inline tag badge
                        builder.addStyle(
                            style = SpanStyle(
                                color = tagColor,
                                fontWeight = FontWeight.Medium
                            ),
                            start = tagStartIndex,
                            end = tagEndIndex
                        )
                        for (k in cursor until marker.end) {
                            positionMap[k] = builder.length
                        }
                        cursor = marker.end
                    }
                    else -> {
                        builder.append(contentWithoutMentions[cursor])
                        cursor++
                    }
                }
            } else {
                builder.append(contentWithoutMentions[cursor])
                cursor++
            }
        }
        positionMap[contentWithoutMentions.length] = builder.length

        // Apply formatting styles (Bold, Underline, Highlight)
        val styleMarkers = markerPositions.filter { it.type == MarkerType.STYLE }
        styleMarkers.forEach { marker ->
            val cleanStart = positionMap[marker.start] ?: 0
            val mappedEnd = positionMap[marker.end] ?: builder.length
            val cleanEnd = minOf(mappedEnd, builder.length)

            if (cleanStart >= 0 && cleanStart < cleanEnd && cleanEnd <= builder.length) {
                val style = when (marker.formatType) {
                    FormatType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    FormatType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                    FormatType.HIGHLIGHT -> SpanStyle(background = HighlightYellow)
                    else -> null
                }
                style?.let { builder.addStyle(it, cleanStart, cleanEnd) }
            }
        }

        // Apply style to checkbox symbols
        val finalText = builder.toAnnotatedString()
        val checkboxSymbolPattern = Regex("""[☐☑]""")
        checkboxSymbolPattern.findAll(finalText.text).forEach { match ->
            val isChecked = match.value == "☑"
            builder.addStyle(
                style = SpanStyle(
                    color = if (isChecked) CheckboxGreen else CheckboxGray,
                    fontWeight = FontWeight.Bold
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        return ParsedRichText(
            annotatedString = builder.toAnnotatedString(),
            tags = extractedTags.distinct(),
            mentions = extractedMentions
        )
    }

    /**
     * Backward-compatible builder for AnnotatedString.
     */
    fun buildAnnotatedStringWithFormatting(
        content: String,
        tagColor: Color = Color(0xFF1A73E8),
        mentionColor: Color = Color(0xFF7C4DFF),
        onTagClick: ((String) -> Unit)? = null,
        onMentionClick: ((Long) -> Unit)? = null,
        onPlainMentionClick: ((String) -> Unit)? = null
    ): AnnotatedString {
        return parseRichText(content, tagColor, onTagClick).annotatedString
    }

    // Helper data classes for marker parsing
    private data class MarkerPosition(
        val start: Int,
        val end: Int,
        val type: MarkerType,
        val formatType: FormatType? = null,
        val tagName: String? = null,
        val noteId: Long? = null
    )

    private enum class MarkerType {
        OPEN,
        CLOSE,
        STYLE,
        TAG
    }

    /**
     * Get tag name at specific text offset.
     *
     * Used for handling tag clicks in ClickableText.
     *
     * @param annotatedString AnnotatedString with tag annotations
     * @param offset Click offset position
     * @return Tag name without # prefix, or null if no tag at offset
     */
    fun getTagAtOffset(annotatedString: AnnotatedString, offset: Int): String? {
        return annotatedString
            .getStringAnnotations(tag = "TAG", start = offset, end = offset)
            .firstOrNull()
            ?.item
    }

    /**
     * Get mention note ID at specific text offset.
     *
     * Stage 8: Used for handling @mention clicks in ClickableText.
     *
     * @param annotatedString AnnotatedString with mention annotations
     * @param offset Click offset position
     * @return Note ID of the mentioned note, or null if no mention at offset
     */
    fun getMentionAtOffset(annotatedString: AnnotatedString, offset: Int): Long? {
        return annotatedString
            .getStringAnnotations(tag = "MENTION", start = offset, end = offset)
            .firstOrNull()
            ?.item
            ?.toLongOrNull()
    }

    /**
     * Get plain @mention name at specific text offset.
     *
     * @param annotatedString AnnotatedString with plain mention annotations
     * @param offset Click offset position
     * @return Mention name without @ prefix, or null if no mention at offset
     */
    fun getPlainMentionAtOffset(annotatedString: AnnotatedString, offset: Int): String? {
        return annotatedString
            .getStringAnnotations(tag = "PLAIN_MENTION", start = offset, end = offset)
            .firstOrNull()
            ?.item
    }

    /**
     * Insert formatting markers at cursor position.
     *
     * Used by editor toolbar buttons to wrap selected text.
     *
     * @param content Current text content
     * @param selectionStart Selection start index
     * @param selectionEnd Selection end index
     * @param formatType Formatting type (BOLD, UNDERLINE, HIGHLIGHT)
     * @return Modified content with markers inserted
     */
    fun insertFormatting(
        content: String,
        selectionStart: Int,
        selectionEnd: Int,
        formatType: FormatType
    ): String {
        // Safety: normalize selection range to prevent crashes
        val safeStart = minOf(selectionStart, selectionEnd).coerceIn(0, content.length)
        val safeEnd = maxOf(selectionStart, selectionEnd).coerceIn(0, content.length)

        val (prefix, suffix) = when (formatType) {
            FormatType.BOLD -> "**" to "**"
            FormatType.UNDERLINE -> "__" to "__"
            FormatType.HIGHLIGHT -> "==" to "=="
            FormatType.MENTION -> "@[" to "](note:0)"  // Not used in toolbar, placeholder only
            FormatType.CHECKBOX_UNCHECKED -> "- [ ] " to ""
            FormatType.CHECKBOX_CHECKED -> "- [x] " to ""
        }

        return if (safeStart == safeEnd) {
            // No selection: insert markers and place cursor between them
            content.substring(0, safeStart) +
                    prefix + suffix +
                    content.substring(safeStart)
        } else {
            // Has selection: wrap selected text
            content.substring(0, safeStart) +
                    prefix +
                    content.substring(safeStart, safeEnd) +
                    suffix +
                    content.substring(safeEnd)
        }
    }

    /**
     * Remove all formatting markers from content.
     *
     * Used for plain text export or clipboard copy.
     *
     * Stage 8: Added @mention marker stripping.
     * Bug Fix: Use stricter regex to handle brackets inside mention text.
     *
     * @param content Formatted text with Markdown markers
     * @return Plain text without markers
     */
    fun stripFormatting(content: String): String {
        return content
            .replace(Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:\d+\)"""), "$1")  // @mention → text only
            .replace(Regex("""\*\*(?s)(.+?)\*\*"""), "$1")  // Bold (DOTALL mode)
            .replace(Regex("""__(?s)(.+?)__"""), "$1")      // Underline (DOTALL mode)
            .replace(Regex("""==(?s)(.+?)=="""), "$1")      // Highlight (DOTALL mode)
            .replace(Regex("""^\s*- \[([ x])\] """, RegexOption.MULTILINE), "")  // Checkbox with optional leading spaces
            .trim()  // Remove leading/trailing whitespace
    }

    /**
     * Count formatting markers in content.
     *
     * Used for analytics or debugging.
     *
     * Stage 8: Added @mention counting.
     * Bug Fix: Use stricter regex to handle brackets inside mention text.
     */
    fun countFormatting(content: String): Map<FormatType, Int> {
        return mapOf(
            FormatType.BOLD to Regex("""\*\*(?s)(.+?)\*\*""").findAll(content).count(),
            FormatType.UNDERLINE to Regex("""__(?s)(.+?)__""").findAll(content).count(),
            FormatType.HIGHLIGHT to Regex("""==(?s)(.+?)==""").findAll(content).count(),
            FormatType.MENTION to Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:\d+\)""").findAll(content).count(),
            FormatType.CHECKBOX_UNCHECKED to Regex("""^- \[ \]""", RegexOption.MULTILINE).findAll(content).count(),
            FormatType.CHECKBOX_CHECKED to Regex("""^- \[x\]""", RegexOption.MULTILINE).findAll(content).count()
        )
    }

    /**
     * Extract all @mentions from content and return list of (noteId, mentionText).
     *
     * Stage 8: Used by EditorViewModel to save NoteRelation records.
     * Bug Fix: Use stricter regex to handle brackets inside mention text.
     *
     * @param content Raw note content with @mention markers
     * @return List of pairs (noteId, mentionText) for all mentions found
     */
    fun extractMentionsFromContent(content: String): List<Pair<Long, String>> {
        val mentionPattern = Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:(\d+)\)""")
        return mentionPattern.findAll(content).mapNotNull { match ->
            val mentionText = match.groupValues[1]
            val noteId = match.groupValues[2].toLongOrNull()
            if (noteId != null) {
                noteId to mentionText
            } else {
                null
            }
        }.toList()
    }

    enum class FormatType {
        BOLD,
        UNDERLINE,
        HIGHLIGHT,
        MENTION,
        CHECKBOX_UNCHECKED,
        CHECKBOX_CHECKED
    }
}
