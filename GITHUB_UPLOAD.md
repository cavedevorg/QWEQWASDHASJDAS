# GitHub'a Yükleme Adımları

Repo hazır! Şimdi GitHub'a yüklemek için:

## 1. GitHub'da Yeni Repo Oluştur

1. https://github.com/new adresine git
2. Repository name: `PrecTVProvider`
3. Public/Private seç
4. **"Add a README file" seçeneğini IŞARETLEME**
5. "Create repository" butonuna bas

## 2. Terminal'de Şu Komutları Çalıştır

```bash
cd C:\Users\caveman\testm\PrecTVProvider

# GitHub remote'u ekle (KULLANICI_ADIN ile değiştir)
git remote add origin https://github.com/KULLANICI_ADIN/PrecTVProvider.git

# Master branch'i main olarak değiştir (GitHub standart)
git branch -M main

# GitHub'a push et
git push -u origin main
```

## 3. İlk push için GitHub kullanıcı adı ve token gerekir

GitHub kullanıcı adın: `?`
Token: Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token

## 4. Tamam!

Repo yüklendi! Artık:
- `https://github.com/KULLANICI_ADIN/PrecTVProvider`
- Başkaları bu provider'ı kullanabilir
- Cloudstream Extensions reposuna ekleyebilirler

---

## Hızlı Komut (Tek Satır)

```bash
cd C:\Users\caveman\testm\PrecTVProvider && git remote add origin https://github.com/KULLANICI_ADIN/PrecTVProvider.git && git branch -M main && git push -u origin main
```
