public final class WallSurface {
   public byte upperTextureId;
   public byte mainTextureId;
   public byte lowerTextureId;
   private byte sectorLinkId;
   public SectorData linkedSector;
   public short textureOffsetX;
   public short textureOffsetY;

   public WallSurface(byte var1, byte var2, byte var3, byte var4, short var5, short var6) {
      this.upperTextureId = var1;
      this.mainTextureId = var2;
      this.lowerTextureId = var3;
      this.sectorLinkId = var4;
      this.textureOffsetX = var5;
      this.textureOffsetY = var6;
   }

   public final void resolveSectorLink(Class_3e6 var1) {
      this.linkedSector = var1.var_32c[this.sectorLinkId & 255];
   }
}
