---
name: chesspvp-seated-patch
description: Rewrite of the closed-source ChessPVP addon (TLM boards) so players sit at either side instead of crouch-clicking; state and gaps
metadata:
  node_type: memory
  type: project
  originSessionId: 32440bd2-43df-4436-a4af-a9a16a5302d2
  modified: 2026-10-02T01:49:15.678Z
---

Published ChessPVP (CurseForge `tlm-cp-chesspvp`, jar `chesspvp-1.21.1-neoforge-1.0.0.jar`, author zawu, "All Rights Reserved", no source) was decompiled with Vineflower (`C:\Users\eteil.000\Claude\alex-mobs-butchery\_research\vineflower.jar`) - three small mixins into TLM's `BlockGomoku/BlockCChess/BlockWChess.useItemOn`. The original jar lives in `modpack-audit/test-server/*/mods/`.

Rewritten at `C:\Users\eteil.000\Claude\chesspvp-seated` (mod id `chesspvp`, v1.3.0 (adds checkmate/stalemate, move-limit and repetition draws for both chess variants), builds to `build/libs/chesspvp-1.3.0.jar`). Right-click a side with an empty hand to sit on a TLM `EntitySit` (ids in the board's persistent data `ChessPvpSeat1/2`); crouch = vanilla dismount. Decisions the user made: on standing up **keep the board and let anyone take the free seat**. Seat 1 = opposite the maid's side, moves first (black Gomoku / red Chinese chess / white chess); Gomoku turn is derived from stone counts.

**Why:** the user wanted a chair-like sit-down flow instead of crouch-right-click. Private patch only - do not publish without the original author's permission.

**How to apply:** `./gradlew.bat runGameTestServer` runs 9 headless GameTests (needs `libs/touhoulittlemaid.jar`, git-ignored). GameTest players must be hand-built `ServerPlayer`s - `makeMockServerPlayerInLevel` trips TLM's join packet and `FakePlayer` refuses to ride.

Not verified in a real client: seat height (`SEAT_HEIGHT` 0.45, guessed), player body yaw, action-bar messages. Not done: maid-present passthrough has no test. Players can look around freely while seated (camera is client-side), so seat yaw only sets the initial body direction.

**Installed 2 Oct 2026, 10:14** in the Prism instance `1.21.1 slim` (`mods/chesspvp-1.3.0.jar`); the original 1.0.0 jar was moved to that instance's `mod-backups/chesspvp-1.21.1-neoforge-1.0.0.jar.bak-20261002-seated`. The `modpack-audit/test-server/*/mods/` copies still hold the old jar. Live instance = `1.21.1 slim` per the modpack-audit install precedent.
