# 30: Maid chef task and the maid task list (30 Sep)

Source: Maidsoul Kitchen 0.1.3 (`maidsoulkitchen1.21.1betav0.1.3yhc4.2.14.jar`) decompiled with Vineflower. Task list built from every jar in the `1.21.1 slim` mods folder that has a class implementing TLM's `IMaidTask`, then filtered by each mod's registration conditions.

## 1. What Maidsoul Kitchen does today

- **There is one task per station type.** In this pack, 8 cooking tasks register: Furnace, FD Cooking Pot, FD Cutting Board, FD Skillet, and YHC Moka Pot, Tea Kettle, Drying Rack and Fermentation Tank. The lang file also lists Farmer's Respite kettle, Brewin' and Chewin' keg and Miner's Delight copper pot, but this build has no code for them. The BnC branch in `TaskRegister` is empty.
- **Recipes are picked at random.** `MaidRecipesManager.getRecs()` calls `Collections.shuffle`. It does not rank by value. Each maid has a per-task whitelist or blacklist of recipe IDs, which is the recipe picker in the task GUI.
- **Only one step.** A maid only cooks recipes whose inputs she already has, either in her backpack or in chests bound to the Culinary Hub. She never makes an intermediate (for example cabbage leaf on the cutting board) in order to cook something else.
- **Ingredient and output chests already exist.** The Culinary Hub goes in the maid's 5th backpack slot and binds up to 3 chests for each of 5 roles: Ingredient, Start-addition, Ingredient-addition, Output-addition and Output. That covers your steps 1 and 4.
- **Search area.** Stations are searched around the home center (or the maid, if home mode is off), out to `maid.getRestrictRadius()` and ±2 blocks vertically. Bound chests are ignored if they are farther than that radius **from the maid's current position**, not from the home center, which is a quirk. The radius comes from TLM's global `MaidWorkRange = 12`. You're right that it can't be set per maid: the compass moves the center, but the radius stays global.

## 2. Recommendation: a separate add-on mod, not an edit to MSK

Working name: **Maid Chef** (`maidchef`). It adds **one** task, "Chef", and does not modify MSK.

Why a separate mod:
- MSK is a beta that is still being updated. Patching its code would mean redoing the patch after every update.
- The new logic (planner, value ranking, requests, blocklist) doesn't fit MSK's one-task-per-station design. A single Chef task has to drive several station types at once.
- MSK's station classes (`TaskFdCookPot`, `TaskFurnace`, and so on) already encode how to recognise each station, which recipe type it uses, and how to fill it. Chef can probably call these as a library without registering them. That would let you turn off MSK's 8 separate cook tasks in `maidsoulkitchen-common.toml` and still keep the station support. This isn't confirmed yet: MSK's AI reads `maid.getTask()` in places, so part of the station interaction may have to be rewritten in Chef.

### The loop (your steps 1–5)

1. **Ingredients.** Chef reads the maid's inventory plus the bound Ingredient chests. It uses the Culinary Hub, so there's nothing new to learn. A Chef-specific binding tool is the fallback if reusing the hub doesn't work.
2. **Stations.** Chef scans for every supported station inside a **per-maid kitchen radius** stored on the maid and set with a slider in the task GUI. The radius is measured from the home center. This doesn't depend on `MaidWorkRange`, so other maids' ranges don't change. Optionally, you can bind specific stations with the hub instead of scanning.
3. **Plan.** Chef builds a recipe graph from the recipe types of the stations it found (crafting table included), then searches up to a configurable depth, default 3 steps. It considers every dish it could reach with the ingredients it has, including intermediate steps, and scores each reachable dish:
   `score = nutrition + saturation + effectBonus` (weights set in config).
   It picks the best dish and plans `min(targetCount, maxPossible)` of it. It reserves shared ingredients first, so an intermediate step can't use up what a later step needs. Bowls and bottles are treated as ingredients.
4. **Cook and deliver.** Chef runs the steps in order, keeps intermediates in her inventory, and sends finished food to the Output chest. The Output chest can be the same chest as the Ingredient chest.
5. **Repeat with a different dish.** Chef keeps the last *K* dishes it made (configurable) and skips them when choosing. That gives the variety you asked for, and Spice of Life (latiao) is installed, so the variety is useful too.

### Ingredient requests, with a block/unblock list

- While planning, Chef also records "near misses": dishes that score at least X% higher than the best one she can make and that are missing no more than **N ingredient types**. N defaults to 1, the configurable max is 2.
- She reports these to her owner through a chat bubble and an owner message, limited to one per in-game day by default.
- **Wishlist tab** in the Chef GUI: an item appears there only after she has requested it. Each entry has a Block/Unblock toggle. Blocked items are never requested again but stay in the list, so you can unblock them later.

### "Prepare X" mode

Rather than adding a second task, this is a toggle inside the Chef GUI: **Best food** or **Target: [item]**. It uses the same planner with a single fixed goal, so it still handles multiple steps. Making it a toggle also keeps the task list from growing.

### Scope and risks

- Station coverage is limited to what MSK supports (the 8 above). Most delight add-ons in this pack add recipes for the FD cooking pot, cutting board and skillet, so those recipes are covered automatically. **Extra Delight** stations (oven, mixing bowl, juicer, …) and Cooking for Blockheads are **not** covered. Each one would need its own adapter.
- Stations that take a long time (fermentation tank, drying rack) break the "make it now" loop. Chef would start them and pick up the result later, and they would not count toward the current batch.
- This is several days of work, similar in size to csextras or mca-rpg-bridge. Suggested phases:
  - **P1:** Chef task, per-maid radius, value ranking, single-step cooking on the 8 stations, hub chests, rotation.
  - **P2:** multi-step planner.
  - **P3:** requests and wishlist/blocklist.
  - **P4:** Prepare-X toggle.
  Each phase gets a server-harness test like the earlier mods.

## 3. Maid task list (what shows in the GUI right now): 44 tasks

| Source mod | Tasks | Notes |
|---|---|---|
| Touhou Little Maid (21) | Idle, Attack, Bow, Crossbow, Danmaku, Trident, Farm, Sugarcane, Melon, Cocoa, Honey, Grass, Snow, Feed (owner), Shears, Milk, Torch, Breed, Extinguishing, Games, Fishing | Gun doesn't appear because TaCZ isn't installed |
| Maidsoul Kitchen (13) | Berries, Fruit, Melon (Compat), BreedT, Serene Seasons Farm, Furnace, Cooking Pot, Cutting Board, Skillet, Moka Pot, Tea Kettle, Drying Rack, Fermentation Tank | Each can be toggled in `maidsoulkitchen-common.toml` (needs a restart) |
| Maid Useful Task (3) | Logging, Locate, Revive Player | Toggles in `maid_useful_task-common.toml` |
| Maid Spell (2) | Spell Combat, Ranged Spell | |
| Maid Storage Manager | Storage Manager | |
| Maid Use Hand Crank | Hand Crank | |
| Love & Loathe | Lazy Maid | |
| Maid Sword Soaring | Sword Soaring | |
| Create Treadmill | Treadmill | **Deprecated by its own mod.** Its description says to leash the maid to the treadmill instead |
| **Total** | **44** | MaidTaskScroll is already installed; it only makes the list scroll |

### Mitigation, cheapest first

**A. Config-only, no new code**
- `CompatMelonFarmTaskEnabled = false`: its `MelonAndStemList` only contains Simple Farming melons, and Simple Farming isn't installed, so the task does nothing.
- Pick one of each duplicate pair:
  - TLM **Farm** vs MSK **Serene Seasons Farm**. The pack runs Serene Seasons, so keep the SS one.
  - TLM **Breed** vs MSK **BreedT**, which adds a per-species cap.
  
  Only MSK's side can be disabled by config. TLM's own tasks can't.
- After Chef exists, turn off MSK's 8 separate cook tasks: **−8**.

**B. A tiny "task curator" mod, client-side only**
- A config list of task IDs to **hide from the GUI**. Hidden tasks stay registered, so a maid already on a hidden task keeps working, and nothing is deleted.
- Optionally group tasks into sections (Combat / Farming / Animals / Cooking / Utility) so the list reads as headings instead of 44 flat entries.
- This is the only way to hide TLM's own tasks and the deprecated Treadmill.

Rough result of A + Chef + hiding the obvious ones (Treadmill, Compat Melon, TLM Farm, one Breed, the combat variants you don't use): around **25–28**.
