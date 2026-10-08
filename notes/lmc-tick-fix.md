---
name: lmc-tick-fix
description: lmctickfix mod works around Load My Chunks 1.2.5.1 "Exception ticking world" (AIOOBE in Long2ObjectLinkedOpenHashMap iterator)
metadata:
  type: project
---

On 25 Sep 2026, Load My Chunks 1.2.5.1 (the latest release) crashed the world tick twice. Its entity-tick override walks ChunkMap's live updatingChunkMap, and ticking rehashes it. The fix mod `lmctickfix` (source `C:\Users\eteil.000\Claude\lmc-tick-fix`) redirects the `values()` call in `LoadedChunkProviders$1.iterateChunks` to a snapshot, retries the copy, and skips the tick as a last resort because C2ME is also in the pack. It is installed in `1.21.1 slim`. Write-up: modpack-audit/18-take-a-seat-and-mca-models.md (end).

**Why:** a crash in normal play (12:05) and in the test harness.

**How to apply:** the mod depends on `loadmychunks` pinned to [1.2.5.1]. On an LMC update, check whether upstream fixed this and the mod can be dropped, or bump the version range. Four test runs passed without a crash, but it isn't proven over long play.
