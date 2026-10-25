// Cloudflare Worker entry (npx wrangler deploy). The logic lives in app.js.
import { handle } from "./app.js";

export default { fetch: handle };
