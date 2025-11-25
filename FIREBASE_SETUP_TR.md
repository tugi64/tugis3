# Firebase Kurulum Rehberi

Firebase başarıyla projeye eklendi! 🎉

## ✅ Yapılan Değişiklikler

1. **Root `build.gradle.kts`**: Firebase Crashlytics ve Google Services plugin'leri etkinleştirildi
2. **`app/build.gradle.kts`**: Firebase bağımlılıkları eklendi
   - Firebase Analytics
   - Firebase Crashlytics
   - Firebase BoM (Bill of Materials) - sürüm yönetimi için

## 📋 Yapmanız Gerekenler

### 1. Google Services JSON Dosyasını Ekleyin

Firebase çalışması için `google-services.json` dosyasına ihtiyacınız var:

#### Adım 1: Firebase Console'a Gidin
1. [Firebase Console](https://console.firebase.google.com/) açın
2. Projenizi seçin veya yeni proje oluşturun
3. "Add app" → Android simgesine tıklayın

#### Adım 2: Uygulama Bilgilerini Girin
- **Android package name**: `com.example.tugis3`
- **App nickname**: Tugis3 (isteğe bağlı)
- **SHA-1**: Gerekirse aşağıdaki komutu çalıştırın:
  ```cmd
  gradlew.bat signingReport
  ```
  Veya debug keystore için:
  ```cmd
  keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

#### Adım 3: google-services.json İndirin
1. Firebase Console'dan `google-services.json` dosyasını indirin
2. Dosyayı şu konuma kopyalayın:
   ```
   C:\Users\CASPER\AndroidStudioProjects\tugis3\app\google-services.json
   ```

### 2. Gradle Sync Yapın

Android Studio'da:
- **File → Sync Project with Gradle Files** (veya Ctrl+Shift+O)

### 3. Firebase'i Test Edin

```kotlin
// MainActivity veya herhangi bir Activity'de
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {
    private lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Firebase Analytics
        analytics = FirebaseAnalytics.getInstance(this)
        
        // Test event
        val bundle = Bundle()
        bundle.putString("screen_name", "main")
        analytics.logEvent("app_open", bundle)
        
        // Firebase Crashlytics
        FirebaseCrashlytics.getInstance().log("App started")
        
        // Test crash (dikkatli kullanın!)
        // FirebaseCrashlytics.getInstance().setCustomKey("test_key", "test_value")
        // throw RuntimeException("Test Crash")
    }
}
```

## 🔧 Ek Firebase Servisleri

Şu an etkin olan servisler:
- ✅ **Firebase Analytics** - Kullanıcı davranışı ve event tracking
- ✅ **Firebase Crashlytics** - Crash raporlama

Ekleyebileceğiniz diğer servisler:
- **Firebase Authentication** - Kullanıcı girişi (Google Sign-In zaten aktif!)
- **Firebase Firestore** - NoSQL veritabanı
- **Firebase Cloud Messaging** - Push notifications
- **Firebase Remote Config** - Uzaktan yapılandırma
- **Firebase Performance** - Performans izleme

### Authentication Eklemek İçin

`libs.versions.toml` dosyasına ekleyin:
```toml
[versions]
firebaseAuth = "22.3.0"

[libraries]
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx", version.ref = "firebaseAuth" }
```

`app/build.gradle.kts` dosyasına:
```kotlin
implementation(libs.firebase.auth)
```

## 🚀 ProGuard/R8 Ayarları

Firebase için ProGuard kuralları zaten `firebase-bom` tarafından otomatik eklenir.
Ek kural gerekmez, ancak özel durum için `proguard-rules.pro` kontrol edin.

## 📊 Firebase Console'da Veri Görüntüleme

1. [Firebase Console](https://console.firebase.google.com/)
2. Projenizi seçin
3. **Analytics → Events** - Event'leri görün
4. **Crashlytics → Dashboard** - Crash raporlarını görün

## ⚠️ Önemli Notlar

1. **Debug vs Release**:
   - Debug build: Crashlytics mapping upload kapalı (daha hızlı build)
   - Release build: Otomatik mapping upload aktif

2. **İnternet İzni**: 
   `AndroidManifest.xml` içinde internet izni zaten var olmalı:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

3. **Google Sign-In ile Entegrasyon**:
   Firebase Authentication kullanarak mevcut Google Sign-In'i geliştirebilirsiniz.

## 🆘 Sorun Giderme

### "google-services.json not found" Hatası
- Dosyanın `app/` dizininde olduğundan emin olun
- Gradle sync yaptığınızdan emin olun

### "FirebaseApp initialization unsuccessful" Hatası
- `google-services.json` dosyasının geçerli olduğundan emin olun
- Package name'in eşleştiğinden emin olun (`com.example.tugis3`)

### Crashlytics Raporları Gelmiyorsa
- İlk crash sonrası 24 saat içinde raporlar gelmeye başlar
- Debug build'de crash testleri yapmayı deneyin
- İnternet bağlantısını kontrol edin

## 📚 Kaynaklar

- [Firebase Documentation](https://firebase.google.com/docs/android/setup)
- [Firebase Analytics Guide](https://firebase.google.com/docs/analytics/get-started?platform=android)
- [Firebase Crashlytics Guide](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Firebase Authentication Guide](https://firebase.google.com/docs/auth/android/start)

---

**Sonraki Adım**: `google-services.json` dosyasını ekleyin ve Gradle sync yapın!

