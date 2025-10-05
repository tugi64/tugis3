update_and_push.bat - Kullanım ve Açıklama

Bu script, proje dizininde (C:\Users\CASPER\AndroidStudioProjects\tugis3) çalıştırılmak üzere hazırlandı. Yapacağı işlemler:

1) .idea klasörünü otomatik olarak masaüstünüze yedekler (varsa):
   %USERPROFILE%\Desktop\tugis3_idea_backup

2) Uzak repo URL'sini sizin verdiğiniz GitHub adresine ayarlar:
   https://github.com/tugi64/tugis3.git

3) origin'den güncellemeleri çeker (git pull origin main --rebase). Eğer çakışma çıkarsa script hata verip durur.

4) Yerel değişiklikleri commit edip (varsa) origin/main'e push eder.

Nasıl çalıştırılır (Windows cmd.exe):

- Adım 1: Komut istemcisini açın (Başlat -> cmd veya Win+R, "cmd" yazıp Enter).
- Adım 2: Aşağıdaki komutu çalıştırın (isterseniz dosyayı çift tıklayarak da çalıştırabilirsiniz):

    C:\Users\CASPER\AndroidStudioProjects\tugis3\update_and_push.bat

Olası Hatalar ve Çözümleri:

- "'git' is not recognized": Git sistemde yüklü değil veya PATH'te değil. Git yükleyin: https://git-scm.com/downloads

- "ERROR: 'git pull' failed" ve "untracked working tree files would be overwritten by merge": Bu durumda .idea klasörünün yedeklendiğinden emin olun. Eğer script .idea'yı taşıyamadıysa veya başka takipsiz dosyalar varsa, manuel olarak taşımayı deneyin:

    mkdir "%USERPROFILE%\Desktop\tugis3_idea_backup"
    move .idea "%USERPROFILE%\Desktop\tugis3_idea_backup"

  Sonra tekrar script'i veya aşağıdaki komutları çalıştırın:

    git remote set-url origin https://github.com/tugi64/tugis3.git
    git fetch origin
    git pull origin main --rebase

- "Authentication failed" veya push sırasında kimlik hatası: GitHub kimlik doğrulaması gerekiyor. Kişisel erişim token (PAT) veya SSH anahtarı kullanın. GitHub dokümanlarına bakın.

- Pull veya rebase sırasında çatışma (conflict) çıkarsa: Çatışma olan dosyaları manuel olarak düzeltin, sonra:

    git add <düzeltilen-dosyalar>
    git rebase --continue

Sonrası: Eğer push başarılı olduysa, proje güncellenmiş olacaktır. Ardından Android Studio veya gradle ile derleme testi yapın:

    cd C:\Users\CASPER\AndroidStudioProjects\tugis3
    .\gradlew.bat clean assembleDebug

Eğer hata alırsanız, terminal çıktısını kopyalayıp bana gönderin; ben adım adım yardım edeceğim.

Not: Bu script .idea'yı yedekleyip taşıyacak — IDE proje ayarları makineye özgüdür, çoğunlukla repoda paylaşılması tavsiye edilmez. Eğer takımınızla paylaşılması gerekiyorsa, önce takım ile konuşun.

