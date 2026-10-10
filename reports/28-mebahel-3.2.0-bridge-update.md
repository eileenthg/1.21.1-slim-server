# 28: Mebahel's RPG 3.2.0 and MCA RPG Bridge 1.2.0 (28 Sep)

Mebahel's RPG went 2.0.6 → **3.2.0** ("Villager Quests & Companions"). 36 new classes, nothing removed; 51 quests → 79. Built `mcarpgbridge-1.21.1-1.2.0.jar` to match. **No test server was run** (as asked), so nothing below is runtime-verified.

## What 3.2.0 broke in bridge 1.1.0
| Hook | 2.0.6 | 3.2.0 | Effect with 1.1.0 |
|---|---|---|---|
| Villager click guard | `lambda$registerQuestInteraction$2` | **`…$3`** (the `$1` guard-conversion lambda is gone) | The guard would not apply. Mebahel takes plain right-clicks on MCA villagers again and the Request button dies |
| Tracker offset | `drawSolidBackdrop(…)` | **gone**, HUD rewritten | Offset silently stops working (it was `require = 0`) |
| `rewardQuestGiver` | same | **unchanged** | fine |
| `creditContractReputation` | same | **unchanged** | fine |
| `handleAbandonQuest` | same | **unchanged** | fine |
| `QuestGiver` / `QuestPlayer` / `QuestManager` / `QuestDefinition` / `PlayerQuestProgress` / `QuestAvailability` / `QuestTranslations` | — | **unchanged** (additive only) | fine |

All 19 Mebahel members the bridge jar references were checked against the 3.2.0 classes: all present.

## Bridge 1.2.0
1. **Click hook no longer hardcodes a lambda number.** The reflective path scans `lambda$registerQuestInteraction$*` by signature, and the mixin lists both `$2` and `$3` at `require = 0`. A new `verifyClickGuard()` runs on server start and logs which handler was found, warning loudly if it is one this build does not guard — the failure mode that would otherwise be silent.
2. **Tracker offset retargeted** to HEAD/RETURN of `render(GuiGraphics, DeltaTracker)`, which both versions keep, instead of the deleted backdrop call. RETURN, not TAIL, so an early return still pops the pose. Skipped entirely when both offsets are 0.
3. **New `MebahelMcaScreenMixin`.** 3.2.0 holds back ambient dialogue while an MCA screen is open, but it tests for screen classes under `net.mca.`; MCA Reborn 7.7 uses **`net.conczin.mca.`**, so that check never fires in this pack. The mixin makes conczin screens count, so villagers stop talking over MCA menus.

Bridge 1.2.0 works with **either** Mebahel version, so the two jars can be swapped in any order.

## 3.2.0 features worth knowing
- **Its own MCA compat** (`McaRebornCompat`): it skips the right-click only when the villager's quest is already *claimed*, so the bridge's routing is still needed for offered and in-progress quests.
- **Native tracker placement**: `questTrackerPosition` (`top_left`/`top_right`/`bottom_left`/`bottom_right`) and `questTrackerScale` in Mebahel's own config. Probably better than the bridge offset for dodging Xaero's minimap; set the bridge offsets to 0 if you use it.
- New: ambient village dialogue (`ambientDialoguesEnabled`, chance 0.08), an in-game quest creator, quest books, delayed follow-up quests, story flags, a quest map, Saint's Dragons compat.

## Your in-progress "build a house" quest: safe
- `questlines/village_build/village_new_home.json` is **byte-identical** in 3.2.0, objective ids included (`place_cobblestone`, `place_oak_planks`, …).
- **No quest file was removed or renamed**; all 51 old paths still exist.
- `PlayerQuestProgress` NBT keys are **additive** (one new `ObservedProtectionEntities`).
- Villager keys (`MebahelRpgQuestId`, `MebahelRpgQuestClaimed`, `MebahelRpgQuestRollDone`, rumor keys) are **identical**; player keys gain only `MebahelRpgStoryFlags` and `MebahelRpgPendingDelayedQuests`.

So the quest, its progress and the giver villager all carry over. Standing already recorded in MCA Reputation is untouched.

## Not verified
Everything here is from decompiled bytecode. Untested at runtime: that the click mixin applies to 3.2.0's `$3`, the retargeted tracker offset, and the new MCA-screen mixin. First launch should show `Mebahel interaction handler lambda$registerQuestInteraction$3: click guard and MCA menu button armed` in the log.
