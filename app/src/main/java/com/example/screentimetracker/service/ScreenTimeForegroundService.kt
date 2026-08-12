package com.example.screentimetracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.screentimetracker.R
import com.example.screentimetracker.ui.UserActivity
import com.example.screentimetracker.util.TrackingPrefs

/**
 * Runs as a foreground service so a tracking session survives the
 * user minimizing the app, locking the phone, or using other apps.
 * It does no timing math itself -- `TrackingPrefs.startTime` is the
 * single source of truth, and the Admin page computes elapsed time
 * from it when the session is stopped. This service's only job is to
 * (a) stay alive and (b) show the required persistent notification.
 */
class ScreenTimeForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userName = intent?.getStringExtra(EXTRA_USER_NAME)
            ?: TrackingPrefs.getUserName(this)
        startForeground(NOTIFICATION_ID, buildNotification(userName))
        // START_STICKY: if the system kills this process under memory
        // pressure, it will try to recreate the service. TrackingPrefs
        // still holds the original startTime, so tracking resumes
        // accurately rather than silently losing the session.
        return START_STICKY
    }

    private fun buildNotification(userName: String): Notification {
        val openAppIntent = Intent(this, UserActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking screen time")
            .setContentText("Session active for $userName")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"
        private const val CHANNEL_ID = "screen_time_tracking_channel"
        private const val NOTIFICATION_ID = 101

        fun start(context: Context, userName: String) {
            val intent = Intent(context, ScreenTimeForegroundService::class.java)
                .putExtra(EXTRA_USER_NAME, userName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenTimeForegroundService::class.java))
        }
    }
}
