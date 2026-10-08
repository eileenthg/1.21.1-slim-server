# Player2 Watch (p2watch)

Client-only NeoForge 1.21.1 mod. It reminds you when the Player2 app isn't running, keeps checking, and tells you when it connects.

## Which mods use Player2 (checked 1 Oct 2026, all 526 jars scanned)

| Mod | Feature | Runs on | Pack setting |
|---|---|---|---|
| Touhou Little Maid 1.5.3 | maid AI chat (LLM) | server (host's Player2) | `sites/llm.json` player2 enabled |
| | maid voices (TTS) | server, audio sent to clients | `sites/tts.json` player2 enabled |
| | maid voice input (STT) | client (your own Player2) | `STTType = PLAYER2` |
| MCA Reborn 7.7.36 | villager AI chat | server | `villagerChatAIModel = player2` |
| | villager voices | client | `enableOnlineTTS = false` (off) |
| Townstead 0.7.6 | dialogue voices, through MCA's Player2 voice code | client | follows MCA's TTS setting |

None of them caches an "offline" state. Each request asks Player2 again; MCA retries its voice list while it's missing. Once Player2 is up, everything works again without a restart or reconnect.

## What it does
- **Title screen:** if Player2 is offline, a toast lists what needs it.
- **Joining a world:** if it's offline, a chat line names what won't work, with a **[Start Player2]** button (it launches `%LOCALAPPDATA%\Player2\player2.exe`).
- **While it's offline:** checks `/v1/health` every 5 s. When Player2 comes up, you get a toast and the chat line "Connected (app x). … work again."
- **While it's online:** checks every 30 s. If Player2 closes, you get "Lost the Player2 app …" and checking goes back to every 5 s.
- **On someone else's server:** only your own client's parts count (maid voice input, villager voices). Chat runs on the host's Player2.
- **Commands:** `/player2` (status), `/player2 check`, `/player2 start`.
- **Config:** `config/p2watch-client.toml`: url (empty = MCA's `player2Url`), check intervals, app path, and toggles for the toast, join message and connect message.

Features are worked out from the TLM and MCA config files, so changing those configs changes what it reminds you about.

## Test
`test-client/run-p2watchtest.ps1` points the mod at a fake Player2 on port 4399 (a Python http.server serving `v1/health`), so the real app is never touched.

**Result, 1 Oct 2026:** 4/4 passed: title toast, join reminder, Connected (6 s after the fake started), Lost (27 s after it stopped). Installed in `1.21.1 slim`.

One thing to know: for the first few minutes in a world, JEI builds its recipe list on the game's main thread. A Connected or Lost notice that happens during that time appears once JEI is done.
