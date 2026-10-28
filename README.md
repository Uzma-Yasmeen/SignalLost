# Signal Lost

**A turn-based stealth game in Java. You escape a facility you can only partly see.**

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Tests](https://img.shields.io/badge/tests-200%20passing-success)
![No dependencies](https://img.shields.io/badge/runtime%20dependencies-none-blue)
![Licence](https://img.shields.io/badge/licence-MIT-green)

<p align="center">
  <img src="docs/screens/02-explored.png" width="820"
       alt="Part way through a level: a lit area around the player, a dimmer trail of remembered corridors, and black everywhere else.">
</p>

Collect the keys, reach the exit, stay out of the guards' sight. You see only
what your character can see — everything else is either a dim memory of
somewhere you have already walked, or black. Patrollers follow fixed routes and
raise an alarm when they spot you. A hunter answers that alarm, and it moves
faster than you do, so running is not a plan; breaking its line of sight is.

Ten levels, six characters, three lives, and a star rating that rewards getting
out quickly without being seen.

## What is worth looking at

- **The rules know nothing about the screen.** 2,662 lines of game logic
  without a single reference to Swing or AWT — and
  [a test](src/test/java/com/signallost/ArchitectureTest.java) that fails the
  build if anybody adds one. The whole suite runs without opening a window, and
  the same logic would run on a phone behind a new front end.
- **Two searches, doing different jobs.** A breadth-first flood fill for the fog,
  so sight bends around corners instead of shining through walls; A\* for the
  hunter's chase. The hint system reuses that same pathfinder over a weighted
  cost map, so what it suggests is a move that avoids being seen rather than the
  shortest move.
- **Undo that is exact.** Every turn is a `Command` holding immutable snapshots
  of the guards, so `U` rewinds the patrol routes, the alarm, the sighting count
  and the clock — not just the player.
- **Levels proven winnable rather than hoped to be.** The game is fully
  deterministic, so a breadth-first search of the entire game tree can answer
  whether a winning line exists at all, and what the fastest one is. That is
  where the star thresholds come from, and it is how two levels that were
  quietly impossible got found.
- **Pixel art as source code.** Every sprite is a 16×16 block of characters in a
  `.java` file, coloured by the character that owns it, so a new animal is a
  block of text that shows up properly in a diff.

## Screenshots

<table>
<tr>
<td width="33%"><img src="docs/screens/03-spotted.png" alt="A guard's line of sight drawn in red across the corridor the player is standing in."></td>
<td width="33%"><img src="docs/screens/04-hint.png" alt="The suggested next move, marked on the board."></td>
<td width="33%"><img src="docs/screens/09-level-select.png" alt="A grid of level boxes, each showing the stars earned on it."></td>
</tr>
<tr>
<td><strong>Spotted.</strong> A patroller's line of sight is drawn in red, the
alarm goes up, and a hunter starts moving towards where you were seen.</td>
<td><strong>A hint, for one turn.</strong> One clever move — away from a
hunter, or on towards a key — never the whole route, and it expires as soon as
you move, so it cannot walk the level for you.</td>
<td><strong>Progress.</strong> Stars per level, saved per player name, with
later levels unlocked by the stars you have collected.</td>
</tr>
</table>

Every image here is a render of the real interface, produced by `ScreenshotTool`
rather than captured by hand, and the walks in them are driven by A\* rather
than hard-coded key presses — so they keep working when a level gets redrawn.
The full set, including the escape screen, a capture, the character roster and
four level shapes with the fog lifted, is in [docs/screens](docs/screens).
Regenerate it with:

```bash
mvn -q test-compile
java -cp "target/classes;target/test-classes" com.signallost.tools.ScreenshotTool docs/screens
```

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
| `Esc` | back to the level select |
| `C` | go back to the character select |

Tests:

```bash
mvn -q test
```

200 tests, covering visibility, fog memory, movement, undo, pathfinding, guard
behaviour, the alarm, capture, lives, keys, scoring, hints, spotting, the turn
limit, level integrity and the window's select-to-level wiring.

## Screen flow

```
home  ->  name  ->  character  ->  level select  ->  the level
                                        ^                |
                                        +----- Esc ------+
```

Esc steps back one screen at a time, so nowhere is a dead end. The home screen
greets a returning player by name with their star count, because a game that
opens straight into a text box tells you nothing about itself.

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

## Score

Escaping earns one star. Two more are on offer:

| Star | Earned by |
| --- | --- |
| ★ | getting out at all |
| ★ | getting out inside **par** |
| ★ | getting out **never having been seen** |

Two separate bars rather than one blended number, so a player who got two stars
can read which one they missed and go back for it. That is the whole point of
the sightings counter: it used to be a number that rose and never paid off,
which teaches a player to ignore it.

**The two bonus stars pull against each other, and that is a trap.** Avoiding a
guard costs turns, so a par set from the guard-free route can make them mutually
exclusive. On level 1 the fastest escape took 38 turns, the fastest *unseen*
escape took 56, and par was 51 — three stars was unreachable no matter how well
anybody played, and the only way to discover that was to keep failing at it.
`StarRatingAchievableTest` searches for the shortest unseen escape and insists
par leaves room for it. Par is now 69 against a 56-turn minimum.

**Par is worked out from the level itself**, not typed into the level file:
`RouteMetrics.shortestClearRoute` walks start to every key to exit ignoring
guards, and par is that plus 40%. It cannot drift out of date when the walls
move, and the same helper feeds `LevelIntegrityTest`, so the test and the rating
can never disagree about what a level costs. Level 1's clear route is 36 turns,
so par is 51 against a limit of 150.

This is also the answer to "why replay a level you have beaten". The first run
is about getting out. The second is about getting out clean.

## Hints

Press `H` and an arrow appears on the tile to step onto next. That is the whole
hint: one direction, from where you are standing, **for that turn only**. Move,
and it is gone; press `H` again if you want another.

It was a toggle to begin with, which meant one press left the arrow answering
every turn for the rest of the level. That is the whole route again, just
delivered a tile at a time. A hint you have to ask for each time is a hint; one
that keeps talking is a walkthrough.

**What it points at depends on the trouble you are in.** Out of sight, it points
along the safest route to the nearest key, then the exit, and the arrow is blue.
Standing in a guard's sight, it points at the quickest way **out of view**, and
the arrow is amber. Telling a player the key is east is no use when they are
about to be caught going there — the progress they would make is progress they
are about to lose.

It used to draw the next seven tiles as a fading trail, and that was the wrong
shape of help. A route drawn across the map answers the level rather than the
turn, most of it runs through fog the player cannot see into, and reading it is
a different job from playing. An arrow says one thing, and they can ask again
after they have moved.

The route behind it is A\* with **weighted costs**: tiles a guard is watching
cost extra, and tiles near a guard cost more the closer it is — far more when
that guard is hunting. Avoiding only what a guard can see right now is not
advice you can survive on, because a hunter covers ground faster than the
player, so by the time you reach a tile it was merely near, it is looking at
you. The arrow aims at the **nearest** key still on the floor, then at the exit.

It is deliberately omniscient, accounting for guards you cannot see. That is the
point of a hint, and the reason it is something you choose to turn on.

## Profile and progress

The game asks who is playing, then keeps that player's best run on every level
in `~/.signal-lost/progress.properties` — plain text, no library, readable and
deletable by hand.

**It lives in the player's home folder, not beside the game.** Nothing is
carried along when the game is copied or shared, so somebody opening it on
their own machine finds no file and starts at zero.

**Each name gets its own progress.** One shared profile meant a friend who sat
down at your laptop and typed their name inherited your stars, and saving
overwrote yours with theirs. Keyed by name, both of you keep what you earned,
and swapping back restores it:

```
player.last=sam
player.sam.name=Sam
player.sam.level1.stars=3
player.sam.level1.turns=58
player.sam.level1.sightings=0
```

Only an improvement is kept. More stars always wins; between runs with the same
stars, fewer turns wins, then fewer sightings. That matters here more than in
most games, because the stealth star rewards a slow careful route — without a
best-of rule, a player chasing it would watch their fast two-star time get
overwritten by every failed attempt.

Nothing in the store throws at the caller. A profile that cannot be read is a
fresh profile, and one that cannot be written is a lost high score; neither is
worth taking the game down for.

## Level select

One box per level, showing the stars earned on it and the best run. A list of
level names tells a player what exists; a grid of boxes with two of three stars
filled in tells them what is left to do, which is the only reason to replay a
level they have already beaten. Levels are locked until the player has enough
stars across all of them, so stars are a currency rather than decoration.

The boxes are numbered by their position in the list rather than by anything
parsed out of a file name. Those two drifted apart once: the levels were ordered
by unlock cost while being labelled from their file names, so the grid read
1, 3, 3, 6 and looked like level 2 had gone missing.

## Levels

Ten levels ship, numbered in play order, each a different shape:

| | Shape | | | Shape |
| --- | --- | --- | --- | --- |
| 1 | lattice of corridors and pillars | | 6 | open field, sparse pillars |
| 2 | four rooms joined by narrow doors | | 7 | nine rooms in a grid |
| 3 | concentric rings | | 8 | twin halls, three crossings |
| 4 | a hall of pillar blocks | | 9 | nested vault, four rings deep |
| 5 | stacked chambers, offset doors | | 10 | the large facility |

They cost 0, 1, 3, 5, 8, 11, 14, 17, 20 and 23 stars to open, out of thirty.

The first three were all lattices to begin with, because a lattice trivially
passes the fairness check — there is cover everywhere by construction. Safe, and
three levels that played identically.

Varying the shape meant levels that could genuinely be wrong, which is what the
checks are for. Between them they caught the ring level with its centre sealed
behind a doorless ring, then with its middle ring cut off from the outer one,
and the chamber level with its bottom room split in two and the only door on the
wrong side of the split. None of those reached a player.

The file names *are* the play order, and the select screen numbers its boxes by
position rather than by anything parsed out of a name. Those two drifted apart
once: the levels were ordered by unlock cost while being labelled from their
file names, so the grid read 1, 3, 3, 6 and looked like level 2 had gone
missing. A locked box shows its own number with the cost underneath, rather than
showing the cost as the number.

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

## Telling the player what the keys do

The control strip across the bottom names every key. It used to be dim grey on
a dark bar, which drew the fair question of how anybody is supposed to know
that `H` gives a hint — they were not, because the strip was legible only if
you already knew to look at it. The keycaps are bright, the captions are
full-strength text, and an accent line separates the strip from the board.

**The window resizes and the board follows it.** Drag it, maximise it, and the
tiles scale to whatever space there is, recomputed on every repaint. Every
screen centres its content, so none of them cling to a corner of a large
window.

**Nothing calls `pack()` after the window is up.** Packing a maximised window
resizes it to its preferred size, which un-maximises it — so every press of
Enter threw the player out of full screen. Once a window is on screen its size
belongs to the player, and the only thing worth doing automatically is growing
it when a screen genuinely needs more room.

It was fixed at whatever size `pack()` chose, and not resizable at all. On a
screen shorter than that, the control strip sat below the display with no way
to reach it — and the fix I reached for first was shrinking the tiles, which
solved the window by ruining the puzzle. The board is the game; the window
should bend around it.

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

## A bug worth keeping in the notes

Undo and restart silently stopped working, and no test caught it.

The window bound `U`, `R`, `H`, `M`, `C` and WASD to game actions, and then a
later loop bound every letter A-Z to "type this into the name box". The second
pass replaced the first in Swing's `InputMap`. The game ran, nothing threw, and
the keys did nothing.

The window test did not catch it because it fired actions **by name** out of the
`ActionMap`, which never goes through the `InputMap` where the collision lived.
A test that drives a thing through a different door than the user does can pass
while the user's door is bricked up.

Two things changed. `KeyBindingTest` now looks each key up in the `InputMap` and
runs whatever it finds, exactly as a keypress does. And `bindKey` throws if a key
is already bound, so the next version of this mistake fails loudly at startup
instead of quietly at the keyboard.

## Could this run on a phone?

Yes, and the reason is the package split rather than any plan for it. Every
external import across the 2,662 lines of game logic:

```
java.io.*            java.util.*
java.nio.charset.*   java.util.function.*
java.nio.file.*
```

All of that exists on Android — `java.nio.file` wants API 26, everything else
is older, and records and switch expressions are desugared by the Android
toolchain. Exactly one line would have to change: `ProgressStore.inUserHome`
asks for a home directory, which a phone does not really have, so Android
would hand it `context.getFilesDir()` instead. The constructor already takes a
path, so nothing else moves.

What would be rewritten is the 3,150 lines of Swing in `ui`, as a `Canvas`
view with touch controls. A new front end, not a new game.

That property is one `import` away from being lost forever, and nothing else
would notice — a model class reaching for `java.awt.Color` because it is a
handy way to hold a colour compiles fine and passes every other test.
`ArchitectureTest` fails the build if any logic package imports Swing or AWT,
and it was checked by planting such an import and watching it fail.

## How it was built

In order, each step working before the next one started:

- **Step 1 — done.** The map, the BFS fog of war, and the game screen.
- **Step 2 — done.** The Command pattern and the undo stack, plus six characters.
- **Step 3 — done.** A\* pathfinding, patrolling and hunting guards, hints, and
  pixel-art characters.
- **Step 4 — done.** Being spotted, the red line-of-sight overlay, and the turn
  limit.
- **Step 6 — done**, ahead of step 5. Ten levels, the home screen, player names,
  and stars saved per player.

## Still to build

| Step | What it adds |
| --- | --- |
| 5 | A sentinel guard with a rotating vision cone |
| 7 | SRS document and UML class and sequence diagrams |
| 8 | Phase 2: a Spring Boot and PostgreSQL leaderboard with a daily level |

## Build notes

Java 17 and Maven. The compiler targets 17 rather than 21 because that is what
`JAVA_HOME` points at on the development machine; nothing in the code needs 21.
