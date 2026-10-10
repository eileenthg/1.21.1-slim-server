---
name: tlm-computer-addon
description: Working Maid Computers - standalone NeoForge addon that opens a vendored AntOS desktop when you right-click a TLM computer while seated; status and caveats
metadata:
  node_type: memory
  type: project
  originSessionId: 1194174c-60db-44be-b791-61b6a643b37f
  modified: 2026-10-02T03:20:19.085Z
---

Repo `C:\Users\eteil.000\Claude\tlm-computer-addon`, mod id `workingmaidcomputers`, package `com.github.eileen.workingmaidcomputers`. `git init` done, **nothing committed yet** (as of 2026-10-02). Same pattern as [[tlm-trumpet-addon]]: no compile-time dependency on TLM, recognises `touhou_little_maid:computer` and `touhou_little_maid:sit` by registry name, so TLM itself needs no hook.

Contains a vendored copy of AntOS (`com.craisinlord.antos`, minus its block, block entity, item, renderer and phone) plus the five mini-games from [[antos-games]]. A `RightClickBlock` handler at HIGH priority: empty hand while seated at the computer -> client opens `ComputerScreen`; floppy in hand -> server installs the disk into the signed-in Anternet account. AntOS is account-based, so no block entity is needed.

**Why:** the user asked for the AntOS hook to live in a separate addon jar, not inside their TLM fork, so the TLM working tree (which holds uncommitted keyboard-music work) stays clean. The TLM tree was restored to exactly the keyboard WIP.
**How to apply:**
- AntOS has no licence and the Paddle Ball port is MIT, so the repo is private use only; never publish it.
- Verified: unit tests (19), a game test run against the stock TLM jar `touhoulittlemaid-1.5.3-neoforge+mc1.21.1-all.jar` (3 pass), and `runClient`. Not verified: actually clicking through the screen and the game layouts in a running client.
- Dev runs need `-PtlmJar=<TLM jar>`; the TLM dev env itself needs `-Pneo_version=21.1.228` because slashblade wants it.
- Game tests must not use `makeMockServerPlayerInLevel` (its connection rejects mod payloads); see `ComputerGameTests.mockPlayer`.
