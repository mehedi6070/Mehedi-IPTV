package com.mehedi.iptv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.abs

data class Channel(val number: Int, val name: String, val url: String)

class MainActivity : Activity() {
    private val m3uUrl = "https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u"
    private val onlineUrl = "https://meheditv.site.je/api/online.php"
    private val totalUrl = "https://meheditv.site.je/api/total.php"

    private lateinit var root: FrameLayout
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private val channels = mutableListOf<Channel>()
    private var current = 0
    private var listPanel: LinearLayout? = null
    private var optionsPanel: LinearLayout? = null
    private var titleChip: TextView? = null
    private var ticker: TextView? = null
    private var homePanel: LinearLayout? = null
    private var downX = 0f
    private var downY = 0f
    private val io = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        buildRoot()
        refreshChannels(showHome = true)
        handler.postDelayed(object : Runnable {
            override fun run() {
                refreshChannels(showHome = false)
                handler.postDelayed(this, 120_000)
            }
        }, 120_000)
    }

    private fun buildRoot() {
        root = FrameLayout(this).apply { setBackgroundColor(0xFF050A12.toInt()) }
        playerView = PlayerView(this).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        root.addView(playerView)
        root.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX=e.x; downY=e.y; true }
                MotionEvent.ACTION_UP -> { handleSwipe(e.x-downX, e.y-downY); true }
                else -> true
            }
        }
        setContentView(root)
    }

    private fun refreshChannels(showHome: Boolean) {
        io.execute {
            try {
                val text = URL(m3uUrl).readText()
                val parsed = parseM3u(text)
                runOnUiThread {
                    channels.clear(); channels.addAll(parsed)
                    if (channels.isNotEmpty()) {
                        current = current.coerceIn(0, channels.lastIndex)
                        if (showHome) showHome() else rebuildListIfVisible()
                    }
                }
            } catch (_: Exception) {}
        }
        updateCounters()
    }

    private fun parseM3u(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var pending: String? = null
        var n = 1
        text.lines().forEach { line ->
            val s=line.trim()
            if (s.startsWith("#EXTINF", true)) {
                pending = s.substringAfterLast(",").trim()
            } else if (s.isNotBlank() && !s.startsWith("#") && pending != null) {
                out.add(Channel(n++, pending!!, s))
                pending=null
            }
        }
        return out
    }

    private fun showHome() {
        stopPlayer()
        root.removeViews(1, root.childCount-1)
        homePanel = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(28,24,28,24)
            setBackgroundColor(0xFF07111F.toInt())
        }
        val logoRow=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val icon=ImageView(this).apply { setImageResource(com.mehedi.iptv.R.drawable.ic_tv) }
        logoRow.addView(icon, LinearLayout.LayoutParams(72,72))
        logoRow.addView(tv("Mehedi IPTV",28,true), LinearLayout.LayoutParams(-2,-2).apply{leftMargin=12})
        homePanel!!.addView(logoRow)
        ticker=tv("মেহেদী ইন্টারনেট সংযোগ নেওয়ার জন্য যোগাযোগ করুন ০১৬২৬৮৮৬০৭০    >>>",16,true)
        homePanel!!.addView(ticker, LinearLayout.LayoutParams(-1,48).apply{topMargin=10})
        val heading=tv("CHANNEL LIST",22,true)
        homePanel!!.addView(heading, LinearLayout.LayoutParams(-1,50).apply{topMargin=12})
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        homePanel!!.addView(ScrollView(this).apply{addView(list)}, LinearLayout.LayoutParams(-1,0,1f))
        channels.take(40).forEach { c -> list.addView(channelRow(c){ playChannel(c.number-1) }) }
        root.addView(homePanel)
        updateCounters()
    }

    private fun channelRow(c: Channel, click:()->Unit): View =
        TextView(this).apply {
            text=String.format("%02d   %s", c.number,c.name)
            textSize=17f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER_VERTICAL
            setPadding(18,0,18,0); isFocusable=true; isClickable=true
            background = if (c.number-1==current) getDrawable(R.drawable.bg_selected) else getDrawable(R.drawable.bg_panel)
            setOnClickListener { click() }
            layoutParams=LinearLayout.LayoutParams(-1,52).apply{bottomMargin=6}
        }

    private fun playChannel(index:Int) {
        if(index !in channels.indices) return
        current=index
        homePanel?.let { root.removeView(it) }; homePanel=null
        val exo=ExoPlayer.Builder(this).build()
        player=exo; playerView.player=exo
        exo.setMediaItem(MediaItem.fromUri(channels[index].url)); exo.prepare(); exo.play()
        showTitleChip()
    }

    private fun showTitleChip() {
        titleChip?.let{root.removeView(it)}
        titleChip=tv("${channels[current].name}  |  ${channels[current].number}",15,true).apply{
            setPadding(18,8,18,8); background=getDrawable(R.drawable.bg_chip)
            gravity=Gravity.CENTER
        }
        val lp=FrameLayout.LayoutParams(250,52,Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        lp.rightMargin=24
        root.addView(titleChip,lp)
        handler.postDelayed({ titleChip?.let{root.removeView(it); titleChip=null} },4000)
    }

    private fun showChannelList() {
        optionsPanel?.let{root.removeView(it);optionsPanel=null}
        listPanel?.let{root.removeView(it);listPanel=null;return}
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,18,18,18);background=getDrawable(R.drawable.bg_panel)}
        panel.addView(tv("ALL CHANNEL",20,true),LinearLayout.LayoutParams(-1,48))
        val scroll=ScrollView(this); val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        channels.forEach{c->list.addView(channelRow(c){ playChannel(c.number-1); root.removeView(panel);listPanel=null })}
        scroll.addView(list);panel.addView(scroll,LinearLayout.LayoutParams(390,-1))
        val lp=FrameLayout.LayoutParams(430,-1,Gravity.LEFT);lp.topMargin=18;lp.bottomMargin=18;lp.leftMargin=18
        root.addView(panel,lp);listPanel=panel
    }

    private fun showOptions() {
        listPanel?.let{root.removeView(it);listPanel=null}
        optionsPanel?.let{root.removeView(it);optionsPanel=null;return}
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,22,22,22);background=getDrawable(R.drawable.bg_panel)}
        panel.addView(tv("PLAYER OPTIONS",20,true))
        listOf("Subtitle / Audio","Original","Fill / Stretch","Zoom","16:9","2.35:1 Widescreen","Add to Favorites","EPG","Show playback controls").forEach{
            panel.addView(tv(it,16,false).apply{setPadding(8,18,8,18);isFocusable=true})
        }
        val lp=FrameLayout.LayoutParams(410,-1,Gravity.RIGHT);lp.topMargin=18;lp.bottomMargin=18;lp.rightMargin=18
        root.addView(panel,lp);optionsPanel=panel
    }

    private fun handleSwipe(dx:Float,dy:Float) {
        if (abs(dx) > abs(dy) && abs(dx)>80) {
            if (dx>0) showChannelList() else showOptions()
        } else if (abs(dy)>80) {
            if (dy<0) nextChannel() else previousChannel()
        }
    }

    private fun nextChannel(){ if(channels.isNotEmpty()) playChannel((current+1)%channels.size) }
    private fun previousChannel(){ if(channels.isNotEmpty()) playChannel((current-1+channels.size)%channels.size) }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if(event.action==KeyEvent.ACTION_DOWN) when(event.keyCode){
            KeyEvent.KEYCODE_DPAD_UP -> { nextChannel(); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { previousChannel(); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { showChannelList(); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { showOptions(); return true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (homePanel != null && channels.isNotEmpty()) playChannel(current)
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if(listPanel!=null){root.removeView(listPanel);listPanel=null;return true}
                if(optionsPanel!=null){root.removeView(optionsPanel);optionsPanel=null;return true}
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun rebuildListIfVisible(){ if(listPanel!=null){ root.removeView(listPanel);listPanel=null;showChannelList() } }

    private fun updateCounters() {
        io.execute {
            val online = fetchJsonInt(onlineUrl,"online")
            val total = fetchJsonInt(totalUrl,"total")
            runOnUiThread {
                if (homePanel != null) {
                    val counter = tv("🟢 Online Now: ${online ?: "-"}    🔴 Total Visitor: ${total ?: "-"}",14,false)
                    homePanel!!.addView(counter, 3, LinearLayout.LayoutParams(-1,44))
                }
            }
        }
    }

    private fun fetchJsonInt(url:String,key:String):Int? = try {
        val con=URL(url).openConnection() as HttpURLConnection
        con.connectTimeout=5000;con.readTimeout=5000
        JSONObject(con.inputStream.bufferedReader().readText()).optInt(key,-1).takeIf{it>=0}
    } catch(_:Exception){null}

    private fun tv(text:String,size:Float,bold:Boolean)=TextView(this).apply{
        this.text=text;textSize=size;setTextColor(0xFFFFFFFF.toInt())
        if(bold) setTypeface(typeface,android.graphics.Typeface.BOLD)
    }

    private fun stopPlayer(){player?.release();player=null;playerView.player=null}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();stopPlayer();super.onDestroy()}
}
