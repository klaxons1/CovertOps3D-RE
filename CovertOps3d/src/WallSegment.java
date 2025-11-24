public final class WallSegment {
   private short wallDefinitionIndex;
   public short startVertexIndex;
   public short endVertexIndex;
   public boolean isFrontFacing;
   public Class_1e1 wallDefinition;
   private WallSurface sectorLink;
   public short textureOffset;

   public WallSegment(short var1, short var2, short var3, boolean var4, short var5) {
      this.startVertexIndex = var1;
      this.endVertexIndex = var2;
      this.wallDefinitionIndex = var3;
      this.isFrontFacing = var4;
      this.textureOffset = var5;
   }

   public final void initializeWallSegment(Class_3e6 var1) {
      this.wallDefinition = var1.var_210[this.wallDefinitionIndex & '\uffff'];
      WallSegment var10000;
      WallSurface var10001;
      if (this.isFrontFacing) {
         var10000 = this;
         var10001 = this.wallDefinition.var_e5;
      } else {
         var10000 = this;
         var10001 = this.wallDefinition.var_133;
      }

      var10000.sectorLink = var10001;
   }

   public final SectorData getWallSector() {
      return this.sectorLink.linkedSector;
   }
}
