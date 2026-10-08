---
name: townstead-timeline-calendar-sync
description: Townstead and RPG Timeline calendars synced 19 Sep via timelinesync mod + 12 custom month names (config + resource pack)
metadata: 
  node_type: memory
  type: project
  originSessionId: 53c27661-4e53-441c-9e6f-bc8d0c27d4c4
  modified: 2026-09-19T01:37:36.496Z
---

Built 19 Sep 2026: `timelinesync-1.21.1-1.0.0.jar` (source `C:\Users\eteil.000\Claude\townstead-timeline-sync`).
Townstead is the source of truth (profile auto → Serene: year = Serene cycles + 1000, 12 months × 8 days). The mod keeps
RPG Timeline's saved `DayOffsetDays` so Timeline shows the same Y/M/D; `/timelinesync` prints both dates.
Tested on `modpack-audit/test-server/T-timelinesync` (minimal server; `libraries` is a junction to A-full).

User-chosen month names (first 3 letters must stay distinct — the user's rule, Timeline uses them as short labels):
Dawnroot, Budwake, Bloomtide, Greengale, Solstice, Suncrest, Stormfall, Reaping, Duskward, Frostveil, Longnight, Darkrest.
Set in `config/rpgtimeline-server.toml` (monthNames, daysPerMonth=8; original at
`proposed-configs/rpgtimeline-server.toml.BEFORE-timelinesync`) and in resource pack `townstead-timeline-months.zip`
(overrides `townstead_calendar.serene.month.*` lang keys; copy in `resourcepacks-out`; enabled + required via `config/resourcepackoverrides.json`).

**Why:** Timeline's month shape must equal Townstead's or days can't match; the mod warns in the log if they diverge.
**How to apply:** if Timeline months revert to Dawnroot..Darkrest ×8 or Townstead shows "Early Spring", a config reset
(see [[test-instance-copy-resets-configs]]) or the pack being disabled is the cause.
