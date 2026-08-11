package com.witte.lozify.core.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * RichTextUtils - Utility for parsing and rendering rich text formatting.
 *
 * Stage 7: Extends Stage 4's tag highlighting with full Markdown-like formatting.
 *
 * Supported Formats:
 * - **Bold**: `**text**` → FontWeight.Bold
 * - __Underline__: `__text__` → TextDecoration.Underline
 * - ==Highlight==: `==text==` → Background #FFF3C4
 * - Checkbox: `- [ ]` / `- [x]` → Rendered with Unicode checkbox symbols
 * - #Tags: `#tagname` → Blue color (from Stage 4)
 *
 * Technical Approach:
 * - Parse content with regex to find all formatting markers
 * - Build AnnotatedString with multiple SpanStyle layers
 * - Support overlapping styles (e.g., bold + highlight)
 * - Preserve original text indices for click handling
 */
object RichTextUtils {

    private val HighlightYellow = Color(0xFFFFF3C4)
    private val TagBlue = Color(0xFF4C88FF)
    private val MentionBlue = Color(0xFF4C88FF)  // Bug Fix: Changed to link blue, removed background
    private val CheckboxGreen = Color(0xFF00C853)
    private val CheckboxGray = Color(0xFF9CA3AF)

    /**
     * Build AnnotatedString with full rich text formatting support.
     *
     * Bug Fix (Stage 7): Strip Markdown markers from rendered text while preserving styling.
     * Stage 8: Added @mention support with clickable links.
     * Bug Fix (Stage 9): Fixed nested marker rendering - now correctly handles **==text==** without duplication.
     *
     * Processing Order:
     * 1. Checkboxes (- [ ] / - [x]) → Replace with Unicode
     * 2. Strip ALL markers in one pass using position tracking (handles nesting correctly)
     * 3. Apply styles to cleaned text positions
     * 4. Tags (#tagname) → Apply color styling
     * 5. Mentions (@[text](note:id)) → Apply mention styling with click
     *
     * @param content Raw text with Markdown markers
     * @param tagColor Color for #tags (default blue)
     * @param onTagClick Callback when tag is clicked (receives tag name without #)
     * @param onMentionClick Callback when @mention is clicked (receives note ID)
     * @return AnnotatedString with all formatting applied, markers removed
     */
    fun buildAnnotatedStringWithFormatting(
        content: String,
        tagColor: Color = TagBlue,
        onTagClick: ((String) -> Unit)? = null,
        onMentionClick: ((Long) -> Unit)? = null
    ): AnnotatedString {
        return AnnotatedString.Builder().apply {
            var processedContent = content

            // Step 1: Replace checkbox markers with Unicode symbols
            val checkboxPattern = Regex("""^- \[([ x])\]""", RegexOption.MULTILINE)
            processedContent = processedContent.replace(checkboxPattern) { matchResult ->
                when (matchResult.groupValues[1]) {
                    "x" -> "☑"  // Checked
                    else -> "☐" // Unchecked
                }
            }

            // Step 2: Find all marker positions (including nested ones)
            val markerPositions = mutableListOf<MarkerPosition>()

            // Find @mention markers (Stage 8: @[text](note:id) format)
            // Bug Fix: Use stricter regex to handle brackets inside mention text (e.g., "- [ ] task")
            Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:(\d+)\)""").findAll(processedContent).forEach { match ->
                val mentionText = match.groupValues[1]
                val noteId = match.groupValues[2].toLongOrNull()
                // Opening: "@[" (2 chars)
                val openStart = match.range.first
                val openEnd = match.range.first + 2
                // Closing: "](note:X)" starts after mentionText
                val closeStart = match.range.first + 2 + mentionText.length
                val closeEnd = match.range.last + 1

                markerPositions.add(MarkerPosition(openStart, openEnd, MarkerType.OPEN))
                markerPositions.add(MarkerPosition(closeStart, closeEnd, MarkerType.CLOSE))
                // Track content range for mention styling (only the mentionText part)
                markerPositions.add(MarkerPosition(
                    openEnd,
                    closeStart,
                    MarkerType.STYLE,
                    FormatType.MENTION,
                    noteId
                ))
            }

            // Find bold markers ((?s) enables DOTALL mode)
            Regex("""\*\*(?s)(.+?)\*\*""").findAll(processedContent).forEach { match ->
                markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
                markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
                markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, FormatType.BOLD))
            }

            // Find underline markers ((?s) enables DOTALL mode)
            Regex("""__(?s)(.+?)__""").findAll(processedContent).forEach { match ->
                markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
                markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
                markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, FormatType.UNDERLINE))
            }

            // Find highlight markers ((?s) enables DOTALL mode)
            Regex("""==(?s)(.+?)==""").findAll(processedContent).forEach { match ->
                markerPositions.add(MarkerPosition(match.range.first, match.range.first + 2, MarkerType.OPEN))
                markerPositions.add(MarkerPosition(match.range.last - 1, match.range.last + 1, MarkerType.CLOSE))
                markerPositions.add(MarkerPosition(match.range.first + 2, match.range.last - 1, MarkerType.STYLE, FormatType.HIGHLIGHT))
            }

            // Step 3: Build clean text by skipping marker symbols, track position mapping
            val cleanText = StringBuilder()
            val positionMap = mutableMapOf<Int, Int>() // original pos -> clean pos
            val markerRanges = markerPositions.filter { it.type == MarkerType.OPEN || it.type == MarkerType.CLOSE }
                .sortedBy { it.start }

            var i = 0
            while (i < processedContent.length) {
                // Map current position before any operation
                positionMap[i] = cleanText.length

                // Check if current position is a marker to skip
                val markerToSkip = markerRanges.firstOrNull { it.start == i }
                if (markerToSkip != null) {
                    // Skip marker symbols, but map all positions within the marker
                    for (j in i until markerToSkip.end) {
                        positionMap[j] = cleanText.length
                    }
                    i = markerToSkip.end
                } else {
                    // Copy character to clean text
                    cleanText.append(processedContent[i])
                    i++
                }
            }

            // Map end position
            positionMap[processedContent.length] = cleanText.length

            // Append clean text
            val finalText = cleanText.toString()
            append(finalText)

            // Step 4: Apply styles to clean text using position mapping
            val styleMarkers = markerPositions.filter { it.type == MarkerType.STYLE }
            styleMarkers.forEach { marker ->
                // Map original content range to clean text range
                val cleanStart = positionMap[marker.start] ?: 0
                // Bug Fix: Use marker.end directly if mapped, but clamp to finalText.length
                val mappedEnd = positionMap[marker.end] ?: cleanText.length
                val cleanEnd = minOf(mappedEnd, finalText.length)

                // Ensure valid range and prevent overflow
                if (cleanStart >= 0 && cleanStart < cleanEnd && cleanEnd <= finalText.length) {
                    val style = when (marker.formatType) {
                        FormatType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        FormatType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                        FormatType.HIGHLIGHT -> SpanStyle(background = HighlightYellow)
                        FormatType.MENTION -> SpanStyle(
                            color = MentionBlue  // Bug Fix: Removed background, only blue color
                        )
                        else -> null
                    }
                    style?.let {
                        addStyle(style = it, start = cleanStart, end = cleanEnd)
                    }

                    // Add click annotation for mentions
                    if (marker.formatType == FormatType.MENTION && marker.noteId != null) {
                        onMentionClick?.let {
                            addStringAnnotation(
                                tag = "MENTION",
                                annotation = marker.noteId.toString(),
                                start = cleanStart,
                                end = cleanEnd
                            )
                        }
                    }
                }
            }

            // Step 5: Apply tag styling (#tagname)
            val tagPattern = Regex("""#[a-zA-Z0-9一-龥_]+""")
            tagPattern.findAll(finalText).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                addStyle(
                    style = SpanStyle(color = tagColor),
                    start = start,
                    end = end
                )

                // Add string annotation for click handling
                onTagClick?.let {
                    val tagName = match.value.substring(1) // Remove # prefix
                    addStringAnnotation(
                        tag = "TAG",
                        annotation = tagName,
                        start = start,
                        end = end
                    )
                }
            }

            // Step 6: Style checkbox symbols
            val checkboxSymbolPattern = Regex("""[☐☑]""")
            checkboxSymbolPattern.findAll(finalText).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                val isChecked = match.value == "☑"
                addStyle(
                    style = SpanStyle(
                        color = if (isChecked) CheckboxGreen else CheckboxGray,
                        fontWeight = FontWeight.Bold
                    ),
                    start = start,
                    end = end
                )
            }
        }.toAnnotatedString()
    }

    // Helper data classes for marker parsing
    private data class MarkerPosition(
        val start: Int,
        val end: Int,
        val type: MarkerType,
        val formatType: FormatType? = null,
        val noteId: Long? = null
    )

    private enum class MarkerType {
        OPEN,    // Opening marker (**, __, ==, @[)
        CLOSE,   // Closing marker (**, __, ==, ](note:id))
        STYLE    // Style range to apply (maps to clean text)
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
        val (prefix, suffix) = when (formatType) {
            FormatType.BOLD -> "**" to "**"
            FormatType.UNDERLINE -> "__" to "__"
            FormatType.HIGHLIGHT -> "==" to "=="
            FormatType.MENTION -> "@[" to "](note:0)"  // Not used in toolbar, placeholder only
            FormatType.CHECKBOX_UNCHECKED -> "- [ ] " to ""
            FormatType.CHECKBOX_CHECKED -> "- [x] " to ""
        }

        return if (selectionStart == selectionEnd) {
            // No selection: insert markers and place cursor between them
            content.substring(0, selectionStart) +
                    prefix + suffix +
                    content.substring(selectionStart)
        } else {
            // Has selection: wrap selected text
            content.substring(0, selectionStart) +
                    prefix +
                    content.substring(selectionStart, selectionEnd) +
                    suffix +
                    content.substring(selectionEnd)
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
