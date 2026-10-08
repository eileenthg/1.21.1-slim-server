---
name: no-test-servers-while-hosting
description: "Don't boot test servers/clients while the user hosts, or while another Claude session's test server/client is running (check all java processes first); causes lag/watchdog freezes"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 55087821-9ef3-4df8-8698-9cf4ca394687
  modified: 2026-09-26T03:58:49.935Z
---

Don't boot `test-server/X-expeditions` (or any other headless server) while the user is hosting their singleplayer/LAN world with Youchrist. Ask first, or check whether a `javaw`/Prism client is running an integrated server with players joined.

**Why:** on 26 Sep 2026, test servers ran back to back from 11:27 on. The hosted world lagged 2–60 s about every 20 s from 11:34 on and hit a 40 s ModernFix IntegratedWatchdog freeze at 11:48. The test boots took up the CPU, so C2ME worldgen slowed down. LazyChunkLoad 1.3.0's preload (`ChunkLoadScheduler.processPreload` → `Level.getChunk`) loads chunks on the main tick thread and waits on them, so the slow worldgen froze the tick. Earlier sessions with no test servers had only 2–4 "Can't keep up" warnings each.

**How to apply:** before a harness boot (see [[test-server-a-full-stale-mods]]), check `latest.log` for recent "joined the game" lines without a matching "left the game", or just ask. `preload.enabled = false` was set in `config/lazychunkload-common.toml` on 26 Sep; the backup is `.bak-20260926`. It takes effect on the next game restart. If lag spikes come back without test servers running, look at LazyChunkLoad again. The `Timm … marketstead_village_biomes` log spam is harmless.

**Also check for other Claude sessions' test runs (1 Oct 2026).** The user runs several Claude sessions against this pack at once. Before launching any test client or server, list every `java.exe`/`javaw.exe` process and its command line. A NeoForge server (`@libraries/net/neoforged/neoforge/.../win_args.txt nogui`) or a Minecraft client that this session didn't start means another session is testing, so wait. `run-*test.ps1` only refuses when a *client* is running; it misses dedicated servers. If a harness run is stopped midway, its `finally` cleanup doesn't run, so remove the tester jar from `mods/` and the world copy from `saves/` by hand.

**Standing instruction (user, 1 Oct 2026):** when another server or client is running, don't ask and don't give up. Watch it (Monitor on its PID or process list) and launch the test on your own once it has closed. Apply this to every future run.
