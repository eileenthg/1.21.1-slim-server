---
name: client-test-scaffold
description: Separate client test scaffold test-client/C-client — user wants client tests there, never in the live instance
metadata:
  type: feedback
---

User (5 Oct 2026): run client tests in a separate scaffold, and make sure no client is running before starting.

**Why:** older client harnesses (run-*test.ps1) launched the live `1.21.1 slim` folder and dropped tester jars/worlds
into it.

**How to apply:** `modpack-audit/test-client/C-client/`: `sync-client.ps1` hard-links live mods/datapacks (minus
client-exclude.txt: AutoModpack, Essential) + extra-mods/extra-datapacks, robocopies config etc., copies "New World" without
chunks to saves/TestWorld; `make_args.py` honours MC_GAMEDIR / MC_ARGS_FILE (client-C.args). `run-tabstest.ps1` is the
template runner (busy check, watches log for crash and kills the hung client). Always wait for zero Minecraft java
processes first ([[wait-for-other-servers-before-boot]]); other sessions' clients use java-runtime-delta too — never kill a
process whose command line lacks client-C.args.
