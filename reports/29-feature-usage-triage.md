# 29 — Feature usage triage (30 Sep 2026)

## Where the numbers come from

- **Stats:** `saves/New World/stats/*.json` for both players, last written 28 Sep. There are about 50 hours of playtime in total: you have 34.5 h and Youchrist has 16.2 h. For each mod ID, I summed picked up, mined, crafted, used/placed, dropped and killed.
- **Mod list:** I read the mod IDs and item/block counts from all 524 jars in the `1.21.1 slim` mods folder.
- **Exploration:** region-file sizes for each dimension.
- **Colony:** MineColonies saved data.
- The scripts and raw output are in the session scratchpad (`usage.py`, `report.py`, `report.txt`).

**Blind spots:**
- Stats only count item and block interactions. Structure mods, client mods, libraries and tweak mods can't be measured this way, so this triage doesn't cover them.
- Items that Almost Unified merges show up under the surviving namespace.
- Right-clicking a block that is already in the world (sitting, opening a waystone) isn't counted.
- Any mod that was installed after 26–28 Sep has had no chance to be used yet.

## Headline facts

- **The Nether and the End have never been visited.** `DIM-1` and `DIM1` are about 5 MB each, which is spawn chunks only. The same is true of Deeper Darker's Otherside, Spectrum's Deeper Down, every Eternal Tales dimension and the Extra Delight cornfield. Only the Bumblezone (16 MB) and the Retreat (7 MB) show any real footprint.
- **No MineColonies colony exists.** `minecolonies_colony_manager.dat` is 232 bytes, which means it is empty.
- The things you actually play are food and cooking (Extra Delight 2 675, Farmer's Delight 966, Rustic, Expanded, Cocktails, Delightful, Crabber's, Miner's), Butchery 870, Eternal Tales 871, Aquaculture 794, Youkai's Homecoming 627, Mythic Metals 477, Water Canteens, PVJ, Immersive Weathering, Supplementaries, Create 164, Spectrum 88 and Sophisticated Storage and Backpacks.

## Tier 1: large content mods with zero use (strongest cut candidates)

These add a lot of registry weight and nobody has touched a single item from them.

| Mod | Items | Blocks | Notes |
|---|---|---|---|
| Colorful Azaleas | 353 | 369 | Decoration and worldgen only |
| Display Delight | 340 | 341 | Food-display blocks |
| Spectral Decorations | 310 | 161 | Spectrum decoration add-on. Spectrum itself is used (88) |
| Vanilla Backport | 224 | 71 | Check first whether pale garden, creaking or other worldgen you like depends on it ([06](06-performance-and-paleoak.md)) |
| Create: Blazing Hot | 171 | 112 | Create itself is only lightly used (164, mostly pickups) |
| Showercore + Hot Bath | 140 + 24 | 138 + 7 | Bathroom decoration |
| Recipes Plus | 136 | – | |
| MC Trade Post (+ tradepostapo) | 141 | 95 | MineColonies add-on, and there is no colony |
| Reinforced Construction | 105 | 105 | |
| Supplementaries Squared | 54 | 53 | Base Supplementaries is used a lot (244) |
| Skill Cloaks | 53 | – | |
| Upcycle, Amelet, Extra Alchemy, Alloy Forgery, Create Power Loader, Large Meals, Corn Delight, Artifact Delight, Silent's Delight, Fright's Delight | small | | Each one is zero or close to zero |
| Immersive Melodies, Convenient Decor | | | 2 interactions each. csextras needs Convenient Decor for the umbrella, so keep that one |

## Tier 2: MineColonies cluster (about 20 jars, no colony founded)

MineColonies, Structurize, Domum Ornamentum, BlockUI, MultiPiston, MineColonies Tweaks, MineColonies Compatibility, MineColonies SoL Carrot, civilis_minecolonies, MCA-Colony-Integration, colonyhandover, colonylifts, minecoloniesmulticolony, smallcoloniesme, the raid and pillager-patrol fixes, createfillthecolonies, jadecolonies, colony_expeditions, MC Trade Post, tradepostapo, and the `colony-autoteach` datapack.

This is your biggest decision. A lot of work has gone into this cluster (expeditions, autoteach, the perf config, the pet-colony plan), but no one has started a colony in about 50 hours. If a colony is still planned, keep it all. If not, it is the single largest cut you could make, both for load time and for the per-tick cost of the citizen AI.

## Tier 3: maid add-on sprawl

Touhou Little Maid itself shows light use (15 interactions: something was placed and used). **These add-ons have zero use:**
- Maid Storage Manager
- TLM Maid Manager
- Maid Spell. The Retreat has a small footprint, so check with Youchrist.
- Love & Loathe

These add-ons can't be measured, but they only matter if a maid is actually working: maid_useful_task, MaidTaskScroll, MaidUseHandCrank, maid-lily, maid_sophi_compat, maid_sword_soaring, maidfriendlyfire, maidsoulkitchen, solmaid, solmaiddream, tlm-self-talk, tlmtrumpet, vanilla_companion, touhou_lost_maid and touhouorigins.

The question to answer: is there a working maid in the world right now? If not, this is roughly 18 jars you can drop.

## Tier 4: progression-gated, not reached yet (keep if you intend to go there)

**Nether (never entered):**
- My Nether's Delight (114 items)
- Nether Depths Upgrade
- Moog's Nether Structures
- YUNG's Better Fortresses

**End (never entered):**
- Enderscape (210 items)
- End Remastered Pro Max
- endrem_additions
- YUNG's Better End Island
- Ender's Delight

End Remastered's eyes *are* being collected (12 interactions), so an End trip does look planned.

**Other dimensions and bosses:**
- Deeper Darker (348 items, Otherside never visited)
- Bosses of Mass Destruction (0)
- Dragonkind Evolved (0)
- Battle Towers (installed 19 Sep, 0)
- Bumblezone: a little use in its dimension (4)

These haven't failed. You just haven't reached them. They only become worth cutting if the world is going to stay overworld-focused.

## Tier 5: installed too recently to judge

Butchery AM Part 2 (27 Sep), csextras 1.2.0, colony_expeditions, Village Waystone Names, Grave Tag Cleaner and Boss Enchant Cleaner. Leave these alone until a few more sessions have been played.

## Also worth a look (can't be measured, only suspected)

- **Waystones (2 uses) plus Fast Travel Waypoints plus Via Romana plus waystone add-ons.** Several travel systems overlap, and none shows much use.
- **Origins + touhouorigins + apoli + caelus.** Check whether either player has actually picked an origin.
- **Chunky.** Pregen is done on the headless P-pregen server now, so the in-client copy may be redundant.
- **chesspvp, PatPat, Two Players One Horse.** Novelty mods.
