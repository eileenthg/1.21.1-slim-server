---
name: item-merge-pipeline
description: How duplicate items are merged in the 1.21.1 pack (AlmostUnified + zz-unified-items datapack) and how to rebuild/verify it
metadata: 
  node_type: memory
  type: project
  originSessionId: 6646a658-2cdb-4a47-8810-94021639f248
  modified: 2026-09-27T03:44:26.366Z
---

Since 2026-09-15 the pack merges 132 groups of same-thing items. The user's rule: keep the item with the most involved recipe chain and leave only that recipe. The merge lives in `config/almostunified/*` plus `datapacks/zz-unified-items.zip`. Sources are in `modpack-audit/unification/` (decisions.py = groups and dominants, build.py = generator, report.json, merge-table.md). MineColonies worker recipes are retargeted, never removed. The old minecolonies-crop-freedom pack is folded in.

**Why:** the user wants one item per real-world thing, and colonies must keep working with the merged items.

**How to apply:**
- build.py reads the AU config from `proposed-configs/almostunified-BEFORE-MERGE`, never the installed one (reading the installed one made runs inherit old tags -> items in two unify tags). Current baseline is `unification/baseline_dump2` (16 Sep, includes Farmer's Respite).
- Rebuild only from a baseline AU dump taken with the ORIGINAL config. Enable `debug.json` dumps on test-server/A-full and copy `logs/almostunified/debug`. A dump from a run that already has the merge makes build.py find nothing to disable.
- Never reuse broad existing tags (`c:foods/cookie`, `c:tea_leaves`, `c:dough`) as unify tags. Use `c:unified/<name>`.
- AU's "Recipes Before" count ignores `neoforge:false` overrides; check RecipeManager "Loaded N recipes" instead.
- User-requested alternative pipelines (16 Sep) live in build.py's "alternative pipelines" block: Spectrum hot chocolate and fresh chocolate via `#c:chocolate/milk/bar`, plus the juicer rules below. Keep them on rebuild. Zip with `zippack.py`.
- Juices (16 Sep): Extra Delight's juicer is the only juicer; Expanded Delight's block and recipes are gone, TAN's crafting-table juices are off, bottling costs purified water, and apple/pumpkin/chorus/cranberry come from the ED mixing bowl (needs "fluids": [] in the JSON).
- Second pass (16 Sep) lives in `extras.py`, called from build.py: geode rarity (all three at 1 in 32), 49 `c:heads/<mob>` groups (Supplementaries > Butchery variants > Spectrum > Butchery) with 87 Spectrum recipes retargeted by hand, Butchery pestle -> ED mortar, Eternal Tales cutting table -> FD board.
- The user's cookie additions live in `config/almostunified/tags.json` custom_tags (`c:foods/cookie`, `extradelight:candy_bowl_valid`) and feed Supplementaries' cookie jar. build.py reads and rewrites that file, so never hand-write tags.json from scratch.
- AU rewrites ids in Create fluid results too; recipes whose result is a fluid sharing an item id go in `ignored_recipe_ids` (the four `create:mixing/` tea and cider ones).
- WaterUpgrade 1.0.0 is a patched jar (`fixed-mods/waterupgrade-1.0.0-1.21.1-busfix.jar`, script `fixed-mods/patch_waterupgrade.py`): it registered a mod-bus creative-tab listener on NeoForge.EVENT_BUS and failed construction. Re-patch or drop if the author updates. `reinforced_construction` still needs `fusion` (client-only) on the test server.
- Compat Delight is a patched jar (`fixed-mods/compatdelight-1.0.1.1-substitutes.jar`, `unification/patch_compatdelight.py`): module keys rewritten ecologics->crabbersdelight, naturalist->aquaculture (was anotherscape until 19 Sep), hexerei->youkaishomecoming, nourished_nether->eternal_tales, tropicraft->pineapple_delight, rats->brewinandchewin. extras.py `CD` table substitutes their recipes; Apotheosis module recipes remapped to Apothic Enchanting items and rarity materials. Re-patch after any Compat Delight update; original in `fixed-mods/original/`. 27 Sep: user declined re-keying the Malum module (Malum not installed; only flesh has a stand-in, the one flesh dish also needs runic sap + soulwood).
- 26 Sep: tentacles now unify on `c:unified/tentacles` (Miner's/Butchery/Ocean's), not `c:tentacles`. Cocktails Delight fills `c:tentacles` with Crabber's squid + glow squid tentacles, Alex's lost tentacle and calamari, which merged them all (squid tentacles crafted the glow squid barrel). Fixed in extras.py and patched into the installed config in place (backups `proposed-configs/almostunified-*.BEFORE-TENTACLE-FIX.json`). Test-server configs still have the old tag. In-game unverified.
- AU 1.4.2 config is host-only: the host's AU writes the recipe-viewer hide list into an item tag that syncs to clients (`ItemHider.applyHideTags` ← `AlmostUnifiedRuntimeImpl`; the client viewer plugins read it on `ClientTagUpdateEvent`). The friend never needs AU config files.
- The old AU config's own overlaps caused the squid/tentacle/cucumber/uranium errors; extras.py fixes them (drop c:squid/c:glow_squid unify tags, tentacles out of raw squid_tentacle, uranium ignored, c:crops/cucumber replace:true).
- Bug meats: Butchery bee back / silverfish / endermite are NOT unified into Miner's Delight arthropod. Both arthropod tags stay in `ignored_tags` (user tried the merge on 17 Sep and reverted it); the custom tags still let Miner's Delight recipes accept them.
- Pufferfish: Crabber's pufferfish slice wins over Ocean's Delight fugu slice (user reversed their choice for the deeper mechanics).
- extras.py also adds `toughasnails:thirst` and `hyperthermia` to `minecraft:no_knockback` (thirst ticks bounced players, 18 Sep).
- Datapacks for new worlds come from Simple Datapacks `requiredDatapacksPaths: ["datapacks"]`. See [[test-instance-copy-resets-configs]] for restoring configs.

- 19 Sep: AnotherScape removed. build.py drops its entries via `REMOVED_MODS`; index.json is gone, so the installed zz-unified-items was patched in place instead of rebuilt (bass/marlin -> Aquaculture, lizard recipes off). A full rebuild needs index.py re-run first and will pick up every mod added since. See [[anotherscape-removed]].
- 19 Sep: Tough As Nails and tanca removed (user switched to Thirst Was Taken + Cold Sweat). Juice groups now won by Extra Delight / Expanded Delight / Farmer's Delight, hot chocolate by Spectrum; purified water = minecraft:potion with thirst:purity 3 (neoforge:components ingredient; ED's mixing bowl only checks the item). no_knockback now carries cold_sweat:cold_scaling/hot_scaling. Patched in place + scripts updated (REMOVED_MODS). Pre-change copies: datapacks-retired/zz-unified-items.BEFORE-TAN-REMOVAL.zip, proposed-configs/*BEFORE-TAN-REMOVAL*.- 25 Sep: MC Trade Post crafting-table foods (stir fry, chickpea stroganoff, mystic tea, veggie juice, wish_gathering_cow) still used MineColonies crops, because crop-freedom only covered the minecolonies namespace and AU's crop tags don't contain MineColonies crops. `extras.tradepost_crops()` swaps in the crop-freedom stand-ins and adds non-colony onion/garlic/mint/chickpea/etc. to `mctradepost:seasoning/protein/stew_ingredients`. Patched into the installed zip in place (backup `datapacks-retired/zz-unified-items.BEFORE-TRADEPOST-CROPS.zip`); not yet boot-tested.
