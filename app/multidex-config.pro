# MultiDex optimizasyonu - Galaxy A21s için hızlı yükleme
# Ana DEX'te sadece başlangıç için gerekli sınıfları tut

# Uygulama başlangıç sınıfları
-keep class com.example.tugis3.TugisApplication { *; }
-keep class com.example.tugis3.ui.navigation.MainNavigationActivity { *; }
-keep class com.example.tugis3.ui.MainActivity { *; }

# Hilt Application
-keep class dagger.hilt.android.** { *; }
-keep class androidx.multidex.** { *; }

# Compose başlangıç
-keep class androidx.activity.ComponentActivity { *; }
-keep class androidx.compose.** { *; }

