# Uygulamam Dil

YouTube videolarını ve PDF belgelerini, kullanıcının tanımladığı öğretim formatına dönüştüren çevrimdışı-öncelikli Android öğrenme uygulaması.

Bu depo tek kullanımlık bir prototip değildir. Android istemcisi, sunucu, LLM sağlayıcısı, TTS servisi, sözleşmeler ve altyapı dosyaları ayrı modüllerde tutulur.

## Faz durumu

- Faz 1: üretim mimarisi, native Android kabuğu, tasarım sistemi, menüler, ders oluşturma formu, ders çalışma alanı, domain doğrulaması ve CI temeli
- Faz 2: YouTube altyazı alımı, zaman kırpma, PDF sayfa çıkarma ve Türkçe/İngilizce OCR yedeği
- Faz 3: alan ekleme/silme/sıralama, öğretim modu, kart ayarları, canlı önizleme, değişmez format sürümleri ve deterministik JSON Schema
- Faz 4: Android–Render gerçek ders işi, kısa ömürlü önizleme oturumu, DeepSeek SSE, idempotent batch üretimi, kart checkpoint'i, duraklat/devam/iptal/yeniden dene ve sağlayıcı ölçümleri
- Yerel kayıt: oluşturulan ders ayarı ve üretilmiş kart modeli cihazda kalıcı olarak saklanır; sahte ders, profil, abonelik ve depolama kotası gösterilmez
- Mikrofon: Android konuşma tanıma ile ekran açıkken gerçek izin ve komut testi çalışır
- Render ve Supabase: altyapı/migration dosyaları hazırdır; canlı bağlantı doğrulanmadan arayüzde bağlı gösterilmez
- DeepSeek V4 Pro: Android doğrudan sağlayıcıya bağlanmaz; anahtar yalnız Render secret store'da kalır ve uygulama imzalı kısa ömürlü sunucu oturumu kullanır
- Edge TTS: ayrı Python servisidir; Android oynatma ve ses önbelleği Faz 5'te bağlanır

Ayrıntılı kararlar için [mimari belgesine](docs/ARCHITECTURE.md), teslim sırası için [10 fazlık yol haritasına](docs/ROADMAP.md), ekran oranları için [mobil arayüz ölçü sözleşmesine](docs/UI_SPEC.md) bakın.

## Depo yapısı

```text
android/
  app/                       Uygulama kabuğu ve yönlendirme
  core/model/                Platformdan bağımsız ders domain modeli
  core/designsystem/         Renk, tipografi ve ortak Compose bileşenleri
  feature/home/              Ana sayfa
  feature/lesson/            Kaynak/format ayarı ve ders çalışma alanı
  feature/library/           Gerçek yerel kayıtları arayan/filtreleyen ders kütüphanesi
  feature/profile/           Bağlantı durumu ve gerçek mikrofon komut testi
  feature/onboarding/        Tanıtım ve Android izin akışı
backend/
  domain/                    Sunucu domain portları
  application/               İş akışı ve orkestrasyon
  infrastructure/deepseek/   DeepSeek V4 Pro istemcisi
  infrastructure/source/     YouTube transcript, PDF text ve OCR zinciri
  infrastructure/supabase/   Supabase bağlantı ve sağlık adaptörü
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
./gradlew :android:core:model:test :android:core:data:test :backend:application:test
```

APK, `android/app/build/outputs/apk/debug/app-debug.apk` altında oluşur. Aynı komut GitHub Actions tarafından çalıştırılır; ZIP bütünlüğü, hizalama, paket kimliği, Android 11 uyumluluğu ve APK imzası doğrulandıktan sonra artifact olarak yayımlanır. Önizleme derlemeleri güncelleme kurulabilmesi için sabit ve yalnız geliştirmede kullanılan bir test anahtarıyla imzalanır.

## Gizli değerler

Hiçbir API anahtarı depoya veya APK'ya yazılmaz.

```text
DEEPSEEK_API_KEY
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
SUPABASE_URL                 yalnızca sunucu
SUPABASE_SERVICE_ROLE_KEY    yalnızca sunucu
```

Android uygulaması yalnızca kısa ömürlü önizleme/kullanıcı oturumunu ve Render API adresini alır. DeepSeek ve Supabase servis anahtarları sunucuda kalır. Faz 4 dosya checkpoint'i telefon bağlantısı kesildiğinde işi sürdürür; Render instance yeniden başlatmalarında kalıcılık Faz 7 Supabase job store ile tamamlanacaktır.
