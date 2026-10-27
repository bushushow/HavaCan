# HavaCan: devir notu (yeni Claude oturumu için)

Bu dosya, işi yeni bir oturumda (başka bir Claude hesabında) devralacak asistan için yazıldı. Önce baştan sona okuyun, sonra kullanıcıya ne yapacağınızı iki üç cümleyle söyleyip başlayın.

**Sıradaki iş, bu sırayla:** (1) **Weather Meet** özelliği (bölüm 7), (2) **mimari düzenlemeler** (bölüm 8).

## 1. Proje

- **HavaCan**: Turkcell ve GSB **Kamp+** yarışması için, hava durumuna göre günlük hayatı planlamaya yardım eden, oyunlaştırılmış bir Android uygulaması. Maskot HavaCan: sarı, parlak, 3D görünümlü top karakter (Canvas ile çiziliyor).
- Yarışmada **canlı demo** yapılacak. Sahte buton, sabit/uydurma veri, kaybolan kayıt olmamalı.
- Ön elemede jüri **ajanlarla** ilk 10 projeyi seçecek; **mimari** önemli bir ölçüt. Herkese aynı temel repo verildi (bölüm 8); bizim mimarimiz onunla karşılaştırılacak.
- Kullanıcı Türkçe konuşuyor. Yanıtlar Türkçe, kısa ve net.

## 2. Çalışma kuralları (kullanıcının açık talepleri)

1. **Onay olmadan `git push` yapma.** Kullanıcı açıkça "commit et" demedikçe commit de etme.
2. **API anahtarlarını koda, APK'ya veya Git geçmişine koyma.** OpenRouter anahtarı yalnızca backend'de (Vercel Secret) durur. Anahtarı kullanıcı terminale kendisi girer; sen girme.
3. **Gerçek olmayan veriyi gerçek gibi gösterme.** Veri yoksa skor/tahmin uydurma; durumu açıkça yaz.
4. **Test etmediğin şeyin çalıştığını söyleme.** Neyi doğruladığını, neyi doğrulamadığını ayrı ayrı yaz.
5. **Mevcut çalışmaları izinsiz silme**, istenmeyen ekranları yeniden tasarlama.
6. **Cihaz/emülatör testlerini kullanıcı yapıyor.** adb ile emülatörü tıklatma. Derle, birim testlerini çalıştır, kullanıcıya kısa bir cihaz test listesi ver.
7. **Hızlı ve küçük adımlarla ilerle.** Kullanıcı uzun süren turlardan rahatsız ("bir ekran 27 dk sürdü"). Önce görünen parçayı bitir ve göster, sonra kalanına geç; başlarken kaba bir süre söyle. Gereksiz keşif ve uzun açıklama yapma.
8. "Turkcell benzeri" renkler kullanılır ama **Turkcell logosu kullanılmaz**; marka logoları (ör. Google Takvim logosu) çizilmez.
9. Kullanıcı tasarımı referans görsellerle veriyor; görseldeki sayılar/isimler örnektir, sabit kodlanmaz (ör. "Merhaba Büşra" değil, kullanıcının girdiği ad).

## 3. Konumlar ve araçlar

- Git deposu: `C:\Users\bsroz\turkcell_camp` (remote `https://github.com/bushushow/HavaCan.git`, dal `main`).
- Android projesi: `C:\Users\bsroz\turkcell_camp\Weather`. Kod paketi **`com.kampplus.havacan`** (`Weather/app/src/main/java/com/kampplus/havacan`). `applicationId` bilerek hâlâ `com.example.weather`: emülatördeki demo verileri kaybolmasın diye; demodan sonra değiştirilebilir (`app/build.gradle.kts` içinde not var).
- Backend: `backend/havacan-api` (mantık `src/app.js`; Vercel girişi `api/index.js`, Cloudflare girişi `src/index.js`).
- Temel (referans) repo: `C:\Users\bsroz\StudioProjects\weather-app` (github.com/tc-yusufburakdemir/weather-app).
- Belgeler: `docs/` (`kamp/uygulama-2..4` kanıtları, `tasarim/`, `data-sources.md`, `havacan-assets.md`).
- Araçlar: AGP 9.4.1 (yerleşik Kotlin), Kotlin 2.2.10, Gradle 9.6, JDK = Android Studio `jbr`, compileSdk/targetSdk 37, minSdk 24, Compose BOM 2026.02.01, Material3, Room 2.8.4 + KSP, WorkManager 2.11.2, Retrofit 3 + Gson, desugaring (java.time), Spotless 7.2.1 + ktlint 1.7.1 (temel repoyla aynı `.editorconfig`).
- Derleme, biçim ve test (Git Bash). Gradle yeni bir şey indirecekse komut ortamın sandbox'ı dışında çalıştırılmalı (`dangerouslyDisableSandbox`), yoksa ağ engellenir:

```bash
cd /c/Users/bsroz/turkcell_camp/Weather && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat spotlessApply spotlessCheck testDebugUnitTest assembleDebug --console=plain -q
```

- `spotlessApply` ile `spotlessCheck` aynı komutta bazen ilk seferde "BUILD FAILED" verip ikinci çalıştırmada geçiyor; hata mesajı yoksa bir kez daha çalıştır.
- Son durumda **146 birim testi** geçiyor.
- Emülatör: `Medium_Phone_API_37.0`; kullanıcı Android Studio'daki Run ile kuruyor. Emülatörün varsayılan konumu Mountain View (ABD): "Mevcut konumumu kullan"ı denemek için Extended controls → Location'dan Türkiye'de bir nokta seçilmeli.
- Uygulama dili `MainActivity.attachBaseContext` içinde Türkçeye sabit.
- `strings.xml` içinde kesme işareti `\'` ile kaçırılır. **Bash heredoc'ları Türkçe/tırnaklı uzun Python betiklerinde bozuluyor** (sık yaşandı): betiği önce Write ile scratchpad'e dosya olarak yaz, sonra `python -I dosya.py` ile çalıştır.
- ktlint `android_studio` stili sondaki virgülleri kaldırıyor; Edit yaparken dosyanın biçimlendirilmiş halini oku.

## 4. Mimari (bugünkü hali)

Ayrıntı ve gerekçeler: [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) ve [`docs/adr/`](adr/).

- İki modül: `:core:domain` (saf Kotlin: `model/`, `domain/`) ve `:app` (`data/`, `di/`, `ui/`). `domain` içinde Android/Room/Compose importu derlenmez.
- Tek kurulum noktası `di/AppContainer`; repository'ler arayüz arkasında (`WeatherRepository`, `CityRepository`, `WardrobeRepository`, `PlanRepository`).
- Ortak `LoadState<T>` ve `ErrorReason` (`ui/common`); sıcaklık birimi ve ekran tercihleri `ProfileStore.prefs` → `LocalDisplaySettings` (global durum yok).
- Navigation Compose: `ui/navigation/HavaRoutes` (sekmeler, `city`, `run?planId&start`).
- Kalite kapısı: `.github/workflows/android.yml` (spotless, testler, derleme).

## 5. Biten özellikler

| Özellik | Durum | Not |
|---|---|---|
| Kamp+ Uygulama 2, 3, 4 | Bitti, etiketli, push edildi | `docs/kamp/` |
| Planlarım, Yeni plan, Plan B, takvim, bildirim | Bitti, push edildi | |
| Dolabım (Salla Kombin) | Bitti, push edildi | Galeri/kamera, Room, renk bulma, sallama |
| Koşu Planla | Bitti, push edildi | `ui/run/` |
| HavaCan Asistan (sohbet) + Sesli Rehber + OpenRouter araçları | Yerelde commit, **push edilmedi**; kullanıcı denedi | Aşağıda |
| Profilim ve alt ekranlar | Yerelde commit, push edilmedi | `ui/profile/` |
| Konum seç + il/ilçe ekranı | Yerelde commit, push edilmedi | `ui/city/LocationPicker*` |
| Ana sayfa yenilemesi + paket adı + ktlint | Yerelde commit, push edilmedi | Aşağıda |

### Ana sayfa (son çalışma)

- HavaCan sahnesi 210 dp (önce 330); büyük sıcaklık 68 sp; ↑ en yüksek / ↓ en düşük seçili günün değeri.
- **Tahmin kartı** `ui/home/components/ForecastCard.kt`: başlıkta gün/saat, durum hapı ve **takvim düğmesi** (yalnızca tahmini olan günler seçilebilir); yağış/nem/rüzgar küçük hapları; kayan sekme seçici (Sıcaklık/Yağış/Rüzgar); kendini çizen yumuşak grafik, seçili saatte değer balonu, altında saat ikonları; 16 güne kadar gün şeridi (seçili gün mavi kart, en düşük–en yüksek aralık çubuğu). Grafiğe ya da güne dokunmak üstteki sıcaklığı ve sahneyi o saate taşır.
- **Koşucu Modu**: önce "koşu için en iyi saat" kartı (`RunnerInsightCard`), altında aynı tahmin kartı ve ilk sekme "Koşu" (saatlik 0–100 puan).
- Ayrı ipucu kartı kaldırıldı: HavaCan'a dokununca **gerçek veriden ipuçları** sırayla konuşma balonunda (`ui/home/HavaCanTips.kt`); yağmur geliyorsa kendisi söyler.
- Salla Kombin ve koşu kartı inceltildi. Kullanıcı genel görünümü beğendi; son iki tur cihazda henüz görülmedi.

### Sohbet ve sesli rehber

- Sohbet `ui/chat/`; beyin `domain/voice/` (kurala dayalı Türkçe komutlar + gerçek veriden yanıtlar). Kuralların anlamadığı cümleler `/v1/assistant`'a gider; model yalnızca sabit araçlardan birini seçebilir (ekran açma, hava, planlar, kombin, koşu saati, plan oluştur/değiştir/sil); yazan işlemler okunur ve "evet" beklenir.
- Oturum `ui/voice/VoiceGuideController.kt`: kullanıcı başlatır, dönüşümlü dinler; TTS bitmeden mikrofon açılmaz; TalkBack açıkken yanıtları TalkBack okur.

### Profil ve konum

- Profil: ad, °C/°F (tüm sıcaklıklar), aktivite/kombin tercihleri (henüz önerileri değiştirmiyor; ekranda böyle yazıyor), bildirim tercihleri, erişilebilirlik (büyük yazı, yüksek kontrast, animasyon azaltma), gizlilik ve onaylı veri silme. "Google Takvim" kartı OAuth değil, telefon takvimi izni.
- Konum seç: internetsiz arama, son konumlar gerçek seçimlerden, "Mevcut konumumu kullan" `LocationManager` + en yakın gerçek ilçe; bulunamazsa uydurmaz.

## 6. Bilinen eksikler

- Backend Vercel'de; Cloudflare Worker da var ama kullanıcının ağı `*.workers.dev`'i engelliyor (tarayıcılar ECH ile açıyor, uygulama açamıyor), kullanılmıyor. Backend değişince: `cd backend/havacan-api && npx vercel --prod` (kullanıcı hesabı bağlı; GitHub otomatik deploy yok).
- WorkManager hatırlatmaları birkaç dakika gecikebilir. Tahmin önbelleği yalnızca bellekte. Sohbet geçmişi yalnızca bellekte.
- Gerçek 3D asset ve uygulama simgesi yok (`docs/havacan-assets.md`).
- CI (GitHub Actions), PR şablonu ve `docs/ARCHITECTURE.md` henüz yok (bölüm 8).
- Kullanılmayan eski kod: `RunnerHourlyStrip`, `HourlyStrip`, `WeatherInsightCard`, `data/Countries.kt`, `regionColor`; mimari turunda temizlenebilir.

## 7. Sıradaki iş 1: Weather Meet (grup buluşması)

Kullanıcı onayladı ("önce bu özelliği yaparız"). Referans görsel gelirse ona göre, gelmezse mevcut tasarım diliyle. Önce kısa plan söyle, sonra yap; ilk görünen ekranı hızlıca teslim et.

Önerilen ve kullanıcıya anlatılan kapsam:

1. **Oluştur**: Planlar'dan "Weather Meet". Başlık, katılımcılar (ad + il/ilçe; mevcut konum seçici `CityRoute` ile), aday günler/saat aralığı (tahmin 16 gün).
2. **Öneri**: Her katılımcının konumu için gerçek Open-Meteo tahmini; aday saatler `PlanAdvisor` risk mantığı (ve gerekirse koşu puanı benzeri bir konfor puanı) ile puanlanır. Sonuç: "Herkes için en uygun: Cumartesi 14.00, Kadıköy · yağmur yok, 19°", katılımcı bazında kısa hava özeti, bir Plan B saati. Buluşma yeri adayları: katılımcıların il/ilçeleri (veya kullanıcının seçtiği bir yer).
3. **Kaydet ve takip et**: Normal `Plan` olarak Planlarım'a (hava takibi, hatırlatma, Plan B aynen çalışır). Katılımcı listesi plana eklenebilir (`Plan` modeline varsayılanlı yeni alan; eski JSON'lar bozulmamalı).
4. **Paylaş**: Android paylaşım ekranıyla hazır davet metni (WhatsApp vb.); istenirse telefon takvimine ekleme (`CalendarGateway.insertIntent`).
5. **Bilerek yok**: Katılımcıların uygulama içinde canlı oy vermesi (hesap + sunucu gerekir); sahte çalışıyormuş gibi gösterilmez.

Saf mantığı `domain/` altına koy ve birim testi yaz; ViewModel `AppContainer`'dan bağımlılık alsın; yeni bağımsız veri sistemi kurma.

## 8. Mimari düzenlemeler (yapıldı)

`arch/refactor` dalında, `pre-architecture-final` etiketinden sonra yapıldı: CI ve commit kuralları, `AppContainer`, repository arayüzleri, Room'dan ayrılmış saf kurallar, global ayarların kaldırılması, ortak `LoadState`, `:core:domain` modülü, Navigation Compose, `ARCHITECTURE.md` ve ADR'ler. Her adım ayrı commit; testler ve derleme her adımda yeşil.

## 9. Tasarım dili

- Renkler (`ui/theme/Color.kt`): Ivory `#FBF7EE`, Navy `#0B1B3F`, HavaBlue `#1D63F0`, Sun `#FFC72C`, GoodGreen, RiskRed. Yazı tipi Nunito.
- Beyaz yuvarlak kartlar (`SoftCard`), yumuşak gölge, yaylı mikro animasyonlar; sevimli ama profesyonel; ekran sade, HavaCan küçük ama etkileşimli.
- Erişilebilirlik: anlamlı `contentDescription`, 48 dp+ dokunma alanı, yalnız renkle bilgi verme, azaltılmış hareket ayarına uyma, büyük yazıda taşmama.

## 10. Yeni oturumda ilk adımlar

1. `git status` ve `git log --oneline -5` ile durumu gör (her şey commit edilmiş olmalı; `main`, `origin/main`'den birkaç commit önde, push edilmedi).
2. Kullanıcıya Weather Meet için kısa planı ve süreyi söyle; referans görseli var mı sor (yoksa mevcut dille tasarla).
3. Weather Meet bitince bölüm 8'e geç.
