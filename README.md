# Mehedi IPTV

Final Android TV / phone IPTV project based on the approved Mehedi IPTV reference design.

## Final UI
- Black + light-red theme.
- App opens to channel list; no autoplay.
- Small Online Now + Total Visitors below the ticker on Home only.
- Full-screen player after selecting a channel.
- Stream's own logo can remain; the app does not overlay a Mehedi IPTV logo on video.
- Small real-time Mbps estimate at top-left while playing.
- Left -> Right swipe: channel list.
- Right -> Left swipe: Video / Format, Subtitle / Audio, Add to Favorites, Playback Controls.
- Remote UP: next channel.
- Remote DOWN: previous channel.
- BACK: return to channel list.
- No Search, Category, EPG, Programs, Google Cast or persistent progress bar.

## Playlist
The app reads:
`https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u`

It refreshes the playlist silently every 2 minutes. The 2-minute interval is not shown in the UI.

## Visitor API
The app sends a heartbeat every 5 seconds to:
`https://mehedi-online.workers.dev`

The API should provide `/api/heartbeat?id=...` and return JSON containing `online` and `total`.

If your Worker URL changes, edit `app/src/main/res/values/strings.xml`.

## GitHub
Upload the contents of this folder to the repository root, then push to the `main` branch.
The included GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## Android Studio
Open the project folder in Android Studio, let Gradle sync, then use:
Build > Build APK(s)

The GitHub workflow can build without a checked-in Gradle wrapper because it installs Gradle 8.11.1 directly.
