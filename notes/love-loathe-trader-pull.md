---
name: love-loathe-trader-pull
description: Love & Loathe jar patched so wandering traders get 2 random maid-item trades instead of 21 fixed ones (25 Sep)
metadata:
  node_type: memory
  type: project
  originSessionId: 96238db0-2fe8-4334-9ba6-c2340f5c0ca1
  modified: 2026-09-25T02:21:39.768Z
---

On 2026-09-25 the user asked to stop wandering traders from always selling Touhou Little Maid items. The fixed trades came only from Love & Loathe (callresponse) `TradingMaidManager.ensureCallResponseOffers`: 21 items (4 callresponse, maid seed, shrine, 2 beds, smart slab, elixir, 10 baubles, wireless IO). No other maid-family mod touches the trader. Its config only has an on/off switch (`[wandering_trader_maid_trade] enabled`).

Patch: `fixed-mods/love-loathe-trade-pull/` (ASM + `TradePull` helper, KEEP = 2, README has the rebuild steps). It is built on top of the English-text jar. The installed jar is `...-2.0.9-en-tradepull.jar`. It boots on test-server A-full. The in-game trade count has not been checked yet: the code only runs when a player is near a trader. The maid-for-sale market was left as it was. Traders that already existed keep their old stock.

**How to apply:** if L&L updates, rerun the English patch (`patch_love_loathe_en.py`) and then this one. The instance folder is now `instances/1.21.1 slim`.

**1 Oct 2026 update:**
- The Shrine is now always offered in addition to the 2 random trades (3 maid trades in total), via `TradePull.ALWAYS`.
- Jar renamed to `...-2.0.9-en-tradepull-shrine.jar`. The old `-tradepull.jar` is in `modpack-audit/removed-mods/`.
- Tested on the test server: 20/20 traders had the Shrine plus 2 others (`test-client/lltrade-tester`).
- Traders that already existed keep their stock.
- Youchrist needs the new jar in his next update ZIP, and the old one goes on his delete list ([[friend-update-zips]]).

**3 Oct 2026 update:** `patch_love_loathe_en.py` now also translates L&L's 21 hard-coded chat bubbles. 17 are in `CustomExtraMaidBrain$LazyLoopBehavior` (the Lazy Maid task), 3 in `TradingMaidManager` and 1 in `WanderingMaidManager`. The user chose "I want cake..." for 想念蛋糕ing.... The script now skips strings that are already English, so it can be re-run on a patched jar; it must run last, after the trade-pull build. Installed jar is now `...-2.0.9-en-tradepull-shrine-bubbles.jar`; the previous one is in the instance's `mod-backups/`. It passed the llvoices game tests, but the bubbles have not been seen in a client. Youchrist needs the new jar and the old one on his delete list ([[friend-update-zips]]).
