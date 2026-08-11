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
        assert report['pvs_mode'] == 'doom-reject'
        assert report['pvs_visible_pairs'] == 6021
        assert (report['wall_textures'], report['flats']) == (32, 21)
        assert (report['doors'], report['enemies'], report['enemy_sprite_materials']) == (8, 29, 3)
        assert report['hud_weapon_frames'] == 16
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
        assert len(materials) == 57
        assert all(os.path.exists(os.path.join(directory, path))
                   for path in materials.values())
        sprite_keys = sorted(key for key in materials if key.startswith('sprite.'))
        assert sprite_keys == ['sprite.1', 'sprite.2', 'sprite.3']
        for weapon in ('fist', 'pistol', 'shotgun', 'chaingun', 'rocket', 'plasma', 'bfg', 'chainsaw'):
            for frame in ('a', 'b'):
                assert os.path.exists(os.path.join(directory, 'hud', weapon + '_' + frame + '.bmp'))
        for key in sprite_keys:
            sprite = open(os.path.join(directory, materials[key]), 'rb').read()
            width, height, planes, bpp = struct.unpack_from('<iiHH', sprite, 18)
            assert 1 <= width <= 255 and height == DOOM.DOOM_RUNTIME_SPRITE_HEIGHT
            assert (planes, bpp) == (1, 4)
        source = C3.load_source(os.path.join(directory, 'level.c3d.json'))
        assert len(source.level.objects) == 33
        assert source.level.objects[0] == dict(x=1056, z=-3616, angle=180, type=1, param=0)
        assert sum(1 for entity in source.level.objects if entity.get('sprite', 0)) == 29
        door_walls = [wall for wall in source.level.walls if wall['type'] == 1]
        assert len(door_walls) == 8
        shared_sky_portals = 0
        for wall in source.level.walls:
            if wall['back'] < 0:
                continue
            front = source.level.sectors[source.level.surfaces[wall['front']]['sector']]
            back = source.level.sectors[source.level.surfaces[wall['back']]['sector']]
            if front['ceil_tex'] == 51 and back['ceil_tex'] == 51 and front['ceil'] != back['ceil']:
                shared_sky_portals += 1
        assert shared_sky_portals == 6
        for wall in door_walls:
            front = source.level.sectors[source.level.surfaces[wall['front']]['sector']]
            back = source.level.sectors[source.level.surfaces[wall['back']]['sector']]
            assert back['ceil'] == back['floor']
            assert front['ceil'] - max(front['floor'], back['floor']) >= 50
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
