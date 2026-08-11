import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Indexed BMP loader with 8-bit and 4-bit support.
 *
 * Ported from https://github.com/rmn20/l2d_m3g/blob/master/src/com/BMPLoader.java
 *
 * Changes vs the original:
 *  - no M3G dependency: returns raw palette indices + ARGB palette
 *    instead of Image2D (this project is MIDP-only, no JSR-184)
 *  - added 4-bit (16 color) BMP decoding
 *  - BITMAPCOREHEADER (v2) now reads its 2-byte width/height fields and
 *    3-byte palette entries correctly
 *  - top-down rasters are copied row by row instead of aliasing the file bytes
 *
 * The pixel data is kept as palette indices on purpose: weapon sprites
 * re-tint the palette depending on the light level of the current sector,
 * so the palette has to stay separate from the pixels.
 *
 * Convention: palette index 0 is the transparent color for weapon sprites.
 */
public final class BMPLoader {

    private static final int BMP_FILE_HEADER_SIZE = 0x0E;
    private static final int BMP_INFO_HEADER_V2_SIZE = 12;
    private static final int BMP_INFO_HEADER_V3_SIZE = 40;

    public int width;
    public int height;
    public int bitDepth;

    /** One palette index per pixel, top-down row order, width*height entries. */
    public byte[] indices;

    /** ARGB colors from the file palette. */
    public int[] palette;

    private final byte[] data;
    private int pos;

    private BMPLoader(byte[] bytes) { data = bytes; }

    private void skip(int bytes) {
        pos += bytes;
    }

    private byte readByte() {
        return data[pos++];
    }

    private int readUByte() {
        return data[pos++] & 0xff;
    }

    private short readShort() {
        return (short) ((data[pos++] & 0xff) | ((data[pos++] & 0xff) << 8));
    }

    private int readUShort() {
        return (data[pos++] & 0xff) | ((data[pos++] & 0xff) << 8);
    }

    private int readInt() {
        return readUShort() | (readUShort() << 16);
    }

    /** Loads an 8-bit or 4-bit BMP resource from the JAR. */
    public static BMPLoader loadBMP(String name) throws IOException {
        if (name == null) throw new NullPointerException();

        InputStream is = BMPLoader.class.getResourceAsStream(name);
        if (is == null) throw new IOException("Resource not found: " + name);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[Math.max(1024, is.available())];

        int len;
        while ((len = is.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }

        is.close();

        return loadBMP(name, baos.toByteArray());
    }

    private static BMPLoader loadBMP(String name, byte[] data) {
        BMPLoader loader = new BMPLoader(data);

        // BITMAPFILEHEADER
        if (loader.readUByte() != 'B' || loader.readUByte() != 'M') {
            throw new RuntimeException("Not a BMP file: " + name);
        }

        // "BM", file size 4b, reserved 4b
        loader.skip(BMP_FILE_HEADER_SIZE - 6);

        int rasterOffset = loader.readInt();

        // BITMAPINFOHEADER
        int headerSize = loader.readInt();
        if (headerSize != BMP_INFO_HEADER_V2_SIZE && headerSize != BMP_INFO_HEADER_V3_SIZE) {
            throw new RuntimeException("Invalid BMP header size: " + name);
        }

        int width;
        int height;
        if (headerSize == BMP_INFO_HEADER_V2_SIZE) {
            // BITMAPCOREHEADER stores width/height as 16-bit values
            width = loader.readUShort();
            height = loader.readShort();
        } else {
            width = loader.readInt();
            height = loader.readInt();
        }
        if (width <= 0 || height == 0) {
            throw new RuntimeException("Invalid BMP resolution: " + name);
        }

        boolean reversed = height >= 0;
        height = Math.abs(height);

        if (loader.readUShort() != 1) {
            throw new RuntimeException("BMP planes != 1: " + name);
        }

        int bpp = loader.readUShort();
        if (bpp != 8 && bpp != 4) {
            throw new RuntimeException("BMP bpp is not 8 or 4: " + name);
        }

        int paletteEntrySize;
        int numColors;

        if (headerSize == BMP_INFO_HEADER_V2_SIZE) {
            // BITMAPCOREHEADER: no compression field, RGBTRIPLE palette
            paletteEntrySize = 3;
            numColors = (bpp == 4) ? 16 : 256;
        } else {
            int compression = loader.readInt();
            if (compression != 0) {
                throw new RuntimeException("BMP compression is not supported: " + name);
            }

            loader.skip(12);
            numColors = loader.readInt();
            if (numColors <= 0) numColors = (bpp == 4) ? 16 : 256;
            loader.skip(4);
            paletteEntrySize = 4;
        }

        int paletteOffset = BMP_FILE_HEADER_SIZE + headerSize;
        if (rasterOffset < paletteOffset + numColors * paletteEntrySize) {
            rasterOffset = paletteOffset + numColors * paletteEntrySize;
        }

        int[] palette = new int[numColors];
        for (int i = 0; i < numColors; i++) {
            int idx = i * paletteEntrySize + paletteOffset;
            int blue  = data[idx] & 0xFF;
            int green = data[idx + 1] & 0xFF;
            int red   = data[idx + 2] & 0xFF;
            palette[i] = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        // Rows are padded to a 4-byte boundary
        int rowBytes = (bpp == 4) ? ((width + 1) >> 1) : width;
        int padding = rowBytes & 3;
        int stride = (padding == 0) ? rowBytes : rowBytes + 4 - padding;

        byte[] bitmap = new byte[width * height];

        for (int y = 0; y < height; y++) {
            int srcRow = rasterOffset + (reversed ? (height - 1 - y) : y) * stride;
            int dstRow = y * width;

            if (bpp == 8) {
                System.arraycopy(data, srcRow, bitmap, dstRow, width);
            } else {
                // 4-bit: two pixels per byte, high nibble first
                for (int x = 0; x < width; x++) {
                    int packedPair = data[srcRow + (x >> 1)] & 0xFF;
                    int nib = ((x & 1) == 0) ? (packedPair >> 4) : (packedPair & 15);
                    bitmap[dstRow + x] = (byte) nib;
                }
            }
        }

        loader.width = width;
        loader.height = height;
        loader.bitDepth = bpp;
        loader.indices = bitmap;
        loader.palette = palette;
        return loader;
    }
}
