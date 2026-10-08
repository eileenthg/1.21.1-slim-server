---
name: timm-structure-lag
description: "Immersive Music Mod (timm) structure check: re-enabled 27 Sep at user's request; cost analysis + cheaper patch-mod idea (not built)"
metadata:
  type: project
---

On 26 Sep, `enableStructure` in `config/timm-client.toml` was set to false because timm's "Structure distance was not registered" log lines sat near "Can't keep up" stalls. **The user had it switched back on on 27 Sep** by restoring `.bak-2026-09-26`.

A javap reading of `ServerPlayerMixin` shows the check runs about once a second per player on the server thread. It calls `getAllStructuresAt` on a hard-coded 5×5 chunks. For structures that have a playlist it then calls `startsForStructure`, which can load the start chunk from disk synchronously. It then tests bounding boxes inflated by a per-structure `distance`. Distances can be overridden in `config/timm/structure_playlists.json` (the default is in the jar at `assets/timm/custom/structure_playlists.json`). Setting distance to 0 gives inside-only music but does NOT lower the cost. Structures are named after the last segment of the structure's biome tag, so modded ones (taverns, marketsteads) just log and are skipped.

The evidence tying timm to lag is weak. Its lines are about a third of the log, and the 26 Sep stalls were mostly test servers plus LazyChunkLoad preload (see [[no-test-servers-while-hosting]]).

Offered but not built: a patch mod that checks only the player's own chunk, filters to structures with playlists first, uses loaded chunks only (getChunkNow), and drops the log spam.

**How to apply:** don't disable timm again as a lag fix without Spark evidence. If the user asks for cheaper or inside-only structure music, build the patch mod, and test it only when they aren't hosting.
