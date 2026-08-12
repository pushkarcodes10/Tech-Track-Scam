package com.example.screentimetracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

/**
 * Generates the per-session report as a CSV file under
 * getExternalFilesDir(DIRECTORY_DOCUMENTS) and prepares secure,
 * shareable Uris for it via FileProvider.
 *
 * CSV (not .xlsx) is used so the report opens natively in Excel /
 * Google Sheets / Numbers with zero extra dependencies. Files are
 * never overwritten or deleted by the app -- each session gets a
 * uniquely timestamped file name, and the Room database (see
 * data/SessionEntity.kt) separately records every file's path so a
 * session can always be re-shared later even after the app restarts.
 */
object CsvExporter {

    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    data class ExportResult(val file: File, val uri: Uri)

    fun exportSessionCsv(
        context: Context,
        userName: String,
        screenTimeFormatted: String,
        sessionEndMillis: Long
    ): ExportResult {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IllegalStateException("External Documents directory unavailable")

        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val fileNameStamp = TimeFormatter.formatForFileName(sessionEndMillis)
        val file = File(documentsDir, "session_report_$fileNameStamp.csv")

        FileWriter(file).use { writer ->
            writer.append("User Name,Screen Time\n")
            writer.append("${escapeCsv(userName)},${escapeCsv(screenTimeFormatted)}\n")
        }

        val uri = uriForFile(context, file)
        return ExportResult(file, uri)
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /** Builds a chooser Intent to open/share the report with Excel, Sheets, Drive, Gmail, etc. */
    fun buildShareIntent(context: Context, uri: Uri): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Open or share session report")
    }
}
