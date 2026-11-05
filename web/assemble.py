"""Puts the finished site together: compiled game, levels, page, icons.

Kept as a script rather than Maven plugin configuration because what it does
is obvious when read, and because GitHub Pages wants a plain folder.
"""
import hashlib, re, shutil
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

web = Path(__file__).resolve().parent
root = web.parent
out = web / "dist"
if out.exists():
    shutil.rmtree(out)
out.mkdir()

shutil.copy(web / "target/generated/js/signal-lost.js", out / "signal-lost.js")
for name in ("index.html", "manifest.webmanifest", "sw.js"):
    shutil.copy(web / "site" / name, out / name)

levels = out / "levels"
levels.mkdir()
for f in sorted((root / "src/main/resources/levels").glob("*.txt")):
    shutil.copy(f, levels / f.name)

# Stamp the service worker with a hash of what it is about to cache.
#
# A browser decides whether to install a new worker by byte-comparing sw.js with
# the copy it already has. While the cache name was a fixed "v1" this file was
# identical on every deploy, so no new worker ever installed and every returning
# player was served the first build they ever opened — for ever. Changing these
# bytes whenever the game changes is the whole mechanism by which an update
# reaches anybody.
digest = hashlib.sha256()
for f in sorted(out.rglob("*")):
    if f.is_file() and f.name != "sw.js":
        digest.update(f.name.encode())
        digest.update(f.read_bytes())
build_id = digest.hexdigest()[:12]

worker = out / "sw.js"
stamped, count = re.subn(r"const BUILD_ID = '[^']*';",
                         f"const BUILD_ID = '{build_id}';", worker.read_text(encoding="utf-8"))
if count != 1:
    raise SystemExit("sw.js has no BUILD_ID to stamp: an update would reach nobody")
worker.write_text(stamped, encoding="utf-8", newline="\n")

# The same icon the phone uses, from the same sprite data.
src = (root / "src/main/java/com/signallost/art/CritterPixels.java").read_text(encoding="utf-8")
cat = re.findall(r'"([.A-Za-z]{16})"', re.search(r"String\[\] CAT = \{(.*?)\};", src, re.S).group(1))
fur, shadow, accent = 0xFFB454, 0xE08A2B, 0xFF8FA3
def mix(a, b, t): return tuple(int(((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) for s in (16, 8, 0))
LEGEND = {'O':(0x1C,0x22,0x2C),'B':mix(fur,fur,0),'D':mix(shadow,shadow,0),'d':mix(shadow,shadow,0),
          'A':mix(accent,accent,0),'W':(0xFF,0xFB,0xF2),'E':(0x24,0x2C,0x3A),
          'c':mix(fur,accent,0.55),'m':(0xFF,0xE8,0xD6)}
# The desktop's palette, so the installed icon is the game's own colours.
BACKDROP=(0x05,0x07,0x0C); FLOOR=(0x24,0x32,0x4B); WALL=(0x6E,0x86,0xB2); WATCHED=(0xEF,0x44,0x44)
SCENE=["  ## "," ..#r"," ...r"," ..#r","  .. "]

def icon(size):
    img = Image.new("RGBA", (size, size), BACKDROP + (255,))
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    r = size * 0.30
    ImageDraw.Draw(glow).ellipse([size/2-r, size/2-r, size/2+r, size/2+r], fill=(255,176,84,46))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(size*0.09)))
    d = ImageDraw.Draw(img, "RGBA")
    tile = size*0.172; left = (size-tile*5)/2; top = (size-tile*5)/2
    for row, line in enumerate(SCENE):
        for col, ch in enumerate(line):
            if ch == " ": continue
            colour = {'.':FLOOR,'#':WALL,'r':WATCHED}[ch]
            fade = max(0.0, 1 - (abs(row-2)+abs(col-2))*0.15)
            shade = tuple(int(BACKDROP[i]+(colour[i]-BACKDROP[i])*(0.4+0.6*fade)) for i in range(3))
            x, y = left+col*tile, top+row*tile
            d.rounded_rectangle([x+tile*0.045, y+tile*0.045, x+tile*0.955, y+tile*0.955],
                                radius=max(1, int(tile*0.22)), fill=shade+(255,))
    step = max(1, int(round(size*0.0195))); sl = size/2-step*8; st = size/2-step*8
    for row, line in enumerate(cat):
        for col, ch in enumerate(line):
            if ch in LEGEND:
                x, y = sl+col*step, st+row*step
                d.rectangle([x, y, x+step, y+step], fill=LEGEND[ch]+(255,))
    return img

for size in (192, 512):
    icon(size).save(out / f"icon-{size}.png")

total = sum(f.stat().st_size for f in out.rglob("*") if f.is_file())
print(f"  dist/ assembled: {len(list(out.rglob('*')))} entries, {total/1024:.0f} KB")
for f in sorted(out.rglob("*")):
    if f.is_file() and f.parent == out:
        print(f"    {f.name:26} {f.stat().st_size/1024:7.1f} KB")
