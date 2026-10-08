---
name: automodpack-installed
description: "AutoModpack 4.0.6 (NeoForge 1.21.1) installed 4 Oct to sync the pack to the friend; LAN/Essential hosting unverified"
metadata:
  type: project
---

4 Oct 2026: installed `automodpack-mc1.21.1-neoforge-4.0.6.jar` (CurseForge file 8606541, 15,397,914 bytes) in the live `mods/`, with a copy in `~/Downloads` for the friend (Youchrist). It's excluded from both test servers (`automodpack-*` in server-exclude.txt), so harness boots don't build or host a pack.

Not yet verified: whether it hosts from the user's integrated/LAN world and works through Essential's P2P join (the docs and jar don't say; its mixins hook MinecraftServer and the login handler, which suggests it might). The server config is created on the first host launch; check which folders it syncs (mods/config by default). The root `datapacks/` folder and the `resourcepacks/` content (e.g. youkai-port-lang-fix) may need adding.

**Why:** the user wants the friend's client kept in sync without hand-made update ZIPs; see [[friend-update-zips]].
**How to apply:** after the first hosted session, read `automodpack/` server config + log, confirm the friend got the pack, and stop making manual ZIPs if it works.
- Also installed on 4 Oct (both boot-tested on X-expeditions): `youkaiportlangfix-1.0.0.jar` and `tlmkeyboardmusic_themes.jar` (Maid Keyboard Music – Touhou Themes 1.0.0, 221 MB, 184 tracks, needs tlmkeyboardmusic). The friend needs both too, via AutoModpack or by hand.

- **7 Oct 2026 config pass (live `automodpack/automodpack-server.json`, backup `proposed-configs/automodpack-server.json.BEFORE-SYNC-2026-10-07`):** syncedFiles now covers /mods/*.jar, /resourcepacks/**, /datapacks/**, /defaultconfigs/**, /config/** (+ kubejs/emotes defaults) with '!' exclusions for backups, logs, *.dat caches, QuantifiedAPI, spark, crash_assistant, chunky, fast_travel_waypoints, mca/presets, *.rpo. allowEditsInFiles stays /config/** and /options.txt; options.txt is deliberately NOT synced, so friend's settings are never touched. Verified with the jar's own FileTreeScanner (tools/automodpack-scan): 1,476 files, 1.8 GB (537 mods, 19 resourcepacks, 28 datapacks, 891 config, 1 defaultconfig), no leaks. Installed jar is 4.0.6 = v4 JSON config; the moddedmc.wiki "latest" docs are v5 (server.conf, groups), which is NOT interoperable with v4 (v4 clients get kicked by v5 servers). Editable means seed-only: later host config changes never reach the friend. Resource packs are delivered but not enabled (needs options.txt or manual). Unverified: Connector (10 Fabric-only jars) may not see mods AutoModpack loads from its own modpack folder; v4 knob is forceCopyFilesToStandardLocation. unloadedactivity is server-only and auto-excluded.

**8 Oct 2026:** the user says AutoModpack isn't working and told me not to bother with it. Friend updates are still done by hand (see [[friend-update-zips]]). Don't troubleshoot it unless the user asks.
