package com.example.tugis3.benchmark

import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.FrameTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uygulama açılış (Cold / Warm) ve temel frame timing ölçümü.
 * Gerçek cihazda (profilable release / benchmark build) çalıştırmanız önerilir.
 * Örnek çalıştırma:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
 *      -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = "com.example.tugis3",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            // Cold start için uygulamayı tamamen öldür.
            killProcess()
        }
    ) {
        // LAUNCHER intent otomatik tetiklenir.
    }

    @Test
    fun warmStartup() = benchmarkRule.measureRepeated(
        packageName = "com.example.tugis3",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.WARM
    ) {
        // Warm start ölçümü (process yaşamaya devam eder, activity yeniden başlar)
    }
}
