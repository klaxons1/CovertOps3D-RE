import java.util.Vector;

public final class Sector {

    private short wallCount;
    private short wallArrayOffset;

    public WallSegment[] walls;

    // Global clip arrays used by the renderer
    public static short[] ceilingClip;
    public static short[] floorClip;

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
        int maxViewportY = PortalRenderer.MAX_VIEWPORT_Y;

        if (floorClip == null) {
            floorClip = new short[viewportWidth];
            ceilingClip = new short[viewportWidth];
        }

        for (int x = 0; x < viewportWidth; x++) {
            floorClip[x] = 0;
            ceilingClip[x] = (short)maxViewportY;
        }
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