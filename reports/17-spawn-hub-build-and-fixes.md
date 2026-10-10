# Spawn hub build, break protection, thirst knockback (18 Sep)

All of this lives in your own mod, `spawn-onboarding`, except the knockback tag. Installed jar sha1 `635f456d74f4…`
(same file as `spawn-onboarding/build/libs/`). Nothing in AnotherScape's own jar was modified.

## AnotherScape spawn hub

| Problem | What it does now |
|---|---|
| Built 4,096 blocks per tick: minutes of lag spikes, half-built town | `HubBuild.paste` places the whole plan in one tick, then marks AnotherScape's save data complete |
| Loose items all over the site | Block drops are turned off for the duration and restored afterwards |
| Mobs wander in mid-build; mobs already inside get entombed | The world is frozen while it builds, and everything except players inside the footprint (66 blocks, y −6 to +60) is moved to 80 blocks out first |
| World spawn on top of the dome | `HubSpawn` picks the floor nearest the hub centre that has head room and a roof, and uses it for the world spawn and for newly arriving players |
| Nothing could be broken within 64 blocks of the hub, including player builds and graves | `WorldSpawnBuildProtectionMixin` keeps protection only for blocks still matching the hub plan (73,882 built + 4,346 decoration) |

Config, `config/spawnonboarding-common.toml` under `[anotherscapeHub]`: `pasteAtOnce`, `freezeWhileBuilding`,
`spawnInsideCity`. If the plan cannot be read (an AnotherScape update renaming things), each part logs an error and
falls back to AnotherScape's own behaviour.

**Why it is not a pasted structure file:** AnotherScape has no .nbt for the hub. `WorldSpawnHubSchematic.buildPlan()`
generates 745,199 placements in code, and the incremental loop exists so the build survives a restart. Of those,
73,882 are real blocks; the rest is air that clears the site. Pasting them all at once is the same result in one go.

**Verified** on a copy of `New World (1)` running headless: protection preload read the plan, and the respawn point
moved to `BlockPos{x=1, y=70, z=134}`, inside the hub. The paste, freeze and evacuation only run when a hub is
built, so they are still unproven in play — they apply to the next new world.

## Thirst knockback

`toughasnails:thirst` and `toughasnails:hyperthermia` were not in `minecraft:no_knockback`, so every damage tick
shoved the player. Vanilla starving, drowning and freezing are all in that tag. Added both through
`zz-unified-items` (`data/minecraft/tags/damage_type/no_knockback.json`), so the damage stays and the bounce goes.

## The SpawnOnboarding mixin popup

A game launched while the jar was being replaced, so FML read it mid-copy and could not load the main class
(`NullPointerException … because "cls" is null`). The jar itself is sound: the installed copy contains the main
class and a valid TOML, and two dedicated-server runs loaded it and logged its startup and runtime lines. Both
copies (instance, test server) are now the same file. Lesson: no copying into the instance while anything may be
launching.
