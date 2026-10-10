# Love & Loathe: Maid Voices

A NeoForge 1.21.1 addon for [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) and
[Love & Loathe](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-love-loathe) (`callresponse`).

Love & Loathe gives every maid trust, fear and hunger, but most changes happen silently. With this addon, maids
bring things up themselves in chat bubbles, and each line is written by that maid's own LLM character setting, so
Reimu doesn't sound like Sakuya.

## What the maids talk about

| Topic | When |
|---|---|
| **Needs** | First signs of hunger (26-40), hungry with no food in her hands or bag, fed again after going hungry, stuffed (75+), hurt (<50% health), badly hurt (<25%), healed |
| **Check-ins** | About every 12 minutes, when you are within 10 blocks, she says how she is doing (hunger, injuries, mood) |
| **Mood** | When her Love & Loathe mood changes (stranger → friendly, friendly → fearful, ...) |
| **Drifting toward a threshold** | Early signs ("near") and "on the brink" for each of Love & Loathe's five emotional cliffs, relief when she backs away, and a reminder every 8 minutes while she stays close |
| **Passive changes** | Reactions to Love & Loathe's passive trust/fear changes: being hit by you, getting hurt, a maid dying nearby, eating, being told to sit, falling asleep, full armour, being ignored, working, resting, company |

### Threshold rings

Distance is the number of trust + fear points still needed to meet Love & Loathe's own condition.
"Near" is 22 points or fewer and "brink" is 10 or fewer. A maid has to back off 3 extra points before she
counts as having left a ring, so values sitting on a line do not make her repeat herself.

| Zone | Love & Loathe condition |
|---|---|
| Betrayal | trust < 10 and fear > 90 (for 10 s) |
| Forgetting | trust ≤ 10 and fear ≤ 10 (for 10 s) |
| Doting | trust ≥ 90 and fear ≤ 10 |
| Devoted | trust ≥ 80 and fear ≥ 80 |
| Panic | fear − trust ≥ 30 |

Reaching the condition itself is left to Love & Loathe, which has its own dialogue for it.

### What is not doubled up

Love & Loathe already speaks for some events: very hungry, starving and overfed hunger; being leashed; the owner
staring at her with food or a weapon; items on her head; darkness; soaking in water; fireworks; changing dimension;
seeing you kill a maid; saddle carries and throws. Those are not repeated. A maid also waits while any bubble is
over her head, including Love & Loathe's, and leaves at least 25 seconds between her own lines.

## Player2 / the LLM

Lines go through TLM's own maid chat (`MaidAIChatManager.chat`), so they use the maid's character setting, her
chat history and her TTS. They are wrapped the same way as Love & Loathe's own dialogue. As a result, Love &
Loathe adds its emotion context to the prompt, cleans up the reply, keeps the prompt out of the maid's chat
history, and counts the line toward its `apiCallsPerMinute` cap.

By default (`llmSite = "player2"`, `routeAllMaidChat = true`) **all** maid LLM chat goes to the Player2 site, not
just this addon's lines. That includes ordinary TLM chat and Love & Loathe's dialogue. This only applies once
Player2 is enabled in TLM's AI site settings and the Player2 app is running. Until then every maid keeps her own
site.

If no LLM can be used (LLM off, the site has no key, the maid has no character setting, or you are out of
tokens), a short canned line from `en_us.json` is shown instead. Set `fallbackLines = false` to stay silent.

## Config

`config/llvoices-common.toml`: master switch, hearing radius, gap between lines, debug echo, LLM site and routing,
fallback lines, check-in interval, mood/drift/passive switches, linger interval, and reaction chances.
Routine changes like working, resting, sleeping, time passing and being ignored use a lower chance (0.2) than
notable ones (0.6), because they fire every few minutes for every maid.

## Building

Copy two jars into `libs/` (git-ignored). The build compiles against them and dev runs load them:

- `libs/touhoulittlemaid.jar`: Touhou Little Maid 1.5.3 for NeoForge 1.21.1
- `libs/callresponse.jar`: Love & Loathe 2.0.9 for NeoForge 1.21.1

```
./gradlew.bat build              # jar + unit tests
./gradlew.bat runGameTestServer  # in-world tests against the real TLM + Love & Loathe
```

## License

MIT. Love & Loathe is MIT (JumDa5he); Touhou Little Maid is MIT (TartaricAcid).
