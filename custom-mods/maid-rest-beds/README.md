# Maid Rest Beds

Touhou Little Maid maids only sleep in maid beds. With this mod, when no maid bed is free, they sleep in a
free ordinary bed or a Comforts hammock instead.

- **Occupied:** the bed or hammock (both halves) is set to `occupied=true` while the maid sleeps and freed when she wakes. That is vanilla `LivingEntity.startSleeping`/`stopSleeping` behaviour, since Comforts hammocks are `BedBlock`s.
- **Less rest than a maid bed:** a maid bed heals fully and gives the full "Sleep" favourability. In this pack that is 18, because Vanilla Companion raises TLM's 2. A bed or hammock heals 50% of max health and gives 50% of the favourability (9 here). It shares the maid bed's once-a-day cooldown.
- **Order:** the nearest usable maid bed comes first. This respects Love & Loathe's multi-maid bed capacity. Then the nearest free bed or hammock. Beds that a villager has claimed as home are skipped.
- **Waking:** a player who clicks a bed or hammock that a maid is sleeping in wakes her and then uses the bed as normal, the same way villagers are woken. Maid beds are never affected.
- **Hammock POI:** hammocks get a `maidrestbeds:hammock` POI so maids can find them. Hammocks placed before install get their POI added when their chunk next loads (a cheap palette check).

Server-side only. POI types aren't a synced registry, the config is `common`, and there are no packets.
Clients without the jar can join.

Config: `config/maidrestbeds-common.toml`, covering allow beds/hammocks, favourability and heal shares,
skip claimed beds, and wake on click.

## How it hooks in
- `MaidBedTaskMixin` wraps the *call* to `MaidBedTask.findBed` inside `checkExtraStartConditions`, not
  `findBed` itself. Love & Loathe cancels `findBed` at HEAD, so this keeps both mods working. A HEAD inject
  on `start` lies the maid down in a bed or hammock. TLM's own body still clears the memories.
- `EntityMaidMixin` wraps `setHealth` and `FavorabilityManager.apply` inside `EntityMaid.startSleeping`.

## Test
`/maidrestbeds selftest` (op). It builds beds at y=250 above spawn, runs TLM's real `MaidBedTask` on real
maids, then clears everything up. 28/28 passed on test-server/X-expeditions on 1 Oct 2026.
Maid Spell cancels direct `setHealth` drops on maids, so the test damages them with `hurt()`.

Build: `./gradlew build` → `build/libs/maidrestbeds-1.21.1-1.0.0.jar`.
