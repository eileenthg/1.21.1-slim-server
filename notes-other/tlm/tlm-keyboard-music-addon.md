---
name: tlm-keyboard-music-addon
description: Maid Keyboard Music - NeoForge addon playing music at TLM's keyboard; state and open items
metadata:
  type: project
---

Separate repo `C:\Users\eteil.000\Claude\tlm-keyboard-music`, mod id `tlmkeyboardmusic`, 1 commit, built from the [[tlm-trumpet-addon]] scaffolding. No compile-time TLM dependency: keyboard/sit/maid recognised by registry name, maid model read from her NBT key `model_id`.

Maid auto-plays when seated; a seated player right-clicks (empty hand) to play. Server picks the track and sends a payload to tracking players; clients stop when the seat empties. Tracks are a datapack registry (`tlmkeyboardmusic/keyboard_track`) with `models` + `weight` for per-character themes - that is the planned easter-egg hook.

4 default tracks: Gymnopedie No.1, C418 Sweden, C418 Wet Hands, Torby Brand's Clark. Audio converted to mono ogg with imageio-ffmpeg (pip), since choco failed and there is no system ffmpeg.

**Open:** never run in-game; user will upload character themes labelled "[character] theme" and needs help mapping them to maid model ids; not published (user is not publishing yet, so rights for the YouTube-sourced tracks are unresolved). Originally prototyped inside the TLM fork, then reverted from it.
