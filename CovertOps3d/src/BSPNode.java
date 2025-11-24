public final class BSPNode {
   private int splitX;
   private int splitZ;
   private int normalX;
   private int splitSlope;
   private int frontChildIndex;
   private int backChildIndex;
   private Object frontChild;
   private Object backChild;
   public boolean[] visibleSectors;
   public static Sector[] visibleSectorsList;
   public static int visibleSectorsCount;

   public BSPNode(int var1, int var2, int var3, int var4, int var5, int var6) {
      this.frontChildIndex = var5;
      this.backChildIndex = var6;
      this.splitX = var1;
      this.splitZ = var2;
      this.normalX = var3;
      this.splitSlope = MathUtils.fixedPointDivide(var4, var3);
      this.visibleSectors = null;
   }

   public final void initializeBSPNode(Class_3e6 var1) {
      BSPNode var10000;
      Object var10001;
      int var10002;
      if ((this.frontChildIndex & '耀') == 32768) {
         var10000 = this;
         var10001 = var1.var_401;
         var10002 = this.frontChildIndex - '耀';
      } else {
         var10000 = this;
         var10001 = var1.var_3c0;
         var10002 = this.frontChildIndex;
      }

      var10000.frontChild = ((Object[])var10001)[var10002];
      if ((this.backChildIndex & '耀') == 32768) {
         var10000 = this;
         var10001 = var1.var_401;
         var10002 = this.backChildIndex - '耀';
      } else {
         var10000 = this;
         var10001 = var1.var_3c0;
         var10002 = this.backChildIndex;
      }

      var10000.backChild = ((Object[])var10001)[var10002];
   }

   private boolean isPointInFront(int var1, int var2) {
      if (this.splitSlope == Integer.MAX_VALUE) {
         return this.splitX - var1 >= 0;
      } else if (this.splitSlope == Integer.MIN_VALUE) {
         return var1 - this.splitX >= 0;
      } else {
         boolean var3 = var2 - this.splitZ - (int)((long)this.splitSlope * (long)(var1 - this.splitX) >> 16) >= 0;
         if (this.normalX < 0) {
            var3 = !var3;
         }

         return var3;
      }
   }

   public final void traverseBSP(Transform3D var1, Class_30a var2) {
      int var3 = var1.x;
      int var4 = var1.z;
      Object var10000;
      Object var5;
      if (!this.isPointInFront(var3, var4)) {
         var5 = this.frontChild;
         var10000 = this.backChild;
      } else {
         var5 = this.backChild;
         var10000 = this.frontChild;
      }

      Object var6 = var10000;
      BSPNode var7;
      Sector var8;
      if (var5 instanceof BSPNode) {
         var7 = (BSPNode)var5;
         if (var2.sub_101(var7)) {
            var7.traverseBSP(var1, var2);
         }
      } else {
         var8 = (Sector)var5;
         if (var2.sub_ca(var8)) {
            visibleSectorsList[visibleSectorsCount++] = var8;
         }
      }

      if (var6 instanceof BSPNode) {
         var7 = (BSPNode)var6;
         if (var2.sub_101(var7)) {
            var7.traverseBSP(var1, var2);
         }

      } else {
         var8 = (Sector)var6;
         if (var2.sub_ca(var8)) {
            visibleSectorsList[visibleSectorsCount++] = var8;
         }

      }
   }

   public final Class_30a findSectorAtPoint(int var1, int var2) {
      return this.findSectorNodeAtPoint(var1, var2).getSectorData();
   }

   public final Sector findSectorNodeAtPoint(int var1, int var2) {
      Object var3;
      return (var3 = this.isPointInFront(var1, var2) ? this.backChild : this.frontChild) instanceof BSPNode ? ((BSPNode)var3).findSectorNodeAtPoint(var1, var2) : (Sector)var3;
   }

   public final boolean[] calculateVisibleSectors() {
      boolean[] var1 = this.frontChild instanceof BSPNode ? ((BSPNode)this.frontChild).calculateVisibleSectors() : ((Sector)this.frontChild).getVisibilityMask();
      boolean[] var2 = this.backChild instanceof BSPNode ? ((BSPNode)this.backChild).calculateVisibleSectors() : ((Sector)this.backChild).getVisibilityMask();
      this.visibleSectors = new boolean[var1.length];

      for(int var3 = 0; var3 < this.visibleSectors.length; ++var3) {
         this.visibleSectors[var3] = var1[var3] && var2[var3];
      }

      return this.visibleSectors;
   }
}
