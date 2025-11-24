public final class Class_30a {
   public short var_10;
   public short var_82;
   public short var_ac;
   public byte var_dd;
   public byte var_fd;
   private short var_114;
   private short var_168;
   public boolean[] var_1dd;
   public Class_358 var_214;
   public Class_358 var_262;
   public int var_282;
   public int var_2bf;
   public int var_2f0;

   public Class_30a(short var1, short var2, short var3, byte var4, byte var5, short var6, short var7, short var8) {
      this.var_10 = var1;
      this.var_82 = var2;
      this.var_ac = var3;
      this.var_dd = var4;
      this.var_fd = var5;
      this.var_114 = var6;
      this.var_168 = var8;
      this.var_1dd = null;
   }

   public final int sub_15() {
      if (GameEngine.screenShake > 0) {
         int var1;
         if ((var1 = (this.var_114 & '\uffff') + (GameEngine.screenShake >> 1)) > 16) {
            var1 = 16;
         }

         return var1;
      } else {
         return this.var_114 & '\uffff';
      }
   }

   public final int sub_5c() {
      return this.var_168 & '\uffff';
   }

   public final boolean sub_ba(Class_30a var1) {
      return !var1.var_1dd[this.var_10];
   }

   public final boolean sub_ca(Sector var1) {
      return !var1.visibilityMask[this.var_10];
   }

   public final boolean sub_101(BSPNode var1) {
      return !var1.visibleSectors[this.var_10];
   }
}
