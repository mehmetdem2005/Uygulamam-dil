# Render dağıtımı

Gerçek Render servisleri Frankfurt bölgesinde ücretsiz instance olarak oluşturulmuştur:

- `uygulamam-dil-api`: Ktor API ve DeepSeek sağlayıcı sınırı
- `uygulamam-dil-edge-tts`: Edge TTS ses servisi

Depo kökündeki `render.yaml` production Blueprint kaynağıdır. Secret değerleri Git'e
yazılmaz. `DEEPSEEK_API_KEY`, `SUPABASE_URL` ve `SUPABASE_SERVICE_ROLE_KEY` Render
Dashboard üzerinden tanımlanır. Servisler yalnızca GitHub CI kontrolleri geçtikten sonra
otomatik dağıtılacak şekilde tanımlanmıştır.

Ücretsiz Render web servisleri kullanılmadığında uykuya geçebilir. Bu yüzden production
gecikme hedefleri ölçülürken ücretli, sürekli çalışan instance kullanılmalıdır.
