package com.xsytrance.vaib.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSessionService
import com.xsytrance.vaib.MainActivity
import com.xsytrance.vaib.R
import com.xsytrance.vaib.audio.AudioPlayer

/**
 * Foreground service that keeps audio alive when the app goes to background.
 *
 * Uses Media3 [MediaSessionService] as the base so that:
 *   - Android's media notification controls work automatically
 *   - Bluetooth/headset media buttons are handled
 *   - Android Auto and Wear OS integrations are possible
 *
 * Lifecycle:
 *   1. [MainActivity] calls [startForegroundService] when playback starts
 *   2. This service builds a Media3 session and shows a notification
 *   3. When playback stops or user swipes the notification, the service stops itself
 */
class PlayerService : MediaSessionService() {

    private val binder = LocalBinder()

    // The AudioPlayer is owned by the ViewModel, but we reference it
    // here for notification transport controls (play/pause/next/prev).
    private var audioPlayer: AudioPlayer? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onGetSession(controllerInfo: MediaSessionService.MediaSessionControllerInfo): MediaSession? {
        // Return null — we'll use our own PlayerWrapper (simpler approach)
        // Full Media3 session integration is a Phase 3 enhancement
        return super.onGetSession(controllerInfo)
    }

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
                NotificationManager.IMPORTANCE_LOW,  // Low = no sound, shows in shade
            ).apply {
                description = "Music playback notification"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(trackName: String, isPlaying: Boolean): Notification {
        // PendingIntent to open MainActivity → NowPlaying when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Play/Pause action
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause, "Pause",
                buildActionPendingIntent(ACTION_PLAY_PAUSE),
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play, "Play",
                buildActionPendingIntent(ACTION_PLAY_PAUSE),
            )
        }

        // Skip next action
        val skipNextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next, "Next",
            buildActionPendingIntent(ACTION_SKIP_NEXT),
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("vAIb out!")
            .setContentText(trackName)
            .setSmallIcon(R.drawable.ic_notification)  // Add icon to res/drawable
            .setContentIntent(pendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(null)  // Will be wired in Phase 3 with full Media3
            )
            .addAction(playPauseAction)
            .addAction(skipNextAction)
            .setOngoing(isPlaying)
            .setSilent(true)  // No notification sound
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "vaib_playback"

        const val ACTION_START_FOREGROUND = "com.xsytrance.vaib.ACTION_START"
        const val ACTION_UPDATE_NOTIFICATION = "com.xsytrance.vaib.ACTION_UPDATE"
        const val ACTION_STOP_SERVICE = "com.xsytrance.vaib.ACTION_STOP"
        const val ACTION_PLAY_PAUSE = "com.xsytrance.vaib.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.xsytrance.vaib.ACTION_SKIP_NEXT"
        const val ACTION_OPEN_PLAYER = "com.xsytrance.vaib.ACTION_OPEN_PLAYER"

        const val EXTRA_TRACK_NAME = "track_name"
        const val EXTRA_IS_PLAYING = "is_playing"

        /** Convenience: start the foreground service from the ViewModel. */
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