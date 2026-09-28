# Mehedi IPTV Android App

এই project-টি Android Phone এবং Android TV-এর জন্য তৈরি করা হয়েছে।

## Included features

- Mehedi IPTV branded dark/blue UI
- শুধু **All Channels** main list
- M3U playlist URL থেকে channel list load
- Channel list **প্রতি ২ মিনিটে auto refresh/update**
- Channel logo UI-তে দেখানো হয়নি
- Media3/ExoPlayer দিয়ে HLS/HTTP stream playback
- Full-screen player
- Android TV remote DPAD Left/Right দিয়ে Previous/Next Channel
- Previous / Next button
- App open হলে logo/title → ticker → All Channels layout
- Full-screen ticker: channel play করার ২ মিনিট পরে প্রায় ১ মিনিট দেখা যাবে, তারপর আবার cycle হবে
- Footer:
  - `© Mehedi Internet`
  - `Online`
  - `Total Visitor`
- Online/Total Visitor API প্রতি ১ মিনিটে refresh
- Total visitor lifetime value API থেকে নেওয়া হয়; app নিজে reset করে না

## URLs

M3U:
https://raw.githubusercontent.com/mehedi6070/Mehedi-IPTV/refs/heads/main/channels.m3u

Online:
https://meheditv.site.je/api/online.php?i=1

Total:
https://meheditv.site.je/api/total.php

## Build

Android Studio-তে project folder open করে Gradle sync করুন এবং Run/Build APK করুন।

Package:
`site.mehhedi.iptv`

Target:
Android 6.0+ (API 23), Android TV friendly.

## Important

Stream availability, M3U channel URLs এবং server/API response-এর উপর playback নির্ভর করবে। এই source project-এ internet access permission দেওয়া আছে।
