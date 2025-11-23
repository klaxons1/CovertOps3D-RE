public final class Class_48 {
   private static int[] var_1d;
   private static int[] var_43;

   public static void sub_17() {
      var_1d = new int[1609];

      int var1;
      for(var1 = 0; var1 < 1609; ++var1) {
         var_1d[var1] = sub_191(sub_a0((var1 << 16) + '耀', 102943) / 1609);
      }

      var_43 = new int[1024];

      for(var1 = 0; var1 < 1024; ++var1) {
         var_43[var1] = sub_3c(65536, sub_191(sub_19c(((var1 << 16) + '耀') / 1024)));
      }

   }

   public static int sub_3c(int var0, int var1) {
      int var2 = var0 >= 0 ? var0 : -var0;
      int var3 = var1 >= 0 ? var1 : -var1;
      if (var2 >> 14 >= var3) {
         return (var0 ^ var1) < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
      } else {
         return (int)(((long)var0 << 32) / (long)var1 >> 16);
      }
   }

   public static int sub_a0(int var0, int var1) {
      return (int)((long)var0 * (long)var1 >> 16);
   }

   public static int sub_d7(int var0, int var1) {
      return (int)(((long)var0 << 32) / (long)var1 >> 16);
   }

   public static int sub_11f(int var0, int var1) {
      var0 = var0 >= 0 ? var0 : -var0;
      var1 = var1 >= 0 ? var1 : -var1;
      if (var0 == var1) {
         return (int)((long)var0 * 92682L >> 16);
      } else {
         long var2;
         int var4;
         if (var0 < var1) {
            if (var0 == 0) {
               return var1;
            } else {
               var2 = (long)var0 << 32;
               var4 = var_43[(int)(var2 / (long)var1 >> 22) & 1023];
               return (int)((long)var0 * (long)var4 >> 16);
            }
         } else if (var1 == 0) {
            return var0;
         } else {
            var2 = (long)var1 << 32;
            var4 = var_43[(int)(var2 / (long)var0 >> 22) & 1023];
            return (int)((long)var1 * (long)var4 >> 16);
         }
      }
   }

   public static int sub_169(int var0, int var1) {
      var0 = var0 >= 0 ? var0 : -var0;
      var1 = var1 >= 0 ? var1 : -var1;
      if (var0 == var1) {
         return (int)((long)var0 * 92682L >> 16);
      } else if (var0 < var1) {
         return var0 == 0 ? var1 : sub_d7(var0, sub_1a6(sub_19c(sub_d7(var0, var1))));
      } else {
         return var1 == 0 ? var0 : sub_d7(var1, sub_1a6(sub_19c(sub_d7(var1, var0))));
      }
   }

   private static int sub_191(int var0) {
      byte var1 = 1;
      if (var0 > 102943 && var0 <= 205887) {
         var0 = 205887 - var0;
      } else {
         label32: {
            int var10000;
            int var10001;
            if (var0 > 205887 && var0 <= 308830) {
               var10000 = var0;
               var10001 = 205887;
            } else {
               if (var0 <= 308830) {
                  break label32;
               }

               var10000 = 411774;
               var10001 = var0;
            }

            var0 = var10000 - var10001;
            var1 = -1;
         }
      }

      int var2 = sub_a0(var0, var0);
      boolean var3 = false;
      int var4 = sub_a0(498, var2);
      var4 -= 10880;
      var4 = sub_a0(sub_a0(var4, var2) + 65536, var0);
      return var1 * var4;
   }

   public static int sub_19c(int var0) {
      byte var1;
      label18: {
         var1 = 0;
         byte var10000;
         if (var0 > 65536) {
            var0 = sub_d7(65536, var0);
            var10000 = 1;
         } else {
            if (var0 >= -65536) {
               break label18;
            }

            var0 = sub_d7(65536, var0);
            var10000 = 2;
         }

         var1 = var10000;
      }

      int var2 = sub_a0(var0, var0);
      boolean var3 = false;
      int var4 = sub_a0(1365, var2);
      var4 -= 5579;
      var4 = sub_a0(var4, var2);
      var4 += 11805;
      var4 = sub_a0(var4, var2);
      var4 -= 21646;
      var4 = sub_a0(sub_a0(var4, var2) + '\ufff7', var0);
      switch(var1) {
      case 1:
         return 102943 - var4;
      case 2:
         return -102943 - var4;
      default:
         return var4;
      }
   }

   public static int sub_1a6(int var0) {
      if ((var0 %= 411775) < 0) {
         var0 += 411775;
      }

      if (var0 > 102943 && var0 <= 205887) {
         var0 = 205887 - var0;
      } else {
         if (var0 > 205887 && var0 <= 308830) {
            var0 -= 205887;
            return -var_1d[var0 >> 6];
         }

         if (var0 > 308830) {
            var0 = 411774 - var0;
            return -var_1d[var0 >> 6];
         }
      }

      return var_1d[var0 >> 6];
   }

   public static int sub_1cb(int var0) {
      if ((var0 = (var0 + 102943) % 411775) < 0) {
         var0 += 411775;
      }

      if (var0 > 102943 && var0 <= 205887) {
         var0 = 205887 - var0;
      } else {
         if (var0 > 205887 && var0 <= 308830) {
            var0 -= 205887;
            return -var_1d[var0 >> 6];
         }

         if (var0 > 308830) {
            var0 = 411774 - var0;
            return -var_1d[var0 >> 6];
         }
      }

      return var_1d[var0 >> 6];
   }
}
