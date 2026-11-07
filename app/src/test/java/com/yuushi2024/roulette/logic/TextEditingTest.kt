package com.yuushi2024.roulette.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class TextEditingTest {

    /** A break goes in where the caret is. */
    @Test
    fun `a break is inserted at the caret`() {
        val result = TextEditing.insertLineBreak("りんご\nみかん", 3, 3)
        assertEquals("りんご\n\nみかん", result.text)
        assertEquals(4, result.cursor)
    }

    /** A selection is replaced, not appended to. */
    @Test
    fun `the selection is replaced by the break`() {
        val result = TextEditing.insertLineBreak("りんご\nみかん", 3, 6)
        assertEquals("りんご\nん", result.text)
        assertEquals(4, result.cursor)
    }

    /** An untouched box appends at the end. */
    @Test
    fun `a break is appended when there is no caret yet`() {
        val result = TextEditing.insertLineBreak("りんご", -1, -1)
        assertEquals("りんご\n", result.text)
        assertEquals(4, result.cursor)
    }

    /** An empty box gets one break. */
    @Test
    fun `an empty field gets a single break`() {
        val result = TextEditing.insertLineBreak("", -1, -1)
        assertEquals("\n", result.text)
        assertEquals(1, result.cursor)
    }

    /** A backwards selection inserts at its start. */
    @Test
    fun `a reversed selection still inserts at the earlier position`() {
        val result = TextEditing.insertLineBreak("abcd", 3, 1)
        assertEquals("a\nd", result.text)
        assertEquals(2, result.cursor)
    }

    /** An out of range caret is clamped. */
    @Test
    fun `an out of range caret is clamped to the end`() {
        val result = TextEditing.insertLineBreak("abc", 99, 99)
        assertEquals("abc\n", result.text)
        assertEquals(4, result.cursor)
    }

    /** A break lets the user start another entry. */
    @Test
    fun `a new entry can be typed after pressing the button`() {
        // Appending a break alone leaves the entry count unchanged until the user
        // types, so start from the text a second tap would produce and add a name.
        val before = WheelFactory.parseEntries("りんご")
        val withBreak = TextEditing.insertLineBreak("りんご", -1, -1).text
        assertEquals("りんご\n", withBreak)
        assertEquals(before.size + 1, WheelFactory.parseEntries(withBreak + "みかん").size)
    }
}
