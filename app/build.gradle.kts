plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // Firebase pluginleri koşullu uygulanacak, burada kaldırıldı.
}

// Firebase pluginlerini yalnızca google-services.json varsa uygula
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else {
    logger.lifecycle("[INFO] google-services.json bulunamadı -> Firebase (Crashlytics & Analytics plugin) atlandı. Sadece bağımlılıklar yüklü fakat plugin task'ları oluşmayacak.")
}

// İsteğe bağlı: Debug amaçlı placeholder oluşturma (manuel çağırma)
tasks.register("createFirebasePlaceholder") {
    group = "firebase"
    description = "Geçici, üretim dışı bir google-services.json placeholder dosyası oluşturur."
    doLast {
        val f = file("google-services.json")
        if (f.exists()) {
            println("google-services.json zaten mevcut, işlem yapılmadı.")
        } else {
            f.writeText("""{
  \"project_info\": {\"project_number\": \"0\",\"firebase_url\": \"https://placeholder.firebaseio.com\",\"project_id\": \"placeholder\",\"storage_bucket\": \"placeholder.appspot.com\"},
  \"client\": [{
    \"client_info\": {\"mobilesdk_app_id\": \"1:0:android:placeholder\",\"android_client_info\": {\"package_name\": \"com.example.tugis3\"}},
    \"api_key\": [{\"current_key\": \"AI_placeholder_KEY\"}],
    \"services\": {\"appinvite_service\": {\"other_platform_oauth_client\": []}}
  }],
  \"configuration_version\": \"1\"
}""")
            println("Placeholder google-services.json oluşturuldu (SADECE GELİŞTİRME İÇİN!)")
        }
    }
}

android {
    namespace = "com.example.tugis3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tugis3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Hilt test runner
        testInstrumentationRunner = "com.example.tugis3.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
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
            // Debug crashlytics upload devre dışı (isteğe bağlı)
            // firebaseCrashlytics { mappingFileUploadEnabled = false }
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-saveable")

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

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

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
    implementation("com.google.android.material:material:1.13.0")
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
    arg("dagger.fastInit", "enabled")
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}
