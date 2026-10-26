# ADR-0004: Elle bağımlılık enjeksiyonu (AppContainer)

Durum: Kabul edildi

## Bağlam
Ana sayfa önceden kendi `WeatherRepository`'sini kuruyordu; uygulamada iki ayrı hava önbelleği vardı. Arka plan işçileri ve ekranlar aynı plan deposunu görmeliydi.

## Karar
Tek bir `di/AppContainer` bütün repository'leri, zamanlayıcıyı ve backend istemcilerini bir kez kurar. ViewModel fabrikaları ve işçiler bağımlılıklarını oradan alır; ViewModel'ler yalnızca arayüzleri görür.

## Neden Hilt değil?
Nesne sayısı az ve yaşam döngüleri basit (uygulama boyunca tekil). Hilt derleme süresi ve anotasyon karmaşası getirir; elle DI aynı test edilebilirliği sağlar. Kapsam büyürse geçiş `AppContainer`'ın yerine modül yazmaktan ibarettir.

## Sonuçlar
Testler ViewModel'lere sahte depolar verir (`testing/`). Yeni bir depo eklemek `AppContainer`'a bir satırdır.
