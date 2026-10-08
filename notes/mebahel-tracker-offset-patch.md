---
name: mebahel-tracker-offset-patch
description: Mebahel quest tracker HUD offset lives in mcarpgbridge 1.1.0 client TOML + in-game config screen (25 Sep); in-game unverified
metadata:
  node_type: memory
  type: project
  originSessionId: 6154f7a5-93dc-4ca0-ab1f-18130dace30a
  modified: 2026-09-25T03:02:08.690Z
---

Mebahel 2.0.6's QuestTrackerHud is hardcoded at (8,8), under Xaero's minimap. On 25 Sep 2026 the user asked for an in-game-editable TOML offset.

It is done in [[mca-rpg-bridge-mod]] 1.1.0:
- `MebahelTrackerOffsetMixin` pushes a pose translate before `drawSolidBackdrop` and pops it at TAIL. Both injections are require=0 and the pop is flag-guarded.
- The offset is in `config/mcarpgbridge-client.toml`, under `[questTracker] offsetX/offsetY`. It was seeded with offsetY=90, which is a guess.
- A NeoForge ConfigurationScreen is registered, so it can be edited in game from Mods → MCA RPG Bridge → Config.

The same day, an earlier jar-patch version (a JSON file) was removed. The stock Mebahel jar is back in mods. `fixed-mods/mebahel-tracker-offset/` holds only that abandoned patch and the original jar. The old bridge 1.0.0 jar was backed up to `mca-rpg-bridge/build/installed-backup-*`.

**Why:** the in-game config screen needs a NeoForge mod. Mebahel is a Fabric mod loaded through Connector, so the bridge hosts the config.

**2026-09-28 (Mebahel 3.2.0):** `drawSolidBackdrop` is gone, so the mixin was retargeted to HEAD/RETURN of `render`. 3.2.0 also adds its own `questTrackerPosition` and `questTrackerScale`, which is the better way to dodge the minimap; zero the bridge offsets if using it.

**How to apply:** it was installed while the game was running, so nothing has been tested in game yet. The first launch should confirm the config screen appears and the tracker moves. If it doesn't move, check the log for a failed MebahelTrackerOffsetMixin injection.
