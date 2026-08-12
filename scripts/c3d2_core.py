#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""C3D2 custom-level source and C3B runtime compiler.

The editor-facing C3D source is JSON and intentionally contains authored
geometry/material references; editable entity placement lives in a companion
UTF-8 INI. BSP, leaves, segments and natural PVS are compiled deterministically
into C3B with the pure-Python Doom-style builder
from co3d_level_core. No shapely dependency is required.

Usage:
    python3 scripts/c3d2_core.py new res/gamedata/custom/demo
    python3 scripts/c3d2_core.py compile level.c3d.json [level.c3b]
    python3 scripts/c3d2_core.py inspect level.c3b
"""

import copy
import json
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import co3d_level_core as LEGACY
import c3d2_entities as ENTITIES

MAGIC = b'C3B1'
VERSION = 1
SOURCE_FORMAT = 'C3D2-SOURCE-1'

# C3B header flags.  The v1 header reserved this byte specifically so a
# package can evolve without making geometry depend on editor-only data.
HEADER_FLAG_EXTERNAL_ENTITIES = 1

# Per-sector flags, stored in the sector record rather than in the header.
SECTOR_FLAG_CEILING_SKY = 1
SECTOR_FLAG_FLOOR_SKY = 2


class C3DDocument(object):
    def __init__(self, level=None, materials='materials.c3m', entities=None):
        self.level = level if level is not None else LEGACY.Level()
        self.materials = materials
        # None means a pre-sidecar C3D source with inline legacy objects.
        # New documents always use a relative INI sidecar.
        self.entities = entities


def new_document():
    """Creates a small but fully valid authored C3D level."""
    lv = LEGACY.Level()
    lv.vertices = [(-128, -128), (128, -128), (128, 128), (-128, 128)]
    lv.sectors = [dict(floor=0, ceil=96, floor_tex=1, ceil_tex=51,
                       light_packed=8 << 4, tag=0, type=0)]
    # A front wall side is rendered only from the sector on its right.  The
    # room boundary must therefore be clockwise in the x,z plane (the stock
    # maps use the same convention).  Counter-clockwise walls are accepted by
    # the BSP builder, but are back-to-front after projection and render no
    # columns at all.
    clockwise_edges = ((0, 3), (3, 2), (2, 1), (1, 0))
    for i, edge in enumerate(clockwise_edges):
        lv.surfaces.append(dict(ox=0, oy=0, upper=1, lower=1, main=1, sector=0))
        lv.walls.append(dict(sv=edge[0], ev=edge[1], flags=1, type=0,
                             special=0, front=i, back=-1))
    # Entity placement deliberately stays outside geometry JSON/C3B.  Keep the
    # records in Level while editing so preview/BSP helpers can still use them,
    # then write them to entities.ini when creating or saving a package.
    lv.objects.append(dict(x=0, z=0, angle=0, type=1, param=0))
    lv.pvs = [bytearray(1)]
    return C3DDocument(lv, entities='entities.ini')


def load_source(path):
    with open(path, 'r', encoding='utf-8') as stream:
        source = json.load(stream)
    if source.get('format') != SOURCE_FORMAT:
        raise ValueError('неверный C3D source format')

    lv = LEGACY.Level()
    lv.vertices = [(_int_pair(v, 'vertices')[0], _int_pair(v, 'vertices')[1])
                   for v in source.get('vertices', [])]
    lv.walls = []
    for wall in source.get('walls', []):
        lv.walls.append(dict(
            sv=_int(wall, 'start'), ev=_int(wall, 'end'),
            flags=_int(wall, 'flags', 1), type=_int(wall, 'type', 0),
            special=_int(wall, 'special', 0), front=_int(wall, 'front'),
            back=_int(wall, 'back', -1)))

    lv.surfaces = []
    for surface in source.get('surfaces', []):
        offset = surface.get('offset', [0, 0])
        lv.surfaces.append(dict(
            ox=_int_pair(offset, 'surface offset')[0],
            oy=_int_pair(offset, 'surface offset')[1],
            upper=_slot(surface, 'upper', 0),
            lower=_slot(surface, 'lower', 0),
            main=_slot(surface, 'main', 0),
            sector=_int(surface, 'sector')))

    lv.sectors = []
    for sector in source.get('sectors', []):
        ceiling_sky = bool(sector.get('ceiling_sky', False))
        floor_sky = bool(sector.get('floor_sky', False))
        light = _int(sector, 'light', 8)
        if light < 0 or light > 15:
            raise ValueError('sector light должен быть 0..15')
        lv.sectors.append(dict(
            floor=_int(sector, 'floor_height', 0),
            ceil=_int(sector, 'ceiling_height', 96),
            floor_tex=51 if floor_sky else _slot(sector, 'floor_material', 0),
            ceil_tex=51 if ceiling_sky else _slot(sector, 'ceiling_material', 0),
            light_packed=light << 4,
            tag=_int(sector, 'tag', 0),
            type=_int(sector, 'type', 0)))

    # New C3D sources keep entity coordinates in their own INI.  Retain the
    # old inline JSON object reader only as a migration path for early C3D1
    # experiments; its C3B output remains readable by the runtime.
    entity_path = source.get('entities')
    if entity_path is not None:
        if 'objects' in source:
            raise ValueError('C3D cannot contain both entities and inline objects')
        entity_path = _relative_path(entity_path, 'entities')
        lv.objects = ENTITIES.load_entities(os.path.join(os.path.dirname(path), entity_path))
    else:
        lv.objects = []
        for obj in source.get('objects', []):
            lv.objects.append(dict(x=_int(obj, 'x'), z=_int(obj, 'z'),
                                   angle=_int(obj, 'angle', 0),
                                   type=_int(obj, 'type'), param=_int(obj, 'param', 0)))

    _validate_authored_level(lv)
    return C3DDocument(lv, source.get('materials', 'materials.c3m'), entity_path)


def dump_source(document, path):
    lv = document.level
    sectors = []
    for sector in lv.sectors:
        sectors.append(dict(
            floor_height=sector['floor'],
            ceiling_height=sector['ceil'],
            floor_material=0 if sector['floor_tex'] == 51 else sector['floor_tex'],
            ceiling_material=0 if sector['ceil_tex'] == 51 else sector['ceil_tex'],
            floor_sky=sector['floor_tex'] == 51,
            ceiling_sky=sector['ceil_tex'] == 51,
            light=(sector['light_packed'] >> 4) & 15,
            tag=sector['tag'], type=sector['type']))

    source = dict(
        format=SOURCE_FORMAT,
        materials=document.materials,
        vertices=[[x, z] for (x, z) in lv.vertices],
        walls=[dict(start=w['sv'], end=w['ev'], front=w['front'], back=w['back'],
                    flags=w['flags'], type=w['type'], special=w['special'])
               for w in lv.walls],
        surfaces=[dict(offset=[s['ox'], s['oy']], upper=s['upper'], lower=s['lower'],
                       main=s['main'], sector=s['sector']) for s in lv.surfaces],
        sectors=sectors)
    if document.entities is None:
        # Compatibility for an early source that has not yet been migrated.
        source['objects'] = [dict(x=o['x'], z=o['z'], angle=o['angle'], type=o['type'],
                                  param=o['param']) for o in lv.objects]
    else:
        source['entities'] = _relative_path(document.entities, 'entities')
    with open(path, 'w', encoding='utf-8') as stream:
        json.dump(source, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.write('\n')


def compile_source(source_path, output_path=None, allow_bsp_mismatches=False):
    document = load_source(source_path)
    level = copy.deepcopy(document.level)
    report = LEGACY.rebuild_derived(level, validate=True)
    if report.fail_samples and not allow_bsp_mismatches:
        raise RuntimeError('BSP validation failed: %d mismatches' % len(report.fail_samples))

    if output_path is None:
        if source_path.endswith('.c3d.json'):
            output_path = source_path[:-9] + '.c3b'
        else:
            output_path = source_path + '.c3b'
    dump_c3b(level, document.materials, output_path, document.entities)
    return output_path, report


def dump_c3b(level, materials_path, output_path, entities_path=None):
    """Writes C3B geometry; entities_path moves placement records into INI."""
    _validate_compiled_level(level)
    material_bytes = materials_path.encode('utf-8')
    if len(material_bytes) > 65535:
        raise ValueError('material path слишком длинный')

    external_entities = entities_path is not None
    entity_bytes = b''
    if external_entities:
        entities_path = _relative_path(entities_path, 'entities')
        entity_bytes = entities_path.encode('utf-8')
        if len(entity_bytes) == 0 or len(entity_bytes) > 65535:
            raise ValueError('entity path слишком длинный')

    leaf_sectors = LEGACY.leaf_sectors(level)
    if any(v < 0 for v in leaf_sectors):
        raise ValueError('BSP leaves without sector')

    header_flags = HEADER_FLAG_EXTERNAL_ENTITIES if external_entities else 0
    object_count = 0 if external_entities else len(level.objects)
    header = struct.pack('<4sBBh8H', MAGIC, VERSION, header_flags, len(level.nodes) - 1,
                         len(level.vertices), len(level.walls), object_count,
                         len(level.surfaces), len(level.sectors), len(level.nodes),
                         len(level.leaves), len(level.segments))
    out = bytearray(header)
    out += struct.pack('<H', len(material_bytes)) + material_bytes
    if external_entities:
        out += struct.pack('<H', len(entity_bytes)) + entity_bytes

    for x, z in level.vertices:
        out += struct.pack('<hh', _i16(x), _i16(z))
    for wall in level.walls:
        out += struct.pack('<HHhhBBBB', wall['sv'], wall['ev'], wall['front'], wall['back'],
                           wall['flags'] & 255, wall['type'] & 255,
                           wall['special'] & 255, 0)
    if not external_entities:
        for obj in level.objects:
            out += struct.pack('<hhhhh', _i16(obj['x']), _i16(obj['z']), _i16(obj['angle']),
                               _i16(obj['type']), _i16(obj['param']))
    for surface in level.surfaces:
        out += struct.pack('<hhBBBBH', _i16(surface['ox']), _i16(surface['oy']),
                           surface['upper'] & 255, surface['lower'] & 255,
                           surface['main'] & 255, 0, surface['sector'])
    for sector in level.sectors:
        flags = (SECTOR_FLAG_CEILING_SKY if sector['ceil_tex'] == 51 else 0)
        flags |= SECTOR_FLAG_FLOOR_SKY if sector['floor_tex'] == 51 else 0
        out += struct.pack('<hhBBBBhh', _i16(sector['floor']), _i16(sector['ceil']),
                           0 if sector['floor_tex'] == 51 else sector['floor_tex'] & 255,
                           0 if sector['ceil_tex'] == 51 else sector['ceil_tex'] & 255,
                           (sector['light_packed'] >> 4) & 15, flags,
                           _i16(sector['tag']), _i16(sector['type']))
    for sx, sz, dx, dz, front, back in level.nodes:
        out += struct.pack('<hhhhhh', _i16(sx), _i16(sz), _i16(dx), _i16(dz),
                           _clean_child(front), _clean_child(back))
    for index, (count, offset) in enumerate(level.leaves):
        out += struct.pack('<HHH', leaf_sectors[index], offset, count)
    for start, end, definition, facing, offset in level.segments:
        out += struct.pack('<HHHBh', start, end, definition, facing & 255, _i16(offset))

    pvs = _natural_pvs(level)
    out += struct.pack('<I', len(pvs)) + pvs
    with open(output_path, 'wb') as stream:
        stream.write(out)


def read_c3b(path):
    data = open(path, 'rb').read()
    offset = 0
    magic, version, flags, root, nv, nw, no, nsf, nsec, nn, nl, nsg = struct.unpack_from('<4sBBh8H', data, offset)
    offset += struct.calcsize('<4sBBh8H')
    if magic != MAGIC or version != VERSION:
        raise ValueError('not C3B1')
    if flags & ~HEADER_FLAG_EXTERNAL_ENTITIES:
        raise ValueError('unsupported C3B header flags: %d' % flags)
    material_len, = struct.unpack_from('<H', data, offset)
    offset += 2
    material = data[offset:offset + material_len].decode('utf-8')
    offset += material_len
    entities = None
    if flags & HEADER_FLAG_EXTERNAL_ENTITIES:
        if no != 0:
            raise ValueError('external-entity C3B must not contain inline objects')
        entity_len, = struct.unpack_from('<H', data, offset)
        offset += 2
        entities = data[offset:offset + entity_len].decode('utf-8')
        offset += entity_len
        if not entities:
            raise ValueError('C3B external entity path is empty')
    return dict(version=version, flags=flags, root=root, materials=material,
                entities=entities, vertices=nv, walls=nw, objects=no,
                surfaces=nsf, sectors=nsec, nodes=nn, leaves=nl,
                segments=nsg, header_size=offset, total_size=len(data))


def create_demo(directory):
    if not os.path.isdir(directory):
        os.makedirs(directory)
    document = new_document()
    source = os.path.join(directory, 'level.c3d.json')
    dump_source(document, source)
    ENTITIES.dump_entities(document.level.objects,
                           os.path.join(directory, document.entities))
    output, report = compile_source(source, os.path.join(directory, 'level.c3b'))
    return source, output, report


def _natural_pvs(level):
    count = len(level.sectors)
    result = bytearray((count * count + 7) // 8)
    # Legacy pvs[to][from] == 1 means hidden. C3B writes natural from->to=visible.
    for source in range(count):
        for target in range(count):
            visible = not bool(level.pvs[target][source])
            if visible:
                bit = source * count + target
                result[bit >> 3] |= 1 << (bit & 7)
    return bytes(result)


def _clean_child(ref):
    if ref & 0x8000:
        return -((ref & 0x7fff) + 1)
    return _i16(ref)


def _validate_authored_level(level):
    if not level.vertices or not level.walls or not level.surfaces or not level.sectors:
        raise ValueError('C3D needs vertices, walls, surfaces and sectors')
    for x, z in level.vertices:
        _i16(x); _i16(z)
    for wall in level.walls:
        if not (0 <= wall['sv'] < len(level.vertices) and 0 <= wall['ev'] < len(level.vertices)):
            raise ValueError('wall vertex out of range')
        if not (0 <= wall['front'] < len(level.surfaces)):
            raise ValueError('wall front surface out of range')
        if not (-1 <= wall['back'] < len(level.surfaces)):
            raise ValueError('wall back surface out of range')
    for surface in level.surfaces:
        if not 0 <= surface['sector'] < len(level.sectors):
            raise ValueError('surface sector out of range')
        for key in ('upper', 'lower', 'main'):
            if not 0 <= surface[key] <= 127:
                raise ValueError('material slot must be 0..127')
    ENTITIES.validate_entities(level.objects)
    _validate_sector_winding(level)


def _validate_sector_winding(level):
    """Checks the front-side orientation required by PortalRenderer.

    A front segment is stored in wall start->end order and its sector is on
    the right of that directed edge.  Back segments use the reverse direction.
    Consequently a normal closed sector has a negative signed area in x,z
    coordinates (clockwise outer contour).  The BSP compiler can partition a
    counter-clockwise room, but PortalRenderer rejects its projected segments
    as back-to-front, producing a black frame.  Catch that authoring mistake
    before a C3B is written instead of silently shipping unusable geometry.
    """
    signed_areas = [0] * len(level.sectors)
    side_counts = [0] * len(level.sectors)

    for wall in level.walls:
        start = level.vertices[wall['sv']]
        end = level.vertices[wall['ev']]
        for surface_index, front_side in ((wall['front'], True), (wall['back'], False)):
            if surface_index < 0:
                continue
            sector_index = level.surfaces[surface_index]['sector']
            if front_side:
                x1, z1 = start
                x2, z2 = end
            else:
                x1, z1 = end
                x2, z2 = start
            signed_areas[sector_index] += x1 * z2 - x2 * z1
            side_counts[sector_index] += 1

    for sector_index in range(len(level.sectors)):
        if side_counts[sector_index] == 0:
            raise ValueError('sector %d has no wall sides' % sector_index)
        area = signed_areas[sector_index]
        if area == 0:
            raise ValueError('sector %d has zero signed area; close its wall boundary'
                             % sector_index)
        if area > 0:
            raise ValueError('sector %d is counter-clockwise; front surfaces must keep '
                             'their sector on the right (use clockwise walls)'
                             % sector_index)


def _validate_compiled_level(level):
    _validate_authored_level(level)
    counts = (len(level.vertices), len(level.walls), len(level.objects), len(level.surfaces),
              len(level.sectors), len(level.nodes), len(level.leaves), len(level.segments))
    if any(v == 0 for v in counts[:2] + counts[3:]):
        raise ValueError('compiled C3B has an empty required section')
    if any(v > 32767 for v in counts):
        raise ValueError('C3B signed index limit exceeded')
    for sector in level.sectors:
        _i16(sector['floor']); _i16(sector['ceil']); _i16(sector['tag']); _i16(sector['type'])
    for node in level.nodes:
        _i16(node[0]); _i16(node[1]); _i16(node[2]); _i16(node[3])


def _relative_path(value, name):
    if not isinstance(value, str) or not value:
        raise ValueError(name + ' path must be a non-empty string')
    normalized = value.replace('\\', '/')
    parts = normalized.split('/')
    if normalized[0] == '/' or any(part == '' or part == '.' or part == '..' for part in parts):
        raise ValueError(name + ' path must be a clean relative C3B-package path')
    return normalized


def _int(mapping, key, default=None):
    if key not in mapping:
        if default is None: raise ValueError('missing field: ' + key)
        return default
    return int(mapping[key])


def _slot(mapping, key, default=0):
    value = _int(mapping, key, default)
    if value < 0 or value > 127:
        raise ValueError(key + ' must be 0..127')
    return value


def _int_pair(value, name):
    if not isinstance(value, list) or len(value) != 2:
        raise ValueError(name + ' must be [x, z]')
    return int(value[0]), int(value[1])


def _i16(value):
    value = int(value)
    if value < -32768 or value > 32767:
        raise ValueError('int16 out of range: %d' % value)
    return value


def _usage():
    print('c3d2_core.py new <directory>')
    print('c3d2_core.py compile <level.c3d.json> [level.c3b]')
    print('c3d2_core.py inspect <level.c3b>')


if __name__ == '__main__':
    if len(sys.argv) < 3:
        _usage()
        sys.exit(1)
    command = sys.argv[1]
    if command == 'new':
        source, binary, report = create_demo(sys.argv[2])
        info = read_c3b(binary)
        print('source:', source)
        print('binary:', binary)
        print('BSP nodes=%d leaves=%d segments=%d splits=%d' %
              (info['nodes'], info['leaves'], info['segments'], report.splits))
    elif command == 'compile':
        output, report = compile_source(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
        print('compiled:', output)
        print('BSP splits=%d mixed=%d fails=%d' %
              (report.splits, report.mixed_leaves, len(report.fail_samples)))
    elif command == 'inspect':
        print(read_c3b(sys.argv[2]))
    else:
        _usage()
        sys.exit(1)
