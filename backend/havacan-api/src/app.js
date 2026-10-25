// HavaCan API (runs as a Cloudflare Worker and as a Vercel Edge Function)
// POST /v1/clothing/analyze  {"image": "<base64 jpeg>"}
//   -> {"name": "...", "category": "Top", "color": "Krem", "seasons": ["Autumn", "Winter"]}
// POST /v1/assistant  {"message": "...", "context": "<facts from the app>", "history": [...]}
//   -> {"reply": "...", "action": null} or {"reply": null, "action": {"name": "update_plan", "args": {...}}}
//   The model may only pick a tool from TOOLS; the app checks it and asks the user before writing.
// The OpenRouter key lives only in this Worker's secrets (OPENROUTER_API_KEY).

const CATEGORIES = ["Top", "Bottom", "Dress", "Outerwear", "Shoes", "Accessory"];
const COLORS = [
  "Beyaz", "Krem", "Bej", "Gri", "Siyah", "Lacivert", "Kahverengi", "Kot mavisi", "Mavi",
  "Açık mavi", "Yeşil", "Haki", "Sarı", "Turuncu", "Kırmızı", "Bordo", "Pembe", "Mor",
];
const SEASONS = ["Spring", "Summer", "Autumn", "Winter"];
const MAX_IMAGE_BASE64 = 1_500_000; // ~1.1 MB JPEG; the app sends ~512 px images
const MAX_MESSAGE = 500;

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

async function openRouter(env, body) {
  return fetch("https://openrouter.ai/api/v1/chat/completions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
      "Content-Type": "application/json",
      "X-Title": "HavaCan",
    },
    body: JSON.stringify({ model: env.MODEL, ...body }),
  });
}

async function analyzeClothing(request, env) {
  let image;
  try {
    ({ image } = await request.json());
  } catch {
    return json(400, { error: "bad_json" });
  }
  if (typeof image !== "string" || image.length === 0 || image.length > MAX_IMAGE_BASE64) {
    return json(413, { error: "bad_image" });
  }

  const upstream = await openRouter(env, {
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
}

// The only things the model may ask the app to do. The app validates every argument again and
// reads write actions back to the user, waiting for "evet", before anything is saved.
const SCREENS = ["Home", "Plans", "Chat", "Wardrobe", "Profile", "RunPlanner"];
const TOOLS = [
  {
    name: "open_screen",
    description: "Uygulamada bir ekran açar. Home=ana sayfa/hava, Plans=Planlarım, Wardrobe=Dolabım, Profile=Profil/ayarlar, RunPlanner=Koşu Planla, Chat=sohbet.",
    parameters: { type: "object", properties: { screen: { type: "string", enum: SCREENS } }, required: ["screen"] },
  },
  {
    name: "weather",
    description: "Ana şehrin hava durumunu uygulama söyler. day_offset: 0 bugün, 1 yarın.",
    parameters: { type: "object", properties: { day_offset: { type: "integer", minimum: 0, maximum: 15 } }, required: ["day_offset"] },
  },
  {
    name: "list_plans",
    description: "Kullanıcının yaklaşan planlarını ve hava risklerini uygulama okur.",
    parameters: { type: "object", properties: {} },
  },
  {
    name: "outfit",
    description: "Kullanıcının kendi dolabından o günün havasına göre kombin önerir.",
    parameters: { type: "object", properties: { day_offset: { type: "integer", minimum: 0, maximum: 15 } }, required: ["day_offset"] },
  },
  {
    name: "run_time",
    description: "Koşu için en uygun saati bulur.",
    parameters: { type: "object", properties: { day_offset: { type: "integer", minimum: 0, maximum: 15 } }, required: ["day_offset"] },
  },
  {
    name: "create_plan",
    description: "Yeni plan taslağı hazırlar; kullanıcı onaylamadan kaydedilmez. Bilinmeyen alanı boş bırak, uydurma.",
    parameters: {
      type: "object",
      properties: {
        title: { type: "string", description: "Kısa plan adı, ör. Ayşe ile kahve" },
        date: { type: "string", description: "YYYY-MM-DD" },
        time: { type: "string", description: "HH:MM, 24 saat" },
        indoor: { type: "boolean", description: "Kapalı alanda mı" },
      },
    },
  },
  {
    name: "update_plan",
    description: "Var olan bir planın adını, gününü, saatini ya da iç/dış mekânını değiştirir; kullanıcı onaylamadan kaydedilmez. plan_id bağlamdaki listeden olmalı.",
    parameters: {
      type: "object",
      properties: {
        plan_id: { type: "string" },
        title: { type: "string" },
        date: { type: "string", description: "YYYY-MM-DD" },
        time: { type: "string", description: "HH:MM" },
        indoor: { type: "boolean" },
      },
      required: ["plan_id"],
    },
  },
  {
    name: "delete_plan",
    description: "Bir planı siler; kullanıcı onaylamadan silinmez. plan_id bağlamdaki listeden olmalı.",
    parameters: { type: "object", properties: { plan_id: { type: "string" } }, required: ["plan_id"] },
  },
];

const ASSISTANT_PROMPT = `Sen HavaCan'sın: hava durumuna göre günlük hayatı planlamaya yardım eden Türkçe asistan.
Kullanıcının isteği bir araca uyuyorsa o aracı çağır; uymuyorsa en fazla 3 kısa cümleyle düz metin yanıt ver.
Kurallar:
- Hava, plan, kombin ve koşu sayılarını kendin söyleme: ilgili aracı çağır, uygulama gerçek veriden söyler.
- Tarihleri bağlamdaki "Bugün" bilgisine göre YYYY-MM-DD olarak hesapla. Saat belirsizse ("akşam") makul bir saat seçme, boş bırak.
- indoor alanını yalnızca kullanıcı iç ya da dış mekân dediyse doldur; yoksa hiç gönderme.
- Plan değiştirme ve silmede plan_id'yi bağlamdaki listeden al. Kullanıcının dediğine adı ya da günüyle tek bir plan uyuyorsa doğrudan onu kullan ve sorma; "bir saat ileri al" gibi göreli değişiklikleri o planın saatinden hesapla. Yalnızca birden fazla plan uyuyorsa ya da hiçbiri uymuyorsa sor.
- Selamlaşma, teşekkür, kısa fıkra ya da hal hatır gibi sohbetlerde samimi ve kısa yanıt ver; reddetme.
- İşlemi yapmış gibi konuşma: araçlar yalnızca taslak hazırlar, kullanıcı onaylar.
- Liste, emoji, markdown kullanma; yanıt sesli okunabilir. Tıbbi, hukuki ya da finansal kesin tavsiye verme.`;

function cleanArgs(name, raw) {
  const a = raw && typeof raw === "object" ? raw : {};
  const str = (v, max) => (typeof v === "string" && v.trim() ? v.trim().slice(0, max) : undefined);
  const day = Number.isInteger(a.day_offset) ? Math.min(Math.max(a.day_offset, 0), 15) : 0;
  switch (name) {
    case "open_screen":
      return SCREENS.includes(a.screen) ? { screen: a.screen } : null;
    case "weather":
    case "outfit":
    case "run_time":
      return { day_offset: day };
    case "list_plans":
      return {};
    case "create_plan":
    case "update_plan":
    case "delete_plan": {
      const out = {
        plan_id: str(a.plan_id, 80),
        title: str(a.title, 60),
        date: /^\d{4}-\d{2}-\d{2}$/.test(a.date ?? "") ? a.date : undefined,
        time: /^\d{2}:\d{2}$/.test(a.time ?? "") ? a.time : undefined,
        indoor: typeof a.indoor === "boolean" ? a.indoor : undefined,
      };
      if (name !== "create_plan" && !out.plan_id) return null;
      return out;
    }
    default:
      return null;
  }
}

async function assistant(request, env) {
  let message, context, history;
  try {
    ({ message, context, history } = await request.json());
  } catch {
    return json(400, { error: "bad_json" });
  }
  if (typeof message !== "string" || message.trim().length === 0 || message.length > MAX_MESSAGE) {
    return json(400, { error: "bad_message" });
  }
  const facts = typeof context === "string" ? context.slice(0, 2000) : "Yok.";
  const past = (Array.isArray(history) ? history : [])
    .slice(-6)
    .filter((m) => (m?.role === "user" || m?.role === "assistant") && typeof m.content === "string")
    .map((m) => ({ role: m.role, content: m.content.slice(0, 400) }));

  const upstream = await openRouter(env, {
    max_tokens: 300,
    temperature: 0.2,
    tools: TOOLS.map((t) => ({ type: "function", function: t })),
    tool_choice: "auto",
    messages: [
      { role: "system", content: `${ASSISTANT_PROMPT}\n\nUygulama verisi:\n${facts}` },
      ...past,
      { role: "user", content: message.trim() },
    ],
  });
  if (!upstream.ok) return json(502, { error: "upstream", status: upstream.status });

  try {
    const data = await upstream.json();
    const msg = data?.choices?.[0]?.message ?? {};
    const call = msg.tool_calls?.[0]?.function;
    if (call && TOOLS.some((t) => t.name === call.name)) {
      let args = {};
      try {
        args = typeof call.arguments === "string" ? JSON.parse(call.arguments || "{}") : call.arguments;
      } catch {
        args = {};
      }
      const clean = cleanArgs(call.name, args);
      if (clean) return json(200, { reply: null, action: { name: call.name, args: clean } });
    }
    const reply = String(msg.content ?? "").replace(/[*#_`]/g, "").trim().slice(0, 600);
    if (!reply) return json(502, { error: "empty_model_output" });
    return json(200, { reply, action: null });
  } catch {
    return json(502, { error: "unreadable_model_output" });
  }
}

const ROUTES = {
  "/v1/clothing/analyze": analyzeClothing,
  "/v1/assistant": assistant,
};

/** Shared by Cloudflare (src/index.js) and Vercel (api/index.js); env holds OPENROUTER_API_KEY, MODEL, RATE_LIMITER. */
export async function handle(request, env) {
  const url = new URL(request.url);
  const handler = ROUTES[url.pathname];
  if (!handler) return json(404, { error: "not_found" });
  if (request.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (!env.OPENROUTER_API_KEY) return json(503, { error: "not_configured" });

  if (env.RATE_LIMITER) {
    const ip =
      request.headers.get("CF-Connecting-IP") ||
      request.headers.get("x-real-ip") ||
      (request.headers.get("x-forwarded-for") || "").split(",")[0].trim() ||
      "unknown";
    const { success } = await env.RATE_LIMITER.limit({ key: ip });
    if (!success) return json(429, { error: "rate_limited" });
  }
  return handler(request, env);
}
