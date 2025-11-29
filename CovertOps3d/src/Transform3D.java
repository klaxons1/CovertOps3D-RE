public class Transform3D {

    public int x;        // X position in world (fixed-point 16.16)
    public int y;        // Y position (height) in world (fixed-point 16.16)
    public int z;        // Z position in world (fixed-point 16.16)
    public int rotation; // Rotation in fixed-point degrees (0..411774 = 0..359.999°)

    // 360 degrees in fixed-point format: 360 * 65536 / 360 = 65536 * (360/360) + fraction
    private static final int FULL_CIRCLE = 411775; // approximately 360 * 114.59 (actual value used in game)

    public Transform3D(int x, int y, int z, int rotation) {
        this.x        = x;
        this.y        = y;
        this.z        = z;
        this.rotation = rotation;
    }

    /**
     * Adds absolute movement deltas and angular velocity.
     * Rotation is normalized to [0..FULL_CIRCLE).
     */
    public final void applyMovement(int deltaX, int deltaY, int deltaZ, int deltaRotation) {
        this.x += deltaX;
        this.y += deltaY;
        this.z += deltaZ;

        // Add angular velocity and wrap around 360 degrees
        this.rotation += deltaRotation;

        // Normalize positive overflow
        while (this.rotation >= FULL_CIRCLE) {
            this.rotation -= FULL_CIRCLE;
        }

        // Normalize negative values
        while (this.rotation < 0) {
            this.rotation += FULL_CIRCLE;
        }
    }

    /** Copies position and rotation from another transform */
    public final void copyFrom(Transform3D source) {
        this.x        = source.x;
        this.y        = source.y;
        this.z        = source.z;
        this.rotation = source.rotation;
    }

    /** Sets absolute position and rotation */
    public final void setPosition(int x, int y, int z, int rotation) {
        this.x        = x;
        this.y        = y;
        this.z        = z;
        this.rotation = rotation;
    }

    /**
     * Moves the object relative to its current facing direction.
     *
     * @param forward  positive = forward, negative = backward
     * @param strafe   positive = right, negative = left
     */
    public final void moveRelative(int forward, int strafe) {
        int sin = MathUtils.fastSin(this.rotation);
        int cos = MathUtils.fastCos(this.rotation);

        // X movement: cos(forward) - sin(strafe)
        this.x += MathUtils.fixedPointMultiply(cos, forward)
                - MathUtils.fixedPointMultiply(sin, strafe);

        // Z movement: -sin(forward) - cos(strafe)
        this.z += MathUtils.fixedPointMultiply(-sin, forward)
                - MathUtils.fixedPointMultiply(cos, strafe);
    }
}