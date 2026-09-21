plugins { id("com.android.application") }

android { namespace = "com.mehedi.iptv"; compileSdk = 35
    defaultConfig { applicationId = "com.mehedi.iptv"; minSdk = 23; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
