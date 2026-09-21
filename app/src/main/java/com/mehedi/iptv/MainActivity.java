package com.mehedi.iptv;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.media.AudioManager;import android.view.*;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import androidx.recyclerview.widget.*;import androidx.media3.common.MediaItem;import androidx.media3.exoplayer.ExoPlayer;import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;import androidx.media3.ui.PlayerView;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private static final String M3U_URL="https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u";
    private static final long REFRESH_MS=2*60*1000L;
    RecyclerView chans; TextView status,speedText,ticker; List<Channel> all=new ArrayList<>(),shown=new ArrayList<>();
    ExoPlayer player; PlayerView playerView; DefaultBandwidthMeter bandwidthMeter; int current=-1;
    Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);showList();loadCached();refreshPlaylist();handler.postDelayed(new Runnable(){public void run(){refreshPlaylist();handler.postDelayed(this,REFRESH_MS);}},REFRESH_MS);}

    void showList(){
        handler.removeCallbacks(speedUpdater); if(player!=null){player.release();player=null;} speedText=null;
        setContentView(R.layout.activity_main); status=findViewById(R.id.status);chans=findViewById(R.id.channels);ticker=findViewById(R.id.ticker);startTicker();
        int span=getResources().getConfiguration().screenWidthDp>=900?5:(getResources().getConfiguration().screenWidthDp>=600?4:2);
        chans.setLayoutManager(new GridLayoutManager(this,span));chans.setAdapter(new ChannelAdapter());
        if(!shown.isEmpty())chans.requestFocus();
    }

    void startTicker(){if(ticker==null)return;ticker.post(()->{View c=findViewById(R.id.tickerContainer);if(c==null)return;ticker.clearAnimation();float from=-ticker.getWidth()-40;float to=c.getWidth()+40;android.view.animation.TranslateAnimation a=new android.view.animation.TranslateAnimation(from,to,0,0);a.setDuration(11000);a.setRepeatCount(android.view.animation.Animation.INFINITE);a.setRepeatMode(android.view.animation.Animation.RESTART);ticker.startAnimation(a);});}
    void loadCached(){String s=getPreferences(0).getString("m3u","");if(!s.isEmpty())parse(s);}
    void refreshPlaylist(){if(status!=null)status.setText("Updating channels...");ExecutorService ex=Executors.newSingleThreadExecutor();ex.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(M3U_URL).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Mehedi IPTV/1.0");BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder sb=new StringBuilder();String line;while((line=r.readLine())!=null)sb.append(line).append('\n');String data=sb.toString();getPreferences(0).edit().putString("m3u",data).apply();runOnUiThread(()->{parse(data);if(status!=null)status.setText(all.size()+" channels");});}catch(Exception e){runOnUiThread(()->{if(status!=null)status.setText(all.size()>0?all.size()+" channels (cached)":"Playlist unavailable");});}finally{ex.shutdown();}});}
    void parse(String data){all.clear();String name=null;for(String raw:data.split("\\r?\\n")){String l=raw.trim();if(l.startsWith("#EXTINF")){name=l.substring(l.lastIndexOf(',')+1).trim();}else if(!l.startsWith("#")&&!l.isEmpty()&&name!=null){all.add(new Channel(name,l));name=null;}}shown.clear();shown.addAll(all);if(chans!=null&&chans.getAdapter()!=null)chans.getAdapter().notifyDataSetChanged();}

    void play(int index){if(index<0||index>=shown.size())return;current=index;Channel c=shown.get(index);if(player!=null)player.release();bandwidthMeter=new DefaultBandwidthMeter.Builder(this).build();player=new ExoPlayer.Builder(this).setBandwidthMeter(bandwidthMeter).build();
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        playerView=new PlayerView(this);playerView.setPlayer(player);playerView.setUseController(true);playerView.setBackgroundColor(Color.BLACK);playerView.setFocusable(true);playerView.setFocusableInTouchMode(true);
        root.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        speedText=new TextView(this);speedText.setText("Speed: -- Mbps");speedText.setTextColor(Color.WHITE);speedText.setTextSize(13);speedText.setGravity(Gravity.CENTER);speedText.setPadding(14,6,14,6);GradientDrawable bg=new GradientDrawable();bg.setColor(0xB0000000);bg.setCornerRadius(18);speedText.setBackground(bg);FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);sp.setMargins(0,18,18,0);root.addView(speedText,sp);
        // Phone touch zones: left side volume, right side channel change.
        addTouchZone(root,true);addTouchZone(root,false);
        setContentView(root);root.post(()->playerView.requestFocus());handler.post(speedUpdater);player.setMediaItem(MediaItem.fromUri(c.url));player.prepare();player.play();
    }

    void addTouchZone(FrameLayout root,boolean left){View zone=new View(this);zone.setBackgroundColor(Color.TRANSPARENT);zone.setFocusable(false);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(0,-1,left?Gravity.START:Gravity.END);p.width=getResources().getDisplayMetrics().widthPixels/3;root.addView(zone,p);zone.setOnTouchListener(new View.OnTouchListener(){float sy;long st;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){sy=e.getY();st=System.currentTimeMillis();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dy=e.getY()-sy;if(Math.abs(dy)>45){if(left){if(dy<0)volume(true);else volume(false);}else{if(dy<0)next();else prev();}}else if(System.currentTimeMillis()-st>500&&left){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,AudioManager.FLAG_SHOW_UI);}return true;}return true;}});}
    void volume(boolean up){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);a.adjustStreamVolume(AudioManager.STREAM_MUSIC,up?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);}
    Runnable speedUpdater=new Runnable(){public void run(){if(speedText!=null&&player!=null){long b=bandwidthMeter!=null?bandwidthMeter.getBitrateEstimate():-1;speedText.setText(b>0?"Speed: "+String.format(Locale.US,"%.2f",b/1000000.0)+" Mbps":"Speed: -- Mbps");}handler.postDelayed(this,1000);}};
    void next(){if(shown.size()==0)return;play((current+1)%shown.size());}
    void prev(){if(shown.size()==0)return;play((current-1+shown.size())%shown.size());}

    @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_DPAD_UP&&player!=null){next();return true;}if(k==KeyEvent.KEYCODE_DPAD_DOWN&&player!=null){prev();return true;}if(k==KeyEvent.KEYCODE_MEDIA_NEXT&&player!=null){next();return true;}if(k==KeyEvent.KEYCODE_MEDIA_PREVIOUS&&player!=null){prev();return true;}if(k==KeyEvent.KEYCODE_VOLUME_UP){volume(true);return true;}if(k==KeyEvent.KEYCODE_VOLUME_DOWN){volume(false);return true;}if(k==KeyEvent.KEYCODE_VOLUME_MUTE){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,AudioManager.FLAG_SHOW_UI);return true;}if((k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||k==KeyEvent.KEYCODE_SPACE)&&player!=null){if(player.isPlaying())player.pause();else player.play();return true;}if(k==KeyEvent.KEYCODE_BACK&&player!=null){showList();return true;}}return super.dispatchKeyEvent(e);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null)player.release();super.onDestroy();}
    static class Channel{String name,url;Channel(String n,String u){name=n;url=u;}}
    class ChannelAdapter extends RecyclerView.Adapter<VH>{public int getItemCount(){return shown.size();}public VH onCreateViewHolder(ViewGroup p,int v){return new VH(getLayoutInflater().inflate(R.layout.item_channel,p,false));}public void onBindViewHolder(VH h,int i){h.t.setText(shown.get(i).name);h.t.setOnClickListener(v->play(i));h.t.setOnFocusChangeListener((v,f)->{GradientDrawable d=new GradientDrawable();d.setCornerRadius(14);d.setColor(f?0xFF260000:0xFF111111);d.setStroke(f?3:1,f?0xFFFF2020:0xFF333333);h.t.setBackground(d);});}}
    static class VH extends RecyclerView.ViewHolder{TextView t;VH(View v){super(v);t=(TextView)v;}}
}
