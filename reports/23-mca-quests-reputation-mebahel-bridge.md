# 23: MCA Quests ↔ MCA Reputation ↔ Mebahel's RPG: the MCA RPG Bridge mod (19 Sep)

## What was wrong
| # | Problem | Evidence |
|---|---|---|
| 1 | **MCA: Quests never used MCA: Reputation.** Quests 1.6.5 binds only when `McaReputationApi.getApiVersion() == 1`. The NeoForge build of Reputation reports **2** (its changelog says Forge stays at 1 and NeoForge is 2, with no signature changes). So Quests fell back to its own standing store, and quest standing, tiers and titles were split from Reputation's. | `latest.log`: "MCA: Reputation reports API v2 but this build was written against v1 … the integration is disabled" |
| 2 | **Mebahel took the right-click on about 30% of MCA villagers.** Its villager mixin targets vanilla `Villager`, which MCA's `VillagerEntityMCA` extends. It hands out a quest to 15% and a rumor to 15% of villagers, and on those a plain right-click opened Mebahel's dialogue instead of MCA's menu. That also hid the MCA: Quests button on those villagers. | `QuestEvents.lambda$registerQuestInteraction$2`, `mebahelrpgrquest_config.json` (`villagerQuestChance`/`villagerRumorChance` 0.15) |
| 3 | Mebahel quests counted for nothing in MCA Reputation. Mebahel's own "reputation" is a per-board number that everyone shares, not a per-player village standing. | `QuestBoardReputationState` |

No newer MCA: Quests exists yet: 1.6.5 (16 Sep) is the latest, and Reputation 0.5.0 shipped after it.

## Chosen approach: bridge mod (you picked it over a full port)
A full port to MCA: Quests would lose Mebahel's spare/mercy branches (103 uses), its scripted named bosses and its quest boards. The bridge keeps both engines and links them.

**`mcarpgbridge-1.21.1-1.0.0.jar`** (source `C:\Users\eteil.000\Claude\mca-rpg-bridge`, needs the jar on both server and client):
1. **Quests ↔ Reputation:** a mixin makes Quests accept API v2 as v1. All 104 Reputation members Quests calls were checked against the 0.5.0 jar and resolve. Standing, tiers and titles now come from Reputation, and Quests keeps its mirrored copy.
2. **MCA villagers:** Mebahel no longer answers a plain right-click on an MCA villager, so MCA's menu always opens. When the villager has a Mebahel quest or rumor, MCA's menu gets a **Request** or **Rumor** button under MCA: Quests' **Quests** button, and that button runs Mebahel's own handler. Vanilla villagers, guards, companions and bosses behave as before.
3. **Deeds:** Mebahel outcomes become MCA Reputation deeds for the giver's village, using Reputation's own `quest_completed`/`quest_abandoned` types. The giver is named as witness. They show in the Standing screen as "Finished …" or "Walked away from …". Mebahel's board reputation is left as it is.

Config: `config/mcarpgbridge-common.toml`:
- `mcaMenuButton`: true
- `questCompleted`: 4 (Quests' medium is 4; easy 2, hard 7)
- `contractCompleted`: 2
- `questAbandoned`: -2
- `villageSearchRadius`: 96

## Tested
Test server **`test-server/Q-questbridge`** (port 25598). It runs only MCA, Quests, Reputation, Mebahel, Connector/FFAPI, the bridge, and a test-only harness `bridgetester` (source `mca-rpg-bridge/tester`, **not for the pack**). `/bridgetest` builds a house, lets MCA register the village, spawns an MCA villager and a vanilla villager, and uses a fake player. **14/14 PASS:**
- Quests bound (`mcareputation:canonical`).
- Right-clicking an MCA villager with a Mebahel quest returns PASS, so it goes to MCA.
- A vanilla villager still opens Mebahel (CONSUME).
- The Request button opens the quest (CONSUME), and on an empty villager it does nothing.
- Standing: 0 → 4 on a claim, no change on a duplicate claim, +2 for a contract, −2 for an abandon. Mebahel still marks the abandoned quest failed.

The first run failed on abandon because Reputation refuses an unwitnessed `quest_abandoned` deed. Fixed by naming the giver as witness.

**Not tested:** the client button. Its drawing and placement in MCA's menu, and whether Mebahel's dialogue opens on top of MCA's menu, need a real client.

## Not changed, noted
- Two quest trackers, HUDs and journals still exist (MCA: Quests and Mebahel). Mebahel's inventory journal button can be hidden with `hideInventoryJournalButton`.
- The 22:15/22:20/22:21 client crash reports are **Sophisticated Backpacks** reading its config before load ("Rendering item"). They are unrelated to MCA.
- Reputation 0.5.0's changelog says Quests' "optional-delta preservation and bound restitution targets" still need a Quests-side patch. That is upstream work.

## Install (not done: game running, your go-ahead needed)
Put `C:\Users\eteil.000\Claude\mca-rpg-bridge\build\libs\mcarpgbridge-1.21.1-1.0.0.jar` in the instance's `mods/`. Do **not** copy `bridgetester`.
