---
name: blockstate-desync-seamless-chunks
description: 28 Sep ghost lava + post-death block desync on the hosted LAN world; prime suspect Seamless Chunks 0.1.0-beta.7 — unconfirmed
metadata:
  node_type: memory
  type: project
  originSessionId: 4f067a18-0cd6-4b7b-b5a7-d85f6e177135
  modified: 2026-09-28T06:36:48.765Z
---

On 28 Sep 2026 the user reported two problems on their LAN-hosted world. First, ghost lava: a bucket places lava only on the client, and it doesn't burn the player. It fails against cobblestone and works against dirt. Second, after a death at 14:07 (PlayerRevive bleed-out, then "tried to swim in lava") blocks desynced: door halves moved separately and mined blocks didn't break. A world restart at 14:23 cleared it. The log shows no errors for either problem.

Prime suspect: `seamlesschunks-neoforge-0.1.0-beta.7`. Its mixins replace `PlayerChunkSender.sendNextChunks` and `onChunkBatchReceivedByClient`, skip `ChunkHolder.getChunkToSend`'s sendSync gate, and skip `ChunkMap.waitForLightBeforeSending`. It also caches an `activePlayer`, which may go stale after a respawn. Vanilla does not send block updates for chunks it thinks the client hasn't received yet, which would explain both problems. Other possible causes looked at: Fluidlogged beta.8 and Mythic Metals' Fabric BucketItemMixin (via Connector).

**Why:** this is unconfirmed. If desync comes back, look here first.

**How to apply:** to test, set `optimizationsEnabled=false` in `config/seamlesschunks.properties`, or remove the mod. Check whether the cobble-vs-dirt difference is really a chunk-by-chunk difference (F3+G shows chunk borders). See [[villager-tps-tuning]] for other perf mods in the pack.

28 Sep follow-up: the user confirmed that cobble fails and dirt works *in the same chunk*, so a stuck chunk doesn't explain that part. `optimizationsEnabled=false` was set with the game closed (backup `seamlesschunks.properties.bak-20260928`). That switch gates shouldSkipSendSync, shouldSkipLightWait and rate adaptation (checked with javap). The in-game pause-screen toggle saves this file, so it can flip back. If lava still fails against cobble after the change, look next at the Immersive Weathering and Dimensional Tears `LiquidBlockMixin`/`LavaFluidMixin` fluid-interaction hooks.

- **Later diagnosis (28 Sep evening, session 4f067a18):** buckets fail against stone and dirt because the SERVER refuses the placement. Ruled out Settled Caves, MineColonies, configs, the origin. Main suspect **Fluidlogged 2.0.1-beta.8** (replaces the bucket fluid check, hooks doors); second Mythic Metals' bucket hook. Proposed: friend tries buckets, then remove Fluidlogged after a backup. Owner never answered; unresolved.
