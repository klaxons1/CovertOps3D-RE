#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Headless model checks for the Pygame C3D2 editor.

The UI dependency is intentionally lazy, so package/save/build behavior stays
testable on CI hosts without Pygame or Pillow.
"""

import os
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'scripts'))
import c3d2_editor as EDITOR
import c3d2_core as C3


def main():
    with tempfile.TemporaryDirectory() as directory:
        package = os.path.join(directory, 'sample')
        model = EDITOR.PackageModel(package, create=True)
        assert model.document.entities == 'entities.ini'
        assert os.path.exists(model.level_path)
        assert os.path.exists(model.entity_path)
        assert os.path.exists(model.manifest_path)
        assert model.materials['wall.1'] == 'textures/wall_1.bmp'
        assert model.materials['flat.1'] == 'textures/flat_1.bmp'
        assert model.materials['sky'] == 'textures/sky.bmp'
        assert len(model.level.objects) == 1

        # The image importer is Pillow-free for this PNG fallback but follows
        # the same resize -> K-means -> BMP4 -> C3M path as the Pygame UI.
        source_image = os.path.join(ROOT, 'res', 'gamedata', 'sprites', 'aim.png')
        imported = model.import_material(source_image, 'flat', 7)
        assert imported['relative_path'] == 'textures/flat_7.bmp'
        assert os.path.exists(os.path.join(package, 'textures', 'flat_7.bmp'))
        assert model.materials['flat.7'] == 'textures/flat_7.bmp'
        sprite = model.import_material(source_image, 'sprite', 8)
        assert sprite['relative_path'] == 'sprites/custom/sprite_8.bmp'
        assert model.materials['sprite.8'] == 'sprites/custom/sprite_8.bmp'

        model.level.objects[0]['x'] = 24
        model.level.objects[0]['z'] = -16
        model.mark_dirty()
        model.save_source()
        output, report = model.compile()
        assert os.path.exists(output)
        assert not report.fail_samples
        info = C3.read_c3b(output)
        assert info['entities'] == 'entities.ini'
        assert info['objects'] == 0

        reopened = EDITOR.PackageModel(package)
        assert reopened.level.objects[0]['x'] == 24
        assert reopened.level.objects[0]['z'] == -16
    print('C3D2 editor model: package, entities and C3B export: OK')


if __name__ == '__main__':
    main()
