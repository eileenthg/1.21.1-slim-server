# 25 — Recipes that undercut a harder method (29 Sep)

Rule being applied (user, 29 Sep): **a more involved method must give more.** A simpler method may exist
beside it only if it gives less, or is more tedious.

Scanner: `recipe-shortcuts/scan.py` → `report.json`. It reads every recipe in the vanilla jar, all 524 mod jars
and the pack's datapacks (28,145 live recipes after datapack overrides and `neoforge:false` stubs), resolves
item tags from the pack itself, applies the AlmostUnified merge **including its `ignored_tags` /
`ignored_items`**, then compares recipes that make the same item: a finding is a *simpler* method whose inputs
are no greater and whose output is no smaller.

## What it found

| Class | Count | Verdict |
|---|---|---|
| Hand recipe strictly better than a harder hand recipe | **1** (tofu) | fixed |
| Grid twin of a cutting-board recipe, identical yield | 20 | your call, listed below |
| Harder recipe that costs *more* and gives the same | 3 (fried egg ×3 routes) | recommend buffing the pot |
| Stonecutter skipping a furnace step | 2 | recommend one fix |
| Equivalent alternates, no progression implied | 2 | leave |
| Machine (Create/Spectrum/Mekanism) matching a hand recipe | ~900 | out of scope by your call: automation is throughput, not yield |
| Extra Delight oven matching a furnace | 100 | not a violation: the oven does up to 9 at once, the furnace is the tedious route |

## Changes made (29 Sep, datapack `zzz-recipe-progression.zip`)

| Change | Detail |
|---|---|
| tofu | grid 2 soybeans → **1**, cooking pot 2 soybeans → **2** |
| beef / porkchop / mutton | **new** cutting-board recipes at **2**. Butchery ships its own under `data/farmersdelight/recipes/` — the 1.20 folder name, which 1.21 never loads — so there was no knife route at all. Written to our own namespace with the modern `c:tools/knife` tool. |
| red dye | **new** cutting-board recipe, rose bush → **3** (vanilla grid gives 2). Delightful's own was in the same dead 1.20 folder. |
| sugar | cutting board 1 → **2** per cane (its extra 50% bonus kept); vanilla grid stays 1 |
| brown + red mushroom | Aquaculture's grid recipes disabled; cutting board (5) is the route |
| date | Ramadan Delight's grid recipe disabled; cutting board (8) is the route |
| stick | saplings removed from `recipes_plus:material_reverting/stick`; dead bush and dry root still convert |
| fried egg | **Extra Nutritious Fried Egg**: Rustic Delight's pot route (egg + cooking oil) now yields a fried egg carrying its own food component — **6 nutrition / 6.0 saturation** against the plain egg's 4 / 3.2 — and a gold name. Same item id, so it is a separate stack in the inventory. All the plain routes (furnace, campfire, smoker) are untouched. |

Reverted on request (29 Sep): the rice cutting yield is back to 1, and the furnace fried egg is enabled again.

Left as they are, by request: cobbled deepslate and cobblestone (both **stonecutter only** — nothing makes them in
a crafting table), cracked nether bricks, the 7 pancake pairs.

### Fixed: tofu
- `minecolonies:tofu` (grid, 2 soybeans → **2**) and `youkaishomecoming:cooking/tofu` (pot, 2 soybeans → **1**).
  Both make the same item after the merge, so the pot was pointless.
- Datapack **`zzz-recipe-progression.zip`** (sorts after `zz-unified-items`): grid → **1**, pot → **2**.

### Grid twin of a cutting-board recipe (20) — same input, same output
The knife route costs durability; neither is really harder, so nothing is being skipped. Listed for a decision.

| Item | Grid recipe | Knife recipe | Yield |
|---|---|---|---|
| pancake ×7 (plain, chocolate, coffee, honey, pumpkin, vegetable, cherry blossom) | `rusticdelight:<x>_from_<x>s` | `rusticdelight:cutting/<x>s` | 6 = 6 |
| beef / porkchop / mutton | `butchery:beefconversion` etc. | `farmersdelight:cutting/<x>_conversion` (shipped by Butchery) | 1 = 1 |
| brown / red mushroom | `aquaculture:<x>_from_<y>` | `aquaculturedelight:cutting/knife/<x>` | 5 = 5 |
| date | `ramadandelight:date_from_cluster` | `ramadandelight:cutting/cutting/dates_from_cluster` | 8 = 8 |
| rice | `farmersdelight:rice` | `farmersdelight:cutting/rice_panicle` (also drops straw) | 1 = 1 |
| sugar | `minecraft:sugar_from_sugar_cane` (vanilla) | `delightful:cutting/sugar_cane` | 1 = 1 |
| red dye | `minecraft:red_dye_from_rose_bush` (vanilla) | `delightful:cutting/rose_bush_alt3` | 2 = 2 |
| stick ×2 | `recipes_plus:…/material_reverting/stick` | `delightful:cutting/oak_sapling`, `…/dark_oak_sapling` | 1 = 1 |
| cobbled deepslate | `recipes_plus:…/more_stonecutting/cobbled_deepslate` | `farmersdelight:salvaging/deepslate` | 1 = 1 |
| cobblestone | `recipes_plus:…/more_stonecutting/cobblestone` | `farmersdelight:salvaging/stone` | 1 = 1 |

### Harder but not better
- **Fried egg:** Rustic Delight's pot recipe costs an egg **plus cooking oil** and still gives 1, the same as
  the plain furnace/campfire/smoker recipes. Recommend the pot giving 2.
- **Cracked nether bricks:** `recipes_plus` adds a stonecutter route that skips vanilla's smelting (and its
  fuel). Recommend disabling the stonecutter one.
- **Polished gingerbread block:** Festive Delight ships both a stonecutter and a furnace recipe, 1:1. The
  furnace one is dead weight; harmless.

### Not violations (checked, left alone)
- **Bone meal:** vanilla grid (bone → 3) vs Quark's smelting (bone → 3). Quark's exists for auto-smelting.
- **Red dye:** Make a Dye Out of It accepts several red things; the Extra Delight juicer takes sweet berries.
  Different inputs, not a bypass.

## Two scanner bugs found while checking these (both fixed)
- **The merge map was read from config**, which over-applied the `c:crops/{material}` wildcard to the *category*
  tag `c:crops/grain` and made it look as though rice, corn and chickpeas all became wheat. They don't.
  The map now comes from AlmostUnified's own log (`[AutoHiding] … for tag '#x' -> [hidden items]`), which is what
  it actually collapsed. **Rice from the cutting board is rice (plus straw), as you remembered.**
- **Recipes in the legacy `data/<ns>/recipes/` folder were being counted.** 1.21 only loads `recipe/`. That is
  why Butchery's meat conversions and Delightful's rose bush looked like live knife recipes.

## Limits of the scan
- Two recipes are only compared when one's inputs are a subset of the other's. Entirely different ingredient
  sets (say 3 wheat vs 1 egg + 1 sugar) aren't comparable without pricing items, so those aren't reported.
- Recipe types outside the tier table (~6,300 recipes: breeding, trading, spawner and similar) are skipped.
- Recipes whose ingredient tag resolves to nothing (a mod that isn't installed) are dropped as uncraftable.
