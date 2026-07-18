# Mobil Arayüz Ölçü Sözleşmesi

Referans ekranlar `728 × 1536 px` portre tuvalinde hazırlanmıştır. Android uygulaması piksel
değerlerini sabitlemez; Compose yoğunluk bağımsız ölçüler ve aşağıdaki oran sözleşmesiyle çalışır.

## Ana ölçüler

| Referans | Compose ölçüsü | Kural |
|---|---:|---|
| 728 px ekran genişliği | 390 dp tasarım genişliği | oran `1.8667 px/dp` |
| 30 px dış boşluk | 16 dp | tüm ekranlarda ortak |
| 26–30 px kart köşesi | 14–16 dp | içerik türüne göre |
| 96–104 px kontrol | 52–56 dp | dokunma hedefi en az 48 dp |
| 138 px alt menü | 72–74 dp | sistem inset'i hariç |
| 20–24 px kart aralığı | 10–12 dp | dikey ritim |

Telefon genişliği `W` dp olduğunda içerik genişliği:

```text
contentWidth = min(W - 32 dp, 520 dp)
```

Bu nedenle küçük telefonlarda yatay taşma oluşmaz, tabletlerde ise kartlar gereksiz biçimde
genişleyip oranını kaybetmez. Kaydırılabilir ekranlarda alt içerik güvenli boşluğu alt menüden
ayrıdır. Başlık, kart ve kontrol ölçüleri ekran bazında yeniden tanımlanmaz; tasarım sistemi
değerleri kullanılır.

## İsimlendirme

Kullanıcı arayüzündeki öğrenme birimi **kart** olarak adlandırılır. Veritabanı ve API'de geriye
dönük teknik tablo kavramı yalnızca SQL tablosu anlamında kullanılabilir; kullanıcı metinlerinde
“öğretim tablosu” ifadesi kullanılmaz.
