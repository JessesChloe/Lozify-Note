package com.witte.lozify.core.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color

/**
 * TagUtils - Utility functions for #tag parsing and rendering.
 *
 * Provides regex-based tag extraction and AnnotatedString generation
 * for highlighting tags in Compose Text components.
 */
object TagUtils {

    /**
     * Regex pattern for matching #tags.
     *
     * Matches:
     * - # symbol
     * - Followed by Unicode letters, numbers, underscores (Chinese/English/etc.)
     * - Must be preceded by start-of-string, whitespace, or punctuation
     * - Must be followed by end-of-string, whitespace, or punctuation
     *
     * Examples:
     * - "#测试" ✅
     * - "#Lozify" ✅
     * - "#Android开发" ✅
     * - "#tag_123" ✅
     * - "abc#tag" ❌ (must have boundary before #)
     */
    private val TAG_REGEX = Regex("(?<=^|[\\s\\p{Punct}])#([\\p{L}\\p{N}_]+)(?=\$|[\\s\\p{Punct}])")

    /**
     * Extract all unique tags from text content.
     *
     * @param content Note content with potential #tags
     * @return List of unique tag names (without # prefix)
     *
     * Example:
     * ```
     * extractTags("Hello #测试 world #Lozify #测试")
     * // Returns: ["测试", "Lozify"]
     * ```
     */
    fun extractTags(content: String): List<String> {
        return TAG_REGEX.findAll(content)
            .map { it.groupValues[1] } // Extract tag name without #
            .distinct()
            .toList()
    }

    /**
     * Build AnnotatedString with #tags highlighted in blue.
     *
     * @param content Note content with potential #tags
     * @param tagColor Color for tag highlighting (default: #4C88FF)
     * @param onTagClick Optional callback when tag is clicked (tag name without #)
     * @return AnnotatedString with styled tags
     *
     * Example:
     * ```
     * val annotated = buildAnnotatedStringWithTags("Hello #测试 world #Lozify")
     * Text(text = annotated)
     * ```
     */
    fun buildAnnotatedStringWithTags(
        content: String,
        tagColor: Color = Color(0xFF4C88FF),
        onTagClick: ((String) -> Unit)? = null
    ): AnnotatedString {
        return buildAnnotatedString {
            var lastIndex = 0

            TAG_REGEX.findAll(content).forEach { matchResult ->
                val matchStart = matchResult.range.first
                val matchEnd = matchResult.range.last + 1
                val tagName = matchResult.groupValues[1]

                // Append text before tag
                if (matchStart > lastIndex) {
                    append(content.substring(lastIndex, matchStart))
                }

                // Append tag with blue styling
                withStyle(style = SpanStyle(color = tagColor)) {
                    append(matchResult.value) // Full match including #
                }

                // Add clickable annotation if callback provided
                if (onTagClick != null) {
                    addStringAnnotation(
                        tag = "TAG",
                        annotation = tagName,
                        start = matchStart,
                        end = matchEnd
                    )
                }

                lastIndex = matchEnd
            }

            // Append remaining text after last tag
            if (lastIndex < content.length) {
                append(content.substring(lastIndex))
            }
        }
    }

    /**
     * Check if text contains any tags.
     *
     * @param content Text to check
     * @return true if at least one tag found
     */
    fun containsTags(content: String): Boolean {
        return TAG_REGEX.containsMatchIn(content)
    }

    /**
     * Get tag click handler for AnnotatedString.
     *
     * Use with ClickableText component to handle tag clicks.
     *
     * Example:
     * ```
     * ClickableText(
     *     text = annotatedString,
     *     onClick = { offset ->
     *         getTagAtOffset(annotatedString, offset)?.let { tagName ->
     *             // Handle tag click
     *         }
     *     }
     * )
     * ```
     */
    fun getTagAtOffset(annotatedString: AnnotatedString, offset: Int): String? {
        return annotatedString.getStringAnnotations(
            tag = "TAG",
            start = offset,
            end = offset
        ).firstOrNull()?.item
    }
}
