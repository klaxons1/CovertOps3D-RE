#!/usr/bin/env python3
"""Generate the tiny BMP4 fixtures used by the C3D loose-material demo.

The runtime intentionally loads these files through BMPLoader/CustomMaterialSet
rather than an atlas. The script uses only Python's standard library.
"""

import os
import struct

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "res", "gamedata", "custom", "demo", "textures")


def write_bmp4(path, width, height, palette, pixel):
    if width & 1:
        raise ValueError("fixture width must be even")
    if len(palette) != 16:
        raise ValueError("BMP4 palette must have 16 colors")

    row_bytes = width // 2
    stride = (row_bytes + 3) & ~3
    pixel_offset = 14 + 40 + 16 * 4
    file_size = pixel_offset + stride * height

    out = bytearray()
    out += struct.pack("<2sIHHI", b"BM", file_size, 0, 0, pixel_offset)
    out += struct.pack("<IiiHHIIiiII", 40, width, height, 1, 4, 0,
                       stride * height, 2835, 2835, 16, 0)
    for red, green, blue in palette:
        out += bytes((blue, green, red, 0))

    # Positive BMP height means bottom-up rows.
    for y in range(height - 1, -1, -1):
        row = bytearray()
        for x in range(0, width, 2):
            row.append((pixel(x, y) & 15) << 4 | (pixel(x + 1, y) & 15))
        row += b"\0" * (stride - len(row))
        out += row

    with open(path, "wb") as stream:
        stream.write(out)


def main():
    if not os.path.isdir(ROOT):
        os.makedirs(ROOT)

    wall_palette = [
        (27, 20, 16), (59, 43, 31), (91, 62, 39), (123, 84, 50),
        (154, 108, 62), (188, 138, 81), (219, 172, 102), (239, 201, 130),
        (48, 50, 52), (75, 77, 79), (104, 106, 108), (135, 137, 139),
        (166, 168, 170), (198, 200, 202), (225, 227, 229), (250, 250, 250),
    ]
    floor_palette = [
        (20, 25, 22), (37, 46, 38), (54, 68, 55), (74, 91, 73),
        (95, 115, 92), (121, 143, 112), (149, 169, 136), (182, 197, 163),
        (31, 34, 37), (49, 53, 57), (69, 75, 80), (92, 99, 105),
        (117, 125, 132), (146, 154, 162), (178, 186, 194), (213, 221, 229),
    ]
    sky_palette = [
        (18, 27, 55), (26, 40, 78), (37, 56, 105), (49, 73, 132),
        (63, 91, 158), (80, 111, 184), (101, 135, 207), (127, 160, 226),
        (155, 183, 238), (182, 204, 246), (205, 221, 250), (224, 235, 253),
        (239, 244, 255), (249, 249, 255), (255, 255, 255), (240, 230, 210),
    ]

    def wall_pixel(x, y):
        mortar = x % 16 == 0 or y % 16 == 0
        return 8 if mortar else 2 + ((x // 8 + y // 8) & 3)

    def floor_pixel(x, y):
        return 2 + (((x >> 3) ^ (y >> 3)) & 3)

    def sky_pixel(x, y):
        band = y >> 4
        cloud = 0
        if 25 < y < 54 and ((x + y * 3) & 31) < 11:
            cloud = 4
        return min(14, 2 + band + cloud)

    write_bmp4(os.path.join(ROOT, "wall.bmp"), 64, 128, wall_palette, wall_pixel)
    write_bmp4(os.path.join(ROOT, "floor.bmp"), 64, 64, floor_palette, floor_pixel)
    write_bmp4(os.path.join(ROOT, "sky.bmp"), 64, 128, sky_palette, sky_pixel)


if __name__ == "__main__":
    main()
