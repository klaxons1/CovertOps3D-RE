#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Convert a classic Doom map from DOOM.WAD into a compact C3D2 package.

Examples:
    python3 scripts/convert_doom_e1m1.py
    python3 scripts/convert_doom_e1m1.py docs/DOOM.WAD res/gamedata/custom/doom-e1m1
    python3 scripts/convert_doom_e1m1.py --map E1M2 --sprites used
"""

import argparse
import os
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(SCRIPT_DIR)
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

import doom_wad_core as DOOM


def main(argv=None):
    parser = argparse.ArgumentParser(description='Classic Doom WAD -> C3D2 converter')
    parser.add_argument('wad', nargs='?', default=os.path.join(ROOT, 'docs', 'DOOM.WAD'),
                        help='source IWAD/PWAD (default: docs/DOOM.WAD)')
    parser.add_argument('output', nargs='?',
                        default=os.path.join(ROOT, 'res', 'gamedata', 'custom', 'doom-e1m1'),
                        help='target C3D2 package directory')
    parser.add_argument('--map', default='E1M1', help='classic map marker, default E1M1')
    parser.add_argument('--height-scale', type=float, default=0.5,
                        help='vertical Doom-unit scale; default 0.5 makes 24-unit stairs walkable')
    parser.add_argument('--world-scale', type=float, default=1.0,
                        help='horizontal Doom-unit scale; default 1.0')
    parser.add_argument('--clearance', type=int, default=64,
                        help='minimum ceiling clearance in C3D units; default 64')
    parser.add_argument('--sprites', choices=('none', 'used', 'all'), default='none',
                        help='optional Doom patch sprite export; default none keeps JAR compact')
    parser.add_argument('--pvs', choices=('auto', 'doom-reject', 'all-visible'), default='auto',
                        help='PVS source; auto keeps E1M1 REJECT and uses all-visible for artifact-safe E1M2')
    parser.add_argument('--shared-assets', default=None,
                        help='optional shared texture directory, e.g. res/gamedata/custom/doom-common')
    args = parser.parse_args(argv)

    try:
        wad = DOOM.WadFile(args.wad)
        doom_map = DOOM.load_map(wad, args.map)
        report = DOOM.convert_map(wad, doom_map, args.output,
                                  height_scale=args.height_scale,
                                  world_scale=args.world_scale,
                                  minimum_clearance=args.clearance,
                                  extract_sprites=args.sprites,
                                  pvs_mode=args.pvs,
                                  shared_asset_dir=args.shared_assets)
    except Exception as error:
        print('Doom conversion failed: %s' % error, file=sys.stderr)
        return 2

    print('Converted %s -> %s' % (args.map.upper(), args.output))
    print('geometry: vertices=%d linedefs=%d sectors=%d' %
          (report['vertices'], report['linedefs'], report['sectors']))
    print('assets: wall=%d flat=%d enemySprites=%d optionalSprites=%d' %
          (report['wall_textures'], report['flats'], report['enemy_sprite_materials'],
           report['sprites']))
    print('gameplay: doors=%d enemies=%d worldItems=%d' %
          (report['doors'], report['enemies'], report['world_items']))
    print('PVS: %s visible=%d/%d' % (report['pvs_mode'], report['pvs_visible_pairs'],
          report['sectors'] * report['sectors']))
    print('C3B: nodes=%d leaves=%d segments=%d splits=%d failures=%d' %
          (report['bsp_nodes'], report['bsp_leaves'], report['bsp_segments'],
           report['bsp_splits'], report['bsp_failures']))
    if report['missing_wall_textures']:
        print('texture fallbacks: %d (see doom_conversion.json)'
              % len(report['missing_wall_textures']))
    return 0


if __name__ == '__main__':
    sys.exit(main())
