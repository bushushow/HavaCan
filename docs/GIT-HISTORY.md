# Git stratejisi ve geçmiş

## Dallar ve etiketler

`main` her zaman çalışan, test edilmiş haldir ve doğrusal ilerler. Her iş kısa ömürlü bir dalda yapılır, kontroller yeşilken `main`'e hızlı ileri (fast-forward) birleştirilir.

| Ad | Tür | Anlamı |
|---|---|---|
| `main` | dal | Çalışan son hal; CI her push'ta doğrular |
| `design/<konu>` | dal | Görsel denemeler (ör. `design/renk`: gökyüzü başlıkları ve karanlık mod) |
| `arch/<konu>` | dal | Görünümü değiştirmeyen yapı değişiklikleri |
| `kamp-uygulama-2`, `-3`, `-4` | etiket | Kamp+ zorunlu uygulamalarının teslim anları |
| `pre-architecture` | etiket | Mimari düzenlemeden önceki, her şeyin çalıştığı son hal |

## Commit biçimi

[Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <özet>`

- **type:** `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`
- **scope:** `home`, `plans`, `run`, `meet`, `wardrobe`, `chat`, `voice`, `profile`, `city`, `ui`, `nav`, `data`, `domain`, `backend`
- Özet İngilizce, küçük harfle, emir kipinde; gövde neyin neden değiştiğini ve **nasıl doğrulandığını** yazar.
- Yapay zekâ ile yazılan commit'ler `Co-Authored-By` satırı taşır.

Bu biçim `527e454`'ten itibaren uygulanıyor; daha eski commit'ler serbest başlıklıdır ve yeniden yazılmadı (geçmiş değiştirilmez).

## Kontrol kapıları

Her push ve PR'da `.github/workflows/android.yml` çalışır:

1. `spotlessCheck`: ktlint biçim kuralları (temel repoyla aynı `.editorconfig`)
2. `testDebugUnitTest`: tüm birim testleri
3. `assembleDebug`: uygulama derlenir
4. Backend: her `.js` dosyası için `node --check`

Yerelde aynısı:

```bash
cd Weather && ./gradlew spotlessCheck testDebugUnitTest assembleDebug
```

## Sıralı geçmiş

| Commit | Ne geldi |
|---|---|
| `ff2b072`…`4687579` | Boş depo ve Android Studio Compose şablonu |
| `98f2699`, `880e11d` | **Uygulama 2:** "Merhaba Kamp!", 15 şehirlik `LazyColumn`, tıklama kaydı |
| `072365e`, `fb71a18` | **Uygulama 3:** `remember` ile arama, filtre, boş durum, `rememberSaveable` |
| `3eb6d8c`, `8ed275e` | **Uygulama 4:** Open-Meteo şehir arama, Retrofit, ViewModel, UI durumları |
| `2cca116`, `d611cdb` | HavaCan temeli: tahmin API'si, koşu uygunluğu, animasyonlu ada ve karakter |
| `5e799f5` | 81 il, 973 ilçe ve ülke seçimi (telefonda arama) |
| `16a9f52` | Planlarım: hava riski, Plan B, takvim, bildirimler |
| `dd48cb7` | Dolabım: fotoğraf, Room, Salla Kombin, yapay zekâ backend'i |
| `cd538ee` | Koşu Planla |
| `4c9d84a` | HavaCan Asistan: sohbet, sesli rehber, OpenRouter araçları |
| `4955334` | Profilim ve yeni konum seçici |
| `28b4006` | Ana sayfa tahmin kartı, paket adı `com.kampplus.havacan`, ktlint |
| `527e454` | Weather Meet ve sürüklenebilir sesli buton |
| `69d5278` | Gökyüzü başlıkları, karanlık mod, renkli sekme simgeleri |
