# Maid Keyboard Music

An addon for [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) that plays music at
the keyboard (piano) block. Minecraft 1.21.1, NeoForge.

- A **maid** who sits at the keyboard starts playing after a short pause, and moves on to another
  piece when one ends.
- A **player** sitting on the keyboard right-clicks it with an empty hand to start a piece.
- The music stops as soon as whoever is sitting gets up, or the keyboard is broken.
- It is positional (24 blocks) and uses the **Jukebox/Note Blocks** volume slider.

Four pieces ship with the mod, and **datapacks can add more**, including themes that only specific
maid models play.

## How it works

TLM's keyboard does nothing on its own. This mod recognises the keyboard seat by registry name
(`touhou_little_maid:sit` standing in a `touhou_little_maid:keyboard` block), so it builds with no
compile-time dependency on TLM. TLM is a runtime dependency only.

The **server picks the track** and sends it to the players tracking the seat, so everyone nearby hears
the same piece. Clients stop it themselves when the seat empties.

## Adding tracks with a datapack

    data/<your_namespace>/tlmkeyboardmusic/keyboard_track/<name>.json

```json
{
  "sound": { "sound_id": "mypack:my_song", "range": 24 },
  "length_seconds": 120
}
```

| Field | Default | Notes |
|---|---|---|
| `sound` | required | a registered sound event id, or an inline `{ "sound_id", "range" }` |
| `length_seconds` | required | the server never reads the audio, so it needs the length to know when the piece ends |
| `weight` | `10` | relative chance |
| `volume` | `1.0` | 0.0 - 4.0 |
| `models` | `[]` | maid model ids. Empty means anyone; otherwise only a maid wearing one of them can play it |

The audio itself comes from a resource pack: a **mono** `.ogg` (stereo does not attenuate with
distance), with a `sounds.json` entry using `"category": "record"` and `"stream": true`.

### Character themes

Give a piece a higher weight and list the models that own it:

```json
{
  "sound": "mypack:keyboard.reimu_theme",
  "length_seconds": 150,
  "weight": 30,
  "models": ["touhou_little_maid:hakurei_reimu"]
}
```

A maid in that model picks from the general pieces plus her own, so her theme turns up much more often
than any single general piece. Players only ever get the general pieces.

The loaded tracks are logged at server start (`Loaded N keyboard track(s)`).
