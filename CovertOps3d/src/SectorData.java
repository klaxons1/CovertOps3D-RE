public final class SectorData {

    public short sectorId;
    public short floorHeight;
    public short ceilingHeight;
    public byte  ceilingTextureId;
    public byte  floorTextureId;

    private short baseLightLevel;   // 0..16, stored as short
    private short sectorType;       // special type (door, elevator, damage, etc.)

    public boolean[] visitedFlags;     // PVS: true = already visited from current sector
    public Sprite    floorTexture;     // resolved after resource loading
    public Sprite    ceilingTexture;

    public int lightLevel;             // current light (cached)
    public int floorOffsetX;           // animated texture offset
    public int ceilingOffsetX;

    public SectorData(short id, short floorH, short ceilingH,
                      byte ceilTex, byte floorTex,
                      short light, short tag, short type) {
        this.sectorId         = id;
        this.floorHeight      = floorH;
        this.ceilingHeight    = ceilingH;
        this.ceilingTextureId = ceilTex;
        this.floorTextureId   = floorTex;
        this.baseLightLevel   = light;
        this.sectorType       = type;

        this.visitedFlags     = null;
    }

    /** Returns current light level (0-16) with screen-shake flash effect */
    public final int getLightLevel() {
        if (GameEngine.screenShake > 0) {
            int boosted = (baseLightLevel & 0xFFFF) + (GameEngine.screenShake >> 1);
            return (boosted > 16) ? 16 : boosted;
        }
        return baseLightLevel & 0xFFFF;
    }

    /** Returns sector special type */
    public final int getSectorType() {
        return sectorType & 0xFFFF;
    }

    /** Portal visibility test */
    public final boolean isSectorVisible(SectorData other) {
        return !other.visitedFlags[this.sectorId];
    }

    /** BSP leaf connectivity test */
    public final boolean isSectorConnected(Sector bspLeaf) {
        return !bspLeaf.visibilityMask[this.sectorId];
    }

    /** BSP subtree visibility test */
    public final boolean isBSPNodeVisible(BSPNode node) {
        return !node.visibleSectors[this.sectorId];
    }
}