# ADR-0007: Navigation Compose

Durum: Kabul edildi

## Bağlam
Kabuk ekranları bir sekme değişkeni ve boolean'larla (`showCityPicker`, `runPlanner`) yönetiyordu; geri tuşu her katmanda ayrı `BackHandler` ile çözülüyordu.

## Karar
Tek `NavHost` (`HavaRoutes`): beş sekme, `city` ve `run?planId&start`. Sekmeler `saveState`/`restoreState` ile durumlarını korur. Koşu Planla argümanları isteğe bağlı sorgu parametresidir ve ViewModel'e `SavedStateHandle` ile ulaşır. Sesli komutla gezinme aynı rotaları kullanır.

## Sonuçlar
Geri davranışı Android standardındadır. Planlar ve Dolabım'ın alt ekranları özellik içi hedef durumuyla kalır; ileride iç içe navigasyon grafiğine taşınabilir.
