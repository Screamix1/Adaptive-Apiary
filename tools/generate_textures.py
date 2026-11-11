#!/usr/bin/env python3
"""Рисует все текстуры мода. Запуск: python3 tools/generate_textures.py

Пиксель-арт задан прямо в коде (строки символов и простая геометрия).
Любой PNG можно заменить своим: имена файлов менять не нужно.
Нужен пакет Pillow (pip install pillow).
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/adaptiveapiary/textures"


def save(img, rel):
    path = TEX / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def sprite(rows, palette):
    """Собирает картинку 16x16 из строк символов. '.' - прозрачный пиксель."""
    assert len(rows) == 16, len(rows)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


def rgb(hex_color, alpha=255):
    return ((hex_color >> 16) & 255, (hex_color >> 8) & 255, hex_color & 255, alpha)


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3]) + (color[3],)


# ---------------------------------------------------------------- окрашиваемые слои
# Пчёлы и соты собираются из трёх слоёв. Слой A и слой B игра красит цветами вида
# (умножением), поэтому они хранятся оттенками серого; слой U не красится.
# Пиксель описывается как ("A", яркость), ("B", яркость) или ("U", (r, g, b, a)).

def save_layers(px, names):
    """px: {(x, y): значение}. names: три имени файла для слоёв A, B и U."""
    layers = [Image.new("RGBA", (16, 16), (0, 0, 0, 0)) for _ in range(3)]
    for (x, y), (kind, data) in px.items():
        if kind == "U":
            layers[2].putpixel((x, y), data)
        else:
            g = max(0, min(255, round(data * 255)))
            layers[0 if kind == "A" else 1].putpixel((x, y), (g, g, g, 255))
    for img, name in zip(layers, names):
        save(img, name)


def U(hex_color, alpha=255):
    return ("U", rgb(hex_color, alpha))


# ---------------------------------------------------------------- пчёлы
# Пчела смотрит влево. A - контур (основной цвет вида), B - тело (второй цвет вида).

BEE_PALETTE = {
    "R": ("A", 1.0), "r": ("A", 0.68),
    "K": U(0x17110C), "k": U(0x3A3029),
    "g": U(0x8C866C), "G": U(0x5A553F), "e": U(0xFFFFFF),
    "Y": ("B", 1.0), "y": ("B", 0.84), "o": ("B", 0.68),
    "1": U(0xFAFDFF, 240), "2": U(0xD8E4EE, 230), "v": U(0xA9BCCD, 240),
    "h": U(0xE1EAF2, 225), "H": U(0xC0CFDC, 230),
    "C": U(0xFFD94A), "c": U(0xD99A1E), "j": U(0xE8352B), "J": U(0x6FD3FF),
}

# R - контур крыльев и головы, остальной контур дорисовывается автоматически
BEE = [
    "..........RRRR..",
    ".........R11v2R.",
    "........R11v22R.",
    ".......R11v22RR.",
    ".......R1v22RhhR",
    "......R1v22RhHhR",
    "..RRR.Rv22RhHhR.",
    "..kkK..YkYkYk...",
    ".KegKKYYKYKYKY..",
    ".KgGKKYYKYKYKYK.",
    ".KKKKKyyKyKyKyKK",
    "..KKK.yyKyKyKy..",
    ".......oKoKoK...",
    ".......K.K.K....",
    "................",
    "................",
]

# украшения над головой: диадема принцессы и корона королевы
BEE_EXTRAS = {
    "drone": {},
    "princess": {(2, 4): "C", (3, 3): "C", (4, 4): "C", (2, 5): "c", (3, 5): "J", (4, 5): "c"},
    "queen": {(1, 3): "C", (3, 2): "C", (5, 3): "C",
              (1, 4): "c", (2, 4): "C", (3, 3): "C", (3, 4): "j", (4, 4): "C", (5, 4): "c",
              (1, 5): "c", (2, 5): "c", (3, 5): "c", (4, 5): "c", (5, 5): "c"},
}


def outline_px(px, outer=("A", 1.0), shadow=("A", 0.68), skip=("A",)):
    """Контур вокруг фигуры (кроме уже нарисованного контура). Снизу и справа он темнее."""
    def solid(x, y):
        v = px.get((x, y))
        return v is not None and v[0] not in skip
    add = {}
    for y in range(16):
        for x in range(16):
            if (x, y) in px:
                continue
            up, left, down, right = solid(x, y - 1), solid(x - 1, y), solid(x, y + 1), solid(x + 1, y)
            if up or left or down or right:
                add[(x, y)] = shadow if (up or left) and not (down or right) else outer
    px.update(add)


# второй кадр взмаха для летающих пчёл: крылья опущены, тело то же самое
BEE_FLAP = [
    "................",
    "................",
    "................",
    "................",
    "..........RRRR..",
    "........RR21vhR.",
    "..RRR..R1122vhhR",
] + BEE[7:]


def bee_px(kind, art=BEE):
    assert len(art) == 16 and all(len(r) == 16 for r in art)
    extra = BEE_EXTRAS[kind]
    px = {(x, y): BEE_PALETTE[ch] for y, row in enumerate(art) for x, ch in enumerate(row) if ch != "."}
    if extra:
        for (x, y), ch in extra.items():
            px[(x, y)] = BEE_PALETTE[ch]
        # корона сидит на голове: верхний контур головы под ней убираем
        for x in range(1, 6):
            if px.get((x, 6)) == BEE_PALETTE["R"]:
                del px[(x, 6)]
    outline_px(px)
    return px


def bee_textures():
    for kind in BEE_EXTRAS:
        save_layers(bee_px(kind), [f"item/bee_{kind}_outline.png", "item/bee_body.png", f"item/bee_{kind}_extra.png"])


# ---------------------------------------------------------------- соты
# Круглый срез сот на правильной шестиугольной сетке. A - мёд, B - восковые стенки.

COMB_CELL = [4, 6, 8, 8, 8, 6, 4]   # ширина ячейки по строкам: шестиугольник 8x7
COMB_DX, COMB_PERIOD = 7, 8         # шаг колонок и период; соседние колонки сдвинуты на полпериода
COMB_GLOSS, COMB_GLOSS_SOFT, COMB_OUT = U(0xFFFDF2), U(0xFFF2C4, 210), U(0x3E2306)


def comb_cell_at(x, y, left=4, top=4):
    """(строка, столбец в строке, ширина строки) для пикселя внутри ячейки или None для стенки."""
    width = max(COMB_CELL)
    for i in range(-3, 5):
        x0 = left + i * COMB_DX
        shift = COMB_PERIOD // 2 if i % 2 else 0
        for j in range(-3, 5):
            r = y - (top + j * COMB_PERIOD + shift)
            if 0 <= r < len(COMB_CELL):
                w = COMB_CELL[r]
                pad = (width - w) // 2
                if x0 + pad <= x < x0 + pad + w:
                    return r, x - x0 - pad, w
    return None


def comb_honey(r, col, w):
    last = len(COMB_CELL) - 1
    if r == 1 and col == 1:
        return COMB_GLOSS
    if r == 1 and col == 2 and w >= 6:
        return COMB_GLOSS_SOFT
    if r == 0 or col == 0:
        return ("A", 0.8)
    if r == last or col == w - 1:
        return ("A", 0.86)
    if r >= last - 1:
        return ("A", 0.93)
    return ("A", 1.0)


def comb_textures():
    mask = {(x, y) for y in range(16) for x in range(16) if math.hypot(x + 0.5 - 8.0, y + 0.5 - 8.0) <= 7.4}
    px = {}
    for x, y in mask:
        hit = comb_cell_at(x, y)
        px[(x, y)] = ("B", 0.62) if hit is None else comb_honey(*hit)
    # край пластины: сверху-слева светлее, снизу-справа темнее
    for x, y in mask:
        if (x - 1, y) not in mask or (x, y - 1) not in mask:
            px[(x, y)] = ("B", 0.8)
        elif (x + 1, y) not in mask or (x, y + 1) not in mask:
            px[(x, y)] = ("B", 0.5)
    for y in range(16):
        for x in range(16):
            if (x, y) not in mask and any((x + dx, y + dy) in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[(x, y)] = COMB_OUT
    save_layers(px, ["item/comb_honey.png", "item/comb_walls.png", "item/comb_details.png"])


# ---------------------------------------------------------------- предметы
# Тот же стиль, что у пчёл и сот: тёмно-коричневый контур, свет сверху-слева,
# 3-5 тонов на объём и белый блик.

ITEM_OUT = rgb(0x3E2306)     # контур, как у сот
GLOSS = rgb(0xFFFDF2)
SHAPE_SS = 8
NEAR = ((1, 0), (-1, 0), (0, 1), (0, -1))


def shape_cover(inside, x, y):
    """Доля пикселя (x, y), попавшая внутрь фигуры: считается по сетке 8x8 точек."""
    hits = 0
    for sy in range(SHAPE_SS):
        for sx in range(SHAPE_SS):
            if inside(x + (sx + 0.5) / SHAPE_SS, y + (sy + 0.5) / SHAPE_SS):
                hits += 1
    return hits / (SHAPE_SS * SHAPE_SS)


def raster(inside, thr=0.5):
    return {(x, y) for y in range(16) for x in range(16) if shape_cover(inside, x, y) >= thr}


def edge(mask):
    """Крайние пиксели фигуры."""
    return {(x, y) for x, y in mask if any((x + dx, y + dy) not in mask for dx, dy in NEAR)}


def ring(mask):
    """Пиксели вокруг фигуры (контур снаружи)."""
    return {(x + dx, y + dy) for x, y in mask for dx, dy in NEAR
            if (x + dx, y + dy) not in mask and 0 <= x + dx < 16 and 0 <= y + dy < 16}


def to_img(px):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c)
    return img


def art_px(art, pal):
    """Пиксели из строк символов 16x16."""
    assert len(art) == 16, len(art)
    px = {}
    for y, r in enumerate(art):
        assert len(r) == 16, (y, r, len(r))
        for x, ch in enumerate(r):
            if ch != ".":
                px[(x, y)] = pal[ch]
    return px


# ---------------------------------------------------------------- капля мёда

def honey_drop():
    art = [
        "................",
        ".......oo.......",
        "......oLMo......",
        "......oLMo......",
        ".....oLLMMo.....",
        ".....oLLMMo.....",
        "....oLWLMMMo....",
        "....oWLLMMMo....",
        "...oLWLMMMMDo...",
        "...oHLMMMMMDo...",
        "...oLMMMMMMDo...",
        "...oLMMMMMLDo...",
        "...oMMMMMLDDo...",
        "....oDMMMDDo....",
        ".....oooooo.....",
        "................",
    ]
    return art_px(art, {"o": rgb(0x5A2D05), "W": GLOSS, "H": rgb(0xFFE29A), "L": rgb(0xFFBE3A),
                      "M": rgb(0xF39A1C), "D": rgb(0xC8660E)})


# ---------------------------------------------------------------- медвяная роса: капля на листке

def honeydew():
    # листок наискосок, на нём бусина медвяной росы
    ax, ay, bx, by = 0.4, 14.6, 15.6, 7.8
    L = math.hypot(bx - ax, by - ay)
    ux, uy = (bx - ax) / L, (by - ay) / L

    def coords(x, y):
        return ((x - ax) * ux + (y - ay) * uy) / L, -(x - ax) * uy + (y - ay) * ux

    def leaf(x, y):
        t, n = coords(x, y)
        if not 0 <= t <= 1:
            return False
        w = 3.3 * math.sin(math.pi * t) ** 0.7
        return -w <= n <= w * 0.8
    lm = raster(leaf)
    px = {}
    for x, y in lm:
        t, n = coords(x + 0.5, y + 0.5)
        px[(x, y)] = rgb(0xA6D85E) if n < -1.4 else rgb(0x78B83E) if n < 0.5 else rgb(0x52922A)
        if abs(n) < 0.5 and 0.1 < t < 0.93:
            px[(x, y)] = rgb(0x3A6E1E)
    for p in edge(lm):
        px[p] = rgb(0x1C3A0E)
    bead = [
        "................",
        "................",
        ".....ooooo......",
        "....oLWLLMo.....",
        "...oLWLLLMMo....",
        "...oWLLLMMMo....",
        "...oLLLMMMDo....",
        "...oLLMMMMDo....",
        "...oLMMMMDDo....",
        "....oMMMDDo.....",
        ".....ooooo......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    px.update(art_px(bead, {"o": rgb(0x6A5A10), "W": GLOSS, "L": rgb(0xFBF6C0), "M": rgb(0xEDE07A),
                          "D": rgb(0xC9B848)}))
    return px


# ---------------------------------------------------------------- прополис: смолистый комок с подтёком

def propolis():
    # тёмная смола: комок с подтёком, с глянцевыми бликами и крошками коры
    art = [
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooHWLLMMoo...",
        "..oHWLLMMMMMMo..",
        "..oWLMMMMMMMMDo.",
        ".oLLMMMMMMMMMDo.",
        ".oLMMMdMMMMMDDo.",
        ".oMMMMMMMMMdDDo.",
        ".oMMMMMMMMDDDDo.",
        "..oMMMMMDDDDDo..",
        "...ooooMDoooo...",
        "......oMDo......",
        "......oDDo......",
        ".......oo.......",
    ]
    return art_px(art, {"o": rgb(0x2A1204), "W": rgb(0xFFF0D0), "H": rgb(0xF0A848), "L": rgb(0xCC7A1E),
                      "m": rgb(0xE09038), "M": rgb(0xA0520E), "D": rgb(0x6E3008), "d": rgb(0x4A2006)})


# ---------------------------------------------------------------- пчелиный воск: брусок-кубик (силуэт шестиугольника)

def beeswax():
    # брусок воска в изометрии; силуэт - шестиугольник, как ячейка сот
    px = {}
    for y in range(16):
        for x in range(16):
            xc, yc = x + 0.5, y + 0.5
            a = abs(xc - 8.0)
            if a > 7.0 or yc < 1.0 + 0.5 * a or yc > 14.0 - 0.5 * a:
                continue
            if yc < 7.75 - 0.5 * a + 0.01:
                px[(x, y)] = "T"
            else:
                px[(x, y)] = "L" if xc < 8.0 else "R"
    m = set(px)
    col = {}
    for (x, y), f in px.items():
        col[(x, y)] = {"T": rgb(0xFFF1B8), "L": rgb(0xEDBE48), "R": rgb(0xBE8226)}[f]
    for (x, y), f in px.items():
        above = px.get((x, y - 1))
        if f == "T" and ((x - 1, y) not in m or (x, y - 1) not in m):
            col[(x, y)] = rgb(0xFFFBE4)
        elif f == "L" and above == "T":
            col[(x, y)] = rgb(0xFADC80)
        elif f == "R" and above == "T":
            col[(x, y)] = rgb(0xD39A38)
        elif f == "L" and px.get((x + 1, y)) == "R":
            col[(x, y)] = rgb(0xF6D270)
    # оттиск ячейки сот на крышке
    for p in [(7, 3), (8, 3), (6, 4), (9, 4), (7, 5), (8, 5)]:
        col[p] = rgb(0xE8C870)
    for p in [(7, 4), (8, 4)]:
        col[p] = rgb(0xF4DC94)
    for p in edge(m):
        col[p] = ITEM_OUT
    return col


# ---------------------------------------------------------------- сачок: обод с сеткой и деревянная ручка

def scoop():
    """Сачок: обод-эллипс сверху, в горловине тёмное нутро, под ним висит мешок сетки
    и качнулся кончиком вправо; ручка крепится к ободу медным кольцом."""
    art = [
        ".......oooooo...",
        ".....ooHGGGGgoo.",
        "....oGXXXXXXXXdo",
        "....oGXXXXXXXXdo",
        "....oggXXXXXXddo",
        "...Yyoggggddddo.",
        "..oWwoNNNNNNNNo.",
        "..oWwoNNNNNNNNo.",
        ".oWwo.oNNNNNNNNo",
        ".oWwo.oNNNNNNNNo",
        "oWwo...oNNNNNNNo",
        "oWwo...oNNNNNNo.",
        "Wwo.....oNNNNNo.",
        "Wwo......oNNNNo.",
        "wo........oNNo..",
        "o..........oo...",
    ]
    pal = {"o": ITEM_OUT, "H": rgb(0xFFF0B8), "G": rgb(0xF4CC72), "g": rgb(0xD0923E), "d": rgb(0xA0662A),
           "W": rgb(0xC08848), "w": rgb(0x7A4E22), "Y": rgb(0xFFE07A), "y": rgb(0xD99A1E)}
    net_tone = [rgb(0xFFFCF4, 250), rgb(0xE8E2D2, 245), rgb(0xC9C0AA, 240), rgb(0xA89E86, 240)]
    hole, hole_shade = rgb(0xD8D0BC, 80), rgb(0x8A806A, 130)
    mouth_tone = [rgb(0x4A4034), rgb(0x5E5444), rgb(0x7A705C)]
    px, net, mouth = {}, [], []
    for y, row in enumerate(art):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch == "N":
                net.append((x, y))
            elif ch == "X":
                mouth.append((x, y))
            elif ch != ".":
                px[(x, y)] = pal[ch]
    # горловина: тёмное нутро, сквозь него видна сетка задней стенки
    for x, y in mouth:
        knot = (x + y) % 2 == 0
        if y == 2:
            px[(x, y)] = net_tone[2] if knot else mouth_tone[2]
        elif y == 3:
            px[(x, y)] = net_tone[3] if knot else mouth_tone[1]
        else:
            px[(x, y)] = mouth_tone[1] if knot else mouth_tone[0]
    # мешок: узлы через пиксель, между ними просвет; свет слева, тень справа, снизу и под ободом
    rows = {}
    for x, y in net:
        rows.setdefault(y, []).append(x)
    top, bottom = min(rows), max(rows)
    for y, xs in rows.items():
        left, right = min(xs), max(xs)
        for x in xs:
            t = (x - left) / max(1, right - left)
            knot = (x + y) % 2 == 0
            if y == top:
                px[(x, y)] = net_tone[2] if knot else hole_shade
            elif y == bottom:
                px[(x, y)] = net_tone[3]
            elif x == right:
                px[(x, y)] = net_tone[2] if knot else net_tone[3]
            elif x == left:
                px[(x, y)] = net_tone[1] if knot else hole
            elif knot:
                px[(x, y)] = net_tone[0] if t < 0.35 else net_tone[1] if t < 0.65 else net_tone[2] if t < 0.85 else net_tone[3]
            else:
                px[(x, y)] = hole
    return px


# ---------------------------------------------------------------- рамка: деревянная рамка улья с сотами внутри

def lattice_cell(x, y, profile, dx, period, left, top):
    width = max(profile)
    for i in range(-4, 6):
        x0 = left + i * dx
        shift = period // 2 if i % 2 else 0
        for j in range(-4, 6):
            r = y - (top + j * period + shift)
            if 0 <= r < len(profile):
                w = profile[r]
                pad = (width - w) // 2
                if x0 + pad <= x < x0 + pad + w:
                    return r, x - x0 - pad, w
    return None


def frame():
    # рамка улья: широкая верхняя планка с ушками, внутри соты с мёдом
    px = {}
    wood_l, wood, wood_d = rgb(0xE0B271), rgb(0xB8843F), rgb(0x7E5326)
    for x in range(0, 16):
        px[(x, 2)] = wood_l
        px[(x, 3)] = wood if 0 < x < 15 else wood_d
    for y in range(4, 13):
        px[(1, y)], px[(2, y)] = wood_l, wood
        px[(13, y)], px[(14, y)] = wood, wood_d
    for x in range(1, 15):
        px[(x, 12)] = wood if x < 14 else wood_d
        px[(x, 13)] = wood_d
    profile = [2, 4, 4, 4, 2]
    wall = rgb(0xB06A16)
    for y in range(4, 12):
        for x in range(3, 13):
            hit = lattice_cell(x, y, profile, 4, 6, 3, 4)
            if hit is None:
                px[(x, y)] = wall
            else:
                r, c, w = hit
                px[(x, y)] = GLOSS if (r == 1 and c == 1) else rgb(0xFFD25A) if r <= 1 else rgb(0xFFBA2E) if r < 4 else rgb(0xF0A222)
    for p in ring(set(px)):
        px[p] = ITEM_OUT
    return px


# ---------------------------------------------------------------- пчелоанализатор

def beealyzer():
    art = [
        "................",
        "...oooooooooo...",
        "..oWLLLLLLLLMo..",
        "..oLkkkkkkkkMo..",
        "..oLkggwwgggkMo.",
        "..oLkgYKYKggkMo.",
        "..oLkeYKYKYgkMo.",
        "..oLkgYKYKggkMo.",
        "..oLkgggggggkMo.",
        "..oLkkkkkkkkMo..",
        "..oLMMMMMMMMMo..",
        "..oLrRMMMbBMMo..",
        "..oLRRMMMBBMDo..",
        "..oMMMMMMMMMDo..",
        "...oooooooooo...",
        "................",
    ]
    pal = {"o": ITEM_OUT, "W": rgb(0xFFE7A8), "L": rgb(0xEDB65E), "M": rgb(0xC98A3A), "D": rgb(0x9A6224),
           "k": rgb(0x2A1A0B), "g": rgb(0x2E5E3A), "w": rgb(0xE8F4FF), "Y": rgb(0xFFD23A), "K": rgb(0x1A1208),
           "e": rgb(0x1A1208), "r": rgb(0xFF7A6A), "R": rgb(0xD8352A), "b": rgb(0x8AD8FF), "B": rgb(0x2E8AD8)}
    return art_px(art, pal)


def product_textures():
    save(to_img(beeswax()), "item/beeswax.png")
    save(to_img(honey_drop()), "item/honey_drop.png")
    save(to_img(honeydew()), "item/honeydew.png")
    save(to_img(propolis()), "item/propolis.png")


def tool_textures():
    save(to_img(scoop()), "item/scoop.png")
    save(to_img(frame()), "item/frame.png")
    save(to_img(beealyzer()), "item/beealyzer.png")


# ---------------------------------------------------------------- блоки

def noise(img, rng, amount):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            d = rng.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)


def hive_textures(name, base, seed):
    rng = random.Random(seed)
    base = rgb(base)
    side = Image.new("RGBA", (16, 16), base)
    d = ImageDraw.Draw(side)
    # слои улья: тёмные швы каждые 4 пикселя и светлая кромка под ними
    for y in (3, 7, 11, 15):
        d.line([(0, y), (15, y)], fill=shade(base, 0.62))
        d.line([(0, (y + 1) % 16), (15, (y + 1) % 16)], fill=shade(base, 1.18))
    for y0 in (0, 4, 8, 12):
        for _ in range(3):
            x = rng.randrange(16)
            d.point((x, y0 + 1 + rng.randrange(2)), fill=shade(base, 0.8))
    # леток
    d.rectangle([6, 8, 9, 10], fill=(24, 18, 12, 255))
    d.point((6, 8), fill=shade(base, 0.62))
    d.point((9, 8), fill=shade(base, 0.62))
    d.line([(0, 0), (0, 15)], fill=shade(base, 0.85))
    d.line([(15, 0), (15, 15)], fill=shade(base, 0.72))
    noise(side, rng, 7)
    save(side, f"block/{name}_side.png")

    top = Image.new("RGBA", (16, 16), shade(base, 0.72))
    d = ImageDraw.Draw(top)
    for i, f in enumerate((0.9, 1.05, 0.82, 1.12, 0.9, 1.05, 0.8)):
        d.rectangle([1 + i, 1 + i, 14 - i, 14 - i], outline=shade(base, f))
    d.rectangle([7, 7, 8, 8], fill=shade(base, 0.6))
    noise(top, rng, 6)
    save(top, f"block/{name}_top.png")


def apiary_textures():
    rng = random.Random(11)
    wood, dark, light = rgb(0xB8935A), rgb(0x6E5330), rgb(0xD4B27A)

    side = Image.new("RGBA", (16, 16), wood)
    d = ImageDraw.Draw(side)
    d.rectangle([0, 0, 15, 15], outline=dark)
    for y in (4, 8, 12):
        d.line([(1, y), (14, y)], fill=dark)
        d.line([(1, y + 1), (14, y + 1)], fill=light)
    d.rectangle([5, 12, 10, 13], fill=(26, 18, 10, 255))   # леток
    d.rectangle([4, 14, 11, 14], fill=light)                # прилётная доска
    d.rectangle([6, 2, 9, 2], fill=rgb(0xF2C230))           # жёлтая метка
    noise(side, rng, 6)
    save(side, "block/apiary_side.png")

    top = Image.new("RGBA", (16, 16), wood)
    d = ImageDraw.Draw(top)
    d.rectangle([0, 0, 15, 15], outline=dark)
    d.rectangle([2, 2, 13, 13], outline=dark, fill=shade(wood, 0.8))
    for x in (4, 7, 10):                                    # рамки, вид сверху
        d.rectangle([x, 3, x + 1, 12], fill=light)
        d.line([(x, 3), (x, 12)], fill=rgb(0xF0DCA8))
    noise(top, rng, 6)
    save(top, "block/apiary_top.png")

    bottom = Image.new("RGBA", (16, 16), shade(wood, 0.75))
    d = ImageDraw.Draw(bottom)
    d.rectangle([0, 0, 15, 15], outline=dark)
    for x in (5, 10):
        d.line([(x, 1), (x, 14)], fill=dark)
    noise(bottom, rng, 6)
    save(bottom, "block/apiary_bottom.png")


def centrifuge_textures():
    rng = random.Random(23)
    metal, dark, light = rgb(0xA9AEB5), rgb(0x565B63), rgb(0xD5D9DE)

    def plate():
        img = Image.new("RGBA", (16, 16), metal)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 15, 15], outline=dark)
        d.line([(1, 1), (14, 1)], fill=light)
        d.line([(1, 1), (1, 14)], fill=light)
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):   # заклёпки
            d.point((x, y), fill=dark)
        return img, d

    side, d = plate()
    d.ellipse([4, 4, 11, 11], fill=dark)                    # смотровое окно
    d.ellipse([5, 5, 10, 10], fill=rgb(0xE8A32B))
    d.point((6, 6), fill=rgb(0xFFE08A))
    d.point((7, 6), fill=rgb(0xFFE08A))
    d.line([(5, 9), (10, 9)], fill=rgb(0xC97A14))
    noise(side, rng, 5)
    save(side, "block/centrifuge_side.png")

    top, d = plate()
    d.ellipse([3, 3, 12, 12], fill=dark)                    # ротор
    d.ellipse([4, 4, 11, 11], fill=shade(metal, 0.8))
    d.line([(4, 4), (11, 11)], fill=dark)
    d.line([(11, 4), (4, 11)], fill=dark)
    d.rectangle([7, 7, 8, 8], fill=light)
    noise(top, rng, 5)
    save(top, "block/centrifuge_top.png")

    bottom, d = plate()
    d.rectangle([4, 4, 11, 11], outline=dark)
    noise(bottom, rng, 5)
    save(bottom, "block/centrifuge_bottom.png")


# ---------------------------------------------------------------- частица пчелы

def particle_textures():
    """Летающая пчела: та же картинка, что у трутня в инвентаре, и те же три слоя
    (контур красится основным цветом вида, тело - вторым). Два кадра: крылья вверх и вниз."""
    for i, art in enumerate((BEE, BEE_FLAP)):
        save_layers(bee_px("drone", art), [f"particle/bee_outline_{i}.png", "particle/bee_body.png", f"particle/bee_extra_{i}.png"])
    for old in ("particle/bee_0.png", "particle/bee_1.png"):
        (TEX / old).unlink(missing_ok=True)


# ---------------------------------------------------------------- интерфейс
# Медовая тема: деревянная рамка, светлый "пергамент", тёплые слоты,
# для пасеки выходные слоты в виде шестиугольников.

BORDER = rgb(0x2A1A0B)
WOOD_L, WOOD, WOOD_D = rgb(0xC08A4A), rgb(0x95622F), rgb(0x5E3A18)
PARCH, PARCH_L, PARCH_D, PARCH_LINE = rgb(0xF2E2B6), rgb(0xFBF1D4), rgb(0xD8C08A), rgb(0xE9D6A2)
SLOT_D, SLOT_L, SLOT_IN = rgb(0x6B4A22), rgb(0xFFF6DA), rgb(0xB8925A)
BAR_BG = rgb(0x4A3218)
HONEY_L, HONEY, HONEY_D = rgb(0xFFE07A), rgb(0xF5A623), rgb(0xC07614)


def wood_panel(width, height):
    """Деревянная рамка толщиной 4 пикселя и пергамент внутри; углы скруглены."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, width - 1, height - 1], radius=3, fill=BORDER)
    d.rounded_rectangle([1, 1, width - 2, height - 2], radius=2, fill=WOOD)
    # свет сверху-слева, тень снизу-справа
    d.line([(2, 1), (width - 3, 1)], fill=WOOD_L)
    d.line([(1, 2), (1, height - 3)], fill=WOOD_L)
    d.line([(2, height - 2), (width - 3, height - 2)], fill=WOOD_D)
    d.line([(width - 2, 2), (width - 2, height - 3)], fill=WOOD_D)
    # волокна дерева
    rng = random.Random(width * 31 + height)
    for _ in range(width // 3):
        x = rng.randrange(3, width - 3)
        y = rng.choice([2, height - 3]) if rng.random() < 0.5 else rng.randrange(3, height - 3)
        if 2 <= y <= 3 or height - 4 <= y <= height - 3 or x <= 3 or x >= width - 4:
            d.point((x, y), fill=shade(WOOD, 0.88))
    # пергамент
    d.rectangle([4, 4, width - 5, height - 5], fill=PARCH)
    d.line([(4, 4), (width - 5, 4)], fill=PARCH_D)
    d.line([(4, 4), (4, height - 5)], fill=PARCH_D)
    d.line([(5, height - 5), (width - 5, height - 5)], fill=PARCH_L)
    d.line([(width - 5, 5), (width - 5, height - 5)], fill=PARCH_L)
    return img, d


def hex_pattern(d, x0, y0, x1, y1):
    """Едва заметные соты на фоне рабочей области."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            col = (x - x0) // 9
            yy = (y - y0 + (5 if col % 2 else 0)) % 10
            xx = (x - x0) % 9
            edge = (xx == 0 and 2 <= yy <= 7) or (yy == 0 and 2 <= xx <= 6) or \
                   (xx in (1, 8) and yy in (1, 8)) or (xx in (1,) and yy in (9,))
            if edge:
                d.point((x, y), fill=PARCH_LINE)


def slot(d, x, y, ghost=None):
    """Слот вокруг предмета, который рисуется в (x, y); ghost - значок пустого слота."""
    d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=SLOT_IN)
    d.line([(x - 1, y - 1), (x + 15, y - 1)], fill=SLOT_D)
    d.line([(x - 1, y - 1), (x - 1, y + 15)], fill=SLOT_D)
    d.line([(x, y + 16), (x + 16, y + 16)], fill=SLOT_L)
    d.line([(x + 16, y), (x + 16, y + 16)], fill=SLOT_L)
    if ghost:
        for (gx, gy), color in ghost.items():
            d.point((x + gx, y + gy), fill=color)


def hex_slot(d, x, y):
    """Шестиугольный слот 24x20 вокруг предмета в (x, y)."""
    cx, cy = x + 8, y + 8
    pts = [(cx - 12, cy), (cx - 6, cy - 10), (cx + 6, cy - 10), (cx + 12, cy), (cx + 6, cy + 10), (cx - 6, cy + 10)]
    d.polygon([(px - 0.5 if px < cx else px - 0.5, py - 0.5) for px, py in pts], fill=SLOT_IN)
    d.line([pts[0], pts[1], pts[2]], fill=SLOT_D)
    d.line([pts[2], pts[3], pts[4], pts[5], pts[0]], fill=SLOT_L)
    d.line([pts[5], pts[0]], fill=SLOT_D)


def player_slots(d, left, top):
    for row in range(3):
        for col in range(9):
            slot(d, left + col * 18, top + row * 18)
    for col in range(9):
        slot(d, left + col * 18, top + 58)


def separator(d, x0, x1, y):
    d.line([(x0, y), (x1, y)], fill=PARCH_D)
    d.line([(x0, y + 1), (x1, y + 1)], fill=PARCH_L)


# Значки пустых слотов: силуэт настоящего предмета в два тона, как вдавленный в дерево слота.
GHOST_DARK, GHOST_MID, GHOST_LIGHT = rgb(0x6E4E26), rgb(0x977140), rgb(0xCDAA70)


def ghost_icon(img):
    """{(x, y): цвет} - силуэт картинки предмета: тёмное (контур, полосы) темнее, светлое светлее."""
    out = {}
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a < 100:
                continue
            luma = 0.299 * r + 0.587 * g + 0.114 * b
            out[(x, y)] = GHOST_DARK if luma < 85 else GHOST_MID if luma < 190 else GHOST_LIGHT
    return out


def tinted_item(names, colors):
    """Собирает предмет из слоёв, как это делает игра: слой умножается на свой цвет."""
    base = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for name, color in zip(names, colors):
        layer = Image.open(TEX / name).convert("RGBA")
        if color is not None:
            cr, cg, cb = (color >> 16) & 255, (color >> 8) & 255, color & 255
            lp = layer.load()
            for y in range(16):
                for x in range(16):
                    r, g, b, a = lp[x, y]
                    lp[x, y] = (r * cr // 255, g * cg // 255, b * cb // 255, a)
        base.alpha_composite(layer)
    return base


def ghost_icons():
    queen = tinted_item(["item/bee_queen_outline.png", "item/bee_body.png", "item/bee_queen_extra.png"], [0x3A2A14, 0xE8C060, None])
    drone = tinted_item(["item/bee_drone_outline.png", "item/bee_body.png", "item/bee_drone_extra.png"], [0x3A2A14, 0xE8C060, None])
    comb = tinted_item(["item/comb_honey.png", "item/comb_walls.png", "item/comb_details.png"], [0xFFC233, 0x6A4010, None])
    frame = Image.open(TEX / "item/frame.png").convert("RGBA")
    return {"queen": ghost_icon(queen), "drone": ghost_icon(drone), "comb": ghost_icon(comb), "frame": ghost_icon(frame)}


def bar_frame(d, x, y, w, h):
    """Утопленная рамка полосы: внутренняя область (x, y, w, h)."""
    d.rectangle([x - 1, y - 1, x + w, y + h], fill=BAR_BG)
    d.line([(x - 1, y - 1), (x + w - 1, y - 1)], fill=SLOT_D)
    d.line([(x - 1, y - 1), (x - 1, y + h - 1)], fill=SLOT_D)
    d.line([(x, y + h), (x + w, y + h)], fill=SLOT_L)
    d.line([(x + w, y), (x + w, y + h)], fill=SLOT_L)


def status_dots(d, x, y):
    """Три точки статуса 7x7: зелёная, красная, серая."""
    for i, (light, base, dark) in enumerate([(0xA6F28A, 0x3DBA2E, 0x1F6E17), (0xFF9A8A, 0xD8352A, 0x7A1510),
                                             (0xE0E0E0, 0x9A9A9A, 0x5A5A5A)]):
        ox = x + i * 8
        d.ellipse([ox, y, ox + 6, y + 6], fill=rgb(dark))
        d.ellipse([ox + 1, y + 1, ox + 5, y + 5], fill=rgb(base))
        d.point((ox + 2, y + 2), fill=rgb(light))
        d.point((ox + 3, y + 2), fill=rgb(light))


def honey_arrow(d, ox, oy, fill, edge=None):
    """Стрелка 24x16, как у печки."""
    d.rectangle([ox, oy + 5, ox + 15, oy + 10], fill=fill)
    for i in range(8):
        d.line([(ox + 15 + i, oy + 1 + i), (ox + 15 + i, oy + 14 - i)], fill=fill)
    if edge:
        d.line([(ox, oy + 5), (ox + 15, oy + 5)], fill=edge)
        d.line([(ox, oy + 5), (ox, oy + 10)], fill=edge)
        d.line([(ox + 15, oy + 1), (ox + 15, oy + 5)], fill=edge)
        d.line([(ox + 15, oy + 1), (ox + 22, oy + 8)], fill=edge)


ROTOR_C = 8.0  # центр спрайта 16x16 - между пикселями 7 и 8, поэтому рисунок симметричен
ROTOR_SS = 8  # подвыборка на пиксель


def _coverage(inside, x, y):
    hits = sum(inside(x + (sx + 0.5) / ROTOR_SS, y + (sy + 0.5) / ROTOR_SS)
               for sy in range(ROTOR_SS) for sx in range(ROTOR_SS))
    return hits / (ROTOR_SS * ROTOR_SS)


def centrifuge_drum():
    """Чаша центрифуги 16x16: тёмная кайма, деревянный обод, утопленное дно с тенью сверху-слева."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    c = ROTOR_C
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy)
            if r > 7.95:
                continue
            if r > 7.1:
                col = BORDER
            elif r > 6.4:
                col = WOOD_L if dx + dy > 2.0 else WOOD_D if dx + dy < -2.0 else WOOD
            else:
                col = rgb(0x553416) if math.hypot(dx + 0.9, dy + 0.9) > 5.9 else rgb(0x7A5028)
            img.putpixel((x, y), col)
    return img


def centrifuge_rotor():
    """Крестовина с четырьмя стаканчиками, симметричная по обеим осям; вращается вокруг центра спрайта."""
    c = ROTOR_C
    cups = [(c, c - 4.6), (c + 4.6, c), (c, c + 4.6), (c - 4.6, c)]

    def arm(px, py):
        dx, dy = px - c, py - c
        return (abs(dx) <= 0.8 and abs(dy) <= 4.8) or (abs(dy) <= 0.8 and abs(dx) <= 4.8)

    def cup(px, py):
        return any(math.hypot(px - cx, py - cy) <= 1.75 for cx, cy in cups)

    def hub(px, py):
        return math.hypot(px - c, py - c) <= 2.1

    fill = {}
    for y in range(16):
        for x in range(16):
            if _coverage(cup, x, y) >= 0.45:
                fill[(x, y)] = "cup"
            elif _coverage(hub, x, y) >= 0.45:
                fill[(x, y)] = "hub"
            elif _coverage(arm, x, y) >= 0.5:
                fill[(x, y)] = "arm"
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    near = ((1, 0), (-1, 0), (0, 1), (0, -1))
    for (x, y), kind in fill.items():
        edge = any(fill.get((x + dx, y + dy)) != kind for dx, dy in near)
        outside = any((x + dx, y + dy) not in fill for dx, dy in near)
        r = math.hypot(x + 0.5 - c, y + 0.5 - c)
        if kind == "cup":
            col = rgb(0x7C6E5C) if outside else rgb(0xF2EEE4)
        elif kind == "hub":
            col = HONEY_L if r < 1.0 else HONEY_D if edge and outside else HONEY
        else:
            col = HONEY_D
        img.putpixel((x, y), col)
    return img


def gui_textures():
    ghosts = ghost_icons()
    # ---------------- пасека, 176x186; координаты совпадают с ApiaryMenu и ApiaryScreen
    img, d = wood_panel(176, 186)
    hex_pattern(d, 5, 15, 170, 76)
    separator(d, 6, 169, 90)
    slot(d, 30, 26, ghosts["queen"])
    slot(d, 30, 52, ghosts["drone"])
    for i in range(3):
        slot(d, 62, 19 + i * 20, ghosts["frame"])
    for x, y in ((116, 38), (116, 16), (116, 60), (95, 27), (137, 27), (95, 49), (137, 49)):
        hex_slot(d, x, y)
    player_slots(d, 8, 104)
    # полоса жизни королевы и полоса прогресса
    bar_frame(d, 16, 26, 4, 42)
    bar_frame(d, 29, 46, 18, 3)
    # указатель от рамок к сотам
    for i in range(4):
        d.line([(83 + i, 44 + i), (83 + i, 52 - i)], fill=WOOD)
    # заполнители справа от панели: жизнь (176,0) 4x42, прогресс (176,48) 18x3, точки статуса (176,56)
    for yy in range(42):
        t = yy / 41
        d.line([(176, yy), (179, yy)], fill=(int(80 + 150 * t), int(205 - 150 * t), 50, 255))
    d.line([(176, 0), (176, 41)], fill=(255, 255, 255, 90))
    d.line([(176, 48), (193, 48)], fill=HONEY_L)
    d.line([(176, 49), (193, 49)], fill=HONEY)
    d.line([(176, 50), (193, 50)], fill=HONEY_D)
    status_dots(d, 176, 56)
    save(img, "gui/apiary.png")

    # ---------------- центрифуга, 176x176; координаты совпадают с CentrifugeMenu и CentrifugeScreen
    img, d = wood_panel(176, 176)
    hex_pattern(d, 5, 15, 170, 76)
    separator(d, 6, 169, 80)
    slot(d, 35, 35, ghosts["comb"])
    for row in range(3):
        for col in range(3):
            slot(d, 98 + col * 18, 17 + row * 18)
    player_slots(d, 8, 94)
    honey_arrow(d, 62, 35, PARCH_D)
    # барабан над стрелкой: неподвижная чаша на панели, а крестовину (176, 20) игра вращает сама
    img.alpha_composite(centrifuge_drum(), (66, 15))
    img.alpha_composite(centrifuge_rotor(), (176, 20))
    honey_arrow(d, 176, 0, HONEY)
    for i in range(24):
        d.point((176 + i, 6), fill=HONEY_L)
    save(img, "gui/centrifuge.png")

    # ---------------- JEI: фоны категорий на прозрачном фоне
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    vanilla = {"in": rgb(0x8B8B8B), "d": rgb(0x373737), "l": rgb(0xFFFFFF)}

    def vslot(x, y):
        d.rectangle([x, y, x + 17, y + 17], fill=vanilla["in"])
        d.line([(x, y), (x + 16, y)], fill=vanilla["d"])
        d.line([(x, y), (x, y + 16)], fill=vanilla["d"])
        d.line([(x + 1, y + 17), (x + 17, y + 17)], fill=vanilla["l"])
        d.line([(x + 17, y + 1), (x + 17, y + 17)], fill=vanilla["l"])

    # центрифуга: 116x56, вход (0,19), стрелка (24,20), выходы 3x3 с (62,1)
    vslot(0, 19)
    honey_arrow(d, 25, 20, rgb(0xB0B0B0))
    for r in range(3):
        for c in range(3):
            vslot(62 + c * 18, 1 + r * 18)
    honey_arrow(d, 0, 60, HONEY)                      # заполнитель стрелки (0,60) 24x16
    # продукты пчелы: 140x40, пчела (0,1), стрелка (24,2), продукты с (56,1) через 21
    vslot(0, 80 + 1)
    honey_arrow(d, 24, 80 + 2, rgb(0xB0B0B0))
    for i in range(4):
        vslot(56 + i * 21, 80 + 1)
    # мутации: 140x44, родители (0,2) и (34,2), стрелка (60,3), потомок (96,2), условие (62,24)
    vslot(0, 130 + 2)
    d.rectangle([22, 130 + 9, 29, 130 + 10], fill=rgb(0x5A5A5A))
    d.rectangle([25, 130 + 6, 26, 130 + 13], fill=rgb(0x5A5A5A))
    vslot(34, 130 + 2)
    honey_arrow(d, 60, 130 + 3, rgb(0xB0B0B0))
    vslot(96, 130 + 2)
    vslot(62, 130 + 24)
    save(img, "gui/jei.png")


def bestiary_textures():
    outline, cover, shadow = rgb(0x25180F), rgb(0x705244), rgb(0x402B22)
    leather_light, copper, copper_light = rgb(0xA67856), rgb(0xB8693C), rgb(0xE7A36C)
    rule, pale = rgb(0xD2A979), rgb(0xE7CCAA)
    left_page, right_page = rgb(0xF0DFC5), rgb(0xF6EACF)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(0, 12), (5, 5), (120, 5), (127, 9), (135, 5), (250, 5), (255, 12),
               (255, 160), (251, 165), (137, 165), (127, 167), (119, 165), (4, 165), (0, 160)],
              fill=outline)
    d.polygon([(2, 12), (6, 7), (120, 7), (127, 11), (135, 7), (249, 7), (253, 12),
               (253, 159), (250, 163), (137, 163), (127, 165), (119, 163), (5, 163), (2, 159)],
              fill=cover)
    d.line([(2, 16), (2, 157), (6, 162), (118, 162), (124, 164)], fill=leather_light)
    d.line([(132, 164), (138, 162), (250, 162), (253, 157), (253, 16)], fill=shadow)
    for y in range(159, 164):
        d.line([(7, y), (118, y), (127, y + 2), (137, y), (248, y)], fill=rule if y % 2 else pale)
    d.polygon([(6, 9), (10, 4), (114, 4), (122, 6), (127, 10), (127, 161),
               (119, 158), (9, 158), (6, 155)], fill=left_page, outline=rule)
    d.polygon([(127, 10), (133, 6), (142, 4), (245, 4), (249, 9), (249, 155),
               (246, 158), (136, 158), (127, 161)], fill=right_page, outline=rule)
    d.line([(8, 10), (11, 6), (112, 6), (119, 7)], fill=right_page)
    d.line([(124, 10), (124, 157), (127, 159)], fill=pale)
    d.line([(129, 10), (129, 158), (128, 159)], fill=pale)
    d.line([(127, 11), (127, 158)], fill=rule)
    for start, end, y in ((15, 115, 36), (15, 115, 140), (142, 240, 118)):
        d.line([(start, y), (end, y)], fill=rule)

    # Медные уголки, скобы на корешке и чертёжная сетка под портретом.
    for x, y, dx, dy in ((3, 10, 1, 1), (252, 10, -1, 1), (3, 160, 1, -1), (252, 160, -1, -1)):
        d.polygon([(x, y), (x + 8 * dx, y), (x, y + 8 * dy)], fill=copper)
        d.line([(x, y + dy), (x, y + 7 * dy)], fill=copper_light)
        d.point((x + 2 * dx, y + 2 * dy), fill=shadow)
    for y in (29, 73, 117):
        d.rectangle((124, y, 129, y + 4), fill=copper)
        d.line([(124, y), (129, y)], fill=copper_light)
        d.point((126, y + 2), fill=shadow)
    for y in range(32, 68, 4):
        for x in range(171, 212, 4):
            d.point((x, y), fill=pale)
    for x, y in ((15, 36), (115, 36), (142, 118), (240, 118)):
        d.line([(x, y - 2), (x, y + 2)], fill=copper)

    # Печать из сот для пустого разворота в отдельной области атласа.
    for x, y in ((16, 175), (8, 187), (24, 187)):
        d.polygon([(x - 2, y - 5), (x + 2, y - 5), (x + 5, y), (x + 2, y + 5),
                   (x - 2, y + 5), (x - 5, y)], fill=pale, outline=rule)
        d.line([(x - 2, y - 3), (x + 2, y - 3)], fill=right_page)
    save(img, "gui/bee_bestiary.png")

    palette = {"A": rgb(0x271810), "B": leather_light, "C": cover, "D": copper,
               "E": copper_light, "F": shadow, "G": right_page, "H": rule}
    save(sprite([
        "................",
        "...AAAAAAAAAA...",
        "..AABBBBBBBBCAA.",
        ".ADECCCCCCCEFCA.",
        ".ADBCCDDDDDCFCA.",
        ".ADBCCDEEEDCFCA.",
        ".ADBCCDFFFDCFEEE",
        ".ADBCCDBBBDCFDDF",
        ".ADBCCDDDFDCFFFF",
        ".ADBCCDDDDDCFCA.",
        ".ADECCCCCCCEFCA.",
        ".ACCBFFFFFFFCAA.",
        ".AAGGGGGGGGGGAA.",
        "..AAHHHHHHHHHAA.",
        "...AAAAAAAAAA...",
        "................",
    ], palette), "item/bee_bestiary.png")


HIVES = {
    "forest_hive": 0xB58B3C, "meadows_hive": 0xE2C24A, "modest_hive": 0xD9C48A, "tropical_hive": 0x6E9A3A,
    "wintry_hive": 0xDDEBF0, "marshy_hive": 0x5E6B34, "rocky_hive": 0x8C8C8C,
}


def main():
    bee_textures()
    comb_textures()
    product_textures()
    tool_textures()
    for seed, (name, color) in enumerate(HIVES.items()):
        hive_textures(name, color, seed + 1)
    apiary_textures()
    centrifuge_textures()
    particle_textures()
    gui_textures()
    bestiary_textures()
    print("textures written to", TEX)


if __name__ == "__main__":
    main()
