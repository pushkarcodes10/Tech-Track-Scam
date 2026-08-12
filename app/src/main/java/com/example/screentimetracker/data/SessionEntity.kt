package com.example.screentimetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per completed tracking session. This is the fail-safe
 * record: even if the exported CSV is later moved, or sharing fails,
 * this row (and the filePath it stores) lets the Admin page find and
 * re-share the report again at any time.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userName: String,

    /** Formatted as HH:mm:ss for display. */
    val screenTimeDuration: String,

    /** Raw duration in milliseconds, kept for any future sorting/analytics. */
    val durationMillis: Long,

    /** When the session was stopped / logged (epoch millis). */
    val timestamp: Long,

    /** Absolute path to the generated CSV file on device storage. */
    val filePath: String
)
