# -*- coding: utf-8 -*-
"""Headless-проверки ядра редактора уровней (запуск из корня репо):

    python3 scripts/test_co3d_core.py

Проверяет:
  1) parse/dump roundtrip всех карт (байт-в-байт);
  2) парсинг атласов + разрешение всех текстур, реально используемых картами;
  3) ребилд сегментов/BSP/PVS для каждой карты: дерево обязано классифицировать
     контрольные точки (объекты + внутренние точки секторов) так же, как
     точечный тест принадлежности сектору; результат можно сохранить и снова
     прогнать roundtrip;
  4) рендер кадров из точек спавна в BMP (для визуальной проверки).
BMP складываются в scripts/_test_out/.
"""

import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import co3d_level_core as C

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..',
                   'res', 'gamedata')
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_test_out')


def test_roundtrip():
    print('== roundtrip ==')
    ok = True
    for name in sorted(os.listdir(os.path.join(RES, 'levels'))):
        data = open(os.path.join(RES, 'levels', name), 'rb').read()
        lv = C.parse_level(data)
        same = C.dump_level(lv) == data
        ok &= same
        print('  %-10s %s' % (name, 'OK' if same else 'MISMATCH'))
    return ok


def test_texture_refs():
    print('== texture refs ==')
    a = C.Assets.load(RES)
    assert not a.errors, a.errors
    missing = set()
    for name in sorted(os.listdir(os.path.join(RES, 'levels'))):
        lv = C.parse_level(
            open(os.path.join(RES, 'levels', name), 'rb').read())
        for s in lv.surfaces:
            for k in ('main', 'upper', 'lower'):
                tid = s[k]
                if tid and a.resolve_wall_tex(tid) is None:
                    missing.add(tid)
        for s in lv.sectors:
            for k in ('floor_tex', 'ceil_tex'):
                tid = s[k]
                if tid and a.resolve_flat(tid) is None:
                    missing.add(('flat', tid))
    if missing:
        print('  НЕ НАЙДЕНЫ:', sorted(map(str, missing)))
        return False
    print('  все ссылки на текстуры разрешаются; wall=%d flat=%d obj=%d'
          % (len(a.wall_tex), len(a.flat_sprites), len(a.obj_tex)))
    return True


def test_rebuild():
    print('== bsp rebuild ==')
    ok = True
    for name in sorted(os.listdir(os.path.join(RES, 'levels'))):
        data = open(os.path.join(RES, 'levels', name), 'rb').read()
        lv = C.parse_level(data)
        t0 = time.time()
        rep = C.rebuild_derived(lv, validate=True)
        dt = time.time() - t0
        # пересохранённый файл должен валидно парситься
        red = C.parse_level(C.dump_level(lv))
        leaf_secs = C.leaf_sectors(red)
        bad_leaf = sum(1 for s in leaf_secs if s < 0)
        ok_leaf = bad_leaf == 0
        fails = len(rep.fail_samples)
        ok &= (fails == 0 and ok_leaf)
        print('  %-10s %.1fs nodes=%d leaves=%d segs=%d splits=%d mixed=%d '
              'fails=%d badleaf=%d' % (name, dt, len(lv.nodes),
                                       len(lv.leaves), len(lv.segments),
                                       rep.splits, rep.mixed_leaves, fails,
                                       bad_leaf))
        if fails:
            for f in rep.fail_samples[:4]:
                print('     fail: x=%.1f z=%.1f exp=%s got=%s' % f)
    return ok


def test_render():
    print('== render ==')
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    a = C.Assets.load(RES)
    for name in sorted(os.listdir(os.path.join(RES, 'levels'))):
        lv = C.parse_level(
            open(os.path.join(RES, 'levels', name), 'rb').read())
        cam = C.spawn_camera(lv)
        t0 = time.time()
        buf = C.render_view(lv, a, cam, W=240, H=288, textured_flats=False,
                            draw_sprites=True)
        dt = time.time() - t0
        C.write_bmp24(os.path.join(OUT, name + '.bmp'), buf, 240, 288)
        print('  %-10s %.2fs -> %s.bmp' % (name, dt, name))
    # пара кадров с текстурированными полами
    for name in ('level_01a', 'level_05'):
        lv = C.parse_level(
            open(os.path.join(RES, 'levels', name), 'rb').read())
        cam = C.spawn_camera(lv)
        buf = C.render_view(lv, a, cam, W=240, H=288, textured_flats=True,
                            draw_sprites=True)
        C.write_bmp24(os.path.join(OUT, name + '_texfloor.bmp'), buf, 240, 288)
    return True


if __name__ == '__main__':
    ok = True
    ok &= test_roundtrip()
    ok &= test_texture_refs()
    ok &= test_rebuild()
    ok &= test_render()
    print('ИТОГ:', 'OK' if ok else 'FAIL')
    sys.exit(0 if ok else 1)
