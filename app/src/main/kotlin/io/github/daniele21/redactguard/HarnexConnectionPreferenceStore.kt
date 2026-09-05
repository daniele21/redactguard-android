package io.github.daniele21.redactguard

import android.content.Context

/** App-private, non-sensitive preference for whether RedactGuard should maintain a Harnex connection. */
internal class HarnexConnectionPreferenceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, true)

    fun writeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "redactguard_harnex_connection"
        const val KEY_ENABLED = "connection_enabled"
    }
}
