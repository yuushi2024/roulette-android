package com.yuushi2024.roulette.data

import android.content.Context

/**
 * Persists the user's list on the device so the wheel survives app restarts.
 * Only the raw text is stored: the wheel is rebuilt from it on launch.
 */
class PreferencesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadEntriesText(): String = prefs.getString(KEY_ENTRIES, "") ?: ""

    fun saveEntriesText(text: String) {
        prefs.edit().putString(KEY_ENTRIES, text).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private companion object {
        const val PREFS_NAME = "roulette_prefs"
        const val KEY_ENTRIES = "entries_text"
    }
}
