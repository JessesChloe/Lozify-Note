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
    private val CheckboxGreen = Color(0xFF00C853)
    private val CheckboxGray = Color(0xFF9CA3AF)

    /**
     * Build AnnotatedString with full rich text formatting support.
     *
     * Processing Order:
     * 1. Checkboxes (- [ ] / - [x])
     * 2. Bold (**text**)
     * 3. Underline (__text__)
     * 4. Highlight (==text==)
     * 5. Tags (#tagname)
     *
     * @param content Raw text with Markdown markers
     * @param tagColor Color for #tags (default blue)
     * @param onTagClick Callback when tag is clicked (receives tag name without #)
     * @return AnnotatedString with all formatting applied
     */
    fun buildAnnotatedStringWithFormatting(
        content: String,
        tagColor: Color = TagBlue,
        onTagClick: ((String) -> Unit)? = null
    ): AnnotatedString {
        return AnnotatedString.Builder().apply {
            var processedContent = content

            // Step 1: Replace checkbox markers with Unicode symbols
            // - [ ] → ☐ (unchecked)
            // - [x] → ☑ (checked)
            val checkboxPattern = Regex("""^- \[([ x])\]""", RegexOption.MULTILINE)
            processedContent = processedContent.replace(checkboxPattern) { matchResult ->
                when (matchResult.groupValues[1]) {
                    "x" -> "☑"  // Checked
                    else -> "☐" // Unchecked
                }
            }

            append(processedContent)

            // Step 2: Apply bold formatting (**text**)
            val boldPattern = Regex("""\*\*(.+?)\*\*""")
            boldPattern.findAll(processedContent).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                // Remove ** markers from displayed text
                val textStart = start + 2
                val textEnd = end - 2
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = textStart,
                    end = textEnd
                )
            }

            // Step 3: Apply underline formatting (__text__)
            val underlinePattern = Regex("""__(.+?)__""")
            underlinePattern.findAll(processedContent).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                val textStart = start + 2
                val textEnd = end - 2
                addStyle(
                    style = SpanStyle(textDecoration = TextDecoration.Underline),
                    start = textStart,
                    end = textEnd
                )
            }

            // Step 4: Apply highlight formatting (==text==)
            val highlightPattern = Regex("""==(.+?)==""")
            highlightPattern.findAll(processedContent).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                val textStart = start + 2
                val textEnd = end - 2
                addStyle(
                    style = SpanStyle(background = HighlightYellow),
                    start = textStart,
                    end = textEnd
                )
            }

            // Step 5: Apply tag styling (#tagname)
            val tagPattern = Regex("""#[a-zA-Z0-9一-龥_]+""")
            tagPattern.findAll(processedContent).forEach { match ->
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
            checkboxSymbolPattern.findAll(processedContent).forEach { match ->
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
     * @param content Formatted text with Markdown markers
     * @return Plain text without markers
     */
    fun stripFormatting(content: String): String {
        return content
            .replace(Regex("""\*\*(.+?)\*\*"""), "$1")  // Bold
            .replace(Regex("""__(.+?)__"""), "$1")      // Underline
            .replace(Regex("""==(.+?)=="""), "$1")      // Highlight
            .replace(Regex("""^- \[([ x])\] """, RegexOption.MULTILINE), "") // Checkbox
    }

    /**
     * Count formatting markers in content.
     *
     * Used for analytics or debugging.
     */
    fun countFormatting(content: String): Map<FormatType, Int> {
        return mapOf(
            FormatType.BOLD to Regex("""\*\*(.+?)\*\*""").findAll(content).count(),
            FormatType.UNDERLINE to Regex("""__(.+?)__""").findAll(content).count(),
            FormatType.HIGHLIGHT to Regex("""==(.+?)==""").findAll(content).count(),
            FormatType.CHECKBOX_UNCHECKED to Regex("""^- \[ \]""", RegexOption.MULTILINE).findAll(content).count(),
            FormatType.CHECKBOX_CHECKED to Regex("""^- \[x\]""", RegexOption.MULTILINE).findAll(content).count()
        )
    }

    enum class FormatType {
        BOLD,
        UNDERLINE,
        HIGHLIGHT,
        CHECKBOX_UNCHECKED,
        CHECKBOX_CHECKED
    }
}
