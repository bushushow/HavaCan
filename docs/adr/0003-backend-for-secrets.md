# ADR-0003: Yapay zekâ anahtarı için backend

Durum: Kabul edildi

## Bağlam
Kıyafet fotoğrafı analizi ve sohbet OpenRouter kullanır. Anahtar APK'ya girerse çıkarılabilir; Git geçmişine girerse sızar.

## Karar
- Uygulama yalnızca kendi backend'imizi (`backend/havacan-api`, Vercel) çağırır; anahtar orada ortam sırrıdır.
- Backend modelin yalnızca sabit araçlardan birini seçmesine izin verir; yazan işlemler (plan oluşturma/değiştirme) uygulamada kullanıcıya okunur ve onay beklenir.
- Backend adresi `local.properties` → `BuildConfig` ile gelir; dosya Git'e girmez.

## Sonuçlar
Backend yoksa uygulama çalışmaya devam eder: renk telefonda bulunur, etiketler elle girilir, sohbet kurala dayalı yanıt verir.
