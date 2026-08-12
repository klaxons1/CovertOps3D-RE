#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""C3D external entity-placement INI reader/writer.

Geometry belongs in ``level.c3d.json`` and compiled C3B.  Entity positions are
editable gameplay data, so C3D2 stores them next to the level in a small UTF-8
INI file.  The runtime has an equivalent Java ME parser in CustomEntitySet.

Example::

    [entities]
    format=C3D-ENTITIES-1

    [entity.0]
    x=0
    z=0
    angle=0
    type=1
    param=0
"""

import os

FORMAT = 'C3D-ENTITIES-1'
_REQUIRED_KEYS = ('x', 'z', 'type')
_OPTIONAL_DEFAULTS = {'angle': 0, 'param': 0}
# sprite is optional and intentionally omitted when zero so old entity sidecars
# stay byte-for-byte familiar. frame1..frame6 are Doom actor animation slots;
# sprite itself is frame0.
_ANIMATION_KEYS = ('sprite', 'frame1', 'frame2', 'frame3', 'frame4', 'frame5', 'frame6')
_OPTIONAL_KEYS = tuple(_OPTIONAL_DEFAULTS.keys()) + _ANIMATION_KEYS
_ALL_KEYS = _REQUIRED_KEYS + _OPTIONAL_KEYS


def load_entities(path):
    """Reads a UTF-8 C3D entity sidecar and returns ordered entity dicts."""
    with open(path, 'r', encoding='utf-8') as stream:
        lines = stream.readlines()

    entities = []
    entity_ids = set()
    current = None
    current_seen = None
    in_entities_header = False
    format_seen = False

    def finish_entity():
        if current is None:
            return
        missing = [key for key in _REQUIRED_KEYS if key not in current_seen]
        if missing:
            raise ValueError('%s: entity.%d missing %s' %
                             (path, len(entities), ', '.join(missing)))
        entities.append(current)

    for line_number, raw_line in enumerate(lines, 1):
        line = raw_line.strip()
        if not line or line[0] == '#' or line[0] == ';':
            continue

        if line[0] == '[' and line[-1:] == ']':
            finish_entity()
            current = None
            current_seen = None
            name = line[1:-1].strip()
            if name == 'entities':
                if format_seen or in_entities_header:
                    raise ValueError('%s:%d: duplicate [entities] section' %
                                     (path, line_number))
                in_entities_header = True
                continue
            if not name.startswith('entity.'):
                raise ValueError('%s:%d: unknown section [%s]' %
                                 (path, line_number, name))
            if not in_entities_header:
                raise ValueError('%s:%d: [entities] must come first' %
                                 (path, line_number))
            entity_id_text = name[7:]
            try:
                entity_id = int(entity_id_text)
            except ValueError:
                raise ValueError('%s:%d: entity section needs a numeric id' %
                                 (path, line_number))
            if entity_id < 0 or entity_id in entity_ids:
                raise ValueError('%s:%d: duplicate or invalid entity id' %
                                 (path, line_number))
            entity_ids.add(entity_id)
            current = dict(_OPTIONAL_DEFAULTS)
            current_seen = set()
            continue

        equals = line.find('=')
        if equals <= 0:
            raise ValueError('%s:%d: expected key=value' % (path, line_number))
        key = line[:equals].strip()
        value = line[equals + 1:].strip()

        if current is None:
            if not in_entities_header or key != 'format' or format_seen:
                raise ValueError('%s:%d: property outside an entity section' %
                                 (path, line_number))
            if value != FORMAT:
                raise ValueError('%s:%d: unsupported entity format: %s' %
                                 (path, line_number, value))
            format_seen = True
            continue

        if key not in _ALL_KEYS:
            raise ValueError('%s:%d: unknown entity key: %s' %
                             (path, line_number, key))
        if key in current_seen:
            raise ValueError('%s:%d: duplicate entity key: %s' %
                             (path, line_number, key))
        current[key] = _i16(value, '%s:%d %s' % (path, line_number, key))
        current_seen.add(key)

    finish_entity()
    if not format_seen:
        raise ValueError('%s: missing [entities] format=%s' % (path, FORMAT))
    validate_entities(entities)
    return entities


def dump_entities(entities, path):
    """Writes canonical deterministic UTF-8 C3D entity-placement INI."""
    validate_entities(entities)
    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)

    with open(path, 'w', encoding='utf-8') as stream:
        stream.write('# C3D entity placement v1\n')
        stream.write('[entities]\n')
        stream.write('format=%s\n' % FORMAT)
        for index, entity in enumerate(entities):
            stream.write('\n[entity.%d]\n' % index)
            stream.write('x=%d\n' % int(entity['x']))
            stream.write('z=%d\n' % int(entity['z']))
            stream.write('angle=%d\n' % int(entity.get('angle', 0)))
            stream.write('type=%d\n' % int(entity['type']))
            stream.write('param=%d\n' % int(entity.get('param', 0)))
            if int(entity.get('sprite', 0)) != 0:
                stream.write('sprite=%d\n' % int(entity['sprite']))
            for frame in range(1, 7):
                key = 'frame%d' % frame
                if int(entity.get(key, 0)) != 0:
                    stream.write('%s=%d\n' % (key, int(entity[key])))


def validate_entities(entities):
    """Validates the runtime int16 record range and a usable player spawn."""
    if not entities:
        raise ValueError('C3D entity file needs at least one player spawn')

    has_spawn = False
    for index, entity in enumerate(entities):
        if not isinstance(entity, dict):
            raise ValueError('entity %d is not a mapping' % index)
        for key in entity:
            if key not in _ALL_KEYS:
                raise ValueError('entity %d has unknown key %s' % (index, key))
        for key in _REQUIRED_KEYS:
            if key not in entity:
                raise ValueError('entity %d missing %s' % (index, key))
        for key in _ALL_KEYS:
            if key in entity:
                _i16(entity[key], 'entity %d %s' % (index, key))
        for key in _ANIMATION_KEYS:
            if key in entity and (int(entity[key]) < 0 or int(entity[key]) > 127):
                raise ValueError('entity %d %s must be 0..127' % (index, key))
        entity_type = int(entity['type'])
        if 1 <= entity_type <= 4:
            has_spawn = True
    if not has_spawn:
        raise ValueError('C3D entity file needs a player spawn (type 1..4)')


def _i16(value, name):
    try:
        value = int(value)
    except (TypeError, ValueError):
        raise ValueError('%s must be an integer' % name)
    if value < -32768 or value > 32767:
        raise ValueError('%s is outside int16 range: %d' % (name, value))
    return value
