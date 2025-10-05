// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Detekt temel konfigürasyonu
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    parallel = true
    ignoredBuildTypes = listOf("release")
    config.from(files(rootProject.file("detekt.yml")))
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
