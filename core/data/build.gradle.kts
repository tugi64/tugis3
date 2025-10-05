plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.tugis3.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val fallbackOff = project.findProperty("fallbackOff") != null
        buildConfigField("boolean", "ENABLE_DESTRUCTIVE_FALLBACK", (!fallbackOff).toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Release'te fallback her zaman kapalı kalsın
            buildConfigField("boolean", "ENABLE_DESTRUCTIVE_FALLBACK", "false")
        }
        debug {
            // Debug'ta Gradle property ile kapatılmadıysa açık
            val fallbackOff = project.findProperty("fallbackOff") != null
            buildConfigField("boolean", "ENABLE_DESTRUCTIVE_FALLBACK", (!fallbackOff).toString())
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        buildConfig = true // BuildConfig referansı (DataModule) için gerekli
    }
}

dependencies {
    // Hilt & Room
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Optional: unit test (placeholder)
    testImplementation(libs.junit)

    // Android Instrumented Test (migration & hilt)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Hilt testler için KSP
    kspAndroidTest(libs.hilt.compiler)

    implementation(project(":core:cad"))
}

// Migration testleri için bilgilendirici kısayol task (filtrelemek için komut satırı parametresi gerekli)
tasks.register("runMigrationTests") {
    group = "verification"
    description = "Run only migration instrumentation tests (use -Pandroid.testInstrumentationRunnerArguments.class=... for filtering)."
    dependsOn(":core:data:connectedDebugAndroidTest")
    doFirst {
        println("Migration tests çalıştırılıyor. Sadece MigrationTest sınıfını çalıştırmak için örnek:")
        println("  gradlew :core:data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.tugis3.data.db.MigrationTest")
    }
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}
