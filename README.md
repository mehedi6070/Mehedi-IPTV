# Mehedi IPTV

Android TV focused IPTV player project.

## Included
- Mehedi IPTV branding/logo
- M3U playlist with category support
- Automatic playlist refresh + cached playlist
- Android TV remote navigation
- Play/Pause, Previous/Next
- System Volume Up/Down/Mute handling
- Full-screen player
- **Live network Speed indicator (Mbps only)**
- No channel logos
- No Used MB / total data counter

## Playlist URL
Edit `M3U_URL` in `app/src/main/java/com/mehedi/iptv/MainActivity.java` and replace the placeholder with your online `channels.m3u` URL.


## Playlist URL
The app uses this GitHub M3U URL and refreshes it automatically:
`https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u`

If the network is unavailable, the last successfully downloaded playlist is used from cache.
