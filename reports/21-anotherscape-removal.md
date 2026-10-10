# 21: AnotherScape removed (19 Sep)

AnotherScape is out of the pack. Eternal Tales and every other mod stay (Waystones too: it was in the pack before AnotherScape). Everything taken out is in **`anotherscape mods/`** with a README on how to put it back.

## Removed from the instance
| Item | What it was |
|---|---|
| `anotherscape-1.0.35.jar` | the mod (the chunk-guard patched build) |
| `godswordspecs-1.21.1-1.1.0.jar` | OSRS Special Attacks (report 20): only applied to AnotherScape weapons |
| `datapacks/anotherscape-townsfolk-no-guild.zip` | townsfolk spawn tweak |
| `config/spawnonboarding-common.toml` (+5 `.bak`) | every setting in it was AnotherScape-only |

## Changed
| Where | Change |
|---|---|
| **spawn-onboarding** → 1.1.0 | Cut: hub placement/paste/freeze/protection, hub respawn, guild NPCs, village staff, townsfolk, sea spawns, starter-pet screen, NPC hold-back, 5 AnotherScape mixins. Kept: MCA destiny timing, the Origins screen queue, Eternal Town placement (now searched around the spawn point instead of beside the hub), tarantula hawk hunting. No config file any more. The build has zero AnotherScape references. |
| **creature-links.zip** | AnotherScape entries dropped. Kept: Eternal Tales / Enderscape / Deeper Darker / Enhanced Celestials arthropods, ET and Youkai deer as prey, ET bugs for the warped toad, the ET sunflower spider for the tarantula hawk. |
| **Compat Delight** (patched jar) | Naturalist module now keyed to Aquaculture instead of AnotherScape. |
| **zz-unified-items.zip** | Compat Delight stand-ins: raw bass → Aquaculture smallmouth bass, raw marlin → Aquaculture tuna, cooked bass/marlin → Aquaculture cooked fish fillet; the 2 lizard-tail cutting recipes turned off (no lizard meat left in the pack). Patched in place, see below. |
| **AlmostUnified** config | `c:shrimp` is now Crabber's + Eternal Tales; `c:gems/ruby` is Eternal Tales only; AnotherScape out of mod priorities. |
| **Lost Maids** config | 69 AnotherScape items removed from the lost-maid gear blacklist. |
| **REI** config | the `anotherscape:osrs_raids` entry removed. |
| **unification/** sources | `extras.py` stand-ins, shrimp group in `decisions.py`/`candidates.py`, and `REMOVED_MODS = ('anotherscape',)` in `build.py` (drops its entries from the pre-merge AU config), so a future rebuild matches. `patch_compatdelight.py` rekeys naturalist → aquaculture. |

**Why patched in place rather than rebuilt:** `unification/index.json` (the recipe index build.py reads) no longer exists. A rebuild means re-running `index.py` over all 460 current mods, which would also pull in everything added since the merge. Only the 7 affected recipe files were touched.

**Left alone on purpose:**
- `config/apotheosis/enchantments.cfg` and `name_generation.cfg`: Apotheosis regenerates these itself.
- `config/patpat` stats.
- `colony-autoteach-proposed/`: it has AnotherScape recipes but isn't installed.
- The test-server folders (A-full, B-no-roadweaver, X-expeditions, baseline) still carry AnotherScape jars. A-full is shared with another session. They are about 430 MB each; C: has 7.9 GB free.

## Verified
Test launch on a copy of New World (1):
- Everything loads.
- spawn-onboarding reports `mca=true origins=true eternal_tales=true`.
- The maid fix still fires.
- No errors from Compat Delight, the item merge, creature-links, AlmostUnified or spawn-onboarding.
- The set of load errors is **identical** to yesterday's run with AnotherScape (126 lines, no new ones). The only additions are the old world's missing AnotherScape blocks (its ores), which is expected.

## The world
New World (1) is untouched in `saves/` and also copied into the archive. Opening it now leaves holes where AnotherScape's ores were and removes its hub blocks and NPCs. As planned, start a fresh world. Eternal Town placement and the MCA/Origins onboarding still run in it.
