// One-off helper: makes the web-sized copies of the art in source-art/.
// The big originals stay in source-art/ and are NEVER published.
//
// Run it only when the art changes:
//   npm install --no-save sharp
//   node scripts/make-web-images.mjs
//
// It writes into static/art/ (these small files ARE published).

import sharp from "sharp";
import { mkdir, copyFile } from "node:fs/promises";

const SRC = "source-art";
const OUT = "static/art";
await mkdir(OUT, { recursive: true });

const webp = { quality: 74, alphaQuality: 80, effort: 6 };

// card, flyer and badge: a full-size copy (max 1600 wide) and a small one for the board
const art = [
  { name: "card", full: 1600, thumb: 640 },
  { name: "flyer", full: 1600, thumb: 560 },
  { name: "badge", full: 1600, thumb: 480 },
];
for (const { name, full, thumb } of art) {
  const src = `${SRC}/${name}.png`;
  await sharp(src).resize({ width: full, withoutEnlargement: true }).webp(webp).toFile(`${OUT}/${name}.webp`);
  await sharp(src).resize({ width: thumb, withoutEnlargement: true }).webp(webp).toFile(`${OUT}/${name}-thumb.webp`);
}

// Link-preview image: the card, centred on the site's dark background, 1200x630.
const card = await sharp(`${SRC}/card.png`).resize({ height: 540, fit: "inside", width: 1040 }).toBuffer();
await sharp({ create: { width: 1200, height: 630, channels: 3, background: "#0e0e0d" } })
  .composite([{ input: card, gravity: "center" }])
  .jpeg({ quality: 84, mozjpeg: true })
  .toFile(`${OUT}/og-card.jpg`);

// The logos are used as-is (colour is set with CSS).
await copyFile(`${SRC}/couplea-cryptids-logo-white.svg`, `${OUT}/logo-white.svg`);
await copyFile(`${SRC}/couplea-cryptids-logo-black.svg`, `${OUT}/logo-black.svg`);

// Favicons from the simple hand-drawn mark in static/favicon.svg
await sharp("static/favicon.svg", { density: 600 }).resize(32, 32).png().toFile("static/favicon-32.png");
await sharp("static/favicon.svg", { density: 600 }).resize(180, 180).png().toFile("static/apple-touch-icon.png");

console.log("Done. Check static/art/ and the favicon files.");
