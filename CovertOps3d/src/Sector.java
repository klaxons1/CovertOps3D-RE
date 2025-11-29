import java.util.Vector;

public final class Sector {

    private short wallCount;        // Number of walls in this sector
    private short wallArrayOffset;  // Offset in global wallSegments[] array

    public WallSegment[] walls;     // Resolved array of wall segments belonging to this sector

    // Global clip arrays used by the renderer (240 columns = screen width)
    public static short[] ceilingClip;  // Highest rendered pixel per column (inclusive)
    public static short[] floorClip;    // Lowest rendered pixel per column (inclusive)

    public Vector dynamicObjects;   // Enemies, items, projectiles currently in this sector

    public boolean[] visibilityMask; // Cached visibility mask from current sector

    public Sector(short wallCount, short wallArrayOffset) {
        this.wallCount       = wallCount;
        this.wallArrayOffset = wallArrayOffset;
        this.dynamicObjects  = new Vector();
    }

    /** Returns true when every screen column is fully occluded (nothing left to draw) */
    public static boolean isRenderComplete() {
        for (int x = 0; x < 240; x++) {
            if (floorClip[x] < ceilingClip[x]) {
                return false; // there is still a visible vertical span in this column
            }
        }
        return true;
    }

    /** Resets clip arrays to initial state before rendering a new frame */
    public static void resetClipArrays() {
        if (floorClip == null) {
            floorClip    = new short[240];
            ceilingClip  = new short[240];
        }

        for (int x = 0; x < 240; x++) {
            floorClip[x]   = 0;     // top of the screen
            ceilingClip[x] = 287;   // bottom of the screen (288 pixels, 240x288)
        }
    }

    /** Returns the SectorData structure associated with this BSP leaf */
    public final SectorData getSectorData() {
        // All walls in a sector share the same SectorData
        return this.walls[0].getWallSector();
    }

    /** Removes all dynamic objects from this sector (called on level restart etc.) */
    public final void clearDynamicObjects() {
        this.dynamicObjects.removeAllElements();
    }

    /** Adds a moving object (enemy, pickup, projectile) to this sector */
    public final void addDynamicObject(GameObject object) {
        this.dynamicObjects.addElement(object);
    }

    /**
     * Called after level loading — resolves wall indices into actual WallSegment references
     */
    public final void initializeWalls(GameWorld world) {
        int count = this.wallCount & 0xFFFF;
        this.walls = new WallSegment[count];

        int baseIndex = this.wallArrayOffset & 0xFFFF;
        for (int i = 0; i < count; i++) {
            this.walls[i] = world.wallSegments[baseIndex + i];
        }
    }

    /**
     * Returns (and caches) the visited-flags array from the SectorData.
     * Used for portal visibility culling.
     */
    public final boolean[] getVisibilityMask() {
        this.visibilityMask = this.walls[0].getWallSector().visitedFlags;
        return this.visibilityMask;
    }
}