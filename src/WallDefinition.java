public final class WallDefinition {

    public short startVertexId;
    public short endVertexId;

    public Point2D normalVector;

    private short frontSurfaceId;
    private short backSurfaceId;

    public WallSurface frontSurface;
    public WallSurface backSurface;

    private byte wallFlags;
    private byte wallType;
    private byte specialType;

    // Bit masks for wallFlags
    private static final byte FLAG_SOLID       = 1;    // 0x01
    private static final byte FLAG_PASSABLE    = 2;    // 0x02 (used by isPassable)
    private static final byte FLAG_TRANSPARENT= 4;    // 0x04
    private static final byte FLAG_DOOR        = 8;    // 0x08
    private static final byte FLAG_SECRET      = 16;   // 0x10
    private static final byte FLAG_RENDERED    = -128; // 0x80

    public WallDefinition(short startVertexId, short endVertexId,
                          short frontSurfaceId, short backSurfaceId,
                          byte flags, byte type, byte special) {
        this.startVertexId   = startVertexId;
        this.endVertexId     = endVertexId;
        this.frontSurfaceId  = frontSurfaceId;
        this.backSurfaceId   = backSurfaceId;
        this.wallFlags       = flags;
        this.wallType        = type;
        this.specialType     = special;

        this.normalVector = new Point2D(0, 0);
    }

    public final int getWallType() {
        return wallType & 0xFF;
    }

    public final int getSpecialType() {
        return specialType & 0xFF;
    }

    /** Resolves surface references and pre-computes normal vector after level load */
    public final void initializeWall(GameWorld world) {
        if (frontSurfaceId == -1) {
            throw new IllegalArgumentException("Wall has no front surface");
        }

        this.frontSurface = world.wallSurfaces[frontSurfaceId & 0xFFFF];

        if (backSurfaceId == -1) {
            this.backSurface = null;
        } else {
            this.backSurface = world.wallSurfaces[backSurfaceId & 0xFFFF];
        }

        Point2D v1 = world.vertices[startVertexId & 0xFFFF];
        Point2D v2 = world.vertices[endVertexId   & 0xFFFF];

        int dy = v1.y - v2.y;
        int dx = v2.x - v1.x;

        int length = MathUtils.preciseHypot(dx, dy);

        this.normalVector.x = MathUtils.fixedPointDivide(dy, length);
        this.normalVector.y = MathUtils.fixedPointDivide(dx, length);
    }

    public final boolean isPassable() {
        return (wallFlags & 3) != 0;
    }

    public final boolean isCollidable() {
        return (wallFlags & 67) != 0;
    }

    public final boolean isSolid() {
        return (wallFlags & FLAG_SOLID) != 0;
    }

    public final boolean isTransparent() {
        return (wallFlags & FLAG_TRANSPARENT) != 0;
    }

    public final boolean isDoor() {
        return (wallFlags & FLAG_DOOR) != 0;
    }

    public final boolean isSecret() {
        return (wallFlags & FLAG_SECRET) != 0;
    }

    public final boolean isRendered() {
        return (wallFlags & FLAG_RENDERED) != 0;
    }

    public final void markAsRendered() {
        wallFlags = (byte)(wallFlags | FLAG_RENDERED);
    }
}