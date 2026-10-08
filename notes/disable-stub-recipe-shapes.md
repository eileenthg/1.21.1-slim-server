---
name: disable-stub-recipe-shapes
description: neoforge:false recipe stubs must still match the real recipe type's field shapes, because Farmer's Contracts parses raw recipe JSON and ignores conditions
metadata:
  type: feedback
---

When disabling a recipe with a `neoforge:false` condition stub, the stub must
still use the **field shapes of the recipe type it overrides** — not the vanilla
shaped/shapeless shape for everything.

- `farmersdelight:cutting` → `result` is an **array** of chance results:
  `"result": [ { "item": { "id": "...", "count": 1 } } ]`
- `minecraft:crafting_shapeless` → `ingredients` must be an **array**
- `minecraft:smelting` / `smoking` / `campfire_cooking` / `blasting` → safe
  either way

**Why:** Farmer's Contracts' `RecipeCostSolver` walks the raw `recipe/` directory
in its own reload listener and **never evaluates `neoforge:conditions`**. It also
calls `getAsJsonArray` with no `isJsonArray` guard, and `reload()` has no
per-file try/catch. So a stub the game correctly ignores still gets parsed, and a
wrong-shaped field is a hard crash — on a dedicated server, "Failed to load
datapacks, can't proceed with server load". This bit us on 2026-09-20 via two
cutting stubs in [[squid-calamari-dedup]]; see
`modpack-audit/24-farmers-contracts-cutting-crash.md`.

**How to apply:** before shipping any disable stub, open the original recipe in
the source mod jar and copy its field shapes. When a datapack-load crash points
at a mod's own recipe scanner, suspect our own override stubs — not the mod the
recipe belongs to, and not whichever mod merely shares the genre. Other packs
that use this stub pattern: `recipesplus-dead-compat-off`, [[item-merge-pipeline]]'s
`zz-unified-items`.
