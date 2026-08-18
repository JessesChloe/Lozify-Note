package com.witte.lozify.core.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SmartInputFilter (Atomic deletion, enter key escape, cursor repulsion).
 */
class SmartInputFilterTest {

    @Test
    fun testAtomicDeletion_centerDeleteRemovesAllFourMarkers() {
        // Cursor between bold markers: "**|**" (pos: 2)
        val oldValue = TextFieldValue(text = "****", selection = TextRange(2))
        // User presses backspace -> standard text field would produce "***" (pos: 1)
        val newValue = TextFieldValue(text = "***", selection = TextRange(1))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        // SmartInputFilter should atomically delete the whole pair "****" -> ""
        assertEquals("", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testAtomicDeletion_edgeDeleteRemovesAllFourMarkers() {
        // Cursor at end of empty highlight markers: "====|" (pos: 4)
        val oldValue = TextFieldValue(text = "====", selection = TextRange(4))
        // User presses backspace -> standard text field would produce "===" (pos: 3)
        val newValue = TextFieldValue(text = "===", selection = TextRange(3))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        assertEquals("", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testSmartEnterEscape_jumpsOutOfClosingMarker() {
        // User types content inside bold: "**Hello|**" (pos: 7)
        val oldValue = TextFieldValue(text = "**Hello**", selection = TextRange(7))
        // User presses Enter -> inserts newline at position 7: "**Hello\n**" (pos: 8)
        val newValue = TextFieldValue(text = "**Hello\n**", selection = TextRange(8))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        // SmartInputFilter jumps out of "**": "**Hello**\n" (pos: 10)
        assertEquals("**Hello**\n", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testCursorRepulsion_repelsCursorFromBetweenDelimiters() {
        // Cursor lands between asterisk: "*|*" (pos: 1 of "**")
        val oldValue = TextFieldValue(text = "**", selection = TextRange(0))
        val newValue = TextFieldValue(text = "**", selection = TextRange(1))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        // Cursor should be cleanly repelled to 0 or 2, not stuck at 1
        assertTrue(result.selection.start == 0 || result.selection.start == 2)
    }

    @Test
    fun testListContinuation_automaticallyInsertsDashOnEnter() {
        // Current line is "- Buy milk|" (pos: 10)
        val oldValue = TextFieldValue(text = "- Buy milk", selection = TextRange(10))
        // User presses Enter -> standard input inserts "\n"
        val newValue = TextFieldValue(text = "- Buy milk\n", selection = TextRange(11))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        // SmartInputFilter should continue list with "\n- "
        assertEquals("- Buy milk\n- ", result.text)
        assertEquals(13, result.selection.start)
    }

    @Test
    fun testListContinuation_emptyItemDeletesDashAndExitsListMode() {
        // Current line is just "- |" (empty list item, pos: 2)
        val oldValue = TextFieldValue(text = "- ", selection = TextRange(2))
        // User presses Enter -> standard input inserts "\n"
        val newValue = TextFieldValue(text = "- \n", selection = TextRange(3))

        val result = SmartInputFilter.applySmartInputFilter(oldValue, newValue)

        // SmartInputFilter should delete "- " and output "\n" to exit list mode
        assertEquals("\n", result.text)
        assertEquals(1, result.selection.start)
    }
}
