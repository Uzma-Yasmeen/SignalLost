# Proving it works, and the bugs that proved it did not

A solver that searches the whole game tree, and the day undo stopped working without a single test noticing.

[&larr; back to the README](../README.md)

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
