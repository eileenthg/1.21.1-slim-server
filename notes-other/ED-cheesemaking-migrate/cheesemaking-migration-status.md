---
name: cheesemaking-migration-status
description: What is built and what is still unverified in the ED -> BnC cheesemaking migration
metadata: 
  node_type: memory
  type: project
  originSessionId: d4ca1cf1-a5aa-4b8f-8f29-73c9962790ce
  modified: 2026-09-05T16:28:52.184Z
---

State as of 2026-09-06. Design rationale lives in [[cheesemaking-migration-decisions]].

**Built** — BnC branch `feature/goat-cheese`, 23 Java files + 37 asset/data files:
goat_cheese and goat_milk fluids (common + both loader impls + NeoForge fluid types),
unripe/ripe wheel blocks, wedge item and food properties, fluid + item + block tags,
fermenting/pouring/crafting/cutting-board datagen, block loot, slicing loot modifier,
advancement criterion, JEI + EMI entries, lang, blockstates/models, and 16 textures
recolored from the flaxen set by hue/saturation shift.

ED branch `feature/cheesemaking-to-bnc` is **one file**: `EDItemTags.java`, repointing
`c:foods/cheese` and `c:foods/goat_cheese` at BnC's wedges via `addOptional`.

Item Obliterator config at `config/item_obliterator.json5` in the working dir (not in
either repo) — blacklists ED's 5 cask blocks + 4 cheese products. Deliberately leaves
goat milk and ED's cheese-consuming foods alone.

**Committed:** BnC `a8c3757` on `feature/goat-cheese` (77 files). ED `70281d4` on
`feature/cheesemaking-to-bnc` (3 files). Neither pushed.

**Verified:** BnC `:neoforge:compileJava`, `:fabric:compileJava` and `:neoforge:runData`
all pass. ED's module compiles (warnings only).

**Not yet verified:** nothing has been run in-game.

**Resolved:** the fermenting recipe id is `brewinandchewin:fermenting/goat_cheese_from_goat_milk`,
matching what the EMI defaults file already expects.

**Caught during review:** the goat recipe originally had 2 ingredients, and
KegFermentingRecipeBuilder pads a fixed list of 4 with Ingredient.EMPTY, emitting `[]`
slots that no other BnC recipe has. Datagen writes JSON but never parses it back, so it
reported success regardless; the recipe could have failed to load at runtime. Fixed by
giving it 4 real ingredients (honeycomb, wheat seeds, sugar, rennet). Keep every
fermenting recipe at exactly 4 ingredients.

**ED datagen is broken upstream:** `runData` fails with
`ParsingException: Invalid bare key: '${mod_id}'`. `src/main/templates/` does not exist so
`generateModMetadata` emits nothing, and the tracked raw
`src/main/resources/META-INF/neoforge.mods.toml` is read instead. Pre-existing, not caused
by this work. ED's two generated cheese tag JSONs were therefore hand-synced to match
ItemTagsProvider's `addOptional` output byte for byte.

**Build gotcha on this machine:** the page file is manually pinned at ~4.9 GB with
automatic management OFF, capping commit at 36.6 GB. A default Gradle run dies in
NeoForm `mergeMappings` with "The paging file is too small". Workaround that worked:
`--no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m`. Also: never pipe gradlew
through `tail` — the shell then reports the pipe's exit code, not Gradle's.
