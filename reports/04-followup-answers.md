# Follow-up: answers, corrections, and generated files

Re-scanned after your update: **371 jars** (was 322). 56 added, 7 removed. You already dropped Quark Oddities, the duplicate Farmer's Delight, the DnT swamp hut, and NNP Fluidlogging (so `fluidlogged` is now the single fluidlogging mod — fine, that resolves item 0.7).

Generated files, none of them inside the minecraft folder:

| File | What it is |
|---|---|
| `proposed-configs/quark-common.toml` | Your Quark config with 16 modules turned off, each with a comment saying why |
| `proposed-datapacks/minecolonies-crop-freedom.zip` | 26 MineColonies crafter recipes rewritten onto other mods' crops |
| `proposed-datapacks/ExtraButchery-fixed.zip` | butchercraft entries stripped, Butchery entries kept |
| `proposed-datapacks/squid-calamari-dedup.zip` | Cultural Delights calamari line disabled so Rustic Delight's wins |

---

## 1. Corrections to the earlier reports

**The DnT splinters — you were right, I was wrong on two of three.** I compared the `.nbt` piece lists:

| Splinter | Pieces | Also in full pack | Verdict |
|---|---|---|---|
| Ancient City Overhaul | 130 | **0** | **Genuinely new content.** The full pack contains no ancient city at all. Keep. |
| Pillager Outpost Overhaul | 28 | **0** | **Genuinely new content.** Keep. |
| Woodland Mansion Replacement | 275 | **275** | Pure duplicate data. Its only real effect is the 100% replacement (see §7). |

The full pack's 97 structures are all `nova_structures:*` plus `minecraft:village_taiga`. It never touches ancient cities or outposts. My "all four duplicate the full pack" claim was wrong — it was only true for the mansion.

**Expanded Delight is not a subset of Extra Delight.** I claimed it was; the item lists disprove it. Details in §3.

**RemoveDupeCalamari's namespace was correct.** I said `culturalrecipes` was the wrong namespace so the pack never overrode anything. Wrong: Cultural Delights ships 81 of its recipe files under `data/culturalrecipes/`, so that *is* its recipe namespace and the override worked. The real problem is different (§10).

**Crash Assistant vs Not Enough Crashes** — accepted, they do different jobs (post-mortem crash report UI vs in-game return-to-title). Keep both. Removed from the drop list.

**Compat Delight** — accepted, breadth is the point. It also got better since your update: you added **Mutant Monsters**, which makes its mutant creeper/enderman/skeleton/snow-golem meat entries live. Keep.

---

## 2. Alex's Delight vs Alex's Mobs Continued Delight

**Different foods, barely any overlap.** Of 23 and 52 items, exactly **2 names are shared**: cooked catfish slice, and kangaroo stew.

- **Alex's Delight** is per-mob dishes: bison (raw/cooked/mince/patty/burger), bunfungus, moose rib, centipede leg, maggot salad, acacia blossom soup, gongylidia bruschetta, lobster pasta, kangaroo shank/pasta.
- **AMC Delight** is a *category* system: fowl cut, game cut, small game cut, predator cut, reptile cut — generic cuts that cover every Alex's Mobs animal — plus dishes (catfish gumbo, lobster bisque, orca platter, void worm terrine, sunbird roast, tiger curry, serpent soup, mimicream trifle, cosmic chowder).

They are not duplicates in content. They *do* both add cutting-board recipes for the same Alex's Mobs meats, so expect Polymorph prompts on some cuts. Keeping both is defensible if you want the specific dishes; if you want one, AMC Delight is the broader and more current one for the Continued fork you run. **This is a taste call, not a conflict.**

## 3. Expanded Delight vs Extra Delight

**Not a subset. Keep both.** Extra Delight has 1439 names, Expanded Delight 100, and only **28 names overlap**.

Expanded Delight's 72 unique entries include whole content lines Extra Delight does not have:
- **Crops**: asparagus, cranberries, sweet potato (plus wild variants, crates, seeds)
- **Goat dairy**: goat milk bucket/bottle, goat cheese wheel, goat cheese cask, honeyed goat cheese tart
- **Casks** (cheese/milk/goat cheese aging) — a mechanic Extra Delight lacks
- **Peanut butter** line and sandwiches, cinnamon apples, peperonata, snickerdoodle
- **Salt ore / deepslate salt ore / salt rock** — worldgen salt, where Extra Delight's salt is crafted
- crushing mallet, glass jar

Mechanically they are also different: Expanded Delight adds one recipe type (`expandeddelight:juicing`), Extra Delight adds **15** (oven, feast, mixing_bowl, chiller, mortar, vat, dough_shaping, melting_pot, drying_rack, juicer, evaporator, bottle_fluid, dynamic_jam, dynamic_toast, tool_on_block) and 96 stations.

The genuine overlap is worth unifying, though — it is the **entire 16-block cinnamon wood set**, plus salt, peanuts/wild peanuts, lemon, juicer, glow berry juice, sweet berry juice, cheese sandwich, grilled cheese, mac and cheese, chocolate cookie, sugar cookie. Two full cinnamon tree sets is the one real piece of bloat here. AU entries for these are in `02-almost-unified-candidates.md`.

## 4. Ender's Delight vs End's Delight

**Zero shared item names.** They are completely different mods that happen to share a theme.

- **Ender's Delight** (24 items) is *enderman/shulker flavoured cooking*: enderman sight, sight fragments, shulker mollusk/filet, stuffed shulker, endermite stew, mite crust, chorus pie/stew/juice, pearl pasta, ender paella, amberveil stew/curry, twisted cereal, uncanny cookies.
- **End's Delight** (55 items) is *dragon and processing focused*: raw/roasted dragon meat and cuts, dragon leg, dragon tooth, dragon egg shell, liquid dragon egg, non-hatchable dragon egg, **four knives** (dragon egg shell, dragon tooth, end stone, purpur), ender sausage, enderman gristle, shulker meat/slices/omelette, chorus wine/popsicle/milk tea/cookie/sauce, bubble tea, ender congee/noodle/rice.

**End's Delight is the more feature-complete one**: more than twice the items, it adds tools (the four knives go in `c:tools/knife`), a raw→cut→cooked processing chain, and drink lines. Ender's Delight is a smaller, flavour-focused set.

**Recommendation: keep both.** They genuinely do not collide — the only overlap in the whole pack is the chorus fruit storage block (`endersdelight:chorus_crate` vs `ends_delight:chorus_fruit_crate` vs `quark:chorus_fruit_block`), and Quark's is now disabled anyway. If you must cut one, cut Ender's Delight and you lose the least.

## 5. Quark fully covers these mods — drop them

Applying your rule. These are the cases where Quark's feature is a complete superset, so Quark wins:

| Drop this mod | Covered by | Evidence |
|---|---|---|
| **Armor Quick Swap** | Quark `management.Expanded Item Interactions` + `management.Quick Armor Swapping` | Armor Quick Swap does exactly two things: right-click armor in inventory to equip/exchange, and sneak-right-click an armor stand to swap the whole outfit. Quark's Expanded Item Interactions does the first ("right clicking an armor item in an inventory will swap it for whatever armor you're currently holding"), Quick Armor Swapping does the second. Complete coverage. |
| **Slot Cycler** | Quark `management.Hotbar Changer` | Slot Cycler's pitch is "nine hotbar slots aren't enough, cycle your whole inventory". Hotbar Changer shows your inventory rows above the hotbar and swaps the chosen row in with 1/2/3. Same feature. |

Cases where the **other mod is the superset**, so Quark's module goes off instead (already done in the generated config):

| Keep this mod | Quark module disabled | Why |
|---|---|---|
| Open Together | `tweaks.Double Door Opening` | Open Together covers doors **plus trapdoors and fence gates**, plus a toggle keybind. Running both makes the two toggle the same door and can cancel out. |
| Pro Placer | `tweaks.Reacharound Placing` | Pro Placer includes reach-around placement plus Bedrock-style fast placement. Two mods hooking the same placement is asking for trouble. |
| Fast Item Frames | `tweaks.Dyeable Item Frames` | Fast Item Frames does colour **plus** vanishing frames and the performance rewrite. |
| Not Enough Recipe Book | `tweaks.Automatic Recipe Unlock` | NERB removes the recipe book entirely, so unlocking all recipes into it is pointless. |

**One genuine either/or I am not deciding for you: Easy Shulker Boxes vs Quark `Expanded Item Interactions`.** Both hook right-click on a shulker box in your inventory. Easy Shulker Boxes is the superset (scroll contents, pick a specific item, drag across slots, single-item mode, ender chest support, and datapack-extensible to modded containers such as your Sophisticated backpacks). Quark's version also has a known conflict with Sophisticated Storage ([issue #187](https://github.com/P3pp3rF1y/SophisticatedStorage/issues/187)). But disabling Quark's module also removes the inventory armor-equip, which is what let you drop Armor Quick Swap. So:
- **Quark route**: keep Expanded Item Interactions on, drop Easy Shulker Boxes *and* Armor Quick Swap. (What the generated config assumes.)
- **ESB route**: keep Easy Shulker Boxes, turn Expanded Item Interactions off, keep Armor Quick Swap.

**Not a Quark case, but the same rule:** drop **Illagers Wear Armor**. Armored Foes covers illagers *and* drowned, wither skeletons, piglins, witches and villagers, and is data-driven through loot tables. Illagers Wear Armor is illagers only — a strict subset.

**Also pick one of Stylish Effects / Convenient Effects.** Both are status-effect display overhauls from the same author and will fight over the same HUD and menu widgets.

## 6. The Quark config

`proposed-configs/quark-common.toml` is your file with **16 modules** set to false, each preceded by a `#[modpack-audit] disabled:` comment. Copy it over when you're happy:

```bash
cp "/c/Users/eteil.000/Claude/modpack-audit/proposed-configs/quark-common.toml" "/c/Users/eteil.000/AppData/Roaming/PrismLauncher/instances/1.21.1/minecraft/config/quark-common.toml"
```

Disabled: `automation.Chute`, `automation.Pistons Move Tile Entities`, `building.Framed Glass`, `building.Gold Bars`, `building.Rope`, `building.Variant Chests`, `building.Compressed Blocks`, `tools.Ancient Tomes`, `tweaks.Double Door Opening`, `tweaks.Reacharound Placing`, `tweaks.Dyeable Item Frames`, `tweaks.Automatic Recipe Unlock`, `world.Spiral Spires`, `world.Chorus Vegetation`, `world.New Stone Types`, `mobs.Crabs`.

**Left ON deliberately:** `building.Vertical Slabs` (see §7), `management.Expanded Item Interactions` and `Quick Armor Swapping` (so you can drop Armor Quick Swap and Slot Cycler), and everything in `[client]`. The `[oddities]` section is now dead weight since you removed the jar — harmless, Quark ignores it.

## 7. Vertical slabs: Youkai's Homecoming does **not** cover Quark's

Counted directly: **Quark 122, Youkai 68, only 36 shared.** So leave Quark's module on.

- **Only Quark (86)**: every sandstone/red sandstone/soul sandstone variant, cut copper in all 5 weathering stages plus waxed, prismarine, dark prismarine, purpur, quartz and smooth quartz, smooth stone, all 16 shingle colours, the Quark stones (jasper, limestone, myalite, shale, permafrost) and their brick/polished forms, andesite/diorite/granite bricks, blackstone bricks, deepslate tile, dripstone, mud brick, dirt bricks, netherrack bricks, iron plate, midori, duskbound, thatch, and the Quark wood types.
- **Only Youkai (32)**: all 16 concrete colours, sikkui and cross-framed sikkui variants, darkstone and darkstone brick, ice brick, packed ice, snow and snow brick, hay, straw, purpur_block, deepslate_tiles.

Youkai's covers barely half, and its 32 unique ones are things Quark will never add. Since Youkai's has no per-feature toggle, the right move is: **keep both modules on and unify the 36 shared blocks in Almost Unified** with `quark` as the higher priority. I've added a note for these to the AU candidates file.

## 8. What the DnT mansion splinter actually overrides

Precisely three mechanisms:

1. **It neuters the vanilla mansion.** It rewrites `minecraft:worldgen/structure/mansion.json` so its `start_pool` is `nova_structures:vanilla_structure_remover` with `size: 1`. The vanilla mansion still "generates" but places nothing.
2. **It adds a parallel structure set.** New file `minecraft:worldgen/structure_set/illager_manor.json` pointing at `nova_structures:illager_manor`, with **salt 10387319, spacing 80, separation 20** — byte-identical placement parameters to `woodland_mansions`. Same salt means the manor lands on exactly the coordinates the vanilla mansion would have used.
3. **It rewrites `minecraft:worldgen/structure_set/woodland_mansions.json`** back to a single entry (`minecraft:mansion`, now a no-op).

Plus 13 illager_mansion loot tables, 14 template pools, 3 processor lists, 6 configured/placed features, and it edits the `pillager_outpost`, `ancient_city`, `on_woodland_explorer_maps` and `enchantment/treasure` tags.

**There is a real conflict here.** Both the splinter and the full pack define `minecraft:worldgen/structure_set/woodland_mansions.json`, and the full pack's version lists **both** `minecraft:mansion` and `nova_structures:illager_manor` at weight 1 each — that's where the 50/50 comes from. Whichever jar loads later wins, and NeoForge does not guarantee which:

- **Splinter wins** → vanilla mansion neutered, manor placed by the `illager_manor` set. 100% manor, correct.
- **Full pack wins** → the 50/50 set rolls `minecraft:mansion` half the time, which is *now a remover*, so **half of all mansion sites generate nothing at all**, while the splinter's separate `illager_manor` set still places a manor at the same salt. You get empty sites and possibly overlapping manors.

If you want the 100% replacement, don't leave it to load order. Add a one-file datapack that pins it, since datapacks beat mod-supplied data:

`data/minecraft/worldgen/structure_set/woodland_mansions.json`
```json
{ "placement": { "type": "minecraft:random_spread", "salt": 10387319, "separation": 20, "spacing": 80, "spread_type": "triangular" },
  "structures": [ { "structure": "nova_structures:illager_manor", "weight": 1 } ] }
```
and delete the splinter's `illager_manor` set duplication by leaving that file alone — or simply drop the splinter and use this one file, since all 275 of its pieces already ship in the full pack.

## 9. Pillager outposts and structure crowding

**Do you need to remove structures from the pool? Mostly no — but you have two new head-on conflicts, and they are not outposts.**

For outposts specifically, nothing actually collides. Only the DnT splinter defines `minecraft:pillager_outposts`; It Takes a Pillage registers its own `takesapillage:pillager_structure` set, TLM Spell has its own, and CTOV adds none. Different structure sets with different salts do not fight — they just compete for space, which is exactly what Better Sparse Structures is for. Your `bettersparsestructures-server.toml` is at defaults (`globalSpacingRadiusChunks = 10`, no overrides), so it is already enforcing a 10-chunk radius between structure starts. If outposts still feel crowded, raise it per-namespace rather than deleting content:

```toml
spacingRadiusOverrides = ["minecraft:pillager_outpost = 24", "takesapillage:* = 20", "idas:* = 16"]
```

**The two real conflicts your update introduced:**

1. **Integrated Stronghold vs YUNG's Better Strongholds.** Both add their own stronghold structure set (`integrated_stronghold:stronghold`, `betterstrongholds:stronghold`) and **both override `minecraft:tags/worldgen/structure/eye_of_ender_located.json`**. Only one wins that tag, and neither disables vanilla strongholds. Pick one.
2. **Integrated Villages vs ChoiceTheorem's Overhauled Village.** Both override `minecraft:tags/worldgen/structure/village.json`, and Integrated Villages adds `regular_villages` and `air_villages` sets on top. Pick one, or you get two village systems fighting for the same biomes.

Also new: **IDAS** adds 8 structure sets of its own. That is fine alongside the rest, it just increases the load Better Sparse Structures is managing.

## 10. Spice of Life: Latiao is the one you want

I read both configs rather than the store pages, and they do **opposite** things.

**Onion** (`config/solonion.json`) is purely a **reward** system. Its `"benefits"` array grants max health at diversity 3, strength at 5, regeneration at 7, speed at 10, armor toughness at 13, and more health at 25 and 31. Its `"detriments"` array is **empty**. It never stops you eating the same thing — it just doesn't reward you for it.

**Latiao** (`config/spiceoflifelatiao-server.toml`) is exactly the diminishing-returns mod you described. Its hunger formula is:

```
HUNGER = HUNGER_ORG*0.4*max(0.9^EATEN_SHORT, max(1-HUNGER_LEVEL/12,0))
       + HUNGER_ORG*0.4*max(0, 1-EATEN_LONG/max(16, 64-2*HUNGER_ORG-SATURATION_ORG))
       + HUNGER_ORG*0.2
```

`EATEN_SHORT` counts recent repeats (16-meal window), `EATEN_LONG` the long window (512). The first term decays geometrically at 0.9 per repeat, the second decays linearly to zero after roughly 16–64 repeats. Saturation decays the same way with no floor.

**So Latiao already does what you want, with one caveat:** that trailing `+ HUNGER_ORG*0.2` is a floor — a spammed food bottoms out at 20% of its hunger value rather than zero. To make repeated food genuinely unable to refill the bar, change the last term of `HUNGER` to `"+0"`:

```toml
HUNGER = ["HUNGER_ORG*0.4*max((0.9^EATEN_SHORT),max(1-HUNGER_LEVEL/12,0))", "+HUNGER_ORG*0.4*max(0,1-EATEN_LONG/max(16,64-2*HUNGER_ORG-SATURATION_ORG))", "+0"]
```

Note the `max(1-HUNGER_LEVEL/12,0)` term in the first bracket — it's a starvation mercy rule that restores value when you're below 12 hunger, so you can't be softlocked into starving. Keep that.

**Verdict:** keep **Latiao**. Onion is optional and not a duplicate — it's a buff system, not a penalty system — but both write to food hunger/saturation values, so running both means two mods modifying the same numbers. If you want only the mechanic you described, run Latiao alone.

## 11. MineColonies crops — `minecolonies-crop-freedom.zip`

**The full crop list is 13**: cabbage, chickpea, corn, durum wheat, eggplant, garlic, mint, onion, peas, rice, soybean, tomato, plus nether pepper.

You were right that the crafter recipes hardcode ids. Every one uses `"inputs": [{"id": "minecolonies:onion"}]` with no tag support. Only **26 of the 163** crafter recipes reference a crop, so this is a small job, and the datapack does all 26.

Mapping used:

| MineColonies | Replaced with | Recipes |
|---|---|---|
| `durum` | `minecraft:wheat` | 13 |
| `garlic` | `extradelight:garlic` | 14 |
| `onion` | `farmersdelight:onion` | 12 |
| `tomato` | `farmersdelight:tomato` | 6 |
| `eggplant` | `culturaldelights:eggplant` | 5 |
| `cabbage` | `farmersdelight:cabbage` | 3 |
| `nether_pepper` | `rusticdelight:bell_pepper_red` | 2 |
| `mint` | `extradelight:mint` | 1 |
| `rice`, `soybean`, `corn`, `peas` | mapped in the file but unused — no crafter recipe references them | 0 |
| `chickpea` | **left alone** — nothing in the pack provides a chickpea | 1 (`chef/pita_hummus`) |

Every replacement id was verified to exist in the current jars. Note `rusticdelight:bell_pepper_red` is the correct id (not `red_bell_pepper`), and Rustic Delight ships nine colours if you'd rather use a different one.

**There is one thing the datapack deliberately does not do**: MineColonies' `recipe-template` type *does* accept a tag, and generates one recipe per tag member with `[NS]:[PATH]` substitution. That would let a colony accept *any* onion rather than one specific onion. But a template can only vary one ingredient, so a recipe like borscht (onion + garlic + cabbage) can't be made fully flexible that way. Straight substitution is the honest fix, and it lines up with Almost Unified unifying `c:crops/onion` onto `farmersdelight:onion` anyway.

**To finish "completely replaced":** you now have **Item Obliterator** installed. Point it at the 13 `minecolonies:*` crop items and their seeds to remove them from the game entirely, once you've confirmed the colony recipes work. Do that *after* testing, not before — and check that no MineColonies building or research still hands them out.

## 12. `ExtraButchery-fixed.zip`

Your reasoning was right and the fix turned out clean. **104 of its 197 files are `c:` tag files** — it's a meat-cut tag bridge (`c:beef/cubed/raw`, `c:pork/ground/cooked`, and so on) that lists both butchercraft and Butchery items so Extra Delight's recipes can reference a generic cut.

So nothing needed deleting wholesale. I removed **110 butchercraft entries** from the tag value lists and kept every Butchery entry. Extra Delight's recipes keep working, and they resolve to Butchery meat only.

**16 tags ended up empty**, meaning Butchery has no equivalent cut and only butchercraft ever filled them. Those files are deleted:

`beef/oxtail/cooked`, `beef/scrap/cooked`, `brain/cooked`, `chicken/cubed/raw`, `chicken/cubed/cooked`, `chicken/ground/raw`, `chicken/ground/cooked`, `chicken/scrap/cooked`, `goat/scrap/cooked`, `mutton/scrap/cooked`, `pork/ground/raw`, `pork/ground/cooked`, `pork/scrap/cooked`, `rabbit/scrap/cooked`, `slime_balls`, `strings`.

If any of those matter to you, the closest Butchery stand-ins are `butchery:meat_scraps` for the `*/scrap/*` tags and `butchery:raw_beef_mince` / `raw_lamb_mince` for the ground ones — Butchery simply has no ground pork or ground chicken. Also still worth fixing separately: three tags this pack references but nothing defines (`c:salami_mix`, `c:cocoa/solids`, `c:spoons`) and one missing id (`extradelight:cocoa_butter_fluid`). And note **Extra Delight itself ships a `butchercraft:grinder` recipe**, so it has its own butchercraft support baked in — installing butchercraft would light both up.

## 13. `squid-calamari-dedup.zip`

Now that Rustic Delight is back, here's the real picture. Squid meat is claimed by **five** mods: Cultural Delights, Rustic Delight, Miner's Delight, Ocean's Delight and Butchery.

The key finding: **Cultural Delights overrides `minecraft:loot_table/entities/squid.json` and `glow_squid.json`**, replacing the vanilla ink sac drop with `culturaldelights:squid`. That is the conflict you were fixing. Rustic Delight instead gates its whole calamari line behind its own config flag, `squids_drop_calamari`, using `rusticdelight:config_boolean` conditions on every recipe.

So you have two clean routes:

- **Keep Rustic Delight's calamari** (the richer line — calamari, slices, cooked, fried, roll, soup, crate): use the generated datapack. It disables Cultural Delights' 12 calamari/squid recipes with proper `neoforge:conditions: [{"type":"neoforge:false"}]` and restores the vanilla squid and glow squid loot tables.
- **Keep Cultural Delights' squid**: skip the datapack, launch once, then set `squids_drop_calamari = false` in Rustic Delight's config.

Two notes on your original pack: it disabled recipes by writing `{}`, which in 1.21 is a parse error rather than a clean removal — it works, but it spams the log. The generated version uses the proper condition. And Cultural Delights' own config file is untouched MDK boilerplate (`logDirtBlock`, `magicNumber = 42`), so there is no config route on that side — the datapack is the only option.

## 14. Other things your update changed

- **`recipes-plus-1.3.5.jar` is now installed.** That settles it: delete the `recipes_plus.zip` datapack, it's the same content as a mod.
- **Resource Pack Overrides is now installed**, so `Stellar 3D-2.2.0.zip.rpo` is finally doing something. It will pin that pack active. Keep the `.rpo` next to the zip.
- **`DnT-woodland-mansion-replacement-v1.2 [NeoForge] (1).jar`** — the ` (1)` is a browser duplicate-download suffix. It's the only mansion jar so it loads fine, but rename it before it confuses you later.
- **Mutant Monsters** validates a chunk of Compat Delight, as noted in §1.
- Reconfirmed as still valid from report 01: the Friends&Foes vs Illager Invasion **Illusioner** double (`enableIllusioner` and `replaceVanillaIllusioner` are still `true`), the four-crab situation (now that Quark's crabs are off, it's F&F + Crabber's + Youkai), and Quark Matrix Enchanting vs Apotheosis (moot — the Oddities jar is gone).

---

## What I'd do next, in order

1. Copy the Quark config; drop **Armor Quick Swap**, **Slot Cycler**, **Illagers Wear Armor**; pick one of Stylish/Convenient Effects.
2. Decide the Easy Shulker Boxes vs Quark Expanded Item Interactions question (§5).
3. Pick one stronghold mod and one village mod (§9). These will corrupt worldgen expectations if left as-is.
4. Pin the mansion structure set with the one-file datapack, or drop the mansion splinter (§8).
5. Install `minecolonies-crop-freedom`, play a colony through a few meals, then use Item Obliterator on the MineColonies crops.
6. Install `ExtraButchery-fixed` and `squid-calamari-dedup`; delete the originals plus `recipes_plus.zip`.
7. Set Latiao's hunger floor to `"+0"` and decide whether Onion stays.

### Sources
- [Armor Quick Swap (GitHub)](https://github.com/Fuzss/armor-quick-swap) · [Quark Expanded Item Interactions / shulker boxes](https://ftb.fandom.com/wiki/Quark_Tweaks) · [Quark Hotbar Changer](https://ftb.fandom.com/wiki/Management_Tweaks)
- [Easy Shulker Boxes](https://www.curseforge.com/minecraft/mc-mods/easy-shulker-boxes) · [Quark/Sophisticated Storage conflict](https://github.com/P3pp3rF1y/SophisticatedStorage/issues/187)
- [Armored Foes](https://www.curseforge.com/minecraft/mc-mods/armored-foes) · [Illagers Wear Armor](https://github.com/seymourimadeit/IllagersWearArmor)
- [SpiceOfLife: Latiao](https://modrinth.com/mod/spiceoflifelatiao) · [Spice of Life Onion](https://www.curseforge.com/minecraft/mc-mods/spice-of-life-onion)
- [D&T Pillager Outpost Overhaul](https://modrinth.com/datapack/dungeons-and-taverns-pillager-outpost-overhaul) · [D&T Ancient City Overhaul](https://modrinth.com/project/DNuNq5bb)
- [Extra Delight](https://www.curseforge.com/minecraft/mc-mods/extradelight) · [Expanded Delight](https://modrinth.com/project/e9V6wFcR) · [End's Delight](https://modrinth.com/mod/ends-delight) · [Ender's Delight](https://www.curseforge.com/minecraft/mc-mods/enders-delight)
