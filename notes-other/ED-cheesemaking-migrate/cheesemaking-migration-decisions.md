---
name: cheesemaking-migration-decisions
description: Design decisions governing the Expanded Delight -> Brewin & Chewin cheesemaking migration
metadata: 
  node_type: memory
  type: project
  originSessionId: d4ca1cf1-a5aa-4b8f-8f29-73c9962790ce
  modified: 2026-09-05T16:06:07.171Z
---

Migrating cheesemaking from Expanded Delight (ED, NeoForge 1.21.1, `ianm1647.expandeddelight`)
into Brewin & Chewin (BnC, multiplatform common/fabric/neoforge, `umpaz.brewinandchewin`).
Repos are cloned side by side under `C:\Users\eteil.000\Claude\ED-cheesemaking-migrate\`.
Work branches: BnC `feature/goat-cheese`, ED `feature/cheesemaking-to-bnc`.

Four decisions the user made on 2026-09-05, none of them recoverable from the code:

1. **ED's plain "Cheese" is folded into BnC's existing Flaxen Cheese** rather than added as a
   new type — a third generic cow cheese would recreate the very overlap being removed.
   Only **Goat Cheese** is genuinely new and gets built out in BnC.
2. **Production is BnC's Keg only.** ED's Cask chain (cask + milk bucket -> milk cask ->
   random-tick `cheese_fermenting` 0..7 -> cheese cask -> right-click for 2 wheels) is NOT
   ported. Goat cheese follows BnC's pipeline: ferment -> cheese fluid -> pour into unripe
   wheel -> ages -> cut into wedges.
3. **Goat milk bridges from ED via tag, it is not reimplemented in BnC.** BnC gained a
   `brewinandchewin:goat_milk` fluid; a NeoForge-only pouring recipe gated on
   `neoforge:mod_loaded expandeddelight` fills it from `expandeddelight:goat_milk_bucket`.
   Consequence the user accepted: BnC's goat cheese is unobtainable without ED installed.
4. **ED is disabled by Item Obliterator config, not by deleting code.** Chosen for
   reversibility. Config lives at `config/item_obliterator.json5` in the working dir.

The load-bearing discovery that made this cheap: **every ED cheese-consuming recipe takes
cheese through the `c:foods/cheese` / `c:foods/goat_cheese` tags, never a concrete item.**
So repointing those two tags in `EDItemTags.java` at BnC's wedges migrates all of ED's cheese
foods at once with zero recipe rewrites. Blacklisting `expandeddelight:cask` is likewise what
actually severs ED's chain, since every other cask state is only reachable through it.

BnC recipes/tags/loot are **datagen-generated** — edit the providers under
`neoforge/src/main/java/umpaz/brewinandchewin/data/`, never `common/src/generated/`.

See [[cheesemaking-migration-status]] for what is built vs still open.
