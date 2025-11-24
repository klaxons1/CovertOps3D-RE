public final class WallSegment {
   private short wallDefinitionIndex;
   public short startVertexIndex;
   public short endVertexIndex;
   public boolean isFrontFacing;
   public WallDefinition wallDefinition;
   private WallSurface sectorLink;
   public short textureOffset;

   public WallSegment(short var1, short var2, short var3, boolean var4, short var5) {
      this.startVertexIndex = var1;
      this.endVertexIndex = var2;
      this.wallDefinitionIndex = var3;
      this.isFrontFacing = var4;
      this.textureOffset = var5;
   }

   public final void initializeWallSegment(GameWorld var1) {
      this.wallDefinition = var1.wallDefinitions[this.wallDefinitionIndex & '\uffff'];
      WallSegment var10000;
      WallSurface var10001;
      if (this.isFrontFacing) {
         var10000 = this;
         var10001 = this.wallDefinition.frontSurface;
      } else {
         var10000 = this;
         var10001 = this.wallDefinition.backSurface;
      }

      var10000.sectorLink = var10001;
   }

   public final SectorData getWallSector() {
      return this.sectorLink.linkedSector;
   }
}
