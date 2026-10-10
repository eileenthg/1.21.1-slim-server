---
name: tlm-trumpet-addon
description: Maid Trumpet Calls - NeoForge addon giving TLM's trumpet random ZUNpet fanfares; state and next steps
metadata:
  type: project
---

Two related pieces of work, target platform **NeoForge 21.1.219 / MC 1.21.1** (TLM's `1.21` branch, not the `1.20` default branch).

**1. AI character prompts** - 174 settings YAMLs covering all 193 built-in maid model IDs, committed inside the TLM fork's bundled pack. Repo `C:\Users\eteil.000\Claude\tlm`, branch `feat/ai-prompts-1.21` (commit 2565fb9). User chose to keep these in the TLM fork rather than ship as a standalone `tlm_custom_pack` zip, so this half stays a fork and must be re-applied on TLM updates.

**2. Trumpet sound addon** - separate repo `C:\Users\eteil.000\Claude\tlm-trumpet-addon`, mod id `tlmtrumpet`, 3 commits, HEAD 28c3de0. Hooks `LivingEntityUseItemEvent.Stop` at LOWEST priority, matches TLM's trumpet by registry key, picks one of 8 sounds **on the server** and broadcasts it (a single multi-entry sound event would let each client roll its own variant). No compile-time dependency on TLM.

Audio pipeline lives in that repo under `tools/` - ZUNpet sampler built from the "Romantic Tp" preset of a user-supplied SF2, a mini notation format (`C5:1 F5:0.5`), convolution reverb, and `build_sounds.py` holding the eight melodies as the source of truth. Source MP3s and the 261 MB .sf2 are in `C:\Users\eteil.000\Claude\audio-sources\` (deliberately outside both repos).

**Paused mid-task. Next up, in order:**
1. Make the sound list datapack-extendable. Design settled on: a NeoForge datapack registry via `DataPackRegistryEvent.NewRegistry`, entries using `SoundEvent.CODEC` so a datapack can either reference a registered sound id or inline `{"sound_id": ..., "range": ...}`. Vanilla 1.21's `jukebox_song` is the precedent. Note a datapack alone cannot add audio - the .ogg and `sounds.json` entry still need a resource pack.
2. Publish to GitHub, then possibly CurseForge and Modrinth.

Never yet run in-game - both projects compile and the event class is confirmed present in NeoForge, but no `runClient` test has happened. See [[tlm-repo-line-endings]].
