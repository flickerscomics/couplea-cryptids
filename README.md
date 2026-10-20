# Couple'a Cryptids: the website

Our shop window on GTA World. It shows what's on the shelves, who makes it,
how to ask for a custom order, and how to reach us. There's no cart and no
checkout. The real deal happens in-game.

You never have to touch code. Two files hold everything you'd want to change:

| File | What's in it |
|---|---|
| **`stock.edn`** | The items on the shelves |
| **`site.edn`** | Phone, location, hours, bios, the site's web address |

Edit them right on the GitHub website, on your phone is fine. When you save,
the site rebuilds by itself and is live in about 2 to 3 minutes.

---

## Change what's on the shelves (from your phone)

1. Open the repo on GitHub (in the GitHub app or in your browser).
2. Tap **`stock.edn`**.
3. Tap the **pencil** icon (top right) to edit.
4. Make your change (see below).
5. Tap **Commit changes**, then **Commit changes** again.
6. Wait 2 to 3 minutes. Refresh the site.

### Add an item

Find an item you like, from its `{` down to its `}`. Copy the whole thing.
Paste it just above the last `]` at the bottom of the file, then change the words:

```clojure
 {:name        "Smoked Beef Jerky"
  :description "Smoked slow. Cut thick."
  :price       300
  :category    :jerky
  :maker       :hunter
  :in-stock    true
  :tag         "Fresh batch"}
```

| Line | What to put |
|---|---|
| `:name` | The item's name, in "quotes" |
| `:description` | One or two short sentences, in "quotes" |
| `:price` | Whole dollars, no `$`, no quotes, like `300` or `1250`. Write `:ask` to show "Ask us" |
| `:category` | `:jerky` `:apples` `:wood` `:candles` or `:handmade`. Pickles and snacks go under `:jerky` |
| `:maker` | `:buck` or `:hunter` or `:both` |
| `:in-stock` | `true` or `false` |
| `:tag` | Optional little stamp in "quotes". Leave the line out for none |
| `:image` | Optional photo. See "Add a photo" below |

Keep the colons (`:`), the quotes (`"`) and the brackets exactly as they are.

### Mark something sold out

Change `:in-stock true` to `:in-stock false`. It slides to the end of the
shelf with a **SOLD OUT** stamp. Change it back to `true` when you've got more.

### Edit or remove an item

- **Edit:** change the words inside the quotes, or the number after `:price`.
- **Remove:** delete the item from its `{` to its `}`.
  (Or put `;` at the start of each line to hide it for now.)

### The shelves look empty?

If everything is sold out (or the list is empty) the site says
"Shelves are bare. Check back soon." That's on purpose.

---

## Change the phone, location, hours or bios

Same steps, but open **`site.edn`**. Anything that looks like `[[ LIKE THIS ]]`
is a placeholder waiting for you. Replace the whole thing, brackets and all.

The important one is **`:site-url`**: it's the web address of the live site
(it looks like `https://yourname.github.io/reponame/`). Discord uses it to build the
link preview when someone pastes the site into a channel.

To ping a Discord role on every custom order, put the role's ID number in
quotes after `:discord-role-id`. Leave it `nil` for no ping.

---

## Add a photo to an item

1. On GitHub, open the **`images`** folder.
2. Tap **Add file**, then **Upload files**, and pick your photo.
   Name it with small letters and no spaces, like `jerky.webp` or `pine-candle.jpg`.
   About 800 pixels wide is plenty. Big phone photos slow the page down.
3. Tap **Commit changes**.
4. Open **`stock.edn`**, and add a line to that item:
   ```clojure
   :image "images/jerky.webp"
   ```
5. Commit. Done.

Capital letters matter: `Jerky.webp` and `jerky.webp` are different files.
No photo? The site shows a hand-drawn picture for that kind of item.

---

## When a build fails

If there's a mistake in `stock.edn` or `site.edn`, the build **stops** and the
**live site stays exactly as it was**. Nothing breaks for visitors. You just
don't see your change yet.

**What it looks like:** GitHub emails you ("Build and deploy: failed"), and in the
**Actions** tab (top of the repo page) your latest run has a red ✗.

**How to find out what's wrong:**

1. Open the **Actions** tab.
2. Tap the run with the red ✗.
3. Tap **build**, then open the step called **npm run build**.
4. Look for the box that says **BUILD STOPPED**. It names the item and the problem:

```
 * Item 3 ("Pine Candle"): :maker is :hunterr, which isn't one we know.
   Use one of: :buck :hunter :both. Did you mean :hunter?
```

**How to fix it:** open the file, fix that line, commit again. The build runs again by itself.

Common mistakes:

| Message says | What happened |
|---|---|
| `is missing :price` | You left a line out. Add it back |
| `isn't one we know` | A typo in `:maker` or `:category`. It suggests the right word |
| `has a field :instock that we don't use` | Typo in a line's name. It should be `:in-stock` |
| `has a typo near line 44` | A missing quote `"`, bracket `]` or brace `}` |
| `the photo "images/x.webp" isn't in the images folder` | Upload the photo first, or fix the spelling |
| `extra content after the closing bracket` | An item got pasted below the last `]` |

**Panic button:** open `stock.edn`, tap **History**, find the last good version and
use the **...** menu to view it, copy it, then paste it back into the file.

---

## Custom orders and Discord

The order form posts a tidy message to a Discord channel using a **webhook**.
The webhook address is kept as a GitHub secret called `DISCORD_WEBHOOK_URL`
(Settings → Secrets and variables → Actions). It is never saved in the files.

> **Heads up:** a website has no private back room, so the webhook address still
> ends up inside the published site. Someone determined could dig it out and
> post to that channel. The form has a spam trap, a 3-minute wait between
> orders and blocks every `@` ping, but use a channel you don't mind getting
> spam in. If the address ever leaks, delete that webhook in Discord, make a new
> one, and update the secret.

**No secret set?** The form runs in **test mode**: it sends nothing, prints what
it *would* have sent to the browser's console, and shows a small
"Test mode: nothing was actually sent to Discord." under the thank-you.
If you ever see that on the live site, the secret is missing or misspelled.

---

## For the curious: running it on a computer

You need **Java 21** and **Node 24** installed.

```bash
npm install          # once
npm run dev          # live-reloading preview at http://localhost:8080
npm run check        # just check stock.edn and site.edn
npm run build        # the real build, into dist/
npm run preview      # look at dist/ at http://localhost:8081
```

Set the `DISCORD_WEBHOOK_URL` environment variable before `npm run build`
to try the real webhook; leave it out for test mode. More notes for
developers are in `NOTES.md`.

---

*Couple'a Cryptids is a roleplay business on GTA World. All names, art and
products here are original.*
