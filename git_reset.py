import subprocess
import sys
from datetime import datetime, timedelta

# Git durumunu kontrol et
print("=== Git Status ===")
subprocess.run(["git", "status"], check=False)

print("\n=== Son 10 commit ===")
result = subprocess.run(["git", "log", "--oneline", "-10"], capture_output=True, text=True)
print(result.stdout)

print("\n=== Son 10 dakikadaki commitler ===")
result = subprocess.run(["git", "log", "--oneline", "--since=10 minutes ago"], capture_output=True, text=True)
print(result.stdout)

print("\n=== Değiştirilmiş dosyalar ===")
result = subprocess.run(["git", "diff", "--name-only", "HEAD"], capture_output=True, text=True)
print(result.stdout)

print("\n=== Son commite dön? (y/n) ===")
# Otomatik olarak hayır diyelim, önce kullanıcı görsün

