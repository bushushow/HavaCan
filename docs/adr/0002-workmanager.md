# ADR-0002: Hatırlatma ve hava takibi için WorkManager

Durum: Kabul edildi

## Bağlam
Plan hatırlatmaları ve "hava bozulursa haber ver" takibi uygulama kapalıyken de çalışmalı, telefon yeniden başlasa da kaybolmamalı.

## Karar
- Hatırlatma: plan saatinden önce bir kez çalışan iş (`PlanReminderWorker`).
- Takip: 3 saatte bir, yalnızca internet varken çalışan periyodik iş (`PlanWatchWorker`); risk durumu değişmedikçe bildirim göndermez.
- İşçiler depoları `AppContainer` üzerinden okur; ekranla aynı plan dosyasını görürler.

## Sonuçlar
WorkManager kesin saatli değildir; pil tasarrufunda hatırlatma birkaç dakika gecikebilir. Ekranda "kuruldu" yalnızca izin ve zaman uygunsa yazılır.
