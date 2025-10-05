plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.tugis3.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 23 // Macrobenchmark minimum önerilen
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY"
        testInstrumentationRunnerArguments["androidx.benchmark.output.enable"] = "true"
    }

    buildTypes {
        create("benchmark") {
            // Optimize edilmiş, ölçüm odaklı build type
            isDebuggable = false
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
        // debug & release otomatik geliyor (com.android.test)
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.core)
    implementation(platform(libs.androidx.compose.bom))
    // UI Automator (benchmark senaryolarında gerek duyulabilir)
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation(libs.androidx.profileinstaller)
}

// Benchmark testleri çalıştırma ipucu (emulator yerine gerçek cihaz tercih edilir):
// ./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark

// Basit benchmark özetleyici: JSON dosyalarından ( *benchmarkData.json ) temel metrikleri toparlar.
// Kullanım: ./gradlew :benchmark:summarizeBenchmarks
val summarizeBenchmarks by tasks.registering {
    group = "reporting"
    description = "benchmarkData.json dosyalarını tarayıp p50 değerlerinin ortalamasını özetler (saf Kotlin, regex tabanlı)."
    doLast {
        val resultFiles = fileTree(projectDir) { include("**/*benchmarkData.json") }.files
        if (resultFiles.isEmpty()) {
            println("[summarizeBenchmarks] Hiç benchmarkData.json yok. Önce macrobenchmark testlerini çalıştırın.")
            return@doLast
        }
        data class Stat(val key: String, val samples: MutableList<Double> = mutableListOf())
        val stats = linkedMapOf<String, Stat>()

        // Çok basit JSON ayıklayıcı: benchmark adı + metric adı + p50
        // 1) "name":"..." alanlarını sırayla okuyup mevcut benchmark adını tutuyoruz
        // 2) Ardından "metrics":{"metricName":{..."p50":X ...}} kalıbını regex ile parse ediyoruz (sınırlı ve gevşek)
        val nameRegex = "\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"".toRegex()
        val metricBlockRegex = "\\\"metrics\\\"\\s*:\\s*\\{(.*)\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
        val singleMetricRegex = "\\\"([a-zA-Z0-9_]+)\\\"\\s*:\\s*\\{([^}]*)\\}".toRegex()
        val p50Regex = "\\\"p50\\\"\\s*:\\s*([0-9]+(\\.[0-9]+)?)".toRegex()

        resultFiles.forEach { f ->
            runCatching {
                val text = f.readText()
                // Her benchmark girdisini name üzerinden yakala
                val names = nameRegex.findAll(text).map { it.groupValues[1] }.toList()
                // Tek metrics bloğunu çıkar (varsayım: yapı basit)
                val metricsSection = metricBlockRegex.find(text)?.groupValues?.get(1) ?: return@runCatching
                // Her metric alt bloğunu tara
                singleMetricRegex.findAll(metricsSection).forEach { m ->
                    val metricName = m.groupValues[1]
                    val body = m.groupValues[2]
                    val p50 = p50Regex.find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@forEach
                    // Çoklu name varsa aynı sıra ilişkisini garanti edemiyoruz, basitçe ilk name'i kullan
                    val benchName = names.firstOrNull() ?: "unknown"
                    val key = "$benchName:$metricName"
                    stats.getOrPut(key) { Stat(key) }.samples += p50
                }
            }.onFailure { ex ->
                println("[summarizeBenchmarks] ${f.name} parse hatası: ${ex.message}")
            }
        }

        val outDir = layout.buildDirectory.dir("benchmark-summary").get().asFile
        outDir.mkdirs()
        val outFile = outDir.resolve("summary.txt")
        outFile.printWriter().use { pw ->
            pw.println("Benchmark Summary (regex p50 ortalamaları)\n")
            if (stats.isEmpty()) {
                pw.println("(Hiç veri toplanamadı – JSON formatı beklenenden farklı olabilir)")
            } else {
                stats.values.sortedBy { it.key }.forEach { st ->
                    val avg = st.samples.average()
                    pw.println("${st.key} -> avg_p50=${"%.2f".format(avg)} ms (${st.samples.size} örnek)")
                }
            }
        }
        println("[summarizeBenchmarks] Rapor oluşturuldu: ${outFile.absolutePath}")
    }
}
