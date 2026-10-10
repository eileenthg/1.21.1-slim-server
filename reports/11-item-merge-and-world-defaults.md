# Item merge, MineColonies recipes, and new-world defaults (15 Sep)

## 1. Even Better Chat

- The "Chat Optimizer" button is added at a fixed spot (`height - 51`) that the Chat Settings option list draws over, so it can't be clicked. Use the keybind instead.
- Rebound **Open Chat Optimizer Config** from `O` (shared with 5 other binds) to **`;`**, and **Open Chat Search** from `J` to **`'`**. Backup: `proposed-configs/options.txt.BEFORE5`.
- Its "Chat history cap" (default 32) limits the **messages you sent** (up-arrow recall), not the chat log. The 100-line chat log limit is vanilla. Only Longer Chat History raised that limit, and it's been removed.

## 2. Packs on every new world

- **Datapacks:** `config/simple_datapacks.json` now has `requiredDatapacksPaths: ["datapacks"]` (the instance's own `datapacks/` folder) and no optional paths. Optional paths had caused the same pack to be listed twice.
  - Verified on a fresh server world: all 17 packs loaded automatically.
- **Trade Rebalance and Bundles:** `globalFeatures: true` adds every vanilla feature pack (`bundle`, `trade_rebalance`) to the Create World screen defaults. This only applies on the client, so it can't be verified on the server. Check the Experiments tab on your next new world.
- **Added to `datapacks/`:** `bloodybucket`, `Bugfix1`, `ButcherySpongeFix`, `RopeConvert` (all "use as is" in report 03), `Villager Clothing Addition REBORN`, and the new `zz-unified-items`.
- **Removed from `datapacks/`:** `minecolonies-crop-freedom` (its content is folded into `zz-unified-items`).
- **Resource packs:** `config/resourcepackoverrides.json` now lists your current 27 packs as `default_packs`, and marks every `file/` pack `required` (always on, even if options.txt is reset). Packs already flagged incompatible also get `force_compatible`. Backup: `proposed-configs/resourcepackoverrides.json.BEFORE`.

## 3. Item merge

- **What changed:**
  - 132 groups of same-thing items merged.
  - 193 duplicate recipes switched off.
  - 18 MineColonies crafting-table recipes turned into colony-worker recipes.
  - 19 MineColonies/Trade Post crafter files retargeted.
- **Files:**
  - `config/almostunified/{tags.json, unification/materials.json, placeholders.json}`. Backup: `proposed-configs/almostunified-BEFORE-MERGE/`.
  - `datapacks/zz-unified-items.zip`.
  - Sources, decisions and the full per-recipe report: `modpack-audit/unification/` (`decisions.py`, `build.py`, `report.json`, `merge-table.md`).

### Rules used
- **Kept item:** the one whose production chain has the most steps or a real workstation. Where tied, the base or ecosystem mod.
- **Juices:** Tough As Nails wins so drinks keep thirst and hydration. Other mods' tags are inherited by the kept item.
- **Other mods' recipes for merged items:** removed.
- **The kept mod's own crafting-table or furnace shortcuts:** removed when a workstation recipe exists.
- **Recipes that are not removed:**
  - Storage conversions (crates, sacks, buckets to bottles).
  - Wild-plant harvesting.
  - Recipes where the item is only a byproduct.
  - Create automation of the kept item.
  - Metal ore smelting.
- **Kept item with no working recipe of its own:** the best other recipe stays, so the item remains obtainable.
- **MineColonies:**
  - Worker recipes (`crafterrecipes`) are never removed. They now take and produce the kept items.
  - MineColonies crafting-table recipes for merged foods became Chef/Baker worker recipes, so colonies can still make them.
  - Recruitment cost `pasta_plain` now asks for `extradelight:cooked_pasta`.
  - Trade Post got values for the 25 kept items that lacked one.

### Existing merge problems fixed along the way
- **`c:dough`:** was turning corn dough, nut dough and sugar-water dough into wheat dough. Split into `c:unified/wheat_dough` and `c:unified/corn_dough`.
- **Stopped merging:**
  - `farmersdelight:cabbage_leaf` (it became cabbage).
  - `culturaldelights:cut_eggplant` and `white_eggplant` (they became eggplant).
  - `quark:iron_plate` blocks (they became Create iron sheets).
  - `cluttered:chiseled_gold_block` (it became a gold block).
- **`adamantite`:** added to AU materials so Eternal Tales and Mythic Metals adamantite merge.

### Verified on the test server (fresh world, same mods and configs)
- 130 of 132 groups merged. `culturaldelights:ginger` and `brewinandchewin:rice_pudding` aren't registered in this pack, so there's nothing to merge.
- The game loads 24,842 recipes.
- MineColonies loads 189 worker recipes (171 + 18) with no errors.
- No new AlmostUnified tag conflicts. The remaining AU errors (squid/tentacle/cucumber/uranium tags, ore stone variants) were there before.

### Not merged, on purpose
Decorative blocks (bricks, vertical slabs, pillars, crates of the disabled Quark module), tools, armour, workstations, backpack vs storage upgrades, Display Delight placeables, MineColonies large bottles (its liquid container system), Miner's Delight cups.

### Worth knowing
- **Hot chocolate** has two routes, both giving `tanca:hot_chocolate`: Create heated mixing, and Spectrum's Potion Workshop (glass bottle + milk + chocolate). *(changed 16 Sep)*
- **Chocolate is interchangeable** *(16 Sep)*: `spectrum:fresh_chocolate`, `create:bar_of_chocolate` and `extradelight:milk_chocolate_bar` are all in `#c:chocolate/milk/bar` (also `#c:chocolate`, `#c:foods/chocolate`).
  - Spectrum recipes that used fresh chocolate (Berliner, Bodacious Berry Bar, Chauve-Souris au Vin, Slushslide, Le Fishe au Chocolat, Hot Chocolate) accept any of the three.
  - Create's heated-mixer melting and Extra Delight's melting pot melt any of the three into liquid chocolate. Fresh chocolate itself is still made in the Titration Barrel.
- **Juices:**
  - Tough As Nails' crafting-table recipes (purified water + sugar + fruit) stay.
  - Extra Delight's juicer (press → juice fluid → bucket → bottles) was never switched off; its bottles become the TAN juices.
  - Expanded Delight's juicer recipes for apple, glow berry, melon and sweet berry are back *(16 Sep)*. They now take a purified water bottle as the container instead of a glass bottle and give the TAN juice. Cranberry juice is unchanged.
  - Purified water is made in TAN's Water Purifier.
- Wheat dough now comes from Extra Delight's mixing bowl (or Create mixing). Farmer's Delight's crafting-grid dough is off.
- Existing worlds: items already in inventories stay as the old item. They still work in recipes, because recipe inputs accept every merged variant.

Full table: `unification/merge-table.md`.
