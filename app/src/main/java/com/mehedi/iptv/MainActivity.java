package com.mehedi.iptv;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.media.AudioManager;import android.view.*;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import androidx.recyclerview.widget.*;import androidx.media3.common.*;import androidx.media3.common.util.Util;import androidx.media3.exoplayer.ExoPlayer;import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;import androidx.media3.ui.PlayerView;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    // Replace this with your live M3U URL. The app also caches the last successful playlist.
    private static final String M3U_URL = "https://YOUR-DOMAIN.example/channels.m3u";
    private static final long REFRESH_MS = 6 * 60 * 60 * 1000L;
    RecyclerView cats, chans; TextView status, speedText; List<Channel> all=new ArrayList<>(), shown=new ArrayList<>(); List<String> groups=new ArrayList<>(); String selectedGroup="All"; ExoPlayer player; PlayerView playerView; DefaultBandwidthMeter bandwidthMeter; int current=-1; Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); setContentView(R.layout.activity_main);
        status=findViewById(R.id.status); cats=findViewById(R.id.categories); chans=findViewById(R.id.channels); setupLists(); loadCached(); refreshPlaylist(); handler.postDelayed(new Runnable(){public void run(){refreshPlaylist();handler.postDelayed(this,REFRESH_MS);}},REFRESH_MS); }

    void setupLists(){ cats.setLayoutManager(new LinearLayoutManager(this)); chans.setLayoutManager(new LinearLayoutManager(this)); cats.setAdapter(new CatAdapter()); chans.setAdapter(new ChannelAdapter()); }
    void loadCached(){String s=getPreferences(0).getString("m3u",""); if(!s.isEmpty()) parse(s); }
    void refreshPlaylist(){ status.setText("Updating channels..."); ExecutorService ex=Executors.newSingleThreadExecutor(); ex.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(M3U_URL).openConnection(); c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Mehedi IPTV/1.0"); InputStream in=c.getInputStream(); BufferedReader r=new BufferedReader(new InputStreamReader(in));StringBuilder sb=new StringBuilder();String line;while((line=r.readLine())!=null)sb.append(line).append('\n');String data=sb.toString();getPreferences(0).edit().putString("m3u",data).apply();runOnUiThread(()->{parse(data);status.setText(all.size()+" channels");});}catch(Exception e){runOnUiThread(()->status.setText(all.size()>0?all.size()+" channels (cached)":"Playlist unavailable"));}finally{ex.shutdown();}}); }
    void parse(String data){all.clear();groups.clear();groups.add("All");String name=null,group="General";for(String raw:data.split("\\r?\\n")){String l=raw.trim();if(l.startsWith("#EXTINF")){name=l.substring(l.lastIndexOf(',')+1).trim();String g=attr(l,"group-title");if(g.isEmpty())g=attr(l,"tvg-group");group=g.isEmpty()?"General":g;}else if(!l.startsWith("#")&&!l.isEmpty()&&name!=null){all.add(new Channel(name,l,group));if(!groups.contains(group))groups.add(group);name=null;}} shown.clear();shown.addAll(all);cats.getAdapter().notifyDataSetChanged();chans.getAdapter().notifyDataSetChanged();}
    String attr(String s,String key){String q=key+"=\"";int i=s.indexOf(q);if(i<0)return "";int a=i+q.length(),b=s.indexOf('"',a);return b>0?s.substring(a,b):"";}
    void play(int index){
        if(index<0||index>=shown.size())return;
        current=index; Channel c=shown.get(index);
        if(player!=null)player.release();
        bandwidthMeter=new DefaultBandwidthMeter.Builder(this).build();
        player=new ExoPlayer.Builder(this).setBandwidthMeter(bandwidthMeter).build();
        FrameLayout root=new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        playerView=new PlayerView(this); playerView.setPlayer(player); playerView.setUseController(true); playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        speedText=new TextView(this); speedText.setText("Speed: -- Mbps"); speedText.setTextColor(Color.WHITE); speedText.setTextSize(13); speedText.setGravity(Gravity.CENTER); speedText.setPadding(14,6,14,6);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xB0000000); bg.setCornerRadius(18); speedText.setBackground(bg);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END); sp.setMargins(0,18,18,0); root.addView(speedText,sp);
        setContentView(root);
        handler.post(speedUpdater);
        player.setMediaItem(MediaItem.fromUri(c.url)); player.prepare(); player.play();
    }
    Runnable speedUpdater=new Runnable(){public void run(){
        if(speedText!=null && player!=null){ long b=bandwidthMeter!=null?bandwidthMeter.getBitrateEstimate():-1; speedText.setText(b>0?"Speed: "+formatMbps(b)+" Mbps":"Speed: -- Mbps"); }
        handler.postDelayed(this,1000);
    }};
    String formatMbps(long bits){return String.format(Locale.US,"%.2f",bits/1000000.0);}

    void showList(){handler.removeCallbacks(speedUpdater);if(player!=null){player.release();player=null;}speedText=null;setContentView(R.layout.activity_main);status=findViewById(R.id.status);cats=findViewById(R.id.categories);chans=findViewById(R.id.channels);setupLists();chans.requestFocus();}
    void next(){if(shown.size()==0)return;play((current+1)%shown.size());} void prev(){if(shown.size()==0)return;play((current-1+shown.size())%shown.size());}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN){int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||k==KeyEvent.KEYCODE_SPACE){if(player!=null){if(player.isPlaying())player.pause();else player.play();}return true;}if(k==KeyEvent.KEYCODE_MEDIA_NEXT){next();return true;}if(k==KeyEvent.KEYCODE_MEDIA_PREVIOUS){prev();return true;}if(k==KeyEvent.KEYCODE_VOLUME_UP){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);return true;}if(k==KeyEvent.KEYCODE_VOLUME_DOWN){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);return true;}if(k==KeyEvent.KEYCODE_VOLUME_MUTE){((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,AudioManager.FLAG_SHOW_UI);return true;}if(k==KeyEvent.KEYCODE_BACK&&player!=null){showList();return true;}}return super.dispatchKeyEvent(e);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null)player.release();super.onDestroy();}

    static class Channel{String name,url,group;Channel(String n,String u,String g){name=n;url=u;group=g;}}
    class CatAdapter extends RecyclerView.Adapter<VH>{public int getItemCount(){return groups.size();}public VH onCreateViewHolder(android.view.ViewGroup p,int v){return new VH(getLayoutInflater().inflate(R.layout.item_category,p,false));}public void onBindViewHolder(VH h,int i){h.t.setText(groups.get(i));h.t.setOnClickListener(v->{selectedGroup=groups.get(i);shown.clear();for(Channel c:all)if(selectedGroup.equals("All")||c.group.equals(selectedGroup))shown.add(c);chans.getAdapter().notifyDataSetChanged();if(shown.size()>0)chans.requestFocus();});}}
    class ChannelAdapter extends RecyclerView.Adapter<VH>{public int getItemCount(){return shown.size();}public VH onCreateViewHolder(android.view.ViewGroup p,int v){return new VH(getLayoutInflater().inflate(R.layout.item_channel,p,false));}public void onBindViewHolder(VH h,int i){h.t.setText(shown.get(i).name);h.t.setOnClickListener(v->play(i));}}
    static class VH extends RecyclerView.ViewHolder{TextView t;VH(View v){super(v);t=(TextView)v;}}
}
