---
name: corn-delight-raw-tortilla-off
description: "Corn Delight raw tortilla crafting recipe disabled via standalone datapack (8 Oct); tortilla merged into Cultural Delights'"
metadata:
  node_type: memory
  type: project
  originSessionId: bc2e8453-fe50-492d-97ab-431cb7567677
  modified: 2026-10-08T06:47:45.497Z
---

8 Oct 2026: `datapacks/corn-delight-raw-tortilla-off.zip` (live instance root datapacks/) stubs `corn_delight:tortilla_raw` (the crafting recipe: 3 corn + water bucket -> 3 raw tortilla).

**Why:** [[item-merge-pipeline]] already disabled all three cooking recipes that turn it into `corn_delight:tortilla` (merged into `culturaldelights:tortilla`, route = corn dough then cook), so the item was a dead end.

**How to apply:** it is a separate pack so a zz-unified-items rebuild doesn't drop it. Item still shows in recipe viewers/creative; only obtainability was cut. Needs world reload/restart; untested in-game. Stub shape follows [[disable-stub-recipe-shapes]].
