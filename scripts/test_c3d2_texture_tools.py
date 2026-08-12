#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Headless checks for C3D2 texture import helpers (no Pillow/Pygame needed)."""

import os
import struct
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'scripts'))
import c3d2_texture_tools as T


def main():
    assert T.target_size(T.MATERIAL_WALL, 64, 128) == (64, 128)
    assert T.target_size(T.MATERIAL_FLAT) == (64, 64)
    assert T.target_size(T.MATERIAL_SKY) == (64, 128)
    assert T.target_size(T.MATERIAL_SPRITE) == (64, 64)
    assert T.material_key(T.MATERIAL_WALL, 12) == 'wall.12'
    assert T.material_key(T.MATERIAL_FLAT, 3) == 'flat.3'
    assert T.material_key(T.MATERIAL_SPRITE, 3) == 'sprite.3'
    assert T.material_key(T.MATERIAL_SKY) == 'sky'

    # A deliberately varied image exercises all 16 K-means clusters.
    pixels = []
    for y in range(32):
        for x in range(32):
            pixels.append(((x * 9) & 255, (y * 7) & 255, ((x + y) * 5) & 255))
    palette, indices = T.kmeans_quantize(pixels, 16, 20)
    assert len(palette) == 16
    assert len(indices) == len(pixels)
    assert all(0 <= index < 16 for index in indices)
    assert T.kmeans_quantize(pixels, 16, 20) == (palette, indices)

    rgba = [(255, 0, 0, 255), (0, 0, 255, 128)]
    assert T.rgba_to_rgb(rgba) == [(255, 0, 0), (0, 0, 128)]
    resized = T.resize_rgba(rgba, 2, 1, 4, 2, fit=False)
    assert len(resized) == 8

    with tempfile.TemporaryDirectory() as directory:
        bmp = os.path.join(directory, 'texture.bmp')
        T.write_bmp4(bmp, 32, 32, palette, indices)
        data = open(bmp, 'rb').read()
        assert data[:2] == b'BM'
        width, height, planes, bpp = struct.unpack_from('<iiHH', data, 18)
        assert (width, height, planes, bpp) == (32, 32, 1, 4)

        manifest = os.path.join(directory, 'materials.c3m')
        open(manifest, 'w', encoding='utf-8').write('# keep me\nwall.1=old.bmp\n')
        T.update_manifest(manifest, 'wall.1', 'textures/wall_1.bmp')
        T.update_manifest(manifest, 'flat.1', 'textures/flat_1.bmp')
        entries = T.load_manifest(manifest)
        assert entries['wall.1'] == 'textures/wall_1.bmp'
        assert entries['flat.1'] == 'textures/flat_1.bmp'
        assert '# keep me' in open(manifest, encoding='utf-8').read()

        package = os.path.join(directory, 'starter')
        starter_manifest = T.create_starter_materials(package)
        starter = T.load_manifest(starter_manifest)
        assert set(('wall.1', 'flat.1', 'sky')).issubset(set(starter.keys()))
        expected_sizes = {'wall.1': (64, 128), 'flat.1': (64, 64), 'sky': (64, 128)}
        for key, relative in starter.items():
            path = os.path.join(package, relative)
            assert os.path.exists(path)
            data = open(path, 'rb').read()
            width, height, planes, bpp = struct.unpack_from('<iiHH', data, 18)
            assert (width, height) == expected_sizes[key]
            assert (planes, bpp) == (1, 4)

    print('C3D2 texture tools: K-means, BMP4 and manifest update: OK')


if __name__ == '__main__':
    main()
