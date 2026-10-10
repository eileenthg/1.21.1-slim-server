# 19: Better Combat set, sea spawns, creature links, maids, clouds (18 Sep)

## 1. New Better Combat mods: conflicts

Test client boot, same instance: all load, no errors from any of them.

| Jar | Verdict |
|---|---|
| bettercombat 2.4.0 + playeranimator 2.0.4 (KosmX) | OK. Coexists with zigy's Player Animation Library (different mod id, both hook `PlayerModel`). AnotherScape declares Better Combat as optional and ships 67 `weapon_attributes` files, so it was built for it. |
| better_mob_combat_reimagined | OK. It applies to **every** humanoid mob that uses `MeleeAttackGoal` (MCA villagers, MineColonies guards, maids, AnotherScape NPCs). `entityBlacklist` in its server config if any of them misbehave. |
| barehandbc | OK. `includeNonWeaponItems = true` means food, blocks, torches also swing fist combos. |
| apothiccombat | OK (Apotheosis present). Harmless refmap warning. |
| bc_particle | **Fabric mod**, loads only through Connector. Connector skipped its bundled Fabric playerAnimator, so no duplicate. Harmless. |
| enhancedequipmentdisplay | Works, but it draws inventory weapons on the back, overlapping **Backslots** (back-slot item) and Sophisticated Backpacks. Check in game; tune with its in-game editor or drop one. TACZ mixin warning is harmless (TACZ not installed). |
| alexscavesbettercombat | **Removed** (Alex's Caves isn't in the pack). In `removed-mods/`. |

Not verified: whether Better Combat swing animations show on MCA PLAYER/VILLAGER bodies. KosmX's library hooks the real `setupAnim(LivingEntity…)`, which MCA's model does call, so it probably does (unlike zigy's, which needed `mcaanimcompat`).

## 2. AnotherScape sea creatures

Cause: AnotherScape does not use biome spawns for these. `OsrsNaturalSpawns.supplementAquaticSpawns` tops every player up to **18** sharks/rays/krakens/orcas/narwhals within 96 blocks **every 3 seconds, in any water**, rivers and ponds included, 8–64 blocks away.

Fix (spawn-onboarding `AquaticSpawnMixin` + `compat/SeaSpawns`): replaced with habitat rules. Config `[anotherscapeSea]`:
- cap 4 (was 18), every 300 ticks (was 60), 24–64 blocks away (was 8–64)
- great white: deep ocean, 25+ blocks of water
- tiger / bull / hammerhead sharks: warm and lukewarm oceans (reef sharks), 12+
- rays: warm/lukewarm (reefs), 4–12+
- narwhal: frozen/cold oceans; orca: cold/frozen/deep ocean
- krakens: deep ocean, 20–28+
- `enabled = false` restores AnotherScape's behaviour.

Blaze rods: not that fish specifically. Every non-boss AnotherScape mob rolls a shared supplemental table (`OsrsNpcDrops.NON_BOSS_SUPPLEMENTAL`) that includes 1–3 blaze rods.

## 3. Alex's Mobs and other mods' mobs

Most Alex's Mobs predators pick prey from entity tags. New datapack `datapacks/creature-links.zip` (instance `datapacks/` + New World (1)):
- `minecraft:arthropod` += AnotherScape spiders, scorpions, kalphites, crawlers, bugs; Eternal Tales spiders, beetles, moths, dragonflies, fireflies; Enderscape rubblemite; Deeper Darker sculk centipede; Enhanced Celestials space moss bug. Gives the tarantula hawk's long paralysis and Bane of Arthropods. Bosses left out on purpose (a pet hawk would otherwise paralyse Araxxor for 2 min).
- orca targets += AnotherScape sharks, rays, narwhal. Cachalot whale targets += AnotherScape krakens (sperm whale vs giant squid).
- passive_land_animals (crocodile, anaconda, komodo, tiger, froststalker prey) += AnotherScape cow, chicken, antelopes, kebbits, jerboa, chinchompas; Eternal Tales + Youkai deer.
- snow leopard, bald eagle, warped toad, mimic octopus fear lists extended the same way.
- All entries are `required: false`.

Hard-coded: the tarantula hawk only hunts `Spider.class`. spawn-onboarding `compat/CreatureLinks` adds a second hunting goal for tag `spawnonboarding:tarantula_hawk_prey` (AnotherScape spiders, Eternal Tales sunflower spider), same conditions (adult, not sitting).

AnotherScape and Alex's Mobs orcas/hammerheads stay separate entities; merging entities isn't practical.

"Spider pig" = **Mutant Monsters** `spider_pig`, not AnotherScape. It never spawns naturally; it's what a pig becomes when hit by Mutant X. Most likely a player made it.

## Verification (test client, copy of New World (1))

All loaded; creature-links tags loaded with no errors; `[anotherscapeSea]` written to config; MaidFriendlyFire logged "kept wandering maid 31 from being turned into a lost maid" on a real maid in the world. Not yet seen in play: respawn fix, sea-spawn habitats, hawk hunting. Log: `test-client/verify-19-latest.log`.

## 4. Tree felling

Nothing ungated. Vein Mining lists logs but `maxBlocksBase = 0` (needs its enchantment). AnotherScape woodcutting only gives a bonus log. Create's saw and Apotheosis's treecapitator trade are gated. Nothing changed.

## 5. AnotherScape special attacks

None exist in 1.0.35. There is no key, and no spec energy. The four godswords are identical (+5 damage, −3.2 speed, Better Combat claymore). The only "spec" in the code is the armadyl crossbow's shot sound. Would need a custom add-on.

## 6–7. Simple Clouds lag / snowstorm "see-through"

`simpleclouds-client.toml`: `renderClouds = false`, `generateMesh = false`, `fogMode = VANILLA` (was SCREEN_SPACE). `bettersimpleclouds-client.toml`: in-cloud immersion off. Weather still comes from clouds server-side. Backups: `proposed-configs/*.BEFORE-18sep`. The see-through snowstorm is most likely the screen-space fog or the in-cloud interior shell, both now off. Not reproduced. These are client configs, so Youchrist needs them on their side too.

## 8. "Check your maid limit"

Not a limit. Limits are effectively unlimited (`OwnerMaxMaidNum = 2147483647`; Lost Maids' `can_tame_count` only affects film drops). The message is Love & Loathe's generic accept failure. **Lost Maids converts every ownerless, empty-handed maid that joins the world into a hostile "lost maid"**, including Love & Loathe's wandering fox maids, and its mixin blocks normal taming of lost maids.

Fix: maid-friendly-fire **1.2.0** `WanderingMaidRescue`. After Lost Maids runs, a maid carrying Love & Loathe's wandering tag gets the lost-maid flag cleared, the gear it was given removed, and her idle task and day schedule restored.

## 9. Pickpocket

Info only. Petting is its own keybind (default crouch + right-click); pickpocketing is crouch + right-click with something in hand.

## 10. AnotherScape crossbows

**Superseded:** the real cause was Sword Blocking Mechanics + AnotherScape tagging the crossbows as swords; fixed by removing that mod. See report 20. The guess below was wrong.


`armadyl_bow`, `zaryte_bow`, `dragon_hunter_bow` are vanilla `CrossbowItem`s. Charging works in code, but AnotherScape registers no `pull`/`charged` item properties and its models have no overrides. So a loaded crossbow looks exactly like an empty one, and there's no pull animation. Likely: loading works (hold right-click about 1.5 s until the loading sounds finish; the next click fires) but gives no visual feedback. **Unconfirmed**. Needs a quick in-game check against a vanilla crossbow.

## Also: respawning on trees

spawn-onboarding moved the world spawn and first join into the hub, but not respawns. Vanilla then picks a random column within `spawnRadius` and stands you on the highest block (trees, dome). `HubSpawn.onRespawn` now moves world-spawn respawns inside the hub; beds/anchors elsewhere untouched.
