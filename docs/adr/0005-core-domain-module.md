# ADR-0005: Saf Kotlin :core:domain modülü

Durum: Kabul edildi

## Bağlam
Kurallar (risk, koşu puanı, buluşma, kombin, sesli komutlar) uygulamanın en değerli ve en çok test edilen kısmı. Daha önce `domain` yanlışlıkla Room varlığını, `model` ise Compose durumunu kullanıyordu.

## Karar
`model` ve `domain` paketleri Android eklentisi olmayan bir `kotlin-jvm` modülüne (`:core:domain`) taşındı. `:app` ona bağımlıdır; tersi mümkün değildir.

## Sonuçlar
- Android, Room ya da Compose importu bu modülde **derlenmez**; katman kuralını kod incelemesi değil derleyici korur.
- Kural testleri emülatörsüz, saf JVM'de çalışır.
- `:core:data` ayrı modül yapılmadı: veri katmanı Android'e (Room, WorkManager, Context) bağlı ve tek tüketicisi `:app`; ayırmak fayda getirmeden derleme yapılandırmasını çoğaltırdı.
