## Ne değişti?
<!-- PR başlığı Conventional Commit biçiminde olmalı: feat(plans): add Weather Meet -->

## Neden?

## Nasıl doğrulandı?
<!-- Çalıştırılan komutlar, cihazda denenen akışlar. Denenmeyeni denenmiş gibi yazma. -->

## Kontrol listesi
- [ ] Katman kuralları korunuyor: `domain` saf Kotlin (Android/Room/Compose yok), UI ağa gitmiyor, repository ekranı bilmiyor
- [ ] Bağımlılıklar `AppContainer` üzerinden veriliyor; ViewModel kendi repository'sini kurmuyor
- [ ] Yeni kural için `domain` altında unit test var
- [ ] `./gradlew spotlessCheck testDebugUnitTest assembleDebug` yeşil (`Weather/` içinde)
- [ ] Yükleniyor / boş / hata durumları ele alındı; veri yoksa uydurulmuyor
- [ ] Açık ve karanlık modda okunuyor, `contentDescription` ve 48 dp dokunma alanı var
- [ ] Kullanıcıya gösterilen metinler Türkçe ve `strings.xml` içinde
- [ ] Gizli anahtar yok (API anahtarı yalnızca backend'de)
- [ ] Ekran görüntüsü eklendi (UI değişikliği varsa)
