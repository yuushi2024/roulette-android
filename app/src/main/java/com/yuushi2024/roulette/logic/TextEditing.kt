package com.yuushi2024.roulette.logic

/**
 * Small text-editing helpers used by the input box.
 *
 * The logic lives here, away from the widgets, so the behaviour of the
 * "add a line" button can be unit tested on the JVM.
 */
object TextEditing {

    /** The new text plus where the caret should sit afterwards. */
    data class Insertion(val text: String, val cursor: Int)

    /**
     * Inserts a line break into [text], replacing the selection from [start] to
     * [end].
     *
     * A negative [start] or [end] means the box has no selection yet, so the
     * break is appended at the end; that is what the soft keyboard reports before
     * the user touches the field.
     */
    fun insertLineBreak(text: String, start: Int, end: Int): Insertion {
        if (start < 0 || end < 0) {
            return Insertion(text + "\n", text.length + 1)
        }
        val from = minOf(start, end).coerceIn(0, text.length)
        val to = maxOf(start, end).coerceIn(0, text.length)
        return Insertion(text.substring(0, from) + "\n" + text.substring(to), from + 1)
    }
}
