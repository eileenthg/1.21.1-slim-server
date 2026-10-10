# Duplicate wild crops and worldgen features

Method: read every `data/*/neoforge/biome_modifier/*.json`, `worldgen/placed_feature/*.json` and `worldgen/configured_feature/*.json` out of all 396 jars — **1,098 biome modifiers, 1,352 placed features, 1,336 configured features**, of which **821 modifiers are `neoforge:add_features`**. For each modifier I collected every namespaced id it references, following string refs into placed/configured features *and* reading inline feature objects, then grouped by a normalised crop token.

The inline part matters. Extra Delight's soybean modifier has no block id and no feature reference at all:

```json
{
  "type": "neoforge:add_features",
  "biomes": "#c:is_temperate",
  "features": {
    "feature": { "type": "extradelight:wild_soybean", "config": { ... } },
    "placement": [ { "type": "minecraft:rarity_filter", "chance": 80 }, ... ]
  }
}
```

The crop identity lives in the **code-registered feature type** (`extradelight:wild_soybean`), inline inside the modifier. A first pass that only resolved string references to configured features found *zero* duplicates — which is how I know that approach was wrong.

---

## Duplicated wild crops

| Crop | Mods | Biome modifiers to choose between |
|---|---:|---|
| **corn** | 4 | `brewinandchewin:add_wild_corn`, `corn_delight:wild_corn`, `culturaldelights:add_corn`, `extradelight:patch_wild_corn` |
| **cucumber** | 3 | `culturaldelights:add_cucumber`, `extradelight:patch_wild_cucumber`, `youkaishomecoming:cucumber` |
| **coffee** | 2 | `extradelight:patch_wild_coffee`, `rusticdelight:add_wild_coffee` |
| **ginger** | 2 | `extradelight:patch_wild_ginger`, `snowyspirit:ginger` + `snowyspirit:ginger_dense` |
| **peanut** | 2 | `expandeddelight:wild_peanuts`, `extradelight:patch_wild_peanut` |
| **soybean** | 2 | `extradelight:patch_wild_soybean`, `youkaishomecoming:soybean` |
| **mandrake** | 2 | `eternal_tales:mandrake_biome_modifier`, `youkaishomecoming:mandrake` |
| **chili** | 2 | `extradelight:patch_wild_chili`, `expandeddelight:wild_chili_pepper` |
| **grapes** | 2 | `brewinandchewin:add_wild_grapes`, `youkaishomecoming:black_grape` + `red_grape` + `white_grape` |

`culturaldelights` (corn, cucumber) and `mandrake` are new finds the earlier partial scan missed entirely.

**chili** and **grapes** did not fall out of the token grouping — `chili` vs `chili_pepper`, and `grape` vs `black_grape`, normalise differently. I confirmed both by hand from the biome-modifier listings. That means the tokeniser has near-miss cases and this table, while much better than the first pass, still may not be exhaustive for crops whose names differ more than a plural.

## Also duplicated (not crops, but same situation)

| Feature | Mods |
|---|---|
| **cinnamon tree** | `expandeddelight:cinnamon_tree`, `extradelight:cinnamon_tree` |
| **palm tree** | `crabbersdelight:add_tree_palm`, `ramadandelight:add_tree_palm` |
| **salt deposits** | `butchery:saltdiscgeneration_biome_modifier`, `eternal_tales:salt_block_biome_modifier` |
| **limestone** | `create:striated_ores_overworld`, `eternal_tales:limestone_biome_modifier` + `limestone_geobstractsite_feature_biome_modifier` |
| **basalt / ash** | `supplementaries:basalt_ash`, `eternal_tales:` sulfur and volcanic geyser modifiers |

### False positives, ignore these

- **orange / lime "trees"** — `extradelight:orange_tree` and `lime_tree` vs `spectrum:colored_tree_patch`. Spectrum's are *dye-coloured* trees; "orange" and "lime" are colour names there, not fruit. Not a duplicate.
- Structural tokens the grouping picked up because they are shared API types, not content: `biome_tag`, `crop`, `features_by_filter`, `config_filter`, `configurable_rarity_filter`, `rng_initializer`, `sandy_shrub`, `palm_foliage_placer`.

---

## How to unify — no mod required

NeoForge registers a no-op biome modifier. Verified in your `neoforge-21.1.249-universal.jar`: `NeoForgeMod` calls `BIOME_MODIFIER_SERIALIZERS.register("none", …)` and `NoneBiomeModifier.class` is present. So the type id is **`neoforge:none`**.

A datapack that ships a file at the loser's exact resource location overrides the mod's own copy:

`data/youkaishomecoming/neoforge/biome_modifier/soybean.json`
```json
{ "type": "neoforge:none" }
```

That cancels the injection entirely — same override mechanism as the Recipes Plus dead-compat pack, and it goes on the Simple Datapacks path alongside the others.

**Caveat:** this only stops *new* chunk generation. Wild crops already placed in explored chunks stay where they are.

---

# Block-by-block comparison

Resolved each modifier's target biomes (following mod-specific biome tags) and the actual wild block, model and texture.

## Real collisions

### corn — 4-way, the worst one

| Mod | Biomes | Wild block | Model / texture |
|---|---|---|---|
| `brewinandchewin` | `#c:is_plains` + `nomansland:prairie` | `wild_corn` | **2 blocks tall** (`wild_corn_lower` + `_upper`), cross |
| `extradelight` | `#c:is_plains` | `wild_corn` | single cross |
| `culturaldelights` | plains, sunflower_plains, snowy_plains, savanna_plateau | `wild_corn` | single cross |
| `corn_delight` | *filter-based* (`farmersdelight:add_features_by_filter`, `biomes: null`) | `wild_corn` | single cross |

All four land in plains. Brewin' and Chewin's is the only two-tall one, so it reads differently from the rest.

### coffee

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `extradelight` | `#minecraft:is_jungle` | `coffee_bush` (4 stages, no separate wild block) | `crops/coffee/coffee_bush_stage0` |
| `rusticdelight` | `#minecraft:is_jungle` + `#c:is_jungle` | `wild_coffee` (+ `potted_wild_coffee`) | Farmer's Delight `template_crop_cross` |

Direct jungle overlap. Rustic Delight is the only one with a true wild variant *and* a potted version.

### ginger

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `extradelight` | `#minecraft:is_jungle` | `wild_ginger` | reuses farmed `crops/ginger/ginger_stage3` |
| `snowyspirit` | `#minecraft:is_jungle` | `wild_ginger` | dedicated `block/wild_ginger` |
| `snowyspirit` (dense) | `minecraft:sparse_jungle` | `wild_ginger` | same |

Both in jungle despite the name — Snowy Spirit's ginger is not a snowy feature.

### mandrake

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `eternal_tales` | **`#minecraft:is_overworld`** — every overworld biome | `mandrake` | `block/mandr` |
| `youkaishomecoming` | dark_forest, mushroom_fields | `wild_mandrake` | `plants/mandrake/wild_mandrake` |

Eternal Tales' spawns everywhere, so it overlaps Youkai's entirely and blankets the rest of the world besides.

### chili

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `extradelight` | `#c:is_hot` | `wild_chili` | `crops/chili/wild_chili` |
| `expandeddelight` | `#c:is_desert` | `wild_chili_pepper` | `block/wild_chili_pepper` |

Desert is a subset of hot, so they collide in deserts.

### grapes

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `brewinandchewin` | `#c:is_cold` + `#c:is_temperate` | `wild_grapes` | `block/wild_grapes` |
| `youkaishomecoming` | `#minecraft:is_taiga` + `sakura_taiga` | `wild_black_grape`, `wild_red_grape`, `wild_white_grape` | `plants/grape/wild_*_grape` |

Taiga is cold, so they overlap there. Youkai's is three colour variants against one generic.

### soybean — partial

| Mod | Biomes | Wild block | Texture |
|---|---|---|---|
| `extradelight` | `#c:is_temperate` | `wild_soybean` | **reuses farmed `crops/soybeans/soybeans_stage7`** |
| `youkaishomecoming` | dark_forest, `#minecraft:is_jungle`, swamp, mangrove_swamp, `#youkaishomecoming:youkai_biomes` | `wild_soybean` | dedicated `plants/soybean/wild_soybean` |

Overlap is partial — dark forest and swamp fall under `c:is_temperate`, jungle does not. Youkai's has purpose-drawn wild art; Extra Delight reuses stage 7 of its farmed crop.

## NOT actually collisions — leave alone

### cucumber — three mods, three disjoint biome sets

| Mod | Biomes |
|---|---|
| `culturaldelights` | swamp, mangrove_swamp |
| `extradelight` | `#c:is_hill` |
| `youkaishomecoming` | `#minecraft:is_taiga` + `sakura_taiga` |

No overlap. Worth noting Youkai's cucumber is by far the most elaborate — a rope crop with 32 models.

### peanut — disjoint

| Mod | Biomes |
|---|---|
| `expandeddelight` | `#c:is_savanna` |
| `extradelight` | `#minecraft:is_jungle` |

No overlap.

---

# Applied — `datapacks/wild-crop-unification.zip`

Rule chosen: **best-looking wins** — per crop, keep the version with dedicated wild art, drop the rest. All 9 loser paths were verified present in their jars before generating the overrides (9/9).

| Crop | Kept | Disabled | Original type of the disabled modifier |
|---|---|---|---|
| soybean | `youkaishomecoming:soybean` | `extradelight:patch_wild_soybean` | `neoforge:add_features` |
| corn | `brewinandchewin:add_wild_corn` | `corn_delight:wild_corn` | `farmersdelight:add_features_by_filter` |
| corn | ” | `culturaldelights:add_corn` | `neoforge:add_features` |
| corn | ” | `extradelight:patch_wild_corn` | `neoforge:add_features` |
| coffee | `rusticdelight:add_wild_coffee` | `extradelight:patch_wild_coffee` | `neoforge:add_features` |
| ginger | `snowyspirit:ginger` + `ginger_dense` | `extradelight:patch_wild_ginger` | `neoforge:add_features` |
| mandrake | `youkaishomecoming:mandrake` | `eternal_tales:mandrake_biome_modifier` | `neoforge:add_features` |
| chili | `expandeddelight:wild_chili_pepper` | `extradelight:patch_wild_chili` | `neoforge:add_features` |
| grapes | `youkaishomecoming:black/red/white_grape` | `brewinandchewin:add_wild_grapes` | `neoforge:add_features` |

Note Brewin' & Chewin' wins corn but loses grapes — separate modifiers, no conflict in doing both.

`corn_delight:wild_corn` was the one non-`neoforge` type (`farmersdelight:add_features_by_filter`). Overriding replaces the whole file, so its type becomes `neoforge:none` regardless of what it was — the mechanism is type-agnostic.

**Deliberately untouched:** cucumber and peanut (disjoint biomes, never actually collided), and the non-crop duplicates — cinnamon tree, palm tree, salt, limestone, basalt/ash.

## Reverting

Delete the zip, or delete individual files inside it to bring back specific crops. Nothing in the mods folder or `config/` was modified.

## Caveat

Only affects **newly generated chunks**. Wild crops already placed in explored terrain stay where they are.

---

# Beyond crops: trees, ores, and why trees are different

## Trees are wood types, not decorations

Disabling is **size-agnostic** — cancelling a biome modifier removes the whole tree regardless of block count. Multi-block only matters if you want to *merge* two trees (rewrite configured features so one places the other's logs/leaves), which is genuinely hard and unnecessary here.

The real obstacle is different. Each duplicate tree ships a **complete wood set**:

- `expandeddelight` cinnamon — 24 items: boat, button, cabinet, chest_boat, door, fence, fence_gate, hanging_sign, leaves, log, planks, pressure_plate, sapling, sign, slab, stairs, trapdoor, wood, stripped variants, plus `cinnamon` and `cinnamon_stick`
- `extradelight` cinnamon — 50+ items: the same wood set plus counter cabinets, food displays, knife blocks, spice racks, sinks, step stools, 16 wallpaper colours, `cinnamon_bark`, `raw_cinnamon`, `ground_cinnamon`

**And the sapling drops only from its own leaves.** Verified by scanning every recipe and loot table in all 396 jars:

| Sapling | Sources |
|---|---|
| `extradelight:cinnamon_sapling` | own leaves only — 0 recipes, 0 chest loot |
| `expandeddelight:cinnamon_sapling` | own leaves only — 0 recipes, 0 chest loot |
| `crabbersdelight:palm_sapling` | own leaves only |
| `ramadandelight:palm_sapling` | own leaves **+ `chests/rd_village_desert_house`** |

So disabling a tree's worldgen makes that entire build palette **permanently unobtainable in survival** — except Ramadan Delight's palm, which has a village-chest fallback. That is categorically worse than losing a wild crop, where you only lose one plant.

Almost Unified does not bridge them either: `grep` over `config/almostunified` finds **cinnamon 0, palm 0, limestone 0, topaz 0**, salt 2, sulfur 4, ruby 1.

## Palm — not a conflict at all

| Mod | Biomes | Tree |
|---|---|---|
| `crabbersdelight:add_tree_palm` | `minecraft:beach` | coastal palm |
| `ramadandelight:add_tree_palm` | `#c:is_desert` | **date palm** (`ramadandelight:date_palm_tree`) |

Disjoint biomes and thematically different trees. Leave the worldgen alone. The duplicate `palm_log` / `palm_planks` / `palm_sapling` *items* are an Almost Unified matter, not a worldgen one.

## Cinnamon — a real conflict, but expensive to resolve

| Mod | Biomes |
|---|---|
| `expandeddelight:cinnamon_tree` | `#c:is_jungle` |
| `extradelight:cinnamon_tree` | `#minecraft:is_jungle` |

Same biomes, two full wood types. Options, in increasing order of disruption:

1. **Keep both.** Two cinnamon wood palettes is arguably a feature, not a bug. Costs only visual repetition in jungles.
2. **Unify the items in AU first, then disable one tree.** Add `c:cinnamon`, `c:cinnamon_stick` etc. so recipes keep working — but you still lose the losing mod's planks/stairs/doors as buildable blocks.
3. **Disable one outright.** Cheapest to do, permanently removes a wood set. Not recommended.

## Non-tree duplicates — safer to unify

These carry no build palette, so removing one costs far less.

| Material | Mods | Biomes |
|---|---|---|
| **salt** | `butchery:saltdiscgeneration` | **`{"type": "neoforge:any"}` — every biome, every dimension** |
| | `eternal_tales:salt_block_biome_modifier` | `#is_overworld` |
| | `expandeddelight:salt_ore` | `#c:is_ocean` |
| **sulfur** | `butchery:sulfuroregeneration` | **`neoforge:any`** |
| | `eternal_tales:sulfur_biome_modifier` | ET biomes only |
| **topaz** | `spectrum:topaz_geode` | `#c:is_overworld` |
| | `eternal_tales:topaz_ore_biome_modifier` | ET biomes only |
| **ruby** | `anotherscape:gem_ores_overworld`, `eternal_tales:ruby_geode_feature_biome_modifier` | — |
| **limestone** | `create:striated_ores_overworld`, `eternal_tales:limestone_*` | — |

Butchery uses `neoforge:any` for both salt and sulfur — that is every biome in **every dimension**, Nether and End included. Worth trimming on its own merits, duplicate or not.

## False positives from the fuzzy sweep — do not act on these

Merging tokens by substring caught `chili`/`chili_pepper` and the grape variants correctly, but also produced junk:

- **salt** matched ba**salt** — `eternal_tales:basalt_ore`, `supplementaries:basalt_ash`, `rayana_sal_tree`
- **corn** matched sea_a**corn** — `eternal_tales:sea_acorn_str_feature`
- **lime** matched **lime**stone *and* lime dye — `extradelight:lime_tree`, `spectrum:colored_tree_patch`
- **grap** matched **grap**efruit — `extradelight:grapefruit_tree`
- **orange / blue / green / pink** — `spectrum:colored_tree_patch` (dye colours) and `eternal_tales:present_str_*` (Christmas presents)

Also checked and **not** duplicates despite matching: `farmersdelight:wild_carrots` vs `minersdelight:wild_cave_carrot`, `farmersdelight:wild_potatoes` vs `expandeddelight:wild_sweet_potato`, `farmersdelight:wild_cabbages` vs `eternal_tales:comet_cabbage`.

---

# Spawn locations for the ore/material duplicates

| Material | Mod | Biomes | Height | Mechanic |
|---|---|---|---|---|
| **salt** | `expandeddelight:salt_ore` | `#c:is_ocean` | y −20..60, count 6 | ore, size 9, replaces stone/deepslate replaceables |
| | `eternal_tales:salt_block` | `#is_overworld` (all) | y 10..30, count 10 | ore, size 16, replaces `minecraft:stone` + granite |
| | `butchery:saltdiscgeneration` | **`neoforge:any`** (every dimension) | surface (heightmap), rarity 1/8 | **discs on dirt/clay**, radius 2–3 |
| **sulfur** | `eternal_tales:sulfur` | 5 ET biomes only: ancient_battlefields, ash_dunes, melting_rocks, volcanech_rocks, volcanech_void | y 0..200, count 10 | ore, size 16, **only replaces `eternal_tales:ash`** |
| | `butchery:sulfuroregeneration` | **`neoforge:any`** | y −64..64 trapezoid, count 2 | ore, size 4, stone/deepslate replaceables |
| **topaz** | `eternal_tales:topaz_ore` | ~10 ET biomes (daredian_village, rayana_cliffs/desert/forest/mesa/mushroom_fields/reed_grove/tropics…) | y 50..319, count 16 | ore, size 8, **only replaces `eternal_tales:rajiit`** |
| | `spectrum:topaz_geode` | `#c:is_overworld` | y 82..top, rarity 1/6 | `spectrum:air_check_geode` |
| **ruby** | `eternal_tales:ruby_geode_feature` | `#is_overworld` | y bottom+8..128, rarity 1/10 | **structure geode** `eternal_tales:ruby_geode_1521` |
| | `anotherscape:ore_ruby` | `#minecraft:is_overworld` | y −48..32, count 4 | ore, size 4 |
| **limestone** | `create:striated_ores_overworld` | `#minecraft:is_overworld` | y −30..70, rarity 1/18 | `create:layered_ore` striated patches |
| | `eternal_tales:limestone` | **`neoforge:any`** | y 20..30, count 10 | ore, size 16, targets `eternal_tales:rajiit` **and `minecraft:stone`** |
| | `eternal_tales:limestone_geobstractsite` | `eternal_tales:geobstractsite_biome` only | — | decorative `block_column` pillars |

## What this changes

- **Topaz and sulfur are NOT worldgen conflicts.** Eternal Tales' topaz only replaces `eternal_tales:rajiit` inside ET biomes; its sulfur only replaces `eternal_tales:ash` inside 5 ET biomes. They never appear in the same rock as Spectrum's geodes or Butchery's ore. Merging these is an **Almost Unified item** job, not a biome-modifier one — disabling ET's worldgen would remove topaz from ET's own dimension without making ET topaz usable in Spectrum recipes.
- **Ruby is a real overlap** — both blanket `is_overworld`.
- **`anotherscape:gem_ores_overworld` is one modifier for seven ores**: diamond, emerald, onyx, red_diamond, ruby, sapphire, zenyte. Disabling it would remove all seven, so the only clean cut is Eternal Tales' side (`ruby_geode_feature_biome_modifier`, which is ruby-only).
- **Limestone is a real overlap**, and Eternal Tales' uses `neoforge:any` while targeting `minecraft:stone` — so it generates limestone in *every dimension*, Nether and End included. The `geobstractsite` one is contained and harmless.
- **Salt is three different mechanics** — ocean-floor ore, underground ore, and surface discs — that partially overlap rather than duplicating outright.

# Structure usage — cinnamon and palm

Scanned every structure file in all 396 jars: **9,816 `.nbt` + 12,702 MineColonies `.blueprint` + 18 `.schem` = 22,536 files**.

| Block prefix | Structures using it |
|---|---|
| `extradelight:cinnamon*` | **0** |
| `expandeddelight:cinnamon*` | **0** |
| `crabbersdelight:palm*` | **0** |
| `ramadandelight:palm*` | 2 — its own `village/palm_tree_1.nbt`, `palm_tree_2.nbt` |

No structure anywhere in the pack — including MineColonies blueprints and Domum Ornamentum — depends on either cinnamon wood. Disabling one cinnamon tree is structurally safe.

(Method note: a first pass reported 0 files scanned because the script was named `struct.py`, shadowing Python's stdlib `struct` module that `zipfile` imports. Renamed and re-run.)

# Palm rename — already done upstream

| | Ramadan Delight | Crabber's Delight |
|---|---|---|
| log | **"Date Palm Log"** | "Palm Log" |
| planks | **"Date Palm Planks"** | "Palm Planks" |
| sapling | **"Date Palm Sapling"** | "Palm Sapling" |

Ramadan Delight already prefixes its entire wood set with "Date Palm", and it has `date`, `cluster_of_dates`, `date_syrup`, `dates_branch`. They are already distinguishable in-game — no rename needed. If you want a stronger split, a lang-only resource pack could shorten Ramadan's to "Date Log / Date Planks / Date Wood", or rename Crabber's to "Coconut Palm".

---

# Round 2 — renames, ET worldgen, ruby loot, salt unification

## Which dimension ET's topaz and sulfur live in

| | Dimensions | Biomes |
|---|---|---|
| **topaz** | **`rayana`** + `ultimate_world` | daredian_village, rayana_cliffs / desert / forest / mesa / mushroom_fields / reed_grove |
| **sulfur** | **`volcanech`** + `ultimate_world` | ancient_battlefields, ash_dunes, melting_rocks, volcanech_rocks, volcanech_void |

Neither reaches the Overworld, so both are dimension-gated progression. ET's topaz chain confirms it: `rough_topaz` → *"Requires 6 Archery Skill Level to polish on Jewelry Table"* → `topaz` → `treated_topaz`, plus topaz armour, tools, smithing template and armour trim. Left alone, renamed only.

## Applied

**Resource pack** `resourcepacks/wood-and-gem-renames.zip` (also in `modpack-audit/resourcepacks-out/`), pack_format 34:

- `crabbersdelight` — 21 keys, "Palm X" → **"Coconut Palm X"**
- `eternal_tales` — 7 keys: `topaz` → **Rayana Topaz**, `rough_topaz` → Rough Rayana Topaz, `treated_topaz` → Treated Rayana Topaz, `topaz_ore` / `topaz_block` / `topaz_bricks` → Rayana Topaz …, and `block.eternal_tales.sulfur` → **Volcanech Sulfur**

Scoped deliberately tight. A first pass renamed all 34 sulfur keys and produced things like "Volcanech Sulfur Netherite Brazier"; it also copied through an upstream typo (`sulfur_button` is *"Surfur Button"* in ET's own lang file). Only the base `sulfur` block actually collides with Butchery's "Sulfur", so only that one is renamed. ET has no `item.eternal_tales.sulfur` key — the block item uses the block key.

**Datapack** `datapacks/et-worldgen-ore-trim.zip`:

| File | Effect |
|---|---|
| `eternal_tales/neoforge/biome_modifier/salt_block_biome_modifier.json` | `neoforge:none` — removes ET salt ore from all overworld biomes |
| `eternal_tales/neoforge/biome_modifier/limestone_biome_modifier.json` | `neoforge:none` — removes the `neoforge:any` limestone ore |

**Almost Unified** (`config/almostunified/`, backups `proposed-configs/au-*.BEFORE2`):

- `c:salt` was **already** in the unification tag list, and `butchery` already sits at priority index 8 — above extradelight (9), compatdelight (14), expandeddelight (21) and eternal_tales (29). So Butchery's salt already wins; the only gap was `expandeddelight:salt` being absent from the `c:salt` tag. Added as a custom tag with all five members.
- `c:gems/{material}` was already in the tag list and `ruby` is already in `placeholders.json`, so `c:gems/ruby` was already targeted — but the real tag contained only `eternal_tales:rubygem`. Added a custom tag with `anotherscape:ruby` + `eternal_tales:rubygem`.
- Added **`anotherscape`** to `mod_priorities` **below** `eternal_tales` (last), so **Eternal Tales' ruby wins** the unification. Tag membership is what makes the two unify at all; priority decides which survives.

## Limestone — why the geobstractsite columns stay

`eternal_tales:limestone` is a plain `block/cube` with `k_cave_limestone` on all six faces — **not** a chiselled pillar. So the `block_column` feature could technically be repointed at `create:limestone`.

**But it should not be.** Once `limestone_biome_modifier` is disabled, the geobstractsite columns become the *only* remaining source of `eternal_tales:limestone`, which ET's `limestone_bricks`, `limestone_asphalt` and related sets are built from. Swapping them to Create's limestone would make all of that unobtainable. Leaving them keeps ET limestone available inside its own themed biome while the global every-dimension ore spam is gone.

## Ruby — reverted to Eternal Tales

An earlier revision of this pack overrode `eternal_tales/loot_table/blocks/ruby_cluster.json` so the geode dropped `anotherscape:ruby`. **That override has been removed.** The geode is back to its native behaviour — clusters drop `eternal_tales:ruby_shard` (count 4, fortune `ore_drops`), silk touch yields the cluster.

That also retires the risk the override created: ET's 7 `ruby_shard` recipes are fed normally again, rather than depending on the `ruby_buddy` mob as the only shard source.

Almost Unified matches: `anotherscape` sits last in `mod_priorities`, below `eternal_tales`, so `eternal_tales:rubygem` is the surviving ruby. Anotherscape's ruby ore still generates (its modifier bundles seven ores and cannot be split), but its ruby item unifies into ET's.

---

# Cinnamon — unified, then Expanded Delight's tree disabled

## The equivalence was already defined upstream

`c:dusts/cinnamon` ships containing **both** items:

```
c/tags/item/dusts/cinnamon.json -> ["expandeddelight:cinnamon", "extradelight:ground_cinnamon"]
```

So the mods already agree that Expanded Delight's **"Cinnamon"** is the same thing as Extra Delight's **"Ground Cinnamon"** — not its cinnamon stick. Worth knowing, because the names invite the wrong pairing.

Extra Delight's chain, for reference:

| Step | Source |
|---|---|
| `cinnamon_bark` | `loot_table/misc/cinnamon_log.json` — the bark-stripping mechanic |
| `raw_cinnamon` | from bark / from block |
| `cinnamon_stick` | drying rack ← `raw_cinnamon` |
| `ground_cinnamon` | mortar ← `cinnamon_stick` |

Expanded Delight has just `cinnamon` and `cinnamon_stick`, with no recipes producing them in its own jar.

## Why AU wasn't already unifying it

AU's tag list contains `c:dusts/{material}`, but **`cinnamon` is not in `placeholders.json`'s material list** (which runs aeternium → zinc and covers metals and gems only). So `c:dusts/{material}` never expanded to `c:dusts/cinnamon`, and neither item was being unified despite the tag existing.

## Applied

**Almost Unified** (backups `proposed-configs/au-*.BEFORE3`):

| Change | Detail |
|---|---|
| `custom_tags` 187 → 189 | `c:dusts/cinnamon` = `[extradelight:ground_cinnamon, expandeddelight:cinnamon]`; `c:cinnamon/stick` = `[extradelight:cinnamon_stick, expandeddelight:cinnamon_stick]` |
| `unification.tags` 172 → 174 | added `c:dusts/cinnamon`, `c:cinnamon/stick` so AU actually processes them |

`c:cinnamon/stick` previously contained only `extradelight:cinnamon_stick`, so the custom tag is what brings Expanded Delight's in.

**Winner:** `extradelight` sits at priority index 9, `expandeddelight` at 21 — Extra Delight takes both, matching the decision to keep the mod with the bark mechanic.

**Datapack** `datapacks/cinnamon-tree-dedup.zip` — overrides `expandeddelight:cinnamon_tree` with `neoforge:none`. Original was:

```json
{ "type": "neoforge:add_features", "biomes": "#c:is_jungle",
  "features": "expandeddelight:cinnamon_tree", "step": "vegetal_decoration" }
```

## What survives, what doesn't

**Survives.** Expanded Delight's two cinnamon foods, `cinnamon_apples` and `cinnamon_rice`, are cooking recipes taking `expandeddelight:cinnamon`. AU now rewrites that input to `extradelight:ground_cinnamon`, so both keep working without its tree.

**Lost.** Expanded Delight's cinnamon **wood set** — planks, stairs, slab, door, trapdoor, fence, gate, sign, hanging sign, button, pressure plate, boat, chest boat, log, wood, stripped variants, leaves, sapling. Its sapling drops only from its own leaves (verified earlier: 0 recipes, 0 chest loot), so with the tree gone the whole palette is unobtainable in survival.

I did **not** unify the wood — that was scoped to "cinnamon items". If you want the palette back, adding `c:planks/cinnamon` and `c:cinnamon_logs` custom tags would let Expanded Delight's wood recipes accept Extra Delight's planks and logs, resurrecting its doors/boats/cabinets as craftable variants. Say the word.

## Cinnamon wood — unified too

Enumerated both mods' cinnamon items from their item models: **Expanded Delight 24, Extra Delight 54, 17 shared shapes** (of which `stick` and the dust were already done above). Added the remaining **16 wood shapes**.

| Shape | Extra Delight (wins) | Expanded Delight |
|---|---|---|
| planks, log, wood, slab, stairs, fence, fence_gate, door, trapdoor, button, pressure_plate, sapling, leaves, cabinet | `cinnamon_*` | `cinnamon_*` |
| stripped_log | `stripped_cinnamon_log` | `stripped_cinnamon_log` |
| stripped_wood | **`strippedcinnamon_wood`** | `stripped_cinnamon_wood` |

Extra Delight ships `strippedcinnamon_wood` — **missing the underscore**. An id-matching approach that assumed symmetric naming would have silently dropped that pair.

### One tag per shape, deliberately

I used `c:cinnamon/<shape>` — 16 separate tags of exactly 2 members each — rather than reusing the existing `c:cinnamon_logs` tag. That tag already exists and contains **all four** of Extra Delight's log variants:

```
c/tags/item/cinnamon_logs.json -> [cinnamon_log, cinnamon_wood, stripped_cinnamon_log, strippedcinnamon_wood]
```

Adding Expanded Delight's items to it and handing it to AU would have unified log, wood, stripped log and stripped wood into a **single** item, collapsing four distinct blocks. Per-shape tags avoid that.

### Totals

| | before | after |
|---|---:|---:|
| `custom_tags` | 189 | **205** |
| `unification.tags` | 174 | **190** |

All 36 cinnamon tag members validated against the jars' registered items — 0 unresolved. Backups: `proposed-configs/au-*.BEFORE4`.

### Bonus: four Expanded Delight items come back

Extra Delight has **no** cinnamon boat, chest boat, sign or hanging sign. Expanded Delight does. Those four are Expanded-Delight-only, so AU leaves their outputs alone — but their recipe *inputs* (`expandeddelight:cinnamon_planks`, `stripped_cinnamon_log`) are now unified, so they become craftable from Extra Delight's wood. The disabled tree costs you nothing there, and you gain four shapes Extra Delight lacks.
