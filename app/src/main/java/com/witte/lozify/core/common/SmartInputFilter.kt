package com.witte.lozify.core.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * SmartInputFilter - Intelligent input interceptor for markdown text fields (V2).
 *
 * Stage 15: Editor Usability Enhancement (Edge Case Hardening)
 *
 * Features:
 * 1. Atomic Deletion (原子级连带删除):
 *    - Center deletion: Backspacing inside empty marker pairs (**|**, ==|==, __|__)
 *      removes all 4 delimiter characters in one shot.
 *    - Edge deletion: Backspacing at the end of 4 empty marker characters (****|, ====|)
 *      removes the full 4-character block.
 * 2. Smart Enter Escape (智能回车跃出):
 *    - IME compatible: When text containing '\n' is inserted directly before closing
 *      markers (**text|**), jumps out past the closing marker before inserting newline.
 * 3. Enhanced Cursor Repulsion (防触摸穿透光标弹斥):
 *    - Prevents cursor from being trapped between delimiter characters (*|*, =|=, _|_).
 *    - Calculates nearest boundary distance and repels cursor outward cleanly.
 */
object SmartInputFilter {

    private val MARKER_PAIRS = listOf("**", "==", "__")

    /**
     * Apply intelligent input filters to transition between [oldValue] and [newValue].
     *
     * @param oldValue Previous state of the TextField
     * @param newValue Incoming state from onValueChange
     * @return Filtered TextFieldValue with smart adjustments applied
     */
    fun applySmartInputFilter(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        var tempValue = newValue

        // Magic 1: Atomic Deletion (原子级连带删除)
        // Condition: Text length decreased and selection was collapsed
        if (oldValue.text.length > newValue.text.length && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            for (marker in MARKER_PAIRS) {
                val quad = marker + marker // e.g. "****", "====", "____"

                // Case 1: Cursor was in the center (**|**)
                if (oldCursor >= 2 && oldCursor + 2 <= oldValue.text.length) {
                    val before = oldValue.text.substring(oldCursor - 2, oldCursor)
                    val after = oldValue.text.substring(oldCursor, oldCursor + 2)
                    if (before == marker && after == marker) {
                        val newText = oldValue.text.removeRange(oldCursor - 2, oldCursor + 2)
                        val newCursor = (oldCursor - 2).coerceIn(0, newText.length)
                        tempValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        break
                    }
                }

                // Case 2: Cursor was at the end of 4 empty markers (****|, ====|)
                if (oldCursor >= 4 && oldCursor <= oldValue.text.length) {
                    val prevFour = oldValue.text.substring(oldCursor - 4, oldCursor)
                    if (prevFour == quad) {
                        val newText = oldValue.text.removeRange(oldCursor - 4, oldCursor)
                        val newCursor = (oldCursor - 4).coerceIn(0, newText.length)
                        tempValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        break
                    }
                }

                // Case 3: Cursor was at the start of 4 empty markers (|****) (Forward Delete)
                if (oldCursor >= 0 && oldCursor + 4 <= oldValue.text.length) {
                    val nextFour = oldValue.text.substring(oldCursor, oldCursor + 4)
                    if (nextFour == quad) {
                        val newText = oldValue.text.removeRange(oldCursor, oldCursor + 4)
                        val newCursor = oldCursor.coerceIn(0, newText.length)
                        tempValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                        break
                    }
                }
            }
        }

        // Magic 2: Smart Enter Escape (智能回车跃出，兼容输入法提交)
        // Condition: Text length increased, inserted content contains \n, and selection was collapsed
        else if (newValue.text.length > oldValue.text.length && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            val addedCount = newValue.text.length - oldValue.text.length

            if (oldCursor >= 0 && oldCursor + addedCount <= newValue.text.length) {
                val insertedText = newValue.text.substring(oldCursor, oldCursor + addedCount)
                if (insertedText.contains('\n')) {
                    // Check list continuation (- item)
                    val lineStart = oldValue.text.lastIndexOf('\n', (oldCursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                    val currentLine = oldValue.text.substring(lineStart, oldCursor)

                    if (currentLine.matches(Regex("""^\s*-\s*$"""))) {
                        // Case A: User pressed Enter on empty list item "- " -> exit list mode (delete the "- ")
                        val prefix = oldValue.text.substring(0, lineStart)
                        val suffix = oldValue.text.substring(oldCursor)
                        val newText = prefix + "\n" + suffix
                        tempValue = TextFieldValue(text = newText, selection = TextRange((prefix.length + 1).coerceIn(0, newText.length)))
                    } else if (currentLine.matches(Regex("""^\s*-\s+(?!\[[ x]\])(.+)$"""))) {
                        // Case B: User pressed Enter on non-empty list item -> automatically continue with "- "
                        val prefix = oldValue.text.substring(0, oldCursor)
                        val suffix = oldValue.text.substring(oldCursor)
                        val newText = prefix + "\n- " + suffix
                        val newCursor = (oldCursor + 3).coerceIn(0, newText.length)
                        tempValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                    } else {
                        for (marker in MARKER_PAIRS) {
                            val markerLen = marker.length

                            // Check if right after oldCursor in oldValue there is a closing marker (e.g. "**", "==", "__")
                            if (oldCursor + markerLen <= oldValue.text.length) {
                                val nextTwo = oldValue.text.substring(oldCursor, oldCursor + markerLen)
                                if (nextTwo == marker) {
                                    // Jump cursor OUT of the closing marker without inserting newline, staying on the same line
                                    val newCursor = (oldCursor + markerLen).coerceIn(0, oldValue.text.length)
                                    tempValue = TextFieldValue(
                                        text = oldValue.text,
                                        selection = TextRange(newCursor)
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        // Magic 3: Enhanced Cursor Repulsion (防触摸穿透光标弹斥)
        // Ensure collapsed cursor doesn't land between delimiter characters (*|*, =|=, _|_)
        if (tempValue.selection.collapsed) {
            val cursor = tempValue.selection.start
            if (cursor > 0 && cursor < tempValue.text.length) {
                val currChar = tempValue.text[cursor]
                val prevChar = tempValue.text[cursor - 1]

                if (currChar == prevChar && (currChar == '*' || currChar == '=' || currChar == '_')) {
                    val c = currChar

                    // Find full contiguous run of character c
                    var segStart = cursor
                    while (segStart > 0 && tempValue.text[segStart - 1] == c) {
                        segStart--
                    }
                    var segEnd = cursor
                    while (segEnd < tempValue.text.length && tempValue.text[segEnd] == c) {
                        segEnd++
                    }

                    val segLen = segEnd - segStart
                    val relPos = cursor - segStart

                    // Inside a delimiter pair (*|*), relPos is odd
                    if (relPos % 2 != 0) {
                        val distLeft = relPos
                        val distRight = segLen - relPos

                        val repelledCursor = when {
                            distLeft < distRight -> segStart
                            distLeft > distRight -> segEnd
                            else -> {
                                val direction = if (tempValue.selection.start >= oldValue.selection.start) 1 else -1
                                (cursor + direction).coerceIn(0, tempValue.text.length)
                            }
                        }

                        tempValue = tempValue.copy(
                            selection = TextRange(repelledCursor.coerceIn(0, tempValue.text.length))
                        )
                    }
                }
            }
        }

        return tempValue
    }
}
