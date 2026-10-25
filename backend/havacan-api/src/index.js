// HavaCan API (Cloudflare Worker)
// POST /v1/clothing/analyze  {"image": "<base64 jpeg>"}
//   -> {"name": "...", "category": "Top", "color": "Krem", "seasons": ["Autumn", "Winter"]}
// The OpenRouter key lives only in this Worker's secrets (OPENROUTER_API_KEY).

const CATEGORIES = ["Top", "Bottom", "Dress", "Outerwear", "Shoes", "Accessory"];
const COLORS = [
  "Beyaz", "Krem", "Bej", "Gri", "Siyah", "Lacivert", "Kahverengi", "Kot mavisi", "Mavi",
  "Açık mavi", "Yeşil", "Haki", "Sarı", "Turuncu", "Kırmızı", "Bordo", "Pembe", "Mor",
];
const SEASONS = ["Spring", "Summer", "Autumn", "Winter"];
const MAX_IMAGE_BASE64 = 1_500_000; // ~1.1 MB JPEG; the app sends ~512 px images

const PROMPT = `Sen bir kıyafet etiketleme asistanısın. Fotoğraftaki TEK kıyafet parçasını etiketle.
Sadece şu JSON'u döndür, başka hiçbir şey yazma:
{"name": "<2-3 kelimelik Türkçe ad, ör. Krem kazak>",
 "category": "<${CATEGORIES.join("|")}>",
 "color": "<${COLORS.join("|")}>",
 "seasons": [<${SEASONS.join("|")} değerlerinden uygun olanlar>]}
Fotoğrafta kıyafet yoksa {"name": null, "category": null, "color": null, "seasons": []} döndür.`;

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

function sanitize(raw) {
  const name = typeof raw?.name === "string" ? raw.name.trim().slice(0, 40) : null;
  return {
    name: name || null,
    category: CATEGORIES.includes(raw?.category) ? raw.category : null,
    color: COLORS.includes(raw?.color) ? raw.color : null,
    seasons: Array.isArray(raw?.seasons) ? raw.seasons.filter((s) => SEASONS.includes(s)) : [],
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/v1/clothing/analyze") return json(404, { error: "not_found" });
    if (request.method !== "POST") return json(405, { error: "method_not_allowed" });
    if (!env.OPENROUTER_API_KEY) return json(503, { error: "not_configured" });

    if (env.RATE_LIMITER) {
      const ip = request.headers.get("CF-Connecting-IP") || "unknown";
      const { success } = await env.RATE_LIMITER.limit({ key: ip });
      if (!success) return json(429, { error: "rate_limited" });
    }

    let image;
    try {
      ({ image } = await request.json());
    } catch {
      return json(400, { error: "bad_json" });
    }
    if (typeof image !== "string" || image.length === 0 || image.length > MAX_IMAGE_BASE64) {
      return json(413, { error: "bad_image" });
    }

    const upstream = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
        "Content-Type": "application/json",
        "X-Title": "HavaCan",
      },
      body: JSON.stringify({
        model: env.MODEL,
        max_tokens: 200,
        temperature: 0,
        response_format: { type: "json_object" },
        messages: [
          { role: "system", content: PROMPT },
          {
            role: "user",
            content: [
              { type: "text", text: "Bu kıyafeti etiketle." },
              { type: "image_url", image_url: { url: `data:image/jpeg;base64,${image}` } },
            ],
          },
        ],
      }),
    });
    if (!upstream.ok) return json(502, { error: "upstream", status: upstream.status });

    try {
      const data = await upstream.json();
      const text = data?.choices?.[0]?.message?.content ?? "";
      const start = text.indexOf("{");
      const end = text.lastIndexOf("}");
      return json(200, sanitize(JSON.parse(text.slice(start, end + 1))));
    } catch {
      return json(502, { error: "unreadable_model_output" });
    }
  },
};
