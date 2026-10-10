# 22: Test server clean-up, new mods, TAN → Thirst Was Taken + Cold Sweat, drinks (19 Sep)

## Test server
Removed from A-full, B-no-roadweaver and X-expeditions:
- AnotherScape (byte-checked against the archive first), Tough As Nails and tanca.
- The townsfolk datapacks, the TAN/tanca configs, and the AnotherScape dimension folders in every test world.
- The AnotherScape/TAN/tanca jars in A-full's `mods-old` backup.

A-full's spawn-onboarding is now 1.1.0. One copy each of TAN and tanca was kept in `removed-mods/`. About 6 GB freed; C: has 14 GB free.

## New mods
All load together (test launch). New errors are the mods' own, listed below.

| Finding | Detail | Action |
|---|---|---|
| **Thirst Was Taken + Thirst Was Reclaimed** | Same mod id `thirst`, so they can't both load. Reclaimed lacks `IThirst`, `DrinkableItem` and `MathHelper`, which Ender Canteen and Water Canteens call. | **Reclaimed removed** (`removed-mods/`) |
| **The Roads More Travelled vs Safepaths** | Both wear paths where players and mobs walk (TRMT: eroded grass/dirt/coarse dirt/sand). Safepaths explicitly counts MineColonies colonists. | **Resolved:** TRMT removed by the user; Safepaths kept. Block Runner (speed on path blocks) complements it |
| **cslcrops vs Serene Seasons** | cslcrops stops crops outside a temperature range; Serene Seasons `seasonal_crops = true` already gates crops by season, and Cold Sweat's temperature follows the season. That's two locks on the same thing. | **Resolved:** cslcrops removed (`removed-mods/`); Serene Seasons handles crops |
| **Cocktails Delight duplicates** | Grapes and grape seeds (Brewin' and Chewin', Youkai's Homecoming), red and white wine (both), moonshine and the blueberry crate (Eternal Tales), cranberry juice (Expanded Delight). | Item-merge candidates, **your call** |
| Cocktails Delight errors | Its 48 Create filling/emptying recipes use an old format, so its drinks can't go through Create spouts or basins. 2 soy-milk recipes point at a missing mod; the hops top half has a broken loot table. | The mod's own bugs; ignored |
| Thirst Was Taken errors | 2 Create compat recipes (cactus, terracotta bowl) don't load. | Its own bug |
| Cold Sweat errors | 2 JEED info entries don't load. | Harmless |
| MCA Colony Integration vs your `colonyhandover` | Different scale: MCI converts single colonists ↔ MCA residents; colonyhandover turns a whole colony into an MCA village. Both touch the same data. MCI is 0.1.0-beta. | Test the two together |
| MCA Social Expansion vs MCA Romantic Expansion | mca-se adds relationships and gifts; Romantic Expansion adds romance/marriage items. They may overlap on marriage. | Check in play |
| MCA Quests / Conversations / Social | Same family; they integrate with Townstead (installed). | OK |
| Cold Sweat vs Project Atmosphere | PA has no player temperature, so no clash. Cold Sweat reads the global rain flag, while PA's rain is local; rain cooling may not match what you see. | Note |
| Hot Bath | Its TAN link is optional, so the baths no longer warm you. Cold Sweat can be given block temperatures for them in its config. | Offer |
| Cold Sweat: Altitude vs Thin Air | Temperature vs oxygen; they complement each other. | OK |
| Configured | Config GUI. | OK |

## Tough As Nails leftovers (TAN and tanca are gone)
- The item merge picked **TAN's juices** as the kept version of five juice groups, and **tanca's hot chocolate** for hot chocolate. New winners: Extra Delight (cactus, glow berry, sweet berry), Expanded Delight (apple), Farmer's Delight (melon), Spectrum (hot chocolate). TAN's ice cream was dropped from its group.
- **Purified water:** Thirst Was Taken has no purified-water item; purity is `thirst:purity` (0–3) on a water bottle. The four juice-from-bucket recipes now require a water bottle with purity 3. Extra Delight's mixing bowl only compared the container item; Drink Fix 1.1.0 now makes it also check the potion and require purity at least the recipe's (see below).
- Chorus and pumpkin juice mixing recipes are off: pumpkin juice existed only in TAN, and chorus juice now comes only from Ender's Delight's own recipe.
- **No-knockback:** Thirst Was Taken already tags its dehydration damage, and Cold Sweat tags `cold`/`hot`. Their difficulty-scaled `cold_scaling`/`hot_scaling` weren't tagged, so they're now in `minecraft:no_knockback` (replacing the TAN entries).
- MCTradePost value moved to FD melon juice; 9 dead TAN/tanca recipe overrides removed; AlmostUnified shrimp/juice/hot-chocolate entries fixed; Lost Maids list cleaned; `config/toughasnails` + `tanca-common.toml` archived in `removed-configs/toughasnails-19sep`.
- Scripts (`unification/`) updated to match, `REMOVED_MODS = ('anotherscape', 'toughasnails', 'tanca')`. Pre-change copies: `datapacks-retired/zz-unified-items.BEFORE-TAN-REMOVAL.zip`, `proposed-configs/*BEFORE-TAN-REMOVAL*`, `proposed-configs/unification-BEFORE-TAN-REMOVAL/`.

## Compat Delight drinks
They were plain food items: eating animation, and no bottle back.
- **New mod Drink Fix** (`drinkfix-1.21.1-1.0.0.jar`, source `C:\Users\eteil.000\Claude\drink-fix`). Items in `#drinkfix:drinks` use the drinking animation and sound. If the recipe used a container, it comes back: glass bottle for 95 of them, Compat Delight's cup for bamboo tea. Drinks whose recipe used no container give nothing back, so there's no free-bottle exploit. All 125 Compat Delight juices, shakes, teas, ciders and punches are tagged.
- **Thirst:** they're now in `config/thirst/item_settings.toml` (juice/cider/punch 8/13, tea 10/14, shake 6/8), along with the new juice winners. TAN entries removed. Default kept at `proposed-configs/thirst-item_settings.DEFAULT.toml`.
- **Mixing bowl purity (Drink Fix 1.1.0):** a mixin on `MixingBowlRecipe.matches` requires the container in the bowl to hold the same potion as the recipe's and a `thirst:purity` at least as high. Bottles without purity count as Thirst Was Taken's default (2). Only containers that carry those components are affected; the mixin is skipped if Extra Delight is absent.
- **Tested in game** (`test-client/drink-tester`, real mixing bowl stirred to completion): apple juice with an untagged bottle, purity 0 or purity 2 → no juice; purity 3 → juice; a healing potion tagged purity 3 → no juice. Cherry juice and melon juice drink and return a glass bottle; bamboo tea drinks and returns the cup. Hydration values from `item_settings.toml` were not measured.
