package com.witte.lozify.core.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * MarkdownVisualTransformation - Real-time Markdown syntax highlighting with marker fading.
 *
 * Stage 15: WYSIWYG Visual Enhancement (Marker Fading)
 *
 * Employs a layered coloring strategy:
 * 1. The outer delimiter symbols (e.g., `**`, `==`, `__`, `@[...]`) are visually faded (light gray #CCCCCC, 14sp).
 * 2. The inner content is highlighted with proper typography (Bold, Highlight background, Underline, Mention purple).
 * 3. Supports empty markers (e.g., `****`, `====`, `____`) during active typing.
 * 4. OffsetMapping.Identity ensures 100% precise 1:1 cursor positions and IME stability.
 *
 * @param tagColor Color for tags (#tag)
 * @param highlightColor Background color for highlights (==text==)
 * @param mentionColor Color for mentions (@[text](note:id))
 * @param checkboxCheckedColor Color for checked todo boxes (- [x])
 * @param checkboxUncheckedColor Color for unchecked todo boxes (- [ ])
 */
class MarkdownVisualTransformation(
    private val tagColor: Color = Color(0xFF84A2EE),
    private val highlightColor: Color = Color(0xFFFFF59D),
    private val mentionColor: Color = Color(0xFF7C4DFF),
    private val checkboxCheckedColor: Color = Color(0xFF00C853),
    private val checkboxUncheckedColor: Color = Color(0xFF9CA3AF)
) : VisualTransformation {

    private val tagRegex = Regex("""(?<![a-zA-Z0-9])#[a-zA-Z0-9\u4e00-\u9fa5_]+""")
    private val boldRegex = Regex("""\*\*(?s)(.*?)\*\*""")
    private val underlineRegex = Regex("""__(?s)(.*?)__""")
    private val highlightRegex = Regex("""==(?s)(.*?)==""")
    private val mentionRegex = Regex("""@\[((?:(?!\]\(note:).)*)\]\(note:\d+\)""")
    private val checkboxRegex = Regex("""^- \[([ x])\]""", RegexOption.MULTILINE)

    // Faded marker style for delimiter symbols
    private val markerStyle = SpanStyle(
        color = Color(0xFFCCCCCC),
        fontSize = 14.sp
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val transformed = buildAnnotatedString {
            append(rawText)

            // 1. Tags (#tag, #中文标签) - symbol is integral to tag badge
            tagRegex.findAll(rawText).forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = tagColor,
                        fontWeight = FontWeight.Medium
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // 2. Bold (**text** or ****) - fade ** symbols, bold inner text if present
            boldRegex.findAll(rawText).forEach { match ->
                addStyle(markerStyle, match.range.first, match.range.last + 1)
                val contentGroup = match.groups[1]
                if (contentGroup != null && contentGroup.value.isNotEmpty()) {
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF454545)
                        ),
                        start = contentGroup.range.first,
                        end = contentGroup.range.last + 1
                    )
                }
            }

            // 3. Highlight (==text== or ====) - fade == symbols, apply yellow background to inner text if present
            highlightRegex.findAll(rawText).forEach { match ->
                addStyle(markerStyle, match.range.first, match.range.last + 1)
                val contentGroup = match.groups[1]
                if (contentGroup != null && contentGroup.value.isNotEmpty()) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = Color(0xFF454545)
                        ),
                        start = contentGroup.range.first,
                        end = contentGroup.range.last + 1
                    )
                }
            }

            // 4. Underline (__text__ or ____) - fade __ symbols, underline inner text if present
            underlineRegex.findAll(rawText).forEach { match ->
                addStyle(markerStyle, match.range.first, match.range.last + 1)
                val contentGroup = match.groups[1]
                if (contentGroup != null && contentGroup.value.isNotEmpty()) {
                    addStyle(
                        style = SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = Color(0xFF454545)
                        ),
                        start = contentGroup.range.first,
                        end = contentGroup.range.last + 1
                    )
                }
            }

            // 5. Mentions (@[text](note:id)) - fade outer delimiters, highlight inner note title if present
            mentionRegex.findAll(rawText).forEach { match ->
                addStyle(markerStyle, match.range.first, match.range.last + 1)
                val contentGroup = match.groups[1]
                if (contentGroup != null && contentGroup.value.isNotEmpty()) {
                    addStyle(
                        style = SpanStyle(
                            color = mentionColor,
                            fontWeight = FontWeight.Medium
                        ),
                        start = contentGroup.range.first,
                        end = contentGroup.range.last + 1
                    )
                }
            }

            // 6. Checkboxes (- [ ] / - [x])
            checkboxRegex.findAll(rawText).forEach { match ->
                val isChecked = match.groupValues[1] == "x"
                addStyle(
                    style = SpanStyle(
                        color = if (isChecked) checkboxCheckedColor else checkboxUncheckedColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }

        // Extremely important: OffsetMapping.Identity ensures 1:1 cursor mapping
        return TransformedText(transformed, OffsetMapping.Identity)
    }
}
