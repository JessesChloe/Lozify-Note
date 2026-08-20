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
    val LinkBlue = Color(0xFF2563EB) // Stage 33: High-contrast hyperlink blue

    // Precompiled static Regex patterns to avoid runtime compilation during list scrolling
    private val LIST_PATTERN = Regex("""(?m)^\s*-\s+(?!\[[ x]\])""")
    private val CHECKBOX_PATTERN = Regex("""^- \[([ x])\]""", RegexOption.MULTILINE)
    private val MENTION_REGEX = Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:(\d+)\)""")
    private val BOLD_REGEX = Regex("""\*\*(?s)(.+?)\*\*""")
    private val UNDERLINE_REGEX = Regex("""__(?s)(.+?)__""")
    private val HIGHLIGHT_REGEX = Regex("""==(?s)(.+?)==""")
    private val TAG_REGEX = Regex("""#[a-zA-Z0-9\u4e00-\u9fa5_]+""")
    val URL_REGEX = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"'{}|\\^`\[\]\u4e00-\u9fa5]+|(?:[a-zA-Z0-9-]+\.)+(?:com|cn|org|net|io|me|cc|top|app|dev|ai|xyz|site|info)(?:/[^\s<>"'{}|\\^`\[\]\u4e00-\u9fa5]*)?""")
    private val CHECKBOX_SYMBOL_PATTERN = Regex("""[☐☑]""")
    private val STRIP_MENTION_REGEX = Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:\d+\)""")
    private val STRIP_CHECKBOX_REGEX = Regex("""- \[([ x])\] """)
    private val CHECKBOX_UNCHECKED_REGEX = Regex("""^- \[ \]""", RegexOption.MULTILINE)
    private val CHECKBOX_CHECKED_REGEX = Regex("""^- \[x\]""", RegexOption.MULTILINE)

    /**
     * Clean trailing punctuation from a URL match (e.g. Chinese period, comma, brackets).
     */
    fun cleanUrlMatch(match: MatchResult): Pair<String, IntRange>? {
        val raw = match.value
        var end = match.range.last + 1
        var trimmed = 0
        val trailingPunct = ".,!?:;，。！？；：()[]{}<>）】\"'“”‘’"
        while (raw.length - trimmed > 0 && raw[raw.length - 1 - trimmed] in trailingPunct) {
            trimmed++
            end--
        }
        if (raw.length - trimmed <= 0) return null
        val cleaned = raw.substring(0, raw.length - trimmed)
        return Pair(cleaned, match.range.first until end)
    }

    // Thread-safe high-performance LRU cache for parsed rich text results (Capacity 300 notes)
    private const val MAX_CACHE_SIZE = 300
    private val richTextCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, ParsedRichText>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParsedRichText>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )

    /**
     * Clear the rich text parse cache (useful when memory is tight or in tests).
     */
    fun clearCache() {
        richTextCache.clear()
    }

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
        tagColor: Color = Color(0xFF84A2EE),
        onTagClick: ((String) -> Unit)? = null,
        searchQuery: String = ""
    ): ParsedRichText {
        // Fast path: check memory cache
        val cacheKey = "$content##${tagColor.value}##$searchQuery"
        richTextCache[cacheKey]?.let { return it }

        var processedContent = content

        // Step 1: Replace list markers (excluding checkboxes) and checkbox markers with symbols
        processedContent = processedContent.replace(LIST_PATTERN, "• ")

        processedContent = processedContent.replace(CHECKBOX_PATTERN) { matchResult ->
            when (matchResult.groupValues[1]) {
                "x" -> "☑"  // Checked
                else -> "☐" // Unchecked
            }
        }

        // Step 2: Extract and strip all @[text](note:id) mentions from body text
        val extractedMentions = mutableListOf<ExtractedMention>()
        MENTION_REGEX.findAll(processedContent).forEach { match ->
            val text = match.groupValues[1]
            val noteId = match.groupValues[2].toLongOrNull()
            if (noteId != null) {
                extractedMentions.add(ExtractedMention(noteId, text))
            }
        }

        // Replace mentions first so body text is clean
        val contentWithoutMentions = processedContent
            .replace(MENTION_REGEX, "")
            .replace(Regex("""\]\(note:\d+\)"""), "")
            .replace(Regex("""@\["""), "")
            .trimEnd()

        val extractedTags = mutableListOf<String>()
        val builder = AnnotatedString.Builder()
        val markerPositions = mutableListOf<MarkerPosition>()

        // Find bold markers ((?s) enables DOTALL mode)
        BOLD_REGEX.findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.BOLD))
        }

        // Find underline markers ((?s) enables DOTALL mode)
        UNDERLINE_REGEX.findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.UNDERLINE))
        }

        // Find highlight markers ((?s) enables DOTALL mode)
        HIGHLIGHT_REGEX.findAll(contentWithoutMentions).forEach { match ->
            markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
            markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
            markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, formatType = FormatType.HIGHLIGHT))
        }

        // Stage 33: Find all URLs and trim trailing punctuation
        val urlMatches = mutableListOf<Pair<String, IntRange>>()
        URL_REGEX.findAll(contentWithoutMentions).forEach { match ->
            cleanUrlMatch(match)?.let { urlMatches.add(it) }
        }

        // Find tags (#tagName) - Supports Chinese characters directly preceding # without space, while preventing URL anchors (page#sec)
        TAG_REGEX.findAll(contentWithoutMentions).forEach { match ->
            val isInsideUrl = urlMatches.any { (_, range) -> match.range.first in range }
            if (!isInsideUrl) {
                val tagName = match.value.substring(1) // remove #
                extractedTags.add(tagName)
                markerPositions.add(MarkerPosition(
                    start = match.range.first,
                    end = match.range.last + 1,
                    type = MarkerType.TAG,
                    tagName = tagName
                ))
            }
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

        // Stage 33: Apply LinkBlue styling and clickable URL StringAnnotations
        urlMatches.forEach { (cleanedUrl, rawRange) ->
            val cleanStart = positionMap[rawRange.first] ?: 0
            val mappedEnd = positionMap[rawRange.last + 1] ?: builder.length
            val cleanEnd = minOf(mappedEnd, builder.length)

            if (cleanStart >= 0 && cleanStart < cleanEnd && cleanEnd <= builder.length) {
                builder.addStyle(
                    style = SpanStyle(
                        color = LinkBlue,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = cleanStart,
                    end = cleanEnd
                )
                builder.addStringAnnotation(
                    tag = "URL",
                    annotation = cleanedUrl,
                    start = cleanStart,
                    end = cleanEnd
                )
            }
        }

        // Apply style to checkbox symbols
        val textWithCheckboxes = builder.toAnnotatedString()
        CHECKBOX_SYMBOL_PATTERN.findAll(textWithCheckboxes.text).forEach { match ->
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

        // Stage 16: Highlight search query matches with bright yellow background
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isNotEmpty()) {
            val textStr = textWithCheckboxes.text
            var startIndex = 0
            while (startIndex < textStr.length) {
                val matchIndex = textStr.indexOf(trimmedQuery, startIndex, ignoreCase = true)
                if (matchIndex == -1) break
                val matchEnd = matchIndex + trimmedQuery.length
                builder.addStyle(
                    style = SpanStyle(
                        background = Color(0xFFFFEB3B), // Bright Material Yellow
                        color = Color(0xFF1A1A1A),       // High contrast text
                        fontWeight = FontWeight.Bold
                    ),
                    start = matchIndex,
                    end = matchEnd
                )
                startIndex = matchEnd
            }
        }

        val result = ParsedRichText(
            annotatedString = builder.toAnnotatedString(),
            tags = extractedTags.distinct(),
            mentions = extractedMentions
        )

        // Store into LRU cache
        richTextCache[cacheKey] = result

        return result
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
     * Get URL string at specific text offset.
     *
     * Stage 33: Used for handling link clicks to launch system browser.
     *
     * @param annotatedString AnnotatedString with URL annotations
     * @param offset Click offset position
     * @return URL string, or null if no URL at offset
     */
    fun getUrlAtOffset(annotatedString: AnnotatedString, offset: Int): String? {
        return annotatedString
            .getStringAnnotations(tag = "URL", start = offset, end = offset)
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
            FormatType.LIST_UNORDERED -> "- " to ""
            FormatType.CHECKBOX_UNCHECKED -> "- [ ] " to ""
            FormatType.CHECKBOX_CHECKED -> "- [x] " to ""
        }

        if (formatType == FormatType.LIST_UNORDERED) {
            val lineStart = content.lastIndexOf('\n', (safeStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val linePrefix = content.substring(lineStart, safeStart)
            if (!linePrefix.trimStart().startsWith("- ")) {
                return content.substring(0, lineStart) + "- " + content.substring(lineStart)
            }
            return content
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
     * Stage 45: Loop stripping for nested mentions and residual fragments.
     *
     * @param content Formatted text with Markdown markers
     * @return Plain text without markers
     */
    fun stripFormatting(content: String): String {
        var clean = content
        var prevClean: String
        do {
            prevClean = clean
            clean = clean.replace(STRIP_MENTION_REGEX, "$1")
        } while (clean != prevClean && clean.contains("](note:"))

        return clean
            .replace(BOLD_REGEX, "$1")
            .replace(UNDERLINE_REGEX, "$1")
            .replace(HIGHLIGHT_REGEX, "$1")
            .replace(STRIP_CHECKBOX_REGEX, "")
            .replace(Regex("""\]\(note:\d+\)"""), "")
            .replace(Regex("""@\["""), "")
            .trim()
    }

    /**
     * Generate a pristine, plain-text single-line summary of note content.
     *
     * Stage 45: Flomo-aligned clean summary extraction.
     * - Strips all @[...](note:id) references (replaces with mention text)
     * - Strips bold (**), underline (__), highlight (==), checkboxes (- [ ])
     * - Strips residual Markdown symbols, brackets, and newlines
     * - Truncates to maxLength without dangling syntax
     *
     * @param content Raw note content
     * @param maxLength Maximum length of summary
     * @return Clean plain text summary
     */
    fun getCleanSummary(content: String, maxLength: Int = 30): String {
        if (content.isBlank()) return "未命名笔记"

        var clean = content
        // 1. Loop to strip any nested or regular mentions (@[text](note:123) -> text)
        var prevClean: String
        do {
            prevClean = clean
            clean = clean.replace(STRIP_MENTION_REGEX, " $1 ")
        } while (clean != prevClean && clean.contains("](note:"))

        // 2. Strip standard Markdown formatters
        clean = clean
            .replace(BOLD_REGEX, "$1")
            .replace(UNDERLINE_REGEX, "$1")
            .replace(HIGHLIGHT_REGEX, "$1")
            .replace(STRIP_CHECKBOX_REGEX, "")
            .replace(CHECKBOX_SYMBOL_PATTERN, "")
            .replace(LIST_PATTERN, "")

        // 3. Remove residual mention fragments like "](note:123)" if malformed
        clean = clean.replace(Regex("""\]\(note:\d+\)"""), "")
        clean = clean.replace(Regex("""@\["""), "")

        // 4. Flatten newlines and multiple spaces
        clean = clean.replace(Regex("""[\r\n\t]+"""), " ")
        clean = clean.replace(Regex("""\s{2,}"""), " ").trim()

        // 5. Remove problematic brackets that could break outer markdown
        clean = clean.replace('[', ' ').replace(']', ' ').replace(Regex("""\s{2,}"""), " ").trim()

        if (clean.isBlank()) return "未命名笔记"

        return if (clean.length > maxLength) {
            clean.take(maxLength) + "..."
        } else {
            clean
        }
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
            FormatType.BOLD to BOLD_REGEX.findAll(content).count(),
            FormatType.UNDERLINE to UNDERLINE_REGEX.findAll(content).count(),
            FormatType.HIGHLIGHT to HIGHLIGHT_REGEX.findAll(content).count(),
            FormatType.MENTION to MENTION_REGEX.findAll(content).count(),
            FormatType.CHECKBOX_UNCHECKED to CHECKBOX_UNCHECKED_REGEX.findAll(content).count(),
            FormatType.CHECKBOX_CHECKED to CHECKBOX_CHECKED_REGEX.findAll(content).count()
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
        return MENTION_REGEX.findAll(content).mapNotNull { match ->
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
        LIST_UNORDERED,
        CHECKBOX_UNCHECKED,
        CHECKBOX_CHECKED
    }
}
