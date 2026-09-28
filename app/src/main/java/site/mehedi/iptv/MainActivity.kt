package site.mehedi.iptv

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val M3U_URL =
            "https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u"
        private const val ONLINE_API =
            "https://meheditv.site.je/api/online.php?i=1"
        private const val TOTAL_API =
            "https://meheditv.site.je/api/total.php"

        private const val LIST_REFRESH_MS = 2 * 60 * 1000L
        private const val FULLSCREEN_TICKER_DELAY_MS = 2 * 60 * 1000L
        private const val FULLSCREEN_TICKER_VISIBLE_MS = 60 * 1000L
    }

    private lateinit var player: ExoPlayer
    private lateinit var adapter: ChannelAdapter
    private val channels = mutableListOf<Channel>()
    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0

    private val listRefresh = object : Runnable {
        override fun run() {
            loadM3U()
            handler.postDelayed(this, LIST_REFRESH_MS)
        }
    }

    private val tickerShow = object : Runnable {
        override fun run() {
            findViewById<View>(R.id.fullscreenTicker).visibility = View.VISIBLE
            handler.postDelayed(tickerHide, FULLSCREEN_TICKER_VISIBLE_MS)
        }
    }

    private val tickerHide = Runnable {
        findViewById<View>(R.id.fullscreenTicker).visibility = View.GONE
        handler.postDelayed(tickerShow, FULLSCREEN_TICKER_DELAY_MS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        player = ExoPlayer.Builder(this).build()
        findViewById<androidx.media3.ui.PlayerView>(R.id.playerView).player = player

        adapter = ChannelAdapter(channels) { position ->
            playChannel(position)
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.channelList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<TextView>(R.id.previousButton).setOnClickListener {
            if (channels.isNotEmpty()) playChannel((currentIndex - 1 + channels.size) % channels.size)
        }
        findViewById<TextView>(R.id.nextButton).setOnClickListener {
            if (channels.isNotEmpty()) playChannel((currentIndex + 1) % channels.size)
        }

        startClock()
        loadM3U()
        refreshVisitors()
        handler.postDelayed(listRefresh, LIST_REFRESH_MS)

        // Fullscreen ticker: first appears after 2 minutes, stays ~1 minute,
        // then repeats every 2 minutes.
        handler.postDelayed(tickerShow, FULLSCREEN_TICKER_DELAY_MS)
    }

    private fun loadM3U() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { parseM3U(downloadText(M3U_URL)) }.getOrDefault(emptyList())
            }
            if (result.isNotEmpty()) {
                val oldName = channels.getOrNull(currentIndex)?.name
                channels.clear()
                channels.addAll(result)
                adapter.submitList(channels.toList())
                findViewById<TextView>(R.id.channelCount).text =
                    "All Channels (${channels.size})"
                val restored = oldName?.let { name ->
                    channels.indexOfFirst { it.name == name }.takeIf { it >= 0 }
                }
                if (restored != null) currentIndex = restored
                if (player.currentMediaItem == null) playChannel(currentIndex)
            }
        }
    }

    private fun refreshVisitors() {
        lifecycleScope.launch {
            val online = withContext(Dispatchers.IO) { fetchValue(ONLINE_API, "online") }
            val total = withContext(Dispatchers.IO) { fetchValue(TOTAL_API, "total") }
            findViewById<TextView>(R.id.onlineText).text = "● Online: ${online ?: "0"}"
            findViewById<TextView>(R.id.totalText).text = "👥 Total Visitor: ${total ?: "0"}"
        }
        handler.postDelayed({ refreshVisitors() }, 60_000L)
    }

    private fun playChannel(position: Int) {
        if (position !in channels.indices) return
        currentIndex = position
        adapter.setSelected(position)

        val channel = channels[position]
        player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
        player.prepare()
        player.play()

        findViewById<TextView>(R.id.currentChannel).text =
            "${channel.name}  |  %03d".format(position + 1)

        // Start/restart the fullscreen ticker cycle when entering a channel.
        handler.removeCallbacks(tickerShow)
        handler.removeCallbacks(tickerHide)
        findViewById<View>(R.id.fullscreenTicker).visibility = View.GONE
        handler.postDelayed(tickerShow, FULLSCREEN_TICKER_DELAY_MS)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (channels.isNotEmpty()) playChannel((currentIndex + 1) % channels.size)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (channels.isNotEmpty()) playChannel((currentIndex - 1 + channels.size) % channels.size)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun parseM3U(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var pendingName: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingName = line.substringAfterLast(",").trim().ifBlank { "Channel" }
                }
                line.isNotBlank() && !line.startsWith("#") && pendingName != null -> {
                    out += Channel(pendingName!!, line)
                    pendingName = null
                }
            }
        }
        return out
    }

    private fun downloadText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }
            .also { conn.disconnect() }
    }

    private fun fetchValue(url: String, key: String): String? {
        return runCatching {
            val body = downloadText(url)
            Regex("\"?$key\"?\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?")
                .find(body)?.groupValues?.getOrNull(1)
        }.getOrNull()
    }

    private fun startClock() {
        val view = findViewById<TextView>(R.id.timeText)
        val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        handler.post(object : Runnable {
            override fun run() {
                view.text = fmt.format(Date())
                handler.postDelayed(this, 30_000L)
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }
}
