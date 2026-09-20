package com.mehedi.iptv;

import android.animation.ObjectAnimator;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private static final String M3U_URL = "https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u";
    private static final long REFRESH_MS = 2 * 60 * 1000L;
    private static final String[] CATEGORY_ORDER = {"All Channels","Bangla","Sports","News","Movie","Music","Kids","Islamic","Entertainment","Other"};

    RecyclerView cats, chans;
    TextView status, speedText, ticker;
    FrameLayout tickerBox;
    List<Channel> all = new ArrayList<>(), shown = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    String selectedGroup = "All Channels";
    ExoPlayer player;
    PlayerView playerView;
    DefaultBandwidthMeter bandwidthMeter;
    int current = -1;
    Handler handler = new Handler(Looper.getMainLooper());
    ExecutorService executor = Executors.newSingleThreadExecutor();
    ObjectAnimator tickerAnimator;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        bindViews();
        setupLists();
        startTicker();
        loadCached();
        refreshPlaylist();
        handler.postDelayed(refreshRunnable, REFRESH_MS);
    }

    void bindViews() {
        status = findViewById(R.id.status);
        cats = findViewById(R.id.categories);
        chans = findViewById(R.id.channels);
        ticker = findViewById(R.id.ticker);
        tickerBox = findViewById(R.id.tickerBox);
    }

    void setupLists() {
        cats.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        chans.setLayoutManager(new GridLayoutManager(this, 5));
        cats.setAdapter(new CatAdapter());
        chans.setAdapter(new ChannelAdapter());
        cats.setOnFocusChangeListener((v, has) -> { if (has && cats.getChildCount() > 0) cats.getChildAt(0).requestFocus(); });
        chans.setOnFocusChangeListener((v, has) -> { if (has && chans.getChildCount() > 0) chans.getChildAt(0).requestFocus(); });
    }

    void startTicker() {
        ticker.post(() -> {
            ticker.setTranslationX(tickerBox.getWidth());
            float distance = tickerBox.getWidth() + ticker.getWidth() + 80f;
            long duration = Math.max(7000L, (long)(distance * 9));
            tickerAnimator = ObjectAnimator.ofFloat(ticker, View.TRANSLATION_X, tickerBox.getWidth(), -ticker.getWidth());
            tickerAnimator.setDuration(duration);
            tickerAnimator.setInterpolator(new LinearInterpolator());
            tickerAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            tickerAnimator.start();
        });
    }

    void loadCached() {
        String s = getPreferences(0).getString("m3u", "");
        if (!s.isEmpty()) parse(s);
    }

    void refreshPlaylist() {
        status.setText(all.size() > 0 ? all.size() + " channels • updating" : "Updating...");
        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(M3U_URL).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(25000);
                c.setRequestProperty("User-Agent", "Mehedi IPTV/1.0");
                c.connect();
                if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new IOException("HTTP " + c.getResponseCode());
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                r.close();
                String data = sb.toString();
                if (!data.contains("#EXTINF")) throw new IOException("Invalid M3U");
                getPreferences(0).edit().putString("m3u", data).apply();
                runOnUiThread(() -> { parse(data); status.setText(all.size() + " channels • updated"); });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText(all.size() > 0 ? all.size() + " channels • cached" : "Playlist unavailable"));
            } finally { if (c != null) c.disconnect(); }
        });
    }

    final Runnable refreshRunnable = new Runnable() {
        @Override public void run() { refreshPlaylist(); handler.postDelayed(this, REFRESH_MS); }
    };

    void parse(String data) {
        all.clear();
        groups.clear();
        groups.addAll(Arrays.asList(CATEGORY_ORDER));
        String name = null, explicitGroup = "";
        for (String raw : data.split("\\r?\\n")) {
            String l = raw.trim();
            if (l.startsWith("#EXTINF")) {
                int comma = l.lastIndexOf(',');
                name = comma >= 0 ? l.substring(comma + 1).trim() : "Unknown Channel";
                explicitGroup = attr(l, "group-title");
                if (explicitGroup.isEmpty()) explicitGroup = attr(l, "tvg-group");
            } else if (!l.startsWith("#") && !l.isEmpty() && name != null) {
                String group = explicitGroup.isEmpty() ? inferCategory(name) : normalizeGroup(explicitGroup);
                all.add(new Channel(name, l, group));
                name = null;
            }
        }
        filterChannels();
        cats.getAdapter().notifyDataSetChanged();
        chans.getAdapter().notifyDataSetChanged();
        if (chans.getChildCount() > 0) chans.getChildAt(0).requestFocus();
    }

    String normalizeGroup(String g) {
        String x = g.toLowerCase(Locale.US);
        if (x.contains("sport")) return "Sports";
        if (x.contains("news")) return "News";
        if (x.contains("movie") || x.contains("cinema")) return "Movie";
        if (x.contains("music")) return "Music";
        if (x.contains("kid")) return "Kids";
        if (x.contains("islam")) return "Islamic";
        if (x.contains("bangla")) return "Bangla";
        return "Entertainment";
    }

    String inferCategory(String name) {
        String x = name.toLowerCase(Locale.US);
        if (containsAny(x, "sports", "t sport", "star sports", "ten sports")) return "Sports";
        if (containsAny(x, "news", "dbc", "somoy", "ekattor", "jamuna", "channel 24", "ntv", "news 21")) return "News";
        if (containsAny(x, "movie", "cinema", "sony max", "sony pix", "colors hd", "goldmines", "bollywood")) return "Movie";
        if (containsAny(x, "music", "sangeet", "9xm", "zoom", "ptc", "yrf", "b4u music")) return "Music";
        if (containsAny(x, "kid", "jungle book", "gopal bhar")) return "Kids";
        if (containsAny(x, "peace tv", "islam", "quran")) return "Islamic";
        if (containsAny(x, "bangla", "satv", "gazi", "atn", "rtv", "boishakhi", "bijoy", "maasranga", "ananda", "ekushey", "my tv", "deepto", "rajdhani", "global-tv", "enter tv", "channel-s")) return "Bangla";
        return "Entertainment";
    }

    boolean containsAny(String s, String... terms) { for (String t : terms) if (s.contains(t)) return true; return false; }

    void filterChannels() {
        shown.clear();
        for (Channel c : all) if (selectedGroup.equals("All Channels") || c.group.equals(selectedGroup)) shown.add(c);
        current = -1;
    }

    String attr(String s, String key) {
        String q = key + "=\"";
        int i = s.indexOf(q);
        if (i < 0) return "";
        int a = i + q.length(), b = s.indexOf('"', a);
        return b > 0 ? s.substring(a, b) : "";
    }

    void play(int index) {
        if (index < 0 || index >= shown.size()) return;
        current = index;
        Channel c = shown.get(index);
        if (player != null) player.release();
        bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();
        player = new ExoPlayer.Builder(this).setBandwidthMeter(bandwidthMeter).build();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        speedText = new TextView(this);
        speedText.setText("Speed: -- Mbps");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(13);
        speedText.setGravity(Gravity.CENTER);
        speedText.setPadding(14, 6, 14, 6);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xB0000000);
        bg.setCornerRadius(18);
        speedText.setBackground(bg);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        sp.setMargins(0, 18, 18, 0);
        root.addView(speedText, sp);
        setContentView(root);
        handler.post(speedUpdater);
        player.setMediaItem(MediaItem.fromUri(c.url));
        player.prepare();
        player.play();
    }

    final Runnable speedUpdater = new Runnable() {
        @Override public void run() {
            if (speedText != null && player != null) {
                long b = bandwidthMeter != null ? bandwidthMeter.getBitrateEstimate() : -1;
                speedText.setText(b > 0 ? "Speed: " + formatMbps(b) + " Mbps" : "Speed: -- Mbps");
                handler.postDelayed(this, 1000);
            }
        }
    };

    String formatMbps(long bits) { return String.format(Locale.US, "%.2f", bits / 1000000.0); }

    void showList() {
        handler.removeCallbacks(speedUpdater);
        if (player != null) { player.release(); player = null; }
        speedText = null;
        setContentView(R.layout.activity_main);
        bindViews();
        setupLists();
        startTicker();
        cats.getAdapter().notifyDataSetChanged();
        chans.getAdapter().notifyDataSetChanged();
        chans.requestFocus();
    }

    void next() { if (!shown.isEmpty()) play((current + 1 + shown.size()) % shown.size()); }
    void prev() { if (!shown.isEmpty()) play((current - 1 + shown.size()) % shown.size()); }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            int k = e.getKeyCode();
            if (k == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || k == KeyEvent.KEYCODE_SPACE) {
                if (player != null) { if (player.isPlaying()) player.pause(); else player.play(); }
                return true;
            }
            if (k == KeyEvent.KEYCODE_MEDIA_NEXT) { next(); return true; }
            if (k == KeyEvent.KEYCODE_MEDIA_PREVIOUS) { prev(); return true; }
            if (k == KeyEvent.KEYCODE_VOLUME_UP) { ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); return true; }
            if (k == KeyEvent.KEYCODE_VOLUME_DOWN) { ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); return true; }
            if (k == KeyEvent.KEYCODE_VOLUME_MUTE) { ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI); return true; }
            if (k == KeyEvent.KEYCODE_BACK && player != null) { showList(); return true; }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tickerAnimator != null) tickerAnimator.cancel();
        executor.shutdownNow();
        if (player != null) player.release();
        super.onDestroy();
    }

    static class Channel { String name, url, group; Channel(String n, String u, String g) { name=n; url=u; group=g; } }

    class CatAdapter extends RecyclerView.Adapter<VH> {
        public int getItemCount() { return groups.size(); }
        public VH onCreateViewHolder(ViewGroup p, int v) { return new VH(getLayoutInflater().inflate(R.layout.item_category, p, false)); }
        public void onBindViewHolder(VH h, int i) {
            h.t.setText(groups.get(i));
            h.t.setOnClickListener(v -> { selectedGroup = groups.get(i); filterChannels(); chans.getAdapter().notifyDataSetChanged(); if (chans.getChildCount() > 0) chans.getChildAt(0).requestFocus(); });
        }
    }

    class ChannelAdapter extends RecyclerView.Adapter<VH> {
        public int getItemCount() { return shown.size(); }
        public VH onCreateViewHolder(ViewGroup p, int v) { return new VH(getLayoutInflater().inflate(R.layout.item_channel, p, false)); }
        public void onBindViewHolder(VH h, int i) { h.t.setText(shown.get(i).name); h.t.setOnClickListener(v -> play(h.getBindingAdapterPosition())); }
    }

    static class VH extends RecyclerView.ViewHolder { TextView t; VH(View v) { super(v); t=(TextView)v; } }
}
