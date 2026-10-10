# Work log and resume guide

Everything that was built, patched, configured or removed for the `1.21.1 slim` pack between 31 Aug and 10 Oct 2026, where it stands, and what is left to do. Written 10 Oct 2026 from all 81 Claude Code session transcripts on the owner's PC (indexed in `SESSIONS.md`), the notes, each project's own docs and git history, and a check of which built jars are byte-identical to the jars in the live `mods/` folder.

**How to use this file**
- Start with **Open threads** to see what can be resumed.
- For the details of any item, read its note in `notes/` (or `notes-other/<project>/` for projects outside `modpack-audit`), then its folder in `custom-mods/`, then `reports/` for the original session report. `SESSIONS.md` says which session did what.
- This file was checked against the transcripts, so where it says a note is outdated, trust this file. Otherwise the note has more detail.
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
| 31 Aug – 6 Sep | Colony Handover started (test instance). Origins port started (Touhou Origins onto Overgrown's Origins). Pack audit: duplicate features, AlmostUnified candidates, old-pack review, performance (ModernFix dynamic resources), old-config migration, fog/FPS overlays, worldgen duplicates. Ports of Delightful and Maidsoul Kitchen; cheese migration into Brewin' and Chewin'; Butchery: Alex's Mobs port; TLM AI prompts and trumpet calls; Sit! port (abandoned); Pale Oak fix | `reports/01`–`09` |
| 7 – 15 Sep | Tamable Arcadian Fairy, Origins port to completion (TLM model parts, shikigami pact), Colony Handover to s7.4, Arcadian Dream port fix, Settled Caves, Rain Source Restore. Dragonkind + YUNG End island checked in-game | – |
| 15 – 20 Sep | Config-reset diagnosis, colony sweep, item-merge pipeline, heads/geodes/workstations, kettles and racks, Back Slots, Compat Delight, spawn hub, combat mods, sea spawns, OSRS special attacks. AnotherScape removed; Tough As Nails replaced by Thirst Was Taken + Cold Sweat; MCA Quests/Reputation/Mebahel bridge; Farmer's Contracts crash; REI categories. Mods: Battletowers port, Guide Library, Spawn Onboarding, Seat Rest, Drink Fix, Colony Expeditions, Season Climate | `reports/10`–`26` |
| 25 – 29 Sep | FD cutting-board hint, DFR JEI fix, Gentle Swings, Load My Chunks tick fix, MCA Animation Compat 1.4, Mebahel 3.2 bridge update, Butchery: Alex's Mobs parts 1 and 2, villager TPS tuning, Thin Air / RoadWeaver performance, Cold Sweat Extras, recipe identity and progression audits, headless pregen | `reports/27`, `28`, `23-recipe-identity-audit`, `25-recipe-shortcuts` |
| 30 Sep – 3 Oct | Feature-usage triage from player stats. Maid Chef (all cooking stations), Maid Rest Beds, Maid Task Curator, Player2 Watch. TLM add-ons: Working Maid Computers (AntOS), Maid Keyboard Music, Maid Trumpet Calls, Love & Loathe voices, Chess PVP (Seated). Love & Loathe English text | `reports/29`, `30` |
| 4 – 8 Oct | Gensokyo Legacy integrated beside the Youkai's Homecoming port (bridge 1.0 → 1.1.1, magical forest, chest loot, structure density cut and thinning). AutoModpack tried, then abandoned. Colorful Azaleas removed. Tab Folders. Lost Maid whitelist. Block-state memory measured. Ground Berries, compost and duplicate-recipe datapacks. Friend resynced with a full mods folder | – |

---

## Open threads (resume candidates)

Ordered roughly by value.

1. **Lava/water bucket bug (28 Sep, unresolved).** On the hosted world the server refuses bucket placement against stone (and later dirt), leaving ghost lava; doors desynced after a death. Seamless Chunks' optimisations were turned off (this only made the server correct the client). Ruled out: Settled Caves, MineColonies, configs, the owner's origin. **Main suspect: Fluidlogged 2.0.1-beta.8** (it replaces the bucket "can fluid go here" check and hooks doors); second: Mythic Metals' bucket hook. Proposed next step: the friend tries buckets; then remove Fluidlogged after a world backup. The owner never answered. `notes/blockstate-desync-seamless-chunks.md` predates this diagnosis.
2. **In-game verification backlog.** Most custom mods passed server or harness tests but were never checked while playing. Highest risk: Gensokyo bridge (quest tab, swapped characters); Princess Carry and lying camera (needs two players); Butchery AM parts 1 and 2 (8 Oct hook, faces and hint fixes); Tab Folders (linked colony clipboard untested); Maid Chef (Spectrum stations); Maid Friendly Fire 1.3.0 trader maids (8 Oct); Lost Maid whitelist; Ground Berries; Origins port two-player features (shikigami tether, betrayal) and rendering/sound checks (`custom-mods/origins-port/VERIFY.md`, `SOUND-TEST.md`); Battletowers full tower run; Colony Expeditions huts in the build tool. Use the client scaffold, never the live instance.
3. **Owner decisions still open** (each was asked and never answered):
   - Butchery AM: should bugs bleed (our cleaver shows blood on arthropods; Butchery's spider doesn't)? Add a text label under the crosshair hint?
   - MCA Anim Compat: lift ground sits ~0.25 block (pose sinks on flat ground)? Carried-player height for MCA-scaled bodies?
   - Paper and compost: make all barks interchangeable for paper, let corn refuse make paper, cut birch bark from 3 to 1 paper (it breaks the progression rule)?
   - Duplicate dishes with different recipes (cod stew FD vs Youkai; cheese sandwich/cheeseburger ED vs Compat Delight; hot cocoa and melon juice FD vs Farmer's Respite; onion soup; potato soup; fruit salad; cranberry juice; coconut milk): disable the non-plateable side? Hide Corn Delight's raw tortilla from JEI?
   - Quark stoneling/toretoise/foxhound spawn-category fix: leave, cut spawn weight, or re-add as CREATURE?
   - Tentacle fix side effect: Rustic Delight and Cultural Delights calamari now merge (Rustic kept) - keep?
   - Village Waystone Names: allow manual renames (toggle)? Maid Rest Beds: add sleeping bags?
   - Older (18 Sep): Back Slots vs Curios back slot; kettle/keg merge (blocked by fluid vs item recipes); give every new player an Eternal Town Spawner; geode rarity re-tune; Spawn Onboarding respawn point (1, 70, 134) sensible?
   - Delete world backups once checked in game: `world-backups/GL-thin-2026-10-06` (1.2 GB) and `2026-10-05 pre-regen` (9.1 GB).
4. **Requested but never built.** Take a Seat "don't force third person" toggle (25 Sep; the height part was done in MCA Anim Compat 1.1.0, the toggle was not). Cheaper Immersive Music structure check (`notes/timm-structure-lag.md`). True BSS reservation for Legacy structures. Cutting board with no durability cost for chosen recipes.
5. **Publishing and upstream.** DFR JEI fix: **PR #27 is open** on txnimc/DistractionFreeRecipes (opened by a cloud session on 25 Sep from branch `fix/jei-19.40-overlay-fp3dmd`); follow it up, and a fork Release was planned (ask first). Maid Trumpet Calls is on GitHub (`eileenthg/maid-trumpet-calls`); the second CI run was never checked, and CurseForge/Modrinth release needs the owner's project ids and tokens, then a `v1.0.0` tag. Maid Keyboard Music is on GitHub (`eileenthg/maid-keyboard-music`), unpublished (track rights unresolved). Chess PVP Seated, Working Maid Computers, AntOS games and the Origins port are private and must not be published (closed-source or licence-blocked originals).
6. **Colony Handover** (MineColonies colony → independent MCA village). Built to stamp `s7.4-fixes` (14 Sep) and installed only in the old test instance, not the live pack. Waiting on the owner's results from the 16-step test list (artifact https://claude.ai/code/artifact/98edb82a-28b1-4829-8555-ee882b43f699). The owner has no colony in the live world yet.
7. **Origins port follow-ups.** Live and working; still open: Smite vulnerability for jiang_shi, `is_closed_eye` predicate, vengeful spirit's calculation, enchantment vulnerability (parked); licence check before any release. Also: the owner's Origins primary/secondary power keys are **unbound** because G belongs to Back Slots (25 Sep).
8. **Runic Gods port** (Forge 1.20.1 → NeoForge 1.21.1). Paused since 19 Sep, to resume after a full Battletowers run in game.
9. **Cheese migration** (Expanded Delight cheese into Brewin' and Chewin'): live, never play-tested; commits are local only (BnC `a8c3757`, ED `70281d4`, `1237028`). First check: the goat-milk keg bridge.
10. **Block trimming** (unused Youkai's Homecoming blocks and others). Deferred by the owner; measured 7 Oct to save tens of MB at most.
11. **Music overlap** (10 Oct, unresolved): a track on the Blocks slider overlapping normal music. Closest candidate is Matmos (own biome music on the Ambient slider; `/matmossettings` → Music off). Waiting on the owner's biome and slider test.
12. **Friend's client.** Updates are by hand (AutoModpack abandoned 8 Oct). The friend reinstalled the whole pack on 10 Oct and it works; diff future changes against that.
13. **Small unknowns.** Boss Enchant Cleaner's source NPC (friend to F3 it); a netherite door found in a chest (no loot source found); possible key clash on H between Gentle Swings and MCA Romantic Expansion's carry key (unverified); the body-turning push while lying on your side (some unknown mod, worked around).

---

## Custom mods (written for this pack)

All are NeoForge 1.21.1. "Live" = the built jar is byte-identical to the one in the live `mods/`.

### Touhou Little Maid add-ons

| Mod (id, version) | What it does | Live | Tested | Open |
|---|---|---|---|---|
| Maid Chef (`maidchef` 0.1.0) | "Chef" maid task: picks the most valuable dish it can make from an ingredient chest, cooks it at any of the pack's stations, outputs to a chest; Player2 chat | yes | harness 45/45 | in-game check; Spectrum stations unchecked |
| Maid Rest Beds (`maidrestbeds` 1.0.0) | Maids sleep in ordinary beds or Comforts hammocks when no maid bed is free | yes | harness 28/28 | in-game |
| Maid Task Curator (`taskcurator` 1.0.0) | Task menu folders, hiding and an editor (client) | yes | harness 23/23 | – |
| Maid Friendly Fire (`maidfriendlyfire` 1.3.0) | Undoes Maid Spell's blanket block so hostile/Lost Maids can hurt players; owned maids target Lost Maids; never hurts the owner or the owner's maids. 1.3.0 (8 Oct): wandering-trader maids stay calm and run off and despawn if their leash breaks, instead of turning Lost | yes | runtime log check (1.1.0) | 1.3.0 trader-maid flee untested |
| Working Maid Computers (`workingmaidcomputers` 0.1.0) | Sit at the maid computer, right-click again: a vendored AntOS desktop opens (files, terminal, mail, paint, tasks, 5 mini-games: Coin Miner, Paddle Ball, Tic-Tac-Toe, Minesweeper, Checkers). Floppies install into the Anternet account | yes | unit tests 19/19 (games) | private only (AntOS has no licence); nothing committed in its repo; keep the games in sync with the `antos-games` fork |
| Maid Keyboard Music (`tlmkeyboardmusic` 1.0.0) | Maids or players at the keyboard block play music (Jukebox/Note Blocks slider, positional); tracks are a datapack registry with per-model character themes. Theme add-ons `tlmkeyboardmusic_themes` (221 MB, 184 tracks) and `tlmkeyboardmusic_seihou` | yes (the owner's fixed build from 3 Oct, newer than the repo) | a cloud session ran in-game tests (not on this PC) | repo `eileenthg/maid-keyboard-music`; not published (track rights) |
| Maid Trumpet Calls (`tlmtrumpet` 1.0.0) | 12 ZUNpet fanfares when the trumpet summons maids; datapacks can add calls with weights | yes | build | on GitHub `eileenthg/maid-trumpet-calls`; CI run 2 and store publishing pending |
| Love & Loathe: Maid Voices (`llvoices` 0.1.0) | Maids voice hunger, mood and trust/fear drift in chat bubbles, written by each maid's LLM character | yes | – | – |
| Chess PVP Seated (`chesspvp` 1.3.0) | Rewrite of ChessPVP: sit at a side of the board; checkmate, stalemate and draw detection | yes | GameTests 9/9 | seat height guessed; private only (original is All Rights Reserved) |
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
| Spawn Onboarding (`spawnonboarding` 1.1.0) | MCA destiny = real spawn; pastes Eternal Town there (one tick, frozen build, mobs moved out, break protection on the town's own blocks); first-join screens destiny → Origins; one banker and one exchange clerk per village next to the town bell; Alex's tarantula hawks hunt modded spiders | yes | dedicated server runs | both sides need it |
| Village Waystone Names (`villagewaystones` 1.0.0) | Village waystones take the MCA village name (re-checked every 5 s; manual renames get undone) | yes | – | in-game (Zedreded → Mossburgh at 45, 74, 30) |
| Colony Expeditions (`colony_expeditions` 0.1.0) | MineColonies expedition huts for End, Otherside, Deeper Down, Bumblezone and 7 Eternal Tales dimensions (research-gated); per-destination minimum gear, per-hut override; Netherworker portals light only with Dimensional Tears | yes | server harness + owner joined the test server | huts in the build tool; Heart of the Deep cost per Otherside trip is steep |
| Guide Library (`guidelibrary` 1.0.0) | Inventory button opening every guide book you have ever held | yes | client test 19 Sep (unlocks, refusal of locked books) | the later 0.25 s click-limit fix for MCA books is untested |
| Tab Folders (`tabfolders` 1.0.0) + `zz-tabs` datapack | Legendary Tabs folders, editor and extra tabs | yes | client test 29/29 | owner's use; friend needs jar |
| Gentle Swings (`gentleswings` 1.0.0) | Key H: Better Combat swings pass through NPCs, animals and players | yes | dev client boot | Better Combat hook never run with Better Combat; possible H clash with the carry key |
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
| Colony Handover (`colonyhandover`) | Converts an abandoned MineColonies colony into an independent MCA village: preview/place/marks commands, placement engine for job blocks, colony coffer, conversion checklist (BlockUI) on the town hall, Grant Independence button, More Villagers, Chef's Delight and Friends&Foes support | no (old test instance only) | owner's in-game tests up to s7.3 | waiting for the s7.4 test results |
| Arcadian Dream NeoForge port (`arcadiandream` 0.2.0) | The live Arcadian Dream jar is this port, with a config-unload crash fix (15 Sep) | yes | – | **its source folder (`Claude/arcadian-dream`) no longer exists on disk** |
| Sit! port (`sit_oth3r`) | Standalone port of Oth3r's Sit! | no | boots | abandoned 6 Sep (wrong mod); folder `Claude/sit!` |
| Extra Delight REI plugin (`edrei`) | Extra Delight's JEI categories for REI | no | – | obsolete: the pack moved to JEI; source folder gone |

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
| Love & Loathe 2.0.9 | English text, 21 chat bubbles translated, wandering traders get 2 random maid trades + Shrine. Re-run the English patch last after any trade-pull rebuild | `love-loathe-trader-pull.md` |
| Back Slots 1.1.5 | Quick-swap toggles and stays, like offhand | `backslots-sticky-swap.md` |
| Distraction Free Recipes 1.2.1 | JEI 19.40+ fix (spliced); upstream PR #27 open | `dfr-jei-fix.md` (outdated: the branch was pushed and the PR opened) |
| Compat Delight 1.0.1.1 | Module keys pointed at installed substitutes | `item-merge-pipeline.md` |
| WaterUpgrade 1.0.0 | Event-bus registration fix | `item-merge-pipeline.md` |
| Thirst Was Taken 2.1.5 | Jade-compat mixin crash removed; thirst can no longer kill on Normal (same rule as hunger) | `survival-stack-twt-coldsweat.md` |
| better_lib 1.0.111 | Patched; purpose not recorded in the notes | – |
| danmaku_api 3.0.6 | Merged with 3.0.5 classes so Legacy and Arcadian Dream share one library | `gensokyo-bridge.md` |
| Touhou Little Maid 1.5.3 | Built from source with AI character settings for every built-in maid model (`feat/ai-prompts-1.21`) | `custom-mods/tlm/` |
| Delightful 3.8.2 (live) | Ported to 1.21.1 (12 local commits: tags, loot conditions, Miner's/Nether's Delight ids) | `custom-mods/delightful-port/patches/` |
| Maidsoul Kitchen 0.1.3 (live) | Kettle and Ferment tasks fixed for the Youkai's Homecoming 1.21.1 port | `custom-mods/maidsoul-fix/`, `notes-other/maidsoul-fix/` |
| Brewin' and Chewin' 5.0.0 + Expanded Delight 0.1.4 (both live) | Cheese migration: B&C gains goat cheese; ED's cheese tags point at B&C wedges; ED casks hidden with Item Obliterator | `custom-mods/ED-cheesemaking-migrate/*/patches/`, `notes-other/ED-cheesemaking-migrate/` |
| Apoli 1.56.0, Origins 1.27.0 (Overgrown's), Touhou Origins 2.0.2 (all live) | Touhou Origins ported from Fabric (Voile) to Overgrown's Origins on NeoForge: all power types, model features from TLM, shikigami pact, water protection enchantment; Player Scale datapack ported | `custom-mods/origins-port/` (CLAUDE.md, NOTES.md, sources in `touhou-origins-neo/`) |
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
| `organic-compost-interchange` | Compost straw slots accept dried corn husk, hay block and thatch; corncob slots accept any corn refuse |
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
| Survival | Thirst Was Taken + Cold Sweat replaced Tough As Nails (19 Sep); biome temperatures incl. magical forest 51–76 °F; Serene Seasons starts in Mid Spring; Season Climate scales seasonal swings by biome; Cold Sweat insulation: 16 thick furs = goat fur, cotton/flax/canvas heat-only | `survival-stack-twt-coldsweat.md`, `coldsweat-furs-fibres.md` |
| Performance | ModernFix dynamic resources on (−1 GB heap); villager AI and Townstead sourcing cut; MineColonies capped (50 citizens); Seamless Chunks optimisations off | `villager-tps-tuning.md`, `minecolonies-perf-config.md`, `blockstate-memory-cost.md` |
| Structures | Better Sparse Structures whitelists `gensokyolegacy:*`; Legacy structures spaced 64 chunks; 52 existing Legacy structures removed on 6 Oct (backup `world-backups/GL-thin-2026-10-06`) | `gensokyo-bridge.md` |
| Worldgen | Magical forest equals forest via Biolith; TerraBlender region mixins on the vanilla region are no-ops with Biolith | `gensokyo-bridge.md` |
| Music | Immersive Music structure check re-enabled 27 Sep | `timm-structure-lag.md` |
| Tabs | L2Tabs hidden (`config/l2configs/l2tabs-client.toml`); Legendary Tabs + Tab Folders | `tab-folders-mod.md` |
| Gamerules | `waterSourceConversion` true; on 25 Sep the owner said it should have been false (Rain Source Restore was built for that) | `pregen-headless-roadweaver.md` |
| Keybinds | Reset to vanilla on 19 Sep for exporting the instance (old keys in `modpack-audit/options.txt.bak-2026-09-19-keybinds`); Back Slots = G, Gentle Swings = H, Origins power keys unbound | – |
| Client | Two Sodium Extra mods both draw an FPS counter; cloud mode HYBRID, max FPS 120 | `reports/08` |

## Removed or rejected

AnotherScape (19 Sep), Tough As Nails + TAN add-ons (19 Sep), REI and YACL (pack is JEI-only by 27 Sep), White Noise, the OSRS special-attacks mod, Alex's Caves, Cluttered, CTOV, Ecliptic Seasons, Better Advancements, Just Enough Resources, Thin Air + Thin Air Throttle (29 Sep), Load My Chunks + tick fix and Lazy Chunk Load (27 Sep), Chunky Offline, EverCrops, Maid Task Scroll (1 Oct), Colorful Azaleas (5 Oct), Cutting Delight and Continents (15 Sep), Sword Blocking Mechanics (5 Sep), Project Atmosphere, TPS Detective, Illager Raid Music. **Rejected, do not re-suggest:** Exspectriments (1 Oct); Compat Delight's Malum module (27 Sep). **Abandoned:** AutoModpack (8 Oct, "not working, don't bother"); Thin Air for performance; Gensokyo Legacy's own sakura forest (YH's is kept). 52 orphan configs were archived on 27 Sep (`removed-configs/`).

## Rules the owner set (keep following them)

- **Never run a test server or client while another Minecraft instance is running**, including the owner's game or another session's test. Check all Java processes immediately before every boot, and wait for a full idle minute (`test-server/X-expeditions/wait-idle.sh`). Not alongside hosting either.
- **Client tests go in `test-client/C-client`, never the live instance.** Server tests go in `test-server/X-expeditions`.
- **The harness `mods/` and `datapacks/` are hard links to the live pack.** Never copy into them; put overrides in `extra-mods/` and `extra-datapacks/` and run `sync-mods.ps1`.
- **Datapacks belong in the instance root `datapacks/`**, not `<world>/datapacks/`. `/reload` is unreliable; restart instead. Watch for the `level.dat` Disabled trap.
- **Ask before installing into the live instance** unless the owner already said to; ask before anything that touches the friend's computer or publishes anything.
- **The friend needs every both-sided mod the owner adds.** Updates are manual ZIPs measured against the 8 Oct baseline.
- **Recipe rule:** a harder method must out-yield an easier one. **Item merges:** rebuild from the baseline dump.
- **Only launch client tests on the owner's signal** (Origins sessions), and tell them before booting anything while they may be hosting.
- **While another session is testing, stage builds in `modpack-audit/fixed-mods/`** instead of copying into the live `mods/` (a jar copied mid-launch caused a spurious crash on 17 Sep).
- **Reply in English**, even where an upstream repo's AGENTS.md asks for Chinese (TLM).
- **Never add Amelet** to Tab Folders. The 11 Youkai's Homecoming characters that Gensokyo Legacy also has (Rumia included) are swapped to Legacy's; YH Remilia stays.

## Where things live on the owner's PC

For local sessions only; the cloud repo has copies of the sources and docs, not the jars.

| Path | Contents |
|---|---|
| `C:/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1 slim/minecraft` | Live instance |
| `C:/Users/eteil.000/Claude/modpack-audit/` | Main work folder: reports, most custom mods, `fixed-mods/`, datapack sources (`datapacks/`, `proposed-datapacks/`), `test-server/`, `test-client/`, `removed-*`, `world-backups/` |
| `C:/Users/eteil.000/Claude/<project>/` | The other mod projects (TLM add-ons, Butchery AM, Origins port, DFR fix, ...) |
| `C:/Users/eteil.000/.claude/projects/C--Users-eteil-000-Claude-modpack-audit/memory/` | The live notes (copied here as `notes/`) |
| `C:/Users/eteil.000/.claude/projects/<project>/memory/` | Notes of the other projects (copied here as `notes-other/`) |
| `C:/Users/eteil.000/.claude/projects/<project>/<id>.jsonl` | Session transcripts (indexed in `SESSIONS.md`) |
| `C:/Users/eteil.000/Claude/modpack-audit/world-backups/` | World backups from the regen and Legacy thinning (10 GB, deletable after an in-game check) |
