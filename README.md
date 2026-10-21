# Signal Lost

A turn-based fog-of-war stealth strategy game in Java. You escape a facility you
can only partly see: grab the key, reach the exit, and stay out of the guards'
sight.

Built step by step.

- **Step 1 — done.** The map, the BFS fog of war, and the game screen.
- **Step 2 — done.** The Command pattern and the undo stack, plus six characters.
- **Step 3 — done.** A\* pathfinding, patrolling and hunting guards, hints, and
  pixel-art characters.
- **Step 4 — done.** Being spotted, the red line-of-sight overlay, and the turn
  limit.
- **Step 5 — next.** A sentinel guard with a rotating vision cone.

## Running it

```bash
mvn -q exec:java
```

Or run `com.signallost.Main` from your IDE. Pick a character, then:

| Key | Does |
| --- | --- |
| arrow keys, or W A S D | move one tile, which costs one turn |
| space, or `.` | wait one turn |
| `U` | undo the last turn: guards, sightings and the clock included |
| `H` | show or hide the suggested route |
| `M` | developer toggle: reveal the whole map |
| `R` | restart the level |
| `C` | go back to the character select |

Tests:

```bash
mvn -q test
```

80 tests, covering visibility, fog memory, movement, undo, pathfinding, guard
behaviour, hints, spotting, the turn limit, level integrity and the window's
select-to-level wiring.

## How the code is laid out

| Package | Holds | Why it is separate |
| --- | --- | --- |
| `model` | `Position`, `TileType`, `Direction`, `GameMap`, `GameState`, `Command`, `MoveCommand`, `GuardMoveCommand`, `TurnCommand` | the rules, with no drawing code in them |
| `vision` | `VisibilityService`, `FogOfWar`, `TileVisibility` | the BFS and the memory built on it |
| `pathfinding` | `AStarPathfinder` | shortest routes, for guards and hints |
| `guard` | `Guard`, `Patroller`, `Hunter`, `GuardMode`, `GuardState`, the behaviours, `GuardFactory` | who is chasing you and how they decide |
| `level` | `Level`, `LevelLoader` | reads levels from plain text files |
| `ui` | `GameFrame`, `GamePanel`, `CharacterSelectPanel`, `HudBar`, `FooterBar`, `Critter`, `CritterArt`, `CritterRenderer`, `Palette`, `Fonts` | Swing, which only reads the model |

Every rule in `model`, `vision`, `pathfinding` and `guard` is unit tested without
opening a window. That is the whole argument for a turn-based design over a
real-time one, and it is why the test suite can assert things like "a hunter
cannot see through a wall" directly.

## The two searches

The project runs breadth-first search and A\* side by side, which makes the
difference between them concrete rather than theoretical.

**BFS, for sight.** `VisibilityService.sightDistancesFrom` floods outward from a
tile one step at a time. A wall is *seen* when the flood reaches it, but the
flood does not continue through it, so the wall hides what is behind. Because
the flood travels along open tiles rather than in straight lines, sight bends
round corners the way a corridor does. Spreading evenly in every direction is
exactly what you want from light, and exactly what you do not want from a chase.

**A\*, for chasing.** `AStarPathfinder` is BFS that guesses. It keeps a priority
queue ordered by *cost so far + estimated cost remaining* and always expands the
most promising tile, so it walks almost straight at its goal instead of flooding
the level. The estimate is Manhattan distance, which can never overestimate a
four-directional route — the condition under which A\* is guaranteed to return a
shortest path rather than merely a path.

Guards see with the first and move with the second. One shared, tested
visibility algorithm is also the reason a guard can never see through a wall the
player is hiding behind: there is no second implementation to get it wrong.

## Fog of war

`FogOfWar` remembers every tile the flood has ever reached, which gives the three
shades on screen:

- **lit** — in sight this turn, and dimmer the further the flood had to travel
- **remembered** — seen earlier, drawn flat and dark
- **unexplored** — never seen, drawn as void

## Undo, and the Command pattern

Nothing writes to `GameState` from outside its package. A player action becomes a
`MoveCommand`, which snapshots what it is about to change, applies itself, and
goes onto a stack. Undo is a pop followed by `Command.undo()`.

A whole turn is a `TurnCommand`: the player's action, then a `GuardMoveCommand`
for each guard, undone in reverse. **This is why undo was built before the
guards.** Adding enemies did not mean rewriting the turn system; enemies just
became more commands inside the turn. One press of `U` rewinds the player's step,
every guard's step, and every guard's state of mind.

One thing is deliberately **not** reversed: the fog's memory. Taking a move back
un-walks the step and gives the turn back, but the player has still seen what
they saw, so the remembered map stays.

## Guards

| Type | Sight | Behaviour |
| --- | --- | --- |
| Patroller | 3 | Walks its route and never reacts, even with the player in plain view |
| Hunter | 4 | Chases what it sees, searches where it last saw you, then gives up |

The hunter is a small state machine:

```
PATROL  --sees the player-->  HUNT
HUNT    --loses sight------>  ALERT   (remembering where you were)
ALERT   --sees the player-->  HUNT
ALERT   --patience runs out or reaches the spot-->  PATROL
```

Each mode delegates its move to a `GuardBehaviour` (Strategy), and the hunter
swaps between them (State). `AlertBehaviour` is what makes hiding worth doing:
without it, breaking line of sight would be a complete escape.

A guard's whole turn is one immutable `GuardState` — position, mode, patrol
progress, and the memory of where you were. That is what makes undo exact: the
turn is a swap, so taking it back is swapping the old value straight back.

## Being spotted, and the clock

Tiles a guard can see are washed **red** — any guard, whatever it is doing, so
red always means one thing: do not end your turn here. How much trouble the guard
itself is in is said by its own colour instead, calm blue on patrol through amber
to red while hunting.

**You are spotted if a turn *ends* with you in sight.** The check happens after
the guards have moved, which is the whole tension: stepping somewhere safe is not
enough, because a guard may turn a corner and be looking at you by the time the
turn ends.

Being seen is **not** the end of the run. It is counted, and `U` takes the move
back, which makes the game a puzzle you work out rather than one you lose by
surprise. The count is what a star rating will be built on.

A level can set a turn limit with a `turns` line. Running the clock out ends the
run; reaching the exit on the very last turn, or while a guard is watching, still
counts as getting away. Both the sighting count and the failure are inside the
`TurnCommand`, so undo takes them back along with everything else.

## Hints

Press `H` for a trail of dots towards whatever you need next — the key, or the
exit once you hold it.

The route is A\* again, but with **weighted costs**: tiles a guard is watching
cost extra, so the hint prefers a longer way round over a short way into trouble.
It is deliberately omniscient, accounting for guards you cannot see, and it only
shows the next seven steps. It points, it does not solve.

## Levels

A level file is the grid, then `---`, then one guard per line:

```
#########################
#@....#.........#.......#
...
#########################
---
turns 250
patroller 1,7 1,15 2,15 2,7
hunter 13,10 13,19
```

| Symbol | Means |
| --- | --- |
| `#` | wall |
| `.` | floor |
| `@` | player start |
| `K` | key |
| `E` | exit |

A guard line is a type then its waypoints as `row,col`; the first is where it
starts, and the route loops. A `turns` line sets the turn limit, and without one
the level is untimed. Every row must be the same width, and a level needs both a
`@` and an `E`.

`LevelIntegrityTest` uses A\* to assert the key and exit are actually reachable
and every guard can walk its route. It is worth having: it caught level 1 being
genuinely unfinishable, with the key sealed in a room with no door. A companion
test checks the turn limit leaves room to play — level 1's optimal route is 130
turns, and the limit is 250, because a player who cannot see the map will not
walk it optimally.

## Character artwork

The six animals and the guards are **16×16 pixel art, written as text in the
source**. Each sprite is a block of characters where every letter names a colour:

```
"..O..........O.."
"..OAO......OAO.."
"..OBWEBBBBEWBO.."
```

The colours come from the `Critter` itself, so one grid produces a ginger cat and
a pink bunny — only the shapes are written out, which is why the set looks like a
set. Art lives in the repository as readable text, shows up properly in a diff,
and a new character is a block of text rather than a binary file nobody can
review. `PixelSprite` rounds one art pixel to a whole number of screen pixels
before centring, because stretching by a fractional amount is what makes pixel
art look wrong.

To use your own sprites instead, drop PNG files into
`src/main/resources/sprites` — see [the notes there](src/main/resources/sprites/README.md)
for names, frame numbering, sizing and where to find art you are allowed to use.
The game picks them up at startup with no code change, animates them if you
supply numbered frames, and scales them with nearest-neighbour so pixel art stays
sharp. Delete them and the built-in sprites come back.

The level itself stays dark on purpose: the fog is the game, and it only works in
the dark. The characters are where the colour goes.

## Screens

Renders of the real UI, produced by `ScreenshotTool` rather than captured by
hand, are in [docs/screens](docs/screens):

- `00-character-select.png` — the roster
- `01-start.png` — the opening position, sight hugging the corner
- `02-explored.png` — part way in, with a trail of remembered tiles
- `03-spotted.png` — caught in a hunter's sightline, with the clock running
- `04-hint.png` — the suggested route, routing around guard sight
- `05-dev-reveal.png` — the whole map with every guard, used to check the logic

The walks are driven by A\*, not hard-coded key presses, so they keep working
when the level is redrawn. Regenerate with:

```bash
mvn -q test-compile
java -cp "target/classes;target/test-classes" com.signallost.tools.ScreenshotTool docs/screens
```

## Still to build

| Step | What it adds |
| --- | --- |
| 5 | A sentinel guard with a rotating vision cone |
| 6 | Multiple levels, star rating, title and level-complete screens |
| 7 | SRS document and UML class and sequence diagrams |
| 8 | Phase 2: a Spring Boot and PostgreSQL leaderboard with a daily level |

## Build notes

Java 17 and Maven. The compiler targets 17 rather than 21 because that is what
`JAVA_HOME` points at on the development machine; nothing in the code needs 21.
