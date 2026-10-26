# Mimari

HavaCan iki Gradle modülü ve dört katmandan oluşur:

| Modül | İçinde | Bağımlı olabildikleri |
|---|---|---|
| `:core:domain` (saf Kotlin/JVM) | `model/` · `domain/` | Kotlin stdlib, `java.time`, Gson etiketi |
| `:app` (Android) | `data/` · `di/` · `ui/` | `:core:domain`, Android, Compose, Room, WorkManager, Retrofit |

```
ui (Route → Screen)  →  ViewModel (StateFlow)  →  Repository (arayüz)  →  Retrofit · Room · dosya · SharedPreferences
        │                      │
        └──── domain kuralları (saf Kotlin, birim testli) ◄────┘
```

## Kurallar

- **UI internete gitmez.** Composable yalnızca aldığı state'i çizer ve olayları yukarı bildirir.
- **ViewModel çizim yapmaz.** Renk ya da Composable bilmez; state üretir, hatayı insan diline çevirir.
- **Repository ekranı bilmez.** Veriyi getirir ve saklar; hatayı sarmalamadan fırlatır.
- **Kurallar `domain`'dedir ve saftır.** Hava riski, koşu puanı, kombin seçimi, buluşma önerisi ve sesli komutlar Android'e, Room'a ya da Compose'a dokunmaz. Bu kural ayrı modül sayesinde **derleyici tarafından** zorlanır: `:core:domain` içinde `import android…` derlenmez.
- **Tek kurulum noktası.** Repository'leri, zamanlayıcıyı ve backend istemcilerini yalnızca `di/AppContainer` oluşturur; ViewModel'ler ve WorkManager işçileri hepsini oradan alır.
- **Global değişken durum yoktur.** Sıcaklık birimi ve ekran tercihleri `ProfileStore.prefs` akışından gelir; ekranlara `LocalDisplaySettings` ile, kurallara parametre olarak ulaşır.
- **Veri yoksa uydurulmaz.** Tahmin gelmediyse puan ya da öneri gösterilmez; durum açıkça yazılır.

## Temel repodan farklar ve gerekçeleri

Kamp+ temel reposu (`tc-yusufburakdemir/weather-app`) bilerek küçük tutulmuş: tek modül, üç katman, DI yok, Room yok. HavaCan'ın kapsamı çok daha geniş olduğu için bazı yapılar **ihtiyaç doğduğu için** eklendi:

| Konu | Temel repo | HavaCan | Neden |
|---|---|---|---|
| Modüller | Tek `:app` | `:app` + `:core:domain` | 7 kural motoru var; saflıklarını derleyici korusun ([ADR-0005](adr/0005-core-domain-module.md)) |
| Katmanlar | `model` · `data` · `ui` | `model` · `domain` · `data` · `ui` (+ `di`) | Kurallar ekranlardan ve veriden bağımsız test edilmeli |
| Bağımlılık enjeksiyonu | Yok (varsayılan parametre) | Elle DI: `AppContainer` | Ekranlar ve arka plan işçileri aynı depoyu, aynı önbelleği paylaşmalı ([ADR-0004](adr/0004-manual-di.md)) |
| Kalıcılık | Yok | JSON dosya (planlar), Room (dolap), SharedPreferences (ayarlar) | Plan, kıyafet fotoğrafı ve tercih kapanınca kaybolmamalı ([ADR-0001](adr/0001-room-for-wardrobe-only.md)) |
| Arka plan işi | Yok | WorkManager | Hatırlatma ve hava takibi uygulama kapalıyken çalışmalı ([ADR-0002](adr/0002-workmanager.md)) |
| Backend | Yok | Vercel'de küçük bir API | OpenRouter anahtarı APK'ya girmemeli ([ADR-0003](adr/0003-backend-for-secrets.md)) |
| Konum arama | Open-Meteo Geocoding | Telefonda 81 il + 973 ilçe, gerekirse Open-Meteo | İnternetsiz ve doğru Türkçe yazımla arama ([ADR-0006](adr/0006-local-place-search.md)) |
| Ortak durum | `LoadState<T>` | Aynı `LoadState<T>` + `ErrorReason` | Aynı dört durum her ekranda |
| Navigasyon | Navigation Compose | Navigation Compose | Aynı ([ADR-0007](adr/0007-navigation.md)) |
| Kalite kapısı | CI: spotless + test + build | Aynısı + backend sözdizimi kontrolü | |

Eklenmeyenler bilerek eklenmedi: Hilt (tek bir `AppContainer` yetiyor), UseCase katmanı (kurallar zaten `domain` nesneleri), `Result` sarmalayıcı zinciri (hata ViewModel'de bir kez çevriliyor).

## Paketler

| Paket | Sorumluluk |
|---|---|
| `model` | Saf veri tipleri: `City`, `Forecast`, `HourlyWeather`, `Plan`, `ClothingItem`, `MeetPerson`, `TemperatureUnit`… |
| `domain/plans` | `PlanAdvisor`: plan için hava riski ve ±4 saatlik Plan B |
| `domain/running` | `RunningSuitabilityCalculator` (saatlik 0–100), `RunSlotPlanner` (Koşu Planla saatleri) |
| `domain/meet` | `MeetPlanner`: grup için en uygun saat ve yer |
| `domain/wardrobe` | `OutfitEngine` (yalnızca gerçek kıyafetlerden kombin), `ClothingColors`, `ColorDetector` |
| `domain/insight` | Ana sayfa ipuçları için `WeatherInsight` |
| `domain/voice` | `VoiceCommandParser`, `HavaCanAssistant` (sohbet ve sesli komutlar) |
| `data` | Repository arayüzleri ve uygulamaları, Retrofit API'leri, `FilePlanStore`, `ProfileStore`, takvim, konum |
| `data/wardrobe` | Room (`ClothingEntity`, `SavedOutfitEntity`, DAO), fotoğraf deposu, yapay zekâ analiz istemcisi |
| `di` | `AppContainer`: uygulamadaki tek kurulum noktası |
| `ui/navigation` | Rotalar (`HavaRoutes`) |
| `ui/common` | `LoadState`, `ErrorReason`, gökyüzü başlığı, gün şeridi |
| `ui/theme` | Renkler, açık/koyu palet (`Hava`), `DisplaySettings` |
| `ui/<özellik>` | Her özellik için Route, Screen, ViewModel |

## Veri akışı

```
Open-Meteo → Retrofit → OpenMeteoWeatherRepository (10 dk bellek önbelleği)
          → ViewModel (try/catch → LoadState) → Route (collectAsStateWithLifecycle)
          → Screen (durumsuz) → Composable
```

Örnek, ana sayfa:

1. `HomeViewModel`, seçili şehri `LocationStore`'dan okur ve `WeatherRepository.forecast()` çağırır.
2. Çağrıdan önce durum `LoadState.Loading` olur; sahnede yükleniyor görünümü çıkar.
3. Cevap gelirse `LoadState.Success(forecast)`, veri yoksa `Empty`, hata olursa `Error(reason)` olur.
4. Aynı tahminden `domain` kuralları ipucu (`WeatherInsight`) ve koşu penceresi (`RunningSuitabilityCalculator`) üretir.
5. `HavaApp` durumu toplar; ekran yeniden çizilir. Gökyüzü başlığının rengi de aynı tahminden gelir.

Planlar için akış aynıdır; kaynak `FilePlanStore`'dur ve WorkManager işçileri aynı depoyu `AppContainer` üzerinden okur.

## Hata yönetimi

Repository hatayı olduğu gibi fırlatır; ViewModel yakalar ve `ErrorReason`'a çevirir (`Throwable.toErrorReason()`). Ekran nedeni kullanıcıya Türkçe söyler.

| Ne oldu | Kotlin'de | `ErrorReason` | Kullanıcıya |
|---|---|---|---|
| İnternet yok, zaman aşımı | `IOException` | `NoConnection` | "İnternet bağlantını kontrol edip tekrar dene." |
| Sunucu 4xx/5xx | `retrofit2.HttpException` | `Server` | "Hava servisi şu an yanıt vermiyor. Biraz sonra tekrar dene." |
| Beklenmeyen JSON | `JsonParseException` | `Unknown` | "Beklenmeyen bir yanıt geldi. Tekrar dene." |
| Yapay zekâ backend'ine ulaşılamadı | `IOException` | — | Kurala dayalı yanıt ya da "bilgileri kendin gir"; uygulama çalışmaya devam eder |

## Dört durum

`LoadState` sealed olduğu için `when` dallarından biri unutulursa derleme hatası alınır.

| Durum | Ana sayfa | Koşu Planla |
|---|---|---|
| `Loading` | İskelet görünüm | Saat listesi yerine yükleniyor |
| `Success` | Sahne, tahmin kartı | Önerilen saatler |
| `Empty` | "Bu konum için tahmin bulunamadı." | "Hava tahmini alınamadı" kartı |
| `Error` | Neden + "Tekrar dene" | Neden + "Tekrar dene"; saat elle seçilebilir |

Konum seç kendi `SearchState`'ini kullanır, çünkü fazladan bir "boşta" durumu (son konumlar ve 81 il) ve sonuçların internetten mi telefondan mı geldiği bilgisi var; hata nedeni yine ortak `ErrorReason`'dır. Weather Meet birden çok konumun tahminini aynı anda yükler; tahmini alınamayan yerleri adıyla listeler.

## Route / Screen ayrımı

- **`XRoute`** ViewModel'i edinir, durumu toplar, izinleri ve sistem ekranlarını (galeri, kamera, paylaşım, takvim) yönetir.
- **`XScreen`** ViewModel tanımaz; değerleri çizer, olayları bir `XActions` nesnesiyle yukarı bildirir.

Planlar, Dolabım, Profil, Koşu Planla, Weather Meet ve Konum seç bu kalıptadır. Ana sayfa ile sohbetin ViewModel'leri kabukta (`HavaApp`) durur, çünkü gökyüzü rengi ve sesli rehber bütün sekmelerde kullanılır; ekranları yine durumsuzdur.

## Navigasyon

`HavaApp` içindeki tek `NavHost` ([ADR-0007](adr/0007-navigation.md)):

| Rota | Ekran |
|---|---|
| `weather` (başlangıç) | Ana sayfa |
| `plans`, `havacan`, `wardrobe`, `profile` | Alt menü sekmeleri; `saveState`/`restoreState` ile durumlarını korurlar |
| `city` | Konum seç; seçilen yer, onu açan ekrana geri verilir |
| `run?planId={planId}&start={start}` | Koşu Planla; argümanlar isteğe bağlı sorgu parametreleridir ve ViewModel bunları `SavedStateHandle`'dan okur |

Sesli komutla gezinme (`VoiceDestination`) ve bildirimden plan açma aynı rotaları kullanır. Planlar ve Dolabım'ın alt ekranları (düzenleyici, Plan B, kombin) özelliğin kendi içinde, kaydedilebilir bir hedef durumuyla yönetilir.

## Testler

- `:core:domain`: 80 birim testi (risk, koşu puanı, Koşu Planla saatleri, buluşma önerisi, kombin, sesli komutlar).
- `:app`: 78 birim testi (ViewModel'ler, repository'ler, JSON eşleme, dosya deposu, rotalar) ve cihazda çalışan Room/DAO testi (`androidTest`).
- Ortak yardımcılar `testing/` altında: `MainDispatcherRule`, `FakeLocationStore`, `RecordingScheduler`. Ağ, sahte `ForecastApi`/`CityApi` ile taklit edilir.

```bash
cd Weather && ./gradlew spotlessCheck :core:domain:test testDebugUnitTest assembleDebug
```

## Genişletme noktaları

| Senaryo | Eklenir | Değişir | Dokunulmaz |
|---|---|---|---|
| Yeni hava değişkeni (ör. polen) | `HourlyWeather` alanı (varsayılanlı), DTO alanı | `ForecastApi` alan listesi, eşleyici, ekran | Kurallar, diğer ekranlar |
| Yeni plan riski (ör. sis) | `RiskReason` sabiti | `PlanAdvisor.risk` + testi | Planlar ekranı, işçiler |
| Farklı hava kaynağı | Yeni `WeatherRepository` uygulaması | `AppContainer` içindeki tek satır | ViewModel'ler, ekranlar |
| Yeni özellik ekranı | `ui/<özellik>` Route + Screen + ViewModel, `HavaRoutes` rotası | `NavHost` | Diğer özellikler |
| Yeni kural | `domain/<alan>` nesnesi + testi | Onu kullanan ViewModel | Veri katmanı |
| Yapay zekâ modeli değişimi | — | Backend'deki `MODEL` değişkeni | Uygulama |

## Karar kayıtları

[`docs/adr/`](adr/) altında: Room yalnızca dolap için, WorkManager, anahtarlar için backend, elle DI, `:core:domain` modülü, telefonda yer arama, Navigation Compose.
