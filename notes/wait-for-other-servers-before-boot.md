---
name: wait-for-other-servers-before-boot
description: "Before EVERY test-server boot (including restarts mid-session), check for other Minecraft servers/clients and wait until they close"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 76bb3d86-d612-4532-b107-6f9e3d682b3c
  modified: 2026-09-30T22:56:32.129Z
---

Before every harness boot, including a quick restart after a rebuild, check for other Minecraft server or client Java processes. Other Claude sessions run their own test servers and clients on this machine. If any are running, keep watching until they have all exited, and only then boot.

**Why:** on 1 Oct 2026 I checked once, booted, then rebooted after a rebuild without checking again, while another session's server was running. The user said: "Monitor until that server closes before you boot. Keep this behaviour for all future runs."

**How to apply:**
- Detect them with `Get-CimInstance Win32_Process` for java.exe/javaw.exe whose command line contains `net.minecraft`, `win_args.txt` or `neoforge`. Ignore the `crash_assistant` helper JVM.
- Use a background loop that exits once the count reaches 0, and re-check right before each boot.
- Other sessions' test clients run from `test-client/client-*.args`; match `client.*\.args` and `prismlauncher` too.
- 5 Oct 2026: a client started 36 s after my server booted, and the user repeated the rule. Use `test-server/X-expeditions/wait-idle.sh`, which blocks until 60 s with no instances, right before each boot. After booting, re-check that nothing else started; if something did, stop mine and wait again.
- This goes on top of [[no-test-servers-while-hosting]]. See also [[test-server-a-full-stale-mods]].
