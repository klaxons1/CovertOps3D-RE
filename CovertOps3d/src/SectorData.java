public final class SectorData {
   public short sectorId;
   public short floorHeight;
   public short ceilingHeight;
   public byte ceilingTextureId;
   public byte floorTextureId;
   private short baseLightLevel;
   private short sectorType;
   public boolean[] visitedFlags;
   public Sprite floorTexture;
   public Sprite ceilingTexture;
   public int lightLevel;
   public int floorOffsetX;
   public int ceilingOffsetX;

   public SectorData(short var1, short var2, short var3, byte var4, byte var5, short var6, short var7, short var8) {
      this.sectorId = var1;
      this.floorHeight = var2;
      this.ceilingHeight = var3;
      this.ceilingTextureId = var4;
      this.floorTextureId = var5;
      this.baseLightLevel = var6;
      this.sectorType = var8;
      this.visitedFlags = null;
   }

   public final int getLightLevel() {
      if (GameEngine.screenShake > 0) {
         int var1;
         if ((var1 = (this.baseLightLevel & '\uffff') + (GameEngine.screenShake >> 1)) > 16) {
            var1 = 16;
         }

         return var1;
      } else {
         return this.baseLightLevel & '\uffff';
      }
   }

   public final int getSectorType() {
      return this.sectorType & '\uffff';
   }

   public final boolean isSectorVisible(SectorData var1) {
      return !var1.visitedFlags[this.sectorId];
   }

   public final boolean isSectorConnected(Sector var1) {
      return !var1.visibilityMask[this.sectorId];
   }

   public final boolean isBSPNodeVisible(BSPNode var1) {
      return !var1.visibleSectors[this.sectorId];
   }
}
