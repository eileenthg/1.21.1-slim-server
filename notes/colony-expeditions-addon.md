---
name: colony-expeditions-addon
description: "Colony Expeditions add-on mod (expedition huts for End, Otherside, Deeper Down, Bumblezone, 7 Eternal Tales dims): design, user rules, test harness, pack bugs found"
metadata: 
  node_type: memory
  type: project
  originSessionId: 06324820-43f9-4c60-861f-6d50932c201b
  modified: 2026-09-20T00:48:31.221Z
---

The user's NeoForge add-on mod lives in `modpack-audit/colony-expeditions` (mod id `colony_expeditions`). It adds 11 expedition huts that reuse MineColonies' Netherworker: the End, Deeper Darker's Otherside, Spectrum's Deeper Down, the Bumblezone, and the Eternal Tales dimensions Comets, Gardens of Eden, Kingdom of Amber, Lands of Karvat, Purgatorium, Rayana and Volcanech. Skipped: Ultimate World (it copies every other dimension), Unahzaal Multiverse (bosses only) and Lunarian Dream.

User rules:
- Bosses never appear as encounters.
- Balance values go in `config/colony_expeditions-common.toml`.
- Each hut is unlocked by a research whose item costs come from its own dimension.
- Portals open with that dimension's own key, and only if the key can be obtained repeatedly:
  - End: eye of ender
  - Otherside: heart of the deep
  - Deeper Down: bottle of Ruin
  - Bumblezone: ender pearl
  - Eternal Tales: that dimension's crystal (loses durability per trip)
- Otherwise Dimensional Tears. The stock Netherworker is patched to use tears, and flint-and-steel lighting is off in the configs.
- Blueprints: one generic set (based on "Minecolonies Original") that shows up under every style pack, in a virtual `expeditions` folder served by the Structurize mixins.

**Why:** the user plays the pack live while this is built, so all testing happens on a server, never in the Prism instance.

**How to apply:**
- Data comes from `tools/generate.py` (reads `destinations.py` and `destinations_more.py`). It checks every id against the installed jars.
- Build with Gradle 8.14.3 from ~/.gradle dists, JAVA_HOME jdk-21.0.7.
- Test server: `test-server/X-expeditions` (port 25571, Xmx 6G, mods copied from the instance minus client-only jars and roadweaver, which stalls spawn generation).
- Server helpers: `tools/testserver.sh` (stop/install/wait/cmd) and `tools/trip-test.sh`.
- In-game harness: `/colony_expeditions test colony|wake|hut|hire|kit|recall|status|watch`.
- Harness gotchas:
  - GlobalGamerules resets doDaylightCycle and doWeatherCycle on every boot. Set them again, otherwise workers idle at night or in rain.
  - The test colony has no town hall, so travellers only come back via `test recall`.
  - Colonies only tick with a member player online, so run `test wake` after every restart.
- Never copy jars into the Prism instance or launch it without the user's go-ahead. Peer sessions also use it.

Bugs found 19 Sep:
- MCA-Colony-Integration 0.1.0-beta (author Jaden) deletes any citizen whose entity is discarded (`onCitizenLeave` checks `!isAlive()`). That deleted every Netherworker on each trip. Colony Expeditions ships a @Pseudo guard mixin (`McaColonyCompatGuardMixin`) so the cleanup only runs for KILLED citizens. Plain chunk unloads were verified safe.
- New MineColonies jobs need an entry in `ModSoundEvents.CITIZEN_SOUND_EVENTS`. A missing entry crashed a level-up mid loot hand-off and duplicated the loot. Fixed by reusing the Netherworker voice set in common setup.
- 20 Sep: all 11 destinations and the tears Netherworker passed full server trips. The release jar is `build/libs/colony_expeditions-0.1.0.jar` (debug mixin removed).
- The jar is NOT installed in the Prism instance yet; the user's visual check is pending.
- Lethal expeditions are intended: the user says much of Eternal Tales is endgame. Do not soften the encounter tables.
- Workers must carry a minimum gear tier before setting off, defaulting to the lowest tier with ~50% simulated survival
  (`/colony_expeditions test simulate <trials>` measures it against the real loot tables). Defaults per destination live in
  `Destination.java`; the config file overrides them; and each hut's Settings tab has a "Minimum gear before leaving" dropdown
  (`BuildingExpedition.MIN_GEAR`) that overrides both. Under-equipped workers wait and request that tier.
- `generate.py` now fails on any bad id. Loot and research items must have an item model; a block without an item (spectrum:doombloom) hung server start.
