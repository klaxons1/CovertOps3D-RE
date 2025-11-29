/**
 * Physics body that extends 3D transform with velocity and physics behavior
 * Handles movement, forces, damping, and velocity thresholds
 */
public final class PhysicsBody extends Transform3D {
    // Velocity components in 3D space
    public int velocityX;        // Horizontal velocity (right/left)
    private int velocityZ;       // Vertical velocity (up/down) - typically gravity
    public int velocityY;        // Horizontal velocity (forward/backward)
    private int angularVelocity; // Rotational velocity

    // Velocity threshold below which movement is considered zero
    private static final int VELOCITY_THRESHOLD = 65;

    /**
     * Constructor for physics body with position and rotation only
     */
    public PhysicsBody(int posX, int posY, int posZ, int rotation) {
        this(posX, posY, posZ, rotation, 0, 0, 0, 0);
    }

    /**
     * Private full constructor with all velocity parameters
     */
    private PhysicsBody(int posX, int posY, int posZ, int rotation,
                        int velX, int velZ, int velY, int angularVel) {
        super(posX, posY, posZ, rotation);
        this.velocityX = velX;
        this.velocityZ = velZ;
        this.velocityY = velY;
        this.angularVelocity = angularVel;
    }

    /**
     * Applies full velocity to transform position (called every frame)
     */
    public final void applyVelocity() {
        this.applyMovement(this.velocityX, this.velocityZ, this.velocityY, this.angularVelocity);
    }

    /**
     * Applies damped velocity (50% reduction) for friction/water/knockback effects
     */
    public final void applyDampedVelocity() {
        this.applyMovement(this.velocityX >> 1,
                this.velocityZ >> 1,
                this.velocityY >> 1,
                this.angularVelocity >> 1);
    }

    /**
     * Scales velocity components by given factors and applies threshold cutoff
     * Used for friction, air resistance, or other velocity modifications
     */
    public final void scaleVelocity(int scaleX, int scaleZ, int scaleY, int scaleRot) {
        this.velocityX = MathUtils.fixedPointMultiply(this.velocityX, scaleX);
        this.velocityZ = MathUtils.fixedPointMultiply(this.velocityZ, scaleZ);
        this.velocityY = MathUtils.fixedPointMultiply(this.velocityY, scaleY);
        this.angularVelocity = MathUtils.fixedPointMultiply(this.angularVelocity, scaleRot);

        // Apply velocity threshold - set to zero if below threshold
        // Note: Duplicate velocityX check appears in original game code
        if (this.velocityX < VELOCITY_THRESHOLD && this.velocityX > -VELOCITY_THRESHOLD) {
            this.velocityX = 0;
        }
        if (this.velocityX < VELOCITY_THRESHOLD && this.velocityX > -VELOCITY_THRESHOLD) {
            this.velocityX = 0;
        }
        if (this.velocityY < VELOCITY_THRESHOLD && this.velocityY > -VELOCITY_THRESHOLD) {
            this.velocityY = 0;
        }
        if (this.angularVelocity < VELOCITY_THRESHOLD && this.angularVelocity > -VELOCITY_THRESHOLD) {
            this.angularVelocity = 0;
        }
        // Note: velocityZ intentionally not zeroed by threshold - used for persistent vertical forces like gravity
    }

    /**
     * Applies horizontal force relative to object's rotation (forward/strafe only)
     */
    public final void applyHorizontalForce(int forward, int strafe) {
        this.applyForce(forward, strafe, 0);
    }

    /**
     * Applies force in local object coordinates, transformed to world coordinates
     * @param forward Force in forward/backward direction (local space)
     * @param strafe Force in left/right direction (local space) 
     * @param torque Rotational force
     */
    public final void applyForce(int forward, int strafe, int torque) {
        int sin = MathUtils.fastSin(super.rotation);
        int cos = MathUtils.fastCos(super.rotation);

        // Transform local forces to world coordinates and apply to velocities
        this.velocityX += MathUtils.fixedPointMultiply(cos, forward)
                - MathUtils.fixedPointMultiply(sin, strafe);

        this.velocityY += MathUtils.fixedPointMultiply(-sin, forward)
                - MathUtils.fixedPointMultiply(cos, strafe);

        this.angularVelocity += torque;
    }

    /**
     * Gets vertical velocity (typically used for gravity/jumping)
     */
    public final int getVelocityZ() {
        return velocityZ;
    }

    /**
     * Sets vertical velocity (for gravity, jumping, etc.)
     */
    public final void setVelocityZ(int velocityZ) {
        this.velocityZ = velocityZ;
    }

    /**
     * Gets angular velocity for rotation
     */
    public final int getAngularVelocity() {
        return angularVelocity;
    }

    /**
     * Sets angular velocity
     */
    public final void setAngularVelocity(int angularVelocity) {
        this.angularVelocity = angularVelocity;
    }
}