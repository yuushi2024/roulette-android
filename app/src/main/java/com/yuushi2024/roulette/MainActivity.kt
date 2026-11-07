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

    private var rotation = 0f
    private var restoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rouletteView = findViewById(R.id.rouletteView)
        entriesInput = findViewById(R.id.entriesInput)
        resultText = findViewById(R.id.resultText)
        sectorCountText = findViewById(R.id.sectorCountText)
        spinButton = findViewById(R.id.spinButton)
        clearButton = findViewById(R.id.clearButton)

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
        // Restore the multi-line input type explicitly so Enter always inserts
        // a line break.
        entriesInput.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        entriesInput.imeOptions =
            EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION

        entriesInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TEXT, entriesInput.text?.toString().orEmpty())
    }

    override fun onPause() {
        super.onPause()
        viewModel.rebuild(entriesInput.text?.toString().orEmpty())
    }

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

    private fun setSpinning(spinning: Boolean) {
        spinButton.isEnabled = !spinning && !viewModel.sectors.value.isNullOrEmpty()
        spinButton.text = if (spinning) getString(R.string.spinning) else getString(R.string.spin)
    }

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
