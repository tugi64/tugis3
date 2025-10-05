package com.example.tugis3.baselineprofile

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uygulama için Baseline Profile üretir. Workflow:
 *  1. Uygulamayı açar
 *  2. Ana liste / ilk Compose ekranının çizilmesini bekler
 *  3. (Genişletilebilir) basit bir gezinme senaryosu çalıştırır
 *
 * Çalıştırma:
 *  ./gradlew :baselineprofile:connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalBaselineProfilesApi::class)
class GenerateBaselineProfile {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() = baselineRule.collect(
        packageName = "com.example.tugis3",
        maxIterations = 8
    ) {
        // Ana aktivite yüklendi mi (Compose root View)? Basit bir timeout ile bekler.
        // Burada belirli bir text/id aranabilir. Gerekirse gerçek UI hiyerarşisine göre düzenleyin.
        device.wait(Until.hasObject(By.pkg("com.example.tugis3").depth(0)), 5_000)
        // TODO: Örnek ek gezinme adımları ekleyin (menü aç, sayfa değiştir vs.)
    }
}

