---
name: lost-maid-model-whitelist
description: "Touhou Lost Maids model_white_list excludes the 12 Gensokyo Legacy NPC characters' maid models (5 Oct); how the whitelist works"
metadata:
  node_type: memory
  type: project
  originSessionId: de3f9b64-5d45-4649-aa81-e3a96a04b94f
  modified: 2026-10-05T04:15:22.003Z
---

On 2026-10-05, at the user's request, `config/touhou_lost_maid-server.toml` `model_white_list` was set to 212 maid models. That is every model in `tlm_custom_pack` except the 21 TLM models of the Gensokyo Legacy NPC characters: Reimu (incl. _2, _type_b, old:reimu_hakurei), Marisa (incl. _2, old:kirisame_marisa), Cirno (4 variants), Rumia, Sanae, Koishi (+_2), Mystia, Yukari, Rinnosuke, Sunny Milk, Luna Child, Star Sapphire. YH Remilia (remilia_scarlet, _2) was also excluded at the user's request, 23 models excluded in total. Backup: `proposed-configs/touhou_lost_maid-server.toml.BEFORE-WHITELIST`. Generator: scratchpad maidmodels.py, which reads every `tlm_custom_pack/*/assets/<ns>/maid_model.json`.

- Lost Maids (touhou_lost_maid 0.0.2) has no blacklist. An empty whitelist means TLM's own finalizeSpawn random pick over all server models applies. A non-empty list makes it re-pick on EntityJoinLevel from the list.
- The re-pick goes through client-only code (`CustomPackLoader` + `PacketDistributor.sendToServer`). It only works on an integrated/LAN host, and an ID that doesn't match a client model silently keeps TLM's random pick. So IDs must be exact, and new maid model packs must be added to the list or they never spawn.
- In-game unverified as of 5 Oct. Test on test-client/C-client, not live.

**How to apply:** when maid model packs or Legacy characters change, regenerate the list. See [[gensokyo-bridge]].
