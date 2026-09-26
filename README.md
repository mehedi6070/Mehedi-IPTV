# Mehedi IPTV

Android IPTV starter project configured for GitHub Actions.

## Included behavior
- App opens to the full channel list; no autoplay on launch.
- M3U playlist loads from the configured GitHub raw URL.
- Channel list refreshes silently every 2 minutes.
- Selecting a channel opens the full-screen player.
- Right-center overlay: `Channel Name | Number`.
- Full-screen player hides the ticker and visitor footer.
- Swipe left: Player Options; swipe right: Channel List.
- Swipe up: Next Channel; swipe down: Previous Channel.
- Footer on list screen: `©Mehedi Internet`, Online Now and Total Visitor.
- Visitor values are read from the supplied API endpoints.
- GitHub Actions builds the debug APK automatically.

## Build
Push to GitHub and run **Actions → Build Mehedi IPTV**. The APK is uploaded as an Actions artifact.
