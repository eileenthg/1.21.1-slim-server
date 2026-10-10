---
name: tlm-ll-voices-addon
description: Love & Loathe Maid Voices addon - maids voice needs/mood/threshold drift/passive changes via their LLM (Player2); state and open items
metadata:
  type: project
---

Repo `C:\Users\eteil.000\Claude\tlm-ll-voices`, mod id `llvoices`, package `com.github.eileen.llvoices`, 1 commit (d4c9afb, 2026-10-03). Built overnight while the user slept, from their request: maids should voice needs, mood and "drifting toward a threshold" signs, and react to L&L's passive trust/fear changes, all through Player2 so lines fit each character.

Design: compiles against `libs/touhoulittlemaid.jar` (1.5.3) and `libs/callresponse.jar` (the installed L&L 2.0.9 English+tradepull jar, which was decompiled to `~/Claude/_research_ll/src`). The GitHub source `1819919010-star/callresponse` is an old 1.0.0 port for MC 26.1, so do not use it. The tracker diffs L&L's values once a second and guesses the cause of each change from surroundings. L&L's `MaidPassiveEvent` is posted every tick whether or not anything fires, so it is useless as a signal. Lines go through `maid.getAiChatManager().chat()`, wrapped with L&L's `EmotionPrompt` + `BroadcastDialogueTracker` marker so L&L's mixins treat them as its own. A mixin on `MaidAIChatData.getLLMSite` routes all maid chat to `player2` when that site is enabled (`routeAllMaidChat`, default true). Canned en_us fallback lines are used only when no LLM is usable.

Verified: 19 unit tests, 4 GameTests against the real TLM + L&L jars, and runClient boots with no errors.
Not verified: real LLM/Player2 replies, line frequency in play, the drift ring sizes (near = 22 / brink = 10 points). Installed 2026-10-03 into `1.21.1 slim` (`mods/llvoices-0.1.0-neoforge+mc1.21.1.jar`). L&L 2.2.1 exists on CurseForge; the user has 2.0.9 installed.
Related: [[tlm-trumpet-addon]] (the AI character YAMLs that give each maid her voice).
