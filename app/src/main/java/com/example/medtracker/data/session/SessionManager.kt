package com.example.medtracker.data.session

import android.content.Context

object SessionManager {
    private const val PREFS_NAME = "medtracker_prefs"
    private const val KEY_UID = "current_uid"

    fun setCurrentUid(context: Context, uid: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UID, uid)
            .apply()
    }

    fun getCurrentUid(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_UID, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_UID)
            .apply()
    }
}

