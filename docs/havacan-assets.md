# HavaCan: görsel asset ihtiyaçları

Şu anki sürümde HavaCan, ada, güneş, bulutlar, şemsiye, ayakkabı ve mont **Compose Canvas ile kodda çizilen 2.5D illüstrasyonlar**. Gerçek 3D model yok. Hareketler (süzülme, göz kırpma, zıplama, koşu adımları, kafa bandı ve ayakkabıların gelmesi, pistin oluşması, adayı sürükleyince parallax ile dönme hissi) bu çizimlerin üzerinde çalışıyor.

Referans görsellerdeki 3D render kalitesine ulaşmak için aşağıdaki asset'ler gerekiyor. Gelince, kodda yalnızca çizim fonksiyonları değişecek; animasyon durumu (`HavaCanState`, `HavaCanMotion`) aynen kalacak.

## Önerilen yol: katmanlı PNG/WebP (2.5D)

Hepsi **şeffaf arka planlı**, **WebP (kayıpsız) veya PNG**, aynı ışık yönü (sol üstten), aynı kamera açısı (referans görsellerdeki gibi hafif yukarıdan).

| Asset | Boyut (px) | Not |
|---|---|---|
| HavaCan gövdesi (yüzsüz, uzuvsuz) | 1024 × 1024 | Ayrı katman ki yüz ve uzuvlar hareket edebilsin |
| Yüz ifadeleri: mutlu `^^`, göz kırpma, çok mutlu (ağız açık), şaşkın | her biri 512 × 512 | Gövdenin üstüne hizalı, aynı merkez |
| Kol (sol, sağ) | 256 × 256 | Dönme noktası omuz |
| Ayak (sade) ve koşu ayakkabısı (sol, sağ) | 384 × 256 | Koşu adımı için ayrı ayrı |
| Kafa bandı | 1024 × 384 | Gövdenin alnına oturacak şekilde |
| Ada tabanı (taş silindir) | 2048 × 1024 | Çim ve parkur olmadan |
| Ada üstü, normal: çim + patika + gölet | 2048 × 1024 | Tabanla aynı perspektif |
| Ada üstü, koşu: çim + atletizm pisti | 2048 × 1024 | Normal üst ile birebir hizalı (geçişte üst üste eriyecek) |
| Ağaçlar (3 farklı), taşlar | 256 × 256 | Ayrı ayrı, kenara dizilecek |
| Güneş, ay, bulut (3 farklı), yağmurlu bulut | 512 × 512 | |
| Kart illüstrasyonları: şemsiye, koşu ayakkabısı, mont | 768 × 768 | |
| Uygulama ikonu (HavaCan yüzü) | 1024 × 1024 + adaptive icon katmanları (ön/arka 432 × 432) | Şu an Android'in varsayılan ikonu duruyor |

## Daha ileri seçenek

- **Rive** (`.riv`): Karakteri iskeletli tek dosyada hazırlarsanız durumlar (Idle, Happy, Running…) Rive state machine'e bağlanabilir. Kütüphane küçük ve performanslı; bu, gerçek 3D'den önce en iyi kalite/performans dengesi.
- **Gerçek 3D** (glTF + Filament/SceneView): Adayı 360° döndürmek gibi gerçek 3D etkileşim için gerekir. APK boyutu ve pil maliyeti belirgin artar; yarışma demosu için şart değil.
- **Lottie**: Tasarımcı After Effects'te çalışıyorsa uygundur, ancak durum makinesi ve etkileşim için Rive daha esnek.
