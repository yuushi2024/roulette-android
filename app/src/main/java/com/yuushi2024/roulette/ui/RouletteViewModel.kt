package com.yuushi2024.roulette.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.yuushi2024.roulette.data.PreferencesStore
import com.yuushi2024.roulette.logic.RouletteSector
import com.yuushi2024.roulette.logic.WheelFactory

/**
 * Holds the wheel state across configuration changes and keeps the stored text in
 * sync with what the user typed.
 */
class RouletteViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PreferencesStore(application)

    private val _sectors = MutableLiveData<List<RouletteSector>>(emptyList())
    val sectors: LiveData<List<RouletteSector>> = _sectors

    private val _result = MutableLiveData<String?>(null)
    val result: LiveData<String?> = _result

    private var lastSavedText: String? = null

    /** Rebuilds the wheel from [text] if it changed since the last rebuild. */
    fun rebuild(text: String) {
        if (text == lastSavedText) return
        lastSavedText = text
        store.saveEntriesText(text)
        val entries = WheelFactory.parseEntries(text)
        _sectors.value = WheelFactory.createSectors(entries)
        _result.value = null
    }

    fun onSpinFinished(sectorIndex: Int) {
        _result.value = _sectors.value?.getOrNull(sectorIndex)?.label
    }

    fun clear() {
        lastSavedText = ""
        store.clear()
        _sectors.value = emptyList()
        _result.value = null
    }

    fun entryCount(): Int = WheelFactory.parseEntries(lastSavedText.orEmpty()).size
}
