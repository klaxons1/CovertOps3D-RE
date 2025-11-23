public final class Class_358 {
   public byte[] var_84;
   public int[][] var_bb;
   public byte var_115;

   public Class_358(byte var1) {
      this.var_115 = var1;
      this.var_84 = null;
      this.var_bb = (int[][])null;
   }

   public Class_358(byte var1, byte[] var2) {
      this.var_115 = var1;
      this.var_84 = var2;
      this.var_84 = new byte[4096];

      for(int var3 = 0; var3 < 64; ++var3) {
         for(int var4 = 0; var4 < 64; ++var4) {
            this.var_84[var3 + (63 - var4 << 6)] = var2[var4 + (var3 << 6)];
         }
      }

   }
}
