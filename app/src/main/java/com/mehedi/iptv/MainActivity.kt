package com.mehedi.iptv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u"
private const val ONLINE_URL = "https://meheditv.site.je/api/online.php"
private const val TOTAL_URL = "https://meheditv.site.je/api/total.php"
private const val TICKER = "মেহেদী ইন্টারনেট সংযোগ নেওয়ার জন্য যোগাযোগ করুন ০১৬২৬৮৮৬০৭০"

data class Channel(val name: String, val url: String)

enum class Screen { LIST, PLAYER, OPTIONS }

class MainActivity : Activity() {
    private lateinit var player: ExoPlayer
    private var lastX = 0f; private var lastY = 0f
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent { MehediApp(player, ::playChannel) }
    }
    private fun playChannel(url: String) { player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.playWhenReady = true }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = super.onKeyDown(keyCode, event)
    override fun onDestroy() { player.release(); super.onDestroy() }
}

@Composable
fun MehediApp(player: ExoPlayer, playChannel: (String) -> Unit) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var screen by remember { mutableStateOf(Screen.LIST) }
    var selected by remember { mutableIntStateOf(0) }
    var online by remember { mutableStateOf("0") }
    var total by remember { mutableStateOf("0") }
    var tickerOffset by remember { mutableFloatStateOf(0f) }

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) { fetchM3U(PLAYLIST_URL) }
        if (result.isNotEmpty()) channels = result
        online = withContext(Dispatchers.IO) { fetchNumber(ONLINE_URL) }
        total = withContext(Dispatchers.IO) { fetchNumber(TOTAL_URL) }
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) { while (true) { delay(120_000); refresh() } }
    LaunchedEffect(Unit) { while (true) { delay(40); tickerOffset -= 1f; if (tickerOffset < -700f) tickerOffset = 0f } }

    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFF020711))) {
        when (screen) {
            Screen.LIST -> {
                Column(Modifier.fillMaxSize()) {
                    Header()
                    Ticker(tickerOffset)
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        ChannelList(channels, selected) { index ->
                            selected = index
                            screen = Screen.PLAYER
                            channels.getOrNull(index)?.let { playChannel(it.url) }
                        }
                    }
                    Footer(online, total)
                }
            }
            Screen.PLAYER -> {
                val ch = channels.getOrNull(selected)
                Box(Modifier.fillMaxSize()
                    .pointerInput(Unit) { detectHorizontalDragGestures(onDragEnd = {}, onHorizontalDrag = { _, drag ->
                        if (drag > 80) screen = Screen.LIST
                        if (drag < -80) screen = Screen.OPTIONS
                    }) }
                    .pointerInput(Unit) { detectVerticalDragGestures(onVerticalDrag = { _, drag ->
                        if (drag < -80 && channels.isNotEmpty()) { selected = (selected + 1) % channels.size; channels[selected].let { playChannel(it.url) } }
                        if (drag > 80 && channels.isNotEmpty()) { selected = (selected - 1 + channels.size) % channels.size; channels[selected].let { playChannel(it.url) } }
                    }) }
                ) {
                    AndroidView(factory = { PlayerView(context).apply { this.player = player; useController = true } }, Modifier.fillMaxSize())
                    if (ch != null) ChannelOverlay(ch.name, selected + 1)
                }
            }
            Screen.OPTIONS -> {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(factory = { PlayerView(context).apply { this.player = player; useController = true } }, Modifier.fillMaxSize())
                    OptionsPanel { screen = Screen.PLAYER }
                }
            }
        }
    }
}

@Composable fun Header() = Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF06152B)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("📺", fontSize = 26.sp); Spacer(Modifier.width(10.dp)); Text("Mehedi IPTV", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("⌕", color = Color.White, fontSize = 30.sp)
}
@Composable fun Ticker(offset: Float) = Box(Modifier.fillMaxWidth().height(38.dp).background(Color(0xFF087EEA)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) { Text("📢  $TICKER", color = Color.White, fontSize = 14.sp, modifier = Modifier.offset(x = offset.dp), maxLines = 1) }
@Composable fun ChannelList(channels: List<Channel>, selected: Int, onClick: (Int) -> Unit) = Column(Modifier.fillMaxSize().padding(10.dp)) {
    Text("📺  Channel List                 Auto Refresh (2m)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
    if (channels.isEmpty()) Text("No channels loaded", color = Color.LightGray, modifier = Modifier.padding(20.dp))
    LazyColumn { itemsIndexed(channels) { i, c -> Row(Modifier.fillMaxWidth().height(38.dp).background(if (i == selected) Color(0xFF087EEA) else Color.Transparent).clickable { onClick(i) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("%02d".format(i + 1), color = Color.White, modifier = Modifier.width(45.dp)); Text(c.name, color = Color.White, fontSize = 14.sp) } } }
}
@Composable fun Footer(online: String, total: String) = Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)) { HorizontalDivider(color = Color(0xFF0D75D7)); Text("©Mehedi Internet", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp)); Text("🟢 Online Now: $online   |   🔴 Total Visitor: $total", color = Color.White, modifier = Modifier.align(Alignment.End)) }
@Composable fun ChannelOverlay(name: String, number: Int) = Box(Modifier.fillMaxSize().padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) { Text("$name  |  $number", color = Color.White, fontSize = 18.sp, modifier = Modifier.background(Color(0xAA062448)).padding(horizontal = 16.dp, vertical = 9.dp)) }
@Composable fun OptionsPanel(onBack: () -> Unit) = Column(Modifier.fillMaxHeight().fillMaxWidth(0.38f).align(Alignment.CenterEnd).background(Color(0xDD050B14)).padding(22.dp)) { Text("Player Options", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp)); listOf("Subtitle / Audio", "Video / Format", "Original", "Fill / Stretch", "Zoom", "16:9", "2.35:1 Widescreen", "Add to Favorites", "EPG", "Playback Controls").forEach { Text(it, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 9.dp)) }; Text("← Back", color = Color.Cyan, modifier = Modifier.padding(top = 15.dp).clickable { onBack() }) }

suspend fun fetchM3U(url: String): List<Channel> = try { withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().use { r -> val lines = r.readLines(); val out = mutableListOf<Channel>(); var name = ""; for (line in lines) { if (line.startsWith("#EXTINF")) name = line.substringAfter(",", "Channel").trim(); else if (line.isNotBlank() && !line.startsWith("#") && name.isNotBlank()) { out += Channel(name, line.trim()); name = "" } }; out } } } catch (_: Exception) { emptyList() }
suspend fun fetchNumber(url: String): String = try { withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().use { it.readText().trim().let { body -> Regex("\\d+").find(body)?.value ?: "0" } } } } catch (_: Exception) { "0" }
