# Maid Chef (maidchef) — P1 to P4 + chat

Adds one Touhou Little Maid task, **Chef**. She looks around her kitchen, picks the most valuable dish she can
make from what's in the ingredient chest, makes a set amount of it, puts it in the output chest, then moves on
to a different dish.

Design and phase plan: `../30-maid-chef-and-task-list.md`. Done: P1 (single dishes), P2 (multi-step chains,
serving feasts) and P3 (ingredient requests + shared block list). Also done: P4 "prepare X" mode.

## Talking to her (TLM AI chat, e.g. Player2)
Look at her and use TLM's chat. Chef registers with TLM's AI system:
- **Every message** carries a short "cooking" block:
  - her mode (best food, or the dish she's preparing)
  - what she's cooking right now, step by step
  - what she cooked recently
  - what she'd like you to bring
- **Tool `chef_kitchen_report`.** The LLM calls it for "what can you cook?", "why aren't you cooking?" or "what's
  missing?". It returns the Status-tab breakdown, without coordinates.
- **Tool `chef_prepare`.**
  - "Could you make fried eggs?" sets Prepare mode; the dish is matched by name or id.
  - "Cook whatever's best" switches back.
  - It reports whether she can make the dish right now, and if not, why: what's missing, or which station is busy.
- **Threading.** Tools run their work on the server thread. TLM may call tools from its HTTP thread.
- **Player2.** Checked on 1 Oct: the app answers at 127.0.0.1:4315, and TLM's `player2` site is enabled.
- **Test.** The self-test chats with a real maid through Player2. When asked to make fried eggs she switched via
  `chef_prepare` and replied "The smoker's busy right now. Hang on, I'll fry the eggs once it's free."

## P4: Prepare X
- **Setup tab, "Prepare:" row:**
  - **[Held]** sets the dish to the item in your main hand.
  - **[x]** goes back to "best food".
  - Or click any dish in the Status tab (a "Can make" or "Closest" line).
- **In this mode she makes only that dish,** in batches of "dishes per round", with chains, feasts and pot ferrying.
  Your choice overrides her own rules: no recent-dish rotation, no No-cook list, no "worth it" check.
- **If the dish can't be made right now,** her status names the first missing ingredient. Her requests then aim at
  that dish, asking for up to `targetMaxMissingKinds` (3) kinds of items, preferring the ingredient itself over things
  further down the chain.

## P3: asking for ingredients
- **When she asks.** Every `intervalTicks` (default 5 min) she looks for a dish that:
  - is worth at least `minGainPercent` (50%) more than what she makes now (anything counts if she's idle), and
  - is missing at most `maxMissingKinds` (1) kinds of items.
  - The missing items can be anywhere in a chain.
- **How she asks.** A chat bubble ("Master, could you get me 2 Tomato? Then I can make Ratatouille!"), plus a chat
  message to her owner with a clickable **[Never ask for X]** per item. She doesn't repeat the same request within
  `repeatTicks` (one day).
- **What she never asks for:**
  - blocked items
  - the `neverRequest` list (dragon loot, nether star...)
  - rare/epic items
  - prepared food (anything any recipe in the game makes from other food: she makes it or asks for its ingredients)
  - with `onlyAskForKnownItems` (on by default), anything her owner has never picked up or crafted. Requests then
    only happen while the owner is online.
- **The request list is shared by all of one player's maids** (each player has their own) and saved with the world.
  - An item only appears after a maid asks for it.
  - Chef screen, **Requests** tab: Blocked/Allowed toggle and forget (x) per item.
  - Commands: `/maidchef block <item>` and `/maidchef unblock <item>`.
  - Blocking only stops requests: she still cooks with a blocked item if it's in the chest.
- **Status tab** shows "Would ask for: ..." when she has a request in mind.
- **She asks for the plainest item:** vanilla first, then shorter names (Bread, not a modded bread from the same tag).
  An item only counts as prepared food if *every* recipe for it uses food, so bread made from wheat stays askable.
- **Known gap:** recipes of custom types that don't list their ingredients (e.g. Youkai's Homecoming rainbow roll)
  aren't recognised as prepared food. The known-items check and the block list cover them.

## P2: chains, feasts, small backpacks
- **Chains.** She may make ingredients first, up to `chainDepth` steps (default 3). Example: fry eggs in the smoker,
  then craft an egg sandwich.
  - The planner searches "make X × n" recursively and works out the quantity for each step.
  - Intermediates stay in her backpack.
  - At the end, everything except the dish goes back to the ingredient chest: spare intermediates, containers,
    leftovers.
- **Serving feasts.** Per-maid "Serve feasts" toggle, on by default.
  - On: the dish she aims for is the serving. She crafts or cooks the feast, places it next to her, serves every
    portion with its container (bowl, bowl of rice...), clears the leftovers and delivers the portions.
  - On: a feast found in the ingredient chest also counts as servings waiting to happen.
  - Off: she delivers the feast block itself.
- **Small backpacks.**
  - She fetches per step, so she only carries what the current step uses.
  - A crafting table takes its ingredients straight from the bound ingredient chests (it has no inventory to fill).
  - A cooking pot is filled over several trips when everything won't fit at once. She won't add a stack if the half-
    filled pot would match a different recipe, and if she can't finish she takes everything back out.
  - "Doesn't fit in her backpack" now only applies to single-input stations with huge batches.

## Using it
- Put the maid in **home mode**. The kitchen is centred on her home/work point.
- **Chests:** put a Maidsoul Kitchen **Culinary Hub** in her 5th backpack slot and bind chests with it. Red chests
  are ingredient chests, green chests are output chests. Without a hub she cooks from her backpack and keeps the
  food there.
- She also uses whatever is in her backpack.
- **Stations** (only ones that are idle and working):
  - furnace, smoker, vanilla campfire
  - FD stove (only when nothing sits on top of it)
  - FD skillet on heat, FD cooking pot on heat
  - FD cutting board (needs a knife in her inventory or the chest)
  - crafting table (only recipes with 2+ ingredients, so no unpacking crates)
- **Task tab** (Setup / No-cook / Requests / Status). **Setup:**
  - serve feasts (on/off)
  - kitchen radius, dishes per round
  - how many recent dishes to remember
  - minimum nutrition, minimum saturation, minimum number of beneficial effects
- **Task tab, Blacklist:** foods she won't make. Type an id or `#tag`, or use the food in your hand. Click an
  entry to remove it.
- **Task tab, Status:** what she's doing, the dishes she made recently, and a scrollable breakdown from the server:
  - her stations and whether each is ready
  - "Now:" her current plan, step by step
  - "Next:" what she'd do after it
  - what she can make and how
  - what's blocked and why (filters, no fuel, no tool, not worth it, doesn't fit)
  - the dishes she's closest to making

  Use Refresh to update it. Hover a cut-off line to read all of it.
- **`/maidchef explain [maid]`** (any player; with no argument it uses the nearest Chef within 16 blocks): lists her
  stations and whether each is ready, what she can make, what's blocked (filters, no fuel, no tool, not worth it),
  and the dishes she's closest to making ("missing Coffee (has 3 of 4)").
- She doesn't eat her ingredients. TLM's "work meal" is off for the Chef task.
- **Kitchen radius is per maid.** While she's on the Chef task, her TLM range becomes her kitchen radius + 3
  (`SchedulePosMixin`), so she isn't pulled back to the global `MaidWorkRange`.

## How she picks
- **Dish value** = nutrition + saturation + beneficial-effect bonus (weights in `config/maidchef-common.toml`).
- **Feasts:**
  - A Farmer's Delight feast is worth servings × one serving.
  - An Extra Delight feast (`extradelight:feast` recipes) is worth servings × the best option's (serving − what it
    uses up). So a serving that takes a bowl of rice is only credited with what it adds over the rice.
  - Measured: the ED beef stew feast is 162 vs 34.2 for a bowl of beef stew; the FD roast chicken block is 160.
  - She delivers the feast **item**. Placing it and serving it out (bowl / rice bowl) is planned for P2.
  - In this pack Extra Delight disables FD's beef stew pot recipe, so beef stew only exists as a feast.
- A recipe only counts if its output is worth more than the food it uses up.
- **Order:** the highest value that isn't in her recent list, random among equal values. The amount is
  `min(dishes per round, what the ingredients allow, what fits in her backpack)`.
- Each input uses the same item every time (a pot slot can only hold one kind of item).
- **Only recent dishes possible:** she tries the **oldest** remembered one first. Dishes she can't make are
  dropped from the list. A failed attempt drops it too. A success moves it to the front as usual.
- **Furnace/smoker fuel:** comes from the chest. The accepted fuels are listed in the config (`#minecraft:coals` by
  default). She never burns an ingredient.

## Taken items
Items that pop out of a station she's using (skillet, campfire, stove, cutting board) get a 10 s pickup delay, so
players and other maids (TLM respects the delay) can't grab them before she does. If a player takes one anyway:
- She pauses and asks for it: "Master, please give back 1 Cooked Chicken (´；ω；`)". She calls her owner "Master"
  and anyone else by name.
- To give it back, drop it at her feet or put it in her backpack.
- If it isn't back within `returnWaitSeconds` (default 60), she gives up on that dish. It counts as recent, and she
  plans a different one.

## Stations

**Supported** (counts from the 1 Oct 2026 jar scan):

| Mod | Stations |
|---|---|
| Vanilla | furnace, smoker, campfire, crafting table (pantry) |
| Farmer's Delight | cooking pot (every Delight mod's pot recipes too), skillet, stove, cutting board; serving feasts |
| Extra Delight | oven, drying rack, dough shaping, mortar (+ fluid results bottled), juicer, mixing bowl (+ fluids), melting pot, chiller (ice), vat (multi-stage), evaporator; serving feasts |
| Brewin' and Chewin' | keg (drinks poured into their tankards or bottles; drink-to-drink brews too) |
| Farmer's Respite | kettle (teas poured into cups; tea-to-tea brews too) |
| Youkai's Homecoming | drying rack (sun), moka pot, kettle, grape basin, fermentation tank, cooking pots (filled with water, served into bowls), steamer, cuisine board |
| Spectrum | potion workshop (crafting: teas, chocolate...), titration barrels (real-hour aging) |
| Butchery | meat grinder, pestle and mortar |
| Expanded Delight | juicer |
| Cooking for Blockheads | oven (furnace food recipes) |

How the less obvious ones work:
- **Fluids as items.** A recipe's fluid is carried as bottles (ED's bottle registry, YHC drinks, B&C/FR drinks she has seen poured) or buckets. Water comes from a water bucket, and any water left over is poured away (water is free). Amounts that don't come out even (100 mB of cocoa butter) are skipped.
- **Tools she brings and takes back.** The oven's baking dish, the mortar's pestle, the mixing bowl's spoon, the chiller's mold, the vat's lid and the grinder's sausage attachment.
- **Slow stations: start, leave, come back.** Kegs, vats, barrels, evaporators, and any load taking longer than `leaveAfterTicks` (2400). She loads the station, notes it in her saved data (it survives restarts), puts the leftovers away and goes on cooking. Each time she plans, she checks those stations first:
  - **Ready:** she brings the containers (tankards, bottles), collects, delivers.
  - **Needs tending:** a vat between stages gets its next ingredient and its lid put on or taken off, then she leaves again.
- **Station conditions she respects.** A keg only makes recipes for its current temperature. The YHC drying rack only works under open sky by day. The evaporator only works in warm biomes. The Spectrum workshop and barrels only run while their owner is online and has the recipe unlocked.

**Tested** 2 Oct 2026, harness self-test 45/45 (`test-server/X-expeditions/logs/selftest-2026-10-02-stations.log`). Every station above has a scene except:
- **Spectrum:** its recipes need an online owner, so the headless test only checks they're read (27 workshop, 70 barrel).
- **Butchery pestle and Expanded Delight juicer:** the station code works, but this pack's unification datapack turns all their recipes off (merged into the Extra Delight mortar and juicer).

The release jar was installed 2 Oct 08:14; the 1 Oct jar is in `removed-mods/maidchef-2026-10-01-1351`.

**Not covered:**
- Cooking for Blockheads toaster: its recipes need Croptopia or Pam's, which aren't installed.
- Eternal Tales vital soup: it's an item you use on a block, not a station.
- Create's machines: automation, not a kitchen.
- Spectrum potion brewing: makes potions, not food (left out on purpose, 2 Oct).

## Not done / known limits
- **Recipes in this pack:**
  - Cultural Delights empties vanilla rabbit stew.
  - Extra Delight disables FD's beef stew pot recipe and the FD roast chicken / shepherd's pie / honey glazed ham
    feast recipes.
  - She only uses recipes that are actually active.
- **Ranking is by value per item,** so with gleaming salad ingredients she picks Rustic Delight's sweet salad unless
  you blacklist it.
- **Not supported:** YHC stations, Extra Delight stations (oven, mixing bowl...), Cooking for Blockheads, blast
  furnaces.
- An outdoor FD stove under snow drops what's on it, because snow counts as covering it. She picks the ingredients
  back up and reports "made 0".
- She doesn't notice a player taking food straight out of a furnace/pot output slot (only picked-up item entities).
- Job progress survives brain refreshes (closing her GUI) but not a world reload. On reload, staged ingredients
  stay in her backpack and get used in the next round.

## Test
`/maidchef selftest` (op). It builds 10 kitchens at y=235 above spawn in force-loaded chunks, runs real maids on
the Chef task and checks the chests and backpacks. Everything is cleared afterwards. `/maidchef selftest clean`
cleans up early.

Scenes:
1. smoker with fuel and leftovers returned
2. FD pot on a covered stove
3. cutting board with the knife fetched from the chest
4. crafting table
5. rotation
6. radius 20 past the global work range
7. taken item, never returned (abort)
8. taken item, given back
9. blacklist + minimum nutrition
10. recent fallback, oldest first
11. chain: egg sandwich
12. feast served into bowls
13. feast kept whole
14. crafting from the chest with a small backpack
15. pot filled over several trips
16. request, then block (the asked-for item is listed; after blocking it isn't asked for again)
17. prepare X (not the best dish)
18. prepare X with a missing ingredient: status says "missing Bread", she asks for plain Bread
19. chat with her through Player2 (two questions; the second switches her to fried eggs)

Two checks without a kitchen:
- the prepared-food rule: bread is askable, fish salad and croque monsieur are not
- dish-name matching

Harness: maids saved by an interrupted run are removed for the first 5 s of a run, and the test cleans up if the
server stops mid-run.

Test kitchens have glass roofs: snow and sandstorm red-sand piles landing on a stove make it drop its food. The
self-test owner isn't a real player, so it skips the known-items check.

Test maids get a big backpack, except in scenes 14–15.

Status: **21/21 passed** (19 scenes + 2 checks, with real chat via Player2) on 1 Oct 2026, log `selftest-2026-10-01-p4-chat.log`. Earlier: **18/18** (P1–P4). Earlier: **16/16 passed** (P1 + P2 + P3) on 1 Oct 2026, log `selftest-2026-10-01-p3.log`. Before that, P1 + P2: **15/15**, log `selftest-2026-10-01-p2.log`. Earlier, P1 alone: **10/10 passed** on test-server/X-expeditions on 1 Oct 2026 (log: `selftest-2026-10-01.log`).

Bugs the harness runs found and fixed:
- `createBrainTasks` must return a mutable list (TLM appends to it).
- TLM's work meal ate her ingredients.
- She stopped collecting after a "work from here" fallback.
- The fallback wiped her recent list whenever nothing at all could be cooked.

Harness lessons:
- Use NeoForge chunk tickets, not vanilla forced chunks. With vanilla forced chunks the maids were unloaded.
- Maids saved to disk in an earlier run reload and cook from the same chests. The test now removes every maid in
  the area first.
- At y=235 snow covers stoves. The test now forces clear weather (this one-line change is unverified; the passing
  run cleared the weather by hand).

`debugLog = true` in the config logs every fetch/load/collect/delivery.

Build:
- `./gradlew build` → test build, which includes `/maidchef selftest` (SelfTest + SelfTestHook).
- `./gradlew clean build -Prelease` → release build, with no self-test code.

Both produce `build/libs/maidchef-1.21.1-0.1.0.jar`. Compile-only deps: the TLM, FD, MSK and Extra
Delight jars from the live instance. **Installed 1 Oct 2026, 13:51, in `1.21.1 slim`** (the release build that passed 21/21). Youchrist needs the same jar.
