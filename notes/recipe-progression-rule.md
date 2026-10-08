---
name: recipe-progression-rule
description: "User's rule that a harder crafting method must out-yield an easier one; the recipe-shortcuts scanner and the zzz-recipe-progression datapack"
metadata:
  node_type: memory
  type: project
  originSessionId: a7532cd9-95f7-4714-9ac8-6b40e36e846f
  modified: 2026-09-29T01:43:05.902Z
---

Rule the user stated on 2026-09-29: **a more involved method must give more.** A simpler method may sit beside it
only if it yields less, or is more tedious. Automation (Create/Spectrum/Mekanism) is exempt: it is throughput,
not yield. Extra Delight's oven is likewise fine (it bakes up to 9 per pass; the furnace is the tedious route).

Tooling: `modpack-audit/recipe-shortcuts/scan.py` → `report.json`; write-up in `modpack-audit/25-recipe-shortcuts.md`.
It reads vanilla + every mod jar + the pack's datapacks, resolves tags from the pack, applies the merge, and
reports a simpler recipe whose inputs are a subset and output is no smaller.

Two traps it hit, both fixed in the script — re-check if it is ever rewritten:
- **Take the merge map from `logs/almostunified/almostunified.log`** (`[AutoHiding] … tag '#x' -> [hidden]`), not
  from materials.json. The config's `c:crops/{material}` wildcard also matches the *category* tag `c:crops/grain`,
  which fakes a rice/corn/chickpea → wheat merge that AU does not perform. Also honour `ignored_tags`
  (e.g. `extradelight:sweetener`, which otherwise fakes sugar → honey).
- **Only `data/<ns>/recipe/` loads in 1.21.** Several mods still ship `recipes/` (1.20): Butchery's beef/pork/mutton
  cutting conversions and Delightful's `rose_bush_alt3` are dead files, so those knife routes did not exist.

Changes live in `modpack-audit/datapacks/zzz-recipe-progression.zip` (sorts after `zz-unified-items`): tofu grid
1 / pot 2; new cutting recipes for beef, porkchop, mutton (2) and rose bush → red dye (3); sugar and rice cutting
raised to 2; Aquaculture mushroom, Ramadan date and the FD furnace fried egg disabled; saplings removed from the
Recipes Plus stick tag. **Not yet verified in game** (a pregen server from another session was running).

**Why:** the user found 2 soybeans → 2 tofu in the grid beating 2 soybeans → 1 in the cooking pot.

**How to apply:** disable stubs keep the original recipe's whole JSON and only add the false condition, per
[[disable-stub-recipe-shapes]]. Install to the root `datapacks/` ([[modpack-datapack-install-location]]).
Open question the user asked: making the cutting board cost 0 durability for chosen recipes needs a mixin —
Farmer's Delight hard-codes `toolStack.hurtAndBreak(1, …)` in `CuttingBoardBlockEntity` with the recipe in scope.
