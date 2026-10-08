---
name: maid-task-curator-mod
description: "taskcurator: client-only TLM task menu folders + hiding + editor (gear button); installed 1 Oct 2026, harness 23/23 in real pack; MaidTaskScroll removed"
metadata:
  type: project
---

On 1 Oct 2026 the user asked for a mod that hides maid tasks and sorts them into folders that open and close in TLM's task menu. The project is `modpack-audit/maid-task-curator/` (the README has the details) and `taskcurator-1.21.1-1.0.0.jar` is installed in the slim instance.

- One client mixin on `AbstractMaidContainerGui` (addTaskListButton, taskPageDown, drawTaskPageCount, addTaskControlButton TAIL, renderBg TAIL). Task rows are still built by TLM's `drawPerTaskButton`.
- The layout is stored in `config/taskcurator.json`, per player, and covers all maids. Default layout has 5 folders; Treadmill and Melon (Compat) are hidden by default.
- **MaidTaskScroll was moved to `removed-mods/`**: it computes pages from the total task count, and TLM 1.5.3 already scrolls the list itself.
- The pack registers **48** tasks, not 44: the extra ones are maidspell:spell_combat_melee (there is no `spell_combat`), 3 touhou_lost_maid tasks (TLM hides these from owned maids, and the editor skips them), and youkaishomecoming:danmaku_attack (no lang name).

**Status:** harness run 2 on 1 Oct passed 23/23 in the real pack: folders, paging, gear, editor edits, JSON save, switching the maid from inside a folder, and the gold marker on a closed folder. A real player has not clicked through it yet. The installed layout file holds the defaults.

**How to apply:** if TLM updates, rerun the harness. A renamed target crashes the game on the first maid screen (`defaultRequire = 1`). See [[no-test-servers-while-hosting]] before running it. Maid Chef (report 30) is planned to go in the Cooking folder as `maidchef:chef`.

**Server folders:** both test servers' `server-exclude.txt` list `taskcurator-*` and `MaidTaskScroll_*`, because they are client-only and the user wants server folders kept tidy. MaidTaskScroll was deleted from P-pregen/mods on 1 Oct. By the time the other session's server closed, the X-expeditions link was already gone, most likely removed by a resync that honoured the new exclusion. Neither server has either jar now.
