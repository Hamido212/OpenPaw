package com.openpaw.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.openpaw.app.MainActivity
import com.openpaw.app.presentation.voice.VoiceInputManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.abs

/**
 * Foreground service that shows a draggable 🐾 overlay bubble over all apps.
 *
 * Tapping the bubble:
 *   1. Signals VoiceInputManager to trigger voice input
 *   2. Brings OpenPaw (MainActivity) to the foreground
 *
 * Dragging: moves the bubble to any screen position.
 *
 * Requires: SYSTEM_ALERT_WINDOW permission (user grants via "Draw over other apps").
 */
@AndroidEntryPoint
class FloatingBubbleService : Service() {

    @Inject lateinit var voiceInputManager: VoiceInputManager

    companion object {
        private const val CHANNEL_ID  = "openpaw_bubble"
        private const val NOTIF_ID    = 1002
        const val ACTION_START        = "com.openpaw.app.BUBBLE_START"
        const val ACTION_STOP         = "com.openpaw.app.BUBBLE_STOP"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, FloatingBubbleService::class.java).apply { action = ACTION_START }
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FloatingBubbleService::class.java).apply { action = ACTION_STOP }
            )
        }
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "OpenPaw Bubble", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification())
                addBubble()
                _isRunning.value = true
            }
            ACTION_STOP -> {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun addBubble() {
        if (bubbleView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        val bubble = TextView(this).apply {
            text = "🐾"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setPadding(22, 22, 22, 22)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(230, 25, 118, 210))  // Material Blue 700
            }
        }

        var initX = 0; var initY = 0
        var touchX = 0f; var touchY = 0f
        var dragged = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) dragged = true
                    params.x = initX + dx
                    params.y = initY + dy
                    windowManager.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        // Tap: fire voice trigger + bring app to front
                        voiceInputManager.triggerVoiceStart()
                        startActivity(
                            Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                        )
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        windowManager.addView(bubble, params)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐾 OpenPaw Bubble")
            .setContentText("Tippe auf die Blase um den Agenten zu rufen")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(android.R.drawable.ic_delete, "Beenden", stopPi)
            .build()
    }

    override fun onDestroy() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
        _isRunning.value = false
        super.onDestroy()
    }
}
