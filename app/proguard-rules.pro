# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

########## R8 / Proguard Optimization Additions ##########
# Hilt / Dagger generated code (genellikle plugin ekliyor ama koruma amaçlı)
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.internal.**

# Keep @HiltAndroidApp uygulama alt sınıfı
-keep @dagger.hilt.android.internal.managers.ApplicationComponentManager class * { *; }
-keep class ** extends android.app.Application { *; }

# Room (annotation processing yansımada metot imzalarını kullanır)
-dontwarn androidx.room.**
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Coroutines (genellikle gerekmez ama agresif optimizasyonlarda güvenlik için)
-dontwarn kotlinx.coroutines.**

# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Gson (model sınıflarınız yansımayla serileşiyorsa paketleri koruyun)
# Örn: -keep class com.example.tugis3.data.api.model.** { *; }

# Crashlytics keep kuralları plugin tarafından ekleniyor; burada ek güvenlik adına log paketini koruyabiliriz.
-dontwarn com.google.firebase.crashlytics.**

# Shrinker yanlış pozitifleri azaltma: (gerektiğinde açın)
# -printusage build/outputs/mapping/unused.txt
##########################################################
