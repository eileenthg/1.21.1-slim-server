---
name: legacy-compostables-restore
description: NeoForge 21.1 ignores legacy COMPOSTABLES/GIFTS/parrot maps; weeds-as-grass + compostables-restore datapacks installed & server-verified 26 Sep
metadata:
  node_type: memory
  type: project
  originSessionId: 1c87e2f2-cae6-4fd3-95c6-968f9fc7f2c3
  modified: 2026-09-26T11:14:09.068Z
---

26 Sep 2026: user asked for Immersive Weathering weeds to be compostable and treated like grass.

Proven with javap: NeoForge 21.1.249's `ComposterBlock.getValue` reads only the `neoforge:compostables` data map. Anything a mod puts in the legacy `COMPOSTABLES` map is dead. That covers direct puts, `ComposterBlock.add`, Moonlight `RegHelper.registerCompostable` and GlitchCore `registerCompostable`. Platform (blackgear) is fine because it patches getValue. Forgified Fabric API is fine because it bridges into data maps. The same is true of `GiveGiftToHero.GIFTS`, `Parrot.MOB_SOUND_MAP` (sound only), `VibrationSystem` frequencies, `MonsterRoomFeature.MOBS`, `HoeItem.TILLABLES` and the furnace `getFuel()`. Strippables, waxing/oxidizing and villager types still fall back to the old maps.

Installed in live `datapacks/` (sources in `proposed-datapacks/`):
- `weeds-as-grass.zip`: the weeds compostable entry (0.3) plus these tags: minecraft:replaceable, replaceable_by_trees, sword_efficient, c:grass, quark:hoe_harvestable, farmersdelight:unaffected_by_rich_soil, sereneseasons:unbreakable_infertile_crops, gravestone:grave_replaceable, and item minecolonies:compostables.
- `compostables-restore.zip`: 102 compostable entries covering Immersive Weathering (bark, leaf piles, moss, etc.), Alex's Mobs, Arcadian Dream, Friends & Foes buttercup, Haunted Harvest, Pineapple Delight, Delightful and Fright's Delight. It also restores the raid_hero_gifts entry for friendsandfoes:beekeeper and the parrot_imitations entry for minecraft:creaking.

Server-verified 26 Sep with a probe: every tag matches, weeds/banana/acorn compost, and the cobblestone control doesn't. There were no data map errors from these packs; the 18 "doesn't exist" data map errors in the log come from other mods. The beekeeper gift and creaking parrot are untested in-game.

Harness quirk: the console CommandSourceStack has a null level in this pack. `execute in` and `schedule` NPE too. To test, use a probe datapack whose `#minecraft:load` function does forceload and then `schedule function`. Boot a throwaway world with `--world <name> --port 25581`. Sleep in the foreground is blocked, so wait with Monitor.

Not fixed (side notes): apoli's FuelItemCondition only sees vanilla fuels; integrated_api's spawner fallback ignores Eternal Tales' monster_room_mobs.

**Why:** mods ported from older Forge silently lose these registrations on NeoForge 1.21.1.
**How to apply:** when an item "should" compost, fuel, etc. but doesn't, look for a missing data map entry first. Don't remove or rename either zip once a world has seen it; see [[modpack-datapack-install-location]].
