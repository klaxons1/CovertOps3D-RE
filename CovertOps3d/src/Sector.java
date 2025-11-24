import java.util.Vector;

public final class Sector {
   private short wallCount;
   private short wallArrayOffset;
   public WallSegment[] walls;
   public static short[] ceilingClip;
   public static short[] floorClip;
   public Vector dynamicObjects;
   public boolean[] visibilityMask;

   public Sector(short var1, short var2) {
      this.wallCount = var1;
      this.wallArrayOffset = var2;
      this.dynamicObjects = new Vector();
   }

   public static boolean isRenderComplete() {
      for(int var0 = 0; var0 < 240; ++var0) {
         if (floorClip[var0] < ceilingClip[var0]) {
            return false;
         }
      }

      return true;
   }

   public static void resetClipArrays() {
      if (floorClip == null) {
         floorClip = new short[240];
         ceilingClip = new short[240];
      }

      for(int var0 = 0; var0 < 240; ++var0) {
         floorClip[var0] = 0;
         ceilingClip[var0] = 287;
      }

   }

   public final SectorData getSectorData() {
      return this.walls[0].getWallSector();
   }

   public final void clearDynamicObjects() {
      this.dynamicObjects.removeAllElements();
   }

   public final void addDynamicObject(GameObject var1) {
      this.dynamicObjects.addElement(var1);
   }

   public final void initializeWalls(Class_3e6 var1) {
      this.walls = new WallSegment[this.wallCount & '\uffff'];

      for(int var2 = 0; var2 < this.walls.length; ++var2) {
         this.walls[var2] = var1.var_433[(this.wallArrayOffset & '\uffff') + var2];
      }

   }

   public final boolean[] getVisibilityMask() {
      this.visibilityMask = this.walls[0].getWallSector().visitedFlags;
      return this.visibilityMask;
   }
}
