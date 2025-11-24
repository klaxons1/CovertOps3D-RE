public final class Class_b0 {
   private Class_282[] var_4d = new Class_282[288];
   private Class_282 var_95 = null;

   public Class_b0() {
      this.sub_4e();
   }

   public final void sub_4e() {
      for(int var1 = 0; var1 < 288; ++var1) {
         if (this.var_4d[var1] != null) {
            Class_282 var10000 = this.var_4d[var1];

            while(true) {
               Class_282 var2 = var10000;
               if (var10000.var_119 == null) {
                  var2.var_119 = this.var_95;
                  this.var_95 = this.var_4d[var1];
                  break;
               }

               var10000 = var2.var_119;
            }
         }

         this.var_4d[var1] = null;
      }

   }

   public final void sub_82(short var1, short var2, short var3, int var4) {
      Class_282 var10000 = this.var_4d[var4];

      while(true) {
         Class_282 var5 = var10000;
         Class_282 var6;
         if (var10000 == null) {
            if (this.var_95 != null) {
               var6 = this.var_95;
               this.var_95 = this.var_95.var_119;
               var6.var_59 = var1;
               var6.var_6e = var2;
               var6.var_79 = var3;
            } else {
               var6 = new Class_282(var1, var2, var3);
            }

            var6.var_119 = this.var_4d[var4];
            this.var_4d[var4] = var6;
            return;
         }

         if (var5.var_79 == var3) {
            Class_282 var7;
            if (var5.var_6e == var1 - 1) {
               var5.var_6e = var2;
               var6 = this.var_4d[var4];

               for(var7 = null; var6 != null; var6 = var6.var_119) {
                  if (var6.var_79 == var3 && var6.var_59 == var2 + 1) {
                     var5.var_6e = var6.var_6e;
                     if (var7 != null) {
                        var7.var_119 = var6.var_119;
                     } else {
                        this.var_4d[var4] = var6.var_119;
                     }

                     var6.var_119 = this.var_95;
                     this.var_95 = var6;
                     return;
                  }

                  var7 = var6;
               }

               return;
            }

            if (var5.var_59 == var2 + 1) {
               var5.var_59 = var1;
               var6 = this.var_4d[var4];

               for(var7 = null; var6 != null; var6 = var6.var_119) {
                  if (var6.var_79 == var3 && var6.var_6e == var1 - 1) {
                     var5.var_59 = var6.var_59;
                     if (var7 != null) {
                        var7.var_119 = var6.var_119;
                     } else {
                        this.var_4d[var4] = var6.var_119;
                     }

                     var6.var_119 = this.var_95;
                     this.var_95 = var6;
                     return;
                  }

                  var7 = var6;
               }

               return;
            }
         }

         var10000 = var5.var_119;
      }
   }

   public final void sub_e5(int var1, int var2, int var3, int var4) {
      Class_282 var10000;
      int var5;
      Class_282 var6;
      Class_30a var7;
      for(var5 = 0; var5 < 144; ++var5) {
         var10000 = this.var_4d[var5];

         while(true) {
            var6 = var10000;
            if (var10000 == null) {
               break;
            }

            var7 = GameEngine.var_505.var_32c[var6.var_79];
            GameEngine.sub_430(var6.var_59, var6.var_6e, var5, var7.var_214.var_84, var7.var_214.var_bb, var7.var_282, var1, var2, var3, var7.var_2bf, var4);
            var10000 = var6.var_119;
         }
      }

      for(var5 = 144; var5 < 288; ++var5) {
         var10000 = this.var_4d[var5];

         while(true) {
            var6 = var10000;
            if (var10000 == null) {
               break;
            }

            var7 = GameEngine.var_505.var_32c[var6.var_79];
            GameEngine.sub_430(var6.var_59, var6.var_6e, var5, var7.var_262.var_84, var7.var_262.var_bb, var7.var_282, var1, var2, var3, var7.var_2f0, var4);
            var10000 = var6.var_119;
         }
      }

   }
}
