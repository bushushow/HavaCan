# Kamp+ Uygulama 4 · İnternete bağlan

Git tag: `kamp-uygulama-4` · Emülatör: Medium Phone API 37.0 · API: Open-Meteo Geocoding (`https://geocoding-api.open-meteo.com/v1/search`)

| Madde | Durum | Kanıt |
|---|---|---|
| INTERNET izni Manifest'te | Tamam | [AndroidManifest.xml](../../../Weather/app/src/main/AndroidManifest.xml) |
| Retrofit + Gson bağımlılıkları, Gradle Sync | Tamam | Retrofit 3.0.0 + converter-gson ([libs.versions.toml](../../../Weather/gradle/libs.versions.toml)); sync ve derleme başarılı |
| `City` ve `CitySearchResponse` data class'ları | Tamam | [City.kt](../../../Weather/app/src/main/java/com/example/weather/model/City.kt), [CitySearchResponse.kt](../../../Weather/app/src/main/java/com/example/weather/model/CitySearchResponse.kt); gerçek API cevaplarıyla parse testi |
| `CityApi` arayüzü ve `ApiClient` | Tamam | [CityApi.kt](../../../Weather/app/src/main/java/com/example/weather/data/remote/CityApi.kt), [ApiClient.kt](../../../Weather/app/src/main/java/com/example/weather/data/remote/ApiClient.kt) |
| `CityViewModel`: Loading → Success / Empty / Error | Tamam | [CityViewModel.kt](../../../Weather/app/src/main/java/com/example/weather/ui/city/CityViewModel.kt), 8 unit test |
| Ekranda dört durum (uçak modu ile Error) | Tamam | [02-loading.png](02-loading.png), [03-success-izmir.png](03-success-izmir.png), [04-empty.png](04-empty.png), [05-error-ucak-modu.png](05-error-ucak-modu.png) |
| Hata ekranındaki Tekrar dene çalışıyor | Tamam | Uçak modu kapatılıp "Tekrar dene"ye basıldı: [06-tekrar-dene-success.png](06-tekrar-dene-success.png); log: [07-okhttp-log.txt](07-okhttp-log.txt) |

## Bonuslar

| Bonus | Durum | Kanıt |
|---|---|---|
| OkHttp log'u | Tamam | Debug build'de `HttpLoggingInterceptor` (BASIC): [07-okhttp-log.txt](07-okhttp-log.txt) |
| Klavyedeki "ara" tuşu (`ImeAction.Search`) | Tamam | Arama yazarken otomatik çalışıyor; "ara" tuşu klavyeyi kapatıyor |
| Araya `CityRepository` ekle | Tamam | [CityRepository.kt](../../../Weather/app/src/main/java/com/example/weather/data/CityRepository.kt): yalnızca yerleşim yerleri, Türkiye sonuçları önce |
| (Uygulama 3 bonusu) State'i `CityViewModel`'e taşı | Tamam | Arama ve seçili şehir `SavedStateHandle` ile döndürmede ve süreç ölümünde korunuyor |

## Mühendislik notları

- **Boş cevap tuzağı:** Sonuç yoksa API `results` alanını hiç göndermiyor. Alan nullable; aksi halde Gson null'ı Kotlin'in non-null listesine yazar ve uygulama çöker. Testli.
- **Yarış durumu yok:** Yazarken 400 ms debounce, ardından `flatMapLatest` eski isteği iptal ediyor. Log'da her arama için tek istek var.
- **İptal güvenliği:** Yalnızca `IOException`, `HttpException`, `JsonParseException` yakalanıyor; `CancellationException` yutulmuyor.
- **1 harflik arama** internete gitmeden yerel popüler şehirlerde filtreleniyor (API tek harfte boş dönüyor).
- Testler: 21 unit test + 7 Compose UI testi (emülatörde) geçiyor.
