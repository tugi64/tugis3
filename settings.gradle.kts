pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Dependency verification (lenient başlangıç - ileride strict yapılabilir)
// Bu, supply-chain güvenliğini artırmak için temel bir iskelet sağlar.
// Dilersen gradle --write-verification-metadata sha256 çalıştırıp metadata sabitleyebilirsin.
/*dependencyVerification {
    verificationMode.set(VerificationMode.STRICT)
}*/

rootProject.name = "tugis3"
include(":app")
include(":core:data")
include(":core:ui")
include(":core:cad")
