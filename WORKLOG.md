# Work log and resume guide

Everything that was built, patched, configured or removed for the `1.21.1 slim` pack between 31 Aug and 10 Oct 2026, where it stands, and what is left to do. Written 10 Oct 2026 from the notes, each project's own docs, git history, and a check of which built jars are byte-identical to the jars in the live `mods/` folder.

**How to use this file**
- Start with **Open threads** to see what can be resumed.
- For the details of any item, read its note in `notes/` (the deepest record), then its folder in `custom-mods/`, then `reports/` for the original session report.
- If this file and a note disagree, the note wins (notes were updated as work happened; this file is a summary of them).
- "Live" means the jar or datapack is in the owner's live instance as of 8–10 Oct. "Tested" names the strongest test that actually ran.

**Test levels used below**
- **in-game**: the owner confirmed it while playing.
- **client test**: an automated run in the separate client scaffold (`test-client/C-client`).
- **server/harness**: a headless test server (`test-server/X-expeditions`) or GameTests, usually a `/<mod> selftest` command with N checks.
- **built only**: compiled, never run.

---

## Timeline

| Dates | Phase | Session reports |
|---|---|---|
| 31 Aug – 6 Sep | Pack audit: duplicate features, AlmostUnified candidates, old-pack review, performance, old-config migration, fog/FPS overlays, worldgen duplicates. Ports of Delightful and Maidsoul Kitchen; Pale Oak fix | `reports/01`–`09` |
| 7 – 13 Sep | Tamable Arcadian Fairy, Origins port (Touhou Origins onto Overgrown's Origins), Settled Caves, Rain Source Restore. Dragonkind + YUNG End island checked in-game | – |
| 15 – 20 Sep | Config-reset diagnosis, colony sweep, item-merge pipeline, heads/geodes/workstations, kettles and racks, Back Slots, Compat Delight, spawn hub, combat mods, sea spawns, OSRS special attacks. AnotherScape removed; Tough As Nails replaced by Thirst Was Taken + Cold Sweat; MCA Quests/Reputation/Mebahel bridge; Farmer's Contracts crash; REI categories. Mods: Battletowers port, Guide Library, Spawn Onboarding, Seat Rest, Drink Fix, Colony Expeditions, Season Climate | `reports/10`–`26` |
| 25 – 29 Sep | FD cutting-board hint, DFR JEI fix, Gentle Swings, Load My Chunks tick fix, MCA Animation Compat 1.4, Mebahel 3.2 bridge update, Butchery: Alex's Mobs parts 1 and 2, villager TPS tuning, Thin Air / RoadWeaver performance, Cold Sweat Extras, recipe identity and progression audits, headless pregen | `reports/27`, `28`, `23-recipe-identity-audit`, `25-recipe-shortcuts` |
| 30 Sep – 3 Oct | Feature-usage triage from player stats. Maid Chef (all cooking stations), Maid Rest Beds, Maid Task Curator, Player2 Watch. TLM add-ons: Working Maid Computers (AntOS), Maid Keyboard Music, Maid Trumpet Calls, Love & Loathe voices, Chess PVP (Seated). Love & Loathe English text | `reports/29`, `30` |
| 4 – 8 Oct | Gensokyo Legacy integrated beside the Youkai's Homecoming port (bridge 1.0 → 1.1.1, magical forest, chest loot, structure density cut and thinning). AutoModpack tried, then abandoned. Colorful Azaleas removed. Tab Folders. Lost Maid whitelist. Block-state memory measured. Ground Berries, compost and duplicate-recipe datapacks. Friend resynced with a full mods folder | – |

---

## Open threads (resume candidates)

Ordered roughly by value. Each has a note in `notes/` with the details.

1. **In-game verification backlog.** Most custom mods passed server or harness tests but were never checked by the owner while playing. The riskiest are the ones with client rendering or UI: Gensokyo bridge quest tab and the swapped characters; MCA Animation Compat Princess Carry (needs two players); Butchery AM parts 1 and 2 (hook fix and see-through faces, 8 Oct); Tab Folders; Maid Chef on real stations (Spectrum unchecked); Lost Maid whitelist re-pick; Ground Berries. Use the client scaffold, never the live instance.
2. **Butchery AM: bleeding for arthropods.** The owner was asked on 8 Oct what they saw (our cleaver sends blood particles even for bugs, unlike Butchery's own spider). Waiting on their answer. `notes/butchery-am-part2.md`.
3. **DFR JEI fix upstream PR.** The fix is installed and tested as a spliced jar; the fork branch `fix/jei-19.40-overlay` is committed but never built or pushed. Next: `./gradlew chiseledBuild`, re-test, push, open PR (show the owner the PR text first). `custom-mods/dfr-jei-fix/HANDOFF.md`.
4. **Origins port** (Touhou Origins → Overgrown's Origins). Phases 1–3 done; 51 datapack errors in three groups remain, plus rendering and sound tests written but not run (`custom-mods/origins-port/VERIFY.md`, `SOUND-TEST.md`). Licensing blocks any public release. Not installed. `custom-mods/origins-port/CLAUDE.md` and `NOTES.md`.
5. **Colony Handover** (MineColonies colony → MCA village). Design and mappings done (55 huts, 51 jobs); not built. Open questions in `custom-mods/colony-handover/CLAUDE.md`. The owner has no colony yet (feature triage, 30 Sep), so low priority.
6. **Runic Gods port** (Forge 1.20.1 → NeoForge 1.21.1). Paused since 19 Sep, to resume after Battletowers is verified in-game. `notes/runic-gods-port.md`.
7. **Duplicate recipes, judgement list.** Cod stew (Farmer's Delight baked vs Youkai pot) and a few others were left for the owner to decide. `notes/duplicate-recipes-plateable.md`.
8. **Block trimming** (unused Youkai's Homecoming blocks and others). Deferred by the owner. Measured on 7 Oct: it saves tens of MB at most, so it is tidiness and load time, not memory. `notes/blockstate-memory-cost.md`.
9. **Ideas offered, not built**: a cheaper structure-music check for Immersive Music (`notes/timm-structure-lag.md`); cutting board costing no durability for chosen recipes (`notes/recipe-progression-rule.md`); Frostiful/Scorchful-style weather effects for Cold Sweat Extras (`notes/coldsweat-rainwear-umbrella-plan.md`); `useMCAVoices` left on pending the owner's choice (`notes/mca-chatai-player2.md`).
10. **Unconfirmed bug: ghost lava and block desync** on the hosted world (28 Sep). Prime suspect Seamless Chunks beta.7; its optimisations were turned off. `notes/blockstate-desync-seamless-chunks.md`.
11. **Music overlap** (10 Oct, unresolved): a track heard on the Blocks slider overlapping normal music. The closest candidate is Matmos (own jungle/desert/badlands/Nether/End music, on the Ambient slider; toggle with `/matmossettings`). Waiting on the owner's biome and slider test.
12. **Friend's client.** Updates are by hand (AutoModpack abandoned 8 Oct). The baseline is the full `LOAD ALRADY.zip` mods folder plus the 8 Oct top-up. Anything installed since needs to go to the friend. `notes/friend-update-zips.md`.

---

## Custom mods (written for this pack)

All are NeoForge 1.21.1. "Live" = the built jar is byte-identical to the one in the live `mods/`.

### Touhou Little Maid add-ons

| Mod (id, version) | What it does | Live | Tested | Open |
|---|---|---|---|---|
| Maid Chef (`maidchef` 0.1.0) | "Chef" maid task: picks the most valuable dish it can make from an ingredient chest, cooks it at any of the pack's stations, outputs to a chest; Player2 chat | yes | harness 45/45 | in-game check; Spectrum stations unchecked |
| Maid Rest Beds (`maidrestbeds` 1.0.0) | Maids sleep in ordinary beds or Comforts hammocks when no maid bed is free | yes | harness 28/28 | in-game |
| Maid Task Curator (`taskcurator` 1.0.0) | Task menu folders, hiding and an editor (client) | yes | harness 23/23 | – |
| Maid Friendly Fire (`maidfriendlyfire` 1.3.0) | Hostile maids can hurt players and fight hostile maids, but never their owner or the owner's maids | yes | – | – |
| Working Maid Computers (`workingmaidcomputers` 0.1.0) | Sit at the maid computer to open the AntOS desktop | yes | GameTests exist (results not recorded) | 9 uncommitted files in its repo |
| Maid Keyboard Music (`tlmkeyboardmusic` 1.0.0) | Maids or players at the keyboard block play music (Jukebox slider); tracks are a datapack registry. Theme packs `tlmkeyboardmusic_themes` and `_seihou` | yes (a later fixed build than the repo's) | – | – |
| Maid Trumpet Calls (`tlmtrumpet` 1.0.0) | 12 ZUNpet fanfares when the trumpet summons maids | yes | – | – |
| Love & Loathe: Maid Voices (`llvoices` 0.1.0) | Maids voice hunger, mood and trust/fear drift in chat bubbles, written by each maid's LLM character | yes | – | – |
| Chess PVP Seated (`chesspvp` 1.3.0) | Rewrite of ChessPVP: sit at a side of the board; checkmate, stalemate and draw detection | yes | GameTests exist (results not recorded) | – |
| Player2 Watch (`p2watch` 1.0.0) | Reminds you when the Player2 app is not running (client) | yes | client test 4/4 | – |
| Lost Maid model whitelist | Config only: 212 models, Gensokyo Legacy characters and YH Remilia excluded | yes | – | in-game re-pick |

### Gameplay and compatibility

| Mod (id, version) | What it does | Live | Tested | Open |
|---|---|---|---|---|
| Gensokyo bridge (`gensokyocompat` 1.1.1) | Runs Gensokyo Legacy beside the Youkai's Homecoming port: character swap, item redirects, hidden duplicates, stranger neutrality, hat effects, graze, quest tab, magical forest at forest frequency (Biolith), Lithium fix. Datapack `zz-gensokyo-bridge` (chest loot, structure biomes, spacing 64) | yes | harness 41/41 (1.1.0; 1.1.1 is a save fix) | quest tab and characters in-game; 1.1.0's Biolith change broke world saves and was fixed in 1.1.1 |
| Cold Sweat Extras (`csextras` 1.2.0) | Rainwear, umbrellas, sun hats, Origins rain/sun fix, shiver, sweat, campfire stoke, heartbeat, pitchfork, warming foods | yes | selftest 48/48 (1.1.0) | 1.2.0 untested by request |
| Season Climate (`seasonclimate` 1.0.0) | Scales Cold Sweat's seasonal swing by biome | yes | – | – |
| Drink Fix (`drinkfix` 1.1.0) | Juices and teas drink instead of eat and return the container; mixing bowl checks Thirst purity | yes | – | – |
| Seat Rest (`seatrest` 1.0.0) | Hunger and thirst drain slower while seated | yes | – | – |
| MCA Animation Compat (`mcaanimcompat` 1.4.0) | Player animations on MCA bodies, lying camera, Princess Carry fixes (both players need it) | yes | animations in-game; carry harness 8/8 | Princess Carry in-game |
| MCA RPG Bridge (`mcarpgbridge` 1.2.0) | Links MCA Quests, MCA Reputation and Mebahel's RPG quests; Mebahel tracker offset config | yes | server 14/14 | client button in-game |
| Spawn Onboarding (`spawnonboarding` 1.1.0) | MCA destiny = real spawn; places Eternal Town there; first-join screens destiny → Origins | yes | – | – |
| Village Waystone Names (`villagewaystones` 1.0.0) | Village waystones take the MCA village name | yes | – | in-game |
| Colony Expeditions (`colony_expeditions` 0.1.0) | MineColonies expedition huts for End, Otherside, Deeper Down, Bumblezone and 7 Eternal Tales dimensions; Netherworker uses Dimensional Tears | yes | server harness | owner's visual check |
| Guide Library (`guidelibrary` 1.0.0) | Inventory button opening every guide book you have ever held | yes | see note (index says tested 19 Sep, note body says in-game test not run) | – |
| Tab Folders (`tabfolders` 1.0.0) + `zz-tabs` datapack | Legendary Tabs folders, editor and extra tabs | yes | client test 29/29 | owner's use; friend needs jar |
| Gentle Swings (`gentleswings` 1.0.0) | Key H: Better Combat swings pass through NPCs, animals and players | yes | – | in-game |
| Grave Tag Cleaner (`gravetagcleaner` 1.0.0) | Strips leaked gravestone slot tags (PlayerRevive cause) | yes | – | in-game |
| Boss Enchant Cleaner (`bossenchantcleaner` 1.0.0) | Strips boss-only marker enchants from items and villager offers | yes | – | source NPC never identified |
| Ground Berries (`groundberries` 1.0.0) | Eternal Tales bushes ripen on the ground (mixin) | yes | server test queued 8 Oct, result unknown | if the game fails to launch, suspect this first |
| Rain Source Restore (`rainsource` 1.0.1) | Rain re-allows water source conversion where it falls | yes | – | – |
| Settled Caves (`settledcaves` 1.0.0) | Cave fluids and falling blocks generate settled | yes | README says untested; tune `maxOpening`/`maxFootprint` | behaviour unverified |
| Tamable Arcadian Fairy (`tamablearcadianfairy` 1.0.0) | Tame Arcadian Dream fairies with cake | yes | – | – |
| Butchery: Alex's Mobs (`butchery_alexs_mobs` 1.1.0) | Port of the Forge add-on | yes | GameTests 11/11 | in-game; friend needs jar |
| Butchery: Alex's Mobs Part 2 (`butchery_alexs_mobs_2` 1.0.0) | 80 more Alex's Mobs carcasses, Alex's/AMC Delight drops | yes | GameTests 109/109 + showcase | in-game; bug bleeding question |
| RoadWeaver Queue Gate (`roadweavergate` 1.0.0) | Throttles RoadWeaver's per-tick queue refresh | yes | server | – |
| Load My Chunks tick fix | Worked around an LMC crash; removed with LMC on 27 Sep | no | – | – |

### Tools (not for players)

| Tool | Purpose |
|---|---|
| Seed Scout (`seedscout`) | Dumps resolved worldgen and scans seeds; `/seedscout biomemap` renders biome share maps (used for the magical forest work) |
| Pregen Helper (`pregenhelper`) | Headless pregen: links Immersive Gateways, plans RoadWeaver roads, runs Chunky with ticking frozen |
| Pack Probe (`packprobe`) | Diagnostics: item combat stats, maid→player damage traces. Removed 18 Sep |
| Recipe audit (`modpack-audit/tools/recipe-audit`) | Scans for variant-erasing recipes and over-merges |
| AutoModpack scan (`modpack-audit/tools/automodpack-scan`) | Dry-runs AutoModpack's matcher |

## Patched third-party jars

Patches live in `modpack-audit/fixed-mods/` on the owner's PC, with the original jar kept as `*.ORIGINAL-BACKUP`.

| Jar | Patch | Note |
|---|---|---|
| Farmer's Delight 1.3.4 | Cutting board says "doesn't seem cuttable" for items without a cutting recipe | `fd-cutting-board-hint-patch.md` |
| Love & Loathe 2.0.9 | English text, 21 chat bubbles translated, wandering traders get 2 random maid trades + Shrine | `love-loathe-trader-pull.md` |
| Back Slots 1.1.5 | Quick-swap toggles and stays, like offhand | `backslots-sticky-swap.md` |
| Distraction Free Recipes 1.2.1 | JEI 19.40+ fix (spliced) | `dfr-jei-fix.md` |
| Compat Delight 1.0.1.1 | Module keys pointed at installed substitutes | `item-merge-pipeline.md` |
| WaterUpgrade 1.0.0 | Event-bus registration fix | `item-merge-pipeline.md` |
| Thirst Was Taken 2.1.5 | Patched; details in `fixed-mods/patch_thirst.py` | `survival-stack-twt-coldsweat.md` |
| better_lib 1.0.111 | Patched; purpose not recorded in the notes | – |
| danmaku_api 3.0.6 | Merged with 3.0.5 classes so Legacy and Arcadian Dream share one library | `gensokyo-bridge.md` |
| Touhou Little Maid 1.5.3 | Built from source with AI character settings for every built-in maid model (`feat/ai-prompts-1.21`) | `custom-mods/tlm/` |
| Delightful 3.8.2 (live), Maidsoul Kitchen 0.1.3 (live) | Rebuilt for this pack's mod versions | `custom-mods/delightful-port/`, `custom-mods/maidsoul-fix/` |
| Brass-Amber BattleTowers | Full port to NeoForge 1.21.1 (`neoport.1`) | `runic-gods-port.md` |

## Custom datapacks (root `datapacks/`, loaded by Simple Datapacks)

| Datapack | Purpose |
|---|---|
| `zz-unified-items` | Merged duplicate items (with AlmostUnified); only the most involved recipe kept. Rebuild from the baseline dump, never reuse broad tags (`item-merge-pipeline.md`) |
| `zzz-recipe-identity` | Compaction recipes accept only the item their uncraft returns |
| `zzz-recipe-progression` | A harder method must out-yield an easier one |
| `zzz-duplicate-recipes` | Keep only the Display Delight plateable version of duplicates |
| `colony-autoteach` | 21,646 auto-taught MineColonies crafter recipes. Generated: unregistered item ids brick server boot |
| `zz-gensokyo-bridge` | Gensokyo bridge data (see above) |
| `zz-tabs` | Legendary Tabs definitions |
| `compostables-restore`, `weeds-as-grass` | Restore compostables and gifts NeoForge 21.1 ignores; weeds compost like grass |
| `organic-compost-interchange` | Compost recipes accept corn refuse for straw |
| `corn-delight-raw-tortilla-off` | Disables a dead-end recipe |
| `coldsweat-thick-furs` | Thick furs count as goat fur |
| `scrap-meat-sources` | Four more ways to get Butchery scrap meat |
| `squid-calamari-dedup`, `cinnamon-tree-dedup`, `wild-crop-unification` | Remove duplicate drops, trees and wild crops |
| `butchery-alexsmobs-wiring`, `ExtraButchery-fixed`, `ButcheryCompats-spectrum`, `ButcherySpongeFix` | Butchery integration |
| `creature-links` | Alex's Mobs and vanilla tags wired to other mods' creatures |
| `spawn-category-fix` | Moves misfiled mobs back to their registered category |
| `eternaltown-structure-set`, `et-worldgen-ore-trim`, `eternaltales-gravel-tame` | Eternal Tales worldgen fixes |
| `recipesplus-dead-compat-off` | Disables Recipes Plus compat for uninstalled mods |
| `player-scale` | Origins Player Scale, ported to 1.21.1 |
| Third-party: `Bugfix1`, `RopeConvert`, `bloodybucket`, `Villager Clothing Addition REBORN` | – |

## Major config decisions

| Area | Decision | Note |
|---|---|---|
| Survival | Thirst Was Taken + Cold Sweat replaced Tough As Nails (19 Sep); biome temperatures incl. magical forest 51–76 °F | `survival-stack-twt-coldsweat.md` |
| Performance | ModernFix dynamic resources on (−1 GB heap); villager AI and Townstead sourcing cut; MineColonies capped (50 citizens); Seamless Chunks optimisations off | `villager-tps-tuning.md`, `minecolonies-perf-config.md`, `blockstate-memory-cost.md` |
| Structures | Better Sparse Structures whitelists `gensokyolegacy:*`; Legacy structures spaced 64 chunks; 52 existing Legacy structures removed on 6 Oct (backup `world-backups/GL-thin-2026-10-06`) | `gensokyo-bridge.md` |
| Worldgen | Magical forest equals forest via Biolith; TerraBlender region mixins on the vanilla region are no-ops with Biolith | `gensokyo-bridge.md` |
| Music | Immersive Music structure check re-enabled 27 Sep | `timm-structure-lag.md` |
| Tabs | L2Tabs hidden; Legendary Tabs + Tab Folders | `tab-folders-mod.md` |

## Removed or rejected

AnotherScape (19 Sep), Tough As Nails + TAN add-ons (19 Sep), Thin Air + Thin Air Throttle (29 Sep), Load My Chunks + tick fix and Lazy Chunk Load (27 Sep), Chunky Offline, EverCrops, Maid Task Scroll (1 Oct), Colorful Azaleas (5 Oct), Cutting Delight and Continents (15 Sep), Sword Blocking Mechanics (5 Sep), Project Atmosphere, TPS Detective, Illager Raid Music. **Rejected, do not re-suggest:** Exspectriments (1 Oct). **Abandoned:** AutoModpack (8 Oct, "not working, don't bother"); Thin Air for performance; Gensokyo Legacy's own sakura forest (YH's is kept). 52 orphan configs were archived on 27 Sep (`removed-configs/`).

## Rules the owner set (keep following them)

- **Never run a test server or client while another Minecraft instance is running**, including the owner's game or another session's test. Check all Java processes immediately before every boot, and wait for a full idle minute (`test-server/X-expeditions/wait-idle.sh`). Not alongside hosting either.
- **Client tests go in `test-client/C-client`, never the live instance.** Server tests go in `test-server/X-expeditions`.
- **The harness `mods/` and `datapacks/` are hard links to the live pack.** Never copy into them; put overrides in `extra-mods/` and `extra-datapacks/` and run `sync-mods.ps1`.
- **Datapacks belong in the instance root `datapacks/`**, not `<world>/datapacks/`. `/reload` is unreliable; restart instead. Watch for the `level.dat` Disabled trap.
- **Ask before installing into the live instance** unless the owner already said to; ask before anything that touches the friend's computer or publishes anything.
- **The friend needs every both-sided mod the owner adds.** Updates are manual ZIPs measured against the 8 Oct baseline.
- **Recipe rule:** a harder method must out-yield an easier one. **Item merges:** rebuild from the baseline dump.
- **Never add Amelet** to Tab Folders. The 11 Youkai's Homecoming characters that Gensokyo Legacy also has (Rumia included) are swapped to Legacy's; YH Remilia stays.

## Where things live on the owner's PC

For local sessions only; the cloud repo has copies of the sources and docs, not the jars.

| Path | Contents |
|---|---|
| `C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1 slim/minecraft` | Live instance |
| `C:/Users/eteil.000/Claude/modpack-audit/` | Main work folder: reports, most custom mods, `fixed-mods/`, datapack sources (`datapacks/`, `proposed-datapacks/`), `test-server/`, `test-client/`, `removed-*`, `world-backups/` |
| `C:/Users/eteil.000/Claude/<project>/` | The other mod projects (TLM add-ons, Butchery AM, Origins port, DFR fix, ...) |
| `C:/Users/eteil.000/.claude/projects/C--Users-eteil-000-Claude-modpack-audit/memory/` | The live notes (copied here as `notes/`) |
