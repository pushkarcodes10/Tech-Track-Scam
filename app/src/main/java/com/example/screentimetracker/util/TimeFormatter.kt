package com.example.screentimetracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    /**
     * Formats a duration in milliseconds as HH:mm:ss. Hours are not
     * capped at 24 so multi-hour/day sessions still display correctly
     * (e.g. 30 hours -> "30:00:00").
     */
    fun formatHms(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** Human-readable date/time for the history list, e.g. "12 Aug 2026, 14:05". */
    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        return sdf.format(Date(millis))
    }

    /** File-name-safe timestamp, e.g. "20260812_140512". */
    fun formatForFileName(millis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return sdf.format(Date(millis))
    }
}
