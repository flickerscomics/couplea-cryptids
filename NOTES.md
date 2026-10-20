# Developer notes

Technical notes for whoever works on the code. The owners' guide is `README.md`;
read that for the non-technical view.

## What this is
Website for **Couple'a Cryptids**, an in-character business on GTA World (a GTA V
roleplay server). Two country boys: **Buck** makes things by hand, **Hunter**
cooks. One page, no cart or payments. The goal is to get players to call or
visit **in-game**. The owners are not programmers and edit stock from the
GitHub website on their phones.

## Rules (do not break)
- **Original names, art and products only.** No real brands, logos or products.
- Everything is **in character** except the OOC footer: `(( Couple'a Cryptids is a roleplay business on GTA World. ))`
- **Build only what the brief asks.** No cart, login, analytics, cookie banner or blog.
- The phone **285538 is in-game only. Never a `tel:` link.** Tapping copies it and shows "Copied".
- Voice: plain, friendly, a little country. Short sentences. Light slang, no heavy accent. At most one or two cryptid jokes on the whole site (currently: Makers intro, Pinned Up intro).
- Anything the owners might change lives in **`site.edn` / `stock.edn`**, never in code.
- Don't invent facts (location, hours, bios, site URL). Placeholders look like `[[ THIS ]]`.
- Paths are **relative** everywhere so it works from `/` or `/repo-name/`.
- The webhook URL is a secret, **never committed**. It comes from the Actions secret `DISCORD_WEBHOOK_URL` at build time (and still ends up in the published JS; that's documented).

## Stack
ClojureScript + shadow-cljs + Reagent 2 (React 19). Plain CSS. One `index.html` shell.
Needs **JDK 21** and **Node 24**. Hosted on GitHub Pages via Actions
(`.github/workflows/deploy.yml`, uploads `dist/` only). Lockfile `package-lock.json` is committed.

## Layout
```
site.edn  stock.edn     owners edit these (validated at build time)
images/                 owners' item photos, published as /images/
static/                 hand-written site files, copied to the site root
  index.html            the shell; {{tokens}} are filled from site.edn at build time
  css/main.css          all styles
  art/                  web-sized card/flyer/badge (WebP), logos, og-card.jpg
  favicon.svg, favicon-32.png, apple-touch-icon.png
source-art/             big originals + logo SVGs. NEVER published
src/couplea/
  core.cljs             page: nav, hero, makers, pinned-up board, find us, app root
  shelves.cljs          filters + item cards (in stock first, sold out last)
  order.cljs            custom-order form + Discord webhook
  icons.cljs            original line-art icons (5 categories + small marks)
  ui.cljs               phone copy button, rule, helpers
  data.cljs             site/stock/build-date baked in by macros
  macros.clj            compile-time macros (fail the build on bad data)
  validate.clj          plain-English validation of the .edn files
  build.clj             check, dev hook (+ .edn watcher), finish (assemble dist/)
scripts/build.mjs       npm run build (check, release, finish)
scripts/serve.mjs       tiny preview server; 3rd arg pretends a sub-folder: `node scripts/serve.mjs dist 8082 /repo/`
scripts/make-web-images.mjs   one-off: `npm i --no-save sharp`, then run it to regenerate static/art + favicons
public/  dist/          generated, gitignored
```

## Look
- Colours: background `#0e0e0d`, text/line art `#d6c1a8` (cream), highlight `#efdbc4`, worn brown edges `#8f7660`. Small brown text uses `#aa917a` for contrast. Errors on cream paper: `#7a1f13`.
- Fonts (Google, max 3): **Permanent Marker** (big headings and prices only, large sizes), **Patrick Hand SC** (small labels), **Source Sans 3** (body).
- Logo is recoloured with a CSS `mask` of `art/logo-white.svg` (so it shows in cream). `logo-black.svg` is the faint watermark on the cream order slip.
- Texture is all CSS/SVG data URIs (`--distress`, `--patchy`, `--grain`, `--wood` in `main.css`). No big texture images. Distress only on big headings.
- Motion is small (paper lift on hover, candle flicker) and off under `prefers-reduced-motion`.
- Mobile first: no sideways scroll, tap targets at least 44px, inputs 16px+.

## Gotchas learned the hard way
- **shadow-cljs does not watch `.edn` files.** `build.clj` runs a small watcher (dev only) that touches `data.cljs` when `stock.edn`/`site.edn` change.
- **Release cache goes stale after `.edn` edits.** `scripts/build.mjs` deletes `.shadow-cljs/builds/app/release` first. Don't remove that.
- Anything sent to Discord goes through **`clj->js`** so advanced compilation can't rename keys. Always test the **release** build, not just dev.
- Strip a leading BOM when reading `.edn` (some Windows editors add one). Done in `validate.clj`.
- Write files as **UTF-8 without BOM**. In Windows PowerShell 5.1 `Set-Content -Encoding UTF8` adds a BOM; use `[IO.File]::WriteAllText`.
- Reagent renders on `requestAnimationFrame`. In a hidden/background browser pane rAF may not fire, so scripted form tests need a screenshot between steps.
- Order form: honeypot field `website` (filled = fake success, send nothing), 3-minute cooldown in `localStorage` (try/catch, plus an in-memory fallback), button disabled while sending, `allowed_mentions` blocks every ping except the optional role from `site.edn`. No webhook set = **test mode** (logs payload, shows success plus a small test-mode note).

## Check before calling it done
`npm run build` passes (release, advanced). Break `stock.edn` on purpose and confirm the
build fails with a clear message, then restore. Submit the form in test mode and
check the console payload. Screenshot at 375px and 1280px, check there is no sideways
scroll, every image has alt text, and focus is visible.
