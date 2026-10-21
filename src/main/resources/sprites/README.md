# Character artwork

Drop PNG files in this folder and the game uses them instead of the characters
drawn in code. Delete them and the drawn versions come back. No Java changes
either way.

## File names

The names are the characters in lower case:

| File name | Character |
| --- | --- |
| `cat.png` | Mochi |
| `fox.png` | Kit |
| `panda.png` | Bao |
| `frog.png` | Pip |
| `owl.png` | Hoot |
| `bunny.png` | Momo |

For an animation, number the frames from 1 instead, with no gaps:

```
cat-1.png
cat-2.png
cat-3.png
```

Frames play in order on a loop, roughly seven a second. Numbered frames win if
both a plain file and numbered ones exist. Up to 16 frames are read.

A character with no file keeps its drawn version, so artwork can arrive one
animal at a time.

## What the files should look like

- **Square-ish, transparent background.** The image is scaled to fit the tile
  without being stretched, so proportions are kept and a very tall sprite will
  end up narrow.
- **Small is fine.** A character is drawn about 20 pixels across in the level and
  about 90 on the select screen. 32×32 or 64×64 source art works well.
- **Pixel art stays sharp.** Images are scaled with nearest-neighbour, never
  blurred, so pixel art keeps its edges.

## Where to get art you are allowed to use

This matters if the project goes in a portfolio. Anything a reviewer can see,
they can check — using art without the right licence is a worse look than having
no art at all.

- [Kenney](https://kenney.nl/assets) — public domain (CC0), no attribution needed
- [OpenGameArt](https://opengameart.org) — filter by CC0 or CC-BY licence
- [itch.io asset packs](https://itch.io/game-assets/free) — read each pack's
  licence, many are free for commercial use

Whatever you use, record it. Put the pack name, the author and the licence in
`docs/credits.md`, and keep the licence file itself next to the images if the
pack ships one. CC-BY art needs the author credited somewhere a player can see.
