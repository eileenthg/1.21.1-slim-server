---
name: guide-library-mod
description: "Guide Library (modid guidelibrary): NeoForge 1.21.1 rewrite of otectus/guide-tome made PatchouliButton-style; built 2026-09-19, in-game test not yet run"
metadata: 
  node_type: memory
  type: project
  originSessionId: 0dce2655-a68e-4a40-81e3-a08d31d1f6a4
  modified: 2026-09-19T00:24:12.606Z
---

On 2026-09-19 the user asked for otectus/guide-tome (Forge 1.20.1) reworked to behave like PatchouliButton, for this pack and future packs. Built as a new mod at `modpack-audit/guide-library`:
- An inventory button opens a library of the books that player has **unlocked**. The user's rule (same day): a book shows up only after a physical copy has been in their inventory once. Unlocks are a server-side player attachment, and ops can grant them with /guidelibrary unlock (quest rewards). unlockMode=EVERYTHING restores list-everything.
- Books are discovered automatically: Patchouli, Modonomicon, and a `#guidelibrary:guide_books` tag plus config for item books.
- No absorption and no tome item.

**Why no keyword heuristic:** in this pack "book"/"tome" names mostly spell tomes (Eternal Tales sorcery books, Apothic tomes, Quark ancient tome). Opening an item book means replaying `use()`, so a wrong match would cast or consume something.

**MCA books** open from the server: the client reads the main-hand item when MCA's `OpenGuiRequest` arrives. So the client keeps a fake book in its selected slot until the screen appears.

**Status (2026-09-19 08:50):** installed in the instance. The in-game test passed:
- a fresh player sees 0/20 books; picking up 3 copies unlocks exactly those 3
- forged requests for a locked book and for an ender pearl are both refused
- all 7 Patchouli books, the Modonomicon book, the Animal Dictionary and the MCA books open, and the held item is restored
- button placement looks right in both the survival and creative inventories

One bug was fixed after the test: the item-book cooldown used game time, which is frozen while a book screen pauses singleplayer, so every other MCA book failed. It now uses a 250 ms wall-clock cooldown. That fix has not been re-run in-game.

**How to apply:** for problems, run `test-client/run-guidelibtest.ps1` with Minecraft closed and read the GLTEST lines.

Related: [[test-instance-copy-resets-configs]].