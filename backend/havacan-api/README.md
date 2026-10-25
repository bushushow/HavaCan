# HavaCan API (Cloudflare Worker)

Uygulamanın OpenRouter ile konuştuğu tek yer. **API anahtarı APK'da ve GitHub'da yok**; yalnızca bu Worker'ın gizli değişkeninde (`OPENROUTER_API_KEY`) durur.

## Kurulum (bir kez)

```bash
cd backend/havacan-api
npx wrangler login
npx wrangler secret put OPENROUTER_API_KEY   # anahtarı burada yapıştır, dosyaya yazma
npx wrangler deploy
```

Deploy çıktısındaki adresi (ör. `https://havacan-api.<hesabın>.workers.dev`) uygulamaya ver:

```
# Weather/local.properties  (git'e girmez)
HAVACAN_API_URL=https://havacan-api.<hesabın>.workers.dev
```

## Vercel'de kurulum (workers.dev engelli ağlar için)

Bazı ağlar `*.workers.dev` adreslerini engelliyor (tarayıcı açar ama uygulama bağlanamaz). Aynı kod Vercel Edge Function olarak da çalışır (`api/index.js`, mantık `src/app.js` içinde ortak):

```bash
cd backend/havacan-api
npx vercel login
npx vercel                                        # ilk kurulum soruları
npx vercel env add OPENROUTER_API_KEY production  # anahtarı burada yapıştır
npx vercel --prod
```

Çıkan `https://<proje>.vercel.app` adresini `HAVACAN_API_URL` olarak yaz. Vercel'de istek sınırı her sunucu örneğinde ayrı sayılır; asıl sınır OpenRouter'daki kredi limitidir.

## Korumalar

- Anahtar yalnızca Worker secret'ında.
- IP başına dakikada 20 istek (Cloudflare rate limiting).
- İstek başına en fazla ~1 MB görsel, cevap en fazla 200 token.
- Model çıktısı sabit listelere (kategori, renk, mevsim) göre süzülür; liste dışı değer uygulamaya ulaşmaz.
- OpenRouter panelinden bu anahtar için **harcama limiti** koy (ör. 2 $): asıl güvence budur.

Uygulama içine gömülen hiçbir şey gizli sayılamaz; bu yüzden Worker adresi gizli değildir, koruma rate limit ve harcama limitidir.

## Uç noktalar

`POST /v1/clothing/analyze` · gövde `{"image": "<base64 jpeg>"}`
→ `{"name": "Krem kazak", "category": "Top", "color": "Krem", "seasons": ["Autumn", "Winter"]}`

`POST /v1/assistant` · gövde `{"message": "<cümle>", "context": "<bugün, ana şehrin tahmini, yaklaşan planlar>", "history": [{"role": "user|assistant", "content": "..."}]}`
→ `{"reply": "<kısa Türkçe yanıt>", "action": null}` ya da `{"reply": null, "action": {"name": "update_plan", "args": {"plan_id": "...", "time": "19:00"}}}`

Bu uç yalnızca uygulamanın kendi kurallarıyla anlamadığı cümleler için çağrılır (sık komutlar internetsiz, kuralla çalışır). Model yalnızca sabit bir araç listesinden seçebilir: `open_screen`, `weather`, `list_plans`, `outfit`, `run_time`, `create_plan`, `update_plan`, `delete_plan`. Argümanlar hem Worker'da hem uygulamada yeniden doğrulanır; oluşturma, değiştirme ve silme kullanıcıya okunur ve "evet" denmeden kaydedilmez. Hava ve plan sayılarını model değil, uygulama gerçek veriden söyler.

**Gizlilik:** Bu uca giden bağlamda plan adları, tarih/saatleri ve ana şehir bulunur; OpenRouter ve seçilen model sağlayıcısına gider.
