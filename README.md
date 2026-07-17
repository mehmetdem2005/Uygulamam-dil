# Uygulamam Dil

YouTube videolarını ve PDF belgelerini, kullanıcının tanımladığı öğretim formatına dönüştüren çevrimdışı-öncelikli Android öğrenme uygulaması.

Bu depo tek kullanımlık bir prototip değildir. Android istemcisi, sunucu, LLM sağlayıcısı, TTS servisi, sözleşmeler ve altyapı dosyaları ayrı modüllerde tutulur.

## Faz durumu

- Faz 1: üretim mimarisi, native Android kabuğu, tasarım sistemi, menüler, ders oluşturma formu, ders çalışma alanı, domain doğrulaması ve CI temeli
- Supabase ve Render: hesap bilgileri eklenene kadar yerel adaptör sınırında
- DeepSeek V4 Pro: anahtar yalnızca sunucuda kullanılan gerçek sağlayıcı adaptörü
- Edge TTS: ayrı Python servisi; üretilen sesler sonraki fazda içerik karmasıyla önbelleklenecek

Ayrıntılı kararlar için [mimari belgesine](docs/ARCHITECTURE.md), teslim sırası için [10 fazlık yol haritasına](docs/ROADMAP.md) bakın.

## Depo yapısı

```text
android/
  app/                       Uygulama kabuğu ve yönlendirme
  core/model/                Platformdan bağımsız ders domain modeli
  core/designsystem/         Renk, tipografi ve ortak Compose bileşenleri
  feature/home/              Ana sayfa
  feature/lesson/            Kaynak/format ayarı ve ders çalışma alanı
backend/
  domain/                    Sunucu domain portları
  application/               İş akışı ve orkestrasyon
  infrastructure/deepseek/   DeepSeek V4 Pro istemcisi
  api/                       Render üzerinde çalışacak Ktor API
services/edge-tts/            Edge TTS mikroservisi
contracts/                    Mobil-sunucu sözleşmeleri
docs/                         Mimari ve faz planı
.github/workflows/            APK ve kalite kontrolleri
```

## Yerel geliştirme

Gereksinimler: JDK 17 ve Android SDK 36. Gradle 9.4.1, SHA-256 doğrulamalı wrapper ile gelir.

```bash
./gradlew :android:app:assembleDebug
./gradlew :android:core:model:test :backend:application:test
```

APK, `android/app/build/outputs/apk/debug/app-debug.apk` altında oluşur. Aynı komut GitHub Actions tarafından çalıştırılır ve APK artifact olarak yayımlanır.

## Gizli değerler

Hiçbir API anahtarı depoya veya APK'ya yazılmaz.

```text
DEEPSEEK_API_KEY
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
SUPABASE_URL                 sonraki bağlantı fazı
SUPABASE_SERVICE_ROLE_KEY    yalnızca sunucu
```

Android uygulaması yalnızca kısa ömürlü kullanıcı oturumunu ve Render API adresini alır. DeepSeek ve Supabase servis anahtarları sunucuda kalır.
