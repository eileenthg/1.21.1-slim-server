---
name: friend-update-zips
description: How to build a mod-update ZIP for the friend (Youchrist) who joins the world; last one made 25 Sep 2026
metadata:
  node_type: memory
  type: project
  originSessionId: 67315980-40f4-46de-8992-3c58bb461dd5
  modified: 2026-09-27T14:50:08.038Z
---

The friend who joins the user's "New World" is **Youchrist** (uuid b941dc0e-…). On 25 Sep 2026 the user asked for a ZIP of the mod changes since his last session (23 Sep 20:33 launch → 24 Sep 02:00). It was sent as `modpack-audit/friend-update-2026-09-25.zip`: 19 jars plus a README listing 4 jars to delete (L&L `-en.jar`, crafttracker, mcaanimcompat 1.0.0, mcarpgbridge 1.0.0). Only mods were included. Configs were not.

**27 Sep 2026 update:** he played on 26 Sep (launch 12:11, log `2026-09-26-5.log.gz`). `friend-update-2026-09-27.zip` holds 9 new jars. It also re-sends 3 catch-up jars that changed between the 25 Sep ZIP and his session (backslots stickyswap, gravetagcleaner, mcaanimcompat 1.4.0), because it's unknown whether he got them. The delete list is chunky-offline, evercrops, lazychunkload, loadmychunks, lmctickfix, backslots-1.1.5.jar and mcaanimcompat 1.3.0. The pack is now 526 jars. The next baseline is 27 Sep, unless he has played since.

**How to apply:** for the next update, use the latest ZIP as the baseline, but check whether he has played since. Look for "Youchrist joined" in `logs/*.log.gz`. To get the exact mod set of a session, take the `Found mod file "…"` lines (mods folder locator) from that session's first log and diff them against the `mods` folder. Library jars and Connector `_mapped_moj` names are noise. Also watch for jars that were patched in place under the same name, such as the FD patched jar, by comparing LastWriteTime against the session start.

**8 Oct 2026:** he has played since: 28 Sep, 7–8 Oct, and 8 Oct at 16:18. The user had hand-delivered "the latest 4 mods". I gave him a delete-only list, measured against the 27 Sep ZIP baseline. These are definitely to delete: MaidTaskScroll, L&L `-tradepull.jar`, chesspvp-1.21.1-neoforge-1.0.0, colorfulazaleas, csextras-1.1.0, maidfriendlyfire 1.2.0, mcarpgbridge 1.1.0, mebahelrpgrquest-2.0.6, thinair-1.0.5 and thinairthrottle. These are to delete if present: L&L `-tradepull-shrine.jar`, exspectriments-0.3.0, chunky-offline, lazychunkload, loadmychunks and lmctickfix. Gotcha: Connector Fabric jars only show in the logs as `<name>_mapped_moj_1.21.1.jar`, not as mods-folder-locator lines, so diff those separately. Also, `unzip -Z1` on a Compress-Archive ZIP gives `mods\x.jar` paths.

**8 Oct 2026, later: new baseline.** The user sent the friend a full mods folder, `Downloads/LOAD ALRADY.zip`, and told him to wipe his mods folder and unzip it. It holds 537 jars plus `.index` at the root. Compared with live, it has the old maidfriendlyfire 1.2.0 and old builds of both butchery_alexs_mobs jars (CRC differs), and it lacks groundberries. `friend-topup-2026-10-08.zip` fixes that: maidfriendlyfire 1.3.0, groundberries and both butchery jars, plus deleting 1.2.0, which brings him to 538. **Next time, diff against LOAD ALRADY + this top-up**, not the older lists above. Compare CRCs as well as names, because the user rebuilds jars in place.
