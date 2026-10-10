# Log review, FPS, overlays, fog — and the Xaero port

Source: `logs/latest.log` (2.5 MB, 16:18–16:43 on 2026-09-06). Clean shutdown, no crash.
GPU reported: **NVIDIA GeForce GTX 1650 Ti**, driver 581.29, OpenGL 4.6.

---

## 1. Log errors — 716 total, and 461 of them are one mod

Grouped by shape, then by the namespace that fails to resolve:

| Failing namespace | Refs / dead recipes | Installed? |
|---|---:|---|
| `unordinary_basics` | 2844 / 342 | **no** |
| `newworld` | 498 / 92 | **no** |
| `geodes` | 76 / 16 | **no** |
| `cobblemon` | 32 / 9 | **no** |
| `xercamusic` | — / 1 | **no** |

Every one of these is emitted by **`recipes-plus-1.3.5.jar`** (Recipes Plus, modId `mr_recipes_plus`). I opened it: it contains **zero class files** — it is a datapack shipped as a mod, so there is no config toggle to turn compat sets off. Its recipe folders:

```
data/recipes_plus/recipe/minecraft/           605   (the point of the mod - keep)
data/recipes_plus/recipe/create/               88   (Create installed - keep)
data/recipes_plus/recipe/waystones/             1   (installed - keep)
data/recipes_plus/recipe/unordinary_basics/   342   dead
data/recipes_plus/recipe/newworld/             92   dead
data/recipes_plus/recipe/geodes/               16   dead
data/recipes_plus/recipe/cobblemon/             9   dead
data/recipes_plus/recipe/xercamusic/            1   dead
```

**Fix applied:** `datapacks/recipesplus-dead-compat-off.zip` — 460 override files, each a `neoforge:false` stub, on the Simple Datapacks path. NeoForge's conditional codec evaluates `neoforge:conditions` *before* decoding the recipe body, so the stub short-circuits and the dead recipe never parses. Same technique as the Cultural Delights calamari disables.

Be aware this is **cosmetic plus a small load-time saving**, not an FPS fix — recipe parsing happens once at resource reload. It removes ~64% of your ERROR lines and shortens reload.

### The rest of the errors, and what they're worth

| Error | Count | Verdict |
|---|---:|---|
| `Couldn't load tag … missing references` (spectrumcompat mob heads, idas BYG/BOP biome tags, `c:tea` from tanca, touhou_little_maid_spell) | 11 | Harmless. Compat tags for absent mods; the tag just ends up smaller. |
| `Object with ID … specified in data map … doesn't exist` (alexscaves, cataclysm, landsoficaria, twilightforest, undergarden, born_in_chaos) | 18 | Harmless. A ranged-weapon data map listing items from mods you don't run. |
| `Couldn't load model goblincraft:models/custom/tntlobber*.json` (×4), `butchery:models/custom/ravager_carcass.json` | 5 | **Real, but upstream.** Those mods reference models they don't ship. You get the missing-model texture. Not fixable from config. |
| `Invalid path in pack: snowyspirit:sounds/block/GBC_Block_Walk_1_B (1).ogg` etc. | ~10 | Upstream packaging bug — filenames with spaces and uppercase. Those sounds silently don't play. |
| `Invalid path in pack: youkaishomecoming:textures/item && cp d殷宇泽妖怪们的归家-移植…` | 1 | Your Youkai's Homecoming jar contains a stray file whose name is a fragment of a shell command from whoever built it. Cosmetic. |
| `Reference map '<x>.refmap.json' could not be read` | ~200 WARN | **Ignore.** Normal for mods built without a refmap; the log line says so itself. |
| `Unable to copy depth between the main and clouds frame buffers` | 2 | Project Atmosphere's volumetric clouds. See §4. |

Nothing in the log came from the four datapacks we built (verified: 0 hits).

---

## 2. The top-left FPS overlay — `sodiumextras`, and you have two look-alike mods

You have **two different mods with near-identical names**:

| Jar | Mod | FPS HUD class | Its config | Corner |
|---|---|---|---|---|
| `sodium-extra-neoforge-0.9.3+mc1.21.1.jar` | **Sodium Extra** | `SodiumExtraHud` | `sodium-extra-options.json` → `"overlay_corner": "BOTTOM_RIGHT"` | bottom-right — **the one you set** |
| `sodiumextras-neoforge-1.0.8-1.21.1.jar` | **Sodium Extras** (an Embeddium Extras port — its config keys are still `embeddiumextras.*`) | `DebugOverlayEvent` | `sodiumextras-client.toml` → `fpsDisplay = "ADVANCED"`, `fpsDisplayGravity = "LEFT"` | **top-left — the redundant one** |

**Applied:** `fpsDisplay = "ADVANCED"` → `"OFF"` in `config/sodiumextras-client.toml`. Backup at `proposed-configs/sodiumextras-client.toml.BEFORE`.

`fpsDisplayGravity` only offers LEFT / CENTER / RIGHT — that is why you couldn't move it off the top; there is no vertical option. Turning it off is the only real answer.

Two other suspects checked and **cleared**: Xaero's minimap info display has no FPS field at all (its options are coords / overworld coords / chunk coords / angles / dimension / biome / weather / light level / time / real time / highlights / indicators), and `tps_detective` has `isOn = false`. TabTPS was installed after this log was written.

### Worth knowing: the two mods overlap heavily

Both provide fog toggles, cloud height, and culling switches. `sodiumextras` is the only one giving you **block-entity and entity distance culling** (currently on, 64-block radius, with `waterframes:*`, ghast, ender dragon, `iceandfire:*`, `create:*` whitelisted), so I would keep it rather than remove it — just with its HUD off.

---

## 3. The ground-fog mod — **Pretty Rain**

Found it in the old pack: `config-kitchen-sink-old/prettyrain-client.toml`.

```toml
doGroundFogParticles = true
doFogParticles       = false

[groundFog]
  density     = 20
  spawnHeight = 64      # <- sea level
  size        = 8.0
```

`spawnHeight = 64` is why you remember it as a layer hugging **beaches and water**: the ground-fog particles spawn at Y=64, so over ocean and shoreline they sit right on the surface, while inland terrain pokes up through them. It is a particle system, not a shader.

**Status now:** the jar is **not installed**, but `config/prettyrain-client.toml` is still sitting in your current config folder as a leftover — so your settings are already waiting if you reinstall it. Note it also sets `renderVanillaWeather = false` and `tickVanillaWeather = false`, which will fight Project Atmosphere's weather ownership. If you add it back, expect to arbitrate between them.

Two other fog mods were in the old pack and are also absent now: `fog` (biome fog *colours*, Polytone-aware, auto-disables under Iris) and `betterfog` (per-biome cave / cloud / void / water / lava fog curves). Neither does a ground layer.

---

## 4. Making FPS higher

Ranked by expected effect on a 1650 Ti / i7-10750H. Current settings read from `options.txt` and `sodium-options.json`.

### The cap — this is the headline

```
maxFps:60          -> APPLIED: 120
enableVsync:false
```

You were hard-capped at 60 FPS. **Applied:** `options.txt` `maxFps:60` → `maxFps:120`. Backup at `proposed-configs/options.txt.BEFORE`.

### Then, in order of payoff

| Setting | Now | Suggest | Why |
|---|---|---|---|
| `projectatmosphere` `cloudMode` | ~~`FULL`~~ → **`HYBRID` applied** | — | FULL ran PA's volumetric raymarch **plus** `enableVolumetricGroundShadows` as a depth post-process. On a 4 GB 1650 Ti that was the most expensive single thing in your pack, and the source of the two framebuffer-depth errors in the log. (I set this to VANILLA earlier; it had gone back to FULL — either you changed it or PA rewrote the file.) Backup: `proposed-configs/projectatmosphere-common.toml.BEFORE2`. If HYBRID still isn't enough, the next lever is `enableVolumetricGroundShadows = false`, which is a separate toggle and stays on under HYBRID. |
| `graphicsMode` | ~~`1`~~ → **`0` applied** | — | Cuts fancy transparency on leaves and water. |
| Sodium `leaves_quality` | ~~`DEFAULT`~~ → **`FAST` applied** | — | Forces opaque leaves regardless of graphics mode. Large win with Eternal Tales + Youkai's + Quark foliage. |
| `particles` | ~~`0` (All)~~ → **`1` (Decreased) applied** | — | With Lodestone, Malum, Spectrum, Create and now `particlerain` all spawning particles, this is real CPU work. |
| `biomeBlendRadius` | ~~`2` (5×5)~~ → **`1` applied** | — | Directly shortens chunk rebuild time — helps stutter more than average FPS. |
| `projectatmosphere` `cloudRenderDistance` | ~~`2000`~~ → **`500` applied** | — | 2000 blocks of cloud against a 96-block render distance. |
| `renderDistance` / `simulationDistance` | `6` / `6` | leave | Already low. Don't go lower; you'd break MineColonies ticking. |
| `entityDistanceScaling` | `0.5` | leave | Already halved. Good. |

### Mods you don't have that would help

- **Entity Culling** — true occlusion culling. Sodium's `use_entity_culling` and `sodiumextras`' distance culling are both *distance*-based; Entity Culling skips entities hidden behind solid geometry. In a MineColonies colony that is a large, visible gain.
- **Clumps** — merges XP orbs into one entity.
- **Noisium** — faster worldgen; targets the chunk-load hitching you get exploring with this many structure mods.

Add them one at a time and check dependencies first, per the `compatartifacts` lesson.

### Not an FPS problem

The 461 recipe errors, the refmap warnings, and the missing-tag errors all happen once during resource reload. Cleaning them up (§1) shortens loading; it will not change your frame rate.

---

## 5. TPS on an overlay — TabTPS

You installed `tabtps-neoforge-mc1.21.1-1.3.25.jar`. It had not launched yet as of the 16:43 log, so `config/TabTPS/` does not exist; it generates on first boot.

From the jar bytecode, the command literals are:

```
/tabtps toggle actionbar
/tabtps toggle bossbar
/tabtps toggle tab
```

plus `/tabtps tickinfo`, `/tabtps memory`, `/tabtps ping`, `/tabtps reload`, `/tabtps about`.

`actionbar` is the one you want for a persistent overlay — it sits above the hotbar, clear of both the minimap (top-left) and your Sodium Extra FPS counter (bottom-right). What each display shows is configurable in `config/TabTPS/main.conf` once it generates.

In singleplayer these are permission-gated commands, so the world needs **cheats enabled**. If your current world doesn't have them, open to LAN with cheats on for the session, or test in a fresh world.

---

## 6. Does fog increase performance load?

**It depends entirely on which kind, and the two kinds pull in opposite directions.**

**Vanilla distance fog makes you faster.** Sodium has `use_fog_occlusion: true` in your config — it culls chunk sections beyond the fog plane, because they would be invisible anyway. Fog is doing free work for you. This is why `sodiumextras`' comment on its own `fog` toggle — *"toggling off may increase performance"* — is **wrong for your setup**: switching fog off pushes the fog distance out, which defeats fog occlusion. At render distance 6 you would lose more to the extra chunks than you would gain. Leave `fog = true`.

**Post-process fog costs you a little.** Project Atmosphere's fog is a screen-space pass over the whole frame — a roughly constant per-frame GPU cost, small on its own. Its `cloudMode = "FULL"` volumetrics are the expensive part, not the fog.

**Particle fog costs the most.** Pretty Rain's ground fog is thousands of particle entities (`maxParticleAmount = 1500`), each with CPU tick and draw-call overhead. That is the one to be careful with if you reinstall it.

Short version: keep vanilla/Sodium fog on; be wary of volumetric and particle fog.

---

## 7. Xaero — ported

**The important discovery: your old Xaero settings were never actually in effect.**

Xaero's Minimap 26.4.2 and World Map 1.45.0 moved to a new config layout under `config/xaero/`. The flat files — `xaerominimap.txt`, `xaeroworldmap.txt`, `xaerominimap_entities.json`, `xaeromaps_waystones.json5`, and the two `-common.txt` files — are **byte-identical to the kitchen-sink copies and still stamped Jan 18 2026**, while the files under `config/xaero/` were rewritten on your last launch. The new version does not read the old ones.

The good news: almost everything had already been migrated by Xaero's own converter. I compared all ~120 keys. The entity radar categories are identical apart from key ordering. Only six things were actually off.

### Applied

| File | Setting | Change |
|---|---|---|
| `xaero/minimap/profiles/default.cfg` | `display_minimap` | `false` → **`true`** |
| `xaerohud.txt` | minimap module position | `x=0;y=0` → **`x=-7;y=9`** |
| `xaero/minimap/client.cfg` | `differentiate_by_server_address` | `true` → **`false`** |
| `xaero/world-map/client.cfg` | `differentiate_by_server_address` | `true` → **`false`** |
| `xaero/world-map/client.cfg` | `only_display_current_map_waypoints` | `true` → **`false`** |
| `xaero/lib/common.cfg` | `everyone_tracks_everyone` | `false` → **`true`** |

`display_minimap = false` is why the top-left FPS text was exposed — your minimap was not drawing at all. That matches your description exactly.

Backups: `proposed-configs/xaero-*.BEFORE` and `xaerohud.txt.BEFORE`.

### Deliberately not applied

Four values differ but look like runtime state or version drift rather than choices, and I would rather not re-apply a stale default the way I nearly did with Trade Post:

| Setting | Old | Current | Why left alone |
|---|---|---|---|
| `minimap_zoom` | `0` | `1` | Live zoom level; changes as you scroll. |
| `minimap_cave_mode_extra_zoom` | `1` | `2` | Same. |
| `minimap_manual_cave_mode_start` | `-1` | `2147483647` | Both mean "unset"; the sentinel changed between versions. |
| `minimap_ui_scale` | `0` (auto) | `1` | Plausibly a real preference — flip it to `0` if the minimap looks too small. |

Also equivalent, not a difference: old `caveModeToggleTimer:1000` (ms) vs new `cave_mode_toggle_timer = 1.0` (seconds).

### Cleanup you can do

`config/xaerominimap.txt`, `xaerominimap-common.txt`, `xaeroworldmap.txt`, `xaeroworldmap-common.txt`, `xaerominimap_entities.json`, `xaeropatreon.txt` and `xaeromaps_waystones.json5` are dead files the current versions ignore. Safe to delete; I left them as reference.

---

## Still open

- **Pagefile: resolved.** Now `AllocatedBaseSize = 8192` MB (was 4966), peak usage 819 MB. Commit limit is no longer the binding constraint on world load.
- **Colorful Seasons is currently `.disabled`** — so the green pale-oak-leaf tint should be gone, along with seasonal colour on every other modded leaf. If you want it back with a per-block exclusion, the patch from report 06 is still on the table.
- Optional carries still not applied from the old pack: Touhou maid ranges (32/32/32), `Butchery Mobs.Salmon`/`Cod = false`, `supplementaries blue_bomb.explosion_radius 5.15`, `minecolonies loadtime 10`, Project Atmosphere wind scales, `simple_datapacks copyDatapacks = true`.

---

## 8. Crash triage, 2026-09-06 evening — TabTPS was not the cause

57 crash reports in `crash-reports/`. Fingerprinting today's evening bursts gives **three distinct causes, none of them TabTPS**:

| Time | Count | Cause | TabTPS installed? |
|---|---:|---|---|
| 19:27:36–19:27:44 | 5 | `tps_detective` — `IllegalStateException: Cannot get config value before config is loaded` at `io.github.catomon.tpsdetective.ClientEvents.onClientTick:26` | yes |
| 19:51:20–19:54:41 | 15 | `illagerraidmusic` — `NullPointerException: Trying to access unbound value: ResourceKey[minecraft:sound_event / illagerraidmusic:raid_music]` at `IllagerRaidMusic$ClientModEvents.onClientTick:58` | yes |
| 20:00:56 (server) | 1 | `artifacts` `NaturalSpawnerMixin` failed to apply — `InvalidInjectionException`, target `NaturalSpawner::spawnCategoryForPosition` was merged by `dev.sixik.tpsum.mixin.entity_spawn.MixinNaturalSpawner$redirect_to_optimized_methods` | **no** |

TabTPS appears in the 19:27 and 19:51 reports only because crash reports embed the full mod list — never in a stack frame. And the 20:00 world crash happened **after** TabTPS was removed, so removing it did not stop the world crashing.

That is not proof TabTPS is harmless — it was loaded during the first two bursts, and adding any mod to a 394-mod pack with Sinytra Connector shifts registry and load order, which is exactly the kind of thing that exposes latent bugs like the unbound sound holder. But nothing here convicts it.

### What each one actually is

**`illagerraidmusic` — still installed, and fragile by construction.** Decompiling `IllagerRaidMusic$ClientModEvents.onClientTick` shows it calls `MusicManager.isPlayingMusic(RAID_MUSIC)` on **every client tick whenever a level is loaded**, before the `activeRaidIds.isEmpty()` branch — with no guard on whether the sound holder actually bound. Registration itself looks conventional (`DeferredRegister.create(Registries.SOUND_EVENT, modid)` → `.register("raid_music", …)` → `SOUND_EVENTS.register(bus)` in the constructor), and the asset and `sounds.json` are both present in the jar. So binding *usually* works — the 20:06 session ran three minutes in a world without crashing. But when it doesn't bind, this throws every tick rather than failing once, which is why you get fifteen crash reports in three minutes.

**`tps_detective` — still installed, `isOn = false`.** Its client tick handler reads a config value before the config spec has loaded. Startup race, fires at the very beginning of client ticking.

**`tpsum-1.21.1-0.0.3.jar` — already removed.** Don't reinstall it alongside Artifacts: both mixin `NaturalSpawner::spawnCategoryForPosition`, and TPSum's redirect destroys the injection point Artifacts needs. This is unfixable from config; it's one or the other.

### Suggested

- Remove `illagerraidmusic-NeoForge-1.21.1-1.3.0.jar` — it's the only live one, and there is no config to gate it.
- Consider removing `tps_detective-1.2.0.jar` — `better_tab` already gives you TPS in the tab list (`"footer_info_format": "§7TPS: {tps} §7PING: {ping}§7ms"`, colour-graded at 19/18/16/10).
- If you want TabTPS back, test it with those two gone, on its own, so the result means something.
