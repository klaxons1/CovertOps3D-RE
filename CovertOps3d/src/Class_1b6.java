public final class Class_1b6 extends Class_71 {
   public int var_40;
   private int var_63;
   public int var_72;
   private int var_b9;
   private static int var_e7 = 65;

   public Class_1b6(int var1, int var2, int var3, int var4) {
      this(var1, var2, var3, var4, 0, 0, 0, 0);
   }

   private Class_1b6(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      super(var1, var2, var3, var4);
      this.var_40 = var5;
      this.var_63 = var6;
      this.var_72 = var7;
      this.var_b9 = var8;
   }

   public final void sub_36() {
      this.sub_58(this.var_40, this.var_63, this.var_72, this.var_b9);
   }

   public final void sub_4a() {
      this.sub_58(this.var_40 >> 1, this.var_63 >> 1, this.var_72 >> 1, this.var_b9 >> 1);
   }

   public final void sub_79(int var1, int var2, int var3, int var4) {
      this.var_40 = Class_48.sub_a0(this.var_40, var1);
      this.var_63 = Class_48.sub_a0(this.var_63, var2);
      this.var_72 = Class_48.sub_a0(this.var_72, var3);
      this.var_b9 = Class_48.sub_a0(this.var_b9, var4);
      if (this.var_40 < var_e7 && this.var_40 > -var_e7) {
         this.var_40 = 0;
      }

      if (this.var_40 < var_e7 && this.var_40 > -var_e7) {
         this.var_40 = 0;
      }

      if (this.var_72 < var_e7 && this.var_72 > -var_e7) {
         this.var_72 = 0;
      }

      if (this.var_b9 < var_e7 && this.var_b9 > -var_e7) {
         this.var_b9 = 0;
      }

   }

   public final void sub_d3(int var1, int var2) {
      this.sub_f9(var1, var2, 0);
   }

   public final void sub_f9(int var1, int var2, int var3) {
      int var4 = Class_48.sub_1a6(super.var_d7);
      int var5 = Class_48.sub_1cb(super.var_d7);
      this.var_40 += Class_48.sub_a0(var5, var1) - Class_48.sub_a0(var4, var2);
      this.var_72 += Class_48.sub_a0(-var4, var1) - Class_48.sub_a0(var5, var2);
      this.var_b9 += var3;
   }
}
