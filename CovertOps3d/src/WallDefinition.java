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

   public WallDefinition(short var1, short var2, short var3, short var4, byte var5, byte var6, byte var7) {
      this.startVertexId = var1;
      this.endVertexId = var2;
      this.normalVector = new Point2D(0, 0);
      this.frontSurfaceId = var3;
      this.backSurfaceId = var4;
      this.wallFlags = var5;
      this.wallType = var6;
      this.specialType = var7;
   }

   public final int getWallType() {
      return this.wallType & '\uffff';
   }

   public final int getSpecialType() {
      return this.specialType & '\uffff';
   }

   public final void initializeWall(GameWorld var1) {
      if (this.frontSurfaceId == -1) {
         throw new IllegalArgumentException();
      } else {
         this.frontSurface = var1.wallSurfaces[this.frontSurfaceId & '\uffff'];
         WallDefinition var10000;
         WallSurface var10001;
         if (this.backSurfaceId == -1) {
            var10000 = this;
            var10001 = null;
         } else {
            var10000 = this;
            var10001 = var1.wallSurfaces[this.backSurfaceId & '\uffff'];
         }

         var10000.backSurface = var10001;
         Point2D var2 = var1.vertices[this.startVertexId & '\uffff'];
         Point2D var3 = var1.vertices[this.endVertexId & '\uffff'];
         int var4 = var2.y - var3.y;
         int var5 = var3.x - var2.x;
         int var6 = MathUtils.preciseHypot(var4, var5);
         this.normalVector.x = MathUtils.fixedPointDivide(var4, var6);
         this.normalVector.y = MathUtils.fixedPointDivide(var5, var6);
      }
   }

   public final boolean isPassable() {
      return (this.wallFlags & 3) > 0;
   }

   public final boolean isCollidable() {
      return (this.wallFlags & 67) > 0;
   }

   public final boolean isSolid() {
      return (this.wallFlags & 1) > 0;
   }

   public final boolean isTransparent() {
      return (this.wallFlags & 4) > 0;
   }

   public final boolean isDoor() {
      return (this.wallFlags & 8) > 0;
   }

   public final boolean isSecret() {
      return (this.wallFlags & 16) > 0;
   }

   public final boolean isRendered() {
      return (this.wallFlags & 128) > 0;
   }

   public final void markAsRendered() {
      this.wallFlags = (byte)(this.wallFlags | 128);
   }
}
