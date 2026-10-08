---
name: blockstate-memory-cost
description: Measured 7 Oct: all 648,697 BlockStates cost only ~100-160 MB of a 1.93 GB client heap with ModernFix on; block trimming is not a memory fix
metadata:
  type: project
---

Client heap histogram after GC (test-client/C-client/run-histotest.ps1 -> histo-classes.txt, histo-heapinfo.txt), main menu, ModernFix dynamic_resources=true: heap used 1,929 MB, metaspace 639 MB. BlockState: 648,697 objects, 72.7 MB; ferritecore FastMapEntryMap 10.4 MB; ModelResourceLocation 686,541 objects, 16.5 MB. Direct cost about 100 MB, estimated 150-160 MB with variant strings, so about 5-8% of the heap, roughly 250 B per state. Baked models stay out of memory (only 6,367 SimpleBakedModel, 190k BakedQuad = 9 MB).

**Why:** the user asked how much the extra YH/Legacy/other blocks really cost. Trimming 20-30k of the JSON variant entries saves tens of MB at most.

**How to apply:** do not sell block trimming as a memory win; it is clutter and load time only. The earlier "128k blockstates / YH 21%" figure counts blockstate JSON entries; the real BlockState count is 648,697, so per-mod shares are only a proxy. An exact per-namespace count needs a server-side registry dump (not done). Genshin was running during the test (heap numbers unaffected; startup 550 s not comparable with the earlier 485 s). See [[feature-usage-triage]].
