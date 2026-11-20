package com.zerowisp.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.zerowisp.app.MainActivity
import com.zerowisp.app.R
import com.zerowisp.app.audio.AudioRecorder
import com.zerowisp.app.audio.TextCleaner
import com.zerowisp.app.engine.WhisperEngine
import com.zerowisp.app.data.AppSettings
import com.zerowisp.app.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class FloatingBubbleService : Service() {

    companion object {
        const val CHANNEL_ID = "zerowisp_bubble_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_HIDE = "com.zerowisp.app.SHOW_HIDE"
        const val ACTION_STOP = "com.zerowisp.app.STOP"
        const val ACTION_TRANSCRIPTION_READY = "com.zerowisp.app.TRANSCRIPTION_READY"
        const val EXTRA_TRANSCRIPTION = "transcription"

        var isRunning = false
            private set

        // Last transcription result — read by WispAccessibilityService
        @Volatile
        var lastTranscription: String = ""
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: BubbleView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var dismissZoneView: View? = null

    private val engine = WhisperEngine()
    private val recorder = AudioRecorder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var appSettings: AppSettings
    private lateinit var historyRepo: HistoryRepository

    private var contextPtr = 0L
    private var isVisible = true

    // Bubble states
    enum class BubbleState { IDLE, LISTENING, PROCESSING }
    @Volatile private var bubbleState = BubbleState.IDLE

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        appSettings = AppSettings(applicationContext)
        historyRepo = HistoryRepository(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupBubble()
        loadModel()
    }

    private fun loadModel() {
        scope.launch {
            val settings = appSettings.settingsFlow.first()
            val modelPath = settings.activeModelPath ?: return@launch
            if (contextPtr != 0L) {
                engine.freeContext(contextPtr)
                contextPtr = 0L
            }
            contextPtr = engine.initContext(modelPath)
        }
    }

    private fun setupBubble() {
        bubbleView = BubbleView(this)
        layoutParams = WindowManager.LayoutParams(
            (56 * resources.displayMetrics.density).toInt(),
            (56 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Track RMS for animation
        scope.launch {
            recorder.rmsFlow.collectLatest { rms ->
                if (bubbleState == BubbleState.LISTENING) {
                    bubbleView.setRms(rms)
                }
            }
        }

        setupTouchHandler()
        windowManager.addView(bubbleView, layoutParams)
    }

    private fun setupTouchHandler() {
        var downX = 0f
        var downY = 0f
        var initialLayoutX = 0
        var initialLayoutY = 0
        var isDragging = false
        val tapThresholdPx = 10 * resources.displayMetrics.density
        val tapTimeoutMs = 300L
        var downTime = 0L

        bubbleView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    initialLayoutX = layoutParams.x
                    initialLayoutY = layoutParams.y
                    isDragging = false
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!isDragging && (abs(dx) > tapThresholdPx || abs(dy) > tapThresholdPx)) {
                        isDragging = true
                        showDismissZone()
                    }
                    if (isDragging) {
                        layoutParams.x = (initialLayoutX + dx).toInt()
                        layoutParams.y = (initialLayoutY + dy).toInt()
                        windowManager.updateViewLayout(bubbleView, layoutParams)
                        checkDismissZone(event.rawX, event.rawY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideDismissZone()
                    if (!isDragging && (System.currentTimeMillis() - downTime) < tapTimeoutMs) {
                        // Tap: toggle recording
                        onBubbleTapped()
                    } else if (isDragging) {
                        // Edge snap
                        snapToEdge()
                        // Check if dropped on dismiss zone
                        if (isOverDismissZone(event.rawX, event.rawY)) {
                            stopSelf()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val displayWidth = resources.displayMetrics.widthPixels
        val bubbleWidth = layoutParams.width
        val midX = displayWidth / 2
        layoutParams.x = if (layoutParams.x + bubbleWidth / 2 < midX) 0
        else displayWidth - bubbleWidth
        windowManager.updateViewLayout(bubbleView, layoutParams)
    }

    private fun showDismissZone() {
        if (dismissZoneView != null) return
        // Simple X zone at bottom center — shown as a Toast hint for simplicity
        // A real implementation would add a WindowManager View
    }

    private fun hideDismissZone() {
        dismissZoneView?.let { windowManager.removeView(it) }
        dismissZoneView = null
    }

    private fun isOverDismissZone(rawX: Float, rawY: Float): Boolean {
        val displayHeight = resources.displayMetrics.heightPixels
        val dismissY = displayHeight * 0.85f
        val dismissCenterX = resources.displayMetrics.widthPixels / 2f
        return rawY > dismissY && abs(rawX - dismissCenterX) < 150
    }

    private fun checkDismissZone(rawX: Float, rawY: Float) {
        // Could animate the X zone highlight here
    }

    private fun onBubbleTapped() {
        when (bubbleState) {
            BubbleState.IDLE -> startRecording()
            BubbleState.LISTENING -> stopRecording()
            BubbleState.PROCESSING -> { /* ignore taps during processing */ }
        }
    }

    private fun startRecording() {
        if (contextPtr == 0L) {
            Toast.makeText(this, "No model loaded. Download a model first.", Toast.LENGTH_SHORT).show()
            return
        }
        bubbleState = BubbleState.LISTENING
        bubbleView.setState(BubbleState.LISTENING)
        updateNotification()

        val settings = appSettings  // cached ref
        val chunksQueue = ArrayDeque<FloatArray>()
        val processingResult = StringBuilder()

        recorder.startRecording(
            onChunkReady = { chunk ->
                scope.launch(WhisperEngine.dispatcher) {
                    val s = appSettings.settingsFlow.first()
                    val lang = if (s.alwaysOutputEnglish) "auto" else s.language
                    val raw = engine.transcribeAudio(
                        contextPtr, chunk,
                        translateToEnglish = s.alwaysOutputEnglish,
                        language = lang
                    )
                    val cleaned = TextCleaner.clean(raw, s.cleanFillerWords)
                    if (cleaned.isNotBlank()) {
                        synchronized(processingResult) {
                            if (processingResult.isNotEmpty()) processingResult.append(" ")
                            processingResult.append(cleaned)
                        }
                    }
                }
            },
            onFinalBuffer = { final ->
                scope.launch {
                    bubbleState = BubbleState.PROCESSING
                    bubbleView.setState(BubbleState.PROCESSING)
                    val s = appSettings.settingsFlow.first()
                    val lang = if (s.alwaysOutputEnglish) "auto" else s.language
                    val raw = engine.transcribeAudio(
                        contextPtr, final,
                        translateToEnglish = s.alwaysOutputEnglish,
                        language = lang
                    )
                    val cleaned = TextCleaner.clean(raw, s.cleanFillerWords)
                    if (cleaned.isNotBlank()) {
                        synchronized(processingResult) {
                            if (processingResult.isNotEmpty()) processingResult.append(" ")
                            processingResult.append(cleaned)
                        }
                    }

                    val fullResult = synchronized(processingResult) {
                        processingResult.toString().trim()
                    }
                    if (fullResult.isNotBlank()) {
                        deliverTranscription(fullResult)
                    }
                    bubbleState = BubbleState.IDLE
                    bubbleView.setState(BubbleState.IDLE)
                    updateNotification()
                }
            }
        )
    }

    private fun stopRecording() {
        recorder.stopRecording()
        // onFinalBuffer callback will handle state transition
    }

    private fun deliverTranscription(text: String) {
        lastTranscription = text
        scope.launch { historyRepo.addEntry(text) }

        // Try AccessibilityService paste
        val pasted = WispAccessibilityService.pasteText(text)
        if (!pasted) {
            // Fallback: clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("zerowisp", text))
            scope.launch(Dispatchers.Main) {
                Toast.makeText(this@FloatingBubbleService, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        // Also broadcast for in-app test box
        sendBroadcast(Intent(ACTION_TRANSCRIPTION_READY).apply {
            putExtra(EXTRA_TRANSCRIPTION, text)
        })
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Zero Wisp Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Zero Wisp floating bubble service" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val showHideIntent = PendingIntent.getService(
            this, 0, Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_SHOW_HIDE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zero Wisp")
            .setContentText(when (bubbleState) {
                BubbleState.IDLE -> "Idle — tap bubble to record"
                BubbleState.LISTENING -> "Listening…"
                BubbleState.PROCESSING -> "Processing…"
            })
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .addAction(0, "Show/Hide", showHideIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_HIDE -> toggleVisibility()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun toggleVisibility() {
        isVisible = !isVisible
        bubbleView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        isRunning = false
        recorder.stopRecording()
        scope.launch(WhisperEngine.dispatcher) {
            if (contextPtr != 0L) {
                engine.freeContext(contextPtr)
                contextPtr = 0L
            }
        }
        try { windowManager.removeView(bubbleView) } catch (_: Exception) {}
        hideDismissZone()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Custom Bubble View ────────────────────────────────────────────────────

    inner class BubbleView(context: Context) : View(context) {

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2A2E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        private val processingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        private val micPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        private var rms = 0f
        private var state = BubbleState.IDLE
        private var arcAngle = 0f
        private val rotationRunnable = object : Runnable {
            override fun run() {
                arcAngle = (arcAngle + 8f) % 360f
                invalidate()
                if (state == BubbleState.PROCESSING) {
                    postDelayed(this, 16)
                }
            }
        }

        fun setRms(newRms: Float) {
            rms = newRms
            invalidate()
        }

        fun setState(newState: BubbleState) {
            state = newState
            if (newState == BubbleState.PROCESSING) {
                post(rotationRunnable)
            } else {
                removeCallbacks(rotationRunnable)
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(cx, cy) - 4f

            // Background circle
            canvas.drawCircle(cx, cy, radius, bgPaint)

            // State-specific rendering
            when (state) {
                BubbleState.IDLE -> {
                    canvas.drawCircle(cx, cy, radius, borderPaint)
                }
                BubbleState.LISTENING -> {
                    // Pulsing accent ring based on RMS
                    val ringRadius = radius + rms * radius * 0.4f
                    accentPaint.alpha = (128 + (rms * 127).toInt()).coerceIn(0, 255)
                    canvas.drawCircle(cx, cy, ringRadius.coerceAtMost(radius + 8f), accentPaint)
                    canvas.drawCircle(cx, cy, radius, borderPaint)
                }
                BubbleState.PROCESSING -> {
                    canvas.drawCircle(cx, cy, radius, borderPaint)
                    val oval = android.graphics.RectF(cx - radius + 6, cy - radius + 6, cx + radius - 6, cy + radius - 6)
                    canvas.drawArc(oval, arcAngle, 270f, false, processingPaint)
                }
            }

            // Mic icon (simple rectangle approximation)
            val micW = radius * 0.3f
            val micH = radius * 0.5f
            val micRect = android.graphics.RectF(cx - micW / 2, cy - micH / 2, cx + micW / 2, cy + micH / 2 - micH * 0.1f)
            canvas.drawRoundRect(micRect, micW / 2, micW / 2, micPaint)
            // Mic stand
            canvas.drawLine(cx, cy + micH / 2 - micH * 0.1f, cx, cy + micH * 0.6f, micPaint)
            canvas.drawLine(cx - micW * 0.7f, cy + micH * 0.6f, cx + micW * 0.7f, cy + micH * 0.6f, micPaint)
        }
    }
}
