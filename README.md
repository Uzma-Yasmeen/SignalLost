# Signal Lost

**A turn-based stealth game in Java. You escape a facility you can only partly see.**

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Tests](https://img.shields.io/badge/tests-205%20passing-success)
![No dependencies](https://img.shields.io/badge/runtime%20dependencies-none-blue)
![Licence](https://img.shields.io/badge/licence-MIT-green)

<p align="center">
  <img src="docs/screens/02-explored.png" width="820"
       alt="Part way through a level: a lit area around the player, a dimmer trail of remembered corridors, and black everywhere else.">
</p>

Collect the keys, reach the exit, stay out of the guards' sight. You see only
what your character can see. Everything else is either a dim memory of
somewhere you have already walked, or black.

Ten levels, six characters, three lives, and a star rating that rewards getting
out quickly without being seen.

## What makes it awkward, in a good way

**You cannot see the level, so you cannot plan the level.** Sight is a
breadth-first flood fill from where you are standing, which means it stops at
walls and bends around corners. You learn a room by walking into it, and what
you learn stays on the map as a dimmer memory — accurate about walls, and
completely out of date about guards.

**You cannot outrun the hunter.** When a patroller spots you it raises an alarm,
and the hunter that answers it moves faster than you do. Running in a straight
line is arithmetic you lose. Breaking its line of sight is the only thing that
works, which turns every chase into a question about corners rather than speed.

**Undo rewinds the guards too.** `U` does not just step you backwards; it
restores the patrol positions, the alarm, the sighting count and the clock,
because every turn is a command object holding immutable snapshots of all of it.
You can genuinely take a move back and try the other door.

## Screenshots

<table>
<tr>
<td width="33%"><img src="docs/screens/03-spotted.png" alt="A guard's line of sight drawn in red across the corridor the player is standing in."></td>
<td width="33%"><img src="docs/screens/04-hint.png" alt="The suggested next move, marked on the board."></td>
<td width="33%"><img src="docs/screens/09-level-select.png" alt="A grid of level boxes, each showing the stars earned on it."></td>
</tr>
<tr>
<td><strong>Spotted.</strong> The patroller's line of sight turns red, the alarm
goes up, and a hunter starts for where you were seen.</td>
<td><strong>Three hints a level.</strong> One good move, never the whole
route, and it expires the moment you move.</td>
<td><strong>Progress.</strong> Stars per level, saved per player name, later
levels unlocked by the stars you have earned.</td>
</tr>
</table>

More in [docs/screens](docs/screens), all rendered from the real interface by a
tool rather than captured by hand.

## Playing it

On Windows, double-click `run.bat`. Otherwise:

```bash
mvn -q compile
java -cp target/classes com.signallost.Main
```

| Key | Does |
| --- | --- |
| arrow keys, or W A S D | move one tile, which costs one turn |
| space, or `.` | wait one turn, or claim your next life after a capture |
| `U` | undo the last turn: guards, sightings and the clock included |
| `H` | show one suggested move — three a level |
| `R` | restart the level |
| `C` | back to the character select |
| `Esc` | back to the level select |
| `M` | developer toggle: reveal the whole map |

Tests:

```bash
mvn -q test
```

## Under the hood

**The rules know nothing about the screen.** All 2,704 lines of game logic — the
fog, the pathfinding, the guards, the undo stack, the scoring — contain no
reference to Swing or AWT, and
[a test](src/test/java/com/signallost/ArchitectureTest.java) fails the build if
anybody adds one. That is why 200 tests run without opening a window, and why
putting this on a phone would mean writing a new front end rather than a new
game.

Two searches do the work: a flood fill for what you can see, and A\* for the
hunter's chase. The hint system reuses that same pathfinder over a weighted cost
map, so what it suggests is a move that avoids being *seen*, not the move that
is shortest.

Every sprite is a 16×16 block of characters in a `.java` file, coloured by the
animal that owns it — so a new character is a block of text that shows up
properly in a diff.

## Two things that went wrong, and what they taught

**Level 1 could not be won, and 200 passing tests had nothing to say about it.**
The game is deterministic, so I wrote a solver that walks the entire game tree
breadth-first and answers one question: is there a winning line at all? It found
that there wasn't — and then I found two bugs in the solver itself, both of
which had confidently reported winnable levels as lost. A test suite that passes
tells you the rules work. It does not tell you the game can be won.

**Undo and restart silently stopped working, and no test noticed.** The window
bound `U` and `R` to game actions, then a later loop bound every letter A–Z to
"type this into the name box", quietly replacing them. The window test missed it
because it fired actions *by name* out of the `ActionMap`, never touching the
`InputMap` where the collision lived. A test that drives a thing through a
different door than the user does can pass while the user's door is bricked up.

Both stories in full, with the fixes, in [docs/testing.md](docs/testing.md).

## Design notes

The detail that used to live here, moved somewhere it can be read on purpose:

| | |
| --- | --- |
| [How the game is put together](docs/architecture.md) | package layout, the two searches, the fog, the Command pattern, guard behaviour |
| [The rules, and why they are these rules](docs/game-design.md) | being spotted, scoring, hints, saved progress, the level format, the pixel art |
| [Proving it works](docs/testing.md) | the game-tree solver, and the bug that beat the test suite |
| [Running it somewhere that is not a desktop](docs/portability.md) | what an Android port would actually have to change |

## Still to build

| Step | What it adds |
| --- | --- |
| 5 | A sentinel guard with a rotating vision cone |
| 7 | SRS document and UML class and sequence diagrams |
| 8 | Phase 2: a Spring Boot and PostgreSQL leaderboard with a daily level |

## Licence

MIT. See [LICENSE](LICENSE).
