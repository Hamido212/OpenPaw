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
import com.openpaw.app.domain.usecase.AgentEvent
import com.openpaw.app.domain.usecase.AgentUseCase
import com.openpaw.app.presentation.voice.VoiceInputManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * Foreground service that shows a draggable 🐾 overlay bubble over all apps.
 *
 * Tapping the bubble starts a full voice interaction WITHOUT opening the app:
 *   1. Blue  = idle    → tap to start listening
 *   2. Red   = listening  → tap again to cancel
 *   3. Orange = processing  → waiting for agent response
 *   4. Response text pops up in a small overlay, TTS reads it aloud
 *
 * Dragging moves the bubble to any screen position.
 * Requires: SYSTEM_ALERT_WINDOW permission ("Draw over other apps").
 */
@AndroidEntryPoint
class FloatingBubbleService : Service() {

    @Inject lateinit var voiceInputManager: VoiceInputManager
    @Inject lateinit var agentUseCase: AgentUseCase

    companion object {
        private const val CHANNEL_ID = "openpaw_bubble"
        private const val NOTIF_ID   = 1002
        const val ACTION_START       = "com.openpaw.app.BUBBLE_START"
        const val ACTION_STOP        = "com.openpaw.app.BUBBLE_STOP"

        // Bubble state colors
        val COLOR_IDLE       = Color.argb(230, 25,  118, 210)  // Blue
        val COLOR_LISTENING  = Color.argb(230, 211,  47,  47)  // Red
        val COLOR_PROCESSING = Color.argb(230, 245, 124,   0)  // Orange

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

    // ── State ─────────────────────────────────────────────────────────────────

    private enum class BubbleState { IDLE, LISTENING, PROCESSING }
    private var bubbleState = BubbleState.IDLE

    private lateinit var windowManager: WindowManager
    private var bubbleView: TextView? = null
    private var responseView: TextView? = null

    /** Coroutine scope for all async work in this service. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Each service instance gets its own DB session so bubble history is separate. */
    private val bubbleSessionId = UUID.randomUUID().toString()

    private var responseJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

    override fun onDestroy() {
        dismissResponse()
        bubbleView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        bubbleView = null
        voiceInputManager.stopListening()
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    // ── Bubble view ───────────────────────────────────────────────────────────

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
            x = 0; y = 300
        }

        val bubble = TextView(this).apply {
            text = "🐾"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setPadding(22, 22, 22, 22)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_IDLE)
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
                    dragged = false; true
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
                    if (!dragged) onBubbleTapped(); true
                }
                else -> false
            }
        }

        bubbleView = bubble
        windowManager.addView(bubble, params)
    }

    // ── Tap handling ──────────────────────────────────────────────────────────

    private fun onBubbleTapped() {
        when (bubbleState) {
            BubbleState.IDLE       -> startVoiceInteraction()
            BubbleState.LISTENING  -> {
                voiceInputManager.stopListening()
                setBubbleState(BubbleState.IDLE)
            }
            BubbleState.PROCESSING -> { /* ignore while agent is working */ }
        }
    }

    // ── Voice interaction (fully in-overlay, no app open) ─────────────────────

    private fun startVoiceInteraction() {
        setBubbleState(BubbleState.LISTENING)
        dismissResponse()

        // Watch for STT errors so we can reset the bubble state
        val sttWatchJob = serviceScope.launch {
            voiceInputManager.sttState.collect { state ->
                if (state == VoiceInputManager.SttState.ERROR &&
                    bubbleState == BubbleState.LISTENING) {
                    setBubbleState(BubbleState.IDLE)
                    cancel()
                }
            }
        }

        voiceInputManager.startListening { recognizedText ->
            sttWatchJob.cancel()
            setBubbleState(BubbleState.PROCESSING)
            serviceScope.launch {
                try {
                    agentUseCase.processMessage(recognizedText, bubbleSessionId)
                        .collect { event ->
                            when (event) {
                                is AgentEvent.FinalResponse -> {
                                    setBubbleState(BubbleState.IDLE)
                                    showResponse(event.text)
                                }
                                is AgentEvent.Error -> setBubbleState(BubbleState.IDLE)
                                else -> { /* Thinking / ToolCall / ToolResult – bubble stays orange */ }
                            }
                        }
                } catch (_: Exception) {
                    setBubbleState(BubbleState.IDLE)
                }
            }
        }
    }

    // ── Visual state ──────────────────────────────────────────────────────────

    private fun setBubbleState(state: BubbleState) {
        bubbleState = state
        val bg = bubbleView?.background as? GradientDrawable ?: return
        bg.setColor(
            when (state) {
                BubbleState.IDLE       -> COLOR_IDLE
                BubbleState.LISTENING  -> COLOR_LISTENING
                BubbleState.PROCESSING -> COLOR_PROCESSING
            }
        )
    }

    // ── Response overlay ──────────────────────────────────────────────────────

    private fun showResponse(text: String) {
        // Speak aloud (bypasses the in-app TTS toggle)
        voiceInputManager.speakAlways(text)

        dismissResponse()

        val bubble = bubbleView ?: return
        val bubbleParams = bubble.layoutParams as? WindowManager.LayoutParams ?: return
        val metrics = resources.displayMetrics

        val preview = text.take(160) + if (text.length > 160) "…" else ""

        val rv = TextView(this).apply {
            this.text = preview
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.WHITE)
            setPadding(36, 28, 36, 28)
            background = GradientDrawable().apply {
                setColor(Color.argb(240, 18, 18, 18))
                cornerRadius = 32f
            }
        }

        val rvWidth = (metrics.widthPixels * 0.78f).toInt()
        val rvParams = WindowManager.LayoutParams(
            rvWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = maxOf(16, minOf(bubbleParams.x - 40, metrics.widthPixels - rvWidth - 16))
            // Show above bubble in lower half of screen, below in upper half
            y = if (bubbleParams.y > metrics.heightPixels / 2) {
                maxOf(16, bubbleParams.y - 220)
            } else {
                minOf(metrics.heightPixels - 320, bubbleParams.y + 90)
            }
        }

        rv.setOnClickListener { dismissResponse() }
        responseView = rv
        try { windowManager.addView(rv, rvParams) } catch (_: Exception) {}

        // Auto-dismiss after 6 seconds
        responseJob?.cancel()
        responseJob = serviceScope.launch {
            delay(6_000)
            dismissResponse()
        }
    }

    private fun dismissResponse() {
        responseJob?.cancel()
        responseJob = null
        responseView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        responseView = null
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐾 OpenPaw Bubble")
            .setContentText("Tippe auf die Blase für Spracheingabe")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(android.R.drawable.ic_delete, "Beenden", stopPi)
            .build()
    }

}
