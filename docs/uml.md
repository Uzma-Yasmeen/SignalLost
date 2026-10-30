# UML diagrams

Section 5 of the [course report](report.md), drawn from the code as it stands
rather than from an earlier plan. Every class, method and relationship below
exists in `src/main/java/com/signallost`.

The diagrams are written as Mermaid source so that they render on GitHub, show
up meaningfully in a diff, and can be regenerated as images for the report by
`docs/render-diagrams.js`.

[&larr; back to the README](../README.md)

---

## 5.1 System architecture

Two layers, and the arrow between them points one way only. That direction is
enforced by `ArchitectureTest`, which fails the build if any package on the
lower half imports a user-interface type.

```mermaid
flowchart TB
    subgraph PRESENTATION["Presentation layer — com.signallost.ui"]
        direction LR
        FRAME["GameFrame<br/><i>screen flow, key bindings</i>"]
        PANEL["GamePanel<br/><i>draws the board</i>"]
        HUD["HudBar · FooterBar<br/><i>status and controls</i>"]
        MENUS["TitlePanel · NamePanel<br/>CharacterSelectPanel · LevelSelectPanel"]
        ART["PixelSprite · CritterArt · Palette<br/><i>16×16 sprites written as text</i>"]
    end

    subgraph RULES["Game rules — no user-interface types anywhere"]
        direction TB
        MODEL["<b>model</b><br/>GameState · GameMap · Position<br/>Command · MoveCommand · TurnCommand<br/>GuardMoveCommand · AlarmCommand · Score"]
        VISION["<b>vision</b><br/>VisibilityService · FogOfWar"]
        GUARD["<b>guard</b><br/>Guard · Patroller · Hunter<br/>GuardBehaviour · GuardMode · GuardState"]
        PATH["<b>pathfinding</b><br/>AStarPathfinder · RouteMetrics"]
        LEVEL["<b>level</b><br/>Level · LevelLoader · LevelCatalog"]
        PROGRESS["<b>progress</b><br/>PlayerProfile · LevelResult · ProgressStore"]
    end

    RESOURCES[("Level files<br/><i>src/main/resources/levels</i>")]
    SAVE[("Saved progress<br/><i>~/.signal-lost</i>")]

    PRESENTATION -->|"reads state, sends input"| RULES
    MODEL --> VISION
    MODEL --> GUARD
    GUARD --> PATH
    MODEL --> PATH
    LEVEL --> MODEL
    LEVEL --> GUARD
    LEVEL -.->|loads| RESOURCES
    PROGRESS -.->|reads and writes| SAVE

    style PRESENTATION fill:#eef2ff,stroke:#4f46e5
    style RULES fill:#ecfdf5,stroke:#059669
```

**Why the boundary is the architecture.** The rules may not know how they are
drawn. That single constraint is what lets the whole test suite run without
opening a window, and what would let the game move to another platform by
replacing only the upper half.

---

## 5.2 Use case diagram

```mermaid
flowchart LR
    PLAYER(["👤<br/>Player"])

    subgraph SYSTEM["Signal Lost"]
        UC1(["Identify<br/>by name"])
        UC2(["Choose a<br/>character"])
        UC3(["Select an<br/>unlocked level"])
        UC4(["Move or<br/>wait one turn"])
        UC5(["Request<br/>a hint"])
        UC6(["Undo the<br/>last turn"])
        UC7(["Restart<br/>the level"])
        UC8(["Collect keys<br/>and escape"])
        UC9(["Earn a<br/>star rating"])
        UC10(["Resume saved<br/>progress"])

        UC11(["Advance<br/>every guard"])
        UC12(["Raise the<br/>alarm"])
        UC13(["Capture the<br/>player"])
    end

    GUARDS(["⚙️<br/>Guards<br/><i>system actor</i>"])

    PLAYER --- UC1
    PLAYER --- UC2
    PLAYER --- UC3
    PLAYER --- UC4
    PLAYER --- UC5
    PLAYER --- UC6
    PLAYER --- UC7
    PLAYER --- UC8
    PLAYER --- UC10

    UC4 -.->|"&lt;&lt;include&gt;&gt;"| UC11
    UC8 -.->|"&lt;&lt;include&gt;&gt;"| UC9
    UC11 -.->|"&lt;&lt;extend&gt;&gt;"| UC12
    UC12 -.->|"&lt;&lt;extend&gt;&gt;"| UC13

    UC11 --- GUARDS
    UC12 --- GUARDS
    UC13 --- GUARDS

    style SYSTEM fill:#f8fafc,stroke:#64748b
```

| Use case | Actor | Description |
| --- | --- | --- |
| Identify by name | Player | Enters a name; progress is loaded for it, or started fresh |
| Choose a character | Player | Selects one of six characters for the run |
| Select an unlocked level | Player | Chooses from the levels their star total has unlocked |
| Move or wait one turn | Player | The primary interaction; always includes advancing the guards |
| Request a hint | Player | Spends one of three; shown one move, for one turn |
| Undo the last turn | Player | Rewinds player, guards, alarm, sightings and clock |
| Restart the level | Player | Abandons the attempt and begins again |
| Collect keys and escape | Player | Completing the objective; always produces a rating |
| Earn a star rating | Player | One star for escaping, one for speed, one for going unseen |
| Resume saved progress | Player | Best result per level is retained under their name |
| Advance every guard | Guards | Each guard acts after the player, per its mode |
| Raise the alarm | Guards | Extends guard movement when the player is sighted |
| Capture the player | Guards | Extends the alarm when a hunting guard reaches the player |

---

## 5.3 Class diagram

### 5.3.1 Core model and the Command pattern

Every turn is an object that knows how to undo itself. A `TurnCommand` owns the
player's action, one `GuardMoveCommand` per guard, and the alarm if one was
raised — which is why undo restores the guards and not merely the player.

```mermaid
classDiagram
    direction TB

    class Command {
        <<interface>>
        +execute() void
        +undo() void
        +description() String
    }

    class GameState {
        -GameMap map
        -FogOfWar fog
        -List~Guard~ guards
        -Deque~Command~ history
        -int hintsLeft
        ~Position player
        ~int turnsTaken
        ~int sightings
        ~int livesLeft
        +movePlayer(Direction) boolean
        +undoLastMove() boolean
        +canUndo() boolean
        +beginNextLife() boolean
        +hintRoute(int) List~Position~
        +spendHint() boolean
        +score() Score
        +parTurns() int
        +isPlayerWatched() boolean
    }

    class TurnCommand {
        -Command playerAction
        -List~Command~ guardActions
        -Command alarmAction
        -int previousSightings
        -boolean previousCaught
        +execute() void
        +undo() void
        -settleEndOfTurn() void
    }

    class MoveCommand {
        -Direction direction
        -Position destination
        -Position previousPosition
        +execute() void
        +undo() void
    }

    class GuardMoveCommand {
        -Guard guard
        -GuardState previousState
        +execute() void
        +undo() void
    }

    class AlarmCommand {
        -List~Guard~ guards
        -Position sighting
        -List~GuardState~ previousStates
        +execute() void
        +undo() void
    }

    class GameMap {
        -TileType[][] tiles
        +tileAt(Position) TileType
        +isWalkable(Position) boolean
        +findAll(TileType) List~Position~
    }

    class Position {
        <<record>>
        +int row
        +int col
    }

    class Score {
        <<record>>
        +int stars
        +int turns
        +int par
        +int sightings
        +of(boolean, int, int, int)$ Score
    }

    class Direction {
        <<enumeration>>
        UP, DOWN, LEFT, RIGHT, WAIT
        +applyTo(Position) Position
    }

    class TileType {
        <<enumeration>>
        FLOOR, WALL, KEY, EXIT, START
    }

    Command <|.. TurnCommand
    Command <|.. MoveCommand
    Command <|.. GuardMoveCommand
    Command <|.. AlarmCommand

    GameState "1" *-- "0..*" Command : history (stack)
    TurnCommand "1" *-- "1" MoveCommand : player action
    TurnCommand "1" *-- "0..*" GuardMoveCommand : one per guard
    TurnCommand "1" o-- "0..1" AlarmCommand : if seen

    GameState "1" *-- "1" GameMap
    GameMap ..> TileType
    GameState ..> Position
    GameState ..> Score
    GameState ..> Direction
```

### 5.3.2 Guards — State, Strategy and Observer together

Three patterns meet in one place. `GuardMode` is the **State**; `GuardBehaviour`
is the **Strategy** selected by that state; `AlarmListener` is the **Observer**
by which one guard's sighting reaches every other.

```mermaid
classDiagram
    direction TB

    class AlarmListener {
        <<interface>>
        +onAlarmRaised(GuardState, Position) GuardState
    }

    class Guard {
        <<abstract>>
        -GuardState state
        +decideTurn(GameMap, Position)* GuardState
        +type()* String
        +canSee(GameMap, Position) boolean
        +state() GuardState
        +applyState(GuardState) void
        +mode() GuardMode
        +position() Position
    }

    class Patroller {
        -Position routeStart
        -Position routeEnd
        +decideTurn(GameMap, Position) GuardState
        +type() String
    }

    class Hunter {
        -Position post
        +decideTurn(GameMap, Position) GuardState
        +onAlarmRaised(GuardState, Position) GuardState
        +type() String
    }

    class GuardBehaviour {
        <<interface>>
        +takeTurn(Guard, GuardState, GameMap, Position) GuardState
    }

    class PatrolBehaviour {
        +INSTANCE$ PatrolBehaviour
        +takeTurn(...) GuardState
    }

    class HuntBehaviour {
        +SURGE_STEPS$ int = 2
        +INSTANCE$ HuntBehaviour
        +takeTurn(...) GuardState
    }

    class AlertBehaviour {
        +INSTANCE$ AlertBehaviour
        +takeTurn(...) GuardState
    }

    class GuardState {
        <<record>>
        +Position position
        +GuardMode mode
        +int routeIndex
        +Position lastSeenPlayer
        +int alertTurnsLeft
        +boolean surgedLastTurn
    }

    class GuardMode {
        <<enumeration>>
        PATROL
        ALERT
        HUNT
        +label() String
    }

    class AStarPathfinder {
        +findPath(GameMap, Position, Position)$ List~Position~
        +findPath(GameMap, Position, Position, ToIntFunction)$ List~Position~
        +nextStepTowards(GameMap, Position, Position)$ Optional~Position~
    }

    AlarmListener <|.. Guard
    Guard <|-- Patroller
    Guard <|-- Hunter

    GuardBehaviour <|.. PatrolBehaviour
    GuardBehaviour <|.. HuntBehaviour
    GuardBehaviour <|.. AlertBehaviour

    Guard "1" *-- "1" GuardState : immutable snapshot
    GuardState ..> GuardMode
    Patroller ..> PatrolBehaviour : delegates
    Hunter ..> HuntBehaviour : delegates
    Hunter ..> AlertBehaviour : delegates
    HuntBehaviour ..> AStarPathfinder : pursues with
    AlertBehaviour ..> AStarPathfinder : searches with
```

> **Why `GuardState` is a record.** A guard's whole condition — where it stands,
> what mode it is in, how far along its route it is, where it last saw the
> player, how long it will keep searching, and whether it surged last turn — is
> one immutable value. A
> `GuardMoveCommand` undoes itself by putting the previous value back, so undo
> is exact by construction rather than by remembering to reverse each field.

### 5.3.3 Vision, levels and progress

```mermaid
classDiagram
    direction LR

    class VisibilityService {
        +visibleFrom(GameMap, Position, int)$ Set~Position~
        +sightDistancesFrom(GameMap, Position, int)$ Map~Position,Integer~
    }

    class FogOfWar {
        -GameMap map
        -int sightRadius
        -Set~Position~ explored
        -boolean revealAll
        +FogOfWar(GameMap, int)
        +update(Position) void
        +visibilityOf(Position) TileVisibility
        +visibleTiles() Set~Position~
        +exploredTiles() Set~Position~
        +toggleRevealAll() void
    }

    class TileVisibility {
        <<enumeration>>
        VISIBLE
        REMEMBERED
        UNEXPLORED
    }

    class Level {
        <<record>>
        +String name
        +GameMap map
        +List~Guard~ guards
        +int turnLimit
    }

    class LevelLoader {
        +loadFromResource(String)$ Level
    }

    class LevelCatalog {
        +load()$ List~Entry~
    }

    class Entry {
        <<record>>
        +String name
        +int starsToUnlock
        +displayName() String
        +shortLabel() String
    }

    class GuardFactory {
        +fromSpecification(String, int)$ Guard
    }

    class PlayerProfile {
        -String name
        -Map~String,LevelResult~ results
        +record(String, Score) boolean
        +bestOn(String) LevelResult
        +starsOn(String) int
        +hasCompleted(String) boolean
        +totalStars() int
    }

    class LevelResult {
        <<record>>
        +int stars
        +int turns
        +int sightings
    }

    class ProgressStore {
        -Path file
        +ProgressStore(Path)
        +inUserHome()$ ProgressStore
        +load() PlayerProfile
        +loadFor(String) PlayerProfile
        +save(PlayerProfile) void
    }

    FogOfWar ..> VisibilityService : asks each turn
    FogOfWar ..> TileVisibility
    LevelLoader ..> Level : builds
    LevelLoader ..> GuardFactory : one guard per line
    LevelCatalog ..> Entry : loads ten
    PlayerProfile "1" *-- "0..*" LevelResult
    ProgressStore ..> PlayerProfile : reads and writes
```

---

## 5.4 Sequence diagrams

### 5.4.1 One turn

The single most important flow in the system. Note the ordering: the player
moves first, then every guard, and only then is it decided whether the player
was seen. That order is the whole tension of the game — stepping somewhere safe
is not enough, because a guard may turn a corner and be looking at you by the
time the turn ends.

```mermaid
sequenceDiagram
    actor Player
    participant Frame as GameFrame
    participant State as GameState
    participant Turn as TurnCommand
    participant Move as MoveCommand
    participant GMove as GuardMoveCommand
    participant Guard as Hunter
    participant Behave as HuntBehaviour
    participant AStar as AStarPathfinder
    participant Alarm as AlarmCommand
    participant Fog as FogOfWar
    participant Panel as GamePanel

    Player->>Frame: presses an arrow key
    Frame->>State: movePlayer(direction)

    State->>State: reject if finished or attempt over
    State->>State: reject if wall, edge, or guard

    State->>Turn: new TurnCommand(state, MoveCommand)
    State->>Turn: execute()
    activate Turn

    Turn->>Turn: snapshot sightings, spotted, failed, caught
    Turn->>Move: execute()
    Move-->>Turn: player moved, key collected if any

    loop for each guard
        Turn->>GMove: execute()
        GMove->>Guard: state()
        Note right of GMove: previous state kept for undo
        GMove->>Guard: decideTurn(map, player)
        Guard->>Behave: takeTurn(guard, state, map, player)
        Behave->>AStar: findPath(map, from, target)
        AStar-->>Behave: route
        Behave-->>Guard: new GuardState
        Guard-->>GMove: new GuardState
        GMove->>Guard: applyState(new)
    end

    Turn->>Turn: settleEndOfTurn()
    Turn->>State: any HUNT guard on the player? → caught
    Turn->>State: isPlayerWatched()? → spotted, sightings++

    alt spotted and not caught
        Turn->>Alarm: execute()
        loop for each guard
            Alarm->>Guard: onAlarmRaised(state, sighting)
            Guard-->>Alarm: HUNT state for hunters
        end
    end

    deactivate Turn
    State->>State: history.push(turn)
    State-->>Frame: true

    Frame->>Fog: update(player)
    Fog-->>Frame: visible and remembered tiles
    Frame->>Panel: repaint()
    Panel-->>Player: new board, fog, guards, alerts
```

### 5.4.2 Undoing a turn

Undo is a pop and a reversal. Because the turn object holds the previous state
of everything it touched, nothing has to be recomputed — and nothing can be
missed.

```mermaid
sequenceDiagram
    actor Player
    participant Frame as GameFrame
    participant State as GameState
    participant Turn as TurnCommand
    participant Alarm as AlarmCommand
    participant GMove as GuardMoveCommand
    participant Move as MoveCommand
    participant Panel as GamePanel

    Player->>Frame: presses U
    Frame->>State: undoLastMove()

    State->>State: refuse if attempt is over
    State->>State: refuse if history is empty
    State->>Turn: pop from history, undo()
    activate Turn

    opt an alarm was raised this turn
        Turn->>Alarm: undo()
        Alarm-->>Turn: every guard restored to its pre-alarm state
    end

    loop guard actions, in reverse
        Turn->>GMove: undo()
        GMove-->>Turn: applyState(previousState)
    end

    Turn->>Move: undo()
    Move-->>Turn: player back, key back on the floor

    Turn->>State: restore sightings, spotted, failed, caught
    deactivate Turn

    Note over State: the hint budget is deliberately NOT restored
    State-->>Frame: true
    Frame->>Panel: repaint()
```

### 5.4.3 Being seen, and the alarm reaching the hunters

```mermaid
sequenceDiagram
    participant Turn as TurnCommand
    participant State as GameState
    participant Patrol as Patroller
    participant Alarm as AlarmCommand
    participant H1 as Hunter
    participant P2 as another Patroller

    Turn->>State: isPlayerWatched()
    State->>Patrol: canSee(map, player)
    Patrol->>Patrol: breadth-first sight, blocked by walls
    Patrol-->>State: true
    State-->>Turn: player is watched

    Turn->>State: sightings++
    Turn->>Alarm: new AlarmCommand(guards, sighting), execute()
    activate Alarm

    Alarm->>H1: onAlarmRaised(state, sighting)
    H1-->>Alarm: GuardState in HUNT, target = sighting

    Alarm->>P2: onAlarmRaised(state, sighting)
    Note right of P2: a patroller has no answer<br/>to an alarm, so it is unchanged
    P2-->>Alarm: state unchanged

    deactivate Alarm
    Note over Alarm: every previous state kept, so undo restores them all
```

---

## 5.5 Activity diagram

The flow of one complete attempt at a level, from entering it to the rating.

```mermaid
flowchart TD
    START([Enter the level]) --> INIT[/"Place player, keys and guards<br/>Reset clock, 3 lives, 3 hints"/]
    INIT --> FOG["Compute visible tiles<br/><i>breadth-first from the player</i>"]
    FOG --> DRAW[/"Draw board, fog and status"/]
    DRAW --> INPUT{"Player<br/>presses"}

    INPUT -->|"U — undo"| UNDO["Pop the turn<br/>restore guards, alarm,<br/>sightings and clock"]
    UNDO --> FOG

    INPUT -->|"H — hint"| HINTCHK{"Hints<br/>left?"}
    HINTCHK -->|no| DRAW
    HINTCHK -->|yes| HINT["Spend one<br/>show a single move"]
    HINT --> DRAW

    INPUT -->|"R — restart"| INIT

    INPUT -->|"move or wait"| LEGAL{"Legal<br/>move?"}
    LEGAL -->|"wall, edge<br/>or guard"| DRAW
    LEGAL -->|yes| MOVE["Move the player<br/>collect a key if present"]

    MOVE --> GUARDS["Every guard takes its turn<br/><i>patrol route · A* pursuit · search</i>"]
    GUARDS --> CAUGHT{"A hunting guard<br/>on the player?"}

    CAUGHT -->|yes| LIFE["Lose a life"]
    LIFE --> LIVES{"Lives<br/>remaining?"}
    LIVES -->|yes| INIT
    LIVES -->|no| OVER([Run over — 0 stars])

    CAUGHT -->|no| SEEN{"Player in any<br/>line of sight?"}
    SEEN -->|yes| SIGHT["sightings++<br/>raise the alarm<br/><i>hunters switch to HUNT</i>"]
    SEEN -->|no| CLOCK
    SIGHT --> CLOCK{"Turn limit<br/>reached?"}

    CLOCK -->|yes| FAIL([Attempt failed])
    CLOCK -->|no| EXIT{"On the exit<br/>with every key?"}
    EXIT -->|no| FOG
    EXIT -->|yes| SCORE["Rate the run:<br/>1 star escaped<br/>+1 within par<br/>+1 never seen"]
    SCORE --> SAVE[/"Save if better than<br/>this player's best"/]
    SAVE --> DONE([Escaped])

    style START fill:#dcfce7,stroke:#16a34a
    style DONE fill:#dcfce7,stroke:#16a34a
    style OVER fill:#fee2e2,stroke:#dc2626
    style FAIL fill:#fee2e2,stroke:#dc2626
    style SIGHT fill:#fef3c7,stroke:#d97706
    style LIFE fill:#fee2e2,stroke:#dc2626
```

---

## 5.6 Guard state machine

The **State** pattern, drawn on its own because the transitions are the part
most often got wrong.

```mermaid
stateDiagram-v2
    [*] --> PATROL

    PATROL --> HUNT : alarm raised<br/>(hunters only)
    PATROL --> PATROL : walk the route,<br/>reverse at each end

    HUNT --> HUNT : A* towards the player,<br/>two tiles on alternate turns
    HUNT --> ALERT : lost sight of the player

    ALERT --> HUNT : player seen again
    ALERT --> ALERT : search around the<br/>last known position
    ALERT --> PATROL : search exhausted,<br/>return to post

    note right of HUNT
        Only a guard in this mode
        can capture the player.
        Everything else is an obstacle.
    end note

    note left of PATROL
        A patroller never leaves
        this state. It raises the
        alarm but cannot answer one.
    end note
```
