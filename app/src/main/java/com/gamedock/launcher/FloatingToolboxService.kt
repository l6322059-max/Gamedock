package com.gamedock.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class FloatingToolboxService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var expanded = false
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, FloatingToolboxService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GameDock floating tools")
                .setContentText("Your gaming toolbox is ready. Tap Stop to dismiss it.")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("GameDock floating tools")
                .setContentText("Your gaming toolbox is ready.")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build()
        }
        startForeground(NOTIFICATION_ID, notification)
        if (overlayView == null) showBubble()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Floating gaming tools", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun showBubble() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        expanded = false
        val bubble = TextView(this).apply {
            text = "GD ⚡"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF07121D.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = rounded(0xFF71E6C1.toInt(), 28)
            elevation = dp(8).toFloat()
        }
        @Suppress("DEPRECATION")
        val overlayType = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(220)
        }
        params = layoutParams
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > dp(5) || kotlin.math.abs(dy) > dp(5)) dragged = true
                    if (dragged) {
                        layoutParams.x = (startX - dx).coerceAtLeast(0)
                        layoutParams.y = (startY + dy).coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(bubble, layoutParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) showPanel()
                    true
                }
                else -> true
            }
        }
        overlayView = bubble
        runCatching { windowManager.addView(bubble, layoutParams) }.onFailure { stopSelf() }
    }

    private fun showPanel() {
        if (expanded) {
            showBubble()
            return
        }
        val old = overlayView ?: return
        runCatching { windowManager.removeView(old) }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(0xF0192236.toInt(), 20)
            elevation = dp(12).toFloat()
        }
        val header = TextView(this).apply {
            text = "GAMEDOCK  ⚡"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF71E6C1.toInt())
            setPadding(dp(4), dp(4), dp(4), dp(10))
        }
        panel.addView(header)
        addAction(panel, "Display & brightness", Settings.ACTION_DISPLAY_SETTINGS)
        addAction(panel, "Sound & volume", Settings.ACTION_SOUND_SETTINGS)
        addAction(panel, "Battery & power", Settings.ACTION_BATTERY_SAVER_SETTINGS)
        addAction(panel, "Do Not Disturb access", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        val minimize = TextView(this).apply {
            text = "Minimize bubble"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF07121D.toInt())
            setPadding(dp(10), dp(11), dp(10), dp(11))
            background = rounded(0xFF71E6C1.toInt(), 12)
            setOnClickListener { showBubble() }
        }
        panel.addView(minimize, rowParams())
        val close = TextView(this).apply {
            text = "Turn off floating tools"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFA8A8.toInt())
            setPadding(dp(8), dp(12), dp(8), dp(6))
            setOnClickListener { stopSelf() }
        }
        panel.addView(close, rowParams())
        val p = params ?: return
        p.width = dp(236)
        p.height = WindowManager.LayoutParams.WRAP_CONTENT
        expanded = true
        overlayView = panel
        runCatching { windowManager.addView(panel, p) }.onFailure { stopSelf() }
    }

    private fun addAction(parent: LinearLayout, label: String, action: String) {
        val item = TextView(this).apply {
            text = label + "  ›"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(10), dp(12), dp(10), dp(12))
            background = rounded(0xFF26344D.toInt(), 12)
            setOnClickListener {
                val launch = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { startActivity(launch) }
            }
        }
        parent.addView(item, rowParams())
    }

    private fun rowParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(7) }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.gamedock.launcher.STOP_FLOATING_TOOLS"
        private const val CHANNEL_ID = "gamedock_floating_tools"
        private const val NOTIFICATION_ID = 7412
    }
}
