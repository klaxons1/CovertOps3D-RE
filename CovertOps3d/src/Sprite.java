/**
 * Sprite class for handling game sprites (objects, items, UI elements)
 */
public final class Sprite {
    public byte[] pixelData;     // Sprite pixel data
    public int[][] colorPalettes; // Color palettes for different lighting conditions
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
}