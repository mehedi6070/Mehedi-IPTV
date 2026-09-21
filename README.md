# Mehedi IPTV

Android TV / Phone IPTV app using the online M3U playlist.

## Final behavior
- No autoplay when the app opens.
- Select a channel to start playback.
- M3U playlist refreshes every 2 minutes.
- Playlist is cached for offline fallback.
- Channel list shows names only; no channel logos, search, categories, or channel numbers.
- TV remote: UP = Next Channel, DOWN = Previous Channel, OK = select/play, BACK = channel list.
- TV hardware volume and mute keys are supported.
- Phone: selecting a channel switches to landscape fullscreen. Right-side swipe up/down changes channel; left-side swipe up/down changes volume.
- Live speed is shown while playing.
- Ticker text moves left-to-right: মেহেদী ইন্টারনেট সংযোগ নেওয়ার জন্য যোগাযোগ করুন ০১৬২৬৮৮৬০৭০

## Playlist
https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u

## Build
Use GitHub Actions workflow `.github/workflows/build-apk.yml`, or Gradle 8.7 + Java 17 locally.
