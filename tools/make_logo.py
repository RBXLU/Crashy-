#!/usr/bin/env python3
"""Draws the Crashy! mod icon.

Deliberately in the same family as Sable's own icon — hand-drawn pixel art on a 64x64
grid, warm browns against a cool sky, soft dithered clouds, no text. Where Sable shows a
floating island at rest, this one is coming apart.
"""
import math
import os

from PIL import Image

OUT = os.environ.get("OUT", "src/main/resources/crashy_logo.png")

GRID = 64
SCALE = 4

# --------------------------------------------------------------------- palette
SKY_TOP = (126, 190, 232)
SKY_BOTTOM = (66, 132, 194)
CLOUD = (238, 247, 253)
CLOUD_SHADE = (196, 224, 244)

GRASS_LIGHT = (132, 178, 102)
GRASS = (100, 148, 78)
GRASS_DARK = (74, 114, 60)

DIRT_LIGHT = (156, 112, 76)
DIRT = (128, 88, 60)
DIRT_DARK = (100, 66, 46)

OUTLINE = (62, 40, 30)

FLASH = (255, 246, 206)
FLASH_WARM = (255, 206, 108)


class Canvas:
    def __init__(self, w, h):
        self.w = w
        self.h = h
        self.px = [[None] * w for _ in range(h)]

    def set(self, x, y, colour):
        if 0 <= x < self.w and 0 <= y < self.h and colour is not None:
            self.px[y][x] = colour

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def row(self, y, x0, x1, colour):
        for x in range(x0, x1):
            self.set(x, y, colour)

    def to_image(self, background=None):
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        p = img.load()
        for y in range(self.h):
            for x in range(self.w):
                colour = self.px[y][x] or background
                if colour:
                    p[x, y] = colour + (255,)
        return img


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# --------------------------------------------------------------------- sky
def draw_sky(canvas):
    for y in range(canvas.h):
        colour = lerp(SKY_TOP, SKY_BOTTOM, y / (canvas.h - 1))
        canvas.row(y, 0, canvas.w, colour)


def blob(canvas, cx, cy, rx, ry, colour, dither=None):
    """A soft ellipse; the dither colour stipples the outer ring so edges stay drawn, not aliased."""
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 0.72:
                canvas.set(x, y, colour)
            elif d <= 1.0 and dither and (x + y) % 2 == 0:
                canvas.set(x, y, dither)


def draw_clouds(canvas):
    # Upper left bank.
    blob(canvas, 10, 9, 11, 5, CLOUD, CLOUD_SHADE)
    blob(canvas, 18, 11, 8, 4, CLOUD, CLOUD_SHADE)
    # Upper right wisp.
    blob(canvas, 52, 7, 9, 4, CLOUD, CLOUD_SHADE)
    # Lower bank, anchoring the island.
    blob(canvas, 14, 55, 13, 6, CLOUD, CLOUD_SHADE)
    blob(canvas, 48, 58, 15, 6, CLOUD, CLOUD_SHADE)
    blob(canvas, 33, 60, 11, 5, CLOUD, CLOUD_SHADE)


# --------------------------------------------------------------------- island
# Half-widths per row, top to bottom: a grassy cap tapering into a rocky teardrop,
# the same silhouette language as Sable's island.
ISLAND_ROWS = [
    14, 15, 15, 15,          # grass cap
    15, 14, 14, 13,          # upper soil
    13, 12, 11, 10,
    9, 8, 7, 6,
    5, 5, 4, 3,
    3, 2, 2, 1,
]
GRASS_ROWS = 4


def build_island():
    """Paints the island into its own canvas so it can be split before compositing."""
    height = len(ISLAND_ROWS) + 3
    width = max(ISLAND_ROWS) * 2 + 4
    canvas = Canvas(width, height)
    cx = width // 2

    for y, half in enumerate(ISLAND_ROWS):
        if y < GRASS_ROWS:
            colour = GRASS_LIGHT if y == 0 else (GRASS if y < 3 else GRASS_DARK)
        else:
            depth = (y - GRASS_ROWS) / max(1, len(ISLAND_ROWS) - GRASS_ROWS)
            colour = DIRT_LIGHT if depth < 0.25 else (DIRT if depth < 0.65 else DIRT_DARK)
        canvas.row(y, cx - half, cx + half, colour)

    # A couple of roots hanging off the bottom, like Sable's.
    for x, extra in ((cx - 5, 3), (cx + 2, 2), (cx - 1, 1)):
        for y in range(len(ISLAND_ROWS), len(ISLAND_ROWS) + extra):
            canvas.set(x, y, DIRT_DARK)

    # Scattered lighter specks so the soil is not a flat fill.
    for x, y in ((cx - 8, 8), (cx + 5, 10), (cx - 3, 13), (cx + 8, 7), (cx - 10, 6), (cx + 2, 16)):
        if canvas.get(x, y):
            canvas.set(x, y, DIRT_LIGHT)

    return canvas, cx


def outline_shape(canvas):
    """Wraps whatever is painted in a dark edge, the way drawn pixel art reads best."""
    additions = []
    for y in range(canvas.h):
        for x in range(canvas.w):
            if canvas.get(x, y) is not None:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if canvas.get(x + dx, y + dy) not in (None, OUTLINE):
                    additions.append((x, y))
                    break
    for x, y in additions:
        canvas.set(x, y, OUTLINE)


def split_island(island, cx):
    """Cracks the island along a diagonal and returns the two halves as pixel lists."""
    left, right = [], []
    for y in range(island.h):
        for x in range(island.w):
            colour = island.get(x, y)
            if colour is None:
                continue
            # Diagonal fracture running down-left; everything right of it breaks away.
            boundary = cx + 2 - (y - 6) * 0.55
            (right if x > boundary else left).append((x, y, colour))
    return left, right


def paste(canvas, pixels, ox, oy):
    for x, y, colour in pixels:
        canvas.set(x + ox, y + oy, colour)


def draw_shard(canvas, x, y, size, grassy):
    """A small chunk of the island, thrown clear."""
    for dy in range(size):
        for dx in range(size):
            top = dy == 0 and grassy
            canvas.set(x + dx, y + dy, GRASS if top else (DIRT if dy < size - 1 else DIRT_DARK))
    for dx in range(-1, size + 1):
        canvas.set(x + dx, y - 1, OUTLINE)
        canvas.set(x + dx, y + size, OUTLINE)
    for dy in range(size):
        canvas.set(x - 1, y + dy, OUTLINE)
        canvas.set(x + size, y + dy, OUTLINE)


def draw_flash(canvas, cx, cy):
    """A warm burst at the fracture — the only thing on the icon that is not scenery."""
    for y in range(cy - 8, cy + 9):
        for x in range(cx - 9, cx + 10):
            d = math.hypot((x - cx) / 8.0, (y - cy) / 6.5)
            if d < 0.4:
                canvas.set(x, y, FLASH)
            elif d < 0.7:
                canvas.set(x, y, FLASH_WARM)
            elif d < 1.0 and (x + y) % 2 == 0:
                canvas.set(x, y, FLASH_WARM)

    # Four short rays, kept sparse so the icon stays minimal.
    for length, (dx, dy) in ((7, (1, 0)), (6, (-1, 0)), (5, (0, -1)), (5, (0, 1))):
        for i in range(3, length):
            if i % 2 == 0:
                canvas.set(cx + dx * i, cy + dy * i, FLASH_WARM)


# --------------------------------------------------------------------- compose
def main():
    canvas = Canvas(GRID, GRID)
    draw_sky(canvas)
    draw_clouds(canvas)

    island, cx = build_island()
    outline_shape(island)
    left, right = split_island(island, cx)

    base_x = GRID // 2 - island.w // 2
    base_y = 17

    # Close enough that it still reads as one island, far enough that it reads as breaking.
    paste(canvas, left, base_x - 2, base_y)
    paste(canvas, right, base_x + 4, base_y - 2)

    draw_flash(canvas, GRID // 2 + 1, base_y + 6)

    draw_shard(canvas, 12, 24, 4, True)
    draw_shard(canvas, 48, 21, 3, True)
    draw_shard(canvas, 45, 38, 4, False)
    draw_shard(canvas, 16, 41, 3, False)

    image = canvas.to_image()
    image = image.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)
    os.makedirs(os.path.dirname(OUT) or ".", exist_ok=True)
    image.convert("RGB").save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
