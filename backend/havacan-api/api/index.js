// Vercel Edge Function entry (npx vercel --prod). The logic lives in ../src/app.js.
// Some networks block *.workers.dev; *.vercel.app works there, with the same code.
import { handle } from "../src/app.js";

export const config = { runtime: "edge" };

const LIMIT = 20;
const WINDOW_MS = 60_000;
const hits = new Map();

// Best effort only: each Vercel instance counts on its own. The OpenRouter credit limit is the real cap.
const memoryLimiter = {
  async limit({ key }) {
    const now = Date.now();
    const recent = (hits.get(key) || []).filter((t) => now - t < WINDOW_MS);
    recent.push(now);
    hits.set(key, recent);
    if (hits.size > 5000) hits.clear();
    return { success: recent.length <= LIMIT };
  },
};

export default function vercelHandler(request) {
  return handle(request, {
    OPENROUTER_API_KEY: process.env.OPENROUTER_API_KEY,
    MODEL: process.env.MODEL || "google/gemini-2.5-flash",
    RATE_LIMITER: memoryLimiter,
  });
}
