# Items to merge with Almost Unified

Derived from `data/tags.json` (shared `c:` tags across jars) and `data/mods.json` (identical display names across mods). Only mods actually in the pack are listed. Groups already covered by your current AU `materials.json` tag list (`c:ingots/*`, `c:nuggets/*`, `c:raw_materials/*`, `c:ores/*`, `c:storage_blocks/*`, `c:dusts/*`, `c:plates/*`) are marked **(auto)**; everything else needs the tag added to `unification/*.json` `tags`, and items without a shared tag need a `custom_tags` entry in `tags.json`.

Current `mod_priorities` is `["minecraft","create"]`. Recommended: `["minecraft","farmersdelight","create","minecolonies","extradelight","supplementaries","quark","mythicmetals","youkaishomecoming","butchery","spectrum","eternal_tales"]` so MineColonies crops keep working in colony farms and FD stays the food baseline.

## A. Metals and materials

| Material | Items | Tag | Note |
|---|---|---|---|
| Copper nugget | `create:copper_nugget`, `mythicmetals:copper_nugget`, `mctradepost:copper_nugget` | `c:nuggets/copper` (auto) | |
| Silver | `mythicmetals:silver_ingot/nugget/raw_silver/silver_ore`, `eternal_tales:silver_ingot/nugget/raw_silver/silver_ore` | `c:ingots/silver` etc. (auto) | ET does not tag its ores; add custom tags if ET stays |
| Tin | `mythicmetals:tin_ingot/nugget/raw_tin`, `eternal_tales:tin_ingot/nugget/raw_tin` | (auto) | same |
| Adamantite | `mythicmetals:adamantite_ingot/nugget`, `eternal_tales:adamantite_ingot/nugget` | (auto) | armour sets duplicated too; cannot unify armour, drop one |
| Brass / zinc / blaze gold / sturdy alloy | single source each (Create, Blazing Hot) | (auto) | nothing to do |
| Iron plate | `create:iron_sheet`, `quark:iron_plate` (Quark Obsidian Plate module makes it), `eternal_tales:iron_plate` | `c:plates/iron` | add Quark/ET to tag via `custom_tags` |
| Copper carrot | `blazinghot:copper_carrot`, `minersdelight:copper_carrot` | none | custom tag `c:foods/copper_carrot` |
| Soul dust | `blazinghot:soul_dust`, `deeperdarker:soul_dust` | none | custom `c:dusts/soul` |
| Ash | `supplementaries:ash`, `spectrum:ash`, `eternal_tales:ash` | `c:dusts` (only partial) | custom `c:dusts/ash` |
| Sulfur | `butchery:sulfur`, `eternal_tales:sulfur` | none | custom `c:dusts/sulfur` |
| Salt | `extradelight:salt`, `expandeddelight:salt`, `butchery:salt`, `compatdelight:salt`, `eternal_tales:salt` | `c:salt`, `c:dusts/salt` | add both tags to `tags` |
| Bone shard | `frightsdelight:bone_shard`, `eternal_tales:bone_shard` | none | custom |
| Rock crystal | `spectrum:rock_crystal`, `touhou_little_maid_spell:rock_crystal` | none | custom |
| Blood / bottle of blood | `butchery:blood`, `eternal_tales:blood`; `butchery:bottle_of_blood`, `eternal_tales:bottle_of_blood` | none | custom |
| Lava sponge / wet lava sponge | `netherdepthsupgrade`, `spectrum` | none | custom (blocks; `world_gen_unification` off, fine) |

## B. Crops, seeds and produce

| Crop | Items | Tag |
|---|---|---|
| Corn | `extradelight:corn_on_cob`, `corn_delight:corn`, `culturaldelights:corn_cob`, `hauntedharvest:corn`, `minecolonies:corn` | `c:crops/corn`, `c:foods/corn`, `c:vegetables/corn` |
| Corn seeds / kernels | `corn_delight:corn_seeds`, `extradelight:corn_seeds`, `hauntedharvest:kernels` | `c:seeds/corn` |
| Cornmeal | `extradelight:cornmeal`, `minecolonies:cornmeal` | `c:cornmeal` |
| Cucumber | `extradelight:cucumber`, `culturaldelights:cucumber`, `youkaishomecoming:cucumber` | `c:crops/cucumber` (Youkai's not tagged; custom) |
| Cucumber seeds | `extradelight:cucumber_seed`, `culturaldelights:cucumber_seeds`, `youkaishomecoming:cucumber_seeds` | custom `c:seeds/cucumber` |
| Ginger | `extradelight:ginger`, `snowyspirit:ginger`, `culturaldelights:ginger` | `c:crops/ginger` |
| Garlic | `extradelight:garlic`, `minecolonies:garlic`, `compatdelight:garlic` | `c:crops/garlic` |
| Onion | `farmersdelight:onion`, `minecolonies:onion` | `c:crops/onion` |
| Tomato | `farmersdelight:tomato`, `minecolonies:tomato` | `c:crops/tomato` |
| Cabbage | `farmersdelight:cabbage`, `minecolonies:cabbage` | `c:crops/cabbage` |
| Rice | `farmersdelight:rice`, `minecolonies:rice`, (`mynethersdelight:ghasmati` is a nether variant, exclude) | `c:crops/rice`, `c:rice` |
| Soybean | `extradelight:soybeans`, `minecolonies:soybean`, `youkaishomecoming:soybean` | `c:crops/soybean` |
| Eggplant | `culturaldelights:eggplant`, `minecolonies:eggplant`, `compatdelight:eggplant` | `c:crops/eggplant` |
| Peas | `minecolonies:peas`, `compatdelight:peas` | `c:crops/peas` |
| Chili pepper | `extradelight:chili`, `expandeddelight:chili_pepper`, `eternal_tales:chili_pepper` | `c:crops/chili_pepper` |
| Peanut | `extradelight:peanuts`, `expandeddelight:peanut` | `c:crops/peanut` |
| Mint | `extradelight:mint`, `minecolonies:mint` | custom `c:crops/mint` |
| Lemon / lime / orange | `extradelight:lemon`, `expandeddelight:lemon`; lime & orange saplings/leaves also exist in Spectrum | `c:fruits/lemon` custom; skip tree blocks |
| Tea leaves / tea seeds | `youkaishomecoming:tea_leaves`, `eternal_tales:tea_leaves`, `spectrum` petals | `c:tea_leaves` (exists) |
| Coffee beans | `extradelight:coffee_beans`, `youkaishomecoming:coffee_beans` | custom |
| Cinnamon (stick + ground) | `extradelight:cinnamon_stick`/`ground_cinnamon`, `expandeddelight:cinnamon_stick`/`cinnamon` | `c:dusts/cinnamon` |
| Pineapple | single source | none |
| Straw bale / hay slab | `farmersdelight:straw_bale`, `youkaishomecoming:straw_bale` | custom |

## C. Dairy, condiments, intermediates

| Item | Sources | Tag |
|---|---|---|
| Butter | `extradelight:butter`, `minecolonies:butter`, `youkaishomecoming:butter` | `c:butter` |
| Cheese | `extradelight:cheese`, `minecolonies:cheddar_cheese`, `expandeddelight:cheese_slice`, `compatdelight:cheese_slice` | `c:cheese`, `c:cheeses`, `c:foods/cheese` (feta is distinct; leave out) |
| Soy milk | `extradelight:soy_milk`, `createfillthecolonies:soy_milk`, `minecolonies:large_soy_milk_bottle` | `c:soy_milks` (bottle sizes differ; unify only the two bottles) |
| Soy sauce | `extradelight:soy_sauce_item`, `minecolonies:soysauce` | `c:condiments/soysauce`, `c:soy_sauces` |
| Tofu | `minecolonies:tofu`, `youkaishomecoming:tofu` | custom |
| Flour | `create:wheat_flour`, `extradelight:flour` | `c:flours/wheat` |
| Dough | `create:dough`, `farmersdelight:wheat_dough`, `minecolonies:bread_dough` | `c:foods/dough/wheat`, `c:dough` |
| Milk bottle | `farmersdelight:milk_bottle`, `expandeddelight:goat_milk_bottle` | `c:foods/milk` (leave buckets alone) |
| Animal fat | `butchery:animal_fat`, `amcdelight:animal_fat` | custom |
| Jerky | `brewinandchewin:jerky`, `extradelight:jerky` | custom |
| Kimchi | `brewinandchewin:kimchi`, `extradelight:kimchi_item`, `minecolonies:kimchi`, `spectrum:kimchi` | custom |
| Fish bones | `aquaculture:fish_bones`, `alexsmobs:fish_bones`, `crabbersdelight:fish_bones` | custom |
| Fish oil | `alexsmobs:fish_oil`, `eternal_tales:fish_oil` | custom |
| Worm | `aquaculture:worm`, `crabbersdelight:worm` | custom |
| Fishing line | `aquaculture:fishing_line`, `crabbersdelight:fishing_line` | custom |
| Message in a bottle | `aquaculture`, `crabbersdelight` | custom |
| Bat wing | `butchery:bat_wing`, `minersdelight:bat_wing` | custom |
| Calamari | `butchery:calamari`, `amcdelight:calamari` | custom |
| Tentacles | `butchery`, `minersdelight`, `oceansdelight` | custom |
| Squid / glow squid (item) | `culturaldelights:squid`/`glow_squid`, `minersdelight:squid`/`glow_squid` | custom |
| Raw dragon meat | `butchery:raw_dragon_meat`, `ends_delight:raw_dragon_meat` | custom |
| Crab (item) | `crabbersdelight:crab`, `youkaishomecoming:crab`; crab claw `crabbersdelight`, `friendsandfoes` | custom |
| Shrimp | `crabbersdelight:shrimp`, `eternal_tales:shrimp` | custom |
| Catfish / goldfish | `aquaculture`, `alexsmobs`, `eternal_tales` | custom (`c:foods/raw_fish` too broad) |
| Cooked catfish slice | `alexsdelight`, `amcdelight` | goes away when one is dropped |
| Cooked fish fillet | `aquaculture:fish_fillet_cooked`, `amcdelight:cooked_fish_fillet` | custom |
| Beef/pork/mutton cuts | Butchery tags its 20 cuts as `c:raw_beef` etc. alongside `minecraft:beef` | **do not unify** (Butchery cuts are intentionally different foods) |

## D. Finished dishes with identical names

Unify only if you want one recipe result; otherwise leave (different nutrition values).

apple pie (FD, MineColonies), pumpkin pie + slice (Extra Delight, FD), cooked rice, fried rice, borscht (Extra, MineColonies, Youkai), congee, corn chowder, potato soup (Extra, Large Meals, MineColonies, Youkai), tomato pasta, rice ball, rice pudding, chicken curry, spicy curry, stuffed pepper, stuffed squid, hot wings, grilled cheese, cheese sandwich, mac and cheese, chocolate cookie, sugar cookie, gingerbread cookie (Extra, Festive, Snowy Spirit), eggnog, mead, miso soup, affogato, candy apple, candy cane, fruit salad, fried fish, fish sandwich, meatloaf, quiche, popcorn/caramel popcorn/creamed corn/cornbread/tortilla, ice cream (Extra, Trade Post), sweet roll (Create, Expanded), sweet berry custard, glow berry / sweet berry juice, kangaroo stew, hot cocoa & apple cider & melon juice (FD item vs Extra Delight fluid block: not the same thing, skip).

## E. Blocks and decoration (AU handles items; these are mostly "pick one mod" cases)

| Block | Sources |
|---|---|
| Vertical slabs — **36 shared of Quark's 122 / Youkai's 68** | Quark, Youkai's Homecoming. Keep both modules on (Youkai covers only half); unify the 36 shared blocks with `quark` as priority |
| Stone pillars: andesite/diorite/granite/calcite/dripstone/tuff/limestone | Create, Quark |
| Limestone (+bricks/slab/stairs/wall) | Create, Quark, Eternal Tales |
| Calcite bricks/slab/stairs/wall, polished calcite | Quark, Spectrum, Eternal Tales |
| Tuff/dripstone bricks, slabs, stairs, walls | Quark, Eternal Tales |
| Diorite bricks | Quark, Butchery |
| Framed glass + pane | Create, Quark |
| Gold bars | Quark, Supplementaries |
| Rope | Quark, Supplementaries, FD |
| Stone lamp, blackstone lamp, gold lantern | Quark/Supplementaries, ET/Supplementaries, ET/Supplementaries/Supp² |
| Thatch (+stairs/slab) | Quark, Trade Post |
| Produce crates (apple, carrot, potato, beetroot, golden apple, golden carrot, cocoa, chorus fruit, corn, cucumber, ginger, chili) | Quark Compressed Blocks, FD, Extra Delight, Ender's/End's Delight, Snowy Spirit, Haunted Harvest, Expanded Delight, Corn Delight (`c:storage_blocks/{crop}` tags exist for carrot/potato/beetroot/chorus_fruit) |
| Cinnamon wood set (16 blocks) | Extra Delight, Expanded Delight — the single biggest real overlap between those two mods |
| Marble set | Cluttered, Eternal Tales, YUNG's Cave Biomes (different textures) |
| Mob heads (30+) | Butchery, Spectrum (+Spectrum Compat), Supplementaries (enderman, spider) |
| Cutting board | FD, Cooking for Blockheads, Cutting Delight |
| Oven | Extra Delight, Cooking for Blockheads |
| Jar | Supplementaries, Extra Delight, Butchery |
| Basin | Create, Butchery |
| Drying rack | Extra Delight, Youkai's |
| Book pile | Supplementaries, Youkai's |
| Hanging flower pot | Amendments, Cluttered |
| Sacks (16 colours) | Supplementaries Squared, Skillcloaks |
| Presents | Supplementaries, Spectrum |
| Grave | Gravestone, MineColonies (citizen), Eternal Tales |
| Waystone | Waystones, Eternal Tales |
| Weather detector | Quark, Spectrum |
| Speedometer | Create, Supplementaries, Eternal Tales |
| Wrench | Create, Supplementaries (both in `c:tools/wrench`) |
| Whisk | Create, Extra Delight |

## F. Draft AU config

See `almostunified-proposed/` in this folder:
- `unification/foods.json`: a second unification group for the crop/dairy/condiment tags above.
- `tags.json`: `custom_tags` giving shared tags to the untagged items (Youkai cucumber, ET metals, animal fat, tentacles, etc.).

Nothing has been copied into the Minecraft config folder; review, then copy into `config/almostunified/` yourself.
