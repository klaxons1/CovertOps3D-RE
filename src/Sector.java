import java.util.Vector;

public final class Sector {

    private short wallCount;
    private short wallArrayOffset;

    public WallSegment[] walls;

    // Global clip arrays used by the renderer
    public static short[] ceilingClip;
    public static short[] floorClip;

    // MascotME's renderer uses bulk copies for repeat framebuffer clears on
    // devices where native arraycopy outperforms a Java per-pixel loop. The
    // same applies to these fixed-size per-frame clip resets.
    private static short[] initialFloorClip;
    private static short[] initialCeilingClip;

    public Vector dynamicObjects;
    public boolean[] visibilityMask;

    public Sector(short wallCount, short wallArrayOffset) {
        this.wallCount = wallCount;
        this.wallArrayOffset = wallArrayOffset;
        this.dynamicObjects = new Vector();
    }

    /**
     * Returns true when every screen column is fully occluded (nothing left to draw).
     */
    public static boolean isRenderComplete() {
        int viewportWidth = PortalRenderer.VIEWPORT_WIDTH;
        for (int x = 0; x < viewportWidth; x++) {
            if (floorClip[x] < ceilingClip[x]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets clip arrays to initial state before rendering a new frame.
     */
    public static void resetClipArrays() {
        int viewportWidth = PortalRenderer.VIEWPORT_WIDTH;

        if (floorClip == null) {
            floorClip = new short[viewportWidth];
            ceilingClip = new short[viewportWidth];
        }
        if (initialFloorClip == null) {
            initialFloorClip = new short[viewportWidth];
            initialCeilingClip = new short[viewportWidth];

            short maxViewportY = (short)PortalRenderer.MAX_VIEWPORT_Y;
            for (int x = 0; x < viewportWidth; x++) {
                initialCeilingClip[x] = maxViewportY;
            }
        }

        // Keep the old values and bounds exactly, but hand the fixed-size
        // copies to the VM's optimized arraycopy implementation instead of
        // performing 480 Java assignments every frame.
        System.arraycopy(initialFloorClip, 0, floorClip, 0, viewportWidth);
        System.arraycopy(initialCeilingClip, 0, ceilingClip, 0, viewportWidth);
    }

    public final SectorData getSectorData() {
        return this.walls[0].getWallSector();
    }

    public final void clearDynamicObjects() {
        this.dynamicObjects.removeAllElements();
    }

    public final void addDynamicObject(GameObject object) {
        this.dynamicObjects.addElement(object);
    }

    public final void initializeWalls(GameWorld world) {
        int count = this.wallCount & 0xFFFF;
        this.walls = new WallSegment[count];

        int baseIndex = this.wallArrayOffset & 0xFFFF;
        for (int i = 0; i < count; i++) {
            this.walls[i] = world.wallSegments[baseIndex + i];
        }
    }

    public final boolean[] getVisibilityMask() {
        this.visibilityMask = this.walls[0].getWallSector().visitedFlags;
        return this.visibilityMask;
    }
}