package com.gamedock.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xFF171F33)
private val Accent = Color(0xFF71E6C1)

data class GameApp(val label: String, val packageName: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameDockScreen() }
    }
}

@Composable
fun GameDockScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val prefs = remember { context.getSharedPreferences("gamedock", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var accentIndex by remember { mutableIntStateOf(prefs.getInt("accent", 0)) }
    var showToolbox by remember { mutableStateOf(false) }
    val accents = listOf(Accent, Color(0xFF8DA8FF), Color(0xFFFFB86B), Color(0xFFE59BFF))
    val accent = accents[accentIndex.coerceIn(accents.indices)]

    val games = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val appName = info.loadLabel(pm).toString()
                val pkg = info.activityInfo.packageName
                val key = (appName + " " + pkg).lowercase()
                val likelyGame = listOf("game", "play", "pubg", "minecraft", "roblox", "freefire", "free fire",
                    "mobile legends", "genshin", "callofduty", "call of duty", "clash", "asphalt",
                    "geometrydash", "geometry dash", "brawl", "stumble", "fortnite").any { key.contains(it) }
                if (pkg != context.packageName && likelyGame) GameApp(appName, pkg) else null
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    val shownGames = if (selectedTab == 1) games.filter { favorites.contains(it.packageName) } else games

    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = Ink, surface = Panel)) {
        Scaffold(
            containerColor = Ink,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showToolbox = true },
                    containerColor = accent,
                    contentColor = Ink,
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    text = { Text("Gaming tools", fontWeight = FontWeight.Bold) }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Color.White) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.SportsEsports, null) }, label = { Text("Library") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Star, null) }, label = { Text("Favorites") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("Turbo") })
                    NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Palette, null) }, label = { Text("Themes") })
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(20.dp))
                Text("GAMEDOCK", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(when (selectedTab) {
                    1 -> "Your favorites"
                    2 -> "Gaming dashboard"
                    3 -> "Game Turbo"
                    4 -> "Make it yours"
                    else -> "Game library"
                }, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                when (selectedTab) {
                    0 -> {
                        HeroCard(accent)
                        Spacer(Modifier.height(16.dp))
                        GameLibrary(games, shownGames, favorites, accent) { game ->
                            val updated = favorites.toMutableSet()
                            if (!updated.add(game.packageName)) updated.remove(game.packageName)
                            favorites = updated
                            prefs.edit().putStringSet("favorites", updated).apply()
                        }
                    }
                    1 -> GameLibrary(games, shownGames, favorites, accent) { game ->
                        val updated = favorites.toMutableSet()
                        if (!updated.add(game.packageName)) updated.remove(game.packageName)
                        favorites = updated
                        prefs.edit().putStringSet("favorites", updated).apply()
                    }
                    2 -> DashboardCard(games.size, favorites.size, accent)
                    3 -> TurboScreen(games.size, accent) { showToolbox = true }
                    4 -> {
                        Text("Choose an accent", color = Color.LightGray)
                        Spacer(Modifier.height(14.dp))
                        accents.forEachIndexed { index, color ->
                            Row(Modifier.fillMaxWidth().clickable {
                                accentIndex = index
                                prefs.edit().putInt("accent", index).apply()
                            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(28.dp).background(color, RoundedCornerShape(50)))
                                Spacer(Modifier.width(12.dp))
                                Text(listOf("Mint", "Blue", "Amber", "Violet")[index], color = Color.White)
                                Spacer(Modifier.weight(1f))
                                if (accentIndex == index) Icon(Icons.Default.Check, null, tint = color)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showToolbox) {
        GamingToolboxDialog(accent = accent, onDismiss = { showToolbox = false })
    }
}

@Composable
private fun GameLibrary(
    games: List<GameApp>,
    shownGames: List<GameApp>,
    favorites: Set<String>,
    accent: Color,
    onFavorite: (GameApp) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = shownGames.filter {
        it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStat("DETECTED", games.size.toString(), accent, Modifier.weight(1f))
        MiniStat("FAVORITES", favorites.size.toString(), accent, Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Search games or packages…", color = Color.LightGray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Color(0xFF39445C),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = accent
        )
    )
    Spacer(Modifier.height(14.dp))
    if (filteredGames.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) "No games match “$searchQuery”."
                else if (favorites.isNotEmpty()) "No games found."
                else "No games here yet. Tap the star on a game to save it.",
                color = Color.LightGray
            )
        }
    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 84.dp)) {
        items(filteredGames, key = { it.packageName }) { game ->
            GameRow(game, favorites.contains(game.packageName), accent, onFavorite = { onFavorite(game) })
        }
    }
}

@Composable
private fun HeroCard(accent: Color) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF24204D), Color(0xFF102D43), Color(0xFF171F33))), RoundedCornerShape(22.dp))
            .padding(start = 18.dp, top = 14.dp, end = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("READY PLAYER ONE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            Text("Your games.\nOne launchpad.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
            Spacer(Modifier.height(5.dp))
            Text("Play more. Better.", color = Color.LightGray, fontSize = 12.sp)
        }
        Image(painter = painterResource(R.drawable.gamedock_hero), contentDescription = "Neon GameDock controller illustration",
            modifier = Modifier.width(142.dp).height(100.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(label, color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

private data class PerformanceSnapshot(
    val freeRam: String,
    val usedRam: String,
    val batteryTemp: String,
    val thermalStatus: String,
    val cpuCores: String,
    val chipset: String
)

private fun readPerformance(context: Context): PerformanceSnapshot {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memory)
    val gb = 1024.0 * 1024.0 * 1024.0
    val total = if (memory.totalMem > 0L) memory.totalMem else memory.availMem
    val used = (total - memory.availMem).coerceAtLeast(0L)
    val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val tenthsC = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val batteryTemp = if (tenthsC >= 0) String.format("%.1f°C", tenthsC / 10.0) else "Unavailable"
    val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val power = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        when (power.currentThermalStatus) {
            android.os.PowerManager.THERMAL_STATUS_NONE -> "Normal"
            android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Light"
            android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
            android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
            android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
            android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
            android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
            else -> "Unknown"
        }
    } else "Not supported"
    val chipset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: Build.HARDWARE else Build.HARDWARE
    return PerformanceSnapshot(
        freeRam = String.format("%.1f GB", memory.availMem / gb),
        usedRam = String.format("%.1f GB", used / gb),
        batteryTemp = batteryTemp,
        thermalStatus = thermal,
        cpuCores = Runtime.getRuntime().availableProcessors().toString(),
        chipset = chipset.ifBlank { "Unavailable" }
    )
}

private fun openGameModeSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent("android.settings.GAME_MODE_SETTINGS")
    } else Intent(Settings.ACTION_SETTINGS)
    runCatching {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }.onFailure {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun DashboardCard(gameCount: Int, favoriteCount: Int, accent: Color) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(readPerformance(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your setup at a glance", color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("GAMES", gameCount.toString(), accent, Modifier.weight(1f))
            MiniStat("FAVORITES", favoriteCount.toString(), accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("FREE RAM", stats.freeRam, accent, Modifier.weight(1f))
            MiniStat("USED RAM", stats.usedRam, accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("BATTERY TEMP", stats.batteryTemp, accent, Modifier.weight(1f))
            MiniStat("THERMAL STATUS", stats.thermalStatus, accent, Modifier.weight(1f))
        }
        InfoCard("Hardware", "Chipset: ${stats.chipset} • Available CPU cores: ${stats.cpuCores}")
        OutlinedButton(onClick = { stats = readPerformance(context) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null, tint = accent)
            Spacer(Modifier.width(8.dp))
            Text("Refresh performance information", color = Color.White)
        }
        InfoCard("Live FPS", "Android does not provide ordinary apps with a universal way to read another game's FPS. GameDock will not invent an FPS number.")
        InfoCard("Game Mode", "Use Android's own Game Mode settings when your phone provides them. Available modes and controls depend on your manufacturer and Android version.")
    }
}

@Composable
private fun TurboScreen(gameCount: Int, accent: Color, onOpenTools: () -> Unit) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(readPerformance(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFF203D50), Color(0xFF1A2040), Panel)), RoundedCornerShape(22.dp)
        ).padding(18.dp)) {
            Column {
                Text("TURBO MODE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your gaming command center", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("$gameCount games in your library", color = Color.LightGray, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenTools, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink)) {
                    Icon(Icons.Default.Bolt, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open floating-style toolbox")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("FREE RAM", stats.freeRam, accent, Modifier.weight(1f))
            MiniStat("USED RAM", stats.usedRam, accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("BATTERY TEMP", stats.batteryTemp, accent, Modifier.weight(1f))
            MiniStat("THERMAL STATUS", stats.thermalStatus, accent, Modifier.weight(1f))
        }
        InfoCard("Hardware", "Chipset: ${stats.chipset} • CPU cores: ${stats.cpuCores}")
        OutlinedButton(onClick = { stats = readPerformance(context) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, null, tint = accent)
            Spacer(Modifier.width(8.dp))
            Text("Refresh device status", color = Color.White)
        }
        Button(onClick = { openGameModeSettings(context) }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink)) {
            Icon(Icons.Default.SportsEsports, null)
            Spacer(Modifier.width(8.dp))
            Text("Open Android Game Mode settings")
        }
        InfoCard("FPS monitor", "Live FPS for other games is not available to ordinary apps on most Android devices. GameDock reports this honestly instead of showing a fake counter.")
        InfoCard("Performance controls", "GameDock reads public device status and opens supported Android settings. It cannot force CPU/GPU overclocking or bypass manufacturer thermal protections.")
        InfoCard("Game detection", "GameDock scans launcher-visible apps. Some games may need to be added manually in a future version.")
    }
}

@Composable
private fun GamingToolboxDialog(accent: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("Gaming toolbox", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick access to Android controls", color = Color.LightGray, fontSize = 12.sp)
                ToolButton("Enable floating bubble", Icons.Default.OpenInNew, accent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        val serviceIntent = Intent(context, FloatingToolboxService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
                        else context.startService(serviceIntent)
                    }
                }
                ToolButton("Display & brightness", Icons.Default.Brightness6, accent) {
                    openSystemSetting(context, Settings.ACTION_DISPLAY_SETTINGS)
                }
                ToolButton("Sound & volume", Icons.Default.VolumeUp, accent) {
                    openSystemSetting(context, Settings.ACTION_SOUND_SETTINGS)
                }
                ToolButton("Battery & power", Icons.Default.BatteryChargingFull, accent) {
                    openSystemSetting(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                }
                ToolButton("Do Not Disturb access", Icons.Default.DoNotDisturbOn, accent) {
                    openSystemSetting(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                }
                ToolButton("All Android settings", Icons.Default.Settings, accent) {
                    openSystemSetting(context, Settings.ACTION_SETTINGS)
                }
                Text("Enable the bubble once, grant Display over other apps permission, then return to GameDock and tap Enable floating bubble again. The draggable bubble stays above games until you turn it off from its notification or panel.", color = Color.LightGray, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = accent) } }
    )
}

@Composable
private fun ToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
        Icon(icon, null, tint = accent)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

private fun openSystemSetting(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun readMemory(context: Context): String {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    return String.format("%.1f GB", info.availMem / (1024.0 * 1024.0 * 1024.0))
}

private fun readBatteryTemperature(context: Context): String {
    val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val tenthsC = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    return if (tenthsC >= 0) String.format("%.1f°C", tenthsC / 10.0) else "Unavailable"
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun GameRow(game: GameApp, favorite: Boolean, accent: Color, onFavorite: () -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable {
        context.packageManager.getLaunchIntentForPackage(game.packageName)?.let(context::startActivity)
    }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        AndroidView(factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(runCatching { context.packageManager.getApplicationIcon(game.packageName) }.getOrNull())
            }
        }, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF26334B)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(game.label, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Tap to launch", color = Color.LightGray, fontSize = 11.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (favorite) accent else Color.LightGray)
        }
    }
}
