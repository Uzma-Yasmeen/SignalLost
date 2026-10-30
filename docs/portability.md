# Running it somewhere that is not a desktop

What an Android port would and would not have to change, and how the project got here.

[&larr; back to the README](../README.md)

## Could this run on a phone?

Yes, and the reason is the package split rather than any plan for it. Every
external import across the 2,704 lines of game logic:

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

## Build notes

Java 17 and Maven. The compiler targets 17 rather than 21 because that is what
`JAVA_HOME` points at on the development machine; nothing in the code needs 21.
