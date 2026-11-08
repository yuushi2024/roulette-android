package com.yuushi2024.roulette

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuushi2024.roulette.data.PreferencesStore
import com.yuushi2024.roulette.logic.SpinMath
import com.yuushi2024.roulette.logic.TextEditing
import com.yuushi2024.roulette.logic.WheelFactory
import com.yuushi2024.roulette.logic.WheelLayout
import com.yuushi2024.roulette.ui.MultilineInputEditText
import com.yuushi2024.roulette.ui.RouletteView
import com.yuushi2024.roulette.ui.RouletteViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: RouletteViewModel by viewModels()

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var rouletteView: RouletteView
    private lateinit var entriesInput: MultilineInputEditText
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
        // Draw edge to edge so the IME insets are delivered to the root view even
        // on Android 10 and older. The listener below then pads the layout by the
        // keyboard height, which is what lifts the input box above the keyboard.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
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

        // Lift the content above the soft keyboard and hand the whole screen to
        // the input box while it is open. Padding the root view is what actually
        // moves the layout: only changing the wheel's height left the bottom of
        // the layout behind the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val imeVisible = isImeVisible(insets, systemBars.bottom, imeBottom)
            val bottom = WheelLayout.bottomInset(imeVisible, imeBottom, systemBars.bottom)
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            setWheelAreaVisible(WheelLayout.showWheelArea(imeVisible))
            insets
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

    /**
     * Whether the soft keyboard is showing.
     *
     * API 30 and newer report this directly. Below that the keyboard is folded into
     * the bottom inset while the system-bar inset keeps reporting the navigation bar,
     * so an IME inset taller than that bar means the keyboard is open.
     */
    private fun isImeVisible(insets: WindowInsetsCompat, systemBarBottom: Int, imeBottom: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.isVisible(WindowInsetsCompat.Type.ime())
        } else {
            WheelLayout.imeVisibleFromInsets(imeBottom, systemBarBottom)
        }

    /** Hides the wheel, result and start button so the input box can take the screen. */
    private fun setWheelAreaVisible(visible: Boolean) {
        val state = if (visible) View.VISIBLE else View.GONE
        rouletteView.visibility = state
        resultText.visibility = state
        spinButton.visibility = state
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
