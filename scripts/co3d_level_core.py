# -*- coding: utf-8 -*-
"""
CovertOps3D-RE — ядро редактора уровней (чистый stdlib, без зависимостей).

Реализует, байт-в-байт сверено с декомпилом (src/LevelLoader.java и др.):
  * парсинг/запись карт res/gamedata/levels/level_* (little-endian, версия 0x99);
  * парсинг атласов текстур tx1..tx4 (magic 0x9954, заголовок записи 9 байт)
    и спрайтов sp1..sp4 (magic 0x9953, заголовок 13 байт) — big-endian;
  * 16-уровневые затенённые палитры (Texture.createColorPalettes);
  * композитные текстуры 39/40 (база 35), 47/48 (база 46), 50 (база 49);
  * таблицу remapLegacyTextureId (6->5, 18..23->17, 42/43->41) для превью;
  * ребилд производных секций при сохранении: сегменты, BSP-узлы/листья
    (Doom-style построитель по сегментам, корень — последний узел),
    PVS-матрицу (консервативно «всё видно», семантика бита: 1 = НЕ виден);
  * софтверный 3D-рендер предпросмотра (портальный рейкаст по секторам,
    текстуры стен/полов/потолков, небо id 51, билборды объектов по sp-атласам).

Этот модуль не импортирует tkinter — полностью тестируется headless.
GUI живёт в level_editor.py.
"""

import math
import struct
import sys

# ----------------------------------------------------------------------------
# Бинарные хелперы
# ----------------------------------------------------------------------------

def _u32le(b, o): return struct.unpack_from('<I', b, o)[0]
def _i16(b, o):  return struct.unpack_from('<h', b, o)[0]
def _u16be(b, o): return struct.unpack_from('>H', b, o)[0]
def _i16be(b, o): return struct.unpack_from('>h', b, o)[0]
def _u32be(b, o): return struct.unpack_from('>I', b, o)[0]

def _i8(byte):  # беззнаковый байт -> знаковый
    return byte - 256 if byte >= 128 else byte

def _pack_i16(v):
    v = int(v)
    if v < -32768 or v > 32767:
        raise ValueError('значение %d не влезает в int16' % v)
    return struct.pack('<h', v)

def unpack_bits_msb(data, start_bit, count, bpp):
    """Извлекает count значений по bpp бит из MSB-first битового потока
    (ровно как decompressSprite/... в LevelLoader.java)."""
    out = bytearray(count)
    byte_i = start_bit >> 3
    bit_i = start_bit & 7
    mask = (1 << bpp) - 1
    for i in range(count):
        cur = data[byte_i]
        sh = 8 - (bpp + bit_i)
        if sh >= 0:
            val = (cur >> sh) & mask
        else:
            need = -sh
            val = ((cur << need) | (data[byte_i + 1] >> (8 - need))) & mask
        out[i] = val
        bit_i += bpp
        if bit_i > 7:
            bit_i -= 8
            byte_i += 1
    return out

# ----------------------------------------------------------------------------
# Модель уровня
# ----------------------------------------------------------------------------

class Level(object):
    """Все секции карты, индексы 1:1 как в файле."""

    def __init__(self):
        self.version = 0x99
        self.vertices = []   # (x, z) int16
        self.walls = []      # dict(sv, ev, flags, type, special, front, back)
        self.objects = []    # dict(x, z, angle, type, param)
        self.surfaces = []   # dict(ox, oy, upper, lower, main, sector)
        self.sectors = []    # dict(floor, ceil, ceil_tex, floor_tex, light_packed, tag, type)
        self.nodes = []      # (sx, sz, dx, dz, front, back)  0x8000=лист
        self.leaves = []     # (seg_count, seg_offset)
        self.segments = []   # (sv, ev, def_idx, ff_byte, tex_off) ff_byte: 0=front
        self.pvs = []        # n x n список bytearray (1 = НЕ виден)

    # ----- производные представления -----

    def sector_sides(self):
        """Для каждого сектора список его "сторон стен":
        dict(x1,z1,x2,z2, wall, surf(def index), surf(dict), other_sector|None,
             other_surf|None, one_sided)."""
        sides = [[] for _ in self.sectors]
        for wi, w in enumerate(self.walls):
            for which in ('front', 'back'):
                si = w[which]
                if si < 0 or si >= len(self.surfaces):
                    continue
                surf = self.surfaces[si]
                sec = surf['sector']
                if sec < 0 or sec >= len(self.sectors):
                    continue
                if which == 'front':
                    v1, v2 = self.vertices[w['sv']], self.vertices[w['ev']]
                    oi = w['back']
                else:
                    v1, v2 = self.vertices[w['ev']], self.vertices[w['sv']]
                    oi = w['front']
                other_sec = None
                other_surf = None
                if 0 <= oi < len(self.surfaces):
                    other_surf = self.surfaces[oi]
                    os_ = other_surf['sector']
                    if 0 <= os_ < len(self.sectors):
                        other_sec = os_
                sides[sec].append(dict(
                    x1=v1[0], z1=v1[1], x2=v2[0], z2=v2[1],
                    wall=wi, surf_idx=si, surf=surf,
                    other_sector=other_sec, other_surf=other_surf,
                    one_sided=(other_sec is None)))
        return sides

    def sectors_edges(self):
        """sector -> список рёбер (x1,z1,x2,z2) (без ориентации)."""
        edges = [[] for _ in self.sectors]
        for w in self.walls:
            x1, z1 = self.vertices[w['sv']]
            x2, z2 = self.vertices[w['ev']]
            for si in (w['front'], w['back']):
                if 0 <= si < len(self.surfaces):
                    s = self.surfaces[si]['sector']
                    if 0 <= s < len(self.sectors):
                        edges[s].append((x1, z1, x2, z2))
        return edges

    def stats(self):
        return dict(vertices=len(self.vertices), walls=len(self.walls),
                    objects=len(self.objects), surfaces=len(self.surfaces),
                    sectors=len(self.sectors), nodes=len(self.nodes),
                    leaves=len(self.leaves), segments=len(self.segments))

# ----------------------------------------------------------------------------
# Парсинг / запись карты
# ----------------------------------------------------------------------------

def parse_level(data):
    if isinstance(data, (bytes, bytearray)):
        buf = bytes(data)
    else:
        buf = open(data, 'rb').read()
    lv = Level()
    o = 0
    lv.version = buf[o]; o += 1

    def section(fmt, size):
        nonlocal o
        total = _u32le(buf, o); o += 4
        if total % size:
            raise ValueError('размер секции %d не кратен %d @%d' % (total, size, o))
        n = total // size
        recs = [struct.unpack_from(fmt, buf, o + i * size) for i in range(n)]
        o += total
        return recs

    lv.vertices = [tuple(v) for v in section('<hh', 4)]
    for (sv, ev, fl, tp, sp, fs, bs) in section('<hhBBBhh', 11):
        lv.walls.append(dict(sv=sv, ev=ev, flags=fl, type=tp, special=sp,
                             front=fs, back=bs))
    for (x, z, a, tp, prm) in section('<hhhhh', 10):
        lv.objects.append(dict(x=x, z=z, angle=a, type=tp, param=prm))
    for (ox, oy, up, lo, mn, sec) in section('<hhBBBB', 8):
        lv.surfaces.append(dict(ox=ox, oy=oy, upper=up, lower=lo, main=mn,
                                sector=sec))
    for (fh, ch, ct, ft, li, tg, ty) in section('<hhBBHhh', 12):
        lv.sectors.append(dict(floor=fh, ceil=ch, ceil_tex=ct, floor_tex=ft,
                               light_packed=li, tag=tg, type=ty))
    lv.nodes = [tuple(v) for v in section('<hhhhHH', 12)]
    lv.leaves = [tuple(v) for v in section('<hh', 4)]
    # ВАЖНО: порядок полей сегмента в файле —
    # sv, ev, defIdx, ffByte, texOff (loader передаёт 3-й short как defIndex
    # конструктора WallSegment, а последний — как texOffset; имена локальных
    # переменных в декомпиле перепутаны).
    lv.segments = [tuple(v) for v in section('<hhhBh', 9)]

    pvs_size = _u32le(buf, o); o += 4
    n = len(lv.sectors)
    pvs_bytes = buf[o:o + pvs_size]; o += pvs_size
    lv.pvs = [bytearray(n) for _ in range(n)]
    # линейный битовый поток: бит i (= col*n + row), LSB-first, row быстрее
    for idx in range(n * n):
        b = pvs_bytes[idx >> 3] if (idx >> 3) < len(pvs_bytes) else 0
        col, row = divmod(idx, n)
        lv.pvs[row][col] = (b >> (idx & 7)) & 1
    if o != len(buf):
        raise ValueError('лишние байты: %d != %d' % (o, len(buf)))
    return lv


def dump_level(lv):
    out = bytearray()
    out.append(lv.version & 0xFF)

    vtx = bytearray()
    for (x, z) in lv.vertices:
        vtx += _pack_i16(x) + _pack_i16(z)
    out += struct.pack('<I', len(vtx)) + vtx

    wrk = bytearray()
    for w in lv.walls:
        wrk += struct.pack('<hhBBBhh', w['sv'], w['ev'], w['flags'] & 0xFF,
                           w['type'] & 0xFF, w['special'] & 0xFF,
                           w['front'], w['back'])
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for ob in lv.objects:
        wrk += struct.pack('<hhhhh', _clamp16(ob['x']), _clamp16(ob['z']),
                           _clamp16(ob['angle']), _clamp16(ob['type']),
                           _clamp16(ob['param']))
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for s in lv.surfaces:
        wrk += struct.pack('<hhBBBB', s['ox'], s['oy'], s['upper'] & 0xFF,
                           s['lower'] & 0xFF, s['main'] & 0xFF,
                           s['sector'] & 0xFF)
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for s in lv.sectors:
        wrk += struct.pack('<hhBBHhh', s['floor'], s['ceil'],
                           s['ceil_tex'] & 0xFF, s['floor_tex'] & 0xFF,
                           s['light_packed'] & 0xFFFF, s['tag'], s['type'])
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for (sx, sz, dx, dz, fr, bk) in lv.nodes:
        wrk += struct.pack('<hhhhHH', sx, sz, dx, dz, fr & 0xFFFF, bk & 0xFFFF)
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for (cnt, off) in lv.leaves:
        wrk += struct.pack('<hh', cnt, off)
    out += struct.pack('<I', len(wrk)) + wrk

    wrk = bytearray()
    for (sv, ev, di, ff, tx) in lv.segments:
        wrk += struct.pack('<hhhBh', _clamp16(sv), _clamp16(ev), _clamp16(di),
                           ff & 0xFF, _clamp16(tx))
    out += struct.pack('<I', len(wrk)) + wrk

    n = len(lv.sectors)
    need = (n * n + 7) // 8
    pb = bytearray(need)
    # бит i (= col*n + row), LSB-first внутри байта, row бежит быстрее
    for idx in range(n * n):
        col, row = divmod(idx, n)
        if lv.pvs[row][col]:
            pb[idx >> 3] |= (1 << (idx & 7))
    out += struct.pack('<I', len(pb)) + bytes(pb)
    return bytes(out)


def _clamp16(v):
    v = max(-32768, min(32767, int(v)))
    return v

# ----------------------------------------------------------------------------
# Геометрия
# ----------------------------------------------------------------------------

def point_in_sector(level, edges, sec, x, z):
    """even-odd тест по рёбрам сектора."""
    inside = False
    for (x1, z1, x2, z2) in edges[sec]:
        if (z1 > z) != (z2 > z):
            xi = x1 + (z - z1) * (x2 - x1) / float(z2 - z1)
            if x < xi:
                inside = not inside
    return inside


def find_sector_at(level, x, z):
    edges = level.sectors_edges()
    for s in range(len(level.sectors)):
        if point_in_sector(level, edges, s, x, z):
            return s
    return None


def sector_centroids(level, edges=None):
    if edges is None:
        edges = level.sectors_edges()
    cents = []
    for s, es in enumerate(edges):
        if not es:
            cents.append((0.0, 0.0))
            continue
        ax = sum((e[0] + e[2]) for e in es) / (2.0 * len(es))
        az = sum((e[1] + e[3]) for e in es) / (2.0 * len(es))
        cents.append((ax, az))
    return cents


def sector_interior_samples(level, max_per_sector=24):
    """Точки гарантированно внутри каждого сектора (середины рёбер,
    подвинутые к центроиду). Нужны для валидации BSP."""
    edges = level.sectors_edges()
    cents = sector_centroids(level, edges)
    samples = [[] for _ in level.sectors]
    for s, es in enumerate(edges):
        if not es:
            continue
        cx, cz = cents[s]
        step = max(1, len(es) // max_per_sector)
        for i in range(0, len(es), step):
            x1, z1, x2, z2 = es[i]
            mx, mz = (x1 + x2) / 2.0, (z1 + z2) / 2.0
            # предпочитаем точки подальше от границ: диагональные стены
            # и клеточная квантованность дают тонкие «спорные» полосы,
            # где даже стоковое BSP расходится с even-odd
            for k in (3, 6, 12, 2):
                px = mx + (cx - mx) / k
                pz = mz + (cz - mz) / k
                if not point_in_sector(level, edges, s, px, pz):
                    continue
                if _min_edge_dist(es, px, pz) >= 2.0:
                    samples[s].append((px, pz))
                    break
        if not samples[s]:
            # fallback: центроид, но только если он внутри и не на границе
            if (point_in_sector(level, edges, s, cx, cz)
                    and _min_edge_dist(es, cx, cz) >= 2.0):
                samples[s].append((cx, cz))
    return samples


def _min_edge_dist(es, px, pz):
    best = 1e30
    for (x1, z1, x2, z2) in es:
        dx, dz = x2 - x1, z2 - z1
        ll = dx * dx + dz * dz
        if ll == 0:
            continue
        t = ((px - x1) * dx + (pz - z1) * dz) / float(ll)
        t = max(0.0, min(1.0, t))
        qx = x1 + t * dx - px
        qz = z1 + t * dz - pz
        d = qx * qx + qz * qz
        if d < best:
            best = d
    return math.sqrt(best) if best < 1e30 else 1e30

# ----- find_sector через BSP-дерево (точная копия BSPNode.isPointInFront) ---

MAXI = 2147483647
MINI = -2147483648


def _fixed_div(dividend, divisor):
    """MathUtils.fixedPointDivide (оба аргумента уже 16.16)."""
    ab = -dividend if dividend < 0 else dividend
    av = -divisor if divisor < 0 else divisor
    if (ab >> 14) >= av:
        return MINI if (dividend ^ divisor) < 0 else MAXI
    q = ((ab << 32) // av)
    if (dividend ^ divisor) < 0:
        q = -q
    return q >> 16


def _java_i32(v):
    v &= 0xFFFFFFFF
    return v - 0x100000000 if v >= 0x80000000 else v


def _in_front(sx, sz, nx, dy, x, z):
    """sx,sz,nx,dy,x,z — фикс. 16.16 int32. Возвращает True если «front»."""
    slope = _fixed_div(dy, nx)
    if slope == MAXI:
        return _java_i32(sx - x) >= 0
    if slope == MINI:
        return _java_i32(x - sx) >= 0
    dx = _java_i32(x - sx)
    pred = _java_i32(sz + _java_i32((slope * dx) >> 16))
    front = _java_i32(z - pred) >= 0
    if nx < 0:
        front = not front
    return front


def find_sector_tree(level, nodes, leaves, sectors_of_leaf, x, z):
    """Спуск по дереву как в игре. x,z — мировые int (не фикс.)."""
    if not nodes:
        return None
    xi = _java_i32(x << 16)
    zi = _java_i32(z << 16)
    idx = len(nodes) - 1  # корень — последний
    guard = 0
    while True:
        guard += 1
        if guard > 10000:
            return None
        sx, sz, dx, dz, fr, bk = nodes[idx]
        if _in_front(_java_i32(sx << 16), _java_i32(sz << 16),
                     _java_i32(dx << 16), _java_i32(dz << 16), xi, zi):
            ref = bk
        else:
            ref = fr
        if ref & 0x8000:
            leaf = ref & 0x7FFF
            if leaf >= len(sectors_of_leaf):
                return None
            return sectors_of_leaf[leaf]
        idx = ref
        if idx >= len(nodes):
            return None


def leaf_sectors(level, nodes=None, leaves=None, segments=None):
    """sector каждого листа = сектор первого его сегмента (как в игре:
    Sector.getSectorData() -> walls[0].getWallSector())."""
    if leaves is None:
        leaves, segments = level.leaves, level.segments
    out = []
    for (cnt, off) in leaves:
        if cnt <= 0 or off < 0 or off >= len(segments):
            out.append(-1)
            continue
        sv, ev, di, ff, tx = segments[off]
        if di < 0 or di >= len(level.walls):
            out.append(-1)
            continue
        w = level.walls[di]
        si = w['front'] if ff == 0 else w['back']
        if si < 0 or si >= len(level.surfaces):
            out.append(-1)
            continue
        out.append(level.surfaces[si]['sector'])
    return out

# ----------------------------------------------------------------------------
# Ребилд производных секций (сегменты, листья, BSP, PVS)
# ----------------------------------------------------------------------------

class BuildReport(object):
    def __init__(self):
        self.mixed_leaves = 0
        self.forced_leaves = 0
        self.splits = 0
        self.fail_samples = []   # (x, z, expected, got)
        self.attempt = 0
        self.object_mismatches = 0


def _work_segs(level):
    """Исходные сегменты из wall-описаний: front (sv->ev, ff=0),
    back (ev->sv, ff=1) для двусторонних."""
    out = []
    for di, w in enumerate(level.walls):
        x1, z1 = level.vertices[w['sv']]
        x2, z2 = level.vertices[w['ev']]
        fs = w['front']
        if 0 <= fs < len(level.surfaces):
            sec_f = level.surfaces[fs]['sector']
            out.append([x1, z1, x2, z2, di, 0, 0, w['sv'], w['ev'], sec_f])
        bs = w['back']
        if 0 <= bs < len(level.surfaces):
            sec_b = level.surfaces[bs]['sector']
            out.append([x2, z2, x1, z1, di, 1, 0, w['ev'], w['sv'], sec_b])
    return out


def _add_vertex(level, x, z):
    x, z = int(x), int(z)
    if not (-32768 <= x <= 32767 and -32768 <= z <= 32767):
        raise ValueError('vertex out of range')
    vs = level.vertices
    for i in range(len(vs) - 1, -1, -1):
        if vs[i][0] == x and vs[i][1] == z:
            return i
    vs.append((x, z))
    return len(vs) - 1


def rebuild_derived(level, validate=True, max_attempts=6, log=None):
    """Перестраивает сегменты/листья/BSP/PVS из (vertices, walls, surfaces).
    Возвращает BuildReport. При validate=True гоняет выборочную проверку
    find_sector_tree против точечной классификации и перебирает до
    max_attempts вариантов дерева (детерминированный сдвиг кандидатов)."""
    rep = BuildReport()
    base_work = _work_segs(level)
    base_vcount = len(level.vertices)
    cents = sector_centroids(level)

    # выборочные контрольные точки (только гарантированные внутренние;
    # объекты НЕ используем как эталон: они могут сидеть ровно на границе
    # секторов, где even-odd и BSP расходятся — сток ведёт себя так же)
    samples = []
    if validate:
        si = sector_interior_samples(level)
        for s, pts in enumerate(si):
            for p in pts:
                samples.append((p[0], p[1], s))

    best = None  # (fails, nodes, leaves, segs, extra_verts)
    for attempt in range(max_attempts):
        del level.vertices[base_vcount:]
        nodes = []
        leaves = []
        out_segs = []
        stats = {'mixed': 0, 'forced': 0, 'splits': 0}
        try:
            _bsp_build(level, [list(w) for w in base_work], cents, nodes,
                       leaves, out_segs, stats, attempt, 0)
            if not nodes and out_segs:
                # уровень из одного сектора: игра берёт корень как
                # nodes[len-1] — без единого узла она упадёт. Делаем
                # тривиальный узел: вертикальный разрез bbox, оба ребёнка —
                # листья того же сектора.
                work = [list(w) for w in base_work]
                xs = [w[0] for w in work] + [w[2] for w in work]
                pivot = (min(xs) + max(xs)) // 2
                front, back = [], []
                for w in work:
                    mx = (w[0] + w[2]) * 0.5
                    (front if mx <= pivot else back).append(w)
                if not front or not back:
                    front = work[:len(work) // 2] or work
                    back = work[len(work) // 2:]
                leaves.clear()
                out_segs.clear()
                fref = _emit_leaf(level, front, leaves, out_segs)
                bref = _emit_leaf(level, back, leaves, out_segs)
                # (front, back) в файле = (cross>0, cross<=0) — см. _bsp_build
                nodes.append((pivot, 0, 0, 16, bref, fref))
        except (_BuildGaveUp, ValueError):
            continue
        leaf_secs = []
        for (cnt, off) in leaves:
            if cnt <= 0:
                leaf_secs.append(-1)
                continue
            sv, ev, di, ff, tx = out_segs[off]
            w = level.walls[di]
            si = w['front'] if ff == 0 else w['back']
            leaf_secs.append(level.surfaces[si]['sector'] if si >= 0 else -1)
        fails = 0
        fail_pts = []
        for (px, pz, exp) in samples:
            got = find_sector_tree(level, nodes, leaves, leaf_secs,
                                   int(round(px)), int(round(pz)))
            if got != exp:
                fails += 1
                if len(fail_pts) < 12:
                    fail_pts.append((px, pz, exp, got))
        # слегка штрафуем смешанные листья
        score = fails * 100 + stats['mixed']
        if best is None or score < best[0]:
            best = (score, fails, nodes, leaves, out_segs,
                    level.vertices[base_vcount:], fail_pts, stats, leaf_secs)
        if fails == 0 and stats['mixed'] == 0:
            break
    if best is None:
        raise RuntimeError('BSP builder: не удалось построить дерево')
    _, fails, nodes, leaves, out_segs, extra_v, fail_pts, stats, leaf_secs = best
    del level.vertices[base_vcount:]
    level.vertices.extend(extra_v)
    level.nodes = nodes
    level.leaves = leaves
    level.segments = out_segs
    n = len(level.sectors)
    level.pvs = [bytearray(n) for _ in range(n)]   # 0 = "всё видно" (консервативно)
    rep.mixed_leaves = stats['mixed']
    rep.forced_leaves = stats['forced']
    rep.splits = stats['splits']
    rep.fail_samples = fail_pts
    rep.attempt = attempt
    return rep


class _BuildGaveUp(Exception):
    pass

BSP_TRACE = False  # отладка построителя


def _bsp_build(level, work, cents, nodes, leaves, out_segs, stats,
               attempt, depth):
    """Рекурсивный Doom-style построитель. work: изменяемые списки
    [x1,z1,x2,z2,def,ff,tex,sv,ev,sector]. Возвращает ссылку: node idx или
    leafidx|0x8000. Без откатов — дерево всегда достраивается."""
    if depth > 200:
        raise _BuildGaveUp()
    sects = set(w[9] for w in work)
    if len(sects) == 1:
        return _emit_leaf(level, work, leaves, out_segs)
    if not work:
        raise _BuildGaveUp()

    n = len(work)
    # кандидаты в сплиттеры: до 24 равномерно + сдвиг попытки
    cand_i = list(range(n))
    if n > 24:
        step = n / 24.0
        cand_i = sorted(set(int(i * step) for i in range(24)))
    if attempt:
        rot = attempt * 7 % len(cand_i)
        cand_i = cand_i[rot:] + cand_i[:rot]

    def vote_side(w, sx1, sz1, dx, dz):
        """Сторона для коллинеарного сплиттеру сегмента. Сегмент всегда
        ориентирован «лицом к своему сектору» (front: sv->ev, back: ev->sv),
        сектор лежит СПРАВА от направления: нормаль n=(dir.z, -dir.x).
        Проверено на стоковых данных: 790/790. Близнецы (та же стена)
        гарантированно разъезжаются по разным сторонам."""
        wdx = w[2] - w[0]
        wdz = w[3] - w[1]
        if wdx == 0 and wdz == 0:
            return -1
        px = (w[0] + w[2]) * 0.5 + wdz   # mid + n (масштаб нормали не важен)
        pz = (w[1] + w[3]) * 0.5 - wdx
        cs = (px - sx1) * dz - (pz - sz1) * dx
        return 1 if cs > 0 else -1  # 1=back, -1=front

    best = None  # (score, ci)
    for ci in cand_i:
        sx1, sz1, sx2, sz2 = work[ci][0], work[ci][1], work[ci][2], work[ci][3]
        dx, dz = sx2 - sx1, sz2 - sz1
        if dx == 0 and dz == 0:
            continue
        nf = nb = nsp = 0
        for wi in range(n):
            w = work[wi]
            ca = (w[0] - sx1) * dz - (w[1] - sz1) * dx
            cb = (w[2] - sx1) * dz - (w[3] - sz1) * dx
            if ca == 0 and cb == 0:
                # коллинеарные (включая сам сплиттер — его «лицо» всегда
                # смотрит в cross>0, т.е. в back) — по нормали лицевой грани
                if vote_side(w, sx1, sz1, dx, dz) < 0:
                    nf += 1
                else:
                    nb += 1
            elif ca <= 0 and cb <= 0:
                nf += 1
            elif ca >= 0 and cb >= 0:
                nb += 1
            else:
                nsp += 1
                nf += 1
                nb += 1  # обе половины попадут в разные стороны
        if nf == 0 or nb == 0:
            continue
        score = nsp * 8 + abs(nf - nb)
        if best is None or score < best[0]:
            best = (score, ci)
            if score == 0:
                break
    if best is None:
        # ничего не делит — вынужденный лист (редкий вырожденный случай)
        stats['forced'] += 1
        stats['mixed'] += 1
        if BSP_TRACE:
            print('  forced(None) depth=%d n=%d sects=%s' %
                  (depth, len(work), sorted(sects)))
        return _emit_leaf(level, work, leaves, out_segs)

    ci = best[1]
    sx1, sz1 = work[ci][0], work[ci][1]
    dx, dz = work[ci][2] - sx1, work[ci][3] - sz1
    front, back = [], []
    for wi in range(n):
        w = work[wi]
        ca = (w[0] - sx1) * dz - (w[1] - sz1) * dx
        cb = (w[2] - sx1) * dz - (w[3] - sz1) * dx
        if ca == 0 and cb == 0:
            if vote_side(w, sx1, sz1, dx, dz) < 0:
                front.append(w)
            else:
                back.append(w)
        elif (ca <= 0 and cb <= 0):
            front.append(w)
        elif (ca >= 0 and cb >= 0):
            back.append(w)
        else:
            # пересечение — делим
            t = ca / float(ca - cb)
            ix = w[0] + t * (w[2] - w[0])
            iz = w[1] + t * (w[3] - w[1])
            vix = _add_vertex(level, int(round(ix)), int(round(iz)))
            vx, vz = level.vertices[vix]
            seglen0 = int(round(math.hypot(vx - w[0], vz - w[1])))
            first = [w[0], w[1], vx, vz, w[4], w[5], w[6], w[7], vix, w[9]]
            second = [vx, vz, w[2], w[3], w[4], w[5], w[6] + seglen0,
                      vix, w[8], w[9]]
            stats['splits'] += 1
            if ca < 0:
                front.append(first); back.append(second)
            else:
                back.append(first); front.append(second)
    if not front or not back or (len(front) >= n and len(back) >= n):
        stats['forced'] += 1
        stats['mixed'] += 1
        if BSP_TRACE:
            print('  forced(sides) depth=%d n=%d nf=%d nb=%d sects=%s' %
                  (depth, n, len(front), len(back), sorted(sects)))
        return _emit_leaf(level, work, leaves, out_segs)

    fref = _bsp_build(level, front, cents, nodes, leaves, out_segs, stats,
                      attempt, depth + 1)
    bref = _bsp_build(level, back, cents, nodes, leaves, out_segs, stats,
                      attempt, depth + 1)
    if not (-32768 <= sx1 <= 32767 and -32768 <= sz1 <= 32767
            and -32768 <= dx <= 32767 and -32768 <= dz <= 32767):
        raise _BuildGaveUp()
    # ВНИМАНИЕ: в файле frontChild = поддерево cross>0, backChild = cross<=0
    # (игра: isPointInFront(p)==true -> backChild, а front == cross<=0).
    # Наши списки: front = cross<=0, back = cross>0 -> пишем как (bref, fref).
    nodes.append((sx1, sz1, dx, dz, bref, fref))
    return len(nodes) - 1


def _emit_leaf(level, work, leaves, out_segs):
    sects = set(w[9] for w in work)
    if len(sects) > 1:
        # по-хорошему недостижимо (обрабатывается выше), но страхуемся
        pass
    li = len(leaves)
    leaves.append((len(work), len(out_segs)))
    for w in work:
        sv, ev = w[7], w[8]
        # порядок индексов в файле должен соответствовать направлению
        # стороны: front = (def.sv -> def.ev), back = (def.ev -> def.sv)
        wdef = level.walls[w[4]]
        if w[5] == 0:
            if wdef['sv'] == ev and wdef['ev'] == sv:
                sv, ev = ev, sv
        else:
            if wdef['sv'] == sv and wdef['ev'] == ev:
                sv, ev = ev, sv
        out_segs.append((sv, ev, w[4], w[5], w[6]))
    return li | 0x8000

# ----------------------------------------------------------------------------
# Атласы текстур (tx/sp)
# ----------------------------------------------------------------------------

MAGIC_TX = 0x9954
MAGIC_SP = 0x9953

# remapLegacyTextureId (LevelLoader) для превью
_REMAP_SIMPLE = {}
_REMAP_SIMPLE[6] = 5
for _i in range(18, 24):
    _REMAP_SIMPLE[_i] = 17
_REMAP_SIMPLE[42] = 41
_REMAP_SIMPLE[43] = 41
_COMPOSITE_BASE = {39: 35, 40: 35, 47: 46, 48: 46, 50: 49}

# композитные текстуры из LevelLoader.loadGameAssets:
# id: (база, [blend-rects (srcX,srcY,w,h,destX,destY)], replace (w,h))
_COMPOSITES = {
    39: (35, [(0, 128, 64, 18, 0, 0), (64, 17, 10, 111, 54, 17)], (64, 128)),
    40: (35, [(0, 128, 64, 18, 0, 0), (74, 17, 10, 111, 0, 17)], (64, 128)),
    47: (46, [(64, 0, 14, 128, 50, 0)], (64, 128)),
    48: (46, [(64, 0, 14, 128, 50, 0), (78, 0, 20, 24, 19, 43)], (64, 128)),
    50: (49, [(64, 0, 20, 27, 25, 19)], (64, 128)),
}

# спрайт (нижняя часть билборда) по типу объекта (LevelResourceManager)
OBJ_SPRITE = {
    5: -53, 13: -53, 10: -3, 12: -10, 26: -16, 60: -18, 61: -17, 82: -21,
    2001: -19, 2002: -20, 2003: -22, 2004: -43, 2005: -50, 2006: -72,
    2007: -48, 2008: -54, 2010: -57, 2012: -55, 2013: -49, 2014: -52,
    2015: -58, 2024: -85, 2047: -56,
    3001: -59, 3002: -4, 3003: -23, 3004: -86, 3005: -35, 3006: -73,
}

OBJ_NAMES = {
    1: 'Старт игрока 1', 2: 'Старт игрока 2', 3: 'Старт игрока 3',
    4: 'Старт игрока 4', 5: 'Золотой ключ', 10: 'Рычаг', 12: 'Кнопка',
    13: 'Серебряный ключ', 26: 'Бочка', 60: 'Аптечка большая',
    61: 'Аптечка малая', 82: 'Снайперка (квест)',
    2001: 'Luger', 2002: 'Mauser', 2003: 'Винтовка M40',
    2004: 'Panzerfaust', 2005: 'Динамит', 2006: 'Sonic Gun',
    2007: 'Патроны Luger', 2008: 'Патроны Mauser', 2010: 'Ракеты',
    2012: 'Аптечка +50', 2013: 'Переход уровня', 2014: 'Аптечка +25',
    2015: 'Броня', 2024: 'Sten', 2047: 'Заряды Sonic',
    3001: 'Элитный солдат', 3002: 'Босс', 3003: 'Солдат',
    3004: 'Офицер', 3005: 'Охранник', 3006: 'Спецназ',
}


# Must stay byte-for-byte in sync with Texture.createColorPalettes().
# Level 8 is the authored palette. The rest are approximately -3 .. +2 EV
# around it in linear light; the upper half uses a soft highlight roll-off.
_LIGHT_EXPOSURES = (32, 42, 54, 70, 91, 118, 152, 198,
                    256, 312, 380, 464, 566, 690, 841, 1024)
_DISPLAY_WHITE = 255
_LINEAR_WHITE = _DISPLAY_WHITE * _DISPLAY_WHITE


def _rounded_square_root(value):
    # value never exceeds 255^2, so double sqrt is exact enough to floor it
    # on every supported CPython version (keeps the editor Python 3.6-friendly).
    root = int(math.sqrt(value))
    next_root = root + 1
    if next_root <= _DISPLAY_WHITE and value - root * root >= next_root * next_root - value:
        return next_root
    return root


# Java uses this compact table with interpolation. The low range is handled
# exactly in _encode_linear_channel to retain one-digit channel detail.
_LINEAR_TO_DISPLAY = [_rounded_square_root(segment * _DISPLAY_WHITE)
                      for segment in range(_DISPLAY_WHITE + 1)]


def _encode_linear_channel(linear):
    if linear <= _DISPLAY_WHITE:
        return _rounded_square_root(linear)
    segment, remainder = divmod(linear, _DISPLAY_WHITE)
    low = _LINEAR_TO_DISPLAY[segment]
    if remainder == 0 or segment == _DISPLAY_WHITE:
        return low
    high = _LINEAR_TO_DISPLAY[segment + 1]
    return low + ((high - low) * remainder + (_DISPLAY_WHITE >> 1)) // _DISPLAY_WHITE


def _shade_palette_channel(channel, exposure):
    """Java Texture.shadeChannel(), using gamma-2 as a CLDC-friendly
    approximation of display transfer."""
    linear = channel * channel
    if exposure <= 256:
        shaded = (linear * exposure + 128) >> 8
    else:
        denominator = _LINEAR_WHITE * 256 + linear * (exposure - 256)
        shaded = (linear * exposure * _LINEAR_WHITE + (denominator >> 1)) // denominator
    return _encode_linear_channel(max(0, min(_LINEAR_WHITE, shaded)))


# Same 16 x 256 cache as Texture.getShadedChannels(). Palette construction
# below therefore stays cheap even when an editor reloads several atlases.
_SHADED_CHANNELS = [[_shade_palette_channel(channel, exposure)
                     for channel in range(_DISPLAY_WHITE + 1)]
                    for exposure in _LIGHT_EXPOSURES]


def create_color_palettes(colors):
    """Texture.createColorPalettes: гамма-корректное приближение освещения.

    Нейтральная строка 8 сохраняет авторские цвета точно. Тёмные строки
    масштабируют освещённость, а яркие мягко сжимают highlights, поэтому
    тени не становятся неестественно насыщенными, а светлые пиксели не
    выгорают в белый.
    """
    pals = []
    for level in range(len(_LIGHT_EXPOSURES)):
        row = []
        channel_shade = _SHADED_CHANNELS[level]
        for c in colors:
            if level == 8:
                row.append(c & 0xFFFFFF)
                continue
            r = channel_shade[(c >> 16) & 0xFF]
            g = channel_shade[(c >> 8) & 0xFF]
            b = channel_shade[c & 0xFF]
            row.append((r << 16) | (g << 8) | b)
        pals.append(row)
    return pals


class TexImage(object):
    """Распакованное изображение атласа: pixels[y*w+x] (индексы палитры),
    shaded[light] — RGB-строки."""
    __slots__ = ('tid', 'w', 'h', 'hoff', 'voff', 'pixels', 'palettes',
                 'shaded', 'avg')

    def __init__(self, tid, w, h, hoff=0, voff=0):
        self.tid = tid
        self.w = w
        self.h = h
        self.hoff = hoff
        self.voff = voff
        self.pixels = None
        self.palettes = None
        self.shaded = None
        self.avg = (128, 128, 128)

    def build_shaded(self):
        self.shaded = []
        for lvl in range(16):
            pal = self.palettes[min(lvl, len(self.palettes) - 1)]
            self.shaded.append([struct.pack('BBB', (c >> 16) & 255,
                                            (c >> 8) & 255, c & 255)
                                for c in pal])
        # средний цвет по нейтральному уровню
        pr = self.palettes[min(8, len(self.palettes) - 1)]
        n = len(self.pixels)
        if n:
            sr = sg = sb = 0
            for p in self.pixels[::max(1, n // 256)]:
                c = pr[p] if p < len(pr) else 0
                sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255
            cnt = len(self.pixels[::max(1, n // 256)])
            self.avg = (sr // cnt, sg // cnt, sb // cnt)


class Assets(object):
    """Все текстуры/спрайты/палитры из tx1..tx4, sp1..sp4."""

    def __init__(self):
        self.wall_tex = {}    # tid (int, может быть отриц.) -> TexImage
        self.flat_sprites = {}  # положительные id 64x64 -> TexImage
        self.obj_tex = {}     # отрицательные id -> TexImage (sp)
        self.errors = []

    @staticmethod
    def load(res_gamedata_dir, wanted_palettes_only=False):
        import os
        a = Assets()
        palettized = {}  # глобальный индекс -> 16-уровневая палитра
        tid2pal = {}     # id (со знаком) -> глобальный индекс палитры
        pending = []     # (TexImage, глобальный индекс)
        g = 0
        for prefix, magic, hdr, is_sp in (
                ('tx', MAGIC_TX, 9, False), ('sp', MAGIC_SP, 13, True)):
            for num in (1, 2, 3, 4):
                path = os.path.join(res_gamedata_dir, 'textures',
                                    '%s%d' % (prefix, num))
                if not os.path.exists(path):
                    a.errors.append('нет файла ' + path)
                    continue
                data = open(path, 'rb').read()
                if _u16be(data, 0) != magic:
                    a.errors.append('плохой magic в %s' % path)
                    continue
                ent_cnt = _u16be(data, 2)
                pal_cnt = _u16be(data, 4)
                poff = _u32be(data, 6)
                o = 10
                entries = []
                for _ in range(ent_cnt):
                    tid = _i8(data[o])
                    w = _u16be(data, o + 1); h = _u16be(data, o + 3)
                    if is_sp:
                        hoff = _i16be(data, o + 5); voff = _i16be(data, o + 7)
                        pal_off = _u16be(data, o + 9)
                        bpp = _u16be(data, o + 11)
                        o += 13
                    else:
                        hoff = voff = 0
                        pal_off = _u16be(data, o + 5)
                        bpp = _u16be(data, o + 7)
                        o += 9
                    packed = (w * h * bpp + 7) // 8
                    raw = data[o:o + packed]
                    o += packed
                    entries.append((tid, w, h, hoff, voff, pal_off, bpp, raw))
                # палитры файла
                po = poff
                for p in range(pal_cnt):
                    cnt = _u32be(data, po); po += 4
                    colors = []
                    for c in range(cnt):
                        colors.append(_u32be(data, po)); po += 4
                    palettized[g + p] = create_color_palettes(colors)
                for (tid, w, h, hoff, voff, pal_off, bpp, raw) in entries:
                    img = TexImage(tid, w, h, hoff, voff)
                    up = unpack_bits_msb(raw, 0, w * h, bpp)
                    # источник хранится постолбцово: pixel (x,y) = up[x*h + y]
                    pix = bytearray(w * h)
                    for x in range(w):
                        src = x * h
                        for y in range(h):
                            pix[y * w + x] = up[src + y]
                    img.pixels = pix
                    gp = g + pal_off
                    pal = palettized.get(gp)
                    if pal is None:
                        a.errors.append('нет палитры %d для %s%d id=%d'
                                        % (gp, prefix, num, tid))
                        continue
                    img.palettes = pal
                    img.build_shaded()
                    if is_sp:
                        a.obj_tex[tid] = img
                    elif w == 64 and h == 64:
                        a.flat_sprites[tid] = img
                    else:
                        a.wall_tex[tid] = img
                g += pal_cnt
        # композиты: 39/40 из 35, 47/48 из 46, 50 из 49
        # (как Texture.compositeTexture: replace задаёт размеры + копирует
        # левый-верхний регион базы, blend копирует rect'ы поверх)
        for comp, (base_id, blends, (cw, ch)) in _COMPOSITES.items():
            base = a.wall_tex.get(base_id)
            if base is None or base.pixels is None:
                continue
            img = TexImage(comp, cw, ch)
            pix = bytearray(cw * ch)
            srcw = base.w
            for y in range(ch):
                for x in range(cw):
                    pix[y * cw + x] = base.pixels[y * srcw + x]
            for (sx, sy, w, h, dx, dy) in blends:
                for row in range(w):
                    for col in range(h):
                        pix[(dy + col) * cw + (dx + row)] = \
                            base.pixels[(sy + col) * srcw + (sx + row)]
            img.pixels = pix
            img.palettes = base.palettes
            img.build_shaded()
            a.wall_tex[comp] = img
        return a

    def resolve_wall_tex(self, raw_id):
        if raw_id in (0,):
            return None
        tid = _i8(raw_id & 0xFF)
        if tid < 0:
            return self.obj_tex.get(tid)
        tid = _REMAP_SIMPLE.get(tid, tid)
        t = self.wall_tex.get(tid)
        if t is not None:
            return t
        return self.flat_sprites.get(tid)

    def resolve_flat(self, raw_id):
        tid = raw_id & 0xFF
        if tid == 51:
            return 'sky'
        tid = _REMAP_SIMPLE.get(tid, tid)
        return self.flat_sprites.get(tid) or self.wall_tex.get(tid)

    def flat_avg_color(self, raw_id, light):
        t = self.resolve_flat(raw_id)
        if t == 'sky':
            return (96, 128, 192)
        if t is None:
            return (48, 48, 48)
        level = min(15, max(0, light))
        if level == 8:
            return t.avg
        exposure = _LIGHT_EXPOSURES[level]
        return (_shade_palette_channel(t.avg[0], exposure),
                _shade_palette_channel(t.avg[1], exposure),
                _shade_palette_channel(t.avg[2], exposure))

    def sky_tex(self):
        return self.wall_tex.get(25)

# ----------------------------------------------------------------------------
# 3D рендер предпросмотра (портальный рейкаст)
# ----------------------------------------------------------------------------

DOOR_TYPES = (1, 26, 28, 62)


class Camera(object):
    def __init__(self, x, z, angle, eye=32):
        self.x = float(x)
        self.z = float(z)
        self.angle = float(angle)   # рад, 0 -> взгляд вдоль +X
        self.eye = eye


def spawn_camera(level):
    for ob in level.objects:
        if 1 <= ob['type'] <= 4:
            ang = (-ob['angle'] * 1144 + 102943) / 65536.0
            return Camera(ob['x'], ob['z'], ang)
    return Camera(0, 0, 0.0)


def render_view(level, assets, cam, W=240, H=288, textured_flats=False,
                draw_sprites=True):
    """Возвращает bytearray RGB (W*H*3). 0 <= light <= 16."""
    buf = bytearray(W * H * 3)
    sides = level.sector_sides()
    edges = level.sectors_edges()
    sec = None
    for s in range(len(level.sectors)):
        if point_in_sector(level, edges, s, cam.x, cam.z):
            sec = s
            break
    if sec is None:
        return buf
    focal = W / 2.0
    sin_a = math.sin(cam.angle)
    cos_a = math.cos(cam.angle)
    zbuf = [1e30] * W
    half = H / 2.0
    # лучи per column: dir = (cosA*f - sinA*dx... )
    _render_columns(level, assets, sides, cam, sec, buf, W, H, focal,
                    sin_a, cos_a, zbuf, textured_flats)
    if draw_sprites:
        _render_sprites(level, assets, sides, cam, sec, buf, W, H, focal,
                        sin_a, cos_a, zbuf, edges)
    return buf


def _render_columns(level, assets, sides, cam, sec0, buf, W, H, focal,
                    sin_a, cos_a, zbuf, textured_flats):
    half = H / 2.0
    sectors = level.sectors
    sky = assets.sky_tex()
    for x in range(W):
        dxs = x - half
        # направление луча: fwd*focal + right*(x-half)
        rdx = cos_a * focal - sin_a * dxs
        rdz = sin_a * focal + cos_a * dxs
        si = sec0
        win_top, win_bot = 0, H - 1
        path = set()
        depth = 0
        while si is not None and depth < 24:
            depth += 1
            if si in path:
                break
            path.add(si)
            sdat = sectors[si]
            light = max(0, min(16, (sdat['light_packed'] >> 4) & 15))
            sfloor, sceil = sdat['floor'], sdat['ceil']
            # ближайшее пересечение луча со сторонами сектора
            best = None
            for sd in sides[si]:
                ex = sd['x2'] - sd['x1']
                ez = sd['z2'] - sd['z1']
                den = rdx * ez - rdz * ex
                if den == 0:
                    continue
                wx = sd['x1'] - cam.x
                wz = sd['z1'] - cam.z
                t = (wx * ez - wz * ex) / float(den)
                if t <= 1e-9:
                    continue
                u = (wx * rdz - wz * rdx) / float(den)
                # u < 0 или > 1 — промах мимо отрезка
                if u < 0.0 or u > 1.0:
                    continue
                if best is None or t < best[0]:
                    best = (t, u, sd)
            if best is None:
                break
            t, u, sd = best
            zperp = t * focal
            if zperp < 0.01:
                break
            # проекции высот
            ytop = int(round(half + (cam.eye - sceil) * focal / zperp))
            ybot = int(round(half + (cam.eye - sfloor) * focal / zperp))
            surf = sd['surf']
            is_door = (level.walls[sd['wall']]['type'] in DOOR_TYPES
                       or (level.walls[sd['wall']]['flags'] & 8))
            is_portal = (not sd['one_sided']) and not is_door
            v1 = level.vertices[level.walls[sd['wall']]['sv']]
            wall_len = max(1, int(round(math.hypot(
                level.vertices[level.walls[sd['wall']]['ev']][0] - v1[0],
                level.vertices[level.walls[sd['wall']]['ev']][1] - v1[1]))))
            if sd['surf_idx'] == level.walls[sd['wall']]['back']:
                uu = int(round((1.0 - u) * wall_len))
            else:
                uu = int(round(u * wall_len))
            uu += surf['ox']

            # пол/потолок текущего сектора в видимом окне
            ctop = max(ytop, win_top)
            cbot = min(ybot, win_bot)
            # потолок: от win_top до ctop-1
            _flat_strip(buf, W, H, x, win_top, ctop - 1, assets,
                        sdat['ceil_tex'], light, sky, cam, dxs, rdx, rdz,
                        focal, half, True, textured_flats, sceil)
            # пол: от cbot+1 до win_bot
            _flat_strip(buf, W, H, x, cbot + 1, win_bot, assets,
                        sdat['floor_tex'], light, sky, cam, dxs, rdx, rdz,
                        focal, half, False, textured_flats, sfloor)

            if not is_portal or sd['other_sector'] == si:
                # сплошная стена
                tex = assets.resolve_wall_tex(surf['main'])
                _tex_column(buf, W, H, x, ctop, cbot, tex, uu,
                            ytop, focal / zperp, surf['oy'], light)
                if ytop <= H - 1 and ybot >= 0:
                    zbuf[x] = zperp
                win_top = max(win_top, cbot + 1)
                break
            else:
                osec = sectors[sd['other_sector']]
                ofloor, oceil = osec['floor'], osec['ceil']
                if oceil != sceil and surf['upper']:
                    yt2 = int(round(half + (cam.eye - oceil) * focal / zperp))
                    y1u = min(yt2, cbot) - 1
                    if y1u >= ctop:
                        tex = assets.resolve_wall_tex(surf['upper'])
                        _tex_column(buf, W, H, x, ctop, y1u, tex, uu,
                                    ytop, focal / zperp, surf['oy'], light)
                        ctop = max(ctop, min(yt2, cbot))
                if ofloor != sfloor and surf['lower']:
                    yb2 = int(round(half + (cam.eye - ofloor) * focal / zperp))
                    y0l = max(yb2, ctop) + 1
                    if y0l <= cbot:
                        tex = assets.resolve_wall_tex(surf['lower'])
                        _tex_column(buf, W, H, x, y0l, cbot, tex, uu,
                                    yb2, focal / zperp, surf['oy'], light)
                        cbot = min(cbot, max(yb2, ctop))
                # сужаем окно и идём глубже
                win_top = max(win_top, ctop)
                win_bot = min(win_bot, cbot)
                if win_top > win_bot:
                    break
                si = sd['other_sector']


def _flat_strip(buf, W, H, x, y0, y1, assets, tex_id, light, sky, cam, dxs,
                rdx, rdz, focal, half, is_ceil, textured, plane_h):
    if y1 < y0 or y1 < 0 or y0 > H - 1:
        return
    tex = assets.resolve_flat(tex_id)
    y0c = max(0, y0)
    y1c = min(H - 1, y1)
    if tex == 'sky':
        _sky_strip(buf, W, x, y0c, y1c, sky, cam, dxs)
        return
    if not textured or tex is None or tex.pixels is None:
        # плоская заливка
        if tex is None or tex.pixels is None:
            rgb = bytes((40, 40, 40))
        else:
            rgb = bytes(assets.flat_avg_color(tex_id, light))
        row = x * 3
        for y in range(y0c, y1c + 1):
            o = y * W * 3 + row
            buf[o:o + 3] = rgb
    else:
        _flat_cast_column(buf, W, x, y0c, y1c, tex, light, rdx, rdz, focal,
                          half, cam, plane_h)


def _sky_strip(buf, W, x, y0, y1, sky, cam, dxs):
    if sky is None or y1 < y0:
        rgb = bytes((96, 128, 192))
        row = x * 3
        for y in range(y0, y1 + 1):
            o = y * W * 3 + row
            buf[o:o + 3] = rgb
        return
    tw, th = sky.w, sky.h
    u = int((cam.angle * tw / (2 * math.pi)) + dxs * (tw / W)) % tw
    shades = sky.shaded[8]
    row = x * 3
    step = th / max(1, (y1 - y0 + 1))
    for i, y in enumerate(range(y0, y1 + 1)):
        v = min(th - 1, int(i * step))
        buf[y * W * 3 + row: y * W * 3 + row + 3] = shades[sky.pixels[v * tw + u]]


def _flat_cast_column(buf, W, x, y0, y1, tex, light, rdx, rdz, focal, half,
                      cam, plane_h):
    """Текстурированный пол/потолок: плоскость y = plane_h."""
    shades = tex.shaded[min(15, max(0, light))]
    row = x * 3
    tw = tex.w
    inv = 1.0 / focal
    dh = cam.eye - plane_h
    if dh == 0:
        return
    for y in range(y0, y1 + 1):
        dy = y - half
        if dy == 0:
            continue
        dist = dh * focal / dy
        if dist <= 0:
            continue
        wx = cam.x + rdx * inv * dist
        wz = cam.z + rdz * inv * dist
        u = int(wx) & (tw - 1)
        v = int(wz) & (tw - 1)
        buf[y * W * 3 + row: y * W * 3 + row + 3] = shades[tex.pixels[v * tw + u]]


def _tex_column(buf, W, H, x, y0, y1, tex, u, anchor_sy, px_per_unit, oy,
                light):
    """Текстурированная колонка стены. v=0 на anchor_sy (мир: потолок),
    1 тексель = 1 мировая единица (px_per_unit = focal/z)."""
    if y1 < y0:
        return
    x3 = x * 3
    if tex is None or tex.pixels is None:
        rgb = b'\xc0\x00\xc0'  # magenta — нет текстуры
        for y in range(max(0, y0), min(H - 1, y1) + 1):
            o = y * W * 3 + x3
            buf[o:o + 3] = rgb
        return
    if px_per_unit <= 0:
        px_per_unit = 0.05
    tw, th = tex.w, tex.h
    shades = tex.shaded[min(15, max(0, light))]
    umod = u % tw
    # шаг по v на экранный пиксель
    vstep = 1.0 / px_per_unit
    v0 = (max(0, y0) - anchor_sy) * vstep + oy
    for y in range(max(0, y0), min(H - 1, y1) + 1):
        v = int(v0) % th
        buf[y * W * 3 + x3: y * W * 3 + x3 + 3] = shades[
            tex.pixels[v * tw + umod]]
        v0 += vstep


def _render_sprites(level, assets, sides, cam, sec0, buf, W, H, focal,
                    sin_a, cos_a, zbuf, edges):
    half = H / 2.0
    items = []
    secs = level.sectors
    sec_of = {}
    edges_ = edges
    for ob in level.objects:
        if 1 <= ob['type'] <= 4:
            continue
        dx = ob['x'] - cam.x
        dz = ob['z'] - cam.z
        cz = dx * cos_a + dz * sin_a
        if cz < 4:
            continue
        cx = -dx * sin_a + dz * cos_a
        items.append((cz, cx, ob))
    items.sort(key=lambda it: -it[0])
    for cz, cx, ob in items:
        tex_id = OBJ_SPRITE.get(ob['type'])
        tex = assets.obj_tex.get(tex_id) if tex_id else None
        sx = half + cx * focal / cz
        # сектор объекта (для пола/света)
        s = sec_of.get(id(ob))
        if s is None:
            s = sec0
            for cand in range(len(secs)):
                if point_in_sector(level, edges_, cand, ob['x'], ob['z']):
                    s = cand
                    break
            sec_of[id(ob)] = s
        sdat = secs[s]
        light = max(0, min(15, (sdat['light_packed'] >> 4) & 15))
        ppu = focal / cz
        floor = sdat['floor']
        if tex is not None and tex.pixels is not None:
            scale_h = tex.h
            yb = half + (cam.eye - floor) * ppu - tex.voff * ppu
            ph = scale_h * ppu
            pw = tex.w * ppu
            x0 = int(round(sx - pw / 2 - tex.hoff * ppu))
            x1 = int(round(sx + pw / 2 - tex.hoff * ppu))
            y0 = int(round(yb - ph))
            y1 = int(round(yb))
            shades = tex.shaded[light]
            tw, th = tex.w, tex.h
            for x in range(max(0, x0), min(W - 1, x1) + 1):
                if zbuf[x] <= cz:
                    continue
                tx = int((x - x0) * tw / max(1.0, pw)) % tw
                vv = 0.0
                dv = th / max(1.0, ph)
                for y in range(max(0, y0), min(H - 1, y1) + 1):
                    ty = min(th - 1, int(vv))
                    idx = tex.pixels[ty * tw + tx]
                    if idx != 0:  # индекс 0 = прозрачность
                        o = y * W * 3 + x * 3
                        buf[o:o + 3] = shades[idx]
                    vv += dv
        else:
            # заглушка: цветной столбик
            pw = max(2, int(24 * ppu))
            phpx = max(2, int(48 * ppu))
            yb = half + (cam.eye - floor) * ppu
            c = (200, 80, 80) if ob['type'] >= 3000 else (80, 200, 120)
            rgb = bytes(c)
            for x in range(max(0, int(sx - pw / 2)),
                           min(W - 1, int(sx + pw / 2)) + 1):
                if zbuf[x] <= cz:
                    continue
                for y in range(max(0, int(yb - phpx)),
                               min(H - 1, int(yb)) + 1):
                    o = y * W * 3 + x * 3
                    buf[o:o + 3] = rgb

# ----------------------------------------------------------------------------
# Валидация ссылочной целостности (для диалога в редакторе)
# ----------------------------------------------------------------------------

def validate_level(level, assets=None):
    errs = []
    warn = []
    nv, nw = len(level.vertices), len(level.walls)
    ns, nsec = len(level.surfaces), len(level.sectors)
    for i, w in enumerate(level.walls):
        if not (0 <= w['sv'] < nv) or not (0 <= w['ev'] < nv):
            errs.append('стена %d: невалидная вершина' % i)
        if not (-1 <= w['front'] < ns):
            errs.append('стена %d: front surface %d вне диапазона'
                        % (i, w['front']))
        if not (-1 <= w['back'] < ns):
            errs.append('стена %d: back surface %d вне диапазона'
                        % (i, w['back']))
        if w['front'] == -1:
            warn.append('стена %d: нет front surface' % i)
    for i, s in enumerate(level.surfaces):
        if not (0 <= s['sector'] < nsec):
            errs.append('surface %d: sector %d вне диапазона' % (i, s['sector']))
    for i, ob in enumerate(level.objects):
        s = find_sector_at(level, ob['x'], ob['z'])
        if s is None:
            warn.append('объект %d (type %d) вне всех секторов'
                        % (i, ob['type']))
    used_surfs = set()
    for w in level.walls:
        used_surfs.add(w['front']); used_surfs.add(w['back'])
    for i in range(ns):
        if i not in used_surfs:
            warn.append('surface %d не используется ни одной стеной' % i)
    if assets is not None:
        for i, w in enumerate(level.walls):
            for side in ('front', 'back'):
                si = w[side]
                if si < 0 or si >= ns:
                    continue
                sf = level.surfaces[si]
                for key in ('main', 'upper', 'lower'):
                    tid = sf[key]
                    if tid and assets.resolve_wall_tex(tid) is None:
                        warn.append('стена %d %s.%s: текстуры id=%d нет '
                                    'в атласах' % (i, side, key, tid))
    if not any(1 <= ob['type'] <= 4 for ob in level.objects):
        warn.append('нет точки появления игрока (type 1..4)')
    return errs, warn

# ----------------------------------------------------------------------------
# Мини-утилиты вывода (PPM для tk.PhotoImage, BMP для тестов)
# ----------------------------------------------------------------------------

def rgb_to_ppm(rgb, w, h):
    return b'P6\n%d %d\n255\n' % (w, h) + bytes(rgb)


def tex_to_ppm(tex, light=8, scale=1):
    """PPM картинки текстуры (для превью/пикеров)."""
    if tex is None or tex.pixels is None:
        w = h = 16
        return b'P6\n16 16\n255\n' + b'\xc0\x00\xc0' * 256
    w, h = tex.w, tex.h
    shades = tex.shaded[min(15, max(0, light))]
    out = bytearray(w * h * 3)
    for y in range(h):
        r = y * w
        o = y * w * 3
        for x in range(w):
            out[o + x * 3: o + x * 3 + 3] = shades[tex.pixels[r + x]]
    if scale > 1:
        out2 = bytearray()
        for y in range(h):
            rowb = bytearray()
            for x in range(w):
                c = bytes(out[(y * w + x) * 3:(y * w + x) * 3 + 3])
                rowb += c * scale
            for _ in range(scale):
                out2 += rowb
        out = out2
        w *= scale
        h *= scale
    return b'P6\n%d %d\n255\n' % (w, h) + bytes(out)


def write_bmp24(path, rgb, w, h):
    row = (w * 3 + 3) & ~3
    img = bytearray()
    pad = b'\0' * (row - w * 3)
    for y in range(h - 1, -1, -1):
        r = y * w * 3
        line = bytearray()
        for x in range(w):
            o = r + x * 3
            line += bytes((rgb[o + 2], rgb[o + 1], rgb[o]))
        img += line + pad
    hdr = struct.pack('<2sIHHI', b'BM', 54 + len(img), 0, 0, 54)
    dib = struct.pack('<IiiHHIIiiII', 40, w, h, 1, 24, 0, len(img),
                      2835, 2835, 0, 0)
    open(path, 'wb').write(hdr + dib + bytes(img))

# ----------------------------------------------------------------------------
# CLI self-test
# ----------------------------------------------------------------------------

def _selftest(resdir):
    import os
    lvdir = os.path.join(resdir, 'levels')
    ok = True
    for name in sorted(os.listdir(lvdir)):
        data = open(os.path.join(lvdir, name), 'rb').read()
        lv = parse_level(data)
        back = dump_level(lv)
        same = back == data
        ok &= same
        print('%s: %d байт, roundtrip %s, sec=%d walls=%d nodes=%d '
              'leaves=%d segs=%d' % (name, len(data),
                                     'OK' if same else 'MISMATCH',
                                     len(lv.sectors), len(lv.walls),
                                     len(lv.nodes), len(lv.leaves),
                                     len(lv.segments)))
    a = Assets.load(resdir)
    print('атласы: wall=%d flat=%d obj=%d ошибок=%d'
          % (len(a.wall_tex), len(a.flat_sprites), len(a.obj_tex),
             len(a.errors)))
    for e in a.errors:
        print('  !', e)
    return ok


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == 'selftest':
        good = _selftest(sys.argv[2])
        sys.exit(0 if good else 1)
    print('co3d_level_core: selftest <res/gamedata>')
