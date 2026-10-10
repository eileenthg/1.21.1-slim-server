---
name: reply-in-english
description: "User wants replies in English, despite the TLM repo's AGENTS.md defaulting to Simplified Chinese."
metadata:
  node_type: memory
  type: feedback
  originSessionId: 959bab76-d39c-4931-99d8-abe0eb9d4a52
  modified: 2026-09-27T05:46:20.907Z
---

Reply to this user in **English**, not Simplified Chinese. On 2026-09-27 they said: "just because
the original mod has Chinese code doesn't mean you need to parrot Chinese back to me."

**Why:** `C:\Users\eteil.000\Claude\tlm\AGENTS.md` (the TouhouLittleMaid fork) opens with a language
policy saying 默认使用简体中文回答 unless English is explicitly requested. That file came with the
upstream mod and reflects its maintainers, not this user. They read Chinese fine but do not want it.

**How to apply:** Treat that AGENTS.md clause as satisfied — English is the explicit request. Keep
code, commands, errors and API names untranslated either way, which that file also asks for. Chinese
still belongs in artifacts *committed* to the TLM fork (comments, `zh_cn.json`) to match the
surrounding code; this is about how I talk to the user. See [[tlm-trumpet-addon]].
