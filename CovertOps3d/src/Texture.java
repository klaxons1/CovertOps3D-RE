/**
 * Texture class for handling game textures with palette-based rendering
 * Supports 4-bit color (16 colors) with multiple lighting variations
 */
public final class Texture {
    public short width;           // Texture width in pixels
    public short height;          // Texture height in pixels
    public short horizontalOffset; // Horizontal offset for texture alignment
    public short verticalOffset;   // Vertical offset for texture alignment
    public byte[][] pixelData;    // 4-bit pixel data (2 pixels per byte) // 30.11.2025 - can be 3, 2 bit
    public int[][] colorPalettes; // 16 color palettes with different lighting levels
    public byte textureType;      // Texture type identifier
    private int widthMask;        // Mask for fast modulo operations (width - 1)

    public Texture(byte textureType, int width, int height, int horizontalOffset, int verticalOffset) {
        this.textureType = textureType;
        this.width = (short)width;
        this.height = (short)height;
        this.widthMask = width - 1;
        this.horizontalOffset = (short)horizontalOffset;
        this.verticalOffset = (short)verticalOffset;

        // Allocate pixel data array (2 pixels per byte, packed format)
        if (width > 0) {
            this.pixelData = new byte[(width >> 1) + (width & 1)][];
        } else {
            this.pixelData = null;
        }

        this.colorPalettes = null;
    }

    public Texture(byte textureType, int width, int height, int horizontalOffset, int verticalOffset, int[] palette) {
        this(textureType, width, height, horizontalOffset, verticalOffset);
        this.colorPalettes = createColorPalettes(palette);
    }

    /**
     * Set pixel data for a specific row
     * @param row Row index (0-based)
     * @param pixels Pixel data (4-bit packed, 2 pixels per byte)
     */
    public final void setPixelData(int row, byte[] pixels) {
        this.pixelData[row >> 1] = pixels;
    }

    /**
     * Get pixel row with wrap-around (for repeating textures)
     * @param row Row index (handles negative and out-of-bounds)
     * @return Pixel data for the requested row
     */
    public final byte[] getPixelRow(int row) {
        // Handle wrap-around for repeating textures
        if (row >= this.width) {
            row %= this.width;
        }

        while(row < 0) {
            row = (row + this.width * 1000) % this.width;
        }

        return this.pixelData[row >> 1];
    }

    /**
     * Fast pixel row access using bitmask (assumes power-of-2 width)
     * @param row Row index
     * @return Pixel data for the requested row
     */
    public final byte[] getPixelRowFast(int row) {
        return this.pixelData[(row & this.widthMask) >> 1];
    }

    /**
     * Composite another texture onto this one
     * @param source Source texture data
     * @param srcX Source X coordinate
     * @param srcY Source Y coordinate
     * @param width Width of area to composite
     * @param height Height of area to composite
     * @param destX Destination X coordinate
     * @param destY Destination Y coordinate
     * @param replace True to replace, false to blend
     */
    public final void compositeTexture(byte[][] source, int srcX, int srcY, int width, int height,
                                       int destX, int destY, boolean replace) {
        if (replace) {
            // Replace mode: set new texture dimensions and copy data
            this.width = (short)width;
            this.height = (short)height;
            this.widthMask = this.width - 1;
            int rowCount = (this.width >> 1) + (this.width & 1);
            this.pixelData = new byte[rowCount][];

            for(int row = 0; row < rowCount; ++row) {
                this.pixelData[row] = new byte[this.height];
                System.arraycopy(source[row], 0, this.pixelData[row], 0, this.height);
            }
        } else {
            // Blend mode: composite source onto destination
            for(int srcRow = 0; srcRow < width; ++srcRow) {
                int sourceIndex = (srcRow + srcX) >> 1;
                int sourceShift = (srcRow + srcX) & 1;
                int destIndex = (srcRow + destX) >> 1;
                int destShift = (srcRow + destX) & 1;

                // Handle different pixel packing scenarios
                if (sourceShift == 0) {
                    if (destShift == 0) {
                        // Both source and destination use high nibble
                        for(int col = 0; col < height; ++col) {
                            this.pixelData[destIndex][destY + col] =
                                    (byte)((this.pixelData[destIndex][destY + col] & 15) |
                                            (source[sourceIndex][srcY + col] & 240));
                        }
                    } else {
                        // Source high nibble -> destination low nibble
                        for(int col = 0; col < height; ++col) {
                            this.pixelData[destIndex][destY + col] =
                                    (byte)((this.pixelData[destIndex][destY + col] & 240) |
                                            ((source[sourceIndex][srcY + col] >> 4) & 15));
                        }
                    }
                } else if (destShift == 0) {
                    // Source low nibble -> destination high nibble
                    for(int col = 0; col < height; ++col) {
                        this.pixelData[destIndex][destY + col] =
                                (byte)((this.pixelData[destIndex][destY + col] & 15) |
                                        ((source[sourceIndex][srcY + col] << 4) & 240));
                    }
                } else {
                    // Both source and destination use low nibble
                    for(int col = 0; col < height; ++col) {
                        this.pixelData[destIndex][destY + col] =
                                (byte)((this.pixelData[destIndex][destY + col] & 240) |
                                        (source[sourceIndex][srcY + col] & 15));
                    }
                }
            }
        }
    }

    /**
     * Create color palettes from base palette with lighting variations
     * @param basePalette 16-color base palette
     * @return 16 palettes with different brightness levels
     */
    public static int[][] createColorPalettes(int[] basePalette) {
        return generateShadedPalettes(basePalette, basePalette.length);
    }

    /**
     * Generate 16 shaded palettes from base palette
     * Each palette has different brightness levels for lighting effects
     * @param basePalette Base color palette
     * @param paletteSize Number of colors in palette
     * @return Array of 16 shaded palettes
     */
    private static int[][] generateShadedPalettes(int[] basePalette, int paletteSize) {

        int[][] palettes = new int[16][paletteSize];

        for(int paletteIndex = 0; paletteIndex < 16; ++paletteIndex) {
            for(int colorIndex = 0; colorIndex < paletteSize; ++colorIndex) {
                // Extract RGB components
                float red = ((basePalette[colorIndex] & 0xFF0000) >> 16) / 255.0f;
                float green = ((basePalette[colorIndex] & 0xFF00) >> 8) / 255.0f;
                float blue = (basePalette[colorIndex] & 0xFF) / 255.0f;

                //to linear
                red = (float) Math.sqrt(red);
                green = (float) Math.sqrt(green);
                blue = (float) Math.sqrt(blue);

                float brightness = paletteIndex / 8.0f * 0.75f + 0.25f;

                red *= brightness;
                green *= brightness;
                blue *= brightness;

                //to srgb
                int red2 = (int) (red * red * 255);
                int green2 = (int) (green * green * 255);
                int blue2 = (int) (blue * blue * 255);

                // Clamp values to 0-255 range
                if (red2 < 0) red2 = 0;
                else if (red2 > 255) red2 = 255;

                if (green2 < 0) green2 = 0;
                else if (green2 > 255) green2 = 255;

                if (blue2 < 0) blue2 = 0;
                else if (blue2 > 255) blue2 = 255;

                // Combine with alpha channel
                palettes[paletteIndex][colorIndex] = 0xff000000 + (red2 << 16) + (green2 << 8) + blue2;
            }
        }

        return palettes;
    }


}