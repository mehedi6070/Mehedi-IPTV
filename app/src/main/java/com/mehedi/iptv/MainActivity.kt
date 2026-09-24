package com.mehedi.iptv

import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.os.*
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlin.math.abs

class MainActivity:AppCompatActivity(){
 private lateinit var playerView:PlayerView; private lateinit var home:View; private lateinit var left:View; private lateinit var right:View
 private lateinit var speed:TextView; private lateinit var playerTicker:TextView; private lateinit var online:TextView; private lateinit var total:TextView; private lateinit var leftTitle:TextView
 private lateinit var homeList:RecyclerView; private lateinit var playerList:RecyclerView
 private lateinit var homeAdapter:ChannelAdapter; private lateinit var playerAdapter:ChannelAdapter
 private var channels=listOf<Channel>(); private var index=-1; private var exo:ExoPlayer?=null; private var meter:DefaultBandwidthMeter?=null; private var playing=false
 private var downX=0f; private var downY=0f; private val ui=Handler(Looper.getMainLooper()); private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main); private var playlistJob:Job?=null
 private val speedLoop=object:Runnable{override fun run(){if(playing){val b=meter?.bitrateEstimate?:0L;speed.text=String.format("%.2f Mbps",if(b>0)b/1_000_000.0 else 0.0);ui.postDelayed(this,1000)}}}
 private val visitorLoop=object:Runnable{override fun run(){heartbeat();ui.postDelayed(this,5000)}}
 override fun onCreate(b:Bundle?){super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(window,false);setContentView(R.layout.activity_main);bind();setupLists();setupTouch();setupOptions();startTicker();loadPlaylist();ui.post(visitorLoop);showHome()}
 private fun bind(){playerView=findViewById(R.id.playerView);home=findViewById(R.id.homeView);left=findViewById(R.id.leftPanel);right=findViewById(R.id.rightPanel);speed=findViewById(R.id.speedText);playerTicker=findViewById(R.id.playerTicker);online=findViewById(R.id.onlineText);total=findViewById(R.id.totalText);leftTitle=findViewById(R.id.leftTitle);homeList=findViewById(R.id.channelRecycler);playerList=findViewById(R.id.playerChannelRecycler)}
 private fun setupLists(){homeAdapter=ChannelAdapter(channels){play(it)};playerAdapter=ChannelAdapter(channels){play(it)};homeList.layoutManager=LinearLayoutManager(this);homeList.adapter=homeAdapter;playerList.layoutManager=LinearLayoutManager(this);playerList.adapter=playerAdapter}
 private fun setupTouch(){playerView.isFocusable=true;playerView.isFocusableInTouchMode=true;playerView.setOnTouchListener{_,e->when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;true};MotionEvent.ACTION_UP->{val dx=e.x-downX;val dy=e.y-downY;if(playing&&abs(dx)>70&&abs(dx)>abs(dy)){if(dx>0)showLeft() else showRight()}else if(playing&&abs(dx)<35&&abs(dy)<35){hidePanels()} ;true};else->true}};playerView.setOnKeyListener{_,k,e->if(e.action!=KeyEvent.ACTION_DOWN||!playing)return@setOnKeyListener false;when(k){KeyEvent.KEYCODE_DPAD_UP->{next();true};KeyEvent.KEYCODE_DPAD_DOWN->{prev();true};KeyEvent.KEYCODE_BACK->{showHome();true};KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->{if(exo?.isPlaying==true)exo?.pause()else exo?.play();true};else->false}}
 }
 private fun setupOptions(){findViewById<TextView>(R.id.favoriteOption).setOnClickListener{toggleFavorite()};findViewById<TextView>(R.id.playbackOption).setOnClickListener{showPlaybackDialog()}}
 private fun startTicker(){findViewById<TextView>(R.id.homeTicker).isSelected=true;playerTicker.isSelected=true}
 private fun loadPlaylist(){playlistJob=scope.launch{refreshPlaylist();while(isActive){delay(120000);refreshPlaylist()}}}
 private suspend fun refreshPlaylist(){val txt=withContext(Dispatchers.IO){runCatching{URL(getString(R.string.playlist_url)).openStream().bufferedReader().use{it.readText()}}.getOrNull()};if(!txt.isNullOrBlank()){val list=M3uParser.parse(txt);if(list.isNotEmpty())withContext(Dispatchers.Main){channels=list;homeAdapter.submit(list);playerAdapter.submit(list);leftTitle.text="‹   Total Channels: ${list.size}"
; findViewById<TextView>(R.id.ourChannels).text="‹   Total Channels : ${list.size}"}}}
 private fun ensurePlayer(){if(exo!=null)return;meter=DefaultBandwidthMeter.Builder(this).build();exo=ExoPlayer.Builder(this).setBandwidthMeter(meter!!).build();playerView.player=exo;playerView.useController=false}
 private fun play(c:Channel){index=channels.indexOfFirst{it.number==c.number};if(index<0)return;ensurePlayer();val media=MediaItem.fromUri(c.url);exo?.setMediaItem(media);exo?.prepare();exo?.play();playing=true;home.visibility=View.GONE;playerView.visibility=View.VISIBLE;playerTicker.visibility=View.VISIBLE;speed.visibility=View.VISIBLE;hidePanels();requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;immersive(true);playerAdapter.setSelected(c.number);ui.removeCallbacks(speedLoop);ui.post(speedLoop);playerView.requestFocus()}
 private fun next(){if(channels.isNotEmpty())play(channels[(index+1)%channels.size])}
 private fun prev(){if(channels.isNotEmpty())play(channels[(index-1+channels.size)%channels.size])}
 private fun showLeft(){left.visibility=View.VISIBLE;right.visibility=View.GONE;if(index>=0){playerList.scrollToPosition(index);playerAdapter.setSelected(channels[index].number)}}
 private fun showRight(){right.visibility=View.VISIBLE;left.visibility=View.GONE}
 private fun hidePanels(){left.visibility=View.GONE;right.visibility=View.GONE}
 private fun showHome(){playing=false;exo?.pause();hidePanels();speed.visibility=View.GONE;playerTicker.visibility=View.GONE;playerView.visibility=View.GONE;home.visibility=View.VISIBLE;requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;immersive(false);homeList.requestFocus()}
 private fun toggleFavorite(){if(index<0)return;val p=getSharedPreferences("favorites",0);val id=channels[index].number.toString();val on=p.getBoolean(id,false);p.edit().putBoolean(id,!on).apply();findViewById<TextView>(R.id.favoriteOption).text=if(on)"★  Add to Favorites" else "★  Added to Favorites"}
 private fun showPlaybackDialog(){val items=arrayOf("Play / Pause","Mute / Unmute");AlertDialog.Builder(this).setTitle("Playback Controls").setItems(items){_,which->if(which==0){if(exo?.isPlaying==true)exo?.pause()else exo?.play()}else{exo?.volume=if((exo?.volume?:1f)>0f)0f else 1f}}.show()}
 private fun heartbeat(){scope.launch(Dispatchers.IO){val sp=getSharedPreferences("mehedi",0);var id=sp.getString("visitor_id",null);if(id==null){id="visitor_"+UUID.randomUUID();sp.edit().putString("visitor_id",id).apply()};runCatching{val base=getString(R.string.visitor_api_base).trimEnd('/');val conn=URL("$base/api/heartbeat?id="+URLEncoder.encode(id,"UTF-8")).openConnection() as HttpURLConnection;conn.connectTimeout=5000;conn.readTimeout=5000;val body=conn.inputStream.bufferedReader().use{it.readText()};conn.disconnect();val obj=org.json.JSONObject(body);withContext(Dispatchers.Main){online.text="● Online Now: ${obj.optInt("online",0)}";total.text="♟ Total Visitors: ${obj.optInt("total",0)}"}}}}
 private fun immersive(on:Boolean){val c=window.insetsController?:return;if(on){c.hide(WindowInsets.Type.systemBars());c.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}else c.show(WindowInsets.Type.systemBars())}
 override fun onBackPressed(){if(playing)showHome()else super.onBackPressed()}
 override fun onDestroy(){ui.removeCallbacks(visitorLoop);ui.removeCallbacks(speedLoop);playlistJob?.cancel();scope.cancel();exo?.release();exo=null;super.onDestroy()}
}
