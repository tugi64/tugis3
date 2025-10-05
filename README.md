# Tugis3 Android App

Modern Android uygulaması - Jetpack Compose ile geliştirilmiştir.

## ♻️ Temizlik (2025-10)
- Eski performans modülleri (:benchmark, :baselineprofile) kaldırıldı.
- Profile Installer bağımlılığı çıkarıldı.
- İlgili CI workflow (macrobenchmark) devre dışı bırakıldı / silinebilir.

## 🚀 Özellikler

- Jetpack Compose UI
- Material Design 3
- Hilt (KSP + Kotlin Compose plugin uyumlu)
- Room + Flow reaktif veri akışı
- Gerçek 5 -> 6 Migration (updatedAt sütunu) + Migration testleri
- Coroutines tabanlı asenkron yapı
- Navigation Compose
- Firebase Crashlytics / Analytics (google-services.json varsa otomatik etkin)
- R8 shrink + resource shrinking (release build)
- Detekt + Ktlint statik analiz (CI entegrasyonu)
- LeakCanary (debug hafıza sızıntısı tespiti)

## 🛠️ Teknolojiler

- **Kotlin** 2.0.21 (JVM 17) + `org.jetbrains.kotlin.plugin.compose`
- **Jetpack Compose** (BOM: 2024.09.02)
- **Hilt (Dagger)** – KSP ile annotation processing
- **Room** – Migration kontrollü (v6)
- **Retrofit & OkHttp** – Ağ katmanı
- **Coroutines / Flow** – Eşzamanlılık & reaktivite
- **Firebase** – Crashlytics & Analytics (koşullu aktivasyon)
- **Detekt / Ktlint** – Kod kalitesi

## 🧱 Mimari Genel Bakış

- Data: Room DAO + Repository (+ updatePointBasic ile kısmi güncelleme)
- DI: Hilt SingletonComponent modülleri (`AppModule` + `AppDatabaseMigrations`)
- UI: Compose ekranları + ViewModel
- Akış: DAO → Flow → ViewModel → Compose State

## 🔄 Kapt -> KSP Geçişi

- Kapt tamamen kaldırıldı; Hilt ve Room derleyicileri `ksp(...)` ile.
- `gradle.properties` incremental / configuration-cache aktif.
- Hilt test runner: `HiltTestRunner`.

KSP Argümanları:
- `dagger.fastInit=enabled`
- `dagger.hilt.android.internal.disableAndroidSuperclassValidation=true`
- `room.schemaLocation=app/schemas`

## 📱 Gereksinimler

- Android Studio Hedgehog/İguana (Kotlin 2.0 Compose desteği)
- JDK 17
- Min SDK 24 / Target & Compile SDK 36

## ⚙️ Performans Ayarları

- `org.gradle.configuration-cache=true`
- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `kotlin.incremental.useClasspathSnapshot=true`
- `ksp.incremental=true`
- `android.enableJetifier=false` (gerekirse geri açılabilir)

## 🔐 R8 / Shrink

Release:
- `isMinifyEnabled=true`, `isShrinkResources=true`
- `proguard-rules.pro` içinde Hilt, Room, Retrofit, Crashlytics koruma

## ☁️ Firebase Entegrasyonu (Koşullu)

`app/build.gradle.kts` Firebase pluginleri sadece `app/google-services.json` mevcutsa apply edilir.

Aktif Etmek İçin:
1. Firebase Console’da proje oluştur.
2. Android uygulama (paket: `com.example.tugis3`) ekle.
3. `google-services.json` dosyasını `app/` dizinine koy.
4. Derle: `gradlew :app:assembleDebug`.

Geçici Placeholder (sadece local derleme, üretim dışı!):
```
gradlew :app:createFirebasePlaceholder
```

## 🗄️ Room Migration (5 -> 6 -> 7 -> 8)

Sürüm zinciri:
- v5 -> v6: `points` tablosuna `updatedAt INTEGER NOT NULL DEFAULT 0` eklendi.
- v6 -> v7: Şema değişikliği yok (identity hash senkronizasyonu / version bump).
- v7 -> v8: `points(projectId)` ve `points(name)` alanları için index eklendi (sorgu performansı).

Kod referansı: `AppDatabaseMigrations` (core/data). Aktif veritabanı dosyası adı: `tugis3.db`.

Debug derlemelerde hızlı geliştirme için `fallbackToDestructiveMigration()` etkin (DataModule içinde koşullu). Release'te veri koruma için uygulanmaz.

Legacy ikinci veritabanı (`app/src/.../database/AppDatabase.kt`) artık `@Deprecated` ve adı `legacy_tugis3_database`; kaldırılana kadar geriye dönük kodu etkilemesin diye tutuluyor.

### Migration Testleri
`core/data/src/androidTest/.../MigrationTest.kt` dosyasında tam zincir (5->8) ve indeks doğrulama testleri örneği mevcut.
Çalıştırma:
```bash
./gradlew :core:data:connectedDebugAndroidTest --tests "*MigrationTest*"
```

Taze kurulum senaryosu için doğrudan v8 açılışı da testte kontrol ediliyor.

## 🔧 Kurulum

```bash
git clone https://github.com/YOUR_USERNAME/tugis3.git
cd tugis3
```
Android Studio ile aç ve Gradle Sync tamamlanınca çalıştır.

## 📦 Build

```bash
# Debug
./gradlew :app:assembleDebug
# Release (imza yapılandırması sonrası)
./gradlew :app:assembleRelease
```
Windows CMD karşılığı `gradlew.bat`.

## ✅ Test & Kalite

```bash
# Birim test
./gradlew :app:testDebugUnitTest
# Instrumented test (emulator/cihaz gerekir)
./gradlew :app:connectedDebugAndroidTest
# Detekt
./gradlew detekt
# Ktlint kontrol
./gradlew ktlintCheck
# Ktlint otomatik format
./gradlew ktlintFormat
```
Windows için `./gradlew` yerine `gradlew.bat`.

Örnek testler:
- `PointRepositoryTest` – Fake DAO birim testi (updatePointBasic dahil)
- `PointDaoTest` – In‑memory DAO testi
- `PointRepositoryInjectionTest` – Hilt injection
- `MigrationPlaceholderTest` – Şema sütunu doğrulama
- `RealMigrationTest` – Gerçek v5 -> v6 migration

## 🛠️ Repository Özel (updatePointBasic)
`PointRepository.updatePointBasic(id, name, northing, easting)` temel kısmi güncellemeyi yapar ve `updatedAt` zaman damgasını otomatik günceller.

## 🤖 CI

`android-ci.yml` adımları:
1. Assemble Debug
2. Unit Tests
3. Detekt
4. Ktlint Check

Instrumented test job’u yorum satırında.

## 🔍 Sorun Giderme

| Sorun | Çözüm |
|-------|-------|
| Compose plugin uyarısı | `org.jetbrains.kotlin.plugin.compose` eklendi (sync gerekebilir) |
| Firebase hata: missing google-services.json | Dosyayı ekle ya da koşullu apply sayesinde ignore et; gerekirse `createFirebasePlaceholder` çalıştır |
| Migration başarısız | MigrationTestHelper ile hata çıktısını incele; SQL ALTER doğrula |
| updatedAt güncellenmiyor | Doğrudan `upsert` yerine `updatePointBasic` kullan
| Jetifier sonrası eski bağımlılık | `gradlew :app:dependencies` ile hangi artifact support refer ediyor bulun |

## 🚀 Sonraki İyileştirme Önerileri

- Migration 6->7 planı (index ekleme / veri transformasyonu)
- Modularization (core-data, core-network, feature-* modülleri)
- Crashlytics sadece release mapping upload scripti
- Compose Preview performans profili
- Dependabot ile otomatik PR inceleme şablonu

## 🔄 Geri Alma (Rollback)

| Değişiklik | Geri Alma |
|------------|-----------|
| KSP | Hilt & Room derleyicilerini `kapt(...)` yap + kapt plugin ekle |
| Jetifier kapalı | `android.enableJetifier=true` |
| Migration aktif | `fallbackToDestructiveMigration()` tekrar ekle (veri kaybı riskine dikkat) |
| Firebase koşullu | Pluginleri doğrudan plugins bloğuna geri yaz |
| R8 shrink | Release build type’da shrink ayarlarını kapat |

## 📄 Lisans

MIT

## 🤝 Katkıda Bulunma

```bash
git checkout -b feature/AmazingFeature
git commit -m "Add AmazingFeature"
git push origin feature/AmazingFeature
```
PR aç.

## 📞 İletişim

Proje Sahibi - [@YOUR_USERNAME](https://github.com/YOUR_USERNAME)

Proje Linki: https://github.com/YOUR_USERNAME/tugis3
