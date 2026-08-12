#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Classic Doom WAD -> compact C3D2 package converter.

The converter intentionally targets the original binary Doom map format used
by E1M1. Classic Doom sectors are horizontal floor/ceiling planes, so no slopes
are required for this source format. It emits a C3D source, C3B, BMP4 material
set and player starts; the game never reads the WAD at runtime.

Only data needed to walk and fight in the map is put under
``res/gamedata/custom``: geometry, used wall textures, used flats, sky, player
starts and three compact Doom enemy billboard families. Remaining Doom things
are preserved in a small metadata INI for later gameplay conversion.
"""

import hashlib
import json
import os
import struct

import c3d2_core as C3
import c3d2_entities as ENTITIES
import c3d2_texture_tools as TEXTURES
import co3d_level_core as LEGACY
from png_to_bmp4 import write_bmp4 as write_transparent_bmp4


class DoomWadError(ValueError):
    pass


# These are only used by optional sprite extraction. The gameplay runtime does
# not yet consume custom Doom billboards, so the default compact conversion
# leaves sprites outside the JAR package.
# Classic Doom lines that are ordinary vertical doors in the first episode.
# Their closed sector is mapped to the existing CovertOps door controller.
DOOM_DOOR_SPECIALS = (1, 2, 3, 4, 16, 26, 27, 28, 31, 32, 33, 34)

# First playable mapping: use the existing enemy AI categories while drawing
# one genuine Doom billboard per monster family through sprite.<slot>.
# Native Doom patches describe original 56-ish unit monsters. The inherited
# Covert billboard projection is tuned for much larger textures, so normalize
# Doom actors to this physical art height during conversion instead of baking a
# per-frame scale cost into the Java ME renderer.
DOOM_RUNTIME_SPRITE_HEIGHT = 160

# First-person weapon patches are separate from world billboards. Keeping two
# neutral/firing frames per weapon is enough for the current compact Java ME
# weapon animation while a later Doom state machine can consume all frames.
DOOM_HUD_WEAPONS = (
    ('fist', 'PUNGA0', 'PUNGB0'),
    ('pistol', 'PISGA0', 'PISGB0'),
    ('shotgun', 'SHTGA0', 'SHTGB0'),
    ('chaingun', 'CHGGA0', 'CHGGB0'),
    ('rocket', 'MISGA0', 'MISGB0'),
    ('plasma', 'PLSGA0', 'PLSGB0'),
    ('bfg', 'BFGGA0', 'BFGGB0'),
    ('chainsaw', 'SAWGA0', 'SAWGB0'),
)

# World projectile sprites are kept separate from HUD patches. Slots begin
# after the three enemy billboard materials.
DOOM_PROJECTILES = (
    ('rocket', 'MISLA1'),
    ('plasma', 'PLSSA0'),
    ('bfg', 'BFUGA0'),
    ('imp_fireball', 'BAL1A0'),
)
DOOM_RUNTIME_PROJECTILE_HEIGHT = 32
DOOM_RUNTIME_ITEM_HEIGHT = 64
DOOM_ITEM_BASE = 9000

# Multiplayer starts have a Doom player sprite prefix but should not become
# world props in the single-player E1M1 import.
DOOM_NON_WORLD_THING_TYPES = (1, 2, 3, 4, 10, 11, 12, 13, 14, 15, 16)

DOOM_ENEMIES = {
    3001: dict(engine_type=3001, sprite='TROOA1', label='imp'),
    3004: dict(engine_type=3004, sprite='POSSA1', label='zombieman'),
    9: dict(engine_type=3003, sprite='SPOSA1', label='shotgun_guy'),
}

THING_SPRITES = {
    9: 'SPOS', 10: 'PLAY', 11: 'PLAY', 12: 'PLAY', 13: 'PLAY',
    14: 'PLAY', 15: 'PLAY', 16: 'PLAY', 17: 'CELP', 18: 'POSS',
    19: 'SPOS', 20: 'TROO', 21: 'SARG', 22: 'HEAD', 23: 'SKUL',
    24: 'POL5', 25: 'POL1', 26: 'POL6', 27: 'POL4', 28: 'POL2',
    29: 'POL3', 30: 'COL1', 31: 'COL2', 32: 'COL3', 33: 'COL4',
    34: 'CAND', 35: 'CBRA', 36: 'COL5', 37: 'COL6', 38: 'COLU',
    39: 'COLU', 40: 'COLU', 41: 'CEYE', 42: 'FSKU', 43: 'TBLU',
    44: 'TGRN', 45: 'TRE1', 46: 'TRE2', 47: 'ELEC', 48: 'ELEC',
    49: 'GOR1', 50: 'GOR2', 51: 'GOR3', 52: 'GOR4', 53: 'GOR5',
    54: 'TRE1', 55: 'TRE2', 56: 'TRE3', 57: 'TRE4', 58: 'SARG',
    59: 'HDB1', 60: 'HDB2', 61: 'POB1', 62: 'POB2', 63: 'POB3',
    64: 'POB4', 65: 'POB5', 66: 'POL1', 67: 'POL2', 68: 'POL3',
    69: 'POL4', 70: 'POL5', 71: 'POL6', 72: 'HDB3', 73: 'HDB4',
    74: 'HDB5', 75: 'HDB6', 76: 'POB1', 77: 'POB2', 78: 'POB3',
    79: 'POB4', 80: 'POB5', 81: 'POB6', 82: 'HDB1', 83: 'HDB2',
    84: 'HDB3', 85: 'HDB4', 86: 'HDB5', 87: 'HDB6', 88: 'BBRN',
    89: 'BRS1',
    2001: 'SHOT', 2002: 'MGUN', 2003: 'LAUN', 2004: 'PLAS',
    2005: 'CSAW', 2006: 'BFUG', 2007: 'CLIP', 2008: 'SHEL',
    2010: 'ROCK', 2011: 'STIM', 2012: 'MEDI', 2013: 'SOUL',
    2014: 'BON1', 2015: 'BON2', 2017: 'CEYE', 2018: 'ARM1',
    2019: 'ARM2', 2022: 'PINV', 2023: 'PSTR', 2024: 'PINS',
    2025: 'SUIT', 2026: 'PMAP', 2028: 'COLU', 2035: 'BAR1',
    2045: 'PVIS', 2046: 'BROK', 2047: 'CELL', 2048: 'AMMO',
    2049: 'SBOX', 3001: 'TROO', 3002: 'SARG', 3003: 'BOSS',
    3004: 'POSS', 3005: 'HEAD', 3006: 'SKUL',
}


class WadFile(object):
    def __init__(self, path):
        self.path = path
        with open(path, 'rb') as stream:
            self.data = stream.read()
        if len(self.data) < 12:
            raise DoomWadError('file is too short for a WAD header')
        magic, count, directory_offset = struct.unpack_from('<4sii', self.data, 0)
        if magic not in (b'IWAD', b'PWAD'):
            raise DoomWadError('not an IWAD/PWAD: %r' % magic)
        if count < 0 or directory_offset < 0 or directory_offset + count * 16 > len(self.data):
            raise DoomWadError('invalid WAD directory')
        self.magic = magic.decode('ascii')
        self.lumps = []
        self.by_name = {}
        for index in range(count):
            offset, size, raw_name = struct.unpack_from('<ii8s', self.data,
                                                        directory_offset + index * 16)
            if offset < 0 or size < 0 or offset + size > len(self.data):
                raise DoomWadError('invalid lump bounds at index %d' % index)
            name = _lump_name(raw_name)
            entry = dict(index=index, name=name, offset=offset, size=size)
            self.lumps.append(entry)
            self.by_name.setdefault(name, []).append(entry)

    def lump(self, name, occurrence=-1):
        entries = self.by_name.get(name.upper())
        if not entries:
            raise DoomWadError('missing lump: ' + name)
        entry = entries[occurrence]
        return self.data[entry['offset']:entry['offset'] + entry['size']]

    def lump_entry(self, name, occurrence=-1):
        entries = self.by_name.get(name.upper())
        if not entries:
            raise DoomWadError('missing lump: ' + name)
        return entries[occurrence]

    def namespace(self, start_name, end_name):
        start = self.lump_entry(start_name)['index']
        end = self.lump_entry(end_name)['index']
        if end <= start:
            raise DoomWadError('bad namespace %s..%s' % (start_name, end_name))
        return self.lumps[start + 1:end]

    def sha256(self):
        return hashlib.sha256(self.data).hexdigest()


class DoomMap(object):
    def __init__(self):
        self.name = ''
        self.vertices = []
        self.linedefs = []
        self.sidedefs = []
        self.sectors = []
        self.things = []
        # Classic Doom REJECT bit matrix: 1 means the sector pair cannot see
        # each other in the original map.
        self.reject = b''


def load_map(wad, map_name='E1M1'):
    marker = wad.lump_entry(map_name.upper())
    start = marker['index']
    expected = ('THINGS', 'LINEDEFS', 'SIDEDEFS', 'VERTEXES', 'SEGS',
                'SSECTORS', 'NODES', 'SECTORS', 'REJECT', 'BLOCKMAP')
    found = wad.lumps[start + 1:start + 1 + len(expected)]
    if len(found) != len(expected) or tuple(item['name'] for item in found) != expected:
        raise DoomWadError('%s is not a classic Doom map marker' % map_name)
    lumps = {}
    for item in found:
        lumps[item['name']] = wad.data[item['offset']:item['offset'] + item['size']]

    result = DoomMap()
    result.name = map_name.upper()
    result.vertices = [_vertex(record) for record in _records(lumps['VERTEXES'], 4)]
    result.linedefs = [_linedef(record) for record in _records(lumps['LINEDEFS'], 14)]
    result.sidedefs = [_sidedef(record) for record in _records(lumps['SIDEDEFS'], 30)]
    result.sectors = [_sector(record) for record in _records(lumps['SECTORS'], 26)]
    result.things = [_thing(record) for record in _records(lumps['THINGS'], 10)]
    result.reject = lumps['REJECT']

    expected_reject_bytes = (len(result.sectors) * len(result.sectors) + 7) >> 3
    if len(result.reject) < expected_reject_bytes:
        raise DoomWadError('REJECT matrix is shorter than sector count requires')

    for index, line in enumerate(result.linedefs):
        if line['start'] >= len(result.vertices) or line['end'] >= len(result.vertices):
            raise DoomWadError('linedef %d has invalid vertex' % index)
        for side in (line['right'], line['left']):
            if side >= len(result.sidedefs):
                raise DoomWadError('linedef %d has invalid sidedef' % index)
    for index, side in enumerate(result.sidedefs):
        if side['sector'] >= len(result.sectors):
            raise DoomWadError('sidedef %d has invalid sector' % index)
    return result


def parse_palette(wad):
    data = wad.lump('PLAYPAL')
    if len(data) < 768:
        raise DoomWadError('PLAYPAL is too short')
    return [tuple(data[index:index + 3]) for index in range(0, 768, 3)]


def parse_texture_definitions(wad):
    pnames = wad.lump('PNAMES')
    if len(pnames) < 4:
        raise DoomWadError('PNAMES is too short')
    count, = struct.unpack_from('<I', pnames, 0)
    if 4 + count * 8 > len(pnames):
        raise DoomWadError('PNAMES is truncated')
    patch_names = [_lump_name(pnames[4 + index * 8:12 + index * 8])
                   for index in range(count)]

    textures = {}
    for lump_name in ('TEXTURE1', 'TEXTURE2'):
        if lump_name not in wad.by_name:
            continue
        data = wad.lump(lump_name)
        if len(data) < 4:
            raise DoomWadError(lump_name + ' is too short')
        texture_count, = struct.unpack_from('<I', data, 0)
        if 4 + texture_count * 4 > len(data):
            raise DoomWadError(lump_name + ' offsets are truncated')
        for index in range(texture_count):
            offset, = struct.unpack_from('<I', data, 4 + index * 4)
            if offset + 22 > len(data):
                raise DoomWadError('bad texture offset in ' + lump_name)
            name = _lump_name(data[offset:offset + 8])
            width, height = struct.unpack_from('<hh', data, offset + 12)
            patch_count, = struct.unpack_from('<h', data, offset + 20)
            if width <= 0 or height <= 0 or patch_count < 0:
                raise DoomWadError('bad texture dimensions: ' + name)
            end = offset + 22 + patch_count * 10
            if end > len(data):
                raise DoomWadError('truncated texture patches: ' + name)
            patches = []
            for patch_index in range(patch_count):
                px, py, patch_number, step_dir, color_map = struct.unpack_from(
                    '<hhHhh', data, offset + 22 + patch_index * 10)
                if patch_number >= len(patch_names):
                    raise DoomWadError('texture %s has invalid patch index' % name)
                patches.append(dict(x=px, y=py, name=patch_names[patch_number],
                                    step=step_dir, color_map=color_map))
            textures[name] = dict(name=name, width=width, height=height, patches=patches)
    return textures


def decode_patch(data, palette):
    if len(data) < 8:
        raise DoomWadError('patch is too short')
    width, height, left_offset, top_offset = struct.unpack_from('<HHhh', data, 0)
    if width <= 0 or height <= 0 or 8 + width * 4 > len(data):
        raise DoomWadError('invalid patch header')
    pixels = [(0, 0, 0, 0)] * (width * height)
    for x in range(width):
        offset, = struct.unpack_from('<I', data, 8 + x * 4)
        if offset >= len(data):
            raise DoomWadError('patch column outside lump')
        cursor = offset
        while True:
            if cursor >= len(data):
                raise DoomWadError('unterminated patch column')
            top_delta = data[cursor]
            cursor += 1
            if top_delta == 255:
                break
            if cursor + 2 > len(data):
                raise DoomWadError('truncated patch post')
            length = data[cursor]
            cursor += 2  # length + unused byte
            if cursor + length + 1 > len(data):
                raise DoomWadError('truncated patch pixels')
            for y in range(length):
                output_y = top_delta + y
                if 0 <= output_y < height:
                    red, green, blue = palette[data[cursor + y]]
                    pixels[output_y * width + x] = (red, green, blue, 255)
            cursor += length + 1  # pixels + trailing unused byte
    return width, height, left_offset, top_offset, pixels


def render_texture(wad, texture, palette):
    pixels = [(0, 0, 0, 255)] * (texture['width'] * texture['height'])
    for patch_info in texture['patches']:
        patch = wad.lump(patch_info['name'])
        width, height, _left, _top, patch_pixels = decode_patch(patch, palette)
        for y in range(height):
            target_y = patch_info['y'] + y
            if target_y < 0 or target_y >= texture['height']:
                continue
            source_offset = y * width
            target_offset = target_y * texture['width']
            for x in range(width):
                target_x = patch_info['x'] + x
                if target_x < 0 or target_x >= texture['width']:
                    continue
                color = patch_pixels[source_offset + x]
                if color[3]:
                    pixels[target_offset + target_x] = color
    return texture['width'], texture['height'], pixels


def decode_flat(wad, name, palette):
    data = wad.lump(name)
    if len(data) != 4096:
        raise DoomWadError('flat %s is not 64x64' % name)
    return 64, 64, [(palette[index][0], palette[index][1], palette[index][2], 255)
                    for index in data]


def convert_map(wad, doom_map, package_dir, height_scale=0.5, world_scale=1.0,
                minimum_clearance=64, extract_sprites='none', pvs_mode='doom-reject'):
    """Converts a parsed classic Doom map into a complete compact C3D package."""
    if height_scale <= 0 or world_scale <= 0:
        raise DoomWadError('height/world scale must be positive')
    if minimum_clearance < 50:
        raise DoomWadError('minimum clearance must be at least 50 for this engine')
    if pvs_mode not in ('doom-reject', 'all-visible'):
        raise DoomWadError('pvs_mode must be doom-reject or all-visible')

    palette = parse_palette(wad)
    texture_defs = parse_texture_definitions(wad)
    wall_names = _used_wall_texture_names(doom_map)
    flat_names = _used_flat_names(doom_map)
    # A door sector is initially closed in Doom (ceiling == floor). Keep that
    # state instead of applying the generic walkability clearance, then map the
    # trigger line to GameEngine's existing type-1 door controller.
    door_targets = _find_closed_door_targets(doom_map)
    sky_names = set(name for name in flat_names if name == 'F_SKY1')
    flat_names -= sky_names
    if not wall_names:
        raise DoomWadError('map has no wall textures')
    if len(wall_names) > 127 or len(flat_names) > 127:
        raise DoomWadError('map exceeds C3D 127 material slots')

    if not os.path.isdir(package_dir):
        os.makedirs(package_dir)
    texture_dir = os.path.join(package_dir, 'textures')
    if not os.path.isdir(texture_dir):
        os.makedirs(texture_dir)

    wall_slots = dict((name, index + 1) for index, name in enumerate(sorted(wall_names)))
    flat_slots = dict((name, index + 1) for index, name in enumerate(sorted(flat_names)))
    fallback_wall = sorted(wall_names)[0]

    report = dict(map=doom_map.name, wad_sha256=wad.sha256(),
                  vertices=len(doom_map.vertices), linedefs=len(doom_map.linedefs),
                  sidedefs=len(doom_map.sidedefs), sectors=len(doom_map.sectors),
                  things=len(doom_map.things), wall_textures=len(wall_slots),
                  flats=len(flat_slots), doors=len(door_targets), enemies=0,
                  height_scale=height_scale,
                  world_scale=world_scale, minimum_clearance=minimum_clearance,
                  missing_wall_textures=[], sprites=0)

    manifest_lines = [
        '# Generated by scripts/convert_doom_e1m1.py from %s.' % doom_map.name,
        '# Indexed BMP4 only; source WAD is never read by the Java ME runtime.',
    ]
    material_lines = ['[doom_materials]', 'format=DOOM-C3D-MATERIALS-1']

    for name in sorted(wall_slots):
        slot = wall_slots[name]
        filename = 'wall_%03d_%s.bmp' % (slot, _safe_name(name))
        destination = os.path.join(texture_dir, filename)
        try:
            texture = texture_defs[name]
            width, height, pixels = render_texture(wad, texture, palette)
        except Exception as error:
            width, height, pixels = 64, 128, _placeholder_rgba(64, 128)
            report['missing_wall_textures'].append('%s: %s' % (name, error))
        target_width, target_height = _wall_target(width, height)
        _write_world_bmp(destination, pixels, width, height, target_width, target_height)
        relative = 'textures/' + filename
        manifest_lines.append('wall.%d=%s' % (slot, relative))
        material_lines.append('wall.%s=%d' % (name, slot))

    for name in sorted(flat_slots):
        slot = flat_slots[name]
        filename = 'flat_%03d_%s.bmp' % (slot, _safe_name(name))
        destination = os.path.join(texture_dir, filename)
        width, height, pixels = decode_flat(wad, name, palette)
        _write_world_bmp(destination, pixels, width, height, 64, 64)
        relative = 'textures/' + filename
        manifest_lines.append('flat.%d=%s' % (slot, relative))
        material_lines.append('flat.%s=%d' % (name, slot))

    sky_texture = texture_defs.get('SKY1')
    sky_destination = os.path.join(texture_dir, 'sky.bmp')
    if sky_texture is not None:
        sky_width, sky_height, sky_pixels = render_texture(wad, sky_texture, palette)
    else:
        sky_width, sky_height, sky_pixels = 64, 128, _placeholder_rgba(64, 128, sky=True)
        report['missing_wall_textures'].append('SKY1: texture not found')
    _write_world_bmp(sky_destination, sky_pixels, sky_width, sky_height, 64, 128)
    manifest_lines.append('sky=textures/sky.bmp')
    material_lines.append('sky=SKY1')

    enemy_sprite_slots = _export_runtime_enemy_sprites(wad, palette, package_dir)
    for doom_type, slot in enemy_sprite_slots.items():
        info = DOOM_ENEMIES[doom_type]
        filename = 'sprites/doom/%02d_%s.bmp' % (slot, _safe_name(info['sprite']))
        manifest_lines.append('sprite.%d=%s' % (slot, filename))
        material_lines.append('sprite.%s=%d' % (info['sprite'], slot))
    projectile_sprite_slots = _export_runtime_projectile_sprites(wad, palette, package_dir,
                                                                   len(enemy_sprite_slots) + 1)
    for name, slot in projectile_sprite_slots.items():
        filename = 'sprites/doom/%02d_%s.bmp' % (slot, name)
        manifest_lines.append('sprite.%d=%s' % (slot, filename))
        material_lines.append('projectile.%s=%d' % (name, slot))
    thing_sprite_slots = _export_runtime_thing_sprites(
            wad, doom_map, palette, package_dir,
            len(enemy_sprite_slots) + len(projectile_sprite_slots) + 1)
    for thing_type, sprite_info in thing_sprite_slots.items():
        slot = sprite_info['slot']
        filename = 'sprites/doom/%02d_%s.bmp' % (slot, sprite_info['name'])
        manifest_lines.append('sprite.%d=%s' % (slot, filename))
        material_lines.append('thing.%d=%d' % (thing_type, slot))
    hud_weapon_count = _export_hud_weapon_sprites(wad, palette, package_dir)
    report['enemies'] = sum(1 for thing in doom_map.things if thing['type'] in enemy_sprite_slots)
    report['enemy_sprite_materials'] = len(enemy_sprite_slots)
    report['enemy_sprite_height'] = DOOM_RUNTIME_SPRITE_HEIGHT
    report['projectile_sprite_materials'] = len(projectile_sprite_slots)
    report['projectile_sprite_height'] = DOOM_RUNTIME_PROJECTILE_HEIGHT
    report['thing_sprite_materials'] = len(thing_sprite_slots)
    report['world_items'] = sum(1 for thing in doom_map.things
                                if thing['type'] in thing_sprite_slots)
    report['hud_weapon_frames'] = hud_weapon_count

    _write_text(os.path.join(package_dir, 'materials.c3m'), '\n'.join(manifest_lines) + '\n')
    _write_text(os.path.join(package_dir, 'doom_materials.ini'), '\n'.join(material_lines) + '\n')

    document, doom_things = _build_c3d_document(doom_map, wall_slots, flat_slots,
                                                 fallback_wall, height_scale, world_scale,
                                                 minimum_clearance, door_targets,
                                                 enemy_sprite_slots, thing_sprite_slots)
    document.materials = 'materials.c3m'
    document.entities = 'entities.ini'
    C3.dump_source(document, os.path.join(package_dir, 'level.c3d.json'))
    ENTITIES.dump_entities(document.level.objects, os.path.join(package_dir, 'entities.ini'))
    _write_doom_things(os.path.join(package_dir, 'doom_things.ini'), doom_things)

    if extract_sprites not in ('none', 'used', 'all'):
        raise DoomWadError('extract_sprites must be none, used or all')
    if extract_sprites != 'none':
        report['sprites'] = export_sprites(wad, doom_map, package_dir, palette,
                                           extract_sprites)

    c3b_path, bsp_report = C3.compile_source(os.path.join(package_dir, 'level.c3d.json'),
                                              os.path.join(package_dir, 'level.c3b'))
    if pvs_mode == 'doom-reject':
        visible_pairs = _install_doom_reject_pvs(c3b_path, doom_map)
    else:
        visible_pairs = len(doom_map.sectors) * len(doom_map.sectors)
    c3b_info = C3.read_c3b(c3b_path)
    report['c3b'] = os.path.basename(c3b_path)
    report['bsp_nodes'] = c3b_info['nodes']
    report['bsp_leaves'] = c3b_info['leaves']
    report['bsp_segments'] = c3b_info['segments']
    report['bsp_splits'] = bsp_report.splits
    report['bsp_failures'] = len(bsp_report.fail_samples)
    report['pvs_mode'] = pvs_mode
    report['pvs_visible_pairs'] = visible_pairs
    _write_text(os.path.join(package_dir, 'doom_conversion.json'),
                json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    return report


def _install_doom_reject_pvs(c3b_path, doom_map):
    """Installs a conservative symmetric version of Doom's REJECT matrix.

    The C3D renderer benefits from not traversing every outdoor sky sector in
    E1M1. A raw REJECT bit can be directional or stale around a moving door, so
    a pair is rejected only when *both* Doom directions reject it. Direct
    portal neighbors are always retained; doors stay visible/openable.
    """
    data = bytearray(open(c3b_path, 'rb').read())
    header_size = struct.calcsize('<4sBBh8H')
    magic, version, flags, root, nv, nw, no, nsf, nsec, nn, nl, nsg = \
            struct.unpack_from('<4sBBh8H', data, 0)
    if magic != C3.MAGIC or version != C3.VERSION or nsec != len(doom_map.sectors):
        raise DoomWadError('C3B layout does not match converted Doom map')

    offset = header_size
    material_length, = struct.unpack_from('<H', data, offset)
    offset += 2 + material_length
    if flags & C3.HEADER_FLAG_EXTERNAL_ENTITIES:
        entity_length, = struct.unpack_from('<H', data, offset)
        offset += 2 + entity_length

    offset += nv * 4
    offset += nw * 12
    offset += no * 10
    offset += nsf * 10
    offset += nsec * 12
    offset += nn * 12
    offset += nl * 6
    offset += nsg * 9
    pvs_bytes, = struct.unpack_from('<I', data, offset)
    offset += 4
    expected_bytes = (nsec * nsec + 7) >> 3
    if pvs_bytes != expected_bytes or offset + pvs_bytes != len(data):
        raise DoomWadError('unexpected C3B PVS layout')

    neighbors = [set() for unused in range(nsec)]
    for line in doom_map.linedefs:
        if line['right'] < 0 or line['left'] < 0:
            continue
        first = doom_map.sidedefs[line['right']]['sector']
        second = doom_map.sidedefs[line['left']]['sector']
        if first != second:
            neighbors[first].add(second)
            neighbors[second].add(first)

    pvs = bytearray(pvs_bytes)
    visible_pairs = 0
    for source in range(nsec):
        for target in range(nsec):
            visible = source == target or target in neighbors[source]
            if not visible:
                visible = not (_doom_reject_hidden(doom_map.reject, nsec, source, target)
                               and _doom_reject_hidden(doom_map.reject, nsec, target, source))
            if visible:
                bit = source * nsec + target
                pvs[bit >> 3] |= 1 << (bit & 7)
                visible_pairs += 1
    data[offset:offset + pvs_bytes] = pvs
    with open(c3b_path, 'wb') as stream:
        stream.write(data)
    return visible_pairs


def _doom_reject_hidden(reject, sector_count, source, target):
    bit = source * sector_count + target
    return (reject[bit >> 3] & (1 << (bit & 7))) != 0


def _find_closed_door_targets(doom_map):
    """Returns classic door trigger line index -> closed Doom sector ID.

    Doom E1M1's normal DR/D1 door lines have the room on the right side and a
    zero-height door sector on the left. C3D mirrors Y into Z and reverses the
    line, so that left sector becomes the C3D back side expected by GameEngine
    type 1 doors. Lines with a non-closed/ambiguous neighbor stay ordinary
    portals rather than guessing a destructive door mapping.
    """
    targets = {}
    for line_index, line in enumerate(doom_map.linedefs):
        if line['special'] not in DOOM_DOOR_SPECIALS or line['left'] < 0:
            continue
        left_sector = doom_map.sidedefs[line['left']]['sector']
        sector = doom_map.sectors[left_sector]
        if sector['ceiling'] <= sector['floor']:
            targets[line_index] = left_sector
    return targets


def _export_runtime_enemy_sprites(wad, palette, package_dir):
    """Exports one compact transparent billboard for each E1M1 enemy family."""
    slots = {}
    sprite_dir = os.path.join(package_dir, 'sprites', 'doom')
    if not os.path.isdir(sprite_dir):
        os.makedirs(sprite_dir)
    slot = 1
    for doom_type in sorted(DOOM_ENEMIES):
        info = DOOM_ENEMIES[doom_type]
        try:
            data = wad.lump(info['sprite'])
            width, height, _left, _top, pixels = decode_patch(data, palette)
        except Exception as error:
            raise DoomWadError('enemy sprite %s: %s' % (info['sprite'], error))
        filename = '%02d_%s.bmp' % (slot, _safe_name(info['sprite']))
        _write_sprite_bmp(os.path.join(sprite_dir, filename), width, height, pixels,
                          DOOM_RUNTIME_SPRITE_HEIGHT)
        slots[doom_type] = slot
        slot += 1
    return slots


def _export_runtime_projectile_sprites(wad, palette, package_dir, first_slot):
    """Exports visible Doom rocket, plasma and BFG projectiles for C3B."""
    slots = {}
    sprite_dir = os.path.join(package_dir, 'sprites', 'doom')
    if not os.path.isdir(sprite_dir):
        os.makedirs(sprite_dir)
    slot = first_slot
    for name, lump in DOOM_PROJECTILES:
        try:
            width, height, _left, _top, pixels = decode_patch(wad.lump(lump), palette)
        except Exception as error:
            raise DoomWadError('projectile sprite %s: %s' % (lump, error))
        _write_sprite_bmp(os.path.join(sprite_dir, '%02d_%s.bmp' % (slot, name)),
                          width, height, pixels, DOOM_RUNTIME_PROJECTILE_HEIGHT)
        slots[name] = slot
        slot += 1
    return slots


def _export_runtime_thing_sprites(wad, doom_map, palette, package_dir, first_slot):
    """Exports one billboard for every visible pickup, barrel and decoration in E1M1."""
    entries = wad.namespace('S_START', 'S_END')
    names = [entry['name'] for entry in entries]
    sprite_dir = os.path.join(package_dir, 'sprites', 'doom')
    if not os.path.isdir(sprite_dir):
        os.makedirs(sprite_dir)

    prefix_slots = {}
    result = {}
    next_slot = first_slot
    for thing_type in sorted(set(thing['type'] for thing in doom_map.things)):
        if thing_type in DOOM_NON_WORLD_THING_TYPES or thing_type in DOOM_ENEMIES:
            continue
        prefix = THING_SPRITES.get(thing_type)
        if not prefix:
            continue
        sprite_lump = _first_sprite_lump(names, prefix)
        if sprite_lump is None:
            continue
        if prefix not in prefix_slots:
            try:
                width, height, _left, _top, pixels = decode_patch(wad.lump(sprite_lump), palette)
            except Exception:
                continue
            slot = next_slot
            next_slot += 1
            filename = '%02d_%s.bmp' % (slot, _safe_name(prefix))
            _write_sprite_bmp(os.path.join(sprite_dir, filename), width, height, pixels,
                              DOOM_RUNTIME_ITEM_HEIGHT, 128)
            prefix_slots[prefix] = dict(slot=slot, name=_safe_name(prefix))
        result[thing_type] = prefix_slots[prefix]
    return result


def _first_sprite_lump(names, prefix):
    for suffix in ('A0', 'A1', 'B0'):
        candidate = prefix + suffix
        if candidate in names:
            return candidate
    for name in names:
        if name.startswith(prefix):
            return name
    return None


def _export_hud_weapon_sprites(wad, palette, package_dir):
    """Exports the two primary first-person frames for all Doom weapons."""
    hud_dir = os.path.join(package_dir, 'hud')
    if not os.path.isdir(hud_dir):
        os.makedirs(hud_dir)
    count = 0
    for weapon, idle_lump, fire_lump in DOOM_HUD_WEAPONS:
        for suffix, lump in (('a', idle_lump), ('b', fire_lump)):
            try:
                width, height, _left, _top, pixels = decode_patch(wad.lump(lump), palette)
            except Exception as error:
                raise DoomWadError('HUD weapon patch %s: %s' % (lump, error))
            _write_sprite_bmp(os.path.join(hud_dir, weapon + '_' + suffix + '.bmp'),
                              width, height, pixels)
            count += 1
    return count


def export_sprites(wad, doom_map, package_dir, palette, mode='used'):
    """Optional compact Doom patch sprite export for future custom billboards."""
    try:
        entries = wad.namespace('S_START', 'S_END')
    except DoomWadError:
        # Some WADs use the extended marker names.
        entries = wad.namespace('SS_START', 'SS_END')
    wanted_prefixes = None
    if mode == 'used':
        wanted_prefixes = set()
        for thing in doom_map.things:
            prefix = THING_SPRITES.get(thing['type'])
            if prefix:
                wanted_prefixes.add(prefix)
    sprite_dir = os.path.join(package_dir, 'sprites', 'doom')
    if not os.path.isdir(sprite_dir):
        os.makedirs(sprite_dir)
    output_lines = ['[doom_sprites]', 'format=DOOM-C3D-SPRITES-1']
    count = 0
    for entry in entries:
        name = entry['name']
        if entry['size'] < 8:
            continue
        if wanted_prefixes is not None and name[:4] not in wanted_prefixes:
            continue
        try:
            width, height, _left, _top, pixels = decode_patch(
                wad.data[entry['offset']:entry['offset'] + entry['size']], palette)
        except Exception:
            continue
        destination_name = _safe_name(name) + '.bmp'
        destination = os.path.join(sprite_dir, destination_name)
        _write_sprite_bmp(destination, width, height, pixels)
        output_lines.append('%s=sprites/doom/%s' % (name, destination_name))
        count += 1
    _write_text(os.path.join(sprite_dir, 'sprites.ini'), '\n'.join(output_lines) + '\n')
    return count


def _build_c3d_document(doom_map, wall_slots, flat_slots, fallback_wall,
                        height_scale, world_scale, minimum_clearance, door_targets,
                        enemy_sprite_slots, thing_sprite_slots):
    level = LEGACY.Level()
    # Keep Doom's x/y plane directly as C3D x/z. Classic Doom front/right
    # sidedefs then retain their original winding, so the imported E1M1 is not
    # mirrored and its player starts match the source automap.
    level.vertices = [(int(round(x * world_scale)), int(round(y * world_scale)))
                      for x, y in doom_map.vertices]

    closed_door_sectors = set(door_targets.values())
    for sector_index, raw in enumerate(doom_map.sectors):
        floor = int(round(raw['floor'] * height_scale))
        ceiling = int(round(raw['ceiling'] * height_scale))
        if sector_index not in closed_door_sectors and ceiling < floor + minimum_clearance:
            ceiling = floor + minimum_clearance
        elif sector_index in closed_door_sectors:
            # Closed vertical door: GameEngine opens this ceiling on use.
            ceiling = floor
        floor_texture = 51 if raw['floor_texture'] == 'F_SKY1' else flat_slots.get(raw['floor_texture'], 0)
        ceiling_texture = 51 if raw['ceiling_texture'] == 'F_SKY1' else flat_slots.get(raw['ceiling_texture'], 0)
        light = _clamp((raw['light'] + 8) // 17, 0, 15)
        # Doom specials/tags drive Doom doors, lifts and scripts. They are
        # deliberately not copied into CovertOps game logic; converted portals
        # are static/open so the player can explore E1M1 immediately.
        level.sectors.append(dict(floor=floor, ceil=ceiling,
                                  floor_tex=floor_texture, ceil_tex=ceiling_texture,
                                  light_packed=light << 4, tag=0, type=0))

    surface_by_side = {}

    def convert_side(index):
        if index < 0:
            return -1
        cached = surface_by_side.get(index)
        if cached is not None:
            return cached
        raw = doom_map.sidedefs[index]
        main = wall_slots.get(raw['middle'], wall_slots[fallback_wall])
        upper = wall_slots.get(raw['upper'], main)
        lower = wall_slots.get(raw['lower'], main)
        surface_by_side[index] = len(level.surfaces)
        level.surfaces.append(dict(ox=int(round(raw['x_offset'] * world_scale)),
                                   oy=int(round(raw['y_offset'] * height_scale)),
                                   upper=upper, lower=lower, main=main,
                                   sector=raw['sector']))
        return surface_by_side[index]

    for line_index, raw in enumerate(doom_map.linedefs):
        right = raw['right']
        left = raw['left']
        if right >= 0:
            # Doom's right sidedef is exactly C3D's front/right side when
            # vertices use x,y -> x,z without a reflection.
            start = raw['start']
            end = raw['end']
            front = convert_side(right)
            back = convert_side(left)
        elif left >= 0:
            # Rare one-sided line with only a Doom left sidedef: reverse it so
            # that this side becomes the required C3D front/right side.
            start = raw['end']
            end = raw['start']
            front = convert_side(left)
            back = -1
        else:
            continue
        door_sector = door_targets.get(line_index)
        is_door = door_sector is not None and back >= 0 \
                and level.surfaces[back]['sector'] == door_sector
        level.walls.append(dict(sv=start, ev=end, flags=8 if is_door else (1 if back < 0 else 0),
                                type=1 if is_door else 0, special=0, front=front, back=back))

    entities = []
    doom_things = []
    for raw in doom_map.things:
        converted_x = int(round(raw['x'] * world_scale))
        converted_z = int(round(raw['y'] * world_scale))
        # Engine rotation turns toward -Z, while Doom angles turn toward +Y.
        # With direct x/y -> x/z coordinates the loader's 90-raw transform
        # needs this compensated raw value to preserve the original heading.
        converted_angle = _normalize_angle(90 + raw['angle'])
        doom_things.append(dict(x=converted_x, z=converted_z, doom_angle=raw['angle'],
                                type=raw['type'], flags=raw['flags'],
                                sprite=THING_SPRITES.get(raw['type'], '')))
        if 1 <= raw['type'] <= 4:
            entities.append(dict(x=converted_x, z=converted_z,
                                 angle=converted_angle, type=raw['type'], param=0))
        elif raw['type'] in enemy_sprite_slots:
            enemy = DOOM_ENEMIES[raw['type']]
            entities.append(dict(x=converted_x, z=converted_z,
                                 angle=converted_angle,
                                 type=enemy['engine_type'], param=0,
                                 sprite=enemy_sprite_slots[raw['type']]))
        elif raw['type'] in thing_sprite_slots:
            entities.append(dict(x=converted_x, z=converted_z,
                                 angle=converted_angle,
                                 type=DOOM_ITEM_BASE + raw['type'], param=0,
                                 sprite=thing_sprite_slots[raw['type']]['slot']))
    if not entities:
        raise DoomWadError('map has no Doom player starts (things 1..4)')
    level.objects = entities
    level.pvs = [bytearray(len(level.sectors)) for _index in level.sectors]
    return C3.C3DDocument(level, materials='materials.c3m', entities='entities.ini'), doom_things


def _write_world_bmp(path, pixels, source_width, source_height, target_width, target_height):
    resized = TEXTURES.resize_rgba(pixels, source_width, source_height,
                                   target_width, target_height, fit=False)
    palette, indices = TEXTURES.kmeans_quantize(TEXTURES.rgba_to_rgb(resized), 16, 24)
    TEXTURES.write_bmp4(path, target_width, target_height, palette, indices)


def _write_sprite_bmp(path, width, height, pixels, target_height=0, max_width=0):
    target_width = width
    if target_height > 0:
        target_width = max(1, (width * target_height + (height >> 1)) // height)
    else:
        target_height = height
    if max_width > 0 and target_width > max_width:
        target_width = max_width
        target_height = max(1, (height * target_width + (width >> 1)) // width)
    if width != target_width or height != target_height:
        pixels = TEXTURES.resize_rgba(pixels, width, height, target_width, target_height,
                                      fit=False)
        width = target_width
        height = target_height
    opaque = [(red, green, blue) for red, green, blue, alpha in pixels if alpha >= 128]
    if not opaque:
        opaque = [(0, 0, 0)]
    palette, _unused = TEXTURES.kmeans_quantize(opaque, 15, 20)
    indices = []
    for red, green, blue, alpha in pixels:
        if alpha < 128:
            indices.append(0)
        else:
            indices.append(_nearest_palette((red, green, blue), palette) + 1)
    write_transparent_bmp4(path, width, height, palette, indices)


def _used_wall_texture_names(doom_map):
    names = set()
    for side in doom_map.sidedefs:
        for key in ('upper', 'lower', 'middle'):
            name = side[key]
            if name and name != '-':
                names.add(name)
    return names


def _used_flat_names(doom_map):
    names = set()
    for sector in doom_map.sectors:
        names.add(sector['floor_texture'])
        names.add(sector['ceiling_texture'])
    return names


def _wall_target(width, height):
    target_width = 2
    while target_width < width:
        target_width <<= 1
    if height <= 16:
        target_height = 16
    elif height <= 64:
        target_height = 64
    else:
        target_height = 128
    return target_width, target_height


def _placeholder_rgba(width, height, sky=False):
    pixels = []
    for y in range(height):
        for x in range(width):
            if sky:
                pixels.append((30 + (y * 100 // max(1, height)),
                               65 + (y * 100 // max(1, height)), 120 + (y * 90 // max(1, height)), 255))
            else:
                shade = 70 if ((x >> 4) ^ (y >> 4)) & 1 else 130
                pixels.append((shade, 0, shade, 255))
    return pixels


def _write_doom_things(path, things):
    lines = ['# Source metadata; C3D runtime entities.ini contains player starts only.',
             '[doom_things]', 'format=DOOM-C3D-THINGS-1']
    for index, thing in enumerate(things):
        lines.extend(('', '[thing.%d]' % index,
                      'x=%d' % thing['x'], 'z=%d' % thing['z'],
                      'angle=%d' % thing['doom_angle'], 'type=%d' % thing['type'],
                      'flags=%d' % thing['flags']))
        if thing['sprite']:
            lines.append('sprite=%s' % thing['sprite'])
    _write_text(path, '\n'.join(lines) + '\n')


def _records(data, size):
    if len(data) % size:
        raise DoomWadError('lump length %d is not divisible by record size %d' %
                           (len(data), size))
    return [data[offset:offset + size] for offset in range(0, len(data), size)]


def _vertex(data):
    return struct.unpack_from('<hh', data, 0)


def _linedef(data):
    start, end, flags, special, tag, right, left = struct.unpack_from('<HHHHHhh', data, 0)
    return dict(start=start, end=end, flags=flags, special=special, tag=tag,
                right=right, left=left)


def _sidedef(data):
    x_offset, y_offset, upper, lower, middle, sector = struct.unpack_from('<hh8s8s8sH', data, 0)
    return dict(x_offset=x_offset, y_offset=y_offset, upper=_lump_name(upper),
                lower=_lump_name(lower), middle=_lump_name(middle), sector=sector)


def _sector(data):
    floor, ceiling, floor_texture, ceiling_texture, light, special, tag = struct.unpack_from(
        '<hh8s8shhh', data, 0)
    return dict(floor=floor, ceiling=ceiling, floor_texture=_lump_name(floor_texture),
                ceiling_texture=_lump_name(ceiling_texture), light=light,
                special=special, tag=tag)


def _thing(data):
    x, y, angle, thing_type, flags = struct.unpack_from('<hhHHH', data, 0)
    return dict(x=x, y=y, angle=angle, type=thing_type, flags=flags)


def _lump_name(data):
    return data.split(b'\0', 1)[0].decode('ascii', 'replace').strip().upper()


def _safe_name(name):
    output = []
    for char in name.lower():
        output.append(char if char.isalnum() or char in ('_', '-') else '_')
    return ''.join(output) or 'unnamed'


def _normalize_angle(angle):
    angle = int(round(angle)) % 360
    return angle


def _nearest_palette(color, palette):
    best = 0
    best_distance = _distance2(color, palette[0])
    for index in range(1, len(palette)):
        distance = _distance2(color, palette[index])
        if distance < best_distance:
            best = index
            best_distance = distance
    return best


def _distance2(first, second):
    red = first[0] - second[0]
    green = first[1] - second[1]
    blue = first[2] - second[2]
    return red * red + green * green + blue * blue


def _clamp(value, lower, upper):
    return lower if value < lower else upper if value > upper else value


def _write_text(path, content):
    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    with open(path, 'w', encoding='utf-8') as stream:
        stream.write(content)
