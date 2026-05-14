package com.xsytrance.vaib.data

import android.content.Context
import android.net.Uri

private const val PREFS_NAME = "vaib_track_prefs"
private const val KEY_TRACK_URI = "track_uri"
private const val KEY_TRACK_NAME = "track_name"

class TrackPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri, displayName: String) {
        prefs.edit()
            .putString(KEY_TRACK_URI, uri.toString())
            .putString(KEY_TRACK_NAME, displayName)
            .apply()
    }

    fun loadUri(): Uri? = prefs.getString(KEY_TRACK_URI, null)?.let { Uri.parse(it) }

    fun loadName(): String? = prefs.getString(KEY_TRACK_NAME, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
