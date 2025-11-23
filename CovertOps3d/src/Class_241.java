public final class Class_241 {
   private int var_42;
   private int var_5f;
   private int var_ad;
   private int var_d2;
   private int var_10e;
   private int var_15e;
   private Object var_1a6;
   private Object var_1c2;
   public boolean[] var_218;
   public static Class_110[] var_24c;
   public static int var_27f;

   public Class_241(int var1, int var2, int var3, int var4, int var5, int var6) {
      this.var_10e = var5;
      this.var_15e = var6;
      this.var_42 = var1;
      this.var_5f = var2;
      this.var_ad = var3;
      this.var_d2 = Class_48.sub_3c(var4, var3);
      this.var_218 = null;
   }

   public final void sub_5c(Class_3e6 var1) {
      Class_241 var10000;
      Object var10001;
      int var10002;
      if ((this.var_10e & '耀') == 32768) {
         var10000 = this;
         var10001 = var1.var_401;
         var10002 = this.var_10e - '耀';
      } else {
         var10000 = this;
         var10001 = var1.var_3c0;
         var10002 = this.var_10e;
      }

      var10000.var_1a6 = ((Object[])var10001)[var10002];
      if ((this.var_15e & '耀') == 32768) {
         var10000 = this;
         var10001 = var1.var_401;
         var10002 = this.var_15e - '耀';
      } else {
         var10000 = this;
         var10001 = var1.var_3c0;
         var10002 = this.var_15e;
      }

      var10000.var_1c2 = ((Object[])var10001)[var10002];
   }

   private boolean sub_ba(int var1, int var2) {
      if (this.var_d2 == Integer.MAX_VALUE) {
         return this.var_42 - var1 >= 0;
      } else if (this.var_d2 == Integer.MIN_VALUE) {
         return var1 - this.var_42 >= 0;
      } else {
         boolean var3 = var2 - this.var_5f - (int)((long)this.var_d2 * (long)(var1 - this.var_42) >> 16) >= 0;
         if (this.var_ad < 0) {
            var3 = !var3;
         }

         return var3;
      }
   }

   public final void sub_110(Class_71 var1, Class_30a var2) {
      int var3 = var1.var_49;
      int var4 = var1.var_ba;
      Object var10000;
      Object var5;
      if (!this.sub_ba(var3, var4)) {
         var5 = this.var_1a6;
         var10000 = this.var_1c2;
      } else {
         var5 = this.var_1c2;
         var10000 = this.var_1a6;
      }

      Object var6 = var10000;
      Class_241 var7;
      Class_110 var8;
      if (var5 instanceof Class_241) {
         var7 = (Class_241)var5;
         if (var2.sub_101(var7)) {
            var7.sub_110(var1, var2);
         }
      } else {
         var8 = (Class_110)var5;
         if (var2.sub_ca(var8)) {
            var_24c[var_27f++] = var8;
         }
      }

      if (var6 instanceof Class_241) {
         var7 = (Class_241)var6;
         if (var2.sub_101(var7)) {
            var7.sub_110(var1, var2);
         }

      } else {
         var8 = (Class_110)var6;
         if (var2.sub_ca(var8)) {
            var_24c[var_27f++] = var8;
         }

      }
   }

   public final Class_30a sub_13a(int var1, int var2) {
      return this.sub_153(var1, var2).sub_da();
   }

   public final Class_110 sub_153(int var1, int var2) {
      Object var3;
      return (var3 = this.sub_ba(var1, var2) ? this.var_1c2 : this.var_1a6) instanceof Class_241 ? ((Class_241)var3).sub_153(var1, var2) : (Class_110)var3;
   }

   public final boolean[] sub_1a8() {
      boolean[] var1 = this.var_1a6 instanceof Class_241 ? ((Class_241)this.var_1a6).sub_1a8() : ((Class_110)this.var_1a6).sub_19b();
      boolean[] var2 = this.var_1c2 instanceof Class_241 ? ((Class_241)this.var_1c2).sub_1a8() : ((Class_110)this.var_1c2).sub_19b();
      this.var_218 = new boolean[var1.length];

      for(int var3 = 0; var3 < this.var_218.length; ++var3) {
         this.var_218[var3] = var1[var3] && var2[var3];
      }

      return this.var_218;
   }
}
