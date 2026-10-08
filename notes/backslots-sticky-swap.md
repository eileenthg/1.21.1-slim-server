---
name: backslots-sticky-swap
description: "Back Slots patched so the quick-swap key swaps and stays swapped (like offhand F); installed 25 Sep, in-game untested"
metadata:
  type: project
---

On 2026-09-25 the user wanted the Back Slots quick-swap to stay swapped, the way the offhand key does, instead of only while the key is held.

**Cause:** in the original mod, `BackSlotsClient.onClientTick` sends `SwapBackSlotPacket` on key press AND on key release, and the server swaps on both.

**Fix:** only `BackSlotsClient.class` was recompiled, with the send on release removed. The server handler is unchanged. The source is at `modpack-audit/backslots-src`: `src/` holds the patched file, `decomp/` the vineflower output, and `out/` the compiled class. It was compiled with javac against gentle-swings' `build/moddev/artifacts/neoforge-21.1.249-merged.jar` plus the bus, FML loader and mergetool-api jars from the gradle cache.

**Files:**
- `fixed-mods/backslots-1.1.5-stickyswap.jar` is installed in the slim instance and replaces `backslots-1.1.5.jar`.
- The original jar is kept as `fixed-mods/backslots-1.1.5.jar.ORIGINAL-BACKUP`.

**Keybinds:** Back Slots is bound to G in options.txt. Gentle Swings stays on its default H, so there is no clash. See [[gentle-swings-mod]].
