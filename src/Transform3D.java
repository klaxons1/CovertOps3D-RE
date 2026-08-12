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

        // The normal game path crosses at most one turn, so retain the cheap
        // add/subtract path there.  A loaded or scripted transform can carry
        // several turns, however; the old while-loops made that case take an
        // unbounded number of iterations on the game thread.
        int rotation = this.rotation + deltaRotation;
        if (rotation >= FULL_CIRCLE) {
            if (rotation >= FULL_CIRCLE + FULL_CIRCLE) {
                rotation %= FULL_CIRCLE;
            } else {
                rotation -= FULL_CIRCLE;
            }
        } else if (rotation < 0) {
            if (rotation <= -FULL_CIRCLE) {
                rotation %= FULL_CIRCLE;
            }
            if (rotation < 0) {
                rotation += FULL_CIRCLE;
            }
        }
        this.rotation = rotation;
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