package com.openpaw.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openpaw.app.MainActivity
import com.openpaw.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ForegroundService that keeps OpenPaw alive in the background.
 *
 * Starting this service:
 *   AgentForegroundService.start(context)
 *
 * Stopping:
 *   AgentForegroundService.stop(context)
 *
 * The service shows a persistent notification with a "Open" action.
 * It keeps the process alive so that:
 *   - AccessibilityService events keep firing
 *   - Scheduled/triggered agent tasks can execute
 *   - Memory + context stay in RAM
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "openpaw_agent"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.openpaw.app.ACTION_START"
        const val ACTION_STOP = "com.openpaw.app.ACTION_STOP"
        const val ACTION_STATUS_UPDATE = "com.openpaw.app.ACTION_STATUS_UPDATE"
        const val EXTRA_STATUS_TEXT = "status_text"

        /** Observable running state – observe this in UI to show start/stop button. */
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateStatus(context: Context, statusText: String) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_STATUS_UPDATE
                putExtra(EXTRA_STATUS_TEXT, statusText)
            }
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Ready – say something!"))
                _isRunning.value = true
            }
            ACTION_STOP -> {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STATUS_UPDATE -> {
                val text = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "Running..."
                notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OpenPaw Agent",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "OpenPaw runs in the background to control your phone"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AgentForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐾 OpenPaw Agent")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
