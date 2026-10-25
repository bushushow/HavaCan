# Veri kaynakları ve lisanslar

| Veri | Kaynak | Lisans |
|---|---|---|
| Hava tahmini (anlık, saatlik, günlük) | [Open-Meteo Forecast API](https://open-meteo.com/) | CC BY 4.0 |
| Şehir arama, il ve ilçe koordinatları | [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api) | CC BY 4.0 |
| 81 il ve 973 ilçenin listesi, Open-Meteo'da olmayan 24 yeni ilçenin koordinatı | [GeoNames](https://www.geonames.org/) (`admin1CodesASCII`, `admin2Codes`, `TR.zip`) | CC BY 4.0 |
| Nunito yazı tipi | Google Fonts | SIL OFL 1.1 (`docs/licenses`) |

Uygulama yayına çıkmadan önce bu kaynaklar uygulama içinde de anılmalı (ör. Profil › Hakkında).

## İl ve ilçe verisini yeniden üretmek

`Weather/app/src/main/java/com/example/weather/data/ProvinceCities.kt` ve
`Weather/app/src/main/assets/districts_tr.json` elle düzenlenmez; şu iki script üretir:

```bash
# GeoNames dosyalarını geonames/ klasörüne indir: admin1CodesASCII.txt, admin2Codes.txt, TR.zip (açılmış TR.txt)
python tools/places/build_places.py geonames places.json
python tools/places/emit_places.py places.json \
  Weather/app/src/main/java/com/example/weather/data/TurkishProvinces.kt \
  Weather/app/src/main/java/com/example/weather/data/ProvinceCities.kt \
  Weather/app/src/main/assets/districts_tr.json
```

İlçeler Open-Meteo sonuçlarıyla aynı GeoNames kimliği (`admin2_id`) üzerinden eşleştirilir; isimler Open-Meteo'nun Türkçe yazımıyla düzeltilir (ör. "Beypazari" → "Beypazarı").
