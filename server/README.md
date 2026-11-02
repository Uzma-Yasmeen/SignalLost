# Signal Lost — leaderboard

Phase 2. A Spring Boot service that records completed runs and serves a
leaderboard, and does one thing the game cannot do for itself: decide whether a
score is real.

[&larr; back to the game](../README.md)

## The idea

A leaderboard that accepts a score has to believe whatever the client sends it.
The game runs on the player's machine, so `{"stars": 3}` is a claim, not a fact,
and the usual defences — obfuscation, a key baked into the client, a
plausibility check — only make it inconvenient to forge.

**This service never accepts a score.** It accepts the moves, replays them
against the same rules the game runs, and works the result out itself. Claiming
three stars requires submitting a sequence of moves that earns three stars,
which is to say, playing the level.

Two properties of the game make that possible, and both are enforced by tests in
the game's own build:

- The rules carry no user interface, so they run inside a web request.
- The game is deterministic, so replaying the same moves from the same start
  reaches the same end.

The `server` module depends on the game artifact and calls `GameState` directly.
There is no second copy of the rules here to drift out of step.

## Running it

The game artifact has to be installed first, since this depends on it:

```bash
mvn install -DskipTests          # in the repository root
cd server && mvn spring-boot:run # http://localhost:8080
```

That starts with an in-memory database and no other setup. For PostgreSQL:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

with `DATABASE_URL`, `DATABASE_USER` and `DATABASE_PASSWORD` in the environment.

## The API

| | |
| --- | --- |
| `POST /api/runs` | Submit a run. 201 with the computed score, or 422 if the moves are not a winning run |
| `GET /api/leaderboard/{level}` | Each player's best run on that level, best first |
| `GET /api/players/{name}/runs` | Everything that player has submitted |
| `GET /api/levels` | The levels this server accepts, read from the game's own catalogue |
| `GET /api/health` | |

A submission looks like this. Note that there is no field for a score:

```bash
curl -X POST http://localhost:8080/api/runs \
  -H 'Content-Type: application/json' \
  -d '{"playerName":"Uzma","levelName":"level1",
       "moves":"DUDUDUDUDDDDDDDDRRRRUUUURRRRUUUURRRRDDDDDDDD"}'
```

```json
{"id":1,"playerName":"Uzma","levelName":"level1",
 "stars":2,"turns":44,"par":69,"sightings":33,
 "submittedAt":"2026-11-02T15:04:40Z"}
```

Every number in that response was computed here. Four moves claiming a win get:

```json
{"error":"run rejected",
 "reason":"The run ends without reaching the exit after 4 moves"}
```

## The schema

Two tables. The decision worth explaining is in the second: **every run is
kept, not only a player's best.** Storing a best score as a column means keeping
it correct on every write and throws away the history. "Best" is a question to
ask the data, so the leaderboard is a query — a window function that numbers
each player's runs from their best and keeps the first.

```
player (id, display_name unique, first_seen)
run    (id, player_id → player, level_name, moves,
        stars, turns, par, sightings, submitted_at)
```

`moves` is stored because it is the only part the client authored, and because a
stored run can be re-verified later or replayed for a spectator.

Flyway owns the schema and Hibernate is set to `validate`, so the tables the
tests run against are the tables a deployment gets.

## Tests

```bash
mvn test
```

18 tests. The verifier is tested from both ends — that a genuine run is accepted
and scored exactly as the game scores it, and that a short, padded, malformed or
impossible one is refused. The API tests go over real HTTP against a real
database, including that a refused run leaves nothing behind, not even a player
row.

The tests find a winning run by searching for one rather than hard-coding a
sequence, which would rot the first time a level was redrawn.

## Notes

- H2 is at `runtime` scope rather than `test` scope deliberately, so the default
  profile starts with no setup. A deployment uses the `postgres` profile and
  never loads it.
- A padded run is not a valid run. Adding a wait to a winning sequence moves
  every guard, so the route stops working — a consequence of the determinism
  this design relies on, and something that caught out an early test here.
