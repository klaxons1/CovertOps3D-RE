#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Regression checks for the classic MAP01 Zakat PWAD import.

The author-supplied PWAD is a converter source under docs/ on main.  It omits
PLAYPAL and a few commercial-IWAD patches, so conversion deliberately layers
it over docs/DOOM.WAD while emitting a self-contained Java ME package.
"""

import os
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'scripts'))
import c3d2_core as C3
import c3d2_texture_tools as TEXTURES
import doom_wad_core as DOOM

BASE_WAD = os.path.join(ROOT, 'docs', 'DOOM.WAD')
ZAKAT_WAD = os.environ.get('ZAKAT_WAD', os.path.join(ROOT, 'docs', 'LEST _ZAKAT.wad'))
ZAKAT_SHA256 = 'e63bbd23b27c957f41536f1f144c05845917dca5d43c1f99cd799d19a8808b4a'


def _material_path(package_dir, relative):
    return os.path.normpath(os.path.join(package_dir, relative))


def main():
    if not os.path.exists(ZAKAT_WAD):
        print('LEST _ZAKAT.wad not present; Zakat conversion fixture skipped')
        return
    if not os.path.exists(BASE_WAD):
        print('DOOM.WAD base not present; Zakat conversion fixture skipped')
        return

    map_wad = DOOM.WadFile(ZAKAT_WAD)
    base_wad = DOOM.WadFile(BASE_WAD)
    resources = DOOM.WadOverlay(map_wad, base_wad)
    assert map_wad.magic == 'PWAD'
    assert map_wad.sha256() == ZAKAT_SHA256
    assert len(DOOM.parse_palette(resources)) == 256

    doom_map = DOOM.load_map(map_wad, 'MAP01')
    assert (len(doom_map.vertices), len(doom_map.linedefs), len(doom_map.sidedefs),
            len(doom_map.sectors), len(doom_map.things)) == (2534, 2365, 4325, 438, 151)

    with tempfile.TemporaryDirectory() as temp_root:
        package_dir = os.path.join(temp_root, 'doom-zakat')
        shared_dir = os.path.join(temp_root, 'doom-zakat-common')
        report = DOOM.convert_map(resources, doom_map, package_dir,
                                  shared_asset_dir=shared_dir,
                                  pvs_mode='doom-reject',
                                  allow_bsp_mismatches=True)

        assert report['shared_assets']
        assert (report['wall_textures'], report['flats'], report['things']) == (50, 22, 151)
        assert (report['enemies'], report['world_items'], report['lifts']) == (0, 135, 0)
        assert (report['bsp_nodes'], report['bsp_leaves'], report['bsp_segments']) == (1861, 1862, 5366)
        # MAP01 contains a small set of Zandronum-style overlapping sector
        # samples. Geometry is emitted with a documented relaxed validation,
        # rather than silently lying about it or shipping the source WAD.
        assert report['bsp_mismatches_allowed']
        assert report['bsp_failures'] == 12
        assert report['structural_closed_sectors'] == 13
        assert report['pvs_mode'] == 'doom-reject'
        assert report['pvs_visible_pairs'] == 123266
        assert len(report['missing_wall_textures']) == 1
        assert report['missing_wall_textures'][0].startswith('SKY1 texture fallback:')

        info = C3.read_c3b(os.path.join(package_dir, 'level.c3b'))
        # BSP splitting introduces deterministic intersection vertices.
        assert (info['vertices'], info['walls'], info['sectors'], info['objects']) == (2987, 2365, 438, 0)
        assert info['entities'] == 'entities.ini'
        assert os.path.getsize(os.path.join(package_dir, 'level.c3b')) < 262144

        committed = os.path.join(ROOT, 'res', 'gamedata', 'custom', 'doom-zakat')
        if os.path.exists(os.path.join(committed, 'level.c3b')):
            assert open(os.path.join(package_dir, 'level.c3b'), 'rb').read() == open(
                os.path.join(committed, 'level.c3b'), 'rb').read()
            assert open(os.path.join(package_dir, 'materials.c3m'), 'rb').read() == open(
                os.path.join(committed, 'materials.c3m'), 'rb').read()

        materials = TEXTURES.load_manifest(os.path.join(package_dir, 'materials.c3m'))
        assert len(materials) == 139
        assert len([key for key in materials if key.startswith('wall.')]) == 50
        assert len([key for key in materials if key.startswith('flat.')]) == 22
        assert len([key for key in materials if key.startswith('sprite.')]) == 66
        for key, value in materials.items():
            if key.startswith('anim.'):
                continue
            assert os.path.exists(_material_path(package_dir, value)), (key, value)

        source = C3.load_source(os.path.join(package_dir, 'level.c3d.json'))
        assert len(source.level.objects) == 136  # one spawn + all 135 world things
        assert sum(1 for entity in source.level.objects if entity['type'] >= DOOM.DOOM_ITEM_BASE) == 135
        assert sum(1 for wall in source.level.walls if wall['type'] != 0) == 0

        # The PWAD's custom BOXBIG patch shares a name with a flat; patch
        # namespace lookup must keep the authored wall rather than a fallback.
        boxbig_slot = None
        for line in open(os.path.join(package_dir, 'doom_materials.ini'), 'r', encoding='utf-8'):
            if line.startswith('wall.BOXBIG='):
                boxbig_slot = int(line.split('=', 1)[1])
                break
        assert boxbig_slot is not None
        boxbig = _material_path(package_dir, materials['wall.%d' % boxbig_slot])
        assert os.path.exists(boxbig)

    print('Zakat MAP01: PWAD overlay, custom materials, all things and compact C3B: OK')


if __name__ == '__main__':
    main()
