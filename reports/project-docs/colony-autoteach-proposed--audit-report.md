# colony-autoteach — recipe audit

`54352` generated recipes curated down to `21646`.


## 1. Crafters removed

These held crafting-table recipes they should never run.


| crafter | recipes dropped | why |
|---|---:|---|
| `sifter_custom` | 254 | custom-only job (sifts loot tables, fixed mesh recipes) |
| `smelter_smelting` | 153 | custom-only job (smelts every ore automatically, not taught) |
| `netherworker_custom` | 99 | custom-only job (expedition loot tables) |
| `smelter_custom` | 92 | custom-only job (smelts every ore automatically, not taught) |
| `crusher_custom` | 50 | custom-only job (fixed cobble->gravel->sand->clay chain) |
| `expedition_comets_custom` | 49 | expedition hut: loot, not crafting |
| `expedition_rayana_custom` | 40 | expedition hut: loot, not crafting |
| `expedition_deeper_down_custom` | 38 | expedition hut: loot, not crafting |
| `concretemixer_custom` | 32 | custom-only job (fixed 16 powders + 16 concretes) |
| `expedition_otherside_custom` | 31 | expedition hut: loot, not crafting |
| `expedition_gardens_of_eden_custom` | 28 | expedition hut: loot, not crafting |
| `expedition_bumblezone_custom` | 24 | expedition hut: loot, not crafting |
| `expedition_end_custom` | 23 | expedition hut: loot, not crafting |
| `expedition_kingdom_of_amber_custom` | 19 | expedition hut: loot, not crafting |
| `expedition_purgatorium_custom` | 14 | expedition hut: loot, not crafting |
| `expedition_lands_of_karvat_custom` | 10 | expedition hut: loot, not crafting |
| `alchemist_brewing` | 8 | wrong module type (brewing stand, and brewing recipes are not datapack data) |
| `expedition_volcanech_custom` | 8 | expedition hut: loot, not crafting |
| `enchanter_custom` | 2 | custom-only job (loot-table books, fixed tomes and scrolls) |

## 2. Builder and Miner

- `builder_crafting`: blanket of **10176** recipes removed, replaced with **196** curated ones.
- `miner_crafting`: blanket of **10176** recipes removed, replaced with **40** curated ones.

## 3. Duplicate ownership resolved

4424 recipes removed across 972 items that more than one crafter claimed.


| item | kept with | taken from |
|---|---|---|
| `alexsmobs:enderiophage_rocket` | `blacksmith_crafting` | stonemason_crafting |
| `alexsmobs:roadrunner_boots` | `blacksmith_crafting` | stonemason_crafting |
| `alexsmobs:squid_grapple` | `blacksmith_crafting` | chef_crafting |
| `amelet:box_amuleto` | `fletcher_crafting` | mechanic_crafting |
| `apotheosis:diamond_upgrade_smithing_template` | `blacksmith_smithing_template_crafting` | stonemason_crafting |
| `apotheosis:gem_case` | `glassblower_crafting` | stonemason_crafting |
| `apotheosis:gold_upgrade_smithing_template` | `blacksmith_smithing_template_crafting` | stonemason_crafting |
| `apotheosis:iron_upgrade_smithing_template` | `blacksmith_smithing_template_crafting` | stonemason_crafting |
| `apotheosis:simple_reforging_table` | `blacksmith_crafting` | stonemason_crafting |
| `aquaculture:neptunium_ingot` | `blacksmith_crafting` | mechanic_crafting |
| `arcadiandream:ordinary_hat` | `fletcher_crafting` | blacksmith_crafting, mechanic_crafting |
| `beautify:lamp_bamboo` | `fletcher_crafting` | blacksmith_crafting, planter_crafting |
| `beautify:lamp_light_bulb` | `glassblower_crafting` | blacksmith_crafting |
| `blazinghot:black_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:blaze_apple` | `blacksmith_crafting` | chef_crafting |
| `blazinghot:blaze_gold_ingot` | `blacksmith_crafting` | mechanic_crafting |
| `blazinghot:blue_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:brass_apple` | `blacksmith_crafting` | chef_crafting |
| `blazinghot:brown_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:copper_apple` | `blacksmith_crafting` | chef_crafting |
| `blazinghot:cyan_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:gray_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:green_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:iron_apple` | `blacksmith_crafting` | chef_crafting |
| `blazinghot:light_blue_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:light_gray_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:lime_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:magenta_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:orange_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:pink_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:purple_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:red_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:white_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:yellow_modern_lamp` | `glassblower_crafting` | dyer_crafting |
| `blazinghot:zinc_apple` | `blacksmith_crafting` | chef_crafting |
| `bosses_of_mass_destruction:levitation_block` | `blacksmith_crafting` | stonemason_crafting |
| `brewinandchewin:apple_turnover` | `baker_crafting` | chef_crafting |
| `brewinandchewin:glow_berry_meringue_pie` | `baker_crafting` | chef_crafting |
| `brewinandchewin:glow_brownie` | `baker_crafting` | chef_crafting |
| `brewinandchewin:pumpkin_roll` | `baker_crafting` | chef_crafting, dairyworker_crafting |
| `brewinandchewin:raw_croissant` | `baker_crafting` | chef_crafting |
| `brewinandchewin:raw_muffin` | `baker_crafting` | chef_crafting |
| `brewinandchewin:rich_chocolate_cake` | `baker_crafting` | dairyworker_crafting |
| `brewinandchewin:tankard` | `blacksmith_crafting` | sawmill_crafting |
| `butchery:acacia_butcher_display` | `glassblower_crafting` | blacksmith_crafting, chef_crafting |
| `butchery:birch_butcher_display` | `glassblower_crafting` | blacksmith_crafting, chef_crafting |
| `butchery:canopy_black` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_blue` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_brown` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_cyan` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_gray` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_green` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_light_blue` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_light_gray` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_lime` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_magenta` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_orange` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_pink` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_purple` | `fletcher_crafting` | blacksmith_crafting |
| `butchery:canopy_red` | `fletcher_crafting` | blacksmith_crafting |

…and 912 more.

## 4. Recipes in the wrong kind of module

7346 recipes dropped: a furnace recipe only belongs in a Smelting module and a table recipe only in a crafting one. 432 items had no crafter of the right kind at all.


| item | recipe runs in | only offered to |
|---|---|---|
| `amcdelight:cooked_kangaroo_cuts` | `None` | chef_smelting |
| `amcdelight:cooked_lobster_meat` | `None` | chef_smelting |
| `amcdelight:cooked_moose_cuts` | `None` | chef_smelting |
| `aquaculture:neptunium_ingot` | `minecraft:blast_furnace` | blacksmith_crafting, mechanic_crafting |
| `arcadiandream:makaite_ingot` | `minecraft:blast_furnace` | blacksmith_crafting |
| `arcadiandream:makaite_ingot` | `minecraft:furnace` | blacksmith_crafting |
| `artifactdelight:regenerating_meatloaf` | `None` | chef_farmers_cooking |
| `create:brass_nugget` | `minecraft:blast_furnace` | blacksmith_crafting |
| `create:brass_nugget` | `minecraft:furnace` | blacksmith_crafting |
| `create:copper_shingle_slab` | `None` | stonemason_stonecutting |
| `create:copper_shingle_stairs` | `None` | stonemason_stonecutting |
| `create:copper_shingles` | `None` | stonemason_stonecutting |
| `create:copper_tile_slab` | `None` | stonemason_stonecutting |
| `create:copper_tile_stairs` | `None` | stonemason_stonecutting |
| `create:copper_tiles` | `None` | stonemason_stonecutting |
| `create:exposed_copper_shingle_slab` | `None` | stonemason_stonecutting |
| `create:exposed_copper_shingle_stairs` | `None` | stonemason_stonecutting |
| `create:exposed_copper_tile_slab` | `None` | stonemason_stonecutting |
| `create:exposed_copper_tile_stairs` | `None` | stonemason_stonecutting |
| `create:oxidized_copper_shingle_slab` | `None` | stonemason_stonecutting |
| `create:oxidized_copper_shingle_stairs` | `None` | stonemason_stonecutting |
| `create:oxidized_copper_tile_slab` | `None` | stonemason_stonecutting |
| `create:oxidized_copper_tile_stairs` | `None` | stonemason_stonecutting |
| `create:rose_quartz_tiles` | `None` | stonemason_stonecutting |
| `create:scoria` | `minecraft:furnace` | stonemason_stonecutting |
| `create:small_rose_quartz_tiles` | `None` | stonemason_stonecutting |
| `create:waxed_copper_shingle_slab` | `None` | stonemason_stonecutting |
| `create:waxed_copper_shingle_stairs` | `None` | stonemason_stonecutting |
| `create:waxed_copper_tile_slab` | `None` | stonemason_stonecutting |
| `create:waxed_copper_tile_stairs` | `None` | stonemason_stonecutting |

…and 402 more.

## 5. Curated tool recipes


| crafter | recipes | serves colony requests |
|---|---:|---|
| `builder_crafting` | 196 | no — SimpleCraftingModule has no request resolver |
| `lumberjack_custom` | 196 | yes |
| `miner_crafting` | 40 | no — SimpleCraftingModule has no request resolver |

## 6. Final counts


| crafter | before | after |
|---|---:|---:|
| `alchemist_brewing` | 8 | 0 |
| `baker_crafting` | 495 | 433 |
| `baker_smelting` | 56 | 27 |
| `bartender_crafting` | 29 | 12 |
| `blacksmith_crafting` | 2407 | 1892 |
| `blacksmith_smithing` | 199 | 164 |
| `blacksmith_smithing_template_crafting` | 3 | 3 |
| `builder_crafting` | 10176 | 175 |
| `chef_crafting` | 2146 | 1167 |
| `chef_farmers_cooking` | 2059 | 1913 |
| `chef_farmers_cutting_knife` | 778 | 417 |
| `chef_farmers_plating` | 3 | 0 |
| `chef_smelting` | 872 | 809 |
| `concretemixer_custom` | 32 | 0 |
| `crusher_custom` | 50 | 0 |
| `dairyworker_crafting` | 588 | 234 |
| `dyer_crafting` | 2250 | 1210 |
| `dyer_smelting` | 79 | 7 |
| `enchanter_custom` | 2 | 0 |
| `expedition_bumblezone_custom` | 24 | 0 |
| `expedition_comets_custom` | 49 | 0 |
| `expedition_deeper_down_custom` | 38 | 0 |
| `expedition_end_custom` | 23 | 0 |
| `expedition_gardens_of_eden_custom` | 28 | 0 |
| `expedition_kingdom_of_amber_custom` | 19 | 0 |
| `expedition_lands_of_karvat_custom` | 10 | 0 |
| `expedition_otherside_custom` | 31 | 0 |
| `expedition_purgatorium_custom` | 14 | 0 |
| `expedition_rayana_custom` | 40 | 0 |
| `expedition_volcanech_custom` | 8 | 0 |
| `farmer_crafting` | 163 | 84 |
| `fletcher_crafting` | 1662 | 1594 |
| `glassblower_crafting` | 530 | 490 |
| `glassblower_smelting` | 13 | 11 |
| `lumberjack_custom` | 116 | 176 |
| `lumberjack_farmers_cutting_axe` | 185 | 79 |
| `mechanic_crafting` | 1843 | 768 |
| `miner_crafting` | 10176 | 40 |
| `netherworker_custom` | 99 | 0 |
| `planter_crafting` | 160 | 57 |
| `sawmill_crafting` | 3769 | 2776 |
| `sifter_custom` | 254 | 0 |
| `smelter_custom` | 92 | 0 |
| `smelter_smelting` | 153 | 0 |
| `stonemason_crafting` | 5839 | 2331 |
| `stonemason_farmers_cutting_pickaxe` | 51 | 40 |
| `stonemason_stonecutting` | 6464 | 4620 |
| `stonesmeltery_smelting` | 267 | 117 |
