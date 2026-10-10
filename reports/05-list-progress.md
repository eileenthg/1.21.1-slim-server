# List progress

State: 378 mods enabled, none disabled. `compatartifacts` deleted, Delightful back in.

## Installed

| What | Where |
|---|---|
| Quark config, **17** modules off (incl. Improved Tooltips) | `config/quark-common.toml` — copy kept in `proposed-configs/` |
| `minecolonies-crop-freedom.zip` (64 files) | `modpack-audit/datapacks/` |
| `ExtraButchery-fixed.zip` (185 files) | `modpack-audit/datapacks/` |
| `squid-calamari-dedup.zip` (15 files) | `modpack-audit/datapacks/` |
| Patched `better_lib` jar | installed; original at `fixed-mods/*.ORIGINAL-BACKUP` |

**Simple Datapacks** is wired to load them: I appended `C:\Users\eteil.000\Claude\modpack-audit\datapacks` to `optionalDatapacksPaths` in `config/simple_datapacks.json`. So the datapacks stay in the audit folder and Simple Datapacks pulls them in — nothing to copy per world. They're *optional* packs, so enable them per world (`copyDatapacks` is currently `false`).

---

## MineColonies crops — much bigger than I first thought

You were right that I'd missed recipes. My first pass only covered `crafterrecipes/` (26 files). MineColonies also ships `data/minecolonies/recipe/` — ordinary crafting/smelting recipes — with another 38 crop-referencing files. That's exactly where **soy milk, tofu, soysauce, fried rice, cooked rice, corn chowder, cornmeal, mint tea, mint jelly, pea soup and pepper hummus** live. The datapack now covers **64 files across both folders**.

I checked before blanket-replacing: **no MineColonies crop is ever a recipe *result*** in either folder, only an ingredient, so there's no risk of creating wheat→wheat loops.

Final mapping, every id verified present:

| MineColonies | → | Files |
|---|---|---|
| `durum` | `minecraft:wheat` | 24 |
| `garlic` | `extradelight:garlic` | 34 |
| `onion` | `farmersdelight:onion` | 31 |
| `tomato` | `farmersdelight:tomato` | 8 |
| `eggplant` | `culturaldelights:eggplant` | 7 |
| `cabbage` | `farmersdelight:cabbage` | 6 |
| `nether_pepper` | `extradelight:chili` | 5 |
| `bell_pepper` | `rusticdelight:bell_pepper_red` | 2 |
| `chickpea` | `ramadandelight:chickpea` | 2 |
| `mint` | `extradelight:mint` | 3 |
| `rice` | `farmersdelight:rice` | 1 |
| `soybean` | `extradelight:soybeans` | 3 |
| `corn` | `extradelight:corn_on_cob` | 3 |
| `peas` | `compatdelight:peas` | 3 |

**Nether pepper** — you asked for chili if it exists, bullet pepper otherwise. Chili exists, so I used `extradelight:chili`. But `mynethersdelight:bullet_pepper` is right there and is an actual *nether* crop, which fits `nether_pepper` far better thematically and costs more, as you predicted. It's a one-line swap in the datapack if you'd rather have it — say the word and I'll rebuild.

**Chickpea** now resolves thanks to Ramadan Delight (`ramadandelight:chickpea`), which also brings dates, parsley, tabbouleh, hummus tahini and maqluba — no conflict with anything you have.

To finish "completely replaced", use **Item Obliterator** on the 14 `minecolonies:*` crop items and their seeds — but only after you've played a colony through a few meals, and check no MineColonies research or building still hands them out.

## Squid / calamari — your memory was right, my datapack was incomplete

Miner's Delight does **not** override the squid loot table. It uses **global loot modifiers** (`scavenging_squid`, `scavenging_glow_squid`, `scavenging_cooked_squid`, `scavenging_tentacles`, plus `fishing_squid`), which stack on top of whatever the table produces. So restoring vanilla loot — which my dedup pack does — does not stop it. Good catch.

**But your cleaver assumption is inverted.** The drop is gated on the *attacker's* mainhand:

```json
"predicate": { "equipment": { "mainhand": { "items": "#farmersdelight:tools/knives" } } }
```

So `minersdelight:squid` drops **only when you kill the squid holding a Farmer's Delight knife** — not "anything except a cleaver". And a **Butchery cleaver does *not* trigger it**, because cleavers aren't in that tag. Butchery only adds cleavers to `butchery:knives`, `c:cleaver` and the *legacy 1.20* path `data/farmersdelight/tags/items/tools/knives.json` — note `items/`, which 1.21.1 ignores (it reads `tags/item/`). That's a real latent bug in Butchery: its contribution to the modern FD knives tag is dead. Its `c:cleaver` and `butchery:knives` tags *include* `#farmersdelight:tools/knives`, which inherits FD knives into Butchery's tags, not the other way round.

Net result, which is actually a tidy division of labour:
- **Butchery cleaver** → carcass block, no MD squid.
- **FD-family knife** (FD, Delightful, End's, Aquaculture, Spectrum) → MD whole squid.
- **Bare hand / sword** → plain loot table.

Since Rustic Delight's calamari is gated behind its own `squids_drop_calamari` config and Cultural Delights was the only mod *unconditionally* replacing the loot table, the dedup pack handles the real conflict. **Miner's Delight is a compatible third mechanic**, so I left it alone. If you'd rather have calamari as the only squid product, say so and I'll add a small pack disabling MD's five squid GLMs — but note that kills MD's squid sandwich, stuffed squid, takoyaki and baked squid.

## ExtraButchery — Butchery stand-ins placed

110 butchercraft entries stripped. Of the 16 tags that emptied, only **3 are actually used by any recipe**, and those now have real Butchery cuts:

| Tag | Stand-in | Used by |
|---|---|---|
| `c:chicken/cubed/raw` | `butchery:raw_chicken_leg`, `raw_chicken_wing` | Extra Delight honey chili chicken, orange chicken |
| `c:pork/ground/raw` | `butchery:raw_pork_shoulder`, `raw_pork_belly` | Extra Delight naem moo ×2 (shoulder/belly are the cuts actually ground for sausage) |
| `c:pork/scrap/cooked` | `butchery:cooked_pork_shoulder`, `cooked_pork_leg` | Extra Delight pulled pork ×2 (shoulder is *the* pulled-pork cut) |

The other 13 are safe to leave deleted. Eleven are referenced by nothing at all, and `c:strings` / `c:slime_balls` are standard tags that 157 and 4 recipes respectively pull from other mods — ExtraButchery was only *adding* butchercraft items to them, so removing its contribution empties nothing.

Butchery genuinely has no oxtail, brain, cubed anything, or ground pork/chicken — that's why those tags existed only for butchercraft.

**On butchercraft itself:** agreed, it overlaps Butchery heavily. Worth knowing that **Extra Delight already ships a `butchercraft:grinder` recipe**, so it has first-class butchercraft support baked in. If you ever do port cuts into Butchery, the gap list is exactly those 13 tags.

## Almost Unified priorities updated

`eternal_tales` is now **last** in `mod_priorities` in both `foods.json` and `materials.json`, so every other mod wins over it while its content still exists — the RPG stays, the duplicates resolve to the better-integrated mod.

New `priority_overrides`: corn → `extradelight` (so **Corn Delight keeps its Display Delight food blocks but uses Extra Delight's corn**), eggplant → `culturaldelights` (kept for the eggplant, everything else deduped), chickpea → `ramadandelight`.

## Corrections accepted

- **Stylish Effects vs Convenient Effects** — you're right, not duplicates. [Stylish Effects](https://modrinth.com/mod/stylish-effects) is the status-effect *HUD/inventory display*; [Convenient Effects](https://modrinth.com/mod/convenient-effects) is an effects *screen/management* mod. Keep both. Struck from the drop list.
- **`lootintegrations_bygonepillage`** — targets It Takes a Pillage, which you have. Keep. My "targets an absent mod" call was wrong.
- **Compat Delight** — breadth is the point; Mutant Monsters activated more of it.
- **Crash Assistant + Not Enough Crashes** — different jobs, both stay.

## Eternal Tales iceologer — where it spawns

Data-driven biome modifier, weight 10, 1 per group, in exactly four biomes:

```
frozen_river, snowy_plains, snowy_beach, ice_spikes
```

Friends&Foes' iceologer spawns from code (config `enableIceologerSpawn`), plus it has an **iceologer cabin structure** and raid integration. So they overlap directly in snowy biomes — you'd meet two different iceologers in the same places.

Since the RPG is the point, keeping both is defensible; they're distinct mobs, not a technical conflict. If you want one, F&F's is the richer implementation (cabin + raids). If you want to keep ET's *and* separate them, the cleanest split is to leave ET's four biomes alone and it'll simply be the rarer variant.

## Still open

- **Alex's Delight vs AMC Delight — pinned**, pending your Butchery compat for Alex's Mobs. Note `butchery_alexs_mobs` is enabled and references both `capuchin` and `capuchin_monkey`, which looks like it straddles a rename between original Alex's Mobs and the Continued fork you run — worth a look when you get to it.
- **Smarter Farmers + Liberty's Villagers merge** — answered below.
- **MCA villagers + Quark/LV/SF** — answered below.

---

# Round 2

## Squid: knife kills now drop plain loot

Done, and the dedup pack is repacked (20 files). I neutralised Miner's Delight's five **kill-based** loot modifiers by keeping their type and payload but replacing the conditions with `minecraft:random_chance` at `0.0`, so they can never fire:

| Modifier | Was giving |
|---|---|
| `scavenging_squid` | `minersdelight:squid` |
| `scavenging_glow_squid` | `minersdelight:glow_squid` |
| `scavenging_cooked_squid` | `minersdelight:baked_squid` |
| `scavenging_tentacles` | `minersdelight:tentacles` |
| `scavenging_cooked_tentacles` | `minersdelight:baked_tentacles` |

I used an impossible loot condition rather than deleting the files or editing `global_loot_modifiers.json`, because that index is shared (`"replace": false`) and rewriting it would mean re-listing every other mod's modifiers.

Result: **FD knife now behaves exactly like a bare hand** — plain vanilla ink sacs. Whole squid comes only from a Butchery cleaver, which drops `butchery:squid_carcass` (and `butchery:squid_head`) for you to butcher.

**Two things to be aware of.** Miner's Delight's squid dishes — squid sandwich, stuffed squid, takoyaki, baked squid, glow ink pasta — take `minersdelight:squid`/`tentacles`, which are now unobtainable by killing. Either unify them onto the Butchery carcass products in Almost Unified, or accept those recipes are dead. Second, I deliberately left `fishing_squid` and `fishing_glow_squid` alone: those are a 5% fishing catch, not a kill, so they don't contradict "get the carcass with a cleaver". Say the word if you want them off too.

## Clouds: Project Atmosphere has a VANILLA mode

You can have exactly what you want. Project Atmosphere hard-requires `simpleclouds [0.7.3,)`, so Simple Clouds must stay installed — but the chunky volumetric look is largely **PA's own renderer**, not just Simple Clouds. PA ships its own volumetric cloud pipeline (`cloudVolumetricRendererEnabled`, `cloudRaymarchQuality`, `enableVolumetricGroundShadows`) — that raymarched shading is why they look so detailed.

`cloudMode` accepts **FULL, HYBRID, VANILLA**. I've set it to `VANILLA` in `config/projectatmosphere-common.toml` (backup: `proposed-configs/*.BEFORE`). PA's weather simulation, wind, temperature and Serene Seasons integration all keep running — only its cloud rendering steps aside.

If Simple Clouds' own clouds still render after that, the ladder is:

1. `config/simpleclouds-client.toml` → `[debug] renderClouds = false` — the mod's own toggle, keeps `generateMesh` and the weather data PA reads.
2. If that leaves you with *no* clouds at all (Simple Clouds cancels vanilla rendering via mixin, so this is possible), put `renderClouds` back to `true` and instead set `clientSideCloudMode = "SINGLE"` with `clientSideSingleModeCloudType = "simpleclouds:itty_bitty"` — the smallest, flattest of its seven types (`cumulonimbus, cumulus, itty_bitty, nimbostratus, small_cumulus, stratocumulus, stratus`). Not vanilla, but far closer than the default.

Try step 1 only if VANILLA mode alone isn't enough — one change at a time.

## Sodium — you're already clean

Worth confirming since Supplementaries declares an incompatibility: `supplementaries` refuses `sodium [0,0.8.12-beta.1)`, and you have **0.8.13**, so you're fine. `chloride` and `reeses-sodium-options` both want `[0.8.12,)` — also satisfied. And Sodium declares `embeddium` **incompatible**; you have no Embeddium. Nothing to do.

## Smarter Farmers + Liberty's Villagers — don't merge, disable overlap instead

They only really collide on the farmer. Liberty's Villagers is broad — it rewrites hostile-mob avoidance, door/bed behaviour, panic, workstation search, sleeping, and gives config toggles for each; farming is one slice. Smarter Farmers is narrow and does the thing you actually want: farmers plant **any** modded crop from their inventory rather than only the vanilla four.

Merging them into one mod isn't worth it, because the fight is only over the farmer's `HarvestFarmland`-equivalent AI task. The right fix is to keep both and turn Liberty's Villagers' farmer features off in `config/LibertyVillagers.json` — it exposes farmer-specific options (crop search range, prefer-same-crop, melon/pumpkin harvesting). Leave its non-farmer behaviour on, and Smarter Farmers owns planting. Since its config is JSON with per-feature booleans, this is a config change, not a code one.

I'd only revisit that if you see farmers stalling mid-field, which is the classic symptom of two mods both trying to own the same AI task.

## Making Quark / Liberty's / Smarter Farmers affect MCA villagers

Short version: **it's possible, but it isn't a small mod.**

The blocker is that MCA replaces villagers with its own entity rather than extending `net.minecraft.world.entity.npc.Villager`. Every one of those mods targets the vanilla class:

- **Smarter Farmers** and **Liberty's Villagers** mixin into vanilla villager AI tasks (`HarvestFarmland`, `VillagerGoalPackages`, POI/workstation logic). MCA's entity never runs those tasks, so the mixins never fire.
- **Quark's** villager tweaks (More Villagers, Villagers Follow Emeralds, Zombie Villagers on Normal) hook vanilla villager registration and trade tables.

So there's nothing to "enable" — the code paths simply aren't reached.

What a bridge mod would actually have to do, in rough order of effort:

1. **Trades are the easy win.** MCA villagers do use professions and trade offers. A small mod could copy vanilla/Quark trade pools onto MCA villagers of the matching profession. That part is genuinely a weekend job.
2. **Farming AI is the hard part.** You'd have to reimplement Smarter Farmers' "plant any crop that's tagged as a seed" logic against MCA's own AI/brain, not patch the vanilla task. That means reading MCA's task system and writing a parallel task — effectively porting the feature, not bridging it.
3. **Liberty's Villagers is the hardest** and least worth it, because much of what it fixes (panic, sleeping, door use, mob avoidance) MCA already reimplements in its own way, so you'd be fighting MCA's design rather than extending it.

My honest recommendation: if you build anything, build **#1 only** — an MCA trade-enrichment mod. For farming, note that your colonies are the real food engine and MineColonies farmers are unaffected by all of this, so the practical loss from vanilla-villager farming tweaks not applying to MCA is small. That's also why disabling Quark's three villager tweaks costs you almost nothing — they'd only ever have affected vanilla villagers, and with MCA installed there are few or none.
