#!/usr/bin/env python3
# png_to_bmp4.py - converts weapon sprite PNGs to 4-bit indexed BMPs for the game.
#
# Pure stdlib (zlib+struct) so it runs anywhere; no PIL required.
#
# Convention expected by the game loader (src/BMPLoader.java + src/Weapon.java):
#   * 4-bit indexed, 16-entry palette, no compression (BI_RGB)
#   * palette index 0 is the transparent background color
#   * the weapon palette is re-tinted at runtime by the current sector light
#
# Usage:  python3 png_to_bmp4.py <in.png> [<in2.png> ...]
# Writes <name>.bmp next to each input file.

import struct
import sys
import zlib


def read_png(path):
    """Minimal PNG decoder: 8-bit, non-interlaced, color types 3/6/2 -> RGBA pixel list."""
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', 'not a PNG: ' + path
    pos = 8
    width = height = None
    bit_depth = color_type = interlace = None
    plte = None
    trns = None
    idat = b''
    while pos < len(data):
        length = struct.unpack('>I', data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if ctype == b'IHDR':
            width, height, bit_depth, color_type, comp, filt, interlace = struct.unpack('>IIBBBBB', chunk)
        elif ctype == b'PLTE':
            plte = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif ctype == b'tRNS':
            trns = chunk
        elif ctype == b'IDAT':
            idat += chunk
        elif ctype == b'IEND':
            break
    assert interlace == 0, 'interlaced PNG not supported: ' + path
    assert color_type in (2, 3, 6), 'unsupported PNG color type: ' + path
    if color_type == 3:
        assert bit_depth in (1, 2, 4, 8), 'unsupported palette bit depth: ' + path
        filter_bpp = 1
        stride = (width * bit_depth + 7) >> 3
    else:
        assert bit_depth == 8, 'only 8-bit truecolor PNG supported: ' + path
        filter_bpp = {6: 4, 2: 3}[color_type]
        stride = width * filter_bpp
    raw = zlib.decompress(idat)
    pixels = []  # flat RGBA list
    prev = bytearray(stride)
    off = 0
    for _y in range(height):
        f = raw[off]
        off += 1
        line = bytearray(raw[off:off + stride])
        off += stride
        for i in range(stride):
            a = line[i - filter_bpp] if i >= filter_bpp else 0
            b = prev[i]
            c = prev[i - filter_bpp] if i >= filter_bpp else 0
            if f == 1:
                line[i] = (line[i] + a) & 0xFF
            elif f == 2:
                line[i] = (line[i] + b) & 0xFF
            elif f == 3:
                line[i] = (line[i] + ((a + b) >> 1)) & 0xFF
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if (pa <= pb and pa <= pc) else (b if pb <= pc else c))) & 0xFF
        prev = line
        if color_type == 3 and bit_depth < 8:
            # unpack packed palette indices, MSB first
            unpacked = []
            per_byte = 8 // bit_depth
            mask = (1 << bit_depth) - 1
            for byte in line:
                for k in range(per_byte):
                    unpacked.append((byte >> (8 - bit_depth * (k + 1))) & mask)
            line_samples = unpacked[:width]
        else:
            line_samples = line
        for x in range(width):
            if color_type == 3:
                idx = line_samples[x]
                r, g, b = plte[idx]
                a = trns[idx] if trns and idx < len(trns) else 255
            elif color_type == 6:
                o = x * 4
                r, g, b, a = line[o], line[o + 1], line[o + 2], line[o + 3]
            else:
                o = x * 3
                r, g, b, a = line[o], line[o + 1], line[o + 2], 255
            pixels.append((r, g, b, a))
    return width, height, pixels


def to_indexed_15(pixels):
    """Map pixels to 15 opaque colors + index 0 for transparent."""
    freq = {}
    for (r, g, b, a) in pixels:
        if a >= 128:
            freq[(r, g, b)] = freq.get((r, g, b), 0) + 1
    colors = sorted(freq.items(), key=lambda kv: -kv[1])
    if len(colors) > 15:
        keep = [c for (c, _n) in colors[:15]]
    else:
        keep = [c for (c, _n) in colors]

    def nearest(rgb):
        best, bestd = 0, 4 * 255 * 255 * 3
        for i, c in enumerate(keep):
            d = (rgb[0] - c[0]) ** 2 + (rgb[1] - c[1]) ** 2 + (rgb[2] - c[2]) ** 2
            if d < bestd:
                best, bestd = i, d
        return best + 1  # palette index (0 = transparent)

    lookup = {c: i + 1 for i, c in enumerate(keep)}
    indices = []
    for (r, g, b, a) in pixels:
        if a < 128:
            indices.append(0)
        else:
            rgb = (r, g, b)
            indices.append(lookup.get(rgb, nearest(rgb)))
    return keep, indices


def write_bmp4(path, width, height, colors, indices):
    palette = [(0, 0, 0)] + colors
    palette += [(0, 0, 0)] * (16 - len(palette))

    row_bytes = (width + 1) >> 1
    stride = (row_bytes + 3) & ~3
    raster = bytearray()
    for y in range(height - 1, -1, -1):  # bottom-up
        row = bytearray()
        for x in range(0, width, 2):
            hi = indices[y * width + x]
            lo = indices[y * width + x + 1] if x + 1 < width else 0
            row.append(((hi & 15) << 4) | (lo & 15))
        row += b'\x00' * (stride - len(row))
        raster += row

    palette_bytes = bytearray()
    for (r, g, b) in palette:
        palette_bytes += bytes((b, g, r, 0))

    file_header_size = 14
    info_size = 40
    raster_offset = file_header_size + info_size + len(palette_bytes)
    file_size = raster_offset + len(raster)

    out = bytearray()
    out += b'BM'
    out += struct.pack('<IHHI', file_size, 0, 0, raster_offset)
    out += struct.pack('<IiiHHIIiiII', info_size, width, height, 1, 4,
                       0, len(raster), 2835, 2835, 16, 0)
    out += palette_bytes
    out += raster
    open(path, 'wb').write(out)
    return file_size


def main():
    for src in sys.argv[1:]:
        w, h, pixels = read_png(src)
        colors, indices = to_indexed_15(pixels)
        dst = src[:-4] + '.bmp' if src.lower().endswith('.png') else src + '.bmp'
        size = write_bmp4(dst, w, h, colors, indices)
        print('%s -> %s  %dx%d, %d colors (+transparent), %d bytes'
              % (src, dst, w, h, len(colors), size))


if __name__ == '__main__':
    main()
