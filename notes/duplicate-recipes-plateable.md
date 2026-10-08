---
name: duplicate-recipes-plateable
description: zzz-duplicate-recipes datapack — rule "keep only the Display Delight plateable version of duplicate recipes"; what was disabled 8 Oct and what was left
metadata:
  type: project
---

8 Oct: user asked to keep, for duplicate recipes, only the one whose result Display Delight can plate (tags `displaydelight:displayable`, `plate_displayable`, `small_plate_displayable`).
Datapack `datapacks/zzz-duplicate-recipes.zip` (source folder beside it, installed live) disables 17 recipes via copied-original + `neoforge:false` stubs:
- ED/Haunted Harvest grilled corn on cob (furnace/smoker/campfire/oven x9, 15 total) — Corn Delight `grilled_corn` is the plateable one (AU already merges corn_delight:corn -> extradelight:corn_on_cob, so its recipes take ED corn).
- Compat Delight smores + its cooked_smores (ED `smore` is plateable).
Scan method: cluster recipes by family (heat/pot/craft), ingredient sets subset-related, differing unified results, exactly one displayable. Only these 2 clusters were real; cod stew (FD baked vs Youkai pot) left as a judgement call, 12 name-similar dishes with different inputs left (cheese sandwich/cheeseburger Compat, hot cocoa/melon juice Farmer's Respite, onion/potato soup, etc.).

**Why:** user wants one obtainable version per dish, and the platable one.
**How to apply:** untested in game (needs restart). Loser items stay registered but unobtainable. See [[disable-stub-recipe-shapes]], [[recipe-identity-audit]].
