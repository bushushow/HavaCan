# ADR-0006: Telefonda yer arama

Durum: Kabul edildi

## Bağlam
Open-Meteo Geocoding bazı Türk ilçelerini bulamıyor ya da yanlış yazıyordu; internet yokken arama hiç çalışmıyordu.

## Karar
81 il ve 973 ilçe telefonda (`ProvinceCities`, `assets/districts_tr.json`, kaynaklar `docs/data-sources.md`). Arama önce burada yapılır; sonuç yoksa Open-Meteo'ya sorulur (yurt dışı yerler).

## Sonuçlar
Türkiye içi arama internetsiz ve doğru yazımla çalışır. Veri `tools/places` betikleriyle yeniden üretilebilir.
