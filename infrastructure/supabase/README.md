# Supabase altyapısı

Bu klasör, uygulamanın Supabase şemasının tek kaynak kaydıdır. Migration dosyaları
isim sırasıyla çalıştırılır ve uygulanan sürüm `public.app_schema_versions`
tablosuna yazılır.

İlk migration şunları kurar:

- Auth kullanıcısından otomatik profil ve ücretsiz hak kaydı
- kaynak, revizyon, format, ders, kart ve ilerleme tabloları
- TTS varlıkları, indirmeler ve arka plan işleri
- sağlayıcı kullanımı, maliyet, abonelik ve hak tabloları
- yönetim analitiği ve değiştirilemez denetim kaydı
- sahiplik/public içerik kuralları için RLS politikaları
- özel PDF ve TTS Storage bucket'ları
- anahtar göstermeyen `app_health()` doğrulama fonksiyonu

`SUPABASE_SERVICE_ROLE_KEY` yalnızca Render secret store'da tutulur. Android
uygulamasına veya GitHub dosyalarına hiçbir zaman yazılmaz.
