# 10 Fazlık Teslim Planı

Her faz derlenebilir durumda kapanır. Sonraki faz için geçici demo kodu ana dala taşınmaz.

## Faz 1 — Mimari ve uygulama kabuğu

- çok modüllü monorepo
- Android tasarım sistemi, menüler, ana sayfa
- YouTube/PDF ve aralık/format ayar formu
- çizime uygun kaynak + öğretim kartı çalışma alanı
- domain doğrulaması ve birim testleri
- backend portları, gerçek DeepSeek sağlayıcı sınırı
- GitHub Actions debug APK

Kabul: temiz checkout CI üzerinde testleri geçer ve kurulabilir debug APK artifact üretir.

## Faz 2 — Kaynak alımı ✅

- Android PDF belge seçici ve kalıcı URI izni
- YouTube URL/video kimliği doğrulama ve gömülü oynatıcı
- zaman/sayfa aralığı seçici
- backend PDF text extraction, taranmış belge tespiti ve OCR kuyruğu
- transcript sağlayıcı zinciri ve hata/fallback ekranı

Kabul: seçilen aralıktan deterministik `SourceSegment` kayıtları oluşur.

## Faz 3 — Format tasarımcısı ✅

- alan ekleme/silme/sıralama
- dil, seviye, öğretim yöntemi ve özel talimat
- kart genişliği ve görünüm ayarları
- istek başına/iş toplamı blok sınırı
- format sürümleme, kopyalama ve JSON Schema derleme

Kabul: aynı format aynı alan şemasını üretir; eski dersler yeni düzenlemeden etkilenmez.

Uygulanan doğrulama: alanlar konum ve anahtara göre kanonik sıralanır; derlenen JSON Schema
SHA-256 kimliğiyle saklanır. Düzenleme mevcut kaydı değiştirmez, yeni revision üretir. Kopyalama
ayrı `formatId` ve revision 1 ile başlar. Supabase format revision kayıtları update/delete'e kapalıdır.

### Faz 3 sağlamlaştırma — yerel gerçek veri ✅

- sabit ders/eğitmen/profil/premium/kota örnekleri kaldırıldı
- ana sayfa, kütüphane ve ders ayrıntısı aynı kalıcı yerel kaydı kullanır
- ders ayarıyla birlikte üretilmiş kart listesi de JSON olarak cihazda saklanabilir
- arama, kaynak filtresi, silme onayı, ders açma ve dil seçimi gerçek durum değiştirir
- mikrofon ekranı yalnız gerçekten dinlerken dinleme durumu gösterir

Kabul: uygulama yeniden açıldığında yerel dersler tekrar API çağrısı yapılmadan listelenir; bağlı
olmayan Supabase, abonelik, arka plan servisi veya kota arayüzde çalışıyormuş gibi gösterilmez.

## Faz 4 — LLM orkestrasyonu ✅

- DeepSeek V4 Pro SSE
- hızlı/kalite modu
- blok bazlı istek, batch seçeneği ve prefetch
- pause/resume/cancel, idempotency, retry ve maliyet sınırı
- token/istek/gecikme ölçümü

Kabul: bağlantı kesilse de iş sunucuda tamamlanır ve blok çoğaltmaz.

Uygulanan doğrulama: Android yalnız kısa ömürlü imzalı önizleme tokenı alır; sağlayıcı anahtarı
APK'ya girmez. Her iş ve batch idempotency kimliğiyle kaydedilir, kartlar sıra numarasına göre
tekilleştirilir ve her başarılı batch'ten sonra atomik dosya checkpoint'i yazılır. Mobil istemci
bağlantı kesilince yeniden bağlanır, tamamlanan kartları yerel depoya yazar ve pause/resume/cancel/
retry komutlarını gerçek API'ye gönderir. Render instance yeniden başlatmaları arasında kalıcı iş
deposu Faz 7 Supabase adaptörüyle tamamlanacaktır.

## Faz 5 — Edge TTS

- ayrı servis ve voice listesi
- hız, perde ve tekrar
- metin karmalı sunucu/cihaz cache
- Media3 kesintisiz sıra ve kilit ekranı kontrolleri

Kabul: daha önce üretilmiş ses çevrimdışı oynar ve ikinci kez TTS çağrısı yapmaz.

## Faz 6 — Mikrofon ve arka plan oturumu

- görünür microphone foreground service
- cihaz üstü komut tanıma
- devam, tekrar, dur, geri, açıkla komutları
- ses odağı, kulaklık ve çağrı kesintisi yönetimi

Kabul: ekran kilitliyken kullanıcı tarafından başlatılmış oturum bildirimle güvenli çalışır.

## Faz 7 — Supabase ve çevrimdışı senkronizasyon

- Auth, RLS, Storage ve Realtime
- Room şeması/migration
- outbox, conflict policy ve indirme yöneticisi
- private/unlisted/public ders ve fork

Kabul: uygulama silinmedikçe yerel ders kaybolmaz; hesaba girişte cihazlar arasında senkron olur.

## Faz 8 — Abonelik

- Play Billing 9.1.0
- ürün/teklif ekranı
- backend purchase doğrulama ve RTDN
- plan hakları, kota ve grace/suspended durumları

Kabul: yalnızca sunucuda doğrulanmış aktif hak ücretli özelliği açar.

## Faz 9 — Yönetici paneli

- ayrı admin frontend ve API
- trafik, proje, kullanıcı, abonelik ve büyüme grafikleri
- LLM/TTS istek, token, cache, hata ve maliyet panelleri
- filtre, dışa aktarma ve audit log

Kabul: tüm metrikler sunucu olaylarından yeniden hesaplanabilir ve özel içerik sızdırmaz.

## Faz 10 — Dağıtım ve sertleştirme

- Render IaC ve health/zero-downtime stratejisi
- Supabase migration/seed politikası
- imzalı AAB, internal track ve APK artifact
- unit/integration/UI/contract/load/security testleri
- Crash/ANR, baseline profile, R8 ve release runbook

Kabul: sürüm adayı otomatik pipeline ile üretilir, rollback ve veri kurtarma denenmiştir.
