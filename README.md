# Mehedi IPTV

Android IPTV app source project.

## Build

Requirements:
- JDK 17
- Android SDK with API 35
- Gradle 8.9+ or a GitHub Actions runner with Gradle available

The project uses:
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Jetpack Compose
- Media3 ExoPlayer 1.5.1

## GitHub Actions

The included workflow builds a debug APK with JDK 17 and uploads it as an artifact.

## Important

The app loads the playlist and visitor counters from the URLs configured in `MainActivity.kt`.
