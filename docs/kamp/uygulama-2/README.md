# Kamp+ Uygulama 2 · İlk ekranın

Git tag: `kamp-uygulama-2` · Emülatör: Medium Phone API 37.0

| Madde | Durum | Kanıt |
|---|---|---|
| "Hava" adında Empty Activity projesi | Tamam | Empty Activity şablonu; `app_name = "Hava"` ([strings.xml](../../../Weather/app/src/main/res/values/strings.xml)) |
| Emülatörde "Merhaba Kamp!" ile açıldı | Tamam | [01-merhaba-kamp.png](01-merhaba-kamp.png) |
| Manifest / strings.xml'de uygulama adı, ana ekranda görünüyor | Tamam | APK etiketi `Hava` (aapt2), TopAppBar başlığı: [02-sehir-listesi.png](02-sehir-listesi.png) |
| `CityCard` composable + `@Preview` | Tamam | [CityCard.kt](../../../Weather/app/src/main/java/com/example/weather/ui/city/CityCard.kt): açık ve karanlık önizleme |
| En az 10 şehirlik sabit liste, `LazyColumn` | Tamam (15 şehir) | [02-sehir-listesi.png](02-sehir-listesi.png), [04-liste-sonu.png](04-liste-sonu.png) |
| Karta dokununca Logcat'e `Tıklandı: Ankara` | Tamam | [03-logcat.txt](03-logcat.txt) |

## Bonuslar

| Bonus | Durum | Kanıt |
|---|---|---|
| Karanlık tema | Tamam | [05-karanlik-tema.png](05-karanlik-tema.png) |
| Plaka rozetine renk | Tamam (coğrafi bölgeye göre) | [Color.kt](../../../Weather/app/src/main/java/com/example/weather/ui/theme/Color.kt) |
| TopAppBar ile başlık | Tamam | [02-sehir-listesi.png](02-sehir-listesi.png) |
| Kendi ikonun | Yapılmadı | Tasarım aşamasına bırakıldı |

## Tasarım notları

- `City` modeli Open-Meteo Geocoding API alanlarıyla kuruldu; sabit listedeki id ve koordinatlar API'den alındı. Uygulama 4'te aynı model kullanılacak.
- 81 ilin plaka ve bölge tablosu [TurkishProvinces.kt](../../../Weather/app/src/main/java/com/example/weather/data/TurkishProvinces.kt) içinde. API bazı illeri "Bursa İli" gibi döndürüyor; arama bunu tolere ediyor. Unit testlerle doğrulandı.
