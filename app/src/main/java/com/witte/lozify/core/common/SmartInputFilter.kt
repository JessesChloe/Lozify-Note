package com.witte.lozify.core.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * SmartInputFilter - Intelligent input interceptor for markdown text fields.
 *
 * Stage 15: Editor Usability Enhancement
 *
 * Features:
 * 1. Atomic Deletion: Pressing backspace inside empty marker pairs (**|**, ==|==, __|__)
 *    deletes all 4 delimiter characters in one shot.
 * 2. Smart Enter Escape: Pressing enter before closing markers (**text|**) jumps out past
 *    the markers before inserting newline (**text**\n).
 * 3. Cursor Repulsion: Prevents cursor from being stuck between delimiter characters
 *    (*|*, =|=, _|_), pushing it cleanly outside or inside the marker bounds.
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
        // Condition: Text length decreased by 1 and selection was collapsed
        if (oldValue.text.length - newValue.text.length == 1 && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            for (marker in MARKER_PAIRS) {
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
            }
        }

        // Magic 2: Smart Enter Escape (智能回车跃出)
        // Condition: Text length increased by 1, inserted character is \n, and selection was collapsed
        else if (newValue.text.length - oldValue.text.length == 1 && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.start
            val insertedChar = newValue.text.getOrNull(oldCursor)
            if (insertedChar == '\n') {
                for (marker in MARKER_PAIRS) {
                    if (oldCursor + 2 <= oldValue.text.length) {
                        val nextTwo = oldValue.text.substring(oldCursor, oldCursor + 2)
                        if (nextTwo == marker) {
                            val before = oldValue.text.substring(0, oldCursor + 2)
                            val after = oldValue.text.substring(oldCursor + 2)
                            val newText = before + "\n" + after
                            val newCursor = (oldCursor + 2 + 1).coerceIn(0, newText.length)
                            tempValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursor)
                            )
                            break
                        }
                    }
                }
            }
        }

        // Magic 3: Cursor Repulsion (光标弹斥结界)
        // Ensure collapsed cursor doesn't land between two identical marker characters (*|*, =|=, _|_)
        if (tempValue.selection.collapsed) {
            val cursor = tempValue.selection.start
            if (cursor > 0 && cursor < tempValue.text.length) {
                val prev = tempValue.text[cursor - 1]
                val next = tempValue.text[cursor]
                val isTrapped = (prev == '*' && next == '*') ||
                        (prev == '=' && next == '=') ||
                        (prev == '_' && next == '_')

                if (isTrapped) {
                    val direction = if (tempValue.selection.start >= oldValue.selection.start) 1 else -1
                    val repelledCursor = (cursor + direction).coerceIn(0, tempValue.text.length)
                    tempValue = tempValue.copy(selection = TextRange(repelledCursor))
                }
            }
        }

        return tempValue
    }
}
