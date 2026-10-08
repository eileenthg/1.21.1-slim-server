# Guide Library (NeoForge 1.21.1)

A button in your inventory that opens your personal library of guide books. A book is added once you
have had a physical copy of it in your inventory. After that you can read it from the library any
time, even after you've stored, lost or thrown away the copy. Click a book to open it in its own GUI.

Built from the ideas in [Guide Tome](https://github.com/otectus/guide-tome) (MIT, see
`LICENSE-guide-tome`), made to work like [PatchouliButton](https://www.curseforge.com/minecraft/mc-mods/patchoulibutton):
- No tome item, no book absorption, and nothing is taken from your inventory.
- Books unlock by being found. The library shows "found / total" so there is something to collect.

Jar: `build/libs/guidelibrary-1.21.1-1.0.0.jar`. Build with `gradlew build`.

## Unlocking
- **What unlocks a book:** a physical copy anywhere in your inventory. The server checks once a second, and at once when you pick an item up. Crafted, looted, quest-reward and `/give` copies all count.
- **Saving:** unlocks are stored on the player on the server (a data attachment). They survive death and relogging, and are synced to your client. You see a message above the hotbar when a book is added (client option `showUnlockMessage`).
- **Item books:** the server only opens one for a player who has unlocked it.
- **Server without the mod:** the client tracks unlocks itself, in `config/guidelibrary-unlocks/<server>.json`.
- **Commands (op level 2, handy as quest rewards):**
  - `/guidelibrary unlock <players> <book>`
  - `/guidelibrary lock <players> <book>`
  - `/guidelibrary list [player]` (any player can use it on themselves)
  - `/guidelibrary books`

  `<book>` is a key, and `*` patterns work (e.g. `patchouli/*`, `*`).
- **Config:**
  - `unlockMode = "EVERYTHING"` brings back list-everything-from-the-start
  - `alwaysUnlocked` hands out specific books, e.g. the pack's own guide
  - the `buttonOpensBook` book always counts as unlocked

## Where books come from (automatic, any pack)
| Source | How it is found | How it opens |
|---|---|---|
| Patchouli | every book in Patchouli's registry | Patchouli API, client side |
| Modonomicon | every book the server synced | Modonomicon's GUI manager, client side |
| Item books | `#guidelibrary:guide_books` tag + `extraBookItems` config | replays the item's right-click (see below) |

The tag ships with known guide items: MCA's 11 books, Alex's Mobs' Animal Dictionary, Tinkers' books, the IE manual, Integrated Dynamics, EvilCraft, Ars Nouveau's Worn Notebook and Occultism. Each entry is optional, so a missing mod is fine. If an item is already a Patchouli or Modonomicon book, it is listed only once.

The mod never guesses from item names. In this pack "book"/"tome" matches ~70 items, and almost all of them are spell tomes (Eternal Tales, Apothic Enchanting, Quark). Clicking one of those would *use* it.

### Item books that the server opens
Some books (MCA, vanilla-style written books) are opened by the server. The client then shows "the book in your main hand". For these:
1. The client asks the server, which replays the right-click and puts the player's own item back straight away.
2. The client shows the book in the selected slot for up to 2 seconds, until the screen appears.

The server only accepts items on the allowlist, and at most once every 10 ticks, so a modified client can't use this to "use" arbitrary items.

The network channel is optional. With the mod on the client only, Patchouli and Modonomicon books still work, and server-opened item books show a message instead.

## Using it
- **Inventory button** sits right of the recipe-book button. It also shows on the creative Inventory tab.
- **Keybind** "Open Guide Library" is unbound by default. Set it in Controls.
- **In the library:**
  - left-click opens a book
  - right-click favorites it (favorites move to the top)
  - type to search by title or mod
  - Enter opens the top result
- **F3+H** adds each book's key (e.g. `patchouli/townstead:guide`) to its tooltip. Use these keys in the config.

## Config
`config/guidelibrary-common.toml` (pack author):
- `extraBookItems`: more item books, e.g. `"somemod:guide_*"`
- `hiddenBooks`: e.g. `"patchouli/apotheosis:apoth_chronicle"`, `"item/mca:book_supporters"`, `"*/somemod:*"`
- `featuredBooks`: pinned to the top in this order, labelled "Pack guide"
- `buttonOpensBook`: the button opens this one book directly (PatchouliButton's pack-guide mode); shift-click still opens the library

`config/guidelibrary-client.toml` (player):
- show/hide the button, in survival and creative
- button position offsets
- the button's icon item

Favorites are saved in `config/guidelibrary-favorites.json`.

## What it found in this pack (scan of the 468 jars, 19 Sep)
- **Patchouli:**
  - Apotheosis Chronicle
  - Arcadian Dream rituals
  - Butchers Guide
  - Extra Alchemy
  - Hot Bath
  - Touhou Little Maid
  - Townstead
- **Modonomicon:** Spectrum's Colorful World
- **Items:** MCA's 11 books, Alex's Mobs' Animal Dictionary

Datapack Patchouli books show up too.

## Testing
`../test-client/run-guidelibtest.ps1` runs a scripted client on a throwaway copy of New World (1):
- clicks the button
- opens every book and logs which screen opened
- checks that the hotbar item comes back
- starts from an empty library, then gets 3 physical books (Patchouli, MCA, Spectrum) and checks they unlock
- sends forged requests for a locked book and for an ender pearl, both of which the server should refuse
- unlocks everything with the command
- takes screenshots of the creative tab

Output is `gltest-*.png` in the instance's screenshots folder, and `GLTEST` lines in `test-client/gltest-latest.log`. It needs Minecraft closed and takes about 15-20 minutes.
