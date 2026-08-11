#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Texture import/export helpers for the C3D2 editor.

The Java ME runtime consumes indexed BMP4/BMP8 files with at most 16 palette
entries.  This module keeps all expensive authoring work outside the runtime:
source images are resized, quantized with a deterministic pure-Python K-means,
and written as standard BMP4 files next to a C3D material manifest.

Pillow is optional for the core project but recommended for the editor because
it lets ``load_image_rgba`` open the image formats Pillow supports (PNG, JPEG,
WebP, TIFF, GIF, BMP, and many others).  A small PNG-only fallback is retained
for installations without Pillow.
"""

import os
import struct


class TextureError(ValueError):
    pass


MATERIAL_WALL = 'wall'
MATERIAL_FLAT = 'flat'
MATERIAL_SKY = 'sky'
VALID_MATERIAL_KINDS = (MATERIAL_WALL, MATERIAL_FLAT, MATERIAL_SKY)
WALL_HEIGHTS = (16, 64, 128)


def target_size(kind, wall_width=64, wall_height=128):
    """Returns the Java ME-compatible destination dimensions for a material."""
    if kind == MATERIAL_WALL:
        if not _is_power_of_two(wall_width) or wall_width < 2:
            raise TextureError('wall width must be a power of two and at least 2')
        if wall_height not in WALL_HEIGHTS:
            raise TextureError('wall height must be one of 16, 64, 128')
        return int(wall_width), int(wall_height)
    if kind == MATERIAL_FLAT:
        return 64, 64
    if kind == MATERIAL_SKY:
        return 64, 128
    raise TextureError('unknown material kind: %s' % kind)


def material_key(kind, slot=None):
    if kind == MATERIAL_SKY:
        return 'sky'
    if kind not in (MATERIAL_WALL, MATERIAL_FLAT):
        raise TextureError('unknown material kind: %s' % kind)
    if slot is None:
        raise TextureError('%s material needs a slot' % kind)
    try:
        slot = int(slot)
    except (TypeError, ValueError):
        raise TextureError('material slot must be an integer')
    if slot < 1 or slot > 127:
        raise TextureError('material slot must be 1..127')
    return kind + '.' + str(slot)


def load_manifest(path):
    """Reads a C3M manifest into a key -> relative path mapping."""
    result = {}
    if not os.path.exists(path):
        return result
    with open(path, 'r', encoding='utf-8') as stream:
        for number, raw_line in enumerate(stream, 1):
            line = raw_line.strip()
            if not line or line[0] == '#' or line[0] == ';':
                continue
            equals = line.find('=')
            if equals <= 0:
                raise TextureError('%s:%d: expected key=value' % (path, number))
            key = line[:equals].strip()
            value = line[equals + 1:].strip()
            if not value:
                raise TextureError('%s:%d: empty material path' % (path, number))
            result[key] = value
    return result


def update_manifest(path, key, relative_path):
    """Replaces one C3M entry while preserving comments and unrelated lines."""
    relative_path = relative_path.replace('\\', '/')
    if not relative_path or relative_path[0] == '/':
        raise TextureError('material path must be relative to the manifest')

    old_lines = []
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as stream:
            old_lines = stream.read().splitlines()

    new_lines = []
    found = False
    for raw_line in old_lines:
        stripped = raw_line.strip()
        equals = stripped.find('=')
        if equals > 0 and not stripped.startswith('#') and not stripped.startswith(';'):
            old_key = stripped[:equals].strip()
            if old_key == key:
                if not found:
                    new_lines.append(key + '=' + relative_path)
                    found = True
                continue
        new_lines.append(raw_line)

    if not found:
        if new_lines and new_lines[-1].strip():
            new_lines.append('')
        new_lines.append(key + '=' + relative_path)

    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    with open(path, 'w', encoding='utf-8') as stream:
        stream.write('\n'.join(new_lines) + '\n')


def load_image_rgba(path):
    """Loads an image from any Pillow-supported format into RGBA tuples.

    Without Pillow a limited PNG fallback uses the existing stdlib decoder;
    this keeps the editor's format conversion code testable in minimal setups.
    """
    try:
        from PIL import Image
        image = Image.open(path)
        image.load()
        image = image.convert('RGBA')
        return image.width, image.height, list(image.getdata())
    except ImportError:
        if not path.lower().endswith('.png'):
            raise TextureError('Pillow is required to import %s; install pillow for all image formats'
                               % os.path.basename(path))
        try:
            from png_to_bmp4 import read_png
            return read_png(path)
        except Exception as error:
            raise TextureError('PNG fallback could not read %s: %s' % (path, error))
    except Exception as error:
        raise TextureError('could not open image %s: %s' % (path, error))


def resize_rgba(pixels, source_width, source_height, target_width, target_height,
                fit=True):
    """Nearest-neighbour resize with optional centred aspect-preserving crop.

    Pillow performs high-quality decode/crop before this function when it is
    available in the UI.  Keeping a deterministic stdlib resampler here makes
    batch conversion and tests independent of a native image library.
    """
    if source_width <= 0 or source_height <= 0:
        raise TextureError('source image dimensions must be positive')
    if len(pixels) != source_width * source_height:
        raise TextureError('source image pixel count does not match dimensions')
    if target_width <= 0 or target_height <= 0:
        raise TextureError('target image dimensions must be positive')

    # Pillow is optional, but when present use its high-quality LANCZOS path.
    # The fallback below keeps PNG-only/minimal editor installs functional.
    try:
        from PIL import Image, ImageOps
        image = Image.new('RGBA', (source_width, source_height))
        image.putdata(pixels)
        resampling = getattr(Image, 'Resampling', Image).LANCZOS
        if fit:
            image = ImageOps.fit(image, (target_width, target_height), method=resampling)
        else:
            image = image.resize((target_width, target_height), resampling)
        return list(image.getdata())
    except ImportError:
        pass

    left = 0.0
    top = 0.0
    crop_width = float(source_width)
    crop_height = float(source_height)
    if fit:
        source_aspect = float(source_width) / float(source_height)
        target_aspect = float(target_width) / float(target_height)
        if source_aspect > target_aspect:
            crop_width = source_height * target_aspect
            left = (source_width - crop_width) * 0.5
        elif source_aspect < target_aspect:
            crop_height = source_width / target_aspect
            top = (source_height - crop_height) * 0.5

    output = []
    for y in range(target_height):
        source_y = int(top + (y + 0.5) * crop_height / target_height)
        if source_y < 0:
            source_y = 0
        elif source_y >= source_height:
            source_y = source_height - 1
        base = source_y * source_width
        for x in range(target_width):
            source_x = int(left + (x + 0.5) * crop_width / target_width)
            if source_x < 0:
                source_x = 0
            elif source_x >= source_width:
                source_x = source_width - 1
            output.append(pixels[base + source_x])
    return output


def rgba_to_rgb(pixels, background=(0, 0, 0)):
    """Composites source alpha once; world BMP materials are opaque."""
    red_bg, green_bg, blue_bg = background
    output = []
    for pixel in pixels:
        red, green, blue, alpha = pixel
        if alpha >= 255:
            output.append((int(red), int(green), int(blue)))
        elif alpha <= 0:
            output.append((red_bg, green_bg, blue_bg))
        else:
            inverse = 255 - alpha
            output.append(((int(red) * alpha + red_bg * inverse + 127) // 255,
                           (int(green) * alpha + green_bg * inverse + 127) // 255,
                           (int(blue) * alpha + blue_bg * inverse + 127) // 255))
    return output


def kmeans_quantize(pixels, colors=16, iterations=24):
    """Returns ``(palette, indices)`` using deterministic weighted K-means.

    Unlike a "top N colors" shortcut, K-means preserves gradients and photos
    considerably better after the mandatory 16-color Java ME quantization.
    The input is already resized to at most a small material texture, so this
    intentionally stays pure Python rather than adding numpy/scikit-learn as
    a mandatory editor dependency.
    """
    if colors < 1 or colors > 16:
        raise TextureError('palette size must be 1..16')
    if not pixels:
        raise TextureError('cannot quantize an empty image')

    histogram = {}
    for color in pixels:
        color = (int(color[0]), int(color[1]), int(color[2]))
        histogram[color] = histogram.get(color, 0) + 1
    ordered = sorted(histogram.items(), key=lambda item: (-item[1], item[0]))
    unique = [item[0] for item in ordered]

    centroids = [unique[0]]
    while len(centroids) < colors:
        best_color = unique[0]
        best_score = -1
        for color, frequency in ordered:
            distance = _nearest_distance(color, centroids)
            score = distance * frequency
            if score > best_score or (score == best_score and color < best_color):
                best_color = color
                best_score = score
        centroids.append(best_color)

    for _pass in range(max(1, int(iterations))):
        sums = [[0, 0, 0, 0] for _index in range(colors)]
        for color, frequency in ordered:
            index = _nearest_index(color, centroids)
            sums[index][0] += color[0] * frequency
            sums[index][1] += color[1] * frequency
            sums[index][2] += color[2] * frequency
            sums[index][3] += frequency

        changed = False
        next_centroids = []
        for index, total in enumerate(sums):
            if total[3] == 0:
                # Deterministically repopulate an empty cluster with the
                # color farthest from the non-empty current centroids.
                replacement = unique[0]
                farthest = -1
                for color, frequency in ordered:
                    distance = _nearest_distance(color, centroids)
                    score = distance * frequency
                    if score > farthest or (score == farthest and color < replacement):
                        replacement = color
                        farthest = score
                next_centroids.append(replacement)
            else:
                next_centroids.append(((total[0] + total[3] // 2) // total[3],
                                       (total[1] + total[3] // 2) // total[3],
                                       (total[2] + total[3] // 2) // total[3]))
            if next_centroids[-1] != centroids[index]:
                changed = True
        centroids = next_centroids
        if not changed:
            break

    indices = [_nearest_index(color, centroids) for color in pixels]
    return centroids, indices


def write_bmp4(path, width, height, palette, indices):
    """Writes an opaque 16-entry BI_RGB indexed BMP4 accepted by BMPLoader."""
    if width <= 0 or height <= 0 or len(indices) != width * height:
        raise TextureError('invalid BMP4 dimensions or index count')
    if not palette or len(palette) > 16:
        raise TextureError('BMP4 palette must contain 1..16 colors')
    for index in indices:
        if index < 0 or index > 15:
            raise TextureError('BMP4 palette index outside 0..15')

    normalized_palette = [(int(color[0]), int(color[1]), int(color[2]))
                          for color in palette]
    while len(normalized_palette) < 16:
        normalized_palette.append(normalized_palette[-1])

    row_bytes = (width + 1) >> 1
    stride = (row_bytes + 3) & ~3
    raster = bytearray()
    for y in range(height - 1, -1, -1):
        row = bytearray()
        base = y * width
        for x in range(0, width, 2):
            high = indices[base + x]
            low = indices[base + x + 1] if x + 1 < width else 0
            row.append(((high & 15) << 4) | (low & 15))
        row += b'\x00' * (stride - len(row))
        raster += row

    palette_bytes = bytearray()
    for red, green, blue in normalized_palette:
        palette_bytes += bytes((blue & 255, green & 255, red & 255, 0))

    offset = 14 + 40 + len(palette_bytes)
    file_size = offset + len(raster)
    output = bytearray()
    output += struct.pack('<2sIHHI', b'BM', file_size, 0, 0, offset)
    output += struct.pack('<IiiHHIIiiII', 40, width, height, 1, 4, 0,
                          len(raster), 2835, 2835, 16, 0)
    output += palette_bytes
    output += raster

    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    with open(path, 'wb') as stream:
        stream.write(output)
    return file_size


def convert_texture(source_path, destination_path, kind, wall_width=64, wall_height=128,
                    fit=True, iterations=24):
    """Imports one source image and returns metadata for the editor log."""
    source_width, source_height, pixels = load_image_rgba(source_path)
    output_width, output_height = target_size(kind, wall_width, wall_height)
    resized = resize_rgba(pixels, source_width, source_height, output_width, output_height,
                          fit=fit)
    palette, indices = kmeans_quantize(rgba_to_rgb(resized), 16, iterations)
    bytes_written = write_bmp4(destination_path, output_width, output_height, palette, indices)
    return dict(source=source_path, destination=destination_path, kind=kind,
                source_width=source_width, source_height=source_height,
                width=output_width, height=output_height, palette=palette,
                bytes=bytes_written, fit=bool(fit))


def create_starter_materials(package_dir, overwrite=False):
    """Creates a tiny valid wall/flat/sky set for a newly created package.

    A new C3D level starts with wall slot 1, flat slot 1 and sky enabled.  The
    editor should therefore export a runnable package before an artist imports
    real assets.  These deterministic checker/gradient BMP4 files are only
    placeholders and are safely replaced by ``import_material``.
    """
    texture_dir = os.path.join(package_dir, 'textures')
    if not os.path.isdir(texture_dir):
        os.makedirs(texture_dir)
    manifest = os.path.join(package_dir, 'materials.c3m')
    palette = [(20, 26, 34), (42, 54, 67), (66, 83, 101), (92, 113, 132),
               (121, 144, 163), (153, 174, 189), (186, 202, 211), (220, 229, 233),
               (35, 39, 45), (58, 63, 72), (84, 91, 102), (111, 120, 132),
               (142, 152, 163), (174, 184, 194), (206, 215, 224), (238, 244, 248)]

    wall_path = os.path.join(texture_dir, 'wall_1.bmp')
    flat_path = os.path.join(texture_dir, 'flat_1.bmp')
    sky_path = os.path.join(texture_dir, 'sky.bmp')
    if overwrite or not os.path.exists(wall_path):
        indices = []
        for y in range(128):
            for x in range(64):
                indices.append(8 if x % 16 == 0 or y % 16 == 0
                               else 2 + ((x // 8 + y // 8) & 3))
        write_bmp4(wall_path, 64, 128, palette, indices)
    if overwrite or not os.path.exists(flat_path):
        indices = []
        for y in range(64):
            for x in range(64):
                indices.append(2 + (((x >> 3) ^ (y >> 3)) & 3))
        write_bmp4(flat_path, 64, 64, palette, indices)
    if overwrite or not os.path.exists(sky_path):
        indices = []
        for y in range(128):
            for x in range(64):
                indices.append(min(14, 3 + (y >> 4) + (1 if ((x + y * 3) & 31) < 8 else 0)))
        write_bmp4(sky_path, 64, 128, palette, indices)

    update_manifest(manifest, 'wall.1', 'textures/wall_1.bmp')
    update_manifest(manifest, 'flat.1', 'textures/flat_1.bmp')
    update_manifest(manifest, 'sky', 'textures/sky.bmp')
    return manifest


def import_material(package_dir, source_path, kind, slot=1, wall_width=64,
                    wall_height=128, fit=True, iterations=24):
    """Converts a source image, places it in textures/, and updates C3M."""
    key = material_key(kind, slot)
    if kind == MATERIAL_SKY:
        filename = 'sky.bmp'
    elif kind == MATERIAL_WALL:
        filename = 'wall_%d.bmp' % int(slot)
    else:
        filename = 'flat_%d.bmp' % int(slot)
    relative_path = 'textures/' + filename
    destination = os.path.join(package_dir, *relative_path.split('/'))
    result = convert_texture(source_path, destination, kind, wall_width, wall_height,
                             fit, iterations)
    manifest = os.path.join(package_dir, 'materials.c3m')
    update_manifest(manifest, key, relative_path)
    result['key'] = key
    result['relative_path'] = relative_path
    result['manifest'] = manifest
    return result


def _nearest_index(color, palette):
    best_index = 0
    best_distance = _distance2(color, palette[0])
    for index in range(1, len(palette)):
        distance = _distance2(color, palette[index])
        if distance < best_distance:
            best_index = index
            best_distance = distance
    return best_index


def _nearest_distance(color, palette):
    return _distance2(color, palette[_nearest_index(color, palette)])


def _distance2(first, second):
    red = first[0] - second[0]
    green = first[1] - second[1]
    blue = first[2] - second[2]
    return red * red + green * green + blue * blue


def _is_power_of_two(value):
    value = int(value)
    return value > 0 and (value & (value - 1)) == 0
