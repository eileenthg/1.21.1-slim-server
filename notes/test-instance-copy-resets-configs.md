---
name: test-instance-copy-resets-configs
description: "Config/options \"resets\" in the 1.21.1 Prism instance came from copying the \"Test minecolonies to MCA conversion\" instance folder over it; how to diagnose and restore"
metadata: 
  node_type: memory
  type: project
  originSessionId: 6646a658-2cdb-4a47-8810-94021639f248
  modified: 2026-09-15T12:41:14.802Z
---

On 2026-09-15 at 17:11, the user merged the whole `minecraft` folder of the Prism instance "Test minecolonies to MCA conversion" into `instances/1.21.1/minecraft`. This overwrote options.txt, ferritecore-mixin.toml and friendsandfoes.json, and added a duplicate FarmersDelight jar. The user called it "configs got reset once again". A similar options.txt rollback also happened on 2026-09-13.

**Why:** the test instance was cloned from the main one around 1–4 Sep, so its files carry old settings.

**How to apply:** when configs look reset, compare file CreationTime with LastWriteTime: copied-in files have an old mtime and a new ctime. cmp against the test instance. Use `modpack-audit/test-server/A-full/config` (13 Sep snapshot) and `proposed-configs/*.BEFORE*` as known-good sources. Re-run the verify checklist (scratchpad verify.py pattern: every setting from the audit reports). Check the duplicate mod ids too. Report: modpack-audit/10-config-reset-and-colony-sweep.md.
