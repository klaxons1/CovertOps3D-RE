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

    // Palette level 8 is the authored texture color. The remaining levels
    // describe exposure in approximately linear light: three stops below the
    // authored exposure at level 0 and two stops above it at level 15.
    // Keeping this as a small table makes the curve deterministic on CLDC 1.1
    // and costs nothing while rendering (the renderer still just selects a row).
    private static final int[] LIGHT_EXPOSURES = new int[]{
            32, 42, 54, 70, 91, 118, 152, 198,
            256, 312, 380, 464, 566, 690, 841, 1024
    };
    private static final int NEUTRAL_LIGHT_LEVEL = 8;
    private static final int NORMAL_EXPOSURE = 256;
    private static final int DISPLAY_WHITE = 255;
    private static final int LINEAR_WHITE = DISPLAY_WHITE * DISPLAY_WHITE;

    // Approximate gamma-2 encoding of a linear-light channel. The small LUT
    // is interpolated for high values; the low range uses an exact integer
    // square root so dim one-digit channels do not disappear to quantization.
    private static byte[] linearToDisplay;

    // All curve work is cached once as 16 x 256 bytes. Palette generation then
    // consists only of three lookups per source color, while frame rendering
    // keeps its original single palette-row lookup per surface.
    private static byte[][] shadedChannels;

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
        // Handle wrap-around for repeating textures.
        // Java % truncates toward zero, so one modulo plus a conditional add
        // produces exactly the non-negative residue the old while-loop
        // converged to (identical modular arithmetic), just without looping.
        if ((row %= this.width) < 0) {
            row += this.width;
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
     * Creates 16 pre-shaded palettes from an authored palette.
     *
     * The old implementation added the same value to R, G and B. That makes
     * dark colors collapse to black and clips bright colors to white, which
     * changes their hue. Here the authored level (8) is preserved exactly;
     * the other rows are exposed in approximate linear light and use a soft
     * highlight roll-off above the neutral exposure.
     *
     * @param basePalette authored RGB/ARGB colors
     * @return 16 palette rows, indexed directly by the renderer
     */
    public static int[][] createColorPalettes(int[] basePalette) {
        int paletteSize = basePalette.length;
        int[][] palettes = new int[LIGHT_EXPOSURES.length][paletteSize];
        byte[][] channelTable = getShadedChannels();

        for (int paletteIndex = 0; paletteIndex < LIGHT_EXPOSURES.length; ++paletteIndex) {
            int[] shadedPalette = palettes[paletteIndex];

            if (paletteIndex == NEUTRAL_LIGHT_LEVEL) {
                // Do not introduce even a one-unit gamma rounding error at
                // the neutral light used by most authored sectors.
                for (int colorIndex = 0; colorIndex < paletteSize; ++colorIndex) {
                    shadedPalette[colorIndex] = 0xFF000000 | (basePalette[colorIndex] & 0xFFFFFF);
                }
                continue;
            }

            byte[] channelShade = channelTable[paletteIndex];
            for (int colorIndex = 0; colorIndex < paletteSize; ++colorIndex) {
                int color = basePalette[colorIndex];
                int red = channelShade[(color >> 16) & 0xFF] & 0xFF;
                int green = channelShade[(color >> 8) & 0xFF] & 0xFF;
                int blue = channelShade[color & 0xFF] & 0xFF;
                shadedPalette[colorIndex] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        }

        return palettes;
    }

    /**
     * Converts a gamma-encoded channel to approximate linear light, applies
     * exposure, then returns to display space. Above normal exposure a
     * normalized Reinhard-style curve protects highlights from hard clipping.
     */
    private static int shadeChannel(int channel, int exposure, byte[] displayTable) {
        int linear = channel * channel;
        int shadedLinear;

        if (exposure <= NORMAL_EXPOSURE) {
            shadedLinear = (linear * exposure + (NORMAL_EXPOSURE >> 1)) >> 8;
        } else {
            int denominator = LINEAR_WHITE * NORMAL_EXPOSURE
                    + linear * (exposure - NORMAL_EXPOSURE);
            shadedLinear = (int)(((long)linear * (long)exposure * (long)LINEAR_WHITE
                    + (denominator >> 1)) / denominator);
        }

        if (shadedLinear < 0) shadedLinear = 0;
        else if (shadedLinear > LINEAR_WHITE) shadedLinear = LINEAR_WHITE;
        return encodeLinearChannel(shadedLinear, displayTable);
    }

    /**
     * Encodes gamma-2 linear light. A 256-byte table plus interpolation gives
     * a close result for normal/high values; the small dark range stays exact
     * so low-color palette entries retain their detail.
     */
    private static int encodeLinearChannel(int linear, byte[] displayTable) {
        if (linear <= DISPLAY_WHITE) {
            return roundedSquareRoot(linear);
        }

        int segment = linear / DISPLAY_WHITE;
        int remainder = linear - segment * DISPLAY_WHITE;
        int low = displayTable[segment] & 0xFF;
        if (remainder == 0 || segment == DISPLAY_WHITE) {
            return low;
        }

        int high = displayTable[segment + 1] & 0xFF;
        return low + ((high - low) * remainder + (DISPLAY_WHITE >> 1)) / DISPLAY_WHITE;
    }

    /** Builds the small inverse-gamma table lazily, once per VM. */
    private static byte[] getLinearToDisplayTable() {
        if (linearToDisplay == null) {
            byte[] table = new byte[DISPLAY_WHITE + 1];
            for (int segment = 0; segment <= DISPLAY_WHITE; ++segment) {
                table[segment] = (byte)roundedSquareRoot(segment * DISPLAY_WHITE);
            }
            linearToDisplay = table;
        }
        return linearToDisplay;
    }

    /** Builds the 16 x 256 lighting curve once; later palettes only look it up. */
    private static byte[][] getShadedChannels() {
        if (shadedChannels == null) {
            byte[][] table = new byte[LIGHT_EXPOSURES.length][];
            byte[] displayTable = getLinearToDisplayTable();

            for (int paletteIndex = 0; paletteIndex < LIGHT_EXPOSURES.length; ++paletteIndex) {
                byte[] row = new byte[DISPLAY_WHITE + 1];
                int exposure = LIGHT_EXPOSURES[paletteIndex];
                for (int channel = 0; channel <= DISPLAY_WHITE; ++channel) {
                    row[channel] = (byte)shadeChannel(channel, exposure, displayTable);
                }
                table[paletteIndex] = row;
            }
            shadedChannels = table;
        }
        return shadedChannels;
    }

    /** Rounded integer square root for 0..255 squared; no floating point needed. */
    private static int roundedSquareRoot(int value) {
        int root = integerSquareRoot(value);
        int next = root + 1;
        if (next <= DISPLAY_WHITE && value - root * root >= next * next - value) {
            return next;
        }
        return root;
    }

    /** Standard bitwise integer square root, bounded to an 8-bit result here. */
    private static int integerSquareRoot(int value) {
        int result = 0;
        int bit = 1 << 14; // Largest power of four not exceeding 255 * 255.

        while (bit > value) {
            bit >>= 2;
        }
        while (bit != 0) {
            int trial = result + bit;
            if (value >= trial) {
                value -= trial;
                result = (result >> 1) + bit;
            } else {
                result >>= 1;
            }
            bit >>= 2;
        }
        return result;
    }
}
