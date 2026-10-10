---
name: slim-server-repo
description: Private GitHub reference repo eileenthg/1.21.1-slim-server (mod manifest, configs, notes, sources, WORKLOG, SESSIONS) for cloud Claude Code; how to refresh
metadata:
  type: reference
---

Repo https://github.com/eileenthg/1.21.1-slim-server (private), local clone `modpack-audit/slim-server-repo/`. Built 8-10 Oct 2026 so cloud sessions can answer mod questions after the owner's Pro plan lapses. Holds no jars, worlds, logs or keys.

Contents: `CLAUDE.md` (how to use), `WORKLOG.md` (status of all modding work, open threads, owner rules; written from all 81 transcripts), `SESSIONS.md` (transcript index), `mods/` (manifest, MODS.md, registry-names.tsv), `config/`, `datapacks/`, `notes/` (this memory dir), `notes-other/`, `custom-mods/` (sources + fork patches), `reports/`.

**How to refresh:** run `tools/build_manifest.py`, `tools/copy_content.py`, `tools/copy_projects.py`, `tools/export_forks.py` in that order, then commit and push. Commits must use the noreply email `40307498+eileenthg@users.noreply.github.com` (GitHub blocks the private address).

Facts found only in transcripts (10 Oct): the live Arcadian Dream jar is our port and its source folder is gone; Colony Handover lives only in the old test instance; Origins port jars are live; DFR PR #27 is open. See [[dfr-jei-fix]], [[blockstate-desync-seamless-chunks]].
