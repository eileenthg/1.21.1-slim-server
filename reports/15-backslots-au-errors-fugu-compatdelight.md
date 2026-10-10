# Back slots, AlmostUnified errors, fugu, Compat Delight Ecologics (17 Sep)

## Back-slot items that render when worn
| Mod | Back item | Renders? |
|---|---|---|
| BackSlots | whatever tool/weapon you put in its slot | yes (`BackSlotLayer`) |
| Skill Cloaks | the skill cloaks | yes (`CloakRenderer`) |
| Eternal Tales | Archangel's Wings, Arei Wings | yes (player wing models) |
| Youkai's Homecoming | Cirno wings | yes (`CirnoModel`) |
| Create Jetpack Curios | backtanks, jetpacks | no renderer in the add-on (2 classes) |
| Apothic Compats | back plate | no renderer |

## AlmostUnified errors, what they were, and the fix
All four came from overlaps inside the original AlmostUnified config, not from the mods.
- **Squid / glow squid** — `c:squid` and `c:squid_corpse` both listed the same squid items (same for glow squid). Removed `c:squid` and `c:glow_squid` from the unify list; the corpse tags already did the work.
- **Tentacles** — `c:foods/raw_meat/squid_tentacle` listed the Butchery, Miner's Delight and Ocean's Delight tentacles, which `c:tentacles` also unifies. Removed them from the first tag, which now unifies raw calamari only.
- **Cucumber** — Cultural Delights nests its cucumber tag into `c:crops/cucumber`, which dragged sliced cucumbers in. The tag is now pinned to the four whole cucumbers.
- **Uranium** — Eternal Tales tags its single uranium item as both `c:ingots/uranium` and `c:gems/uranium`. Nothing else in the pack is uranium, so it is now ignored.
- **"Recipes unified and threw exceptions"** — all broken before unification: Recipes+ hopper asks for `minecraft:trapped` (no such item), Create's rotten tomato haunting has a fluid input it cannot take, Compat Delight's warden pasta needs Apotheosis. All three switched off. `minecraft:red_dye` is Farmer's Respite's coffee-berry dye with a pre-1.21 result key; rewritten so it works.
- Left alone: "bound to multiple stone variant tags" warnings for vanilla ores — AlmostUnified's stone-variant detection noise, no items affected.

## Fugu slice vs pufferfish slice
| | Fugu Slice (Ocean's Delight) | Raw Pufferfish Slice (Crabber's Delight) |
|---|---|---|
| Food | 1 hunger, 0.1 saturation, fast | 1 hunger, 0.1 saturation, fast, **30% Poison 15 s** |
| From one pufferfish | 6 slices + bone meal | 2 slices + fish bones |
| Used in | Fugu Roll (7 hunger) | cooks into Cooked Pufferfish Slice (3 hunger) |

Merged with **Fugu Slice** winning. Cooked pufferfish slice recipes accept the fugu slice through the tag. Note it no longer poisons when eaten raw (food effects are code, not data).

## Compat Delight's Ecologics items, turned on
Compat Delight registers each compat module only when the target mod id is loaded (`ModCompat`, `ModList.isLoaded`). The patched jar rewrites that one key from `ecologics` to `crabbersdelight`; its item classes reference no Ecologics code, so nothing else changes. Original jar: `fixed-mods/original/`.

Now craftable, with substitutes: coconut cookie, coconut custard, coconut shake, coconut box (and back), crab stew, ice shake, ice knife, prickly pear box (and back), prickly pear cider, juice, jam, pie, ice cream, glazed coconut block, walnut cookie, walnut juice, walnut salad.

| Ecologics item | Substitute |
|---|---|
| coconut | Crabber's Delight coconut |
| coconut slice | Crabber's Delight coconut halve |
| crab meat | Youkai crab meat |
| prickly pear, cooked prickly pear | Yung's Cave Biomes prickly peach |
| ice bricks | Youkai ice bricks |
| walnut | any nut (`#c:nuts`: acorn, peanut, hazelnut, walnut) |

Still off (no stand-in): coconut-husk soups and knife, log/wood cutting, azalea flower, penguin meat dishes, walnut box, crab claw box (Crabber's claws don't stack). Compat Delight's own coconut milk merges into Crabber's.

The other 38 dead Compat Delight modules are for magic/tech mods (Botania, Mekanism, AE2, Ars Nouveau …) with no similar items in the pack; closest food candidates if wanted later: Naturalist (venison, catfish, bass), Tropicraft (citrus, marlin, pineapple), Hexerei (mandrake, belladonna), Nourished Nether (hoglin meat), Rats (cheese).

Verified on the server: 25,672 recipes, MineColonies 189, no parse errors from anything we ship, no AlmostUnified item errors.
