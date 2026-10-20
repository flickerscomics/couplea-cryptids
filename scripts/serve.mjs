// Tiny static server for trying the built site (dist/) on your own computer.
//
//   npm run preview                      -> http://localhost:8081/
//   node scripts/serve.mjs dist 8081 /couplea/
//                                        -> http://localhost:8081/couplea/
//
// The second form pretends the site lives in a sub-folder, the way GitHub
// Pages does (https://name.github.io/repo-name/). Use it to prove that all
// paths are relative.

import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { extname, join, normalize, resolve } from "node:path";

const [dir = "dist", port = "8081", prefixArg = "/"] = process.argv.slice(2);
const root = resolve(dir);
const prefix = prefixArg.endsWith("/") ? prefixArg : prefixArg + "/";

const types = {
  ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8", ".svg": "image/svg+xml", ".webp": "image/webp",
  ".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".json": "application/json",
};

createServer(async (req, res) => {
  const url = new URL(req.url, "http://x");
  let path = decodeURIComponent(url.pathname);
  if (!path.startsWith(prefix)) {
    if (path === prefix.slice(0, -1) || path === "/") {
      res.writeHead(302, { Location: prefix }).end();
      return;
    }
    res.writeHead(404).end("Not found (this server is pretending to live under " + prefix + ")");
    return;
  }
  path = path.slice(prefix.length);
  if (path === "" || path.endsWith("/")) path += "index.html";
  const file = normalize(join(root, path));
  if (!file.startsWith(root)) { res.writeHead(403).end(); return; }
  try {
    await stat(file);
    const body = await readFile(file);
    res.writeHead(200, { "Content-Type": types[extname(file)] || "application/octet-stream" }).end(body);
  } catch {
    res.writeHead(404).end("Not found");
  }
}).listen(Number(port), () => console.log(`Serving ${dir}/ at http://localhost:${port}${prefix}`));
