# The rules, and why they are these rules

What being seen costs you, how the stars are worked out, what a hint is allowed to tell you, and how the levels are built.

[&larr; back to the README](../README.md)

## Screen flow

```
home  ->  name  ->  character  ->  level select  ->  the level
                                        ^                |
                                        +----- Esc ------+
```

Esc steps back one screen at a time, so nowhere is a dead end. The home screen
greets a returning player by name with their star count, because a game that
opens straight into a text box tells you nothing about itself.

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
and it is gone.

**You get three a level, and that is all.** Unlimited hints made the budget
version of this section unnecessary and the puzzle optional: a player could ask
every single turn and be walked to the exit a move at a time, which is the
walkthrough problem again wearing a different hat. Three is enough to get
unstuck twice and still keep one for the part that actually goes wrong, and it
turns the hint into a decision about *when* rather than a key to lean on. The
counter sits in the bar at the top and turns amber on the last one.

They do not come back. Losing a life gives you a fresh attempt but not a fresh
set of hints, and undo cannot buy one back — either would make the limit a
formality, since asking and then undoing would be free. Dismissing a hint and
looking at it again before you move is free, though: the hint is bought for the
turn, not for the keypress.

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
`src/main/resources/sprites` — see [the notes there](../src/main/resources/sprites/README.md)
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
