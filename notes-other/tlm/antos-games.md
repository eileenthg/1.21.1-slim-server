---
name: antos-games
description: AntOS fork at ~/Claude/antos-games with 5 built-in mini-games; AntOS-into-TLM wiring and MrCrayfish computer idea are parked
metadata:
  node_type: memory
  type: project
  originSessionId: 1194174c-60db-44be-b791-61b6a643b37f
  modified: 2026-10-02T02:40:33.112Z
---

Fork of craisinlord/AntOS (no license; user keeps it private) at `C:\Users\eteil.000\Claude\antos-games`, branch `feat/mini-games`, uncommitted as of 2026-10-02.

Added to the AntOS Games app: Coin Miner (original; MrCrayfish's is a "Coming Soon" stub), Paddle Ball vs AI (ported from MrCrayfish Furniture Refurbished, MIT, notice kept in `PaddleBallSim`), Tic-Tac-Toe, Minesweeper, Checkers (original). Chess was dropped by the user. No multiplayer (user may want it later). Glaidens Games jar is All Rights Reserved and closed source, so not ported.

API change: `ComputerGame.Session` gained `hover` and `mouseClicked`; `ComputerScreen` forwards them. Logic is pure Java in `content/minigame`, tests in `neoforge-1.21.1/src/test` (JUnit 4, 19 pass). Client smoke test started clean; the renderers have not been looked at on screen yet.

**Why:** user wants a working in-game computer on the TLM maid computer after sitting down.
**How to apply:** this fork is the staging copy of the games. The AntOS-into-TLM hook was built as a separate addon, see [[tlm-computer-addon]]; keep the two copies of the games in sync if either changes. The MrCrayfish `IComputer` soft-dependency route was evaluated and not used.
