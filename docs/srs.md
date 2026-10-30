# Software Requirements Specification

**Signal Lost — a fog-of-war stealth strategy game**

| | |
| --- | --- |
| Version | 1.0 |
| Status | Phase 1 complete |
| Author | Uzma Yasmeen |
| Standard | Adapted from IEEE 830 |

[&larr; back to the README](../README.md)

---

## 1. Introduction

### 1.1 Purpose

This document specifies what Signal Lost does, for whom, and under what
constraints. It is written against the software as built rather than as
imagined: every functional requirement below names the automated test that
enforces it, and section 6 lists the requirements that are **not** met, which is
the part of an SRS that is usually quietly omitted.

The intended readers are the course assessor, any developer picking the project
up, and the author six months from now.

### 1.2 Scope

Signal Lost is a single-player, turn-based stealth puzzle game for desktop. The
player moves one tile per turn through a facility hidden by fog of war,
collecting keys and reaching an exit while avoiding guards that patrol and
hunt.

**In scope for phase 1 (this document):** the game rules, the fog, the guards,
the pathfinding, undo, hints, scoring, ten levels, per-player saved progress,
and a Swing user interface.

**Out of scope for phase 1:** any network communication, any server, any
account system, multiplayer, and sound. A Spring Boot and PostgreSQL
leaderboard is planned as phase 2 and is specified only as far as the
constraints it places on phase 1 (section 3.4).

### 1.3 Definitions

| Term | Meaning |
| --- | --- |
| **Tile** | One square of the grid, addressed by row and column |
| **Turn** | One player action followed by one move from every guard |
| **Fog of war** | The rule that the player sees only what is currently visible, remembers what was seen, and knows nothing of the rest |
| **Remembered tile** | A tile seen earlier and not currently visible: its walls are shown, its occupants are not |
| **Sighting** | A turn that ends with the player inside a guard's line of sight |
| **Alarm** | The event raised when a patroller sights the player, which puts hunters into HUNT mode |
| **Capture** | A hunting guard reaching the player, which costs a life |
| **Attempt** | One try at a level, ending in escape, capture, or the turn limit |
| **Run** | All three attempts at a level, taken together |
| **Par** | The turn count a level considers a brisk route, derived from its geometry |
| **Clear route** | The shortest path ignoring guards, used to derive par |

### 1.4 References

1. *Signal Lost: A Fog-of-War Stealth Strategy Game* — project overview (the brief this is written against)
2. [How the game is put together](architecture.md) — design notes
3. [The rules, and why they are these rules](game-design.md) — the reasoning behind section 3.2
4. [Proving it works](testing.md) — verification approach
5. [UML class and sequence diagrams](uml.md)

### 1.5 Overview

Section 2 describes the product in general terms. Section 3 states the
requirements, each with an identifier and the test that verifies it. Section 4
gives the traceability matrix. Section 5 covers verification. Section 6 is an
honest account of what is not built.

---

## 2. Overall description

### 2.1 Product perspective

Signal Lost is self-contained. It has no server, reads no network, and depends
on no library at runtime — only the Java standard library. JUnit 5 is a test
dependency and is not shipped.

The software is organised in two layers, and the boundary between them is a
requirement in its own right (NFR-2):

- **The rules.** Six packages — `model`, `vision`, `pathfinding`, `guard`,
  `level`, `progress` — holding every decision the game makes. 2,662 lines with
  no reference to Swing or AWT.
- **The interface.** One package, `ui`, which draws the rules and sends player
  input to them, and is the only part that knows a screen exists.

### 2.2 Product functions

| | Function |
| --- | --- |
| F1 | Reveal the map progressively as the player explores it |
| F2 | Move the player and advance the guards, one turn at a time |
| F3 | Detect the player, raise an alarm, and hunt |
| F4 | Capture the player, costing a life |
| F5 | Gate the exit behind keys |
| F6 | Undo a turn exactly, including the guards |
| F7 | Suggest one good move, a limited number of times |
| F8 | Rate a completed level from one to three stars |
| F9 | Unlock later levels with stars earned on earlier ones |
| F10 | Remember each player's best result per level, by name |

### 2.3 User characteristics

One class of user: a player, assumed to be able to operate a keyboard and read
English, with no prior knowledge of the game. No accessibility conformance is
claimed (see section 6.3).

### 2.4 General constraints

| | Constraint |
| --- | --- |
| C1 | Java 17. The compiler targets 17, not a later release |
| C2 | Desktop only in phase 1; Swing is the only UI toolkit used |
| C3 | No runtime dependency beyond the Java standard library |
| C4 | Saved data stays on the machine that produced it |
| C5 | The rules must not depend on the interface (see NFR-2) |

### 2.5 Assumptions and dependencies

- A JDK 17 or later is installed and on the path.
- The user's home directory is writable. If it is not, the game is fully
  playable and progress is simply not retained.
- The display is at least 1024×700. Below that the board is not reduced; the
  window scrolls instead, because shrinking the puzzle to fit the window
  defeats the puzzle.

---

## 3. Specific requirements

Each requirement is stated as a rule the software must obey, followed by the
test class that fails when it does not. Requirements marked **(derived)** were
not in the original brief and came out of playtesting.

### 3.1 External interface requirements

#### 3.1.1 User interfaces

| ID | Requirement | Verified by |
| --- | --- | --- |
| UI-1 | The game shall present screens in the order: title, name entry, character selection, level selection, level | `KeyBindingTest` |
| UI-2 | `Esc` shall return to the previous screen from any screen, so no screen is a dead end | `KeyBindingTest` |
| UI-3 | Every key bound to an action shall be named on screen during play | *manual* |
| UI-4 | No key shall be bound to two actions; a duplicate binding shall fail at startup rather than silently | `KeyBindingTest` |
| UI-5 | The board shall not be scaled down to fit a smaller window | *manual* |
| UI-6 | The status bar shall show lives, hints, keys, sightings and turns remaining | *manual* |

#### 3.1.2 Hardware interfaces

Keyboard only. The mouse is used for the level and character screens and for
nothing during play.

#### 3.1.3 Software interfaces

Level definitions are plain text files on the classpath. A level file is a grid
of characters, a `---` separator, then a turn limit and one line per guard.
Progress is a Java properties file. Both formats are specified in
[game-design.md](game-design.md).

### 3.2 Functional requirements

#### FR-1 — Visibility and fog of war

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-1.1 | Visible tiles shall be computed as a breadth-first flood fill from the player, limited to a sight radius of 5 | `VisibilityServiceTest` |
| FR-1.2 | Walls shall block the fill, so sight does not pass through them | `VisibilityServiceTest` |
| FR-1.3 | A wall bounding a visible tile shall itself be visible, so rooms read as rooms | `VisibilityServiceTest` |
| FR-1.4 | A tile once seen shall remain drawn as remembered, showing its terrain but not its occupants | `FogOfWarTest` |
| FR-1.5 | A developer toggle shall reveal the whole map, for testing the guard logic | `FogOfWarTest` |

#### FR-2 — Turns and movement

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-2.1 | A turn shall consist of one player action followed by one move from each guard, in that order | `TurnWithGuardsTest` |
| FR-2.2 | The player shall move one orthogonally adjacent tile per turn, or wait | `GameStateTest` |
| FR-2.3 | A move into a wall or off the grid shall be refused and shall not consume a turn | `GameStateTest` |
| FR-2.4 | A move onto a tile occupied by a guard shall be refused; guards block movement | `TurnWithGuardsTest` |
| FR-2.5 | Each level shall impose a turn limit, after which the attempt fails | `TurnLimitTest` |

#### FR-3 — Guards

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-3.1 | A patroller shall walk a fixed route between two points, reversing at each end | `GuardTest` |
| FR-3.2 | A hunter shall move towards the player's last known position using A\* | `GuardTest`, `AStarPathfinderTest` |
| FR-3.3 | A guard shall be in exactly one of three modes: PATROL, ALERT, HUNT | `GuardTest` |
| FR-3.4 | A hunter shall move two tiles on alternate turns, so that it gains on a fleeing player **(derived)** | `GuardTest` |
| FR-3.5 | A hunter that loses the player shall return to PATROL after searching | `AlarmTest` |

> **FR-3.4 rationale.** A hunter moving at exactly the player's speed never
> closes the gap, so a chase froze at whatever distance it started and the
> hunter appeared to escort the player. Moving at a net half tile per turn means
> running in a straight line loses and breaking line of sight wins, which is the
> behaviour the game is about.

#### FR-4 — Detection and the alarm

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-4.1 | A turn ending with the player inside any guard's line of sight shall count as a sighting | `SpottingTest` |
| FR-4.2 | Tiles within a guard's line of sight shall be drawn in red when visible to the player | *manual* |
| FR-4.3 | A sighting shall raise an alarm, which shall put every hunter into HUNT mode via an observer | `AlarmTest` |
| FR-4.4 | Sightings shall be counted for the whole run, not reset per attempt | `ScoreTest` |

#### FR-5 — Capture and lives

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-5.1 | A guard in HUNT mode reaching the player shall capture them | `CaptureTest` |
| FR-5.2 | A guard **not** in HUNT mode shall block the player but shall not capture **(derived)** | `CaptureTest` |
| FR-5.3 | A run shall begin with three lives; a capture shall cost one | `LivesTest` |
| FR-5.4 | A new life shall reset the player, the keys, the guards and the clock, but not the sighting count | `LivesTest` |
| FR-5.5 | Losing the last life shall end the run | `LivesTest` |

> **FR-5.2 rationale.** Making all contact lethal rendered level 1 unwinnable:
> a patroller in a one-wide corridor could not be passed at all. Only a guard
> that is actively hunting kills; everything else is an obstacle.

#### FR-6 — Keys and the exit

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-6.1 | A level may contain several keys; all of them shall be required | `MultipleKeysTest` |
| FR-6.2 | Stepping onto a key shall collect it and remove it from the map | `MultipleKeysTest` |
| FR-6.3 | The exit shall not open until every key is held | `MultipleKeysTest` |
| FR-6.4 | Reaching an open exit shall end the attempt as an escape | `GameStateTest` |

#### FR-7 — Undo

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-7.1 | Every turn shall be recorded as a command on a stack | `UndoTest` |
| FR-7.2 | Undo shall restore the player, the guards, the alarm state, the sighting count, the keys and the clock | `UndoTest` |
| FR-7.3 | Undo shall be repeatable back to the start of the attempt | `UndoTest` |
| FR-7.4 | Undo shall be refused once the attempt has ended | `UndoTest` |
| FR-7.5 | The query "can I undo?" shall agree with what undo actually does **(derived)** | `UndoTest` |

> **FR-7.5 rationale.** These two disagreed once. A test looping
> `while (canUndo()) undoLastMove();` span forever, because `canUndo` said yes
> in a state `undoLastMove` refused to act on.

#### FR-8 — Hints

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-8.1 | A hint shall indicate **one** move, not a route | `HintTest` |
| FR-8.2 | A hint shall expire when the player moves | `HintLifetimeTest` |
| FR-8.3 | When the player is in a guard's sight, the hint shall point towards cover rather than towards the objective | `HintCoverTest` |
| FR-8.4 | Otherwise the hint shall point along a route to the nearest key, then the exit, weighted to avoid guard sight | `HintTest` |
| FR-8.5 | A level shall allow three hints **(derived)** | `HintBudgetTest` |
| FR-8.6 | Neither undo nor a new life shall restore a spent hint **(derived)** | `HintBudgetTest` |

#### FR-9 — Scoring

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-9.1 | A failed attempt shall score zero stars | `ScoreTest` |
| FR-9.2 | Escaping shall score one star | `ScoreTest` |
| FR-9.3 | Escaping within par shall score a second | `ScoreTest` |
| FR-9.4 | Escaping without being seen shall score a third | `ScoreTest` |
| FR-9.5 | Par shall be derived from the level's own geometry, not written into the level file | `ScoreTest` |
| FR-9.6 | Three stars shall be achievable on every level **(derived)** | `StarRatingAchievableTest` |

> **FR-9.6 rationale.** This is the most important requirement in the document
> and the easiest to violate by accident. With par set at 1.4× the clear route,
> level 1 had a fastest escape of 38 turns, a fastest *unseen* escape of 56, and
> a par of 51 — so the two bonus stars excluded each other and three stars was
> unreachable however well anyone played. That is not difficulty, it is a broken
> promise, and only a test can keep it from coming back.

#### FR-10 — Levels and progression

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-10.1 | The game shall provide ten levels of increasing size | `LevelIntegrityTest` |
| FR-10.2 | Every level shall be winnable: all keys and the exit reachable from the start | `LevelIntegrityTest` |
| FR-10.3 | Every tile a guard can watch shall have cover within reach, so being seen is always escapable | `LevelFairnessTest` |
| FR-10.4 | Levels shall unlock at 0, 1, 3, 5, 8, 11, 14, 17, 20 and 23 stars | `LevelIntegrityTest` |
| FR-10.5 | The level list shall be data, so adding a level requires no code change | `LevelIntegrityTest` |

#### FR-11 — Players and saved progress

| ID | Requirement | Verified by |
| --- | --- | --- |
| FR-11.1 | A player shall enter a name, under which progress is stored | `ProgressTest` |
| FR-11.2 | The best result per level per player shall be retained | `ProgressTest` |
| FR-11.3 | Saving one player's progress shall not disturb another's | `ProgressTest` |
| FR-11.4 | An unknown name shall start from zero | `ProgressTest` |
| FR-11.5 | Progress shall be written only to the local machine | `ProgressTest` |
| FR-11.6 | A missing, empty or malformed save file shall be treated as "no progress", never as an error the player sees | `ProgressTest` |

### 3.3 Non-functional requirements

| ID | Requirement | Verified by |
| --- | --- | --- |
| NFR-1 | A turn shall resolve fast enough to feel instant on a 29×21 grid | *manual; no measured budget* |
| NFR-2 | No class in `model`, `vision`, `pathfinding`, `guard`, `level` or `progress` shall import `java.awt`, `javax.swing` or `java.applet` | `ArchitectureTest` |
| NFR-3 | The whole test suite shall run without opening a window | `ArchitectureTest` (by consequence) |
| NFR-4 | The game shall be deterministic: identical input from identical state shall give an identical result | `TurnWithGuardsTest` |
| NFR-5 | `ProgressStore` shall accept a path from its caller, so a platform with no home directory can supply one | `ArchitectureTest` |
| NFR-6 | The game shall send no data anywhere and shall require no network | *by inspection: no networking API is imported* |

> **NFR-4 rationale.** Determinism is not a nicety here. It is what makes the
> exhaustive solver in section 5.2 possible, and therefore what makes FR-10.2
> and FR-9.6 checkable at all rather than merely hoped for.

### 3.4 Constraints imposed by phase 2

The planned leaderboard requires that a completed run can be expressed as data
independent of the interface. `Score` and `LevelResult` are records holding
exactly that, which is why they carry no drawing logic.

---

## 4. Traceability matrix

| Area | Requirements | Test class |
| --- | --- | --- |
| Visibility | FR-1.1 – FR-1.3 | `VisibilityServiceTest` |
| Fog memory | FR-1.4, FR-1.5 | `FogOfWarTest` |
| Turns, movement | FR-2.1 – FR-2.4, NFR-4 | `GameStateTest`, `TurnWithGuardsTest` |
| Turn limit | FR-2.5 | `TurnLimitTest` |
| Guards | FR-3.1 – FR-3.4 | `GuardTest` |
| Pathfinding | FR-3.2 | `AStarPathfinderTest` |
| Alarm | FR-3.5, FR-4.3 | `AlarmTest` |
| Detection | FR-4.1, FR-4.4 | `SpottingTest` |
| Capture | FR-5.1, FR-5.2 | `CaptureTest` |
| Lives | FR-5.3 – FR-5.5 | `LivesTest` |
| Keys and exit | FR-6.1 – FR-6.4 | `MultipleKeysTest` |
| Undo | FR-7.1 – FR-7.5 | `UndoTest` |
| Hints | FR-8.1, FR-8.4 | `HintTest` |
| Hint lifetime | FR-8.2 | `HintLifetimeTest` |
| Hint under threat | FR-8.3 | `HintCoverTest` |
| Hint budget | FR-8.5, FR-8.6 | `HintBudgetTest` |
| Scoring | FR-9.1 – FR-9.5 | `ScoreTest` |
| Stars achievable | FR-9.6 | `StarRatingAchievableTest` |
| Level integrity | FR-10.1, FR-10.2, FR-10.4, FR-10.5 | `LevelIntegrityTest` |
| Level fairness | FR-10.3 | `LevelFairnessTest` |
| Progress | FR-11.1 – FR-11.6 | `ProgressTest` |
| Architecture | NFR-2, NFR-3, NFR-5 | `ArchitectureTest` |
| Key bindings | UI-3, UI-4 | `KeyBindingTest` |
| Screen flow | UI-1, UI-2 | `KeyBindingTest` |
| Artwork | — | `CritterArtTest` |

**25 test classes, 205 tests, all passing.**

---

## 5. Verification

### 5.1 Automated tests

`mvn test`. The suite runs headless and takes a few seconds.

### 5.2 Exhaustive search

Because the game is deterministic (NFR-4), levels can be *proved* winnable
rather than played and hoped about. A breadth-first search over the entire game
tree — keyed on position, turn count and which keys remain — answers whether a
winning line exists and how short the shortest one is. This is where the par
figures and the FR-9.6 thresholds come from.

The solver found that level 1 was unwinnable with its guards in place, which no
amount of playtesting would have distinguished from "hard". It also contained
two bugs of its own, each of which reported winnable levels as lost: a
depth-first version memoised states without the turn count, which is unsound
while a clock runs; and a state key recorded how many keys were held rather
than which, so holding the first and the third looked identical.

### 5.3 Manual verification

Requirements marked *manual* are visual or interactive and are checked by
playing. Screenshots are generated from the real interface by `ScreenshotTool`
rather than captured by hand, so they cannot drift from the build.

### 5.4 Known limits of the verification

- **Nothing measures NFR-1.** It is asserted, not tested.
- **The UI is thinly tested.** `KeyBindingTest` covers navigation and
  bindings by resolving real keystrokes; nothing covers what is drawn.
- **Two tests were once fake.** Two hint tests were wrapped in a condition that
  never became true, so they passed while asserting nothing. They now assert
  that the condition was reached. The whole hint set was then mutation-tested by
  deliberately breaking the budget and confirming three tests fail.

---

## 6. Requirements not met

An SRS that lists only what was achieved is a sales document.

### 6.1 Not implemented

| From the brief | Status |
| --- | --- |
| **A sentinel guard with a rotating vision cone** | Not built. `GuardFactory` and the `GuardBehaviour` interface are the extension points it would use; no other code would change. This is the one guard type of the three in the brief that does not exist. |
| **Spring Boot and PostgreSQL leaderboard** | Phase 2, not started. Deliberately deferred. |

### 6.2 Partially met

| Requirement | Gap |
| --- | --- |
| UI-3 | Every key is named in the footer strip, but nothing in the game explains the *rules* — that red tiles are watched, that only hunters kill, or how stars are earned. A new player infers these by dying. |
| NFR-1 | No performance budget is stated or measured. |

### 6.3 Not addressed

Accessibility. There is no keyboard-only path through the level and character
screens, no screen-reader support, no colour-blind-safe palette, and the red
line-of-sight overlay carries meaning by colour alone. This is a real gap and
is recorded rather than excused.

---

## 7. Document history

| Version | Change |
| --- | --- |
| 1.0 | First issue, written against the phase 1 build |
