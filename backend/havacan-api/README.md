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

## Korumalar

- Anahtar yalnızca Worker secret'ında.
- IP başına dakikada 20 istek (Cloudflare rate limiting).
- İstek başına en fazla ~1 MB görsel, cevap en fazla 200 token.
- Model çıktısı sabit listelere (kategori, renk, mevsim) göre süzülür; liste dışı değer uygulamaya ulaşmaz.
- OpenRouter panelinden bu anahtar için **harcama limiti** koy (ör. 2 $): asıl güvence budur.

Uygulama içine gömülen hiçbir şey gizli sayılamaz; bu yüzden Worker adresi gizli değildir, koruma rate limit ve harcama limitidir.

## Uç nokta

`POST /v1/clothing/analyze` · gövde `{"image": "<base64 jpeg>"}`
→ `{"name": "Krem kazak", "category": "Top", "color": "Krem", "seasons": ["Autumn", "Winter"]}`
