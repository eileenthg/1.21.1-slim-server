---
name: colorful-azaleas-removed
description: "Colorful Azaleas removed 5 Oct 2026; autoteach datapack stripped of its 136 recipes; world had none placed"
metadata:
  type: project
---

On 5 Oct 2026 the user removed Colorful Azaleas. In the pack it only gave Spectrum 28 optional ink-converting recipes and a guidebook page. The jar was moved to `removed-mods/colorfulazaleas-2026-10-05/`. A read-only scan found 0 chunks of the live world containing its blocks, and no player inventories with its items (level.dat only lists it as a mod).

`datapacks/colony-autoteach.zip` was rebuilt in place, same filename, without the 136 crafterrecipes naming colorfulazaleas items (120 dyer, 16 sawmill). An unregistered id in a crafterrecipe bricks server boot (see [[colony-autoteach-datapack]]). The original is at `datapacks-retired/colony-autoteach.BEFORE-AZALEA-REMOVAL.zip`. On the next generator rerun, the fresh item audit drops these automatically.

**How to apply:** when removing any mod, grep colony-autoteach.zip (and other datapacks) for its namespace first.
