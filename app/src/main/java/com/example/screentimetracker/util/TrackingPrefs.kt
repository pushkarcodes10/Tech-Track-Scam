package com.example.screentimetracker.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Source of truth for "is there an active tracking session right now,
 * and whose is it". Backed by SharedPreferences (not just an in-memory
 * flag) so that:
 *  - The Admin page can correctly reflect an active session even if it
 *    wasn't the Activity that started it.
 *  - State survives process death while the ForegroundService keeps
 *    the process alive, and is still consistent if the process is
 *    restarted while the service is running.
 */
object TrackingPrefs {

    private const val PREFS_NAME = "screen_time_tracking_prefs"
    private const val KEY_IS_TRACKING = "key_is_tracking"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_START_TIME = "key_start_time"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun startSession(context: Context, userName: String) {
        prefs(context).edit()
            .putBoolean(KEY_IS_TRACKING, true)
            .putString(KEY_USER_NAME, userName)
            .putLong(KEY_START_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Remembers the name typed on the User page without marking a
     * session as active yet -- used while permission prompts (Usage
     * Access / notifications) are still pending, so the name survives
     * the round trip to Settings and back.
     */
    fun saveDraftUserName(context: Context, userName: String) {
        prefs(context).edit()
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    fun endSession(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .apply()
    }

    fun isTracking(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_TRACKING, false)

    fun getUserName(context: Context): String =
        prefs(context).getString(KEY_USER_NAME, "") ?: ""

    fun getStartTime(context: Context): Long =
        prefs(context).getLong(KEY_START_TIME, System.currentTimeMillis())
}
