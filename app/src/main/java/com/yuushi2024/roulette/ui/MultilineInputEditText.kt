package com.yuushi2024.roulette.ui

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.google.android.material.textfield.TextInputEditText
import com.yuushi2024.roulette.logic.TextEditing

/**
 * A multi-line input box that always offers a real line break.
 *
 * A plain [TextInputEditText] with `textMultiLine` is not enough: while the field
 * shows a hint, and on several Japanese IMEs, the Enter key is turned into a
 * "done" action and no newline can be typed. This view forces a multi-line input
 * type and a "no action" Enter key on every new input connection, and keeps key
 * and editor-action listeners as a last-resort fallback that insert the break
 * itself.
 */
class MultilineInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.editTextStyle,
) : TextInputEditText(context, attrs, defStyleAttr) {

    init {
        // The hint can flip the field back to a single line, so re-assert it here.
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        maxLines = Int.MAX_VALUE
        setSingleLine(false)
        setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                insertLineBreakAtCaret()
            } else {
                false
            }
        }
        setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER ||
                actionId == EditorInfo.IME_ACTION_NONE ||
                actionId == EditorInfo.IME_ACTION_UNSPECIFIED
            if (isEnter && event?.action != KeyEvent.ACTION_UP) {
                insertLineBreakAtCaret()
            } else {
                false
            }
        }
    }

    /**
     * Re-applies the multi-line contract on the connection the IME will use.
     *
     * The action bits are cleared before [EditorInfo.IME_ACTION_UNSPECIFIED] is
     * set, because the flag alone does not overwrite an action that is already
     * present. [EditorInfo.IME_FLAG_NO_ENTER_ACTION] then keeps the IME from
     * showing a "done" key, so the Enter key inserts a newline.
     */
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.inputType = outAttrs.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = (outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION.inv()) or
            EditorInfo.IME_ACTION_UNSPECIFIED or
            EditorInfo.IME_FLAG_NO_ENTER_ACTION
        return super.onCreateInputConnection(outAttrs)
    }

    /** Inserts a newline at the caret and reports whether it was handled. */
    private fun insertLineBreakAtCaret(): Boolean {
        val editable = text ?: return false
        val start = selectionStart
        val end = selectionEnd
        val missingSelection = start < 0 || end < 0
        val from = if (missingSelection) editable.length else minOf(start, end).coerceIn(0, editable.length)
        val to = if (missingSelection) editable.length else maxOf(start, end).coerceIn(0, editable.length)
        // Reuse the shared helper so the caret lands exactly where the
        // "＋行追加" button would leave it.
        val caret = TextEditing.insertLineBreak(editable.toString(), start, end).cursor
        editable.replace(from, to.coerceAtLeast(from), "\n")
        setSelection(caret.coerceIn(0, editable.length))
        return true
    }
}
