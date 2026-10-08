---
name: survival-stack-twt-coldsweat
description: Since 2026-09-19 thirst = Thirst Was Taken, temperature = Cold Sweat (TAN removed); drink handling via custom drinkfix mod and TWT item_settings
metadata:
  type: project
---

On 2026-09-19 the user replaced Tough As Nails with Thirst Was Taken 2.1.5 and Cold Sweat 2.4.3.1 (plus the addons Altitude, Create: Cold Sweat, More Sweat Compat and cslcrops). Thirst Was Reclaimed was also dropped in by mistake. It shares mod id `thirst` and lacks the classes that Ender Canteen and Water Canteens call, so it went to `removed-mods`. Keep Taken.

- Custom mod `drinkfix` (`C:\Users\eteil.000\Claude\drink-fix`): items in `#drinkfix:drinks` drink instead of eat and hand back `#drinkfix:returns/*` containers. It covers the 125 Compat Delight drinks.
- `config/thirst/item_settings.toml`: TAN entries replaced with the Compat Delight drinks and the new juice winners. The default list is kept at `proposed-configs/thirst-item_settings.DEFAULT.toml`.
- The installed Thirst Was Taken jar is **patched** (`modpack-audit/fixed-mods/patch_thirst.py`, pristine copy alongside it as `.ORIGINAL-BACKUP`): its `jade.MixinFluidView` is removed (it fails its required injection against Jade 15.10.6 and hard-crashes the tick loop the first time Jade reads a fluid block), and one byte in `PlayerThirst.tick` turns `getHealth() > 0.0F` into `> 1.0F` so thirst cannot kill on Normal, matching vanilla hunger. Rerun the script after any TWT update. See `modpack-audit/25-thirst-jade-crash-and-normal-death.md`.
- Purified water in recipes is `minecraft:potion` with `thirst:purity` = 3. Drink Fix 1.1.0 also makes the Extra Delight mixing bowl enforce the potion and purity of its container (tested; test mod at `modpack-audit/test-client/drink-tester`).

**Why:** the user asked for juices to be drinks, not food.

**How to apply:** new drink items from other mods need entries in both the tag and item_settings. Cold Sweat's `cold_scaling`/`hot_scaling` damage is in no_knockback via zz-unified-items. See [[item-merge-pipeline]].

**Season Climate mod (2026-09-20):** `modpack-audit/seasonclimate` (mod id `seasonclimate`, jar installed in the instance, v1.0.0). Cold Sweat applies Serene Seasons' offset to the whole dimension (its SereneSeasonsTempModifier checks only the dimension whitelist, never the biome), so no location escaped winter. This mod listens to `TempModifierEvent.Calculate.Post`, matches that modifier by class name and scales only its offset by the player's biome. Defaults in `config/seasonclimate-common.toml`: tropical (#sereneseasons:tropical_biomes) 0.15, warm 0.3, temperate 0.5, cool 1.0, cold 1.25, ocean/beach x0.7, plus id/#tag overrides.
Built and installed WITHOUT testing at the user's request (they were low on tokens) — it has never been run. Check the log for `[seasonclimate]` and the config generating on first launch.
Also changed that day: `sereneseasons/seasons.toml` `starting_sub_season` 1 -> 2 (Mid Spring, month "Budwake") because a new world started in Early Spring, which is -8.4F in Cold Sweat and put plains nights below the 48.8F habitable minimum. Cold Sweat habitable band: 48.8-95.5F; season mid-swing is +/-25.2F. Backups: `proposed-configs/*.BEFORE.20260920.toml`.
