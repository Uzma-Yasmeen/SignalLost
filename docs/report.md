# Signal Lost — A Fog-of-War Stealth Strategy Game

*Course project report source. `docs/build-report.py` turns this into the
formatted Word document. Diagrams are in [uml.md](uml.md) and are rendered to
images at build time.*

| | |
| --- | --- |
| Course | Software Engineering and System Design (24CS301PC311) |
| Programme | III Year, I Semester B.Tech-CSE |
| Student | Uzma Yasmeen |
| Hall ticket | `TO BE SUPPLIED` |
| Supervisor | `TO BE SUPPLIED` |
| Department | Computer Science & Artificial Intelligence |
| Institution | SR University, Warangal, Telangana, India |
| Session | 2026-2027 |

---

## 1. Abstract

Signal Lost is a turn-based stealth strategy game developed in Java. The player
must escape a facility that is hidden by a fog of war, collecting keys and
reaching an exit while avoiding guards. Visibility is computed as a
breadth-first flood fill from the player's position, so that walls block sight
and tiles already visited remain on the map as a dimmer memory. Guards patrol
fixed routes and raise an alarm when they see the player, and a hunter responds
to that alarm by pursuing the player using A* search.

The project was undertaken to demonstrate graph algorithms, design patterns and
layered architecture in a single application. Because the game is turn-based and
deterministic, every decision it makes is plain logic that can be tested. Five
design patterns are used: Command for undoable turns, Strategy for guard
behaviours, State for guard modes, Observer for the alarm, and Factory for
guards and levels.

The completed system has ten levels, six playable characters, three lives per
run, a limited hint system, a star rating based on turns taken and times seen,
and saved progress for each player. It is verified by 205 automated tests in 25
test classes. These include an architectural test that fails the build if the
game rules ever refer to the user interface, and a fairness test that checks
every level can be completed. Level difficulty was calibrated using an
exhaustive breadth-first search of the game tree, which confirmed that a winning
route exists on every level.

**Keywords:** fog of war, breadth-first search, A* pathfinding, design patterns,
layered architecture, automated testing.

---

## 2. Introduction

### 2.1 Problem Statement

Most student game projects are built in real time, and real time hides the
software engineering. When a character moves across the screen sixty times a
second, the important decisions — what can be seen, where an opponent moves,
whether a rule was broken — are buried inside animation code. Such a project is
difficult to explain, difficult to draw in UML, and very difficult to unit test,
because almost nothing can be separated from the rendering that drives it.

There is a second problem with games built this way. Nobody can prove they are
playable. A level is considered finished when its author manages to complete it,
which treats "difficult" and "impossible" as the same thing. A level whose only
key sits behind a door that never opens will pass every unit test written for
it, because unit tests check that the rules behave correctly, not that the game
built from those rules can be won.

Signal Lost addresses both problems. The game is turn-based, so every decision
is a separate step rather than a frame of animation. The rules are written
without any reference to the display, so they can be tested without opening a
window. And because the game is deterministic, it can be searched rather than
only played, which allows the question "can this level be completed?" to be
answered directly.

### 2.2 Objectives

1. Implement fog of war as a graph traversal, using a breadth-first flood fill
   bounded by a sight radius and blocked by walls, with seen terrain kept as
   memory separate from current visibility.
2. Implement guard pursuit using A* search with a Manhattan distance heuristic
   and a priority queue, with deterministic tie-breaking.
3. Apply design patterns where they are genuinely needed rather than for
   display: Command, Strategy, State, Observer and Factory.
4. Keep the game rules independent of the user interface, and enforce that
   independence automatically.
5. Verify behaviour by automated tests covering visibility, pathfinding, undo,
   detection, capture, scoring and saved progress.
6. Verify playability by exhaustive search, confirming that every level can be
   completed and that its star ratings can be achieved.
7. Deliver a complete, playable game rather than a technical demonstration.

### 2.3 Scope

The project covers a single-player desktop game with a grid-based level model,
breadth-first visibility with fog memory, patrolling and hunting guards with a
three-state mode machine, an observer-driven alarm, A* pathfinding, a command
stack supporting exact undo, collectible keys that unlock an exit, three lives
per run, a limited hint system, a star rating, ten levels with star-based
unlocking, saved progress for each player, a Swing user interface, and an
automated test suite.

The system does not use a network, does not store data remotely, and has no
account system, multiplayer or sound. A Spring Boot and PostgreSQL leaderboard
is planned as a second phase and is not implemented.

One of the three guard types described in the original proposal, a stationary
sentinel with a rotating vision cone, is not implemented. The extension points
it would use already exist. Other gaps are recorded in Section 8.3 and in the
accompanying Software Requirements Specification.

---

## 3. Software Requirements Specification

This section summarises the requirements. The full specification, with rationale
for each requirement and a complete traceability matrix, is held in the
repository as `docs/srs.md`.

Each requirement names the automated test that verifies it. Requirements marked
**(D)** are derived: they were not in the original proposal and were added after
testing or play revealed a fault.

### 3.1 Functional Requirements

**Visibility and fog of war**

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-1.1 | Visible tiles are found by breadth-first flood fill from the player, limited to a sight radius of 5 | `VisibilityServiceTest` |
| FR-1.2 | Walls block the fill, so sight does not pass through them | `VisibilityServiceTest` |
| FR-1.3 | A wall next to a visible tile is itself visible, so rooms appear enclosed | `VisibilityServiceTest` |
| FR-1.4 | A tile once seen stays drawn as remembered terrain, without its occupants | `FogOfWarTest` |
| FR-1.5 | A developer toggle reveals the whole map for testing | `FogOfWarTest` |

**Turns and movement**

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-2.1 | A turn is one player action followed by one move from every guard | `TurnWithGuardsTest` |
| FR-2.2 | The player moves one adjacent tile per turn, or waits | `GameStateTest` |
| FR-2.3 | A move into a wall or off the grid is refused and costs no turn | `GameStateTest` |
| FR-2.4 | A move onto a guard is refused; guards block movement | `TurnWithGuardsTest` |
| FR-2.5 | Each level has a turn limit, after which the attempt fails | `TurnLimitTest` |

**Guards, detection and capture**

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-3.1 | A patroller walks a fixed route, reversing at each end | `GuardTest` |
| FR-3.2 | A hunter moves towards the player's last known position using A* | `GuardTest`, `AStarPathfinderTest` |
| FR-3.3 | A guard is in exactly one of three modes: PATROL, ALERT, HUNT | `GuardTest` |
| FR-3.4 | **(D)** A hunter moves two tiles on alternate turns, so it gains on a fleeing player | `GuardTest` |
| FR-4.1 | A turn ending inside a guard's line of sight counts as a sighting | `SpottingTest` |
| FR-4.2 | A sighting raises an alarm, which puts every hunter into HUNT mode | `AlarmTest` |
| FR-4.3 | Sightings are counted for the whole run, not per attempt | `ScoreTest` |
| FR-5.1 | A guard in HUNT mode reaching the player captures them | `CaptureTest` |
| FR-5.2 | **(D)** A guard not in HUNT mode blocks the player but does not capture | `CaptureTest` |
| FR-5.3 | A run starts with three lives; a capture costs one | `LivesTest` |
| FR-5.4 | A new life resets player, keys, guards and clock, but not the sighting count | `LivesTest` |

**Keys, undo and hints**

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-6.1 | A level may hold several keys, and all are required | `MultipleKeysTest` |
| FR-6.2 | The exit stays closed until every key is held | `MultipleKeysTest` |
| FR-7.1 | Every turn is recorded as a command object on a stack | `UndoTest` |
| FR-7.2 | Undo restores player, guards, alarm, sighting count, keys and clock | `UndoTest` |
| FR-7.3 | Undo repeats back to the start of the attempt, and is refused once it has ended | `UndoTest` |
| FR-8.1 | A hint shows one move, never a route, and expires when the player moves | `HintTest`, `HintLifetimeTest` |
| FR-8.2 | When the player is in a guard's sight, the hint points towards cover | `HintCoverTest` |
| FR-8.3 | **(D)** A level allows three hints, and neither undo nor a new life restores one | `HintBudgetTest` |

**Scoring, levels and saved progress**

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-9.1 | Escaping scores one star, escaping within par a second, escaping unseen a third | `ScoreTest` |
| FR-9.2 | Par is calculated from the level's own geometry, not written into the level file | `ScoreTest` |
| FR-9.3 | **(D)** Three stars can be achieved on every level | `StarRatingAchievableTest` |
| FR-10.1 | Ten levels are provided, and every key and the exit are reachable in each | `LevelIntegrityTest` |
| FR-10.2 | **(D)** Every watched tile has cover within reach, so being seen can always be survived | `LevelFairnessTest` |
| FR-10.3 | Levels unlock at 0, 1, 3, 5, 8, 11, 14, 17, 20 and 23 stars | `LevelIntegrityTest` |
| FR-11.1 | Progress is saved under a player name, keeping the best result per level | `ProgressTest` |
| FR-11.2 | Saving one player's progress does not affect another's | `ProgressTest` |
| FR-11.3 | Progress is written only to the local machine | `ProgressTest` |

### 3.2 Non-Functional Requirements

| ID | Category | Requirement | Verified by |
| --- | --- | --- | --- |
| NFR-1 | Performance | A turn resolves quickly enough to feel instant on a 29x21 grid | manual |
| NFR-2 | Maintainability | No class in the six rule packages imports `java.awt`, `javax.swing` or `java.applet` | `ArchitectureTest` |
| NFR-3 | Testability | The whole test suite runs without opening a window | `ArchitectureTest` |
| NFR-4 | Reliability | The game is deterministic: the same input from the same state gives the same result | `TurnWithGuardsTest` |
| NFR-5 | Portability | `ProgressStore` takes its storage path from the caller | `ArchitectureTest` |
| NFR-6 | Privacy | The game sends no data anywhere and needs no network | by inspection |

NFR-2 and NFR-4 are the two that carry the most weight. NFR-2 is what allows the
test suite to run without a display, and what would allow the rules to be reused
on another platform. NFR-4 is what makes the exhaustive search in Section 6.4
possible, and therefore what makes FR-9.3 and FR-10.1 checkable rather than
merely claimed.

### 3.3 Hardware and Software Requirements

| | Requirement |
| --- | --- |
| Java runtime | JDK 17 or later |
| Operating system | Any supporting Java SE with a display; developed on Windows 11 |
| Runtime libraries | None beyond the Java standard library |
| Build tool | Apache Maven 3.6 or later |
| Test framework | JUnit 5, test scope only |
| Version control | Git and GitHub |
| Processor | Any processor supporting a Java 17 runtime |
| Memory | 512 MB available to the JVM |
| Display | 1024 x 700 or larger |
| Storage | About 5 MB for the application, under 10 KB for saved progress |
| Input | Keyboard; the mouse is used only on the menu screens |

The display figure is a real lower bound. The board is deliberately not scaled
down to fit a smaller window, because reducing the tile size makes the puzzle
harder to read without making it easier to solve.

---

## 4. System Analysis

### 4.1 Existing System

There is no single system that Signal Lost replaces. What exists is a set of
common approaches to building games of this kind, each with weaknesses that this
project was designed to avoid.

**Real-time architecture.** The usual approach to a stealth game is a continuous
simulation driven by a frame loop, with floating-point positions, interpolated
movement and detection evaluated by casting rays many times a second. Three
engineering consequences follow. The logic cannot be separated from the
rendering, because state advances as a function of elapsed frame time. Behaviour
is not reproducible, because the same inputs produce slightly different results
on different hardware. And the design cannot be drawn honestly, because the
rules are spread across update methods rather than held in identifiable objects.

**Visibility by ray casting.** Line of sight is usually found by casting rays
from the observer and recording what each one hits first. This is fast, but it
computes line of sight literally, so visibility leaks through the diagonal gaps
between wall corners, and walls bounding a lit room are often left dark so that
rooms do not appear enclosed. A large body of corrective algorithms exists for
exactly these artefacts, which suggests the underlying model fits a grid
imperfectly.

**Verification by play.** A level is usually validated by its author completing
it. This shows that one person can win once. It does not show that the level can
be won in general, that its difficulty rating can be reached, or that it is not
in fact impossible. Unit tests, where they exist, usually check that individual
rules behave correctly, which is a much weaker claim.

### 4.2 Proposed System

| Problem | Solution in Signal Lost | Enforced by |
| --- | --- | --- |
| Logic cannot be tested without the UI | All game logic is in six packages that reference no user-interface type | `ArchitectureTest` |
| Behaviour is not reproducible | A discrete, deterministic turn model with no dependence on elapsed time | `TurnWithGuardsTest` |
| Patterns are obscured by the frame loop | Command, Strategy, State, Observer and Factory, each an identifiable set of classes | Section 5.3 |
| Ray casting leaks through corners | Visibility as breadth-first traversal of the grid graph | `VisibilityServiceTest` |
| Playability is only claimed | Exhaustive search of the game tree proves a winning route exists | `LevelIntegrityTest` |
| Difficulty thresholds may be unreachable | Star thresholds derived from level geometry and checked attainable | `StarRatingAchievableTest` |

The main advantages are that the rules can be tested in isolation, that a
failing scenario can be written down as a sequence of moves and reproduced
exactly, that visibility behaves correctly at corners without special cases,
that playability is demonstrated rather than assumed, and that moving the game
to another platform means writing a new interface rather than a new game.

Two costs were accepted. A turn-based model gives up the tension of continuous
movement. And visibility is recomputed each turn rather than updated
incrementally, which is more work per turn than necessary. Neither matters at
this grid size.

---

## 5. System Design

The diagrams are generated from Mermaid source held in `docs/uml.md`, so they
are version-controlled as text and cannot drift away from the code.

### 5.1 System Architecture

![System architecture](diagrams/5.1-system-architecture.png)

The system has two layers with one permitted direction of dependency. The
presentation layer reads game state and forwards input. The rules layer knows
nothing about it.

This is enforced rather than conventional. `ArchitectureTest` scans every source
file in the six rule packages and fails the build if any of them imports
`java.awt`, `javax.swing` or `java.applet`. The test was added after seeing how
easily the property could be lost: one model class using `java.awt.Color` as a
convenient way to hold a colour would compile, pass every other test, and tie
the rules permanently to the desktop.

### 5.2 Use Case Diagram

![Use case diagram](diagrams/5.2-use-case-diagram.png)

The player is the only human actor. Guards are a system actor, because they act
every turn without being invoked and because two of the most important events in
the game, the alarm and a capture, start with them. The include relationship
from *Move or wait one turn* to *Advance every guard* captures the central rule:
there is no player action that does not also give every guard a move.

### 5.3 Class Diagram

The class model is shown in three parts for legibility.

#### 5.3.1 Core model and the Command pattern

![Core model and the Command pattern](diagrams/5.3.1-core-model-and-the-command-pattern.png)

`GameState` holds the mutable position of play. Its mutable fields are
package-private, so only command objects in the same package can write to them.

The Command pattern is used at two levels. A `TurnCommand` represents one whole
turn and owns the smaller commands that make it up: a `MoveCommand` for the
player, a `GuardMoveCommand` for each guard, and an `AlarmCommand` if the player
was seen. Undoing a turn means undoing each part in reverse order, so nothing
can be missed.

#### 5.3.2 Guards: State, Strategy and Observer

![Guards: State, Strategy and Observer](diagrams/5.3.2-guards-state-strategy-and-observer-together.png)

Three patterns meet at the guard. `GuardMode` is the State and decides which
behaviour applies. `GuardBehaviour` is the Strategy, with three stateless
implementations selected by that mode. `AlarmListener` is the Observer, through
which one guard's sighting reaches every other guard without any of them holding
a reference to the others.

`GuardState` is an immutable record holding a guard's whole condition. A guard
does not change itself; it calculates a new state and has one applied. This is
what makes undo exact, because `GuardMoveCommand` restores the previous record
rather than trying to reverse each field.

#### 5.3.3 Vision, levels and progress

![Vision, levels and progress](diagrams/5.3.3-vision-levels-and-progress.png)

`VisibilityService` is stateless and calculates what is visible from a position.
`FogOfWar` adds memory, classifying each tile as `VISIBLE`, `REMEMBERED` or
`UNEXPLORED`. Keeping them separate makes the geometry testable on its own.

`LevelLoader` and `GuardFactory` form the Factory pattern, building a level and
its guards from a text file so that adding a level needs no code change.
`ProgressStore` is the only class in the rules layer that touches the file
system, and it takes its path from the caller.

### 5.4 Sequence Diagram

#### 5.4.1 Taking one turn

![Taking one turn](diagrams/5.4.1-one-turn.png)

This is the system's main flow. The ordering is where the difficulty of the game
comes from: the player acts, then every guard acts, and only then is it decided
whether the player was seen. Moving to a tile that is safe when you leave is
therefore no guarantee, because a guard may turn a corner and be looking at it
by the time the turn ends.

#### 5.4.2 Undoing a turn

![Undoing a turn](diagrams/5.4.2-undoing-a-turn.png)

Undo pops the turn and reverses its parts in the opposite order: the alarm
first, then each guard, then the player. Nothing is recalculated; everything is
restored from values captured at the time.

One omission is deliberate. The hint budget is not restored, because if it were,
three hints would be unlimited: a player could ask, undo, and repeat.

### 5.5 Activity Diagram

![Activity diagram](diagrams/5.5-activity-diagram.png)

The diagram follows one attempt at a level from entry to rating, including undo,
hints and restart, and both ways of failing: capture, which costs a life, and
the turn limit, which ends the attempt.

### 5.6 Guard State Machine

![Guard state machine](diagrams/5.6-guard-state-machine.png)

Only a guard in HUNT mode can capture the player. A patroller that walks into
the player blocks them and nothing more. This rule was adopted after the
stricter version made a single-width corridor on level 1 impossible to pass,
which made the level unwinnable.

---

## 6. Methodology

### 6.1 Development approach

Development was incremental, with each stage producing a working program rather
than a layer of an unfinished one.

| Stage | Delivered |
| --- | --- |
| 1 | Grid model, breadth-first fog of war, playable board |
| 2 | Command pattern, undo stack, six characters |
| 3 | A* pathfinding, patrolling and hunting guards, hints |
| 4 | Detection, line-of-sight overlay, turn limit |
| 6 | Ten levels, title screen, player names, saved progress |
| 7 | This report, the SRS and the UML diagrams |

Stage 5, the sentinel guard, was deferred in favour of stage 6, on the judgement
that a complete game with two guard types shows more than an incomplete one with
three. Stage 8, the Spring Boot leaderboard, is phase 2.

### 6.2 Version control

Git was used throughout and the repository is published on GitHub. There are
thirty-eight commits, each limited to one concern.

### 6.3 Testing strategy

Testing was done at four levels:

1. **Unit tests** on individual rules, such as visibility geometry, pathfinding
   and scoring.
2. **Integration tests** on whole turns, where the player acts, guards respond
   and the outcome is checked.
3. **Property tests** across all ten levels at once: that every key is
   reachable, that every watched tile has cover within reach, and that three
   stars can be achieved.
4. **Architectural tests** on the source itself, failing the build if the
   dependency rule in Section 5.1 is broken.

The order of events was often the reverse of the textbook one. Several of the
most significant faults were found by playing rather than by testing: a hunter
that escorted the player instead of catching them, a level whose only key stood
behind a door that never opened, and a star rating that could not be achieved.
In each case a test was written afterwards to prevent that kind of fault
returning. The test suite is therefore a record of mistakes already made as much
as a specification written in advance. The property tests at level 3 exist
precisely because unit tests had proved unable to find faults of that kind.

### 6.4 Verification by exhaustive search

Because the game is deterministic, the question "can this level be won?" has a
definite answer. A solver was written that performs a breadth-first search over
the whole game tree, with each state identified by player position, turn count
and the set of keys still to collect.

The solver established three things that testing had not. Level 1 was
unwinnable with both guards in place, which no amount of play would have
distinguished from a level that was merely hard. The star thresholds were
unreachable: on level 1 the fastest escape took 38 turns and the fastest unseen
escape took 56, against a par of 51, so the speed star and the stealth star
excluded each other. And the solver itself was wrong twice before it was right.
One version stored states without the turn count, which is unsound while a clock
is running, and a later one recorded how many keys were held rather than which,
so holding the first key and holding the third looked identical. Both versions
reported winnable levels as lost.

That last point is worth noting. A verification tool is software too, and is no
more trustworthy than the software it checks until it has itself been checked.

---

## 7. Implementation

### 7.1 Module organisation

| Package | Lines | Responsibility |
| --- | --- | --- |
| `model` | 1,148 | Game state, map, commands, undo stack, scoring |
| `guard` | 612 | Guard types, modes, behaviours, the alarm |
| `progress` | 284 | Player profiles and saved progress |
| `level` | 258 | Level and catalogue loading from text |
| `pathfinding` | 213 | A* search and route metrics |
| `vision` | 189 | Visibility and fog memory |
| **Total** | **2,704** | **No user-interface dependency** |

### 7.2 Fog of war

Visibility is a breadth-first flood fill from the player's tile, bounded by the
sight radius and blocked by walls. Walls next to a visible tile are marked
visible in the same pass, so rooms appear enclosed.

A flood fill was chosen over ray casting. Because it spreads only between
adjacent tiles, visibility cannot leak through the diagonal gap between two wall
corners, which ray-cast implementations have to correct for explicitly. Sight
bending around a corner follows naturally from the traversal.

### 7.3 Pathfinding and pursuit

A* is used over the grid graph, with a Manhattan distance heuristic and a
priority queue ordered by estimated total cost. Ties are broken deterministically
so identical situations always resolve identically.

A second version of the method accepts a cost for each tile. This is used by the
hint system rather than by the guards: tiles under observation cost more to
cross, and tiles near a hunting guard more still, so the route returned avoids
being seen rather than being shortest.

A hunter moving at exactly the player's speed can never close a gap. The first
version did this, so a chase stayed at whatever distance it started and the
hunter appeared to accompany the player rather than chase them. The fix was to
let a hunter take two steps on alternate turns, giving a net gain of half a tile
per turn. Running in a straight line now loses, and breaking line of sight wins.
A hunter that always took two steps was tried first and proved impossible to
escape, because capture arrived in four turns where the nearest cover was five
away.

### 7.4 Undo

Every turn is a `TurnCommand` holding a `MoveCommand`, one `GuardMoveCommand`
per guard, and an `AlarmCommand` where one was raised. Each sub-command keeps
the state it replaced. Guards keep an immutable `GuardState` record holding
position, mode, route index, last known player position, remaining alert turns
and surge status.

Undo therefore restores the guards, the alarm, the sighting count, the keys and
the clock, not only the player's position. What makes this exact is that guards
are never changed field by field, only replaced whole.

### 7.5 Saved progress and artwork

Progress is stored as a properties file in the user's home directory, keyed by
player name, holding the best result for each level. Writing merges rather than
overwrites, so one player's progress cannot disturb another's. A file that is
missing, empty or damaged is treated as "no progress" rather than shown as an
error. `ProgressStore` takes its path from the caller, so a platform without a
home directory can supply its own.

All sprites are 16x16 pixel art written as arrays of strings in Java source,
where each character names a colour supplied by the character that owns the
sprite. The same shape data therefore produces a ginger cat and a pink rabbit.
Artwork lives in the repository as readable text and can be reviewed like any
other source.

### 7.6 Output Screenshots

The images below are renders of the running game, produced by a tool that drives
the real game with A* rather than scripted keystrokes, so they cannot fall out
of step with the build.

![Level selection](screens/09-level-select.png)
**Figure 7.1 — Level selection.** Levels unlock against accumulated stars, and
each box shows the best rating achieved on it.

![Part way through](screens/02-explored.png)
**Figure 7.2 — Fog of war.** Bright tiles are currently visible, dim tiles are
remembered terrain, and black is unexplored.

![Spotted](screens/03-spotted.png)
**Figure 7.3 — Spotted.** The guard's line of sight is drawn in red, the alarm
is raised, and a hunter begins moving towards the sighting.

![A hint](screens/04-hint.png)
**Figure 7.4 — A hint.** One suggested move, shown for a single turn, routed to
avoid being seen rather than to be shortest.

![Escaped](screens/07-escaped.png)
**Figure 7.5 — Escaped.** The rating screen, showing turns taken against par and
the stars awarded.

![Developer reveal](screens/05-dev-reveal.png)
**Figure 7.6 — Developer reveal.** The whole map with every guard shown, used to
check guard behaviour against expectation.

---

## 8. Conclusion

### 8.1 Summary

Signal Lost was delivered as a complete, playable turn-based stealth game in
Java, with ten levels, six characters, two guard types, exact undo, a limited
hint system, star-based scoring and saved progress. The rules are 2,704 lines
across six packages and are verified by 205 automated tests in 25 test classes.

All seven objectives in Section 2.2 were met. Fog of war is a bounded
breadth-first flood fill, pursuit uses A* with a Manhattan heuristic and a
priority queue, five design patterns are applied where they carry structural
weight, the separation of rules from interface is enforced by a test, behaviour
is verified by testing, playability is verified by search, and the result is a
finished game.

### 8.2 What the project demonstrates

An architectural rule that nothing enforces will not survive. The separation
between rules and interface is the project's most useful property, and also one
that a single convenient import would quietly destroy. Writing a test that fails
the build on such an import turns an intention into a guarantee.

Passing tests and a working game are different claims. The project's most
instructive fault was a level that could not be won while every test passed,
because the tests checked that each rule behaved correctly and nothing checked
that the game built from those rules could be finished.

Tools need checking too. The solver written to validate the levels was itself
wrong twice, each time reporting winnable levels as unwinnable.

### 8.3 Limitations

- The sentinel guard with a rotating vision cone is not implemented.
- No performance budget is measured; NFR-1 is claimed rather than verified.
- Nothing in the game teaches its rules. A new player learns that red tiles are
  watched, and that only hunters capture, by being caught.
- No accessibility conformance is claimed. The line-of-sight overlay in
  particular conveys meaning by colour alone.
- Automated testing of the interface covers navigation and key bindings only;
  nothing checks what is drawn.

### 8.4 Future work

| Term | Work |
| --- | --- |
| Near | The sentinel guard; an in-game tutorial; a colour-blind-safe line-of-sight overlay |
| Phase 2 | The Spring Boot and PostgreSQL leaderboard with daily levels. `Score` and `LevelResult` are already plain data records for this purpose |
| Longer | An Android release. Because the rules import no user-interface type, this needs a new interface layer rather than a new game; the only rules-layer change identified is giving `ProgressStore` a platform-appropriate path |

---

## 9. References

1. Hart, P. E., Nilsson, N. J. and Raphael, B. (1968). "A Formal Basis for the
   Heuristic Determination of Minimum Cost Paths." *IEEE Transactions on Systems
   Science and Cybernetics*, 4(2), pp. 100–107.

2. Dijkstra, E. W. (1959). "A Note on Two Problems in Connexion with Graphs."
   *Numerische Mathematik*, 1(1), pp. 269–271.

3. Gamma, E., Helm, R., Johnson, R. and Vlissides, J. (1994). *Design Patterns:
   Elements of Reusable Object-Oriented Software*. Addison-Wesley.

4. IEEE Std 830-1998. *IEEE Recommended Practice for Software Requirements
   Specifications*. Institute of Electrical and Electronics Engineers.

5. Oracle Corporation. *Java SE 17 Documentation*.
   https://docs.oracle.com/en/java/javase/17/

6. The JUnit Team. *JUnit 5 User Guide*.
   https://junit.org/junit5/docs/current/user-guide/

7. Mermaid. *Diagramming and charting tool*. https://mermaid.js.org/

8. Yasmeen, U. (2026). *Signal Lost — source repository*.
   https://github.com/Uzma-Yasmeen/SignalLost
