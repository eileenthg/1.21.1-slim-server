# 23: Recipes that erase item variants (29 Sep)

Started from two reports: any cheese crafts into Extra Delight's cheese block and comes back as Extra Delight cheese, and 9 tentacles used to offer both squid and glow squid barrels (already fixed). Both are the same bug shape, so the whole pack was scanned instead of fixing them one at a time.

Tooling: `tools/recipe-audit/` (see its README). It reads every mod jar and datapack the way the game does — datapack overrides, `neoforge:false`, AlmostUnified's merge map (placeholders, ignored tags/items, stone variants), `neoforge:difference` ingredients — and ignores items no mod registers. 26,798 recipes, 10,113 crafting, 623 cutting.

## Three problem shapes

**A. Lossy round trip** — a block accepts several variants but gives one back. **Fixed** (datapack `zzz-recipe-identity.zip`, overrides the mod recipe so only the item you get back is accepted):

| Block | Was accepting | Gave back | Now accepts |
|---|---|---|---|
| Extra Delight cheese block | 6 cheeses: B&C flaxen/goat/scarlet wedges, feta, ED cheese slice, ED cheese | ED cheese | ED cheese only |
| Extra Delight egg crate | 16 eggs incl. turtle, **dragon**, parrot, emu, strider, brown/blue | plain egg | plain egg only |
| Coffee bean sack | roasted beans **and** green (unroasted) coffee | roasted beans | roasted beans only |
| Coffee cherry crate | beans, green coffee, Youkai coffee berries | Youkai berries | berries only |
| Peanut in shell sack | shelled peanuts and in-shell | in-shell | in-shell only |

The coffee ones were also a free step backwards through the roasting chain (green coffee → sack → roasted beans; beans → crate → raw berries).

**Left alone:** coral blocks (coral or coral fan → block, cut → Crabber's fragments; neither is recoverable either way, so nothing is lost by the exchange) and Eternal Tales' dredert stem top/bottom → log.

**B. Same input, different results** — 15 cases where a crop compacted into competing containers (the tentacle shape). **Merged** into one item each, added to the item merge (`decisions.py` + live AlmostUnified config):

| Kept | Removed |
|---|---|
| Farmer's Delight beetroot / carrot / potato crates | Quark's three crates |
| Extra Delight corn crate | Corn Delight corn crate |
| Ender's Delight chorus crate | End's Delight chorus fruit crate |
| Frights Delight flesh crate | Eternal Tales flesh block |
| Bumblezone pile of pollen | Compat Delight pollen puff crate |
| Extra Delight coffee bean sack | Farmer's Respite coffee beans sack |

**Bookshelves:** Recipes Plus overrides the *vanilla* bookshelf recipe to accept any planks, which collided with Quark's variant bookshelves (one per wood, and the feature is on). Deleting it would have removed bookshelf crafting for every other mod's wood, so instead the recipe now subtracts the 13 woods that have their own bookshelf (`neoforge:difference` against a small tag in the datapack). Oak stays, because Quark's oak recipe yields the vanilla bookshelf anyway.

**C. Merges that erased a mod's own variants** — found by checking each unify tag for two items from the same mod. **Fixed** in `build.py` (`REMOVE_UNIFY_TAGS` / `IGNORE_ITEMS` / stone variants) and in the live config:

- **Bell peppers — corrected.** I first read the config as merging all nine colours into one. Checking `logs/almostunified/almostunified.log` (the ground truth: it lists every tag AU actually unified) shows **no entry for `c:crops/bell_pepper`** — AlmostUnified leaves a tag alone when every member comes from one mod, and Rustic Delight is the only pepper mod. So nothing was being erased, and the "nine crates from one pepper" conflict my scanner reported was an artifact of the same mistake. The tag is still removed from the unify list as a precaution: a second pepper mod would make it collapse the colours. `scan_recipes.py` derives its merge map from the config, so treat its merges as a hypothesis and confirm against that log.
- **Nether gold ore** was genuinely being merged into plain gold ore (`#c:ores/gold` hid it alongside the modded ones); now an ignored item.
- Deeper Darker's gloomslate/sculk-stone ores were folding into the vanilla ores (confirmed in the log: `#c:ores/gold` hid both): added both as stone variants, the way deepslate already is.
- Eternal Tales karvat/rayana gold ore and Butchery's repaired iron golem head are now ignored items (both confirmed merged in the log). Amber sand ore is precautionary, same case as the peppers.
- Left as intended: meatloaf/meatloaf plate and the spider-leg merges are deliberate entries in `decisions.py`; cave and normal spider legs merging is the same call.

## Still open (your call)
- **Chorus fruit** still offers the crate or Quark's decorative chorus fruit block. Different purposes, so I left the picker.
- **MCTradePost wish cow vs wish sheep** share coin + lead + wheat; that looks like the mod's own design.
- **Calcite stairs/walls (Quark vs Spectrum)** and **gold bars (Quark vs Supplementaries)** are the same block from two mods — merge candidates if you want them gone.
- **Wildcard recipes I did not touch**, because disabling them removes real function: Cold Sweat fur armour (any thick fur) vs Eternal Tales' bear/coonskin hats, Eternal Tales' furnace-from-any-stone vs Quark's blackstone furnace, Snowy Spirit's any-leaves wreath vs Extra Delight's cinnamon wreath, Recipes Plus' stone sword vs Eternal Tales' rajiit sword, Quark's easy sticks, Festive Delight's rolling pin.

## State
- Verified by re-running the scan: the 5 lossy round trips and 12 of the 15 duplicate conflicts are gone; pepper colours and ore variants no longer collapse.
- **Not yet tested in game.** The Chunky pre-generation server was running, so no test launch was made (test boots caused the 26 Sep lag). Worth a look on next load: the recipes appear in JEI, and no "Parsing error loading recipe" for `minecraft:bookshelf` or the five Extra Delight ones.
- Merged items already in chests stay as they are; AlmostUnified only rewrites recipes and loot.
- Backups: `proposed-configs/almostunified-materials.BEFORE-VARIANT-FIX.json`, `almostunified-tags.BEFORE-DUP-MERGE.json`.
