public final class WallSegment {

    private short wallDefinitionIndex;   // Index in GameWorld.wallDefinitions[]
    public  short startVertexIndex;      // Index of start vertex (for rendering order)
    public  short endVertexIndex;        // Index of end vertex

    public boolean isFrontFacing;        // true = we see front side of wall, false = back side

    public WallDefinition wallDefinition; // Resolved after initializeWallSegment()
    private WallSurface   sectorLink;     // Surface that links to adjacent sector (portal)
    public short          textureOffset;  // Horizontal texture offset (for doors, switches, etc.)

    /**
     * Constructor used during level loading.
     *
     * @param startVertex   start vertex index
     * @param endVertex     end vertex index
     * @param defIndex      index of WallDefinition
     * @param frontFacing   true if this segment belongs to front side of the wall
     * @param texOffset     texture X offset (animated doors, etc.)
     */
    public WallSegment(short startVertex, short endVertex,
                       short defIndex, boolean frontFacing,
                       short texOffset) {
        this.startVertexIndex     = startVertex;
        this.endVertexIndex       = endVertex;
        this.wallDefinitionIndex  = defIndex;
        this.isFrontFacing        = frontFacing;
        this.textureOffset        = texOffset;
    }

    /**
     * Called after level load: resolves references to WallDefinition
     * and determines which WallSurface connects to the adjacent sector.
     */
    public final void initializeWallSegment(GameWorld world) {
        this.wallDefinition = world.wallDefinitions[wallDefinitionIndex & 0xFFFF];

        if (isFrontFacing) {
            this.sectorLink = this.wallDefinition.frontSurface;
        } else {
            this.sectorLink = this.wallDefinition.backSurface;
        }
    }

    /**
     * Returns the SectorData of the sector on the other side of this wall segment.
     * Used heavily by the renderer and portal system.
     */
    public final SectorData getWallSector() {
        return this.sectorLink.linkedSector;
    }
}