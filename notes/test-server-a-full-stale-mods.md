---
name: test-server-a-full-stale-mods
description: "Only test-server/X-expeditions remains; since 25 Sep its mods/ and datapacks/ are hard links to the live pack, rebuilt by sync-mods.ps1"
metadata:
  node_type: memory
  type: project
  originSessionId: b679692b-56b7-4f79-9fa6-e058598ed20a
  modified: 2026-09-25T04:39:25.079Z
---

On 2026-09-25 the user pruned the test servers down to `modpack-audit/test-server/X-expeditions`. A-full, B-no-roadweaver, BT, Q, T, W-seedscout and _baseline_tmp went to the Windows Recycle Bin, where they can still be restored. The user also wants scaffolding only, not duplicated mods. So `mods/` and `datapacks/` are now NTFS hard links into `instances\1.21.1 slim\minecraft`, rebuilt by `X-expeditions/sync-mods.ps1`. Run `-Clean` to empty them. The harness's own files are ~418 MB (libraries, config, test world).

- `server-exclude.txt` lists client-only jar patterns (58 as of 25 Sep). Boot tests found `specutils` (a Connector mod that loads client code). Add to the list whenever a boot crashes on a client class.
- Test-only jars and datapacks go in `extra-mods/` and `extra-datapacks/`. A file there overrides the live file of the same name.
- **Never `cp` or edit a file inside `mods/` or `datapacks/`.** It writes through the hard link into the live Prism instance. `colony-expeditions/tools/testserver.sh install` was changed on 25 Sep to go through `extra-mods/`.
- After syncing it boots to `Done` in ~34 s. NeoForge is 21.1.249 on the server and 21.1.250 on the client; the mismatch has been harmless. Boot with `echo stop | "/c/Program Files/Java/jdk-21.0.7/bin/java" @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.249/win_args.txt nogui`.

**Why:** the old copies drifted. A stale `squid-calamari-dedup.zip` in the harness brought back the Farmer's Contracts datapack crash that report 24 had fixed.

**How to apply:** run sync-mods.ps1 before any harness test. A-full's `config/` was the 13 Sep known-good config source for [[test-instance-copy-resets-configs]]. That source is now only in the Recycle Bin; `proposed-configs/*.BEFORE*` still exists.

**3 Oct 2026 sync:** X-expeditions and P-pregen were synced to the live pack (471 and 472 jars with pregenhelper; 26 datapacks). Both excludes now also skip `p2watch-*.jar` and `tlmkeyboardmusic*`. Maid Keyboard Music (the user's own mod, github eileenthg/maid-keyboard-music) crashes a dedicated server. `TlmKeyboardMusic.onRegisterPayloads` (line 61) passes the method reference `client.KeyboardMusicClient::handle` to `playToClient`. Resolving that reference loads KeyboardMusicClient, which drags in `SoundInstance`. Drop that exclude once it's fixed. The stale Maid Chef dev jar was moved from X `extra-mods/` to `removed-mods/harness-extra-2026-10-03/`; an extra jar with the same name as a live one silently overrides it. The X boot was verified clean. P-pregen was synced but not booted. `test-server/mini-thinair` was sent to the Recycle Bin on 3 Oct at the user's request. The client harness (`test-client/run-*.ps1`) launches the live instance itself, so it never needs syncing. Another session may re-sync X in between; recheck `mods/` before blaming the excludes.
- **3 Oct 18:00 update:** the user supplied a fixed tlmkeyboardmusic.jar (17:57, handler now a lambda). It's installed live; the old jar is in `removed-mods/tlmkeyboardmusic-2026-10-03-1650/`. The exclude was removed from both servers. X boots clean with it.
