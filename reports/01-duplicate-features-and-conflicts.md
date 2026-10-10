# Duplicate features and conflicts

Pack: PrismLauncher `1.21.1` instance, NeoForge, 322 jars scanned (plus one `.disabled`).
Method: read every jar's mod metadata, lang files, `c:`/`forge:` item tags, vanilla data overrides and worldgen files; every Quark module is currently **enabled** in `config/quark-common.toml`; cross-checked uncertain pairs on the web (sources at the bottom).

Legend: **DROP** = remove the mod / disable the module. **PICK ONE** = keep one of the pair. **WATCH** = overlaps but usually coexists; decide on taste.

---

## 0. Hard problems (fix first)

| # | Issue | Evidence | Action |
|---|---|---|---|
| 0.1 | **Two copies of Farmer's Delight** are loaded: `FarmersDelight-1.21.1-1.3.1.jar` and `FarmersDelight-1.21.1-1.3.4-patched.jar` | Both were discovered in the last `latest.log`. NeoForge picks one, but the other sits in the folder as a time bomb and confuses every FD addon's version check. | DROP `FarmersDelight-1.21.1-1.3.1.jar`. |
| 0.2 | **SUPERSEDED — see `04-followup-answers.md` §1.** Only the *mansion* splinter duplicates the full pack (275/275 pieces). The Ancient City (130 pieces) and Pillager Outpost (28 pieces) splinters share **zero** pieces with it and are genuinely new content — keep them. Original (wrong) claim follows.<br>~~All four Dungeons and Taverns "splinter" jars duplicate content already inside `dungeons-and-taverns-v4.4.4`~~ | The full pack and the splinters ship the same files: the Woodland Mansion replacement overrides the same 13 mansion loot tables, 14 template pools, 4 processor lists and `structure_set/woodland_mansions.json`; the Swamp Hut splinter overrides the same `witch_hut_base` features and template pool. The Ancient City and Pillager Outpost splinters describe themselves as "standalone split version of ... from Dungeons and Taverns". | DROP the 4 `DnT-*.jar` splinters, keep the full pack. If you specifically want the 100% mansion replacement, keep only the mansion splinter and accept that it overrides the full pack's 50% version. |
| 0.3 | **Swamp hut is claimed by three mods**: DnT swamp hut overhaul (overrides `minecraft:swamp_hut` and the `swamp_huts` structure set), YUNG's Better Witch Huts (replaces the same structure), and the last log shows Alex's Caves also mixing into `SwampHutPiece`. | Structure override collision. Last loaded wins; the others silently do nothing or generate mismatched pieces. | PICK ONE: YUNG's Better Witch Huts **or** the DnT version. Since DnT full already includes its hut rework, dropping the splinter (0.2) and YUNG's Better Witch Huts gives one consistent result. |
| 0.4 | **Pillager outposts are overhauled by three mods**: DnT Pillager Outpost splinter (overrides `minecraft:pillager_outpost`), ChoiceTheorem's Overhauled Village (adds its own outposts), It Takes a Pillage (encampments/fortresses). | Same structure slot. | DROP the DnT outpost splinter (covered by 0.2). CTOV + It Takes a Pillage coexist. |
| 0.5 | **Quark Oddities: Matrix Enchanting vs Apotheosis (Apothic Enchanting)** | Both replace the enchanting table. Quark's own issue tracker says they "will never play nice". Quark caps enchant levels, Apotheosis raises them to 150. | DROP module `oddities.Matrix Enchanting`. Also disable `tools.Ancient Tomes` (Quark's over-levelling clashes with Apothic Enchanting's own over-levelling and Apotheosis affixes). |
| 0.6 | **Quark Oddities: Totem of Holding vs Gravestone (Max Henkel) + Gravestone Carryables** | Both intercept player death and grab the inventory. Documented: totem still spawns, grabs a few items, rest spills. | DROP module `oddities.Totem Of Holding`. Keep Gravestone (Carryables depends on it). |
| 0.7 | **Two fluidlogging engines**: `fluidlogged` (Leximon) and `nnp_fluidlogging` | Both mixin the same block/fluid state code to let any block hold any fluid. | PICK ONE. Keep NNP Fluidlogging (the 1.21.1-native one); drop `fluidlogged-neoforge-1.21.1-2.0.1-beta.8.jar`. |
| 0.8 | **Two Spice of Life diet systems**: Spice of Life Onion and SpiceOfLife Latiao | Both rewrite hunger/saturation/eating behaviour of every food. Onion rewards variety with buffs; Latiao punishes repetition by shrinking restoration. Running both double-applies food logic. | PICK ONE. `solmaid` / `solmaiddream` are maid-side SoL and `minecolonies_solcarrot` runs standalone, so neither forces the choice. |
| 0.9 | **Illusioner enabled by two mods**: Friends&Foes (`enableIllusioner`, `replaceVanillaIllusioner=true`, spawns + raids + two structures) and Illager Invasion (its own survival Illusioner + Illusionary Dust) | Two different Illusioner implementations in raids at once. | In `config/friendsandfoes.json` set `enableIllusioner`, `enableIllusionerSpawn`, `enableIllusionerInRaids`, `replaceVanillaIllusioner` to false, or disable it in Illager Invasion. Keep F&F's Iceologer (Eternal Tales also ships one; see 5). |
| 0.10 | **Distraction Free Recipes is fine** (it supports REI as well as EMI/JEI), listed here only so you don't remove it. | | keep |
| 0.11 | The last log still had `alexscaves-1.0.8` in the mods folder and printed "Alex's Caves world generation is incompatible with eternal_tales". The jar is gone now but `config/alexscaves*` remains. | Leftover config, plus `config/eclipticseasons*` from a removed seasons mod. | Cosmetic; safe to delete the orphan configs later. |

---

## 1. Quark, module by module (all currently ON)

Quark's value here is the tweaks. Most of its *content* modules are duplicated by Create, Supplementaries, Sophisticated Storage/Backpacks, or Youkai's Homecoming. Recommended toggles for `config/quark-common.toml`:

### Oddities (`[oddities]`)
| Module | Duplicates | Verdict |
|---|---|---|
| Backpack | Sophisticated Backpacks (plus Curios). Quark backpack is a chest-slot armour item with its own inventory. | **DROP** |
| Crate | Sophisticated Storage barrels/chests, Supplementaries sacks | **DROP** |
| Magnets | Nothing exact (Create has no magnet), but it is a redstone-moving-blocks system on top of Create + Multi-Piston + Supplementaries pulleys | WATCH, lean drop |
| Matrix Enchanting | Apotheosis | **DROP** (0.5) |
| Pipes | Create belts/chutes/mechanical pipes; Sophisticated Storage upgrades | **DROP** |
| Tiny Potato | nothing | keep |
| Totem of Holding | Gravestone | **DROP** (0.6) |

### Automation
| Module | Duplicates | Verdict |
|---|---|---|
| Chute | `create:chute` exists with the same name and job | **DROP** |
| Pistons Move Tile Entities | Create contraptions already move BEs; known source of dupes with Create/Sophisticated storage | **DROP** |
| Feeding Trough | nothing | keep |
| Gravisand, Iron Rod, Redstone Randomizer, Ender Watcher, Obsidian Plate, Metal Buttons, Dispensers Place Blocks, Chains Connect Blocks | mostly unique (Supplementaries has no equivalents) | keep |

### Building
| Module | Duplicates | Verdict |
|---|---|---|
| Vertical Slabs | Youkai's Homecoming registers vertical slabs for every vanilla stone/wood (`youkaishomecoming:oak_vertical_slab` etc.). 40+ identical blocks | **PICK ONE** (Quark's are wider-reaching: they also cover modded woods via Zeta; if you keep Quark's, ask AU to unify) |
| Framed Glass | `create:framed_glass`, `create:framed_glass_pane` | **DROP** |
| New Stone Types (world) + More Stone Variants | Create ships limestone + pillars for andesite/diorite/granite/calcite/dripstone/tuff/limestone; Spectrum ships calcite bricks/polished calcite; Eternal Tales ships limestone/jasper/permafrost/calcite/tuff/dripstone stairs/slabs/walls | Keep Quark's stone *variants of vanilla stones*; **DROP `world.New Stone Types`** (limestone/jasper/shale/myalite) since Create limestone + Eternal Tales limestone already exist |
| Gold Bars | `supplementaries:gold_bars` | **DROP** |
| Rope | `supplementaries:rope` (also FD rope, Create rope pulley) | **DROP** (Supplementaries has a config to replace `#supplementaries:ropes` in loot) |
| Thatch | Trade Post thatch/stairs/slab | WATCH |
| Variant Chests | Sophisticated Storage chests (wood variants) | **DROP** |
| Variant Bookshelves | Cluttered bamboo bookshelf, Youkai's Homecoming | WATCH |
| Glass Item Frame | Supplementaries? no. Unique | keep |
| Industrial Palette | Create industrial iron blocks look-alike (Create adds `industrial_iron_*`) | WATCH |
| Compressed Blocks (crates of apples, carrots, potatoes, beetroot, golden apple, golden carrot, cocoa beans, chorus fruit) | Farmer's Delight crates, Extra Delight crates, Ender's/End's Delight chorus crates | **DROP** (or unify, see file 02) |
| Hedges, Leaf Carpet, Shingles, Stools, Wooden Posts, Hollow Logs, Japanese Palette, Duskbound, Midori, Soul Sandstone, Sturdy Stone, More Brick/Mud Blocks, Rainbow/Celebratory Lamps, Variant Furnaces/Ladders, Vertical Planks, Nether Brick Fence Gate, Raw Metal Bricks, Shear Vines, More Potted Plants | mostly unique (Supplementaries `stone_lamp` duplicates Quark's `stone_lamp` inside Industrial? Quark's is in **More Stone Variants**; minor) | keep |

### Management
Inventory Sorting, Easy Transferring, Hotbar Changer, Quick Armor Swapping, Automatic Tool Restock, Item Sharing, Expanded Item Interactions: no duplicates in the pack (Sophisticated Backpacks sorts only its own inventory). keep.

### Tools
| Module | Duplicates | Verdict |
|---|---|---|
| Ancient Tomes | Apothic Enchanting | **DROP** |
| Pathfinder Maps | Xaero's World Map + Supplementaries adventurer quills (Supplementaries already swaps maps for quills when Quark is present) | WATCH |
| Bottled Cloud | Artifacts "Cloud in a Bottle" (same display name, different function) | keep, rename via lang if confusing |
| Seed Pouch | Supplementaries sack, Sophisticated Backpacks | WATCH |
| Abacus, Ambient Discs, Beacon Redirection, Color Runes, Endermosh disc, Parrot Eggs, Pickarang, Skull Pikes, Slime in a Bucket, Torch Arrow, Trowel | unique | keep |

### Tweaks
All unique in this pack **except**:
- **Dragon Scales** overlaps with Dragonkind Evolved's dragon-respawn reward loop. WATCH.
- **More Villagers / Villagers Follow Emeralds / Zombie Villagers on Normal** touch villager AI alongside MCA, Liberty's Villagers, Smarter Farmers, Chef's Delight, Create Better Villagers. MCA villagers are a separate entity so Quark's tweaks only affect MineColonies-free vanilla villagers, of which there will be few. Low value; safe to disable.
- **Improved Tooltips** (food/shulker/map) overlaps MenagerieSaturation (AppleSkin-alike food tooltips) and Jade. WATCH.
- **Automatic Recipe Unlock** is fine with REI/Polymorph.

### World
| Module | Duplicates | Verdict |
|---|---|---|
| Spiral Spires, Chorus Vegetation | Enderscape (total End overhaul with its own biomes) | **DROP** both |
| New Stone Types | see Building | **DROP** |
| Glimmering Weald (underground biome) | YUNG's Cave Biomes, Deeper and Darker, YUNG's Better Caves | WATCH (biome weight competition underground) |
| Big Stone Clusters, Corundum, Fairy Rings, Fallen Logs, Blossom/Azalea/Ancient trees, Monster Box, Nether Obsidian Spikes, Permafrost, No More Lava Pockets | unique | keep. Eternal Tales also has a `permafrost` block (different) |

### Mobs
| Module | Duplicates | Verdict |
|---|---|---|
| Crabs | Friends&Foes crab (`enableCrab`), Youkai's Homecoming crab, Crabber's Delight crab: **four crab mobs** | **DROP** Quark Crabs; also decide between F&F crab and Crabber's crab (Crabber's one feeds its recipes, keep it; disable F&F `enableCrabSpawn`) |
| Foxhound, Shiba, Stonelings, Toretoise, Wraith, Forgotten | unique | keep (Forgotten's hat is the only Quark helmet) |

### Client
All unique. Chest Searching and Camera fine. keep.

### Experimental: all already off. Good.

Quark Oddities jar itself (`QuarkOddities-1.21.1.jar`): after dropping Backpack, Crate, Pipes, Matrix Enchanting and Totem, only Magnets and Tiny Potato remain. **Simplest: remove `QuarkOddities-1.21.1.jar` entirely** and keep base Quark for tweaks. `QuarkPonders` is Ponder pages for Quark; keep only if you keep the content modules.

---

## 2. Food / Farmer's Delight family (biggest bloat)

There are 25+ FD addons. Real duplicates:

| Pair / group | Overlap | Verdict |
|---|---|---|
| **Alex's Delight** vs **Alex's Mobs Continued Delight (amcdelight)** | Both are the Alex's Mobs ↔ FD bridge. Both add cooked catfish slice, kangaroo stew, cuts for every AM meat. | **PICK ONE**. amcdelight is written for the Continued fork you run (52 items); Alex's Delight (23 items) is the older upstream. Drop `alexsdelight-1.6.jar`. |
| ~~**Extra Delight** vs **Expanded Delight**~~ **CORRECTED — see `04-followup-answers.md` §3: not a subset, keep both.** Only 28 of 100/1439 names overlap; Expanded Delight uniquely adds asparagus, cranberries, sweet potato, goat dairy, casks and salt ore. Original claim: | cinnamon tree (all 15 blocks duplicated), salt, peanuts, lemon, sweet berry/glow berry juice, juicer, cheese slice, grilled cheese, cheese sandwich, mac and cheese, chocolate/sugar cookies, sweet roll, chili pepper. | **DROP Expanded Delight** (Extra Delight covers everything and adds workstations). If you prefer the smaller one, drop Extra Delight instead and know you lose the oven/keg/jars. |
| **Corn**: Corn Delight, Cultural Delights, Extra Delight, Haunted Harvest, MineColonies | five corn crops, four popcorns, three cornbreads, two tortillas | Keep Extra Delight corn (already there) + MineColonies corn (needed by colony farms). **DROP Corn Delight**. Cultural Delights stays for eggplant/cucumber/curry unless you also drop it (its corn, cucumber, ginger, tortilla, popcorn, rice ball, squid are all duplicated elsewhere). |
| **Cucumber**: Cultural Delights, Extra Delight, Youkai's Homecoming | three cucumber crops | unify (file 02) |
| **Ginger**: Cultural Delights, Extra Delight, Snowy Spirit | three ginger crops | unify |
| **Ender's Delight** vs **End's Delight** | Two End-themed FD addons by different authors: both add chorus crates, shulker/dragon meat dishes, End knives | **PICK ONE** (End's Delight has 55 items, Ender's 29; End's Delight ships knives that go in `c:tools/knife`). |
| **Squid/tentacles**: Cultural Delights, Miner's Delight, Ocean's Delight, Butchery, Crabber's | glow squid item x2, tentacles x3, stuffed squid x2 | unify |
| **Salt**: Butchery, Extra Delight, Expanded Delight, (Eternal Tales), Compat Delight | four salts | unify; drop with Expanded Delight |
| **Cheese/butter/soy**: Extra Delight, MineColonies, Youkai's Homecoming, Expanded Delight | butter x3, cheese x3, tofu x2, soybean x3, soy milk x2, soy sauce x2 | unify |
| **Cooking for Blockheads** cutting board + oven vs FD cutting board + Extra Delight oven | separate systems with same names | WATCH, keep (CfB is a kitchen UI) |
| **Cutting Delight** | improves FD cutting board, no dupe | keep |
| ~~**Compat Delight**~~ **CORRECTED: keep.** Broad coverage is the point of the mod, and Mutant Monsters (added later) activates more of it. Original note: 90% of its items target mods you did not have at the time (rats, naturalist, mutant monsters, ars nouveau, aether, mowzies). It still registers eggplant, garlic, peas, salt, cheese slice, coconut milk that duplicate MineColonies/Crabber's. | **DROP** unless you rely on a specific bridge. |
| **Butchery** (327 items, 455 blocks) | Adds 60+ raw meats with `c:raw_beef/pork/mutton` tags, mob heads for every mob (duplicated by Spectrum's mob heads and Supplementaries' enderman/spider heads), its own salt, jar, basin (same name as `create:basin`), cleavers in `c:sword`. | WATCH: heavy. Keep only if butchering is a core loop. |
| **Large Meals**, **Festive Delight**, **Pineapple Delight**, **Frights**, **Miner's**, **Crabber's**, **Ocean's**, **Silent's**, **Aquaculture Delight**, **My Nether's Delight**, **Brewin' and Chewin'**, **Chefs Delight**, **Display Delight**, **Artifacts' Delight** | distinct themes; only single-item name clashes (gingerbread, potato soup, chicken curry, rice pudding, hot wings, stuffed pepper, spicy curry) | keep; unify the clashes |

---

## 3. Storage, transport, misc utility

| Group | Overlap | Verdict |
|---|---|---|
| Sophisticated Backpacks + Sophisticated Storage + Quark Backpack/Crate + Spectrum Storage | see Quark | drop Quark's |
| Sophisticated Emerald Upgrade | adds a "stack upgrade tier 5" for backpacks while Sophisticated Storage already has its own tier 5 | keep, it is intentional |
| **Waystones UI+** vs **Fast Travel Waypoints** | Both replace/extend the Waystones destination screen (UI+ rewrites the browser with search/favourites; FTW adds a "choose via world map" button to the *vanilla Waystones* screen). Neither documents compat with the other. | WATCH; test. If the FTW button vanishes under UI+, pick one. |
| **Via Romana** + **Via Equina** vs Waystones + Immersive Gateways | Three fast-travel systems (waystones, road-sign travel, gateways). Not a technical conflict; a design one. | your call |
| **RoadWeaver** + **Safepaths** + Via Romana | RoadWeaver generates roads; Safepaths makes walked paths; Via Romana consumes roads. Complementary. | keep |
| ~~Crash Assistant vs Not Enough Crashes~~ | **CORRECTED: keep both.** They do different jobs — Crash Assistant surfaces logs after a hard crash, Not Enough Crashes lets you return to the title screen instead of being ejected. | keep both |
| **Longer Chat History** vs **Even Better Chat** | Even Better Chat is a full chat suite (timestamps, dedupe, search); Longer Chat History only raises the buffer. Check EBC's settings for a history size; if it has one, drop LCH. | WATCH |
| **Observable** + **TPS Detective** + **MemGuard** | diagnostics only | keep |
| **Immersive Optimization** + **ServerCore** | Both throttle entity ticking (IO schedules entity ticks; ServerCore has dynamic mob-cap/view-distance and its own entity tick tweaks). Usually coexist, but disable ServerCore's `entity` tick features if you see mobs freezing. | WATCH |
| **Jade + Jade Addons + JadeColonies + Jade Modded Entities + MC Jade Crops** | complementary | keep |
| **MenagerieSaturation** (AppleSkin-alike) vs Quark Improved Tooltips | food tooltips twice | keep one |

---

## 4. World, weather, seasons, structures

| Group | Overlap | Verdict |
|---|---|---|
| Serene Seasons + Colorful Seasons + Serene Seasons Plus + Seasonal Integration + Serene_Seasons_GenFix + Real Magic x Seasons + Project Atmosphere | all addons of one system; Project Atmosphere adds temperature/weather sim on top | keep (leftover `eclipticseasons*` configs can be deleted) |
| Snow! Real Magic + Immersive Snow Reloaded + Snowy Spirit | ISR explicitly supports SRM; Snowy Spirit is content | keep |
| Simple Clouds, Stellar View, Auroras, Rainbows, Enhanced Celestials 2 (+3 moon addons + Safe Moons), Pretty Rain, Desert Storms, Effectual | Stellar View has fixed EC2 compat; Safe Moons *replaces* Harvest/Blue moons from the Default Lunar Events addon, so those two overlap by design | keep; note Peaceful Moon + Safe Moons both aim at "no hostile moons" |
| **YUNG's Better End Island** + **Dragonkind Evolved** + **End Remastered** (+ProMax, Additions, Eye Crushing x2) + **Enderscape** | BEI rebuilds the main island and dragon fight; Dragonkind Evolved also modifies the dragon fight (respawn rewards, phases). End Remastered only changes portal eyes. Enderscape overhauls outer End. BEI advertises compat with BetterEnd/Nullscape but not with Dragonkind Evolved. | **Verified working** by the pack author (2026-09-04): Dragonkind Evolved phases run correctly under YUNG's Better End Island. Keep both. |
| YUNG's Better Caves + YUNG's Cave Biomes + Deeper and Darker + Quark Glimmering Weald | underground biome/carver competition | keep, expect fewer of each |
| Continents + TerraBlender + Eternal Tales (adds many dimensions and Overworld biomes) | Eternal Tales is a 2,500-item RPG mod that injects overworld biomes/structures. It caused Alex's Caves to warn about incompatible worldgen. | Biggest single source of bloat: it duplicates Mythic Metals (silver, tin, adamantite, copper armour), Spectrum (topaz, amethyst/bloodstone/malachite bricks, polished basalt), Quark stone variants, Waystones (`eternal_tales:waystone`), Butchery (salt, sulfur, blood), Snowy Spirit (candy cane), Youkai (tea, deer, straw hat). Decide whether the RPG is the point of the pack; if not, **DROP**. |
| Structures: YUNG's x11, DnT, CTOV, It Takes a Pillage, Moog's Nether, Underground Village, Immersive Pillagers, Beekeeper Hut, Compat Structures, Bygone Pillage loot integration (`lootintegrations_bygonepillage` targets a mod you don't have) | see 0.2–0.4 | drop DnT splinters; `lootintegrations_bygonepillage` is dead weight |
| Better Sparse Structures + Lithostitched + WorldgenFeatureFix | complementary | keep |

---

## 5. Mobs and NPCs

| Group | Overlap | Verdict |
|---|---|---|
| Illusioner: F&F vs Illager Invasion | 0.9 | fix config |
| Crabs x4 | Quark, F&F, Youkai, Crabber's | drop Quark's, disable F&F spawn |
| Iceologer: F&F vs Eternal Tales | two Iceologers | keep F&F's; ET's goes if ET goes |
| Raccoon: Alex's Mobs vs Eternal Tales; Deer: Youkai vs Eternal Tales; Catfish: Aquaculture vs Alex's Mobs vs Eternal Tales | duplicate animals | unify drops (file 02) or drop ET |
| Villagers: MCA, Townstead(+Factions), Liberty's Villagers, Smarter Farmers, Chef's Delight, Create Better Villagers, Quark villager tweaks, Haunted Harvest (trick-or-treat) | MCA replaces vanilla villagers with its own entity; Liberty's Villagers and Smarter Farmers both patch *vanilla* farmer AI (LV: crop range/prefer same crop; SF: smarter harvesting). They overlap on farmers and will mostly affect nobody once MCA is in. | Keep one of Liberty's/Smarter Farmers (Smarter Farmers is lighter). Consider whether Quark's villager tweaks are worth it. |
| MineColonies fixes: `minecolonies_raid_fix`, `minecolonies_pillager_patrol_fix ... - Kopie.jar`, Civillis (+compat), `minecoloniesmulticolony` | "- Kopie" means "copy": it is a renamed duplicate of a 1.0.0 patch. Raid fix keeps raids out of the border; patrol fix and Civillis both suppress hostile spawns/patrols near civilisation. | Rename/keep one copy; Civillis + raid fix coexist. Check the Kopie jar is not a second copy of the same modId you have elsewhere (it is the only `minecolonies_pillager_patrol_fix` jar, so fine). |
| SmallColonies + SmallColonies ME | ME is the "Mod Extended" style pack of the same author; base SmallColonies is the plain version. Both are style packs, not code. | keep both only if you want both styles |
| Touhou Little Maid + 14 maid addons | complementary; `touhou_lost_maid`, `tlm-self-talk` fine | keep |
| Alex's Mobs + Cosy Critters + Friends&Foes + Netherdepths + Deeper and Darker + Enderscape mobs | distinct | keep |

---

## 6. Sound and music

Alex's Mobs Music, Boss Music, Illager Raid Music, The Immersive Music Mod, Matmos, Audio Improvements, Immersive Melodies. TIMM is biome/structure music and has a documented incompatibility with *Biome Music*-style mods; Boss Music and Raid Music are event music and coexist. Supplementaries flute vs Immersive Melodies flute and TLM trumpet vs IM trumpet are same-name, different-function items. keep all; rename via lang if the names confuse.

---

## 7. "Example mod" placeholders

These jars still carry template names ("Example Mod", "Example mod description") and register `example_block`/`example_item`: `bettersparsestructures`, `chesspvp`, `mca_hat_layer_fix`, `menageriesaturation`, `minecolonies_pillager_patrol_fix`, `minecolonies_raid_fix`, `minecoloniesmulticolony`, `realmagic_x_seasons`, `touhou_lost_maid`, `viaequina`, `baguettelib`, `fishingcrates`. They work, but 12 mods each add a junk "Example Block" to the creative menu. `chesspvp` ("Chess PVP Addon") could not be identified on CurseForge/Modrinth; if you don't know what it does, drop it.

---

## 8. Suggested removal list (summary)

Remove jars:
- `FarmersDelight-1.21.1-1.3.1.jar`
- `DnT-ancient-city-overhaul-v2`, `DnT-pillager-outpost-overhaul-v2.2`, `DnT-swamp-hut-overhaul-v2`, `DnT-woodland-mansion-replacement-v1.2`
- `YungsBetterWitchHuts` (or keep it and drop the DnT hut; not both)
- `QuarkOddities-1.21.1.jar` (or disable the five modules above)
- `fluidlogged-neoforge-...beta.8.jar`
- one of `Spice of Life Onion` / `spiceoflifelatiao`
- `alexsdelight-1.6.jar`
- `expandeddelight-0.1.4.jar`
- `corn_delight-1.2.10`
- one of `endersdelight` / `ends_delight`
- `compatdelight-1.0.1.1.jar`
- `notenoughcrashes` (keep Crash Assistant)
- `lootintegrations_lootintegrations_bygonepillage-1.2.jar` (targets an absent mod)
- one of `libertyvillagers` / `smarterfarmers`
- Consider: Eternal Tales, Butchery, Cultural Delights, chesspvp

Quark config toggles: Oddities Backpack/Crate/Pipes/Matrix Enchanting/Totem of Holding, automation Chute + Pistons Move Tile Entities, building Framed Glass/Gold Bars/Rope/Variant Chests/Compressed Blocks, tools Ancient Tomes, world New Stone Types/Spiral Spires/Chorus Vegetation, mobs Crabs.

F&F config: `enableIllusioner*`, `replaceVanillaIllusioner`, `enableCrabSpawn` to false.

---

### Sources
- Quark/Apotheosis: [Quark issue #2198](https://github.com/VazkiiMods/Quark/issues/2198), [Quark issue #2844](https://github.com/VazkiiMods/Quark/issues/2844), [Apotheosis issue #269](https://github.com/Shadows-of-Fire/Apotheosis/issues/269)
- Totem of Holding vs Gravestone: [Quark issue #4692](https://github.com/VazkiiMods/Quark/issues/4692), [Gravestone issue #294](https://github.com/NightKosh/Gravestone-mod-Graves/issues/294)
- Dungeons and Taverns splinters: [Pillager Outpost Overhaul](https://modrinth.com/datapack/dungeons-and-taverns-pillager-outpost-overhaul), [Ancient City Overhaul](https://modrinth.com/project/DNuNq5bb), [D&T GitHub](https://github.com/TheLegendofSaram/Dungeons-and-Taverns)
- Distraction Free Recipes REI support: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/distraction-free-recipes)
- Fast Travel Waypoints: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fast-travel-waypoints)
- Stellar View + Enhanced Celestials: [changelog](https://moddedmc.wiki/en/project/stellarview/docs/changelog)
- Immersive Snow Reloaded + Snow Real Magic: [Modrinth](https://modrinth.com/mod/immersive-snow-reloaded)
- Liberty's Villagers: [Modrinth](https://modrinth.com/mod/libertyvillagers)
- Spice of Life Onion / Latiao: [Onion](https://modrinth.com/mod/spice-of-life-onion), [Latiao](https://modrinth.com/mod/spiceoflifelatiao)
- SoL MineColonies Carrot: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/minecolonies-sol-carrot)
- Friends&Foes Illusioner: [wiki](https://github.com/Faboslav/friends-and-foes/wiki/Illusioner)
- Alex's Delight / AM Delight: [Alex's Delight](https://www.curseforge.com/minecraft/mc-mods/alexs-delight)
- Ender's / End's Delight: [End's Delight](https://modrinth.com/mod/ends-delight), [Ender's Delight](https://www.curseforge.com/minecraft/mc-mods/enders-delight)
- Extra / Expanded Delight: [Extra Delight](https://www.curseforge.com/minecraft/mc-mods/extradelight), [Expanded Delight](https://modrinth.com/project/e9V6wFcR)
- SmallColonies ME: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/smallcolonies-me)
- Via Romana: [Modrinth](https://modrinth.com/mod/via-romana)
- YUNG's Better End Island compat: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/yungs-better-end-island)
- TIMM: [Modrinth](https://modrinth.com/mod/immersivemusicmod)
