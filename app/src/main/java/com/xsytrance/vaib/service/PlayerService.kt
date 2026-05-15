package com.xsytrance.vaib.service

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
import androidx.core.content.ContextCompat
import com.xsytrance.vaib.MainActivity

/**
 * Foreground service that keeps audio alive when the app goes to background.
 */
class PlayerService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                val trackName = intent.getStringExtra(EXTRA_TRACK_NAME) ?: "Unknown"
                createNotificationChannel()
                val notification = buildNotification(trackName, isPlaying = true)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val trackName = intent.getStringExtra(EXTRA_TRACK_NAME) ?: "Unknown"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val notification = buildNotification(trackName, isPlaying)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "vAIb Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Music playback notification"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(trackName: String, isPlaying: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("vAIb out!")
            .setContentText(trackName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "vaib_playback"

        const val ACTION_START_FOREGROUND = "com.xsytrance.vaib.ACTION_START"
        const val ACTION_UPDATE_NOTIFICATION = "com.xsytrance.vaib.ACTION_UPDATE"
        const val ACTION_STOP_SERVICE = "com.xsytrance.vaib.ACTION_STOP"
        const val ACTION_OPEN_PLAYER = "com.xsytrance.vaib.ACTION_OPEN_PLAYER"

        const val EXTRA_TRACK_NAME = "track_name"
        const val EXTRA_IS_PLAYING = "is_playing"

        fun start(context: Context, trackName: String) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_START_FOREGROUND
                putExtra(EXTRA_TRACK_NAME, trackName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun update(context: Context, trackName: String, isPlaying: Boolean) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_TRACK_NAME, trackName)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlayerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
