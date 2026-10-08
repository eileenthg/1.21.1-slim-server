---
name: mca-chatai-player2
description: MCA villager chat AI works through Player2 only when Player2 is running before the session. The AI is reached by typing in normal chat near a villager; the interact GUI is canned. Diagnosed 1 Oct 2026
metadata:
  type: project
---

`config/mca.json` is set up correctly:
- `enableVillagerChatAI` is true
- the endpoint is `http://127.0.0.1:4315/v1/chat/completions`
- the model is `player2`

On 1 Oct 2026 a direct POST to Player2 (client 0.10.83, backend model deepseek-v4-flash) returned a reply.

The cause of "it doesn't work": in every logged session (24–28 Sep, 1 Oct morning), debug.log shows `Failed to check player2 health: Connection refused` once a minute. Player2 wasn't running. Its process started 1 Oct 12:50.

MCA only calls the AI from a mixin on `ServerGamePacketListenerImpl.handleChat`. That means plain chat typed near a villager, addressed by name or while already in conversation. The right-click interaction GUI uses canned phrases. mcaconversations is deterministic and has no LLM.

Requests go from the host's integrated server, so only the host needs Player2 for chat. TTS is client side.

Not yet verified in game: whether another chat mixin (towntalk, chatoptimizer, Essential, mcaconversations) swallows the chat packet first.

**How to apply:** for future reports that "AI dialogue doesn't work", check Player2 is running, then grep debug.log for `player2 health` and `Error parsing answer`.

**TTS "language not supported" (traced 1 Oct 2026):** this message comes from MCA's `command.tts_unsupported_language`. `Player2SpeechManager.getVoices` fetches `/v1/tts/voices`, and when the fetch fails it returns null. MCA then turns that into an empty list and blames the language. The 23 Sep log shows `Failed to fetch voices: Connection refused`, which means Player2 was not running.

With Player2 running, `en_us` maps to `american_english`, which has 11 female and 9 male voices, so English is supported. The current config has `enableOnlineTTS=false` and `onlineTTSModel=elevenlabs`. Player2 voices need `true` and `player2`.

**The user wants text only, no voices (1 Oct 2026).** Keep `enableOnlineTTS=false`. The command `/mca chatAI player2` also sets `enableOnlineTTS=true` and `onlineTTSModel=player2`. That is what switched voices on before. Don't run it, because the chat AI fields are already set. If it gets run anyway, undo it with `/mca tts disable`.

`useMCAVoices=true` is separate. It controls MCA's built-in recorded sound clips (greet, hurt, trade and so on), not TTS. It was left on, pending the user's choice.
