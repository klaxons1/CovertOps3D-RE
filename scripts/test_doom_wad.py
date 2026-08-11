#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Regression checks for classic DOOM.WAD -> compact C3D2 conversion."""

import os
import struct
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'scripts'))
import c3d2_core as C3
import c3d2_texture_tools as TEXTURES
import doom_wad_core as DOOM

WAD = os.path.join(ROOT, 'docs', 'DOOM.WAD')


def main():
    if not os.path.exists(WAD):
        print('DOOM.WAD not present; conversion fixture skipped')
        return
    wad = DOOM.WadFile(WAD)
    assert wad.magic == 'IWAD'
    doom_map = DOOM.load_map(wad, 'E1M1')
    assert (len(doom_map.vertices), len(doom_map.linedefs), len(doom_map.sidedefs),
            len(doom_map.sectors), len(doom_map.things)) == (437, 452, 622, 83, 130)
    assert any(thing['type'] == 1 for thing in doom_map.things)

    with tempfile.TemporaryDirectory() as directory:
        report = DOOM.convert_map(wad, doom_map, directory, extract_sprites='none')
        assert report['missing_wall_textures'] == []
        assert report['bsp_failures'] == 0
        assert (report['wall_textures'], report['flats']) == (32, 21)
        info = C3.read_c3b(os.path.join(directory, 'level.c3b'))
        assert info['entities'] == 'entities.ini'
        committed = os.path.join(ROOT, 'res', 'gamedata', 'custom', 'doom-e1m1')
        if os.path.exists(os.path.join(committed, 'level.c3b')):
            assert open(os.path.join(directory, 'level.c3b'), 'rb').read() == open(
                os.path.join(committed, 'level.c3b'), 'rb').read()
            assert open(os.path.join(directory, 'materials.c3m'), 'rb').read() == open(
                os.path.join(committed, 'materials.c3m'), 'rb').read()
        assert (info['walls'], info['sectors']) == (452, 83)
        assert info['vertices'] >= 437
        assert info['objects'] == 0
        assert os.path.getsize(os.path.join(directory, 'level.c3b')) < 32768
        assert os.path.getsize(os.path.join(directory, 'materials.c3m')) > 1000
        materials = TEXTURES.load_manifest(os.path.join(directory, 'materials.c3m'))
        assert len(materials) == 54
        assert all(os.path.exists(os.path.join(directory, path))
                   for path in materials.values())
        source = C3.load_source(os.path.join(directory, 'level.c3d.json'))
        for surface in source.level.surfaces:
            for slot in (surface['upper'], surface['lower'], surface['main']):
                if slot:
                    assert 'wall.%d' % slot in materials
        for sector in source.level.sectors:
            for slot in (sector['floor_tex'], sector['ceil_tex']):
                if slot and slot != 51:
                    assert 'flat.%d' % slot in materials
        texture_dir = os.path.join(directory, 'textures')
        assert len(os.listdir(texture_dir)) == 54
        for name in os.listdir(texture_dir):
            data = open(os.path.join(texture_dir, name), 'rb').read()
            width, height, planes, bpp = struct.unpack_from('<iiHH', data, 18)
            assert planes == 1 and bpp == 4
            if name.startswith('wall_'):
                assert width > 0 and (width & (width - 1)) == 0
                assert height in (16, 64, 128)
            elif name.startswith('flat_'):
                assert (width, height) == (64, 64)
            elif name == 'sky.bmp':
                assert (width, height) == (64, 128)
    print('DOOM E1M1: geometry, BMP4 materials, spawn and compact C3B: OK')


if __name__ == '__main__':
    main()
