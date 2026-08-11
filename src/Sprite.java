/**
 * Sprite class for handling game sprites (objects, items, UI elements)
 */
public final class Sprite {
    public byte[] pixelData;     // Sprite pixel data
    public int[][] colorPalettes; // Color palettes for different lighting conditions

    // One representative color for each lighting level. Used only by the
    // optional flat-floor quality mode, where it replaces costly texture
    // mapping while retaining the sector's distance lighting.
    public int[] flatColors;

    public byte spriteId;        // Unique identifier for this sprite

    /**
     * Create an empty sprite (placeholder)
     * @param spriteId Unique identifier for the sprite
     */
    public Sprite(byte spriteId) {
        this.spriteId = spriteId;
        this.pixelData = null;
        this.colorPalettes = null;
    }

    /**
     * Create a sprite from pixel data with 90-degree rotation
     * Sprites are stored rotated for efficient rendering
     * @param spriteId Unique identifier for the sprite
     * @param sourcePixels
     */
    public Sprite(byte spriteId, byte[] sourcePixels) {
        this.spriteId = spriteId;
        this.pixelData = new byte[4096];

        // Rotate sprite 90 degrees clockwise for rendering optimization
        for(int x = 0; x < 64; ++x) {
            for(int y = 0; y < 64; ++y) {
                // Original: sourcePixels[y + (x << 6)]
                // Rotated:  pixelData[x + (63 - y << 6)]
                this.pixelData[x + ((63 - y) << 6)] = sourcePixels[y + (x << 6)];
            }
        }
    }

    /**
     * Builds approximate flat colors from sampled texels. This runs only at
     * level load and lets the optional performance mode fill floors/ceilings
     * without per-pixel texture-coordinate math.
     */
    public final void buildFlatColors() {
        if (pixelData == null || colorPalettes == null) {
            flatColors = null;
            return;
        }

        int levels = colorPalettes.length;
        int[] colors = new int[levels];
        int sampleStep = pixelData.length > 256 ? pixelData.length / 256 : 1;

        for (int level = 0; level < levels; ++level) {
            int[] palette = colorPalettes[level];
            int red = 0;
            int green = 0;
            int blue = 0;
            int samples = 0;

            for (int pixel = 0; pixel < pixelData.length; pixel += sampleStep) {
                int colorIndex = pixelData[pixel] & 0xFF;
                if (colorIndex < palette.length) {
                    int color = palette[colorIndex];
                    red += (color >> 16) & 0xFF;
                    green += (color >> 8) & 0xFF;
                    blue += color & 0xFF;
                    samples++;
                }
            }

            if (samples == 0) {
                colors[level] = 0xFF000000;
            } else {
                colors[level] = 0xFF000000
                        | ((red / samples) << 16)
                        | ((green / samples) << 8)
                        | (blue / samples);
            }
        }

        flatColors = colors;
    }
}
