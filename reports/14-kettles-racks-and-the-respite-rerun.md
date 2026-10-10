# ButcheryCompats, drying racks, kettles and the Farmer's Respite re-run (16 Sep, part 3)

## 1. ButcheryCompats retargeted to Spectrum

You were right that Pastel is a Spectrum fork — every `pastel:` id has a Spectrum twin, so the pack works as-is once retargeted. `ButcheryCompats-spectrum.zip`:

- 8 crafting recipes now convert **carcass ↔ Spectrum head** for elder guardian, guardian, pufferfish and strider.
- 5 loot tables now drop Spectrum heads: glow squid head, magma cube (cut 4), salmon, slime (cut 4), wither heart.
- Dropped its `glowsquid_cut_1/2_drop` tables — `squid-calamari-dedup` owns those, and they were the only overlap with anything installed.
- Everything it references exists. Its ender dragon butchering tables and the Farmer's Delight guardian loot modifier come along unchanged.

## 2. Curios — my mistake

Curios ships slot *definitions*, and on its own gives none of them a size. The back slot is live because **five other mods request it**: Apothic Compats, Create Jetpack Curios, Eternal Tales, Skill Cloaks and Youkai's Homecoming. BackSlots then adds a *second*, separate back slot with `"operation": "SET"`. So the overlap is real, but it isn't Curios' doing — if you want one back slot, drop BackSlots.

## 3. Cutting board: one input, one outcome

Rather than add CuttingDelight, I detected the ambiguity directly — every single-input workstation recipe, grouped by input **and tool**, looking for inputs with more than one outcome. Across the cutting board, mortar, juicer and both drying racks there were only **4**, and two of those turned out to be phantom entries for items that are not registered (Graveyard Delight's bone dust, Ecologics' azalea flower). The two real ones are fixed by merging the items instead of picking a recipe:

| Input | Was ambiguous between | Fix |
|---|---|---|
| Ender pearl | `endersdelight:ender_shard` x2 vs `ends_delight:ender_pearl_grain` x4 | merged, grain wins (3 uses vs 1) |
| Pufferfish | `crabbersdelight:pufferfish_slice` x2 vs `oceansdelight:fugu_slice` x6 | merged, pufferfish slice wins (4 uses vs 2) |
| Cooked duck (Naturalist) | compatdelight's own `cut_cooked_duck` vs `duck_meat` | `duck_meat` recipe switched off |

Everything else that looked ambiguous is tool-differentiated and therefore fine: grater vs knife on lemon, lime, orange, potato and ginger, for example.

## 4. Ecologics compat recipes

Ecologics is gone, so its compat recipes were dead. Almost all of them stay dead on purpose: **compatdelight only registers its Ecologics items when Ecologics is loaded**, so a recipe producing `compatdelight:coconut_cookie` or consuming `compatdelight:coconut_box` can never work, remapped or not. Only recipes whose inputs and outputs all exist were remapped — that left **2**, both Delightful's cooked crab (now from Youkai crab meat). The mapping used:

`ecologics:coconut` → Crabber's Delight coconut, `coconut_slice` → coconut halve, `crab_claw` → Crabber's Delight, `crab_meat` → Youkai's, `prickly_pear` → Yung's prickly peach, `ice_bricks` → Youkai's.

## 5. Drying racks — both kept, recipes cross-ported

16 recipes ported, both racks keep everything:

- **Youkai's bamboo rack is twice as fast** (Extra Delight's times halved, floor of 100 ticks) because it only works under open sky.
- **Extra Delight's rack is twice as slow** on Youkai's recipes, but works indoors and underground.
- A maid can operate the Youkai rack (MaidsoulKitchen `TaskYhcDryingRack`); there is no task for Extra Delight's.

## 6. Kettles and kegs — keep all four, and why

From the MaidsoulKitchen build in `maidsoul-fix`:

- Its cooking tasks cover **Youkai's tea kettle, fermentation tank, drying rack and moka pot**, plus **Brewin' and Chewin's keg**, Farmer's Delight, Barbeque's Delight, Cuisine, Simple Farming and vanilla furnaces.
- **Farmer's Respite has no maid task.** The jar mentions `farmersrespite` only in its mod-id list; the shipped `maidsoulkitchen.mixins.json` has mixins for `brewinandchewin.KegBlockEntityMixin` and `youkaishomecoming.BasePotBlockEntityMixin`, and none for Farmer's Respite.
- Your patch removes `yhc.KettleBlockAccessor` from the mixin list for 4.2.14. The kettle task classes are still there and still referenced, but **worth testing in game** that a maid actually brews at the Youkai kettle — that accessor is how the task used to read the kettle.

On the fluid question: **Youkai's kettle in 4.2.14 is not fluid-based.** Its recipes are `container` (glass bottle) + ingredients → item, exactly like a cooking pot. It was the *fermentation tank* that is fluid-to-fluid (`inputFluid` grape juice → `outputFluid` burgundy). So no refactor is needed for teas.

**Fluids cannot be added by datapack or resource pack.** A fluid is a registry entry with a type, still-and-flowing block, bucket item and textures, all registered in code — recolouring water only changes how water looks. So porting Youkai's fermentation to the keg would need either a small mod or the keg accepting Youkai's fluids, which is not how its recipes are written. Since a maid can run both the keg *and* the fermentation tank, leaving them as separate lines costs nothing.

## 7. Farmer's Respite merge re-run

Fresh baseline dump taken with the original AlmostUnified config and the merge pack pulled out, on a server synced to the current mod list (30,060 recipes seen). Result: **143 groups** (was 132).

New or extended groups: black, green and yellow tea; black tea leaves; green tea leaves; tea seeds; coffee; coffee beans; coffee berries; coffee cake and its slice; apple cider; hot cocoa; plus the ender pearl and pufferfish pairs above.

- **Teas go to Youkai** because a maid can run that kettle. Farmer's Respite's kettle still brews them — its `kettle_pouring` recipes are kept as conversions, and AlmostUnified rewrites what they pour into, so both kettles end at the same bottle.
- **Coffee stays Extra Delight**, cider and cocoa stay **Farmer's Delight** (both are ingredients across the pack).

### One hazard worth recording

AlmostUnified rewrites ids wherever it finds them, and **Create fluid results use the same JSON shape as item results**. `create:mixing/yellow_tea` produces 250 mB of the *fluid* `farmersrespite:yellow_tea`, and the merge rewrote that to `youkaishomecoming:yellow_tea` — an item id, in a fluid slot. Four recipes were affected (`create:mixing/` apple_cider, black_tea, green_tea, yellow_tea); they are now listed in `ignored_recipe_ids` in the AlmostUnified config. If any more fluid-backed drinks get merged later, check `unification.txt` for rewritten results that carry an `amount`.

### Head tags and a build fix

48 of the 49 head groups already had a narrow per-head unify tag in the old config (`c:cow_head`, `c:drowned_head` …), so the merge unifies on those, and `c:heads/<mob>` is kept only as the tag Spectrum's recipes read. The build also now always starts from the pristine pre-merge config (`proposed-configs/almostunified-BEFORE-MERGE`) instead of the installed one — reading the installed config made every run inherit the previous run's tags, which is what put heads in two unify tags at once. Your cookie (56) and candy bowl (45) custom tags come from that same file and are kept.

Verified: 25,652 recipes load, MineColonies 189, no errors from any file we ship. The AlmostUnified errors left (squid, tentacle, cucumber, uranium, one Recipes+ recipe) were there before the merge.

## 8. Files

- `datapacks/zz-unified-items.zip` — rebuilt.
- `datapacks/ButcheryCompats-spectrum.zip` — new.
- `config/almostunified/*` — head tags, Farmer's Respite groups, the four ignored recipe ids.
- Sources: `unification/build.py`, `unification/extras.py`, `unification/decisions.py`, `unification/overlaps.py`, `unification/report.json`.
