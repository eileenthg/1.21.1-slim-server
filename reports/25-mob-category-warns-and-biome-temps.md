# Mob-category warns and the missing biome temperatures (20 Sep)

## 1. What the 352 warns actually are

Every line of the form

> Detected `quark:stoneling` that was registered with CREATURE mob category but was added under MONSTER mob category for `<biome>` biome!

is NeoForge comparing two things: the `MobCategory` the entity **type** was registered with, and the category list inside the biome that the mob was **added to**. When they disagree the mob spawns on one pass but is counted against the other pass's mob cap, so its own cap never fills and the other one fills with the wrong mob.

Nothing in this pack's configs or datapacks causes any of them. Broken down by source:

| Warn | Count | Registered → listed | Source | Fixable from here? |
|---|---|---|---|---|
| `quark:toretoise` | 170 | CREATURE → MONSTER | Quark `ToretoiseModule` — the class hardcodes `CREATURE` on the entity builder and `MONSTER` on `registerSpawn`. Applied through Zeta's `zeta:biome_modifier`. | Not losslessly — see §3 |
| `quark:stoneling` | 170 | CREATURE → MONSTER | Quark `StonelingsModule`, same bug | Not losslessly — see §3 |
| `alexsmobs:skreecher` | 4 | CREATURE → MONSTER | Alex's Mobs `AMMobSpawnBiomeModifier`; category is in code, `config/alexsmobs/*.json` only carries biome conditions | Not losslessly |
| `quark:foxhound` | 3 | CREATURE → MONSTER | Quark, same bug (nether biomes) | Not losslessly |
| `minecraft:bee` | 3 | CREATURE → MONSTER | Eternal Tales biome JSONs list a bee under `"monster"` in `hunting_grounds`, `karvat_grotto`, `karvat_mushroom_fields` | **Yes — fixed** |
| `yungscavebiomes:sand_snapper` | 1 | MONSTER → AMBIENT | Yung's Cave Biomes lists the snapper under `"ambient"` in `lost_caves` | **Yes — fixed** |
| `alexsmobs:stradpole` | 1 | WATER_AMBIENT → WATER_CREATURE | Alex's Mobs, in code | Not losslessly |

The split is: the four fixed entries come from **biome JSON files**, which a datapack can rewrite. The rest are added by **NeoForge biome modifiers at runtime**, and NeoForge's `remove_spawns` runs in the REMOVE phase *after* `add_spawns` runs in ADD — so anything removed cannot be re-added in the same pass. Those can be deleted, not moved.

## 2. Datapack: `spawn-category-fix`

`datapacks/spawn-category-fix.zip` — four Lithostitched worldgen modifiers:

- `et_bee_remove` / `ycb_sand_snapper_remove` (`priority: 0`) — `lithostitched:remove_biome_spawns` strips the miscategorised entry.
- `et_bee_add` / `ycb_sand_snapper_add` (`priority: 1`) — `lithostitched:add_biome_spawns` puts it back with the same weight and group size.

Two reasons this works where NeoForge's own modifiers don't:

- Lithostitched modifiers run against the biome registry at load, before NeoForge's biome modifiers, and `ModifierManager.sortByPriority` sorts ascending, so remove-then-add is deterministic.
- `AddBiomeSpawnsModifier` files each spawner under `EntityType.getCategory()` — the entity's *registered* category — so the re-added entry is correct by construction.

Net effect: the bee moves from the monster list to the creature list in three Eternal Tales biomes (weight 1, 1–1), and the sand snapper moves from ambient to monster in `yungscavebiomes:lost_caves` (weight 50, 1–2). The snapper will spawn on the monster pass now instead of sharing the 15-mob ambient cap with bats; if lost caves feel crowded, drop its weight in `ycb_sand_snapper_add.json`.

**Tested** on `test-server/A-full` (`console-spawncat.out`, 20 Sep): server picked the pack up on its own in the existing world — *"Found new data pack file/spawn-category-fix.zip, loading it automatically"* — reached `Done (20.433s)`, no Lithostitched errors, and the warn count went 352 → 348: all three `minecraft:bee` lines and the `sand_snapper` line are gone, the 348 Quark/Alex's Mobs ones remain as expected. The re-add half is inferred rather than seen in-world — `ModifierManager` sorts with `Comparator.comparingInt(priority)` and no `.reversed()`, so priority 0 runs before priority 1 — worth confirming once in-game by looking up Lost Caves in Advanced Worldgen Info and checking the snapper sits under monster.

Two repairs to the harness were needed first: `tan-curios-compat`, `tan-compat-pack` and `tough-as-nails-vanilla-pack` were still in `test-server/A-full/mods` after TAN came out of the pack on 19 Sep, and `tancurios` hard-fails load without `toughasnails`. They're in `mods-old/` now. The harness is otherwise **86 mods behind the live instance** (many client-only, so not all of them belong there) — fine for a worldgen/spawn test, not for anything gameplay-wide.

## 3. The Quark and Alex's Mobs half — options, none free

Left alone, stonelings and toretoises spawn through the monster pass (below y=0, per `Max Y Level = 0` in `quark-common.toml`) and are counted against the **animal** cap. Because `CREATURE` is a persistent category they never despawn, so in loaded chunks they accumulate and squeeze passive animal spawns — the thing MineColonies herders care about.

Three ways out, in increasing disruption:

1. **Leave it.** Log noise plus some animal-cap pressure deep underground. This is what's currently in place.
2. **Cut the weights.** `quark-common.toml`: `[mobs.stonelings.spawn_config] "Spawn Weight" = 80` and `[mobs.toretoise.spawn_config] = 120` are both high. Dropping them to ~10 keeps the mobs and most of the cap back. Warns stay.
3. **Move them to CREATURE properly.** Set each mob's `spawn_config.biomes.biomes` to `Biomes = []` with `"Is Blacklist" = false` (whitelist nothing → Quark adds no spawns anywhere), then re-add them with a `neoforge:add_spawns` modifier, which files them under CREATURE automatically. Quark's own spawn placement (y ≤ 0, stone below) is registered in the same `registerSpawn` call and survives, so placement rules are kept. Warns go away and caps are correct — but CREATURE mobs only spawn on the friendly pass (once per 400 ticks), so both become much rarer.

Same shape for `alexsmobs:skreecher` and `alexsmobs:stradpole`: the only lever is emptying the biome list in `config/alexsmobs/…` and re-adding via datapack.

## 4. Project Atmosphere: 29 biomes had no temperature range

`config/projectatmosphere/biome_temps.json` covered 90 biomes; the server was warning `❌ No temperature range defined for biome …` for 29 more — 25 CliffTree biomes plus `minecraft:pale_garden`, `minecraft:sulfur_caves` (both backported, so PA has no built-in default for them), `yungscavebiomes:frosted_caves` and `yungscavebiomes:lost_caves`.

All 90 existing entries follow one rule, so the new ones were generated from it rather than hand-picked:

```
spring.min = clamp(15 × biome_temperature − 12, −20, +20)
spring.max = spring.min + 15
summer     = spring + 10      autumn = spring      winter = spring − 10
```

Each biome's `temperature` was read out of its own biome JSON in the mod jar. The 90 existing entries were left byte-identical. Backup of the old file: `proposed-configs/biome_temps.json.BEFORE`; the new one is installed at `config/projectatmosphere/biome_temps.json` and mirrored to `proposed-configs/biome_temps.json`. The test server confirms it parses: *"Applied 119 biome temperature override(s)"*.

**Worth a second look:** CliffTree tags `coniferous_badlands`, `shrubland`, `warm_caves` and `warm_river` with `temperature: 2.0` — the vanilla "hot" marker used for desert/badlands snow and freezing rules, not a climate statement. The formula turns that into 18–33 °C in spring and 28–43 °C in summer, which is desert-hot for a shrubland. If PA's forecast reads wrong there, override those four by hand; everything else lands where its analogue already sits (`clifftree:cold_river` matches `climaterivers:cold_river` exactly, the cave biomes match the Spectrum cave entries, and so on).
