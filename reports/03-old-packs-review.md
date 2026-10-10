# Resource packs and data packs list: what needs changing

Source: `modpack-audit/Resource packs and data packs list/` (15 datapacks, 15 resource packs). Every zip was opened and every `namespace:id` it references was checked against what the 323 jars in the current `1.21.1` instance actually register. Script: `data/check_packs.py`.

Target formats for 1.21.1: **datapack `pack_format` 48**, **resource pack `pack_format` 34**. Data folders were renamed in 1.21 (`recipes/` → `recipe/`, `loot_tables/` → `loot_table/`, `advancements/` → `advancement/`, `tags/items/` → `tags/item/`). Conditions must use the `neoforge:conditions` key, not `conditions`.

Verdicts: **USE** = works as is. **RENAME** = pastel → spectrum edits only. **FIX** = content edits needed. **DROP** = targets mods you no longer have.

---

## Datapacks

| Pack | Verdict | What is wrong | What to change |
|---|---|---|---|
| `bloodybucket.zip` | USE | Adds bucket ↔ bottle recipes for Butchery blood in a `draft` namespace. All four Butchery ids exist. | Nothing. Optionally rename the `draft` namespace. |
| `Bugfix1.zip` | USE | Smoking recipes for Butchery pork belly/leg and a Supplementaries item shelf fix. All ids exist. | Nothing. |
| `ButcherySpongeFix.zip` | USE | Two recipes for `butchery:sponge`; exists. | Nothing. |
| `RopeConvert.zip` | USE (or drop) | Converts `farmersdelight:rope` ↔ `supplementaries:rope`. Both exist. | Nothing. Becomes redundant if you unify ropes with Almost Unified or drop Quark's rope per report 01. |
| `cleavernotreasurehunter.zip` | RENAME | Single file `data/pastel/enchantment/treasure_hunter.json`. Its content is byte-for-byte Spectrum's current definition except the `pastel` prefixes, so it no longer changes anything on its own (the old point was removing Butchery cleavers from `supported_items`; that is now controlled by the tag). | Move to `data/spectrum/enchantment/treasure_hunter.json`, replace every `pastel:` with `spectrum:`. If the goal was "cleavers can't get Treasure Hunter", instead ship `data/spectrum/tags/item/enchantable/treasure_hunter.json` with `"replace": true` and the item list you want. |
| `mobhead_compat.zip` | RENAME + small FIX | 25 Spirit Instiller recipes turning Butchery variant heads (cats, horses, llamas, iron golem, spider, enderman) into Spectrum memories, 2 pedestal idol recipes, plus tag additions. All Spectrum ids exist under the new name: recipe types `spectrum:spirit_instiller` / `spectrum:pedestal`, item `spectrum:memory`, tag `spectrum:memory_bonding_agents`, advancements `spectrum:unlocks/blocks/memories` and `unlocks/blocks/idols`. | Rename folders `data/pastel/` → `data/spectrum/` and replace all `pastel:` (also inside `components`: `"pastel:memory"` → `"spectrum:memory"`). Two tag files need a different target: Spectrum has no `mob_heads/cat_heads` or `mob_heads/iron_golem_heads` tags any more; its current head tags are `axolotl, bovine, equidae, fish, fox, frog, guardian, hoglin, lizard, mooshroom, parrot, shulker, slime, spider, zombie`. Put cat heads into `spectrum:mob_heads` directly and horse/llama heads into `mob_heads/equidae_heads`. Two references to `supplementaries:enderman_head`/`spider_head` are valid. |
| `ButcheryCompats.zip` | RENAME + FIX | Butchery carcass recipes for Spectrum heads (`pastel:guardian_head` etc.; all nine heads exist in Spectrum), Ocean's/End's/Ender's/My Nether's Delight drops. Problems: (1) one reference to `unusualend:enderblob_moult` (mod absent); (2) `data/farmersdelight/loot_modifiers/guardian_drop.json` uses the old top-level `conditions` key and there is no `data/neoforge/loot_modifiers/global_loot_modifiers.json` index, so NeoForge never loads it; (3) it overrides vanilla `loot_table/squid.json` and `glow_squid.json`. | Replace `pastel:` → `spectrum:`. Delete the recipe that uses `unusualend`. For the loot modifier: rename key to `neoforge:conditions` and add `data/neoforge/loot_modifiers/global_loot_modifiers.json` listing `farmersdelight:guardian_drop` (or move it into a namespace you own). Check the squid loot tables still match what Butchery/Ocean's Delight expect. |
| `BugFixes2.zip` | FIX | Mixed bag: Spice of Life Onion lunchbox recipes, Fright's Delight slimeapple pie, Extra Delight mortar sugarcane, Spectrum gem cutting (`pastel:` → `spectrum:`, all four gems exist), and Miner's Delight insect recipes. Broken parts: tags `c:choice_beef/lamb/pork(_cooked)` no longer exist (Butchery now uses `butchery:raw_beef`, `butchery:cooked_beef`, etc.), tags `c:arthropod_egg`, `c:foods/raw_meat/arthropod`, `c:foods/cooked_meat/arthropod` don't exist (Miner's Delight now uses `minersdelight:raw_insect_meat` / `cooked_insect_meat`), and item `minersdelight:insect_meat` was renamed. Everything sits in the `minecraft` namespace. | Rename pastel ids. Rewrite the six `tag_choice*` cutting recipes to Butchery's own tags. Rewrite the insect recipes to Miner's Delight's current tags/items. Consider moving files out of `data/minecraft/`. |
| `RemoveDupeCalamari.zip` | FIX (probably DROP) | Overrides Cultural Delights calamari/squid/pickle recipes in a `culturalrecipes` namespace, so it does not actually replace the originals (a different namespace adds recipes instead of overriding). Its purpose (dedupe squid between Cultural Delights and Miner's/Ocean's Delight) is exactly what Almost Unified will do. | Drop and let AU handle `c:squid`/`c:tentacles` (see report 02). If kept, files must live under `data/culturaldelights/recipe/...` with the original file names to override. |
| `minecoloniesFoodCompat.zip` | USE with one FIX | Rewrites 26 MineColonies crafter recipes to accept Farmer's Delight / Extra Delight / Cultural Delights ingredients. All ids exist except `rusticdelight:bell_pepper_red` (Rustic Delight absent). | Remove that one ingredient alternative. Re-check after you drop Cultural Delights, since 7 recipes use its eggplant. |
| `ExtraButchery.zip` | FIX (heavy) | 270 files. About 111 reference `butchercraft:` ids (a different mod, absent). Butchery ids referenced are valid. Also `extradelight:cocoa_butter_fluid` and tags `c:salami_mix`, `c:cocoa/solids`, `c:spoons` don't exist. | Delete every file that mentions `butchercraft`, then fix the three tags. |
| `EDxPastelxIE.zip` | DROP | Extra Delight × Spectrum × Immersive Engineering compat. 196 references to `immersiveengineering` (absent) and ~150 `c:` gem/bud/pigment tags that the pastel fork provided but current Spectrum does not (it only ships `c:tea_leaves`). | Drop unless Immersive Engineering returns; even then the tags would need recreating. |
| `EP-FD-Compat - v.1.04.zip` | DROP | Cutting recipes for `phantasm:pream_*` wood (End's Phantasm absent). Also uses the 1.20 `recipes/` folder. | Drop. |
| `recipes_plus.zip` | DROP (or gut) | 1,774 files from a different pack: of 1,417 data files, 713 reference absent mods (`unordinary_basics`, `newworld`, `enchanted-vertical-slabs`, `geodes`, `cobblemon`, ...). It also overrides 23 vanilla recipes (beacon, enchanting table, piston, dispenser, chain...) and vanilla stone slab/stair loot tables, and 4 files use the old `conditions` key. | Drop. If you want the vanilla recipe tweaks, extract only `data/minecraft/recipe/` (23 files) into a new pack. |
| `Villager Clothing Addition.zip` | USE (dedupe) | Combined MCA skin pack (`data/vca/skins/...` + textures). Identical copy in both folders (same md5). Profession lists name absent mods' professions (`morevillagers:*`, `iceandfire:scribe`, `forestry:*`) which MCA just ignores. `pack_format` 15 gives a "made for an older version" warning but loads. | Keep one copy (it needs to be in both resourcepacks *and* datapacks unless you split it, since it carries assets and data). Bump `pack_format` to 34/48 to silence the warning. |

## Resource packs

| Pack | Verdict | Notes |
|---|---|---|
| `AJKs_MCAR_v1.21.zip` (34) | USE | MCA book/dialogue lang overrides. |
| `Minecolonies Refreshed 4.0.0-1.21.1.zip` (34) | USE | MineColonies/Domum Ornamentum retexture. |
| `pastel_style_amethyst.zip` (34) | USE | Retextures **vanilla** amethyst only; no pastel/spectrum references despite the name. |
| `The Alchemist Mca faces.zip` (15) | USE | Version warning only. |
| `Icon Xaero's 1.21.zip` (15, supports 1–63) | USE | Minimap icons for ~70 mob mods, most absent; harmless, just 10k files. |
| `Extra Xaero's Minimap Icons - 1.20.1 - 1.0.zip` (15) | USE | Version warning only. |
| `REVIVED_Farmer's Delight_crops_3D_R1.5.1-1.20.x.zip` (15) | USE | 3D crops for FD, Corn Delight, Cultural Delights, My Nether's Delight, Supplementaries; extra assets for absent Farmer's Respite/Nether's Delight are inert. |
| `Stellar 3D-2.2.0.zip` (15, max 42) | USE | Vanilla 3D models. The `.rpo` beside it is a Resource Pack Overrides file; that mod isn't installed, so it does nothing. |
| `Stellar Tweaks-2.5.3.zip` (15, max 42) | USE | Vanilla + FD tweaks; assets for tetra/appleskin/etc. are inert. |
| `Nether Depths - Re-Fleshed.zip` (**13**) | USE, check | Fish entity textures for Nether Depths Upgrade. Format 13 is 1.19; textures still load, but verify the mod's texture paths didn't change in 3.2. |
| `crops-3d(2).zip` (**55**) | USE, check | Vanilla crop models only; format 55 is 1.21.4+. Models still use the old `models/item` layout so they load, but expect the "newer version" warning. Overlaps with the REVIVED FD crops pack on vanilla crops: put one above the other deliberately. |
| `Stellar_Beacons_v12111.zip` (**63**) | DROP | Uses `assets/minecraft/items/beacon.json` (the 1.21.4+ item-definition system). 1.21.1 ignores that folder, so only the block texture applies. |
| `Incendium Language Fix v1.0.0.zip` (61) | DROP | Incendium isn't installed. |
| `Villager Clothing Addition.zip` | see above | duplicate of the datapack copy. |

## Global "pastel → spectrum" rename rules

Applies to `cleavernotreasurehunter`, `mobhead_compat`, `ButcheryCompats`, `BugFixes2` (and `EDxPastelxIE` if ever revived):

1. Folder `data/pastel/` → `data/spectrum/`.
2. Every string `pastel:` → `spectrum:` (item ids, tag ids, recipe `type`, `required_advancement`, and component keys such as `"pastel:memory"`).
3. `#pastel:...` tag references → `#spectrum:...`.
4. Verified present in Spectrum 1.12.5: gems `citrine/topaz/onyx/moonstone` `_shard`/`_block`, all nine mob heads used, `memory`, `cat_idol`, `enderman_idol`, `stardust`, `quitoxic_powder`, `vegetal`, tag `memory_bonding_agents`, recipe types `pedestal` and `spirit_instiller`, advancements `unlocks/blocks/memories` and `unlocks/blocks/idols`, enchantment `treasure_hunter`.
5. Not present any more: `mob_heads/cat_heads`, `mob_heads/iron_golem_heads` tags, and all the `c:` gem/pigment/bud tags the pastel fork used to ship.

## Summary

- Use as is: bloodybucket, Bugfix1, ButcherySpongeFix, RopeConvert, minecoloniesFoodCompat (minus one line), Villager Clothing Addition (one copy), and all resource packs except Stellar Beacons and Incendium Language Fix.
- Rename only: cleavernotreasurehunter, mobhead_compat (plus two tag targets).
- Real edits: BugFixes2 (Butchery and Miner's Delight tags), ButcheryCompats (loot modifier format + one absent mod), ExtraButchery (strip butchercraft).
- Drop: EDxPastelxIE, EP-FD-Compat, recipes_plus, RemoveDupeCalamari (AU replaces it), Stellar Beacons, Incendium Language Fix.
