package com.gamedock.launcher

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
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
                .setContentText("GameDock can stay above your game. Tap Stop to dismiss it.")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("GameDock floating tools")
                .setContentText("GameDock can stay above your game.")
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
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Floating gaming tools", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun baseParams(width: Int = WindowManager.LayoutParams.WRAP_CONTENT): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(220)
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
        val layoutParams = baseParams()
        params = layoutParams
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = layoutParams.x; startY = layoutParams.y
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
                MotionEvent.ACTION_UP -> { if (!dragged) showPanel(); true }
                else -> true
            }
        }
        overlayView = bubble
        runCatching { windowManager.addView(bubble, layoutParams) }.onFailure { stopSelf() }
    }

    private fun showPanel(gamesTab: Boolean = false) {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(0xF0192236.toInt(), 22)
            elevation = dp(14).toFloat()
        }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(tab("Gaming tools", !gamesTab) { showPanel(false) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        tabs.addView(tab("Games", gamesTab) { showPanel(true) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        panel.addView(tabs)

        if (gamesTab) {
            addText(panel, "YOUR GAMES", 11, 0xFF71E6C1.toInt(), true, dp(10))
            val pm = packageManager
            val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val games = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
                .mapNotNull { info ->
                    val label = info.loadLabel(pm).toString()
                    val pkg = info.activityInfo.packageName
                    val key = (label + " " + pkg).lowercase()
                    val isGame = listOf("game", "pubg", "minecraft", "roblox", "freefire", "free fire",
                        "genshin", "callofduty", "call of duty", "clash", "asphalt", "geometrydash",
                        "geometry dash", "brawl", "stumble", "fortnite", "candy").any { key.contains(it) }
                    if (isGame && pkg != packageName) label to pkg else null
                }.distinctBy { it.second }.sortedBy { it.first.lowercase() }
            if (games.isEmpty()) addText(panel, "No games detected yet.", 13, 0xFFFFFFFF.toInt())
            games.take(12).forEach { (label, pkg) ->
                val item = actionText("$label  ›") {
                    packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                panel.addView(item, rowParams())
            }
        } else {
            addText(panel, "DEVICE STATUS", 10, 0xFFB8C6D9.toInt(), true, dp(8))
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memory = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(memory)
            val total = if (memory.totalMem > 0) memory.totalMem else memory.availMem
            val usedPercent = if (total > 0) (((total - memory.availMem).toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0
            val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val tempText = if (temp >= 0) String.format("%.1f°C", temp / 10f) else "Unavailable"
            val batteryLevel = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val batteryText = if (batteryLevel >= 0) "$batteryLevel%" else "Unavailable"
            val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val power = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                when (power.currentThermalStatus) {
                    0 -> "Normal"; 1 -> "Light"; 2 -> "Moderate"; 3 -> "Severe"
                    4 -> "Critical"; 5 -> "Emergency"; 6 -> "Shutdown"; else -> "Unknown"
                }
            } else "Not supported"
            val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            stats.addView(statCard("RAM USED", "$usedPercent%", 1f), LinearLayout.LayoutParams(0, dp(62), 1f))
            stats.addView(statCard("BATTERY", batteryText, 1f), LinearLayout.LayoutParams(0, dp(62), 1f))
            panel.addView(stats)
            val fps = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
            }
            addText(fps, "FPS", 10, 0xFFB8C6D9.toInt(), true)
            addText(fps, "—", 32, 0xFFFFFFFF.toInt(), true)
            addText(fps, "Not exposed by Android", 10, 0xFFB8C6D9.toInt())
            panel.addView(fps)
            addText(panel, "Thermal: $thermal  •  Battery temp: $tempText", 11, 0xFFD6DFEA.toInt(), false, dp(5))
            addText(panel, "RAM available: " + String.format("%.1f GB", memory.availMem / (1024.0 * 1024.0 * 1024.0)), 11, 0xFFD6DFEA.toInt(), false, dp(5))
            addText(panel, "Performance mode", 11, 0xFFB8C6D9.toInt(), true, dp(10))
            panel.addView(actionText("Open Android Game Mode  ›") { openSetting("android.settings.GAME_MODE_SETTINGS") }, rowParams())
            addText(panel, "GAMING TOOLS", 10, 0xFFB8C6D9.toInt(), true, dp(9))
            val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row1.addView(gridAction("Display", Settings.ACTION_DISPLAY_SETTINGS), LinearLayout.LayoutParams(0, dp(46), 1f))
            row1.addView(gridAction("Sound", Settings.ACTION_SOUND_SETTINGS), LinearLayout.LayoutParams(0, dp(46), 1f))
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row2.addView(gridAction("Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS), LinearLayout.LayoutParams(0, dp(46), 1f))
            row2.addView(gridAction("DND", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), LinearLayout.LayoutParams(0, dp(46), 1f))
            grid.addView(row1); grid.addView(row2)
            panel.addView(grid)
            panel.addView(actionText("Refresh device information  ↻") { showPanel(false) }, rowParams())
        }

        panel.addView(actionText(if (gamesTab) "Back to Gaming tools" else "Minimize to GD ⚡") {
            if (gamesTab) showPanel(false) else showBubble()
        }, rowParams())
        panel.addView(actionText("Turn off floating tools  ×", 0xFFFFA8A8.toInt()) { stopSelf() }, rowParams())
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(panel) }
        val screenHeight = resources.displayMetrics.heightPixels
        val p = params ?: baseParams()
        p.width = dp(310)
        p.height = minOf(dp(560), (screenHeight * 0.78f).toInt())
        expanded = true
        overlayView = scroll
        runCatching { windowManager.addView(scroll, p) }.onFailure { stopSelf() }
    }

    private fun tab(label: String, selected: Boolean, click: () -> Unit) = TextView(this).apply {
        text = label; gravity = Gravity.CENTER; textSize = 13f
        typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        setTextColor(if (selected) 0xFF71E6C1.toInt() else 0xFFFFFFFF.toInt())
        setOnClickListener { click() }
    }

    private fun statCard(label: String, value: String, weight: Float) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(9), dp(7), dp(9), dp(7))
        background = rounded(0xFF26344D.toInt(), 12)
        addText(this, label, 9, 0xFFB8C6D9.toInt(), true)
        addText(this, value, 19, 0xFFFFFFFF.toInt(), true)
    }

    private fun gridAction(label: String, setting: String) = actionText(label) { openSetting(setting) }

    private fun actionText(label: String, color: Int = 0xFFFFFFFF.toInt(), click: () -> Unit) = TextView(this).apply {
        text = label; textSize = 12f; gravity = Gravity.CENTER
        setTextColor(color); setPadding(dp(8), dp(10), dp(8), dp(10))
        background = rounded(0xFF26344D.toInt(), 11)
        setOnClickListener { click() }
    }

    private fun addText(parent: LinearLayout, text: String, size: Int, color: Int, bold: Boolean = false, top: Int = 0) {
        val view = TextView(this).apply {
            this.text = text; textSize = size.toFloat(); setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), top, dp(5), dp(5))
        }
        parent.addView(view, rowParams())
    }

    private fun openSetting(action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            if (intent.resolveActivity(packageManager) != null) startActivity(intent)
            else startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    private fun rowParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(6) }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
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
