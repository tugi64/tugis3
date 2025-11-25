plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // Firebase: google-services.json eklendikten sonra etkinleştirin
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.example.tugis3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tugis3"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Hilt test runner
        testInstrumentationRunner = "com.example.tugis3.HiltTestRunner"

        // MultiDex desteği - büyük APK'lar için
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }

        // NDK abiFilters KALDIRILDI - Splits ile çakışıyor
        // Split APK yapılandırması aşağıda tanımlı
    }

    buildTypes {
        release {
            isMinifyEnabled = true // R8 shrink aktif
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug build - HIZLI YÜKLEME MOD (Galaxy A21s için)
            // Minify/Shrink KAPALI - derleme hızlı ama APK büyük (~80-100 MB)
            isMinifyEnabled = false
            isShrinkResources = false

            // MultiDex optimizasyonu
            multiDexEnabled = true

            // Dex'i küçült (legacy multidex kullan)
            multiDexKeepProguard = file("multidex-config.pro")

            // Firebase etkinleştirildiğinde Crashlytics mapping ayarı buraya gelecek
        }
    }

    // APK Split - Galaxy A21s için HIZLI YÜKLEME modu
    // Universal APK = tek APK, tüm mimariler, hızlı yükleme
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a") // Sadece Galaxy A21s için
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    // NOT: installOptions deprecated - AGP 8.0+ ile kaldırıldı
    // APK yüklemesi için zaman aşımı artık IDE/adb ayarlarından yapılmalı
    // Android Studio -> Preferences -> Build -> APK Installation Timeout


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Gereksiz dosyaları çıkar - APK boyutunu küçült
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = false // Modern packaging kullan
        }
    }
    buildToolsVersion = "35.0.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    google()
    mavenCentral()
    // JitPack kaldırıldı: kullanılmıyor ve 401 hatasına neden oluyor
}

dependencies {
    // Core modules
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:cad"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime.saveable)

    // Maps
    implementation(libs.play.services.maps)
    implementation(libs.androidx.maps.compose)
    implementation(libs.maps.utils.ktx)

    // Hilt (KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room (KSP)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Location Services
    implementation(libs.play.services.location)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase: google-services.json eklendikten sonra etkinleştirin
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.analytics)
    // implementation(libs.firebase.crashlytics)

    // LeakCanary (yalnızca debug)
    debugImplementation(libs.leakcanary)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    // dxf-parser kaldırıldı: internal parser kullanılıyor ve JitPack 401 hatası veriyor
}

// KSP configuration
ksp {
    arg("dagger.fastInit", "enabled")
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}

// IDE uyumluluğu için testClasses task alias'ı ekle
tasks.register("testClasses") {
    group = "build"
    description = "Alias for IDE compatibility - compiles test classes"
    dependsOn("compileDebugUnitTestKotlin")
}
