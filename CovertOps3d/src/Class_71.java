public class Class_71 {
   public int var_49;
   public int var_7a;
   public int var_ba;
   public int var_d7;

   public Class_71(int var1, int var2, int var3, int var4) {
      this.var_49 = var1;
      this.var_7a = var2;
      this.var_ba = var3;
      this.var_d7 = var4;
   }

   public final void sub_58(int var1, int var2, int var3, int var4) {
      this.var_49 += var1;
      this.var_7a += var2;
      this.var_ba += var3;
      Class_71 var10000 = this;
      int var10001 = this.var_d7;
      int var10002 = var4;

      while(true) {
         var10000.var_d7 = var10001 + var10002;
         if (this.var_d7 >= 0) {
            while(this.var_d7 >= 411775) {
               this.var_d7 -= 411775;
            }

            return;
         }

         var10000 = this;
         var10001 = this.var_d7;
         var10002 = 411775;
      }
   }

   public final void sub_8e(Class_71 var1) {
      this.sub_bb(var1.var_49, var1.var_7a, var1.var_ba, var1.var_d7);
   }

   public final void sub_bb(int var1, int var2, int var3, int var4) {
      this.var_49 = var1;
      this.var_7a = var2;
      this.var_ba = var3;
      this.var_d7 = var4;
   }

   public final void sub_ca(int var1, int var2) {
      int var3 = Class_48.sub_1a6(this.var_d7);
      int var4 = Class_48.sub_1cb(this.var_d7);
      this.var_49 += Class_48.sub_a0(var4, var1) - Class_48.sub_a0(var3, var2);
      this.var_ba += Class_48.sub_a0(-var3, var1) - Class_48.sub_a0(var4, var2);
   }
}
