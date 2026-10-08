---
name: coldsweat-rainwear-umbrella-plan
description: "csextras mod (1.2.0 warming foods installed 28 Sep) + Convenient Decor installed 27 Sep — rainwear/umbrella/sun-hat temps, Origins rain/sun fix, shiver, sweat, campfire stoke, heartbeat, pitchfork; key Apoli facts"
metadata:
  node_type: memory
  type: project
  originSessionId: cb53aa67-011d-4c63-8066-7841be2dabe2
  modified: 2026-09-27T13:26:21.440Z
---

On 2026-09-27 the user added Convenient Decor 0.4.4 (Fabric, loaded through Connector; boots cleanly) and a custom mod, `csextras` 1.0.0 "Cold Sweat Extras". Source is at `modpack-audit/csextras`. Both jars are in the live `mods/`. The live `config/convenientdecor.json5` has `rainclothesIncreasedHp` false, which removes the raincoat's bonus hearts (a Health Boost effect in 1.21.1), and both Origins flags false.

What csextras does (config `csextras-common.toml`; tags under `data/csextras/tags`):
- Rain protection. Cold Sweat's WaterTempModifier rain soak is blended toward "dry only". Hood-down raincoat 0.4, rain hat alone 0.4, raincoat + (hood up or rain hat) 0.8, open umbrella 0.4. These stack.
- Origins `in_rain` returns false only with raincoat + (hood or hat) in normal rain; the user's rule is "hood up and a rain hat" = the full-cover tier. In thunder, `origins:hurt_by_water` from rain is multiplied by 0.5. An umbrella does NOT count for Origins rain by default (config).
- Shade. An open umbrella (Convenient Decor, or an upright Artifacts umbrella) = full Cold Sweat shade. Sun hats (`csextras:sun_hats`: Artifacts villager/cowboy/angler hats, Eternal Tales and Youkai straw hats, Alex's Mobs sombrero; Curios slots are checked too) = 0.6 shade. An umbrella makes Origins' `exposed_to_sun` false (Phantom, Touhou vampire).
- Shivering: body temp below -40 warms by +0.2 to 0.5/s and costs hunger; it stops at food 6. Sweating: registered modifier `csextras:sweat`, body temp above 30 gives up to -0.2 MC of cooling, scaled by TWT thirst (none at 6 or below, full at 12 or above), plus small thirst exhaustion. TWT already speeds thirst loss from Cold Sweat body temperature on its own.
- Campfire: right-click a lit campfire with a `#minecraft:logs_that_burn` log to give Cold Sweat Warmth (3600 ticks) within 6 blocks.
- Heartbeat (client): starts at body temp 60 and plays Subtle Effects' `entity.player.heartbeat`.
- Pitchfork: ×6 break speed on hay, straw and thatch; breaking a leaf pile, fallen leaves or leaf carpet also sweeps a 5x5 area, y±1 (sneak to skip).
- Plastic shovels: recipes disabled by `neoforge:false` stubs in the mod's data (shapeless shape kept), added to `c:hidden_from_recipe_viewers` for JEI, and removed from creative tabs.

**1.1.0 (27 Sep): sandstorms.** A sandstorm is `isRaining()` in a biome with no precipitation that is in `csextras:has_sandstorms` (desert + #c:is_desert: desert, clifftree desert_cliff/oasis/shrubland) or `has_red_sandstorms` (badlands tags). Exposure uses the MOTION_BLOCKING heightmap, not canSeeSky, because sky light lags after a block change.
- Exposed mobs get -20% speed and -50% follow range as transient modifiers, checked every 10 ticks. Players are included (config). Exempt: `csextras:ignores_sandstorms` (husk, camel, breeze, a few AM desert mobs).
- Breezes are added to MONSTER potential spawns. PositionCheck forces SUCCEED because breezes have no spawn placement and fail the monster light check. It requires sturdy ground, and fails if 3+ breezes are within 48 blocks.
- New blocks: `csextras:sand_pile` / `red_sand_pile` (layered FallingBlock with item; 4→1 sand and back) and `sand_cauldron` / `red_sand_cauldron` (level 1-3; random tick fills at 0.3 chance during a storm). A shovel or empty hand gives 1 sand when full, otherwise one pile per level. Accumulation comes from a HEAD mixin on `ServerLevel.tickPrecipitation` (vanilla's snow tick): 0.5 chance per hit, capped at 2 layers.
- Hot Bath already ships Cold Sweat compat (`hotbath:immersion` modifier sets world temp to the bath temp; it also neutralises WaterTempModifier), and showercore routes through Hot Bath. The user said to leave both alone.

Testing: `/csextras selftest` on the harness went 36/36 in 1.0.0 and 48/48 in 1.1.0. It exercises everything on a FakePlayer in a plains biome. It sets rain and thunder levels directly, because `setWeatherParameters` only ramps them up. FakePlayers are not in the entity list, so the campfire check uses an armor stand. NOT tested on a client: sand pile/cauldron models, heartbeat, creative tab hiding, raincoat hood toggle, textures.

Verified facts:
- Overgrown Apoli 1.56.0 `SimpleFlagConditions.lambda$inRain$8` uses `Level.isRainingAt`, and `lambda$exposedToSun$6` is its own check. So Artifacts' `Entity.isInRain` hook, and the Fabric-Apoli mixins in Artifacts and Convenient Decor, never apply. The csextras mixin targets these lambda names; if Apoli updates, re-check them with javap.
- Particle Rain 4.0 beta already renders sandstorms: dust, heavy_dust and dust_haze in biomes with no precipitation over sand, terracotta and sandstone, plus sandstorm sounds.

**1.2.0 (28 Sep): warming foods — INSTALLED 28 Sep, NOT TESTED (user said skip testing).** Eating an item in `csextras:warming_foods` (only `extradelight:borscht`, the AU target that the MineColonies/Youkai borscht merge into) gives Cold Sweat Warmth I for 3600 ticks via `LivingEntityUseItemEvent.Finish`. A client tooltip line shows it. Config `[warmingFoods]` has enabled/duration/amplifier. Cold Sweat's own "Temperature-Affecting Foods" can't do this: FoodData only has temperature/duration/stack_limit/entity/temp_modifiers, with no mob effect. Selftest adds 3 checks. The jar is at `csextras/build/libs/csextras-1.2.0.jar`. It replaced 1.1.0 in live `mods/` after the game closed; 1.1.0 was moved to `build/libs/csextras-1.1.0.removed-from-mods.jar`. The selftest (expect 51/51) has not been run. The friend needs it for the tooltip only.

**How to apply:** the friend's client needs both jars (the sweat modifier syncs to clients). Frostiful and Scorchful ideas not built yet: snow build-up, freezing wind, mirages. See [[survival-stack-twt-coldsweat]] and [[friend-update-zips]].
