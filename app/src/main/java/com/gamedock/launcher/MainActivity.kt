package com.gamedock.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val prefs = remember { context.getSharedPreferences("gamedock", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var selectedTab by remember { mutableStateOf(0) }
    var accentIndex by remember { mutableIntStateOf(prefs.getInt("accent", 0)) }
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
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.SportsEsports, null) }, label = { Text("Library") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Star, null) }, label = { Text("Favorites") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Palette, null) }, label = { Text("Themes") })
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(20.dp))
                Text("GAMEDOCK", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(when (selectedTab) { 1 -> "Your favorites"; 2 -> "Gaming dashboard"; 3 -> "Make it yours"; else -> "Game library" },
                    color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                when (selectedTab) {
                    2 -> DashboardCard(games.size, favorites.size, accent)
                    3 -> {
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
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStat("DETECTED", games.size.toString(), accent, Modifier.weight(1f))
                            MiniStat("FAVORITES", favorites.size.toString(), accent, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(18.dp))
                        if (shownGames.isEmpty()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text(if (selectedTab == 1) "No favorites yet. Tap the star on a game." else "No games detected yet. Try installing a game.",
                                    color = Color.LightGray)
                            }
                        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(shownGames, key = { it.packageName }) { game ->
                                GameRow(game, favorites.contains(game.packageName), accent,
                                    onFavorite = {
                                        val updated = favorites.toMutableSet()
                                        if (!updated.add(game.packageName)) updated.remove(game.packageName)
                                        favorites = updated
                                        prefs.edit().putStringSet("favorites", updated).apply()
                                    }, onLaunch = {
                                        context.packageManager.getLaunchIntentForPackage(game.packageName)?.let(context::startActivity)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(label, color = Color.LightGray, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DashboardCard(gameCount: Int, favoriteCount: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your setup at a glance", color = Color.LightGray)
        MiniStat("GAMES FOUND", gameCount.toString(), accent, Modifier.fillMaxWidth())
        MiniStat("SAVED FAVORITES", favoriteCount.toString(), accent, Modifier.fillMaxWidth())
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Text("Performance tools", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Device temperature, live FPS and per-game performance metrics depend on Android and device-maker permissions. GameDock does not fake these readings or force system performance settings.",
                    color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GameRow(game: GameApp, favorite: Boolean, accent: Color, onFavorite: () -> Unit, onLaunch: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable(onClick = onLaunch).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Color(0xFF26334B), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SportsEsports, null, tint = accent)
        }
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
