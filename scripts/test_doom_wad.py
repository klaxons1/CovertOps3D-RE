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
        assert (report['wall_textures'], report['flats']) == (32, 23)
        assert (report['animated_walls'], report['animated_flats'], report['damage_sectors']) == (0, 1, 4)
        assert (report['doors'], report['exits'], report['enemies'],
                report['enemy_sprite_materials'], report['enemy_death_sprite_materials']) == (8, 1, 29, 33, 12)
        assert report['hud_weapon_frames'] == 16
        assert (report['projectile_sprite_materials'], report['projectile_sprite_height']) == (4, 32)
        assert (report['thing_sprite_materials'], report['world_items']) == (19, 84)
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
        assert len(materials) == 113
        material_paths = dict((key, path) for key, path in materials.items()
                              if key == 'sky' or key.startswith('wall.')
                              or key.startswith('flat.') or key.startswith('sprite.'))
        assert all(os.path.exists(os.path.join(directory, path))
                   for path in material_paths.values())
        assert len([key for key in materials if key.startswith('anim.wall.')]) == 0
        assert len([key for key in materials if key.startswith('anim.flat.')]) == 1
        sprite_keys = sorted((key for key in materials if key.startswith('sprite.')),
                             key=lambda key: int(key.split('.')[1]))
        assert sprite_keys == ['sprite.%d' % index for index in range(1, 57)]
        # BFUGA0 is the pickup; the emitted BFG projectile is the 32x32
        # BFS1 ball in its stable generated slot.
        assert DOOM.DOOM_PROJECTILES[2] == ('bfg', 'BFS1A0')
        assert materials['sprite.36'].endswith('sprites/doom/36_bfg.bmp')
        for weapon in ('fist', 'pistol', 'shotgun', 'chaingun', 'rocket', 'plasma', 'bfg', 'chainsaw'):
            for frame in ('a', 'b'):
                assert os.path.exists(os.path.join(directory, 'hud', weapon + '_' + frame + '.bmp'))
        for key in sprite_keys:
            sprite = open(os.path.join(directory, materials[key]), 'rb').read()
            width, height, planes, bpp = struct.unpack_from('<iiHH', sprite, 18)
            slot = int(key.split('.')[1])
            if slot <= 21:
                assert 1 <= width <= 255 and height == DOOM.DOOM_RUNTIME_SPRITE_HEIGHT
            elif slot <= 33:
                # Wide falling/corpse poses retain aspect while being capped
                # to the Java ME bitmap width limit.
                assert 1 <= width <= DOOM.DOOM_RUNTIME_ACTOR_MAX_WIDTH
                assert 1 <= height <= DOOM.DOOM_RUNTIME_SPRITE_HEIGHT
            elif slot <= 37:
                assert 1 <= width <= 255 and height == DOOM.DOOM_RUNTIME_PROJECTILE_HEIGHT
            else:
                assert 1 <= width <= DOOM.DOOM_RUNTIME_DECORATION_MAX_WIDTH
                assert 1 <= height <= DOOM.DOOM_RUNTIME_DECORATION_HEIGHT
            assert (planes, bpp) == (1, 4)
        # Pickups and barrels are intentionally smaller than static decor;
        # this catches a regression that made a medikit as tall as a lamp.
        dimensions = {}
        for key in sprite_keys:
            name = os.path.basename(materials[key])
            data = open(os.path.join(directory, materials[key]), 'rb').read()
            dimensions[name] = struct.unpack_from('<ii', data, 18)
        assert dimensions['47_medi.bmp'][1] == DOOM.DOOM_RUNTIME_PICKUP_HEIGHT
        assert dimensions['53_bar1.bmp'][1] == DOOM.DOOM_RUNTIME_BARREL_HEIGHT
        assert dimensions['40_elec.bmp'][1] == DOOM.DOOM_RUNTIME_DECORATION_HEIGHT
        source = C3.load_source(os.path.join(directory, 'level.c3d.json'))
        assert len(source.level.objects) == 117
        assert source.level.objects[0] == dict(x=1056, z=-3616, angle=180, type=1, param=0)
        assert sum(1 for entity in source.level.objects if entity.get('sprite', 0)) == 113
        animated_enemies = [entity for entity in source.level.objects
                            if entity['type'] in (3001, 3003, 3004)]
        assert len(animated_enemies) == 29
        assert all(all(entity.get('frame%d' % frame, 0) != 0 for frame in range(1, 7))
                   for entity in animated_enemies)
        assert all(all(entity.get('death%d' % frame, 0) != 0 for frame in range(1, 5))
                   for entity in animated_enemies)
        assert sum(1 for entity in source.level.objects if entity['type'] >= DOOM.DOOM_ITEM_BASE) == 84
        assert sum(1 for sector in source.level.sectors
                   if sector['type'] == DOOM.DOOM_SECTOR_DAMAGE_5) == 4
        door_walls = [wall for wall in source.level.walls if wall['type'] == 1]
        exit_walls = [wall for wall in source.level.walls if wall['type'] == 11]
        assert len(door_walls) == 8
        assert len(exit_walls) == 1
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
        assert len(os.listdir(texture_dir)) == 56
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
    # E1M2 is the second menu route and is generated by the same compact
    # pipeline. Keep its geometry, exit switch and all imported things locked
    # down so a future converter optimization cannot silently route to legacy
    # map data or omit its backpack/props.
    doom_map = DOOM.load_map(wad, 'E1M2')
    assert (len(doom_map.vertices), len(doom_map.linedefs), len(doom_map.sidedefs),
            len(doom_map.sectors), len(doom_map.things)) == (942, 1033, 1323, 200, 262)
    with tempfile.TemporaryDirectory() as directory:
        report = DOOM.convert_map(wad, doom_map, directory, extract_sprites='none')
        assert (report['doors'], report['exits'], report['enemies'], report['world_items']) \
                == (19, 1, 80, 148)
        assert (report['wall_textures'], report['flats']) == (59, 29)
        assert (report['enemy_sprite_materials'], report['enemy_death_sprite_materials']) == (33, 12)
        assert (report['animated_walls'], report['animated_flats'], report['damage_sectors']) == (1, 1, 5)
        # E1M2 gets a conservative all-visible PVS because Doom REJECT is not
        # a render PVS and was visibly culling the connected outdoor sectors.
        assert report['pvs_mode'] == 'all-visible'
        assert report['pvs_visible_pairs'] == 40000
        info = C3.read_c3b(os.path.join(directory, 'level.c3b'))
        assert (info['walls'], info['sectors'], info['objects']) == (1033, 200, 0)
        assert os.path.getsize(os.path.join(directory, 'level.c3b')) < 65536
        committed = os.path.join(ROOT, 'res', 'gamedata', 'custom', 'doom-e1m2')
        assert open(os.path.join(directory, 'level.c3b'), 'rb').read() == open(
            os.path.join(committed, 'level.c3b'), 'rb').read()
        assert open(os.path.join(directory, 'materials.c3m'), 'rb').read() == open(
            os.path.join(committed, 'materials.c3m'), 'rb').read()
        materials = TEXTURES.load_manifest(os.path.join(directory, 'materials.c3m'))
        assert len(materials) == 150
        assert len([key for key in materials if key.startswith('anim.wall.')]) == 1
        assert len([key for key in materials if key.startswith('anim.flat.')]) == 1
        source = C3.load_source(os.path.join(directory, 'level.c3d.json'))
        assert len(source.level.objects) == 232
        assert sum(1 for entity in source.level.objects if entity['type'] >= DOOM.DOOM_ITEM_BASE) == 148
        assert sum(1 for entity in source.level.objects if entity['type'] in (3001, 3003, 3004)) == 80
        assert sum(1 for entity in source.level.objects if entity['type'] == DOOM.DOOM_ITEM_BASE + 13) == 1
        assert sum(1 for sector in source.level.sectors
                   if sector['type'] == DOOM.DOOM_SECTOR_DAMAGE_5) == 5
        assert sum(1 for wall in source.level.walls if wall['type'] == 11) == 1
        assert sum(1 for wall in source.level.walls if wall['type'] == 28) == 2
    print('DOOM E1M1/E1M2: geometry, BMP4 materials, actors, exits and compact C3B: OK')


if __name__ == '__main__':
    main()
