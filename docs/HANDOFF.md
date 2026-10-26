# HavaCan: devir notu (yeni Claude oturumu için)

Bu dosya, işi yeni bir oturumda devralacak asistan için yazıldı. Önce baştan sona okuyun, sonra kullanıcıya ne yapacağınızı kısaca söyleyip başlayın.

## 1. Proje

- **HavaCan**: Turkcell ve GSB **Kamp+** programı için geliştirilen, hava durumuna göre günlük hayatı planlamaya yardım eden, oyunlaştırılmış bir Android uygulaması. Maskotu HavaCan: sarı, parlak, 3D görünümlü bir top karakter.
- Yarışmada **canlı demo** yapılacak. Sahte buton, sabit/uydurma veri, kaybolan kayıt olmamalı.
- Kullanıcı Türkçe konuşuyor. Yanıtlar Türkçe, kısa ve net olmalı.

## 2. Çalışma kuralları (kullanıcının açık talepleri)

1. **Onay olmadan `git push` yapma.** Commit'i yerelde at; kullanıcı açıkça "commit et" demediyse commit de etme. Son talepte "kendiliğinden commit/push yapma" dendi.
2. **API anahtarlarını koda, APK'ya veya Git geçmişine koyma.** OpenRouter anahtarı yalnızca backend'de (Cloudflare Worker secret) durur.
3. **Gerçek olmayan veriyi gerçek gibi gösterme.** Hava verisi yoksa skor/tahmin uydurma; durumu açıkça yaz.
4. **Test etmediğin şeyin çalıştığını söyleme.** Neyi doğruladığını, neyi doğrulamadığını ayrı ayrı yaz.
5. **Mevcut çalışmaları izinsiz silme**, istenmeyen ekranları yeniden tasarlama.
6. **Cihaz/emülatör testlerini kullanıcı yapıyor.** adb ile emülatörü adım adım tıklatarak test etme; çok yavaş ve kullanıcı bundan şikâyet etti. Derle, birim testlerini çalıştır, kullanıcıya kısa bir test listesi ver.
7. **Hızlı ilerle.** Kullanıcı geliştirmelerin uzun sürmesinden rahatsız. Gereksiz keşif ve uzun açıklama yapma.
8. Tasarımda "Turkcell benzeri" renkler kullanılır ama **Turkcell logosu kullanılmaz**.

## 3. Konumlar ve araçlar

- Git deposu: `C:\Users\bsroz\turkcell_camp` (remote: `https://github.com/bushushow/turkcell_camp.git`, dal: `main`)
- Android projesi: `C:\Users\bsroz\turkcell_camp\Weather` (paket: `com.example.weather`)
- Backend: `backend/havacan-api` (Cloudflare Worker)
- Belgeler: `docs/` (`kamp/uygulama-2..4` kanıtları, `tasarim/` ekran görüntüleri, `data-sources.md`, `havacan-assets.md`)
- Araçlar: AGP 9.4.1 (yerleşik Kotlin), Kotlin 2.2.10, Gradle 9.6, JDK = Android Studio `jbr`, compileSdk/targetSdk 37, minSdk 24, Compose BOM 2026.02.01, Material3, Room 2.8.4 + KSP 2.3.12, WorkManager 2.11.2, Retrofit 3 + Gson, core library desugaring (java.time).
- Derleme ve test (Git Bash):

```bash
cd /c/Users/bsroz/turkcell_camp/Weather && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat testDebugUnitTest assembleDebug --console=plain -q
```

- Emülatör: `Medium_Phone_API_37.0`. Kullanıcı uygulamayı Android Studio'daki yeşil Run (↻) düğmesiyle kuruyor. Emülatör uzun süre açık kalınca "isn't responding" uyarısı verebiliyor; Cold Boot çözüyor.
- Uygulama dili `MainActivity.attachBaseContext` içinde Türkçeye sabitlenmiş.
- `strings.xml` içinde kesme işareti `\'` ile kaçırılmalı (ör. `Planlarım\'a`). Bash heredoc'ları uzun Python betiklerinde bozulabiliyor; betiği dosyaya yazıp çalıştırmak daha güvenli.

## 4. Mimari

- MVVM; `StateFlow`; her ekranın ViewModel'i `Factory` ile `AppGraph.get(context)` üzerinden bağımlılık alır (`data/AppGraph.kt`).
- Saf alan mantığı `domain/` altında ve birim testli: `RunningSuitabilityCalculator`, `RunSlotPlanner`, `PlanAdvisor`, `OutfitEngine`, `WeatherInsight`.
- Veri: Open-Meteo Forecast API (16 gün, saatlik, `timezone=auto`) → `WeatherRepository` (10 dk bellek önbelleği). Şehir arama: Open-Meteo Geocoding + 81 il ve 973 ilçe statik verisi.
- Planlar: `FilePlanStore` (JSON dosyası, atomik yazma). Koşu planları da burada; ikinci bir plan veritabanı yok.
- Dolabım: Room (`havacan.db`), fotoğraflar `filesDir/wardrobe`.
- Bildirimler: WorkManager (`ui/plans/PlanWork.kt`): hatırlatma işi + 3 saatte bir hava takibi; bildirime dokununca Plan B açılır (`EXTRA_PLAN_ID`).
- Animasyon: tek bir sahne saati (`rememberSceneClock`) yalnızca çizim sırasında okunur; karakter `ui/home/character/` altında Canvas ile çiziliyor (`drawHavaCan`, `HavaCanPose`, koşu kıyafeti `gear`). Azaltılmış hareket ayarına uyulur (`rememberReduceMotion`).
- Uygulama kabuğu: `ui/HavaApp.kt`; alt menü: Hava, Planlar, HavaCan (orta), Dolabım, Profil. Şehir seçici ve Koşu Planla, sekmelerin üstünde tam ekran katman olarak açılır.

## 5. Biten özellikler

| Özellik | Durum | Not |
|---|---|---|
| Kamp+ Uygulama 2, 3, 4 | Bitti, etiketli (`kamp-uygulama-2/3/4`), push edildi | Kanıtlar `docs/kamp/` |
| Ana ekran: Normal ve Koşucu Modu | Bitti, push edildi | Animasyonlu ada, HavaCan, saatlik tahmin, koşu uygunluk penceresi |
| Şehir seçimi: 81 il, 973 ilçe, ülke | Bitti, push edildi | Kaynaklar `docs/data-sources.md` |
| Planlarım, Yeni plan, Plan B, takvim, bildirim | Bitti, push edildi | |
| Dolabım (Salla Kombin) | Bitti, push edildi (`dd48cb7`) | Galeri/kamera, Room, cihazda renk bulma, sallama ve butonla kombin, eksik kategori uyarısı |
| Koşu Planla | Kodlandı, push edildi (`cd538ee`); son tur cihazda görülmedi | Ayrıntılar aşağıda |
| HavaCan Asistan (sohbet) + Sesli Rehber + Profil + OpenRouter | Yerelde commit edildi, **push edilmedi**; kullanıcı cihazda denedi, çalışıyor | Ayrıntılar aşağıda |
| Profilim (ad, °C/°F, aktivite/kombin tercihleri, bildirim, erişilebilirlik, gizlilik) | Yerelde commit edildi, push edilmedi; cihazda tam denenmedi | `ui/profile/`, tercihler `data/ProfileStore.kt` (SharedPreferences); birim `model/Units.kt` tüm sıcaklık gösterimlerinde; Google Takvim kartı OAuth değil, telefon takvimi izni |
| Konum seç + il/ilçe ekranı | Yerelde commit edildi, push edilmedi | `ui/city/LocationPicker*`; arama internetsiz (`data/PlaceDirectory.kt`), yurt dışı için Open-Meteo; son konumlar gerçek seçimlerden; "Mevcut konumumu kullan" LocationManager + en yakın ilçe (emülatörde varsayılan konum Mountain View) |

### HavaCan Asistan ve Sesli Rehber (son çalışma)

- Orta düğme artık sohbet ekranını açıyor (`ui/chat/ChatScreen.kt`, `AssistantHero.kt`). Mikrofon mesaj çubuğunun sağında; yazı varken gönder düğmesi. Ayrı "Sesli konuş" düğmesi ya da "Sesli yanıt açık" kartı yok.
- Beyin: `domain/voice/VoiceCommandParser.kt` (kurala dayalı Türkçe komut ayrıştırma: navigasyon, bilgi sorgusu, işlem, oturum sonu) ve `HavaCanAssistant.kt` (yanıtları `WeatherRepository`, plan deposu, Dolabım, `PlanAdvisor`, `RunSlotPlanner`, `OutfitEngine` ile gerçek veriden üretir). Plan oluşturma/silme önce okunur, "evet" beklenir. Plan değiştirme ve takvim güncelleme sesle desteklenmiyor; dürüstçe söyleniyor.
- Oturum: `ui/voice/VoiceGuideController.kt` (activity ViewModel). Kullanıcı başlatır; tek cümle dinler, yanıtlar, TTS bittikten 400 ms sonra yeniden dinler. İki kez sessizlik, "sesli rehberi kapat", çarpı düğmesi, uygulamadan çıkış ya da 5 dk sınırı oturumu bitirir. TalkBack açıkken yanıtları TalkBack okur (kendi TTS'imiz susar) ve sıradaki tur mikrofona dokunarak başlar.
- Her ana ekranda `VoiceGuideBar` (sol altta mikrofon, oturumda durum çubuğu). Navigasyon `HavaApp.kt` içindeki `go()` ile.
- Profil: `ui/profile/ProfileScreen.kt`: "HavaCan ile Sesli Kullanım" anahtarı (açılınca mikrofon izni açıklamalı istenir), "Yazılı mesajlara sesli yanıt", "Hakkında" ve veri atıfları.
- Serbest sorular (kuralların anlamadığı) backend `POST /v1/chat` ucuna gider; yalnızca metin döner, uygulama ona göre işlem yapmaz. Backend deploy edilmeden "Bunu anlayamadım" + örnek komutlar gösterilir.
- Birim testleri: toplam 131 test geçiyor (`VoiceCommandParserTest`, `HavaCanAssistantTest`, `VoiceGuideControllerTest` dahil).

### Koşu Planla (son çalışma)

- Akış: Ana ekran → Koşucu Modu → "Koşuyu planla" → Koşu Planla → "Koşumu planla" → Planlarım. Planlarım'da koşu kartına dokununca düzenleme modunda açılır.
- Dosyalar: `ui/run/RunPlannerScreen.kt`, `RunPlannerViewModel.kt`, `RunPlannerRoute.kt`, `RunScene.kt` (Canvas park sahnesi + koşan HavaCan + konfeti), `WheelTimePicker.kt` (kaydırmalı tekerlekler), `domain/running/RunSlotPlanner.kt`; `model/Plan.kt` içine `run: RunDetails?` ve `reminderMinutes: Long?` eklendi.
- Kullanıcı geri bildirimiyle yapılanlar:
  - Puanlar ekranda gösterilmiyor (sıralama arka planda puana göre).
  - Tarih: 14 günlük kaydırılabilir gün şeridi, her günde hava simgesi, sonda takvim ("Diğer").
  - "HavaCan en uygun saati buldu" yazısı kaldırıldı.
  - Önerilen saatler kompakt liste; hissedilen, rüzgar, yağış, UV aynı satırda.
  - Süre: kartın içinde saat/dakika tekerleği (5 dk–5 sa).
  - Hatırlatma: "koşudan ne kadar önce" süre tekerleği (en az 5 dk); altında bildirim saati.
  - HavaCan'ın öneri/tavsiye kartı istenmedi; ekleme.
- Bu son tur **cihazda görülmedi**; kullanıcı test edip geri bildirim verecek.
- Birim testleri: toplam 99 test geçiyor (`RunSlotPlannerTest`, `RunPlannerViewModelTest` dahil).

## 6. Açık işler ve bilinen eksikler

- **Commit/push**: Sesli rehber ve OpenRouter işi yerelde commit edildi, push edilmedi. Yeni işlerde yine önce kullanıcıdan onay al.
- **Backend**: `backend/havacan-api` Vercel'de çalışıyor: `https://havacan-api.vercel.app` (OpenRouter anahtarı Vercel'de Secret ortam değişkeni). `Weather/local.properties` içinde `HAVACAN_API_URL` bu adres. Cloudflare Worker da deploy edildi ama kullanıcının ağı `*.workers.dev` adreslerini engelliyor (tarayıcı ECH ile açıyor, uygulama açamıyor); kullanılmıyor. Backend değişince: `cd backend/havacan-api && npx vercel --prod`. Vercel GitHub bağlantısı kurulamadı (private repo), otomatik deploy yok.
- **GPS konumu yok**: Konum izni eklenmedi; konum il/ilçe seçiciyle seçiliyor.
- **Hatırlatma zamanlaması**: WorkManager kesin saatli değil; pil tasarrufunda birkaç dakika gecikebilir.
- **Tahmin önbelleği yalnızca bellekte**: İnternet yokken eski tahmin gösterilmiyor; hata kartı çıkıyor.
- **Görseller**: HavaCan ve sahneler Canvas çizimi. Gerçek 3D asset'ler ve uygulama simgesi henüz yok (`docs/havacan-assets.md`). Uygulama şu an varsayılan Android simgesini kullanıyor.
- **Sesli rehber sınırları**: Uyandırma kelimesi ve arka planda dinleme yok (bilerek). Konuşma tanıma telefonun hizmetine bağlı (emülatörde host mikrofonu açılmalı); Türkçe TTS paketi yoksa yanıtlar yalnızca yazılı. Sohbet geçmişi yalnızca bellekte. Sesle plan şehri her zaman ana şehir; türü açık hava.

## 7. Yol haritası (kullanıcıyla konuşulan, sırayla yapılacak özellikler)

Kullanıcı bunları "sırayla yaparız" dedi; her biri için önce kısa plan, kullanıcının referans görseli gelirse ona göre tasarım.

1. **Weather Meet (grup buluşması)**: Bir grup için ortak buluşma planı; herkesin konumuna/saatine göre hava açısından en uygun zamanı ve yeri öner, grup üyelerine paylaş. Planlarım altyapısını (Plan, Plan B, hava takibi) yeniden kullan.
2. **SeaSense**: Deniz/plaj modu: deniz suyu sıcaklığı, dalga, rüzgar, UV'ye göre denize girme uygunluğu (Open-Meteo Marine API).
3. **Work Mode (İş modu)**: İşe gidiş-dönüş saatlerine göre hava özeti, şemsiye/mont uyarısı, yolculuk planı.
4. **Weather DNA**: Kullanıcının hava tercihlerini (sıcak/soğuk hassasiyeti, yağmur toleransı) öğrenip önerileri kişiselleştirme.
5. **Event Scanner**: Etkinlik afişi/davetiye fotoğrafından tarih, saat, yer okuyup plana ekleme ve hava riskini gösterme.
6. ~~**HavaCan sohbet / sesli asistan**~~: yapıldı (yukarıda). Sonraki adım: sesle plan değiştirme (onaylı), sohbet geçmişini saklama.
7. **Seyahat / bavul önerisi**: Gidilecek yerin tahminine göre bavul listesi; Dolabım ile bağlanabilir.
8. **Profil**: Sesli kullanım ayarları ve "Hakkında" yapıldı; kişisel tercihler (ad, Weather DNA) eksik.

Ortak altyapı fikri: hepsi aynı `WeatherRepository`, `AppGraph`, plan deposu ve HavaCan karakter/animasyon sistemini kullanmalı; yeni bağımsız veri sistemleri kurulmamalı.

## 8. Tasarım dili

- Renkler (`ui/theme/Color.kt`): Ivory `#FBF7EE` zemin, Navy `#0B1B3F` metin, HavaBlue `#1D63F0`, Sun `#FFC72C`, GoodGreen, RiskRed.
- Yazı tipi: Nunito (OFL lisansı `docs/licenses/`).
- Beyaz yuvarlak kartlar (`SoftCard`), yumuşak gölge, yaylı mikro animasyonlar, sevimli ama profesyonel.
- Kullanıcı tasarımı referans görsellerle veriyor; görseldeki sayılar örnektir, sabit kodlanmaz. Tam ekran görseli arka plan yapmak yerine gerçek Compose bileşenleri kullanılır.

## 9. Yeni oturumda ilk adımlar

1. Kullanıcının Koşu Planla test geri bildirimini al ve hızlıca düzelt.
2. Sonra yol haritasındaki bir sonraki özelliğe (kullanıcı hangisini isterse) geç.
