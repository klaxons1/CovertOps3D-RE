#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Headless regression checks for the C3D2 source/compiler pipeline."""

import copy
import os
import struct
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'scripts'))
import c3d2_core as C3

DEMO = os.path.join(ROOT, 'res', 'gamedata', 'custom', 'demo')


def check_bmp4(path, expected_w, expected_h):
    data = open(path, 'rb').read()
    assert data[:2] == b'BM', path
    width, height, planes, bpp = struct.unpack_from('<iiHH', data, 18)
    assert (width, height, planes, bpp) == (expected_w, expected_h, 1, 4), path


def main():
    source = os.path.join(DEMO, 'level.c3d.json')
    document = C3.load_source(source)
    assert document.materials == 'materials.c3m'
    assert len(document.level.vertices) == 4
    assert len(document.level.walls) == 4

    with tempfile.TemporaryDirectory() as temp:
        one = os.path.join(temp, 'one.c3b')
        two = os.path.join(temp, 'two.c3b')
        C3.compile_source(source, one)
        C3.compile_source(source, two)
        compiled = open(one, 'rb').read()
        assert compiled == open(two, 'rb').read()
        # Keep the bundled fixture in lockstep with its editable source.
        assert compiled == open(os.path.join(DEMO, 'level.c3b'), 'rb').read()
        info = C3.read_c3b(one)
        assert info['materials'] == 'materials.c3m'
        assert (info['vertices'], info['walls'], info['sectors']) == (4, 4, 1)
        assert (info['nodes'], info['leaves'], info['segments']) == (1, 2, 4)

        # The renderer's front side faces the sector on its right.  A room
        # drawn counter-clockwise used to compile but generated no visible
        # wall columns, leaving the gameplay frame black.
        backwards = copy.deepcopy(document.level)
        for wall in backwards.walls:
            wall['sv'], wall['ev'] = wall['ev'], wall['sv']
        bad_source = os.path.join(temp, 'counter_clockwise.c3d.json')
        C3.dump_source(C3.C3DDocument(backwards, document.materials), bad_source)
        try:
            C3.load_source(bad_source)
            raise AssertionError('counter-clockwise C3D source was accepted')
        except ValueError as error:
            assert 'counter-clockwise' in str(error)

    check_bmp4(os.path.join(DEMO, 'textures', 'wall.bmp'), 64, 128)
    check_bmp4(os.path.join(DEMO, 'textures', 'floor.bmp'), 64, 64)
    check_bmp4(os.path.join(DEMO, 'textures', 'sky.bmp'), 64, 128)
    print('C3D2: source, deterministic C3B compile and loose BMP4 materials: OK')


if __name__ == '__main__':
    main()
