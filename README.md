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

On Windows, double-click `run.bat`, or from a terminal:

```bash
mvn -q compile
java -cp target/classes com.signallost.Main
```

`mvn -q exec:java` also works, but it needs the exec plugin, which Maven fetches
from the network the first time. The two lines above need nothing but the JDK
once the project has been built, so they are the ones to reach for. You can also
just run `com.signallost.Main` from your IDE.

That starts level 1, a short level meant to teach the rules. For the bigger one:

```bash
java -cp target/classes com.signallost.Main level2
```

Pick a character, then:

| Key | Does |
| --- | --- |
| arrow keys, or W A S D | move one tile, which costs one turn |
| space, or `.` | wait one turn, or claim your next life after a capture |
| `U` | undo the last turn: guards, sightings and the clock included |
| `H` | show or hide the suggested route |
| `M` | developer toggle: reveal the whole map |
| `R` | restart the level |
| `C` | go back to the character select |

Tests:

```bash
mvn -q test
```

117 tests, covering visibility, fog memory, movement, undo, pathfinding, guard
behaviour, the alarm, capture, keys, hints, spotting, the turn limit, level
integrity and the window's select-to-level wiring.

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
| Patroller | 3 | Walks its route, never chases, **cannot catch you** — but seeing you raises the alarm |
| Hunter | 4 | **Surges** after what it sees, answers alarms, searches, then gives up |

### Speed, and why you cannot run

A hunting guard **surges: two tiles, then one, then two again.** Getting that
number right took three tries, and the wrong answers were instructive.

At a flat **one tile** a turn it moved at exactly the player's speed, so once it
fell in behind you the gap froze forever. It could never close and you could
never shake it. It escorted you to the exit, which quietly made being seen cost
nothing at all.

At a flat **two** it gained a whole tile every turn — and measuring the levels
showed that was impossible to survive. In level 1 there are tiles where the
nearest cover is five steps away but capture would arrive in four. Being seen
there was death regardless of how well you played.

Alternating gains **half a tile a turn**. Running still fails, because the gap
does close, but spotted at the edge of a hunter's sight you have roughly eight
turns to get out of view. `LevelFairnessTest` checks every tile of every
hunter's patrol against that budget, so a level cannot ship with a corner that
cannot be escaped. The speed was set to fit the measurement rather than the
other way round, and the test caught a dead-end pocket in level 2 that had no
exit except along the hunter's own route.

**So the way out is breaking line of sight, not speed.** That is what turns the
fog from scenery into the thing that saves you.

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

**The alarm goes up.** The guard that saw you does not chase alone: the sighting
is broadcast to every guard on the map, and hunters answer it from wherever they
are. That is the Observer pattern doing real work, and it is what gives a
patroller teeth — it cannot chase anybody, but it can tell everyone who can, so
the dumbest guard in the facility is the most dangerous thing to walk past.

A hunter that can already see you ignores the report, because its own eyes are
better information.

**Only a hunting guard can catch you.** A guard that has seen you and is closing
in ends the attempt if it reaches your tile. Everyone else — a patroller, or a
guard that has lost you and is searching — is a **moving wall**: it blocks the
tile it stands on, it can raise the alarm, and it cannot kill. Nobody walks
through anybody, in either direction.

That distinction was not a preference, it was forced. An earlier rule made any
guard touching you fatal, on the reasoning that a rule with no exceptions is
easier to learn. Then a solver run showed the level had **no winning line at
all**: the corridors are one tile wide, so a patroller coming the other way was
unavoidable death delivered by something that could not even see you.

**You get three lives.** Being caught, or running the clock out, ends the
attempt and costs one. The player goes back to the start, the keys go back on
the floor, the guards go back to their posts and the clock goes back to zero —
a life is a clean attempt, not a wounded continuation. Sightings carry across,
because they score the run rather than the attempt.

**A capture cannot be undone.** Undo is for mistakes you notice, not the ones
that cost you a life; otherwise the lives counter would be decoration, since
you could rewind every death. Running out of lives ends the run.

Being seen is **not** the end of the attempt. It is counted, `U` takes the move back,
and undo calls the alarm off too — the facility should not stay alerted about a
sighting that, as far as the game is concerned, never happened. That makes this a
puzzle you work out rather than one you lose by surprise. The count is what a
star rating will be built on.

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

## Keys

The exit needs **every** key on the level, not one. Level 1 has three, in three
different corners.

One key made a level a straight line: find it, walk out, and after two attempts
you knew the route. Three force the player across the map and, more to the
point, back again past guards who are now looking for them. `GameState` counts
the keys on the map at load, so a level decides how many it wants simply by how
many it draws.

The hint points at the **nearest** key still on the floor, so it sends you to the
one in front of you rather than back across the level.

## Levels

**Level 1** is a lattice: corridors with pillars, so there is always a way
round. That matters more than it sounds — the previous level 1 had a single
one-tile chokepoint and its only route south ran the length of the hunter's
patrol with no cover, which a solver proved unwinnable. Three keys, 150 turns.
**Level 2** is the large facility, about 130 turns at its fastest. Level 1 was
built after playing level 2 first and finding it had no guard anywhere near the
start, so nothing happened for the first fifty turns.

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

`LevelIntegrityTest` runs **every** shipped level through A\*: the key must be
reachable from the start, the exit from the key, every guard must be able to walk
its route, and the turn limit must leave real slack over the optimal route,
because a player who cannot see the map will not walk it optimally. A new level
file is validated the moment it is added rather than when a player hits the wall.

It is worth having. It caught level 2 being genuinely unfinishable, with the key
sealed in a room that had no door.

## Character artwork

The six animals, the guards, the keys and the exit are all **16×16 pixel art,
written as text in the source**. Each sprite is a block of characters where every letter names a colour:

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
- `03-spotted.png` — caught, with the alarm up and a hunter answering it
- `04-hint.png` — the suggested route, routing around guard sight
- `05-dev-reveal.png` — the whole map with every guard, used to check the logic
- `06-caught.png` — a guard reached the player, and the run is over

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

## Proving it is actually playable

The game is deterministic, so it can be searched rather than guessed at. The
work that is not in this repository but shaped it most is a solver that walks
the entire game tree breadth-first and answers one question: **is there a
winning line at all?**

It has earned its keep three times, and twice at my own expense:

- It found that level 1 could not be won, with either guard present. No amount
  of play would have been enough to tell the difference between a hard level and
  an impossible one.
- It found two bugs in itself first. A depth-first version memoised states
  without the turn count, which is unsound while a clock is running; and the
  state key counted *how many* keys you held instead of *which*, so holding the
  first key and holding the third looked identical and perfectly good routes got
  pruned. Both versions confidently reported winnable levels as lost.
- Playing the level with two scripted players — one that follows the hint, one
  that only runs from hunters — showed the hint walked the player into a hunter,
  because it routed around where guards were looking rather than where they
  would be. The hint now weights proximity to a guard, heavily when it is
  hunting.

The lesson worth keeping: a test suite that passes tells you the rules work. It
does not tell you the game can be won.

## Build notes

Java 17 and Maven. The compiler targets 17 rather than 21 because that is what
`JAVA_HOME` points at on the development machine; nothing in the code needs 21.
