// The one command that builds the live site:   npm run build
//
//   1. check   : site.edn and stock.edn are valid (plain-English errors, exit 1)
//   2. release : shadow-cljs advanced-optimised build
//   3. finish  : assemble dist/ (the only folder that gets published)
//
// If any step fails the whole build stops, nothing is deployed, and the
// live site keeps its last good version.

import { spawnSync } from "node:child_process";
import { rmSync } from "node:fs";

function run(label, args) {
  console.log(`\n=== ${label} ===`);
  const r = spawnSync(`npx shadow-cljs ${args.join(" ")}`, { stdio: "inherit", shell: true });
  if (r.status !== 0) {
    console.error(`\nBuild stopped at: ${label}`);
    process.exit(r.status || 1);
  }
}

// Check first. If the .edn files are wrong we stop before touching anything.
run("1/3 Checking site.edn and stock.edn", ["clj-run", "couplea.build/check"]);

// Start clean so nothing from a dev session sneaks into the release.
rmSync("public/js", { recursive: true, force: true });
rmSync("dist", { recursive: true, force: true });

// site.edn and stock.edn are baked into the JavaScript by a macro. shadow-cljs
// only looks at the .cljs files when deciding what changed, so it could reuse
// an old cached copy after an .edn edit. Clearing its release cache means every
// release build reads the .edn files fresh (costs about 10 extra seconds).
rmSync(".shadow-cljs/builds/app/release", { recursive: true, force: true });

run("2/3 Compiling (release, advanced)", ["release", "app"]);
run("3/3 Assembling dist/", ["clj-run", "couplea.build/finish"]);

console.log("\nBuild finished. Try it with:  npm run preview");
