---
name: fd-cutting-board-hint-patch
description: FD 1.3.4 jar patched so the cutting board says "doesn't seem cuttable" for items with no cutting recipe; Youkai pods recipe restored
metadata:
  type: project
---

25 Sep 2026: the FD jar in the pack (`FarmersDelight-1.21.1-1.3.4-patched.jar`, already a patched jar before this) got an ASM patch: getMatchingRecipe message keys now come from an added `CuttingBoardHint` class. Sources: `modpack-audit/fixed-mods/fd-cutting-hint-patch/`. Notes: `27-youkai-pods-and-cutting-board-hint.md`. Also on 25 Sep, `youkaishomecoming:cutting/pods_cutting` was taken out of zz-unified-items (build.py KEEP_IDS).

**Why:** upstream FD always showed "Maybe with a different tool" because it matches item+tool together. The soybean merge had left Youkai pods with no use.

**How to apply:** redo the patch after any FD update. Both were installed on 25 Sep (checksums verified) but haven't been tested in-game yet. See [[item-merge-pipeline]].
