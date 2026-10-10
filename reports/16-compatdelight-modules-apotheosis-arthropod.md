# Compat Delight modules, Apotheosis, arthropod meat, pufferfish (17 Sep)

Installed. Server check: 25,720 recipes, MineColonies 189, no recipe errors from Compat Delight or anything we ship, no AlmostUnified errors.

## Compat Delight modules turned on

Patched jar `fixed-mods/compatdelight-1.0.1.1-substitutes.jar` (script `unification/patch_compatdelight.py`, original in `fixed-mods/original/`). Each module now waits for an installed mod instead of the missing one:

| Module | Now keyed to | Recipes working |
|---|---|---|
| Ecologics | Crabber's Delight | 19 |
| Naturalist | AnotherScape | 8 |
| Tropicraft | Pineapple Delight | 20 |
| Hexerei | Youkai's Homecoming | 3 |
| Nourished Nether | Eternal Tales | 7 |
| Rats | Brewin' and Chewin' | 4 |

### Stand-ins
- **Naturalist:** bass → AnotherScape raw bass / bass; catfish → Alex's Mobs raw / cooked catfish; venison → Youkai raw venison / Eternal Tales cooked venison; lizard tail → AnotherScape raw / cooked moss lizard.
  - Off: duck, cooked duck, butterfly (nothing similar).
- **Tropicraft:** coconut chunk → Crabber's coconut halve; coffee berry → any coffee berry; fresh / seared marlin → AnotherScape raw marlin / marlin; cooked frog leg → Crabber's cooked frog leg; grapefruit, lemon, lime, orange → the crop tags (Extra Delight); pineapple → Pineapple Delight.
  - Off: papaya and starfish dishes; lemon, lime, grapefruit and pineapple juice and coconut milk (Extra Delight's juicer, Pineapple Delight and Crabber's already make these; the items are merged).
- **Hexerei:** belladonna berries → Eternal Tales belladonna; mandrake root / flowers → Youkai mandrake root / flower.
  - Off: moon knife, root and leaves, salad, willow cup, yellow dock drinks (moon dust, mugwort, yellow dock, willow: nothing similar).
- **Nourished Nether:** ectoplasm → Eternal Tales ectoplasm; raw / cooked hoglin → the hoglin meat tags (Butchery).
  - Off: necronium knife, nether fungus dishes, strange elixir drinks.
- **Rats:** cheese → any cheese.
  - Off: blue cheese, nether cheese, contaminated food, rat meat, oratchalcum, potato pancake dishes; cheese slice (Expanded Delight's, merged).

## Apotheosis

Installed as `apotheosis` plus the split mods (Apothic Enchanting, Attributes, Spawners …), so Compat Delight's Apotheosis module was already registering its items — but its recipes pointed at pre-split ids. Remapped 17 recipes:

| Old id | 1.21 id |
|---|---|
| `apotheosis:warden_tendril` | `apothic_enchanting:warden_tendril` |
| `apotheosis:infused_breath` | `apothic_enchanting:infused_breath` |
| `apotheosis:prismatic_web` | `apothic_enchanting:prismatic_web` |
| `apotheosis:common_material` | `apotheosis:mysterious_scrap_metal` (common rarity material) |
| `apotheosis:rare_material` | `apotheosis:luminous_crystal_shard` (rare rarity material) |

`apotheosis:gem_dust` is unchanged. Warden pasta is back on.

Also fixed two Cave Biomes Delight typos in the jar's data: the icicle knife recipe produced `icicle_knife1` (now `icicle_knife`), and glazed frost lily produced a block that is never registered (switched off).

## Arthropod meat — left as it was (reverted)

Tried unifying Butchery's bug meats into Miner's Delight arthropod, then reverted at your request. Back to the original setup: both arthropod tags stay in `ignored_tags`, so nothing is converted, but your custom tags (`c:foods/raw_meat/arthropod`, `cooked_meat/arthropod`, `minersdelight:insect_meat`) still let Miner's Delight recipes accept Butchery's bee back meat, silverfish chunks and endermite chunks.

## Pufferfish slice

Back to Crabber's Delight pufferfish slice as the kept item (raw poison chance, cooks into a cooked slice, fish bones from cutting). Fugu slices from Ocean's Delight still work in recipes via the tag.
