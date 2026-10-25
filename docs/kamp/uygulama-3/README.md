# Kamp+ Uygulama 3 · Canlanan ekran

Git tag: `kamp-uygulama-3` · Emülatör: Medium Phone API 37.0

| Madde | Durum | Kanıt |
|---|---|---|
| `OutlinedTextField` ile arama kutusu (`remember` + `mutableStateOf`) | Tamam | Commit `072365e`, [CityScreen.kt](../../../Weather/app/src/main/java/com/example/weather/ui/city/CityScreen.kt) |
| Yazdıkça şehir listesi süzülüyor | Tamam | [01-filtre-an.png](01-filtre-an.png), UI testi `typingFiltersTheList` |
| Döndürünce arama kayboldu mu? Test edildi | Tamam: `remember` ile kayboldu | [02-remember-dondurunce-kayboldu.png](02-remember-dondurunce-kayboldu.png) ("an" yazılıydı, yatay ekranda boş) |
| `rememberSaveable` ile düzeltildi | Tamam | [05-rememberSaveable-dondurme.png](05-rememberSaveable-dondurme.png), UI testi `queryAndSelectionSurviveRecreation` |
| Bir şehre dokununca seçili şehir vurgulanıyor | Tamam | [04-secili-sehir.png](04-secili-sehir.png); seçim de döndürmede korunuyor |
| Sonuç yoksa "Şehir bulunamadı" | Tamam | [03-sehir-bulunamadi.png](03-sehir-bulunamadi.png), UI testi `noMatchShowsCityNotFound` |

## Bonuslar

| Bonus | Durum | Kanıt |
|---|---|---|
| State'i `CityViewModel`'e taşı | Uygulama 4'te | Uygulama 4 zaten `CityViewModel` istiyor; orada `SavedStateHandle` ile yapılacak |
| Kutuya temizle (×) düğmesi | Tamam | [04-secili-sehir.png](04-secili-sehir.png), UI testi `clearButtonResetsTheSearch` |
| Sonuç sayısını göster | Tamam | Arama kutusunun altında "6 şehir" |

## Ek

- Arama Türkçe karakterlere duyarsız: "eskisehir" → Eskişehir, "DIYARBAKIR" → Diyarbakır ([CitySearch.kt](../../../Weather/app/src/main/java/com/example/weather/data/CitySearch.kt), unit testli).
- Testler: 10 unit test ve 5 Compose UI testi (emülatörde) geçiyor.
- Bilinen konu: yatay ekranda liste için az yer kalıyor. Tasarım aşamasında ele alınacak.
