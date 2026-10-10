# Chess PVP (Seated)

A rewrite of the ChessPVP addon for Touhou Little Maid (NeoForge 1.21.1) that replaces
crouch-right-clicking the board with sitting down at it.

- Right-click a side of a Gomoku, Chinese chess or chess board with an empty hand to sit there.
  You play that side for as long as you stay seated.
- Crouch to stand up. The board is kept as it is and anyone can take the free seat.
- First seat (opposite where the maid sits) moves first: black in Gomoku, red in Chinese chess, white in chess.
- Checkmate ends a chess or Chinese chess game: the board shows TLM's win/lose banner, both players get the
  winner in their action bar, and moves stop until someone resets. In Chinese chess having no legal move
  loses; in chess it is checkmate only when the side is in check, otherwise a stalemate draw message
  (no banner, since TLM has none for it).
- Draws work as in TLM's maid games: the 60-move rule (Chinese chess) or 50-move rule (chess), and repetition,
  set TLM's draw flags so its banner shows and moves stop until a reset.
- Clicking the board's reset corner resets the game; both players stay seated.
- A maid sitting at the board hands control back to Touhou Little Maid's normal player-versus-maid game,
  and a maid will not sit down at a board someone is seated at.

Seats are Touhou Little Maid `EntitySit` entities. Their ids are stored in the board's persistent data
(`ChessPvpSeat1` / `ChessPvpSeat2`), so no new entities, blocks or packets are registered.

Based on ChessPVP 1.0.0 by zawu (All Rights Reserved). This is a private patch: do not redistribute it
without the original author's permission.

## Building and testing

Put a Touhou Little Maid 1.5.x NeoForge jar at `libs/touhoulittlemaid.jar` (not committed), then:

    ./gradlew.bat build              # jar in build/libs
    ./gradlew.bat runGameTestServer  # headless tests: seating, turns, leaving, retaking a seat, board removal, mate and draws

Remove the original `chesspvp-*.jar` from `mods` before installing this one (same mod id).
