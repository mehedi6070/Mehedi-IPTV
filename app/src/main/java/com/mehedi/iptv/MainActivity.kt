package com.mehedi.iptv

import android.app.Activity.conponentActivity
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u"
private const val ONLINE_URL = "https://meheditv.site.je/api/online.php"
private const val TOTAL_URL = "https://meheditv.site.je/api/total.php"
private const val TICKER = "মেহেদী ইন্টারনেট সংযোগ নেওয়ার জন্য যোগাযোগ করুন ০১৬২৬৮৮৬০৭০"

private val Navy = Color(0xFF020A18)
private val Panel = Color(0xFF07182F)
private val Blue = Color(0xFF087EEA)
private val Blue2 = Color(0xFF0B4FA7)
private val Gold = Color(0xFFD8A72C)
private val White = Color.White

data class Channel(val name: String, val url: String)
enum class Screen { LIST, PLAYER, OPTIONS }

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build()

        setContent {
            MehediApp(player)
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun MehediApp(player: ExoPlayer) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selected by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(Screen.LIST) }
    var online by remember { mutableStateOf("0") }
    var total by remember { mutableStateOf("0") }
    var splash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(650)
        splash = false
    }

    suspend fun refresh() {
        val list = fetchM3U(PLAYLIST_URL)
        if (list.isNotEmpty()) {
            channels = list
            if (selected >= list.size) selected = 0
        }
        online = fetchNumber(ONLINE_URL)
        total = fetchNumber(TOTAL_URL)
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120_000)
            refresh()
        }
    }

    fun play(index: Int) {
        if (channels.isEmpty()) return
        val safe = index.coerceIn(0, channels.lastIndex)
        selected = safe
        player.setMediaItem(MediaItem.fromUri(channels[safe].url))
        player.prepare()
        player.playWhenReady = true
        screen = Screen.PLAYER
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(screen, channels.size) { focusRequester.requestFocus() }

    if (splash) {
        SplashScreen()
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (screen) {
                    Screen.LIST -> when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> { if (channels.isNotEmpty()) selected = (selected + 1) % channels.size; true }
                        KeyEvent.KEYCODE_DPAD_UP -> { if (channels.isNotEmpty()) selected = (selected - 1 + channels.size) % channels.size; true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { play(selected); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { if (channels.isNotEmpty()) play(selected); true }
                        else -> false
                    }
                    Screen.PLAYER -> when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> { if (channels.isNotEmpty()) play((selected + 1) % channels.size); true }
                        KeyEvent.KEYCODE_DPAD_DOWN -> { if (channels.isNotEmpty()) play((selected - 1 + channels.size) % channels.size); true }
                        KeyEvent.KEYCODE_DPAD_LEFT -> { screen = Screen.OPTIONS; true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { screen = Screen.LIST; true }
                        KeyEvent.KEYCODE_BACK -> { screen = Screen.LIST; true }
                        else -> false
                    }
                    Screen.OPTIONS -> when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BACK -> { screen = Screen.PLAYER; true }
                        else -> false
                    }
                }
            }
    ) {
        when (screen) {
            Screen.LIST -> ListScreen(channels, selected, online, total, ::play)
            Screen.PLAYER -> PlayerScreen(player, channels.getOrNull(selected), selected + 1, channels.size, ::play, { screen = Screen.LIST }, { screen = Screen.OPTIONS })
            Screen.OPTIONS -> OptionsScreen { screen = Screen.PLAYER }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(118)
            Spacer(Modifier.height(16.dp))
            Text("Mehedi", color = White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text("IPTV", color = Blue, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text("Live TV  •  Sports  •  Entertainment", color = Color.LightGray, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ListScreen(channels: List<Channel>, selected: Int, online: String, total: String, play: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppHeader()
        MovingTicker()
        Row(Modifier.weight(1f).fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(0.43f).fillMaxHeight()) {
                AllChannelsCard(channels.size)
                Spacer(Modifier.height(8.dp))
                Text("Channel List", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Panel).border(1.dp, Blue2, RoundedCornerShape(8.dp))) {
                    if (channels.isEmpty()) {
                        Text("Loading channels…", color = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(contentPadding = PaddingValues(4.dp)) {
                            itemsIndexed(channels) { index, channel ->
                                val active = index == selected
                                Row(
                                    Modifier.fillMaxWidth().height(39.dp).clip(RoundedCornerShape(4.dp))
                                        .background(if (active) Blue else Color.Transparent)
                                        .clickable { play(index) }.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("%02d".format(index + 1), color = White, modifier = Modifier.width(42.dp))
                                    Text(channel.name, color = White, fontSize = 14.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.weight(0.57f).fillMaxHeight()) {
                PreviewCard(channels.getOrNull(selected))
                Spacer(Modifier.height(8.dp))
                NavigationHint()
            }
        }
        Footer(online, total)
    }
}

@Composable
private fun AppHeader() {
    Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF06152B)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoMark(38)
        Spacer(Modifier.width(9.dp))
        Column {
            Text("Mehedi", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("IPTV", color = Blue, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.weight(1f))
        Text("⌕", color = White, fontSize = 28.sp)
    }
}

@Composable
private fun MovingTicker() {
    var x by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(35)
            x -= 1.1f
            if (x < -950f) x = 0f
        }
    }
    Box(Modifier.fillMaxWidth().height(34.dp).background(Blue), contentAlignment = Alignment.CenterStart) {
        Text("📢  $TICKER", color = White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.offset(x.dp))
    }
}

@Composable
private fun AllChannelsCard(count: Int) {
    Box(
        Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF092451)).border(2.dp, Gold, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(48)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("All Channels", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(if (count == 0) "Channel List" else "$count Channels", color = Color.LightGray, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("›", color = White, fontSize = 34.sp)
        }
    }
}

@Composable
private fun PreviewCard(channel: Channel?) {
    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF01050C)).border(1.dp, Blue2, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(88)
            Spacer(Modifier.height(10.dp))
            Text(channel?.name ?: "Select a channel", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Press OK / Select to Play", color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun NavigationHint() {
    Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(8.dp)).background(Panel).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("← Player Options", color = White, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text("↑ Next  •  ↓ Previous", color = Color.LightGray, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text("Channel List →", color = White, fontSize = 14.sp)
    }
}

@Composable
private fun PlayerScreen(player: ExoPlayer, channel: Channel?, number: Int, count: Int, play: (Int) -> Unit, toList: () -> Unit, toOptions: () -> Unit) {
    val context = LocalContext.current
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(count) {
                detectHorizontalDragGestures(onHorizontalDrag = { _, drag -> dx += drag }, onDragEnd = { if (dx > 100) toList(); if (dx < -100) toOptions(); dx = 0f })
            }
            .pointerInput(count) {
                detectVerticalDragGestures(onVerticalDrag = { _, drag -> dy += drag }, onDragEnd = { if (count > 0 && dy < -100) play((number) % count); if (count > 0 && dy > 100) play((number - 2 + count) % count); dy = 0f })
            }
    ) {
        AndroidView(factory = { PlayerView(context).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize())
        if (channel != null) {
            Box(Modifier.fillMaxSize().padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) {
                Text(channel.name + "  |  " + number, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xCC062448)).border(1.dp, Blue, RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun OptionsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF050B14)).padding(24.dp)) {
        Text("Player Options", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        listOf("Subtitle / Audio", "Video / Format", "Original", "Fill / Stretch", "Zoom", "16:9", "2.35:1 Widescreen", "Add to Favorites", "EPG", "Playback Controls", "More Options").forEach {
            Text("▸  $it", color = White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Text("→ Back to Player", color = Blue, fontSize = 17.sp, modifier = Modifier.clickable { onBack() })
    }
}

@Composable
private fun Footer(online: String, total: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("©Mehedi Net", color = White, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text("🟢 Online : $online   |   Total Visitor : $total", color = White, fontSize = 13.sp)
    }
}

@Composable
private fun LogoMark(size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 5).dp)).background(Color(0xFF071B3B)).border(2.dp, Blue, RoundedCornerShape((size / 5).dp)), contentAlignment = Alignment.Center) {
        Text("▶", color = White, fontSize = (size / 2.7f).sp, fontWeight = FontWeight.Bold)
    }
}

private suspend fun fetchM3U(url: String): List<Channel> = try {
    withContext(Dispatchers.IO) {
        URL(url).openStream().bufferedReader().use { reader ->
            val out = mutableListOf<Channel>(); var name = ""
            reader.forEachLine { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> name = line.substringAfter(",", "Channel").trim()
                    line.isNotBlank() && !line.startsWith("#") && name.isNotBlank() -> { out += Channel(name, line.trim()); name = "" }
                }
            }
            out
        }
    }
} catch (_: Exception) { emptyList() }

private suspend fun fetchNumber(url: String): String = try {
    withContext(Dispatchers.IO) {
        URL(url).openStream().bufferedReader().use { text ->
            Regex("\\d+").find(text.readText())?.value ?: "0"
        }
    }
} catch (_: Exception) { "0" }
