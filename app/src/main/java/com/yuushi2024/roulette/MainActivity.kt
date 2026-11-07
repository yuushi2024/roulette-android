package com.yuushi2024.roulette

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.yuushi2024.roulette.data.PreferencesStore
import com.yuushi2024.roulette.logic.SpinMath
import com.yuushi2024.roulette.logic.TextEditing
import com.yuushi2024.roulette.logic.WheelFactory
import com.yuushi2024.roulette.ui.RouletteView
import com.yuushi2024.roulette.ui.RouletteViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: RouletteViewModel by viewModels()

    private lateinit var rouletteView: RouletteView
    private lateinit var entriesInput: TextInputEditText
    private lateinit var resultText: TextView
    private lateinit var sectorCountText: TextView
    private lateinit var spinButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var addLineButton: MaterialButton

    private var rotation = 0f
    private var restoring = false

    /** Wires the widgets, restores the saved input and rebuilds the wheel. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rouletteView = findViewById(R.id.rouletteView)
        entriesInput = findViewById(R.id.entriesInput)
        resultText = findViewById(R.id.resultText)
        sectorCountText = findViewById(R.id.sectorCountText)
        spinButton = findViewById(R.id.spinButton)
        clearButton = findViewById(R.id.clearButton)
        addLineButton = findViewById(R.id.addLineButton)

        val savedText = if (savedInstanceState != null) {
            savedInstanceState.getString(STATE_TEXT) ?: PreferencesStore(this).loadEntriesText()
        } else {
            PreferencesStore(this).loadEntriesText()
        }

        restoring = true
        entriesInput.setText(savedText)
        entriesInput.setSelection(savedText.length)
        restoring = false
        viewModel.rebuild(savedText)

        // TextInputEditText turns the Enter key into an IME action while it is
        // showing a hint, which blocks newlines even with textMultiLine set.
        // Restore the multi-line input type and clear the IME action so Enter
        // always inserts a line break. IME_FLAG_NO_ENTER_ACTION is deliberately
        // not set: on Gboard it turns Enter into a "done" action instead.
        entriesInput.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        entriesInput.imeOptions = EditorInfo.IME_ACTION_NONE

        entriesInput.addTextChangedListener(object : TextWatcher {
            /** No-op: the wheel is rebuilt in [afterTextChanged] instead. */
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            /** No-op: the wheel is rebuilt in [afterTextChanged] instead. */
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            /** Rebuilds the wheel whenever the user edits the input. */
            override fun afterTextChanged(s: Editable?) {
                if (restoring) return
                viewModel.rebuild(s?.toString().orEmpty())
            }
        })

        rouletteView.setOnSpinEndListener { index ->
            viewModel.onSpinFinished(index)
            setSpinning(false)
        }

        spinButton.setOnClickListener { startSpin() }
        clearButton.setOnClickListener { confirmClear() }
        addLineButton.setOnClickListener { addLine() }

        viewModel.sectors.observe(this) { sectors ->
            rouletteView.setSectors(sectors)
            val entries = viewModel.entryCount()
            sectorCountText.text = if (sectors.isEmpty()) {
                ""
            } else {
                getString(R.string.sector_count, entries, sectors.size)
            }
            spinButton.isEnabled = sectors.isNotEmpty()
        }

        viewModel.result.observe(this) { result ->
            resultText.text = result?.let { getString(R.string.result_label, it) }
                ?: getString(R.string.result_placeholder)
        }
    }

    /** Keeps the input across configuration changes. */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TEXT, entriesInput.text?.toString().orEmpty())
    }

    /** Persists the input so it survives leaving the app. */
    override fun onPause() {
        super.onPause()
        viewModel.rebuild(entriesInput.text?.toString().orEmpty())
    }

    /** Starts a spin unless one is already running or the wheel is empty. */
    private fun startSpin() {
        if (rouletteView.isSpinning()) return
        if (viewModel.sectors.value.isNullOrEmpty()) {
            Toast.makeText(this, R.string.empty_hint, Toast.LENGTH_SHORT).show()
            return
        }
        setSpinning(true)
        rotation = SpinMath.nextRotation(rotation)
        rouletteView.spinTo(rotation, SPIN_DURATION_MS)
    }

    /** Reflects the spinning state in the start button. */
    private fun setSpinning(spinning: Boolean) {
        spinButton.isEnabled = !spinning && !viewModel.sectors.value.isNullOrEmpty()
        spinButton.text = if (spinning) getString(R.string.spinning) else getString(R.string.spin)
    }

    /**
     * Inserts a line break at the caret, so a new entry can be started without
     * relying on the soft keyboard's Enter key.
     */
    private fun addLine() {
        val text = entriesInput.text?.toString().orEmpty()
        val start = entriesInput.selectionStart
        val end = entriesInput.selectionEnd
        val insertion = TextEditing.insertLineBreak(text, start, end)
        restoring = true
        entriesInput.setText(insertion.text)
        entriesInput.setSelection(insertion.cursor)
        restoring = false
        viewModel.rebuild(insertion.text)
    }

    /** Asks for confirmation, then clears the input and the wheel. */
    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_confirm_title)
            .setMessage(R.string.clear_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_confirm_ok) { _, _ ->
                restoring = true
                entriesInput.setText("")
                restoring = false
                viewModel.clear()
                rouletteView.setSectors(emptyList())
                Toast.makeText(this, R.string.cleared, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private companion object {
        const val STATE_TEXT = "state_entries_text"
        const val SPIN_DURATION_MS = 4200L
    }
}
