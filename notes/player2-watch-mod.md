---
name: player2-watch-mod
description: "p2watch client mod — Player2 offline reminder/auto-detect reconnect; which mods use Player2 (TLM, MCA, Townstead) and server vs client side"
metadata:
  node_type: memory
  type: project
  originSessionId: 76bb3d86-d612-4532-b107-6f9e3d682b3c
  modified: 2026-10-01T09:19:11.584Z
---

Built 1 Oct 2026 at modpack-audit/player2-watch (client-only, mod id p2watch). It shows a toast on the title screen and a chat line on join when the Player2 app (127.0.0.1:4315) is offline, checks /v1/health every 5 s while offline and every 30 s while online, and says Connected or Lost. Commands: /player2, /player2 check, /player2 start (launches %LOCALAPPDATA%\Player2\player2.exe).

A jar scan found only TLM, MCA and Townstead using Player2. Townstead uses MCA's Player2SpeechManager for voices. None caches an offline state, so they recover by themselves once Player2 starts. Chat (both mods) and TLM TTS run server-side on the host's Player2. TLM STT and MCA/Townstead TTS run client-side.

Status 1 Oct ~18:40: client-tested 4/4: title toast, join reminder, Connected 6 s after start, Lost 27 s after stop. INSTALLED in the live instance (527 mods). Gotchas: JEI blocks the render thread for minutes after joining, so chat notices queue until it's done (the test waits for 'Starting JEI took'); this pack's title screen opens under the loading overlay, so the toast retries every second on client tick; force-closing the client spawns a Crash Assistant java helper (the script kills it).

**Why:** the user wanted a reminder at boot plus a reconnect once they start Player2 mid-game.
**How to apply:** client test is test-client/run-p2watchtest.ps1, which uses a fake Player2 on port 4399, never the real app. Related: [[mca-chatai-player2]], [[maid-chef-mod]].
