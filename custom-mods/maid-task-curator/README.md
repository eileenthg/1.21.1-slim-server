# Maid Task Curator (`taskcurator`)

A client-only add-on for Touhou Little Maid 1.5.3+ that organises the task menu, the list you open with the task switch button in a maid's screen.

- **Folders.** Folders appear as header rows (▶ Cooking (13)). Click one to open or close it. A task inside an open folder has an amber stripe to its left. A gold ● on a closed folder means the maid's current task is inside it.
- **Hiding.** Hidden tasks disappear from the menu only. A maid already on a hidden task keeps doing it, and the top of her screen still shows it. Idle can't be hidden.
- **Editor.** The ⚙ button on the task list header opens it. There you can:
  - show or hide any task
  - move a task between folders (click the "Folder:" button; shift-click goes backwards)
  - reorder tasks and folders with ▲/▼
  - create, rename and delete folders (deleting a folder moves its tasks to the top level)
  - reset everything to the default layout
- **Storage.** The layout is saved in `config/taskcurator.json` and applies to every maid in every world. Each player has their own copy. The server and other players don't need the jar (`displayTest = IGNORE_ALL_VERSION`).
- **Tasks the layout doesn't know yet** (from a newly added mod) are appended to the top level on the next launch. IDs of mods that were removed stay in the file, so reinstalling a mod puts its tasks back where they were.
- **Scrolling.** TLM 1.5.3 already scrolls the list with the mouse wheel, and this mod feeds it the folder-aware page count. **MaidTaskScroll is not needed and conflicts with it**, because it computes pages from the total task count. It was moved to `removed-mods/`.

## Default layout

Idle is at the top level. The five folders start closed:

| Folder | Tasks |
|---|---|
| Combat | Attack, Bow, Crossbow, Danmaku, Trident, (Gun), Youkai danmaku, Spell Combat, Ranged Spell, Sword Soaring, plus Lost Maids' three tasks (TLM never shows those to an owned maid, and the editor skips them too) |
| Farming | Farm, Serene Seasons Farm, (Ecliptic Seasons Farm), Sugarcane, Melon, Melon (Compat), Cocoa, Berries, Fruit, Grass, Logging |
| Animals | Breed, BreedT, Shears, Milk, Honey, Fishing |
| Cooking | (Maid Chef), Furnace, Cooking Pot, Cutting Board, Skillet, (Cuisine Skillet, Grill, Basin), Moka Pot, Tea Kettle, Drying Rack, Fermentation Tank, (Beer Barrel) |
| Utility | Feed (owner), Torch, Snow, Extinguishing, Locate, Revive Player, Storage Manager, Hand Crank, Lazy Maid, Games, Treadmill |

Tasks in parentheses aren't registered in this pack and are skipped. The pack registers 48 tasks (the 30 Sep count of 44 missed 4 whose names weren't in the lang scan). Youkai's Homecoming's danmaku task has no English name, so it shows as `task.youkaishomecoming.danmaku_attack`; that's YHC's missing lang entry. **Hidden by default:**
- Treadmill: its own mod deprecates it in favour of leashing the maid to the treadmill.
- Melon (Compat): its melon list only contains Simple Farming melons, and Simple Farming isn't installed.

## How it hooks in

A single mixin on TLM's `AbstractMaidContainerGui`:
- `addTaskListButton` is replaced. Each task row is still built by TLM's own `drawPerTaskButton`, so tooltips, enable conditions and the switch packet are unchanged. Folder rows are `FolderButton`s placed in the same slots.
- `taskPageDown` and `drawTaskPageCount` use the row count instead of the task count.
- `addTaskControlButton` TAIL adds the ⚙ button.
- `renderBg` TAIL draws the folder stripes.

If a TLM update renames any of these, the game fails at the first maid screen, because `defaultRequire = 1`.

## Build and test

`./gradlew build` produces `build/libs/taskcurator-1.21.1-1.0.0.jar`.

`test-client/run-curatortest.ps1` runs the in-game test. It uses a copy of *New World* without its chunk data and the `curator-tester` mod. The tester:
1. spawns an owned maid and opens her screen
2. clicks folders, scrolls, and opens the editor
3. hides and moves tasks and reorders folders and tasks
4. checks the menu and the JSON, then switches the maid's task from inside a folder

It logs lines starting with `CTEST` and saves `ctest-*.png` screenshots.
