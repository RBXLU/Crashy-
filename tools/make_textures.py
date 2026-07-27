#!/usr/bin/env python3
"""Generates Crashy!'s 16x16 textures.

Each texture is an explicit pixel map so shading stays deliberate: light comes from the
upper left, every material gets a 3-4 step ramp, and silhouettes are closed with a dark
outline the way vanilla items are.
"""
import os

from PIL import Image

OUT = os.environ["OUT"]
T = (0, 0, 0, 0)


def px(name, rows, palette):
    assert len(rows) == 16, f"{name}: {len(rows)} rows"
    for i, row in enumerate(rows):
        assert len(row) == 16, f"{name}: row {i} is {len(row)} wide -> {row!r}"

    img = Image.new("RGBA", (16, 16), T)
    p = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                p[x, y] = palette[ch]

    path = os.path.join(OUT, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", path)


# ============================================================ glue bottle
GLUE = {
    "k": (26, 24, 34, 255),      # outline
    "n": (108, 114, 128, 255),   # nozzle metal
    "N": (150, 156, 170, 255),   # nozzle highlight
    "c": (58, 190, 206, 255),    # cap
    "C": (124, 232, 242, 255),   # cap highlight
    "b": (28, 128, 148, 255),    # cap shade
    "w": (226, 228, 232, 255),   # bottle body
    "W": (250, 251, 252, 255),   # body highlight
    "d": (162, 168, 180, 255),   # body shade
    "y": (240, 178, 62, 255),    # label
    "Y": (255, 214, 122, 255),   # label highlight
    "z": (186, 126, 34, 255),    # label shade
    "o": (250, 200, 96, 255),    # glue bead
    "a": (255, 236, 176, 255),   # bead highlight
}
px("item/glue.png", [
    "................",
    ".......ao.......",
    ".......oo.......",
    "......kNnk......",
    "......kNnk......",
    ".....kcCcck.....",
    ".....kCCcbk.....",
    ".....kcccbk.....",
    "....kwWwwwdk....",
    "....kWWwwwdk....",
    "....kyYyyyzk....",
    "....kYYyyyzk....",
    "....kyYyyyzk....",
    "....kwWwwwdk....",
    "....kwwwwwdk....",
    "....kkkkkkkk....",
], GLUE)


# ============================================================ launcher
LAUNCHER = {
    "k": (22, 22, 28, 255),      # outline
    "S": (168, 176, 192, 255),   # steel highlight
    "m": (108, 116, 132, 255),   # steel mid
    "c": (62, 68, 82, 255),      # steel dark
    "e": (255, 152, 46, 255),    # energy
    "E": (255, 226, 150, 255),   # energy hot
    "r": (198, 64, 42, 255),     # accent
    "g": (96, 62, 42, 255),      # grip
    "G": (134, 92, 62, 255),     # grip highlight
}
px("item/launcher.png", [
    "...........kkk..",
    "..........kmmSk.",
    ".........kmSSmk.",
    "........kmSmmk..",
    ".....kkkkmmmk...",
    "....kceeeemk....",
    "...kcEeeeemk....",
    "...kcEeeemk.....",
    "..kmmcccmk......",
    "..kmrrmmk.......",
    "..kmrmk.........",
    "..kggmk.........",
    "..kgGgk.........",
    "..kgGgk.........",
    "..kggk..........",
    "..kkk...........",
], LAUNCHER)


# ============================================================ blueprint
BLUEPRINT = {
    "l": (24, 44, 92, 255),       # sheet outline
    "b": (42, 78, 152, 255),      # paper blue
    "B": (52, 92, 172, 255),      # paper blue, lighter band
    "w": (108, 148, 216, 255),    # faint grid
    "W": (228, 240, 255, 255),    # drawing
}
px("item/blueprint.png", [
    "................",
    "..llllllllllll..",
    ".lBBBBBBBBBBBBl.",
    ".lbwbbbwbbbwbbl.",
    ".lbbbbbbbbbbbbl.",
    ".lwwwwwwwwwwwwl.",
    ".lbbbbbWWbbbbbl.",
    ".lbwbbWWWWbbwbl.",
    ".lbbbWWbbWWbbbl.",
    ".lwwWWwwwwWWwwl.",
    ".lbbbWWbbWWbbbl.",
    ".lbwbbWWWWbbwbl.",
    ".lbbbbbWWbbbbbl.",
    ".lBBBBBBBBBBBBl.",
    "..llllllllllll..",
    "................",
], BLUEPRINT)


# ============================================================ machine chassis
def chassis(accent, accent_bright, core, core_bright, base=None):
    """Shared palette for the two machine blocks; only the glowing parts differ."""
    p = {
        "k": (16, 16, 20, 255),      # outline
        "h": (128, 134, 148, 255),   # lit bevel (upper left)
        "s": (58, 62, 74, 255),      # shaded bevel (lower right)
        "o": (176, 182, 196, 255),   # bolt
        "d": (44, 47, 57, 255),      # panel, dark
        "D": (68, 72, 85, 255),      # panel, mid
        "b": (30, 32, 40, 255),      # recess
        "c": accent,
        "C": accent_bright,
        "E": core,
        "F": core_bright,
    }
    if base:
        p.update(base)
    return p


TOP_ROWS = [
    "kkkkkkkkkkkkkkkk",
    "khhhhhhhhhhhhhsk",
    "khoDDDDDDDDDDosk",
    "khDdddddddddDdsk",
    "khDdccccccccddsk",
    "khDdcbbbbbbcddsk",
    "khDdcbEEEEbcddsk",
    "khDdcbEFFEbcddsk",
    "khDdcbEFFEbcddsk",
    "khDdcbEEEEbcddsk",
    "khDdcbbbbbbcddsk",
    "khDdccccccccddsk",
    "khDdddddddddDdsk",
    "khoDDDDDDDDDDosk",
    "kssssssssssssssk",
    "kkkkkkkkkkkkkkkk",
]

ACTIVATOR = chassis(
    accent=(34, 138, 158, 255),
    accent_bright=(86, 214, 232, 255),
    core=(150, 240, 250, 255),
    core_bright=(232, 253, 255, 255),
)
px("block/physics_activator_top.png", TOP_ROWS, ACTIVATOR)

# Spent activator: the glow drains out to a dull ember.
ACTIVATOR_OFF = chassis(
    accent=(74, 44, 42, 255),
    accent_bright=(118, 62, 54, 255),
    core=(92, 52, 46, 255),
    core_bright=(140, 74, 60, 255),
)
px("block/physics_activator_top_triggered.png", TOP_ROWS, ACTIVATOR_OFF)


# ============================================================ 3D machine parts
# These feed the multi-element block models, so they are designed to survive being
# sliced: a 3px-wide corner post samples three columns of the casing and still reads
# as a ribbed metal panel.

def casing(rib_dark, rib_mid, rib_light, trim, trim_light, trim_dark, rivet):
    palette = {
        "t": trim, "T": trim_light, "a": trim_dark,
        "D": rib_light, "m": rib_mid, "d": rib_dark,
        "o": rivet,
    }
    body = "DmdmDmdmDmdmDmdm"
    rows = ["tttttttttttttttt", "TTTTTTTTTTTTTTTT"] + [body] * 12 + ["tttttttttttttttt", "aaaaaaaaaaaaaaaa"]

    # Rivets, punched in after the ribbing so they read as hardware on top of the panel.
    rows = [list(r) for r in rows]
    for y in (4, 11):
        for x in (2, 13):
            rows[y][x] = "o"
    return ["".join(r) for r in rows], palette


px("block/machine_casing.png", *casing(
    rib_dark=(38, 41, 50, 255), rib_mid=(54, 58, 70, 255), rib_light=(76, 81, 96, 255),
    trim=(150, 112, 58, 255), trim_light=(196, 154, 84, 255), trim_dark=(104, 76, 40, 255),
    rivet=(198, 204, 218, 255)))

px("block/saver_casing.png", *casing(
    rib_dark=(36, 44, 62, 255), rib_mid=(50, 62, 88, 255), rib_light=(70, 86, 118, 255),
    trim=(174, 122, 74, 255), trim_light=(216, 162, 102, 255), trim_dark=(118, 82, 50, 255),
    rivet=(206, 216, 234, 255)))


def radial(name, ramp):
    """A glowing core: colour picked by distance from the centre, with a little dithering."""
    img = Image.new("RGBA", (16, 16), T)
    p = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            if (x + y) % 7 == 0:
                d = min(1.0, d + 0.08)
            index = min(len(ramp) - 1, int(d * len(ramp)))
            p[x, y] = ramp[index]
    path = os.path.join(OUT, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", path)


radial("block/physics_core.png", [
    (240, 254, 255, 255),
    (176, 244, 252, 255),
    (108, 224, 240, 255),
    (56, 186, 210, 255),
    (30, 134, 162, 255),
    (20, 88, 112, 255),
])

radial("block/physics_core_off.png", [
    (92, 64, 58, 255),
    (78, 52, 48, 255),
    (64, 44, 42, 255),
    (52, 38, 38, 255),
    (42, 32, 34, 255),
    (34, 26, 28, 255),
])


# Desk surface for the saver: dark boards with a metal rim.
DESK = {
    "e": (58, 62, 76, 255),      # metal rim
    "E": (86, 92, 108, 255),     # rim highlight
    "w": (92, 66, 46, 255),      # board
    "W": (112, 82, 56, 255),     # board light
    "v": (72, 52, 36, 255),      # board dark
    "s": (48, 34, 24, 255),      # seam
}
px("block/saver_desk.png", [
    "EEEEEEEEEEEEEEEE",
    "eWwWwWwWwWwWwWwe",
    "ewWwWwWwWwWwWwWe",
    "esssssssssssssse",
    "eWwWwWwWwWwWwWwe",
    "ewWwWwWwWwWwWwWe",
    "evvvvvvvvvvvvvve",
    "esssssssssssssse",
    "eWwWwWwWwWwWwWwe",
    "ewWwWwWwWwWwWwWe",
    "esssssssssssssse",
    "eWwWwWwWwWwWwWwe",
    "ewWwWwWwWwWwWwWe",
    "evvvvvvvvvvvvvve",
    "eEeEeEeEeEeEeEee",
    "eeeeeeeeeeeeeeee",
], DESK)


# The sheet pinned to the desk, in the two states.
SHEET = {
    "l": (22, 40, 86, 255),
    "b": (40, 74, 146, 255),
    "w": (96, 138, 208, 255),
    "W": (224, 238, 255, 255),
    "p": (196, 206, 226, 255),   # pins
}
SHEET_ROWS = [
    "llllllllllllllll",
    "lpbbbbbbbbbbbbpl",
    "lbbbwbbbbbwbbbbl",
    "lbwwwwwwwwwwwwbl",
    "lbbbwbbWWbbwbbbl",
    "lbbbwbWWWWbwbbbl",
    "lbbbwWWbbWWwbbbl",
    "lbwwwWWbbWWwwwbl",
    "lbwwwWWbbWWwwwbl",
    "lbbbwWWbbWWwbbbl",
    "lbbbwbWWWWbwbbbl",
    "lbbbwbbWWbbwbbbl",
    "lbwwwwwwwwwwwwbl",
    "lbbbwbbbbbwbbbbl",
    "lpbbbbbbbbbbbbpl",
    "llllllllllllllll",
]
px("block/saver_sheet.png", SHEET_ROWS, SHEET)

SHEET_LOADED = dict(SHEET)
SHEET_LOADED["b"] = (56, 100, 190, 255)
SHEET_LOADED["w"] = (150, 190, 246, 255)
SHEET_LOADED["W"] = (255, 255, 255, 255)
px("block/saver_sheet_loaded.png", SHEET_ROWS, SHEET_LOADED)
