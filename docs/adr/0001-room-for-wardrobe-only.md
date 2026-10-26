# ADR-0001: Room yalnızca dolap için

Durum: Kabul edildi

## Bağlam
Planlar, kıyafetler ve tercihler uygulama kapanınca kaybolmamalı. Planlar küçük ve tek liste halinde okunur; kıyafetler fotoğraflı, filtrelenir ve kombin için sorgulanır.

## Karar
- Kıyafetler ve kaydedilen kombinler **Room**'da (`havacan.db`); fotoğraflar `filesDir/wardrobe` altında, satırda yalnızca dosya yolu.
- Planlar tek bir **JSON dosyasında** (`FilePlanStore`): geçici dosyaya yazılıp yeniden adlandırılır, yarım yazılmış dosya kalmaz.
- Tercihler **SharedPreferences**'ta.
- Room varlıkları (`ClothingEntity`) yalnızca `data`'da; `domain` saf `ClothingItem` ile çalışır.

## Sonuçlar
Plan modeli değişince göç (migration) gerekmez; yeni alanlar varsayılanlıdır. Dolap tablosunun sütunları değişirse Room göçü yazılmalıdır.
