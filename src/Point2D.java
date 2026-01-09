/**
 * Simple 2D point with fixed-point coordinates (16.16 format).
 * Used everywhere in the engine: vertices, normals, texture offsets, etc.
 */
public final class Point2D {

    /** X coordinate in fixed-point 16.16 format (integer part in high 16 bits) */
    public int x;

    /** Y coordinate in fixed-point 16.16 format (in world space Y is actually height/Z in Wolf3D style) */
    public int y;

    /**
     * Constructs a new 2D point.
     *
     * @param x  X coordinate (fixed-point)
     * @param y  Y coordinate (fixed-point)
     */
    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }
}