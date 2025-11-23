public final class Class_1e1 {
   public short var_22;
   public short var_5c;
   public Class_13c var_88;
   private short var_a7;
   private short var_ce;
   public Class_8e var_e5;
   public Class_8e var_133;
   private byte var_18e;
   private byte var_1be;
   private byte var_1d8;

   public Class_1e1(short var1, short var2, short var3, short var4, byte var5, byte var6, byte var7) {
      this.var_22 = var1;
      this.var_5c = var2;
      this.var_88 = new Class_13c(0, 0);
      this.var_a7 = var3;
      this.var_ce = var4;
      this.var_18e = var5;
      this.var_1be = var6;
      this.var_1d8 = var7;
   }

   public final int sub_5e() {
      return this.var_1be & '\uffff';
   }

   public final int sub_90() {
      return this.var_1d8 & '\uffff';
   }

   public final void sub_ca(Class_3e6 var1) {
      if (this.var_a7 == -1) {
         throw new IllegalArgumentException();
      } else {
         this.var_e5 = var1.var_370[this.var_a7 & '\uffff'];
         Class_1e1 var10000;
         Class_8e var10001;
         if (this.var_ce == -1) {
            var10000 = this;
            var10001 = null;
         } else {
            var10000 = this;
            var10001 = var1.var_370[this.var_ce & '\uffff'];
         }

         var10000.var_133 = var10001;
         Class_13c var2 = var1.var_138[this.var_22 & '\uffff'];
         Class_13c var3 = var1.var_138[this.var_5c & '\uffff'];
         int var4 = var2.var_83 - var3.var_83;
         int var5 = var3.var_2b - var2.var_2b;
         int var6 = Class_48.sub_169(var4, var5);
         this.var_88.var_2b = Class_48.sub_3c(var4, var6);
         this.var_88.var_83 = Class_48.sub_3c(var5, var6);
      }
   }

   public final boolean sub_129() {
      return (this.var_18e & 3) > 0;
   }

   public final boolean sub_18c() {
      return (this.var_18e & 67) > 0;
   }

   public final boolean sub_1d1() {
      return (this.var_18e & 1) > 0;
   }

   public final boolean sub_1f8() {
      return (this.var_18e & 4) > 0;
   }

   public final boolean sub_232() {
      return (this.var_18e & 8) > 0;
   }

   public final boolean sub_25c() {
      return (this.var_18e & 16) > 0;
   }

   public final boolean sub_29e() {
      return (this.var_18e & 128) > 0;
   }

   public final void sub_2ea() {
      this.var_18e = (byte)(this.var_18e | 128);
   }
}
