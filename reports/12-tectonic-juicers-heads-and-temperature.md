# Tectonic, juicers, mob heads and the temperature mods (16 Sep)

## 1. Tectonic + CliffTree

**World height: nothing to change.** Tectonic's own `minecraft:worldgen/noise_settings/overworld` keeps vanilla bounds — `min_y: -64`, `height: 384` (so -64 to 320). Surface terrain is taller and caves deeper *within* those bounds, so no geode, ore or structure Y range falls outside the world.

**Spectrum geodes** (from `data/spectrum/worldgen/placed_feature/`):

| Geode | Y range | Rarity | Effect of Tectonic |
|---|---|---|---|
| Citrine | 32 – 55 (`above_bottom 96` to `absolute 55`) | 1 in 50 chunks | Fewer: lava tunnels and underground rivers sit in this band and eat the stone they need |
| Topaz | 82 – 320 (`below_top 0`) | 1 in 6 chunks | More: Tectonic's mountains put far more stone above y82 |
| Amethyst (vanilla) | -58 – 30 | 1 in 24 chunks | Slightly fewer, same reason as citrine |
| Moonstone | -52 – -34 | 1 in 24 | Deeper Down only, unaffected |
| Ash | every layer, Nether | 1 in 4 | Unaffected |

Nothing is broken, the *ratio* just shifts: topaz becomes common, citrine rarer. If it feels off after playing, the fix is a datapack override of those two placed features — citrine `chance` 50 → 30, topaz 6 → 12.

**Biome and terrain conflicts** — every jar and datapack scanned for files that overwrite vanilla worldgen:

- **CliffTree replaces 71 vanilla biome files** (badlands, beach, birch forest, cherry grove …). Nothing else in the pack replaces those same files, so no clash. Tectonic ships a built-in `overlay.clifftree` for this pair.
- **Tectonic replaces 34 vanilla density functions** plus the overworld noise settings. Nothing else touches them — Yung's Better Caves ships an *empty* `noise_settings/` folder, so it does not fight Tectonic.
- **Eternal Tales** (51 biomes, 800 biome modifiers), **Youkai's Homecoming** (4), **AnotherScape**, **Bumblezone**, **Deeper Darker** add biomes by modifier or in their own dimensions — none reshape overworld terrain.
- **TerraBlender users** (Enderscape, ClimateRivers, Yung's Cave Biomes) only choose *which* biome goes where; Tectonic only changes terrain *shape*, and Lithostitched (which Tectonic uses) is installed. They stack rather than conflict.
- Watch by eye, not a file conflict: **ClimateRivers** adds wide surface rivers and Tectonic reworks rivers too, so valleys may look doubled up.

## 2. Juicers — Extra Delight wins

**Why:** Extra Delight's juicer is a hand-cranked block — 8 crank turns per batch, its own 4000 mB tank with a visible fill level, a fluid capability so Create pipes can drain it, cosmetic styles, and a dye byproduct at a per-recipe percent chance. Expanded Delight's is a GUI box with a progress bar.

**Done:**
- Expanded Delight's juicer crafting recipe is off and all 5 of its juicing recipes are gone.
- Old ones convert: `expandeddelight:juicer` → `extradelight:juicer` in the crafting grid.
- **All 7 Tough As Nails crafting-table juice recipes are off** (apple, cactus, chorus fruit, glow berry, melon, pumpkin, sweet berry).
- Bottling the juicer's tank now costs **purified water bottles** instead of empty glass bottles — cactus, glow berry, melon, sweet berry.
- The four juices with no juicer fluid — apple, pumpkin, chorus fruit, and Expanded Delight's cranberry — are made in **Extra Delight's mixing bowl**: purified water bottle as the container, fruit + sugar, 4 stirs with a spoon.
  - The juicer physically can't do these: one input slot, and its recipe must output a *registered fluid*. There is no apple-juice, pumpkin, chorus or cranberry fluid in the pack. Apple is worse still — the juicer already turns apples into apple *cider* fluid, and a second apple recipe would be ambiguous.

**Still on plain glass bottles** (not Tough As Nails drinks, so no thirst value at stake): lemon, lime, orange, grapefruit, tomato, pickle juice, apple cider.

## 3. Recipes and workstations worth flagging

**Mob heads — the biggest duplicate left.** Butchery adds 76 heads, Spectrum 130, spectrumcompat 45. **44 are the same mob in two mods** (`butchery:cow_head` vs `spectrum:cow_head`, …), plus Supplementaries' enderman and spider heads. Merging is the same job as the food merge: a `c:heads/<mob>` tag per animal, keep Butchery's (they drop from butchering, mount on plaques, and already have colour variants for cats, horses, llamas, axolotls, mooshrooms), then point Spectrum's idol / fusion shrine / spirit instiller recipes at the tag.

**Apple cider vs apple juice.** Farmer's Delight apple cider and Tough As Nails apple juice are arguably the same drink. Left separate because cider is an ingredient across many Farmer's Delight recipes and merging would give all of them thirst values.

**Workstations that do the same real-world job**, ranked by how clean the merge would be:

| Mechanic | Duplicates | Suggested keeper |
|---|---|---|
| Mortar and pestle | `extradelight:mortar` (42 recipes), `butchery:pestleandmortar` (7) | Extra Delight — port the 7 |
| Meat grinder | `butchery:meatgrinder` (7), `butchercraft:grinder` (1) | Butchery |
| Drying rack | `extradelight:drying_rack` (10), `youkaishomecoming:drying_rack` (6) | Extra Delight |
| Cutting | `farmersdelight:cutting` (917), `eternal_tales:cutting_table` (8) | Farmer's Delight board |
| Kettle / tea | `farmersrespite:kettle` (19), `youkaishomecoming:kettle` (21), moka pot (16) | Farmer's Respite kettle; keep the moka pot, espresso is a different job |
| Fermenting | `brewinandchewin:keg` (45), `youkaishomecoming:simple_fermentation` (23), `spectrum:titration_barrel` (72) | Keg for food and drink; leave the titration barrel, it is the magic chain |
| Crushing / milling | `create` (592), `mekanism:crushing` (50), `immersiveengineering:crusher` (13), `enderio:sag_milling` (8), `neepmeat:grinding` (26), `spectrum:anvil_crushing` (338) | One tech chain — needs a call on which tech mod leads |
| Pressing | `create:pressing` (46), `thermal:press` (20), `immersiveengineering:metal_press` and `squeezer` (9), `forestry:squeezer` (4) | Create |

Each is a recipe-porting job the size of the juicer one.

## 4. Frostiful + Scorchful vs Project Atmosphere — they cannot be used here

From the mods' own sources:

- **Frostiful** `gradle.properties` (2.3.2+1.21.1): `loader_version=0.16.9`, `fabric_version=0.108.0+1.21.1`, plus Cardinal Components, Trinkets, Thermoo. **Fabric only.**
- **Scorchful** `gradle.properties` (0.15.11+1.21.1): `loader_version=0.16.10`, `fabric_version=0.115.4+1.21.1`, same Fabric-only stack (Thermoo, CCA, Trinkets, Satin).

No NeoForge build exists for either, and their temperature model lives in **Thermoo**, a Fabric-only library. This is a NeoForge pack, so they are out unless the pack moves to Fabric.

**What Project Atmosphere integrates with** — from the installed jar (`NeoForge-projectatmosphere-0.9.1.2`, `net/Gabou/projectatmosphere/compat/`): `ToughAsNailsCompat`, `ColdSweatCompat`, `LegendarySurvivalCompat`, Serene Seasons / Serene Seasons Plus, Simple Clouds, Auroras, Cool Rain. **No Frostiful or Scorchful compat exists.**

`ToughAsNailsCompat` reads Tough As Nails, it does not write to it: `TemperatureHelperImpl.getTemperatureAtPosWithoutProximity(level, pos)` returns a band, mapped to ICY -20 °C, COLD 0, NEUTRAL 15, WARM 28, HOT 38, and that base feeds Project Atmosphere's 7-day forecast curve and its snowstorm logic. Tough As Nails is what gives the forecast its regional temperature.

**Recommendation:** keep Tough As Nails, or switch to **Cold Sweat** for a deeper temperature sim — Project Atmosphere supports both, and Cold Sweat is the closest NeoForge equivalent to Frostiful/Scorchful. Dropping Tough As Nails for nothing leaves Project Atmosphere on its own biome-based temperature and removes thirst, which the whole juice chain is built around.

## 5. ButterBee, Butchery variants, All the Heads

**ButterBee** is a datapack plus a resource pack, an add-on to CliffTree, adding biome-based skins for cows, pigs, chickens, wolves and others. Both halves are needed: the datapack in `datapacks/` (auto-loads on new worlds now), and the resource pack in `resourcepacks/`, which also wants adding to the forced list in `config/resourcepackoverrides.json`.

**Variant heads from Butchery: not without code.** Butchery is a MCreator mod and every head is a *separate block class* (`net/mcreator/butchery/block/BlackhorseheadBlock`, …) with the drop chosen in generated code, not by loot table. There is no data hook for "this cow is the ButterBee marsh variant, drop the marsh cow head". Its existing variant heads are hard-coded against vanilla variants. Doing it properly means new blocks, textures and drop logic — a small companion mod.

**All the Heads: skip it.** It adds a *third* full set of head items on top of Butchery's 76 and Spectrum's 130, making the duplicate problem worse, and it still would not know about ButterBee variants. The workable part of the idea is the head merge in section 3.
