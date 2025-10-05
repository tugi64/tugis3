plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.tugis3.baselineprofile"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

// NOT: baselineProfile bloğu kaldırıldı (mergeIntoMain AGP 8.6.1'de desteklenmiyor)
// Güncel AGP sürümünde profil birleştirme varsayılan olarak etkindir

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

// Baseline profile üretimi komutu örneği:
// ./gradlew :baselineprofile:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
