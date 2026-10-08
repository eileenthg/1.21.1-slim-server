---
name: modpack-datapack-install-location
description: "Datapacks in this pack must go in the instance root datapacks/ folder, not <world>/datapacks/; plus the level.dat Disabled trap and broken /reload"
metadata:
  type: project
---

This modpack ships **`simple_datapacks`**, whose `config/simple_datapacks.json`
sets `requiredDatapacksPaths: ["datapacks"]` and mixes into `PackRepository`,
`ServerPacksSource`, `FolderRepositorySource` and
`MinecraftServer#getDataPackConfig`.

**Datapacks must go in the instance/server ROOT `datapacks/` folder**, beside the
pack's other 19 zips — not `<world>/datapacks/`. A pack in the world folder is
discovered by vanilla ("Found new data pack …, loading it automatically") but the
mod then substitutes its own selection, so it never reaches the resource manager.
Proven 2026-09-25 with a two-file probe: even a single recipe in the
`minecolonies` namespace was ignored from `<world>/datapacks/` and picked up
immediately from the root folder.

Two traps found the hard way:
- Once a pack filename has been seen and then goes missing, the server writes it
  to `level.dat` → `DataPacks.Disabled` and **never auto-enables that name
  again**. Re-adding the same filename silently does nothing. Use a new name, or
  don't delete the zip between test boots. Cost several wasted boot cycles.
- **`/reload` is broken in this pack, independent of any datapack**: it dies with
  `Failed to parse thing: 'Unknown registry key … croptopia:soy_milk'` →
  `NoSuchElementException` → "Failed to execute reload". Always restart the
  server instead of relying on `/reload`.

**Why:** three separate mechanisms each look like "my datapack is broken" when it
is actually not being loaded at all.

**How to apply:** install to root `datapacks/`; on the test harness that means
`extra-datapacks/` + `sync-mods.ps1` (see [[test-server-a-full-stale-mods]]).
Used by [[colony-autoteach-datapack]].
