# Heads, geodes, workstation merges and the new mods (16 Sep, part 2)

Built, tested on the server (24,837 recipes, no errors, MineColonies unchanged) and installed.
Backups: `proposed-configs/zz-unified-items.zip.BEFORE-HEADS`, `proposed-configs/almostunified-BEFORE-HEADS/`.

## 1. Geodes

All three overworld geodes now sit at the same rarity, **1 in 32 chunks**, overriding:

| Geode | Was | Now |
|---|---|---|
| Amethyst (vanilla) | 1 in 24 | 1 in 32 |
| Citrine | 1 in 50 | 1 in 32 |
| Topaz | 1 in 6 | 1 in 32 |

Y ranges are untouched (amethyst -58…30, citrine 32…55, topaz 82…320).

On citrine near water: there is no biome or water condition in its placement — `biomes: #c:is_overworld`, `step: underground_structures`, height 32–55. That band is exactly where river and lake beds cut into stone, so geodes there end up exposed and visible. Tectonic's underground rivers and lava tunnels do the same thing more often, which is why they seemed to move.

## 2. Mob heads — merged

**49 groups**, one `c:heads/<mob>` tag each, and **87 Spectrum recipes** (pedestal idols, fusion shrine, spirit instiller) now take the tag instead of a fixed head item. AlmostUnified makes the rest interchangeable.

Priority used, as asked:
1. **Supplementaries** — `enderman` and `spider`. Those are its only two head items, and its "Mob Head Shaders" tweak is what applies the screen effect when worn.
2. **Butchery variant heads** — colour and breed variants win, since those drop from butchering: `blue_axolotl`, `cyan_axolotl`, `gold_axolotl`, `brown_mooshroom`, `red_mooshroom`.
3. **Spectrum** — the other 42 plain mobs (cow, pig, sheep, chicken, wolf, warden …).
4. **Butchery** — fallback.

Butchery's own variant heads with no Spectrum counterpart (cat colours, horse colours, llama colours, and the rest of its 76) are untouched, and its heads still work everywhere thanks to the tags.

**Your old datapack:** `Resource packs and data packs list/datapacks/ButcheryCompats.zip` — it is *not* about Spectrum or Supplementaries heads. It converts Butchery ↔ **Pastel** heads (elder guardian, guardian, pufferfish, strider), adds squid/glow squid loot tables, a guardian loot modifier, and ender dragon butchering tables. Pastel is not installed, so those recipes are dormant. It is also not in the instance `datapacks/` folder. Say the word if you want the Pastel entries retargeted to Spectrum and the pack added.

**Cookie tag kept.** Your additions live in `config/almostunified/tags.json` → `custom_tags`: `c:foods/cookie` (56 items) and `extradelight:candy_bowl_valid` (45). The build reads that file and writes it back, so both survived — verified in the server log. Supplementaries' cookie jar reads `#c:foods/cookie`, so the jar keeps accepting all of them.

## 3. Workstations

**Merged (23 recipes ported):**
- **Mortar and pestle** → Extra Delight's mortar. Butchery's 7 pestle recipes (wishbone, bird foot, icey bone, sculk bone, hoof, wither bone) became `extradelight:mortar` recipes at 8 grinds. Butchery's pestle and mortar is no longer craftable, and an existing one converts to `extradelight:mortar_stone`.
- **Cutting** → Farmer's Delight cutting board. Eternal Tales' 6 single-input cutting-table recipes became board recipes (knife). Its 10 multi-input armour recipes cannot be a board recipe — a board takes one item — so they became shapeless crafting recipes that still require a knife. The cutting table is no longer craftable and converts to a cutting board.

**No action needed — the mod isn't installed.** You were right to check. These recipe types exist only as dormant compat files shipped by other mods:
- Mekanism crushing/enriching/sawing — shipped by Deeper Darker and Spectrum.
- Immersive Engineering crusher, squeezer, fermenter, metal press, cloche — Farmer's Delight, Delightful, Corn Delight, Ramadan Delight, MineColonies Compatibility.
- Thermal press/pulverizer/sawmill/etc. and Forestry squeezer and EnderIO sag milling — Delightful.
- NeepMeat grinding/advanced crushing — Spectrum.
- ButcherCraft grinder — one recipe from Extra Delight.
So: **pressing** is Create only, **meat grinding** is Butchery only, nothing to merge.

**Crushing:** left alone as you asked — Spectrum anvil crushing early, Create crushing later.

**Kettle and fermenting: blocked, need a decision.** Both are fluid systems that do not line up:
- Farmer's Respite's kettle brews **fluid → fluid** (`base: 1000 mB water` → `result: 1000 mB farmersrespite:black_tea`). Youkai's kettle makes **items** (`container: glass bottle` → `youkaishomecoming:black_tea`). Porting Youkai's 21 recipes needs a registered fluid per tea, which doesn't exist. Options: (a) keep both kettles, (b) drop Youkai's 21 teas and use Farmer's Respite's equivalents where they exist, (c) rewrite Youkai's teas as Farmer's Respite fluid recipes only for the teas Farmer's Respite already has a fluid for.
- Brewin' and Chewin's keg takes a **base fluid ingredient** plus up to 4 items; Youkai's `simple_fermentation` goes **fluid → fluid** using its own grape juice fluids (burgundy, champagne …). A port would need those fluids accepted as a keg base, which I would have to verify in the keg's code first.

## 4. Farmer's Respite is a new duplicate source

It was installed after the merge was built, so the merge doesn't know about it. It duplicates:

- **Teas:** black, green and yellow tea and their leaves, plus tea seeds and tea bush — against Youkai's Homecoming (and Eternal Tales' tea bush/seeds).
- **Coffee:** coffee, coffee beans, coffee berries, coffee bush, coffee cake and its slice — against Extra Delight, Rustic Delight and Youkai.
- **Drinks:** apple cider (Farmer's Delight, Extra Delight fluid), hot cocoa (Farmer's Delight), melon juice (Farmer's Delight, Extra Delight, tanca).

That's roughly 20 more merge groups. Worth a re-run of the merge with a fresh baseline dump.

## 5. Ecologics — heavy overlap

Entities: `penguin`, `squirrel`, `coconut_crab`, **`camel`**, plus its own boats.

- **Camel:** Ecologics adds its own camel *and* a spawn rule for it, next to the vanilla 1.21 camel. Two different camels.
- **Azalea wood:** a full 16-block wood set that duplicates **Quark's azalea wood**, item for item.
- **Crabs:** the pack already has `crabbersdelight:crab`, `friendsandfoes:crab`, `quark:crab` and `youkaishomecoming:crab`. Coconut crab makes five.
- **Items:** coconut (Crabber's Delight), crab claw (Crabber's Delight, Friends and Foes), crab meat (Youkai), ice bricks and snow bricks (Youkai, Eternal Tales), thin ice (Immersive Weathering).
- **Worldgen:** it *removes* vanilla features and re-adds its own — `remove_trees_plains`, `remove_rooted_azalea_tree`, `remove_classic_vines_cave_feature`. Removing plains trees is a real risk next to CliffTree, which replaces 71 vanilla biome files to place its own trees.

**Recommendation: drop Ecologics**, unless penguins and squirrels matter to you. If it stays, it needs a datapack that neutralises `ecologics:spawn_camel` and `remove_trees_plains`, plus about 20 more merge groups for the azalea, crab and brick items.

## 6. Other new mods

- **tanfluids vs tanca** — both add the full Tough As Nails fluid set: purified water, dirty water, and every juice, each with block and bucket. The juice and hot chocolate recipes here use **tanca's** fluids. Drop one; keep tanca.
- **backslots vs Curios** — Curios already provides a back slot. Two back-slot systems can double up; worth checking in game whether you see two.
- **reinforced_construction vs reinforced_deepslate vs horizontalreinforceddeepslate** — three mods in the same space; reinforced_construction's obsidian bricks and obsidian pillar also duplicate Eternal Tales'.
- **cuttingdelight** — it adds its own `cutting_board`, a third one next to Farmer's Delight's and Cooking for Blockheads'. Worth a look now that cutting is consolidated.
- **Longer Chat History is back in the pack**, so the 100-line chat log limit is raised again — the Even Better Chat note from the last report no longer applies.
