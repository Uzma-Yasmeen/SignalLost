# How the game is put together

The shape of the code, the two searches that drive it, and why undo can rewind a guard.

[&larr; back to the README](../README.md)

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
