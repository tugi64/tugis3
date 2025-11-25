// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    // Firebase: google-services.json eklendikten sonra etkinleştirin
    // alias(libs.plugins.firebase.crashlytics) apply false
    // alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versions.plugin)
}

// Global dependency alignment: Yalnızca Room sürümlerini sabitle,
// kotlinx-serialization sürümünü KSP classpath'ine zorlamaktan kaçın.
// Gradle 9+ ile uyum için allprojects yerine subprojects kullan.
subprojects {
    configurations.configureEach {
        val n = name
        // KSP/kapt/annotation processor ve Kotlin compiler classpath konfigürasyonlarını atla
        if (
            n.startsWith("ksp", ignoreCase = true) ||
            n.contains("kapt", ignoreCase = true) ||
            n.contains("annotationProcessor", ignoreCase = true) ||
            n.contains("kotlinCompilerClasspath", ignoreCase = true)
        ) return@configureEach

        resolutionStrategy {
            force(
                "androidx.room:room-runtime:2.8.2",
                "androidx.room:room-compiler:2.8.2",
                "androidx.room:room-ktx:2.8.2"
            )

            // Kotlin metadata uyumsuzluğunu çözmek için Kotlin stdlib'i sabitle
            force(
                "org.jetbrains.kotlin:kotlin-stdlib:2.2.21",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.21",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.21"
            )

            // Google Maps kütüphanelerini Kotlin 2.0.x uyumlu versiyonlara force et
            force(
                "com.google.maps.android:maps-compose:4.3.3",
                "com.google.maps.android:maps-utils-ktx:3.4.0",
                "com.google.maps.android:android-maps-utils:3.19.1"
            )

            // Coroutines sürümü sabitle
            force(
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
            )

            // OkHttp ve OkIO uyumlu sürümlerini force et
            force(
                "com.squareup.okhttp3:okhttp:4.12.0",
                "com.squareup.okio:okio:3.16.4"
            )

            // ÖNEMLİ: kotlinx-serialization sürümünü burada zorlamıyoruz;
            // böylece Room'un derleme zamanında beklediği sürüm KSP classpath'inde kullanılabilir.
        }
    }
}

// Detekt konfigürasyonu - root proje
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/detekt.yml"))
}

// Detekt ve Ktlint'i tüm alt projelere uygula
subprojects {
    pluginManager.apply("io.gitlab.arturbosch.detekt")
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    // Alt projeler için detekt konfigürasyonu (root ayarıyla aynı dosyayı paylaşır)
    extensions.findByName("detekt")?.let { ext ->
        (ext as io.gitlab.arturbosch.detekt.extensions.DetektExtension).apply {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
        }
    }

    // Ktlint için basit ayarlar
    extensions.findByName("ktlint")?.let { ext ->
        (ext as org.jlleitschuh.gradle.ktlint.KtlintExtension).apply {
            android.set(true)
            verbose.set(false)
        }
    }
}

// Tüm modüllerde statik analizleri çalıştıran ortak quality görevi
tasks.register("quality") {
    group = "verification"
    description = "Run code quality checks (Detekt + Ktlint) on all modules"
    // Her alt projede var olan detekt ve ktlintCheck görevlerine zincirle
    subprojects.forEach { p ->
        dependsOn(p.tasks.matching { it.name == "detekt" })
        dependsOn(p.tasks.matching { it.name == "ktlintCheck" })
    }
}

// Tüm modüllerde otomatik format (ktlintFormat) çalıştıran görev
tasks.register("format") {
    group = "verification"
    description = "Run code formatters (ktlintFormat) on all modules"
    subprojects.forEach { p ->
        dependsOn(p.tasks.matching { it.name == "ktlintFormat" })
    }
}

// Tüm modüllerde lint kontrolü çalıştıran görev
tasks.register("lintAll") {
    group = "verification"
    description = "Run Android Lint on all modules"
    subprojects.forEach { p ->
        dependsOn(p.tasks.matching { it.name == "lint" })
    }
}

// Bağımlılık güncellemelerini kontrol etmek için görev (versions plugin)
tasks.named("dependencyUpdates").configure {
    group = "help"
    description = "Check for dependency updates"
}

// Temizleme
tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
