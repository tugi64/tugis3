# KSP (Kotlin Symbol Processing) HATASI ÇÖZÜMÜ

## SORUN
```
e: [ksp] java.lang.NullPointerException
at com.google.devtools.ksp.symbol.impl.java.KSClassDeclarationJavaImpl$simpleName$2.invoke
```

## NEDEN
- `app/src/main/java/com/` klasöründe bozuk/boş Java dosyaları var
- Bu dosyalar eskiden silinmiş ama izleri kalmış
- KSP bu dosyaları işlerken `NullPointerException` veriyor

## ÇÖZÜM

### Yöntem 1: Otomatik Düzeltme (ÖNERİLEN)
1. Terminalde şu komutu çalıştır:
   ```
   fix_ksp_complete.bat
   ```

2. Sonra build yap:
   ```
   gradlew assembleDebug --no-daemon
   ```

### Yöntem 2: Manuel Düzeltme
1. Şu klasörleri sil:
   - `app/src/main/java/com/survey/`
   - `app/src/main/java/com/tools/`
   - `app/src/main/java/com/project/`
   - `app/src/main/java/com/device/`
   - `app/src/main/java/com/MainActivity.java`

2. KSP cache'i temizle:
   ```
   rmdir /s /q app\build\kspCaches
   ```

3. Clean build yap:
   ```
   gradlew clean --no-daemon
   gradlew assembleDebug --no-daemon
   ```

## ÖNLEME
- Dosya sildiğinde Git'ten de sil: `git rm dosya.java`
- Build klasörlerini düzenli temizle: `gradlew clean`
- KSP hataları için önce cache'i temizle

## DOSYALAR
- `fix_ksp_complete.bat` - Tam otomatik çözüm
- `fix_ksp_error.bat` - Sadece cache temizleme
- `delete_broken_java_files.bat` - Sadece bozuk dosyaları sil

## TEST
Build başarılı olduktan sonra:
1. Bluetooth bağlantısını test et
2. GPS sinyallerini kontrol et
3. Cihaz iletişimini dene

## NOTLAR
- KSP Java ve Kotlin dosyalarını birlikte işler
- Bozuk Java dosyaları KSP'yi bozabilir
- Cache sorunları --no-daemon ile çözülebilir

