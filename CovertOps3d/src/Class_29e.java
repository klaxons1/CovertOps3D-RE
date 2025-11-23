import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public final class Class_29e {
   private static int var_37;
   private static Class_358[] var_75;
   private static Class_318[] var_85;
   private static Hashtable var_e1;
   public static int var_117 = 0;
   public static boolean var_161;
   public static boolean var_1bf;
   public static boolean var_1ed;
   public static boolean var_1fa;
   public static boolean var_223;
   public static boolean var_280;
   public static boolean var_2cd;
   public static boolean var_2f7;
   public static boolean var_344;
   public static boolean var_364;
   public static boolean var_3b8;
   public static boolean var_3e3;
   public static boolean var_446;
   public static int var_480;
   public static int var_4c8 = 0;
   public static Class_3e6 var_505 = null;
   public static Class_1b6 var_553;
   public static Class_71 var_563;
   public static Class_30a var_5c2;
   private static Class_13c var_5f8 = new Class_13c(0, 0);
   private static Class_13c var_654 = new Class_13c(0, 0);
   private static int var_6a4;
   private static int var_6f4;
   private static int var_723;
   private static int var_771;
   private static int var_7ba;
   private static int var_817;
   public static int[] var_843;
   private static Class_318 var_894;
   private static Class_318 var_8f4;
   private static short[] var_926;
   private static int var_958;
   private static int var_9b4;
   private static int var_9e4;
   private static int var_9fa;
   private static int var_a27;
   private static int var_a34;
   private static Class_b0 var_a80;
   private static int[] var_ab4;
   private static int[] var_acc;
   public static Vector var_b02;
   public static Vector var_b3f;
   public static Vector var_b86;
   public static Vector var_bd3;
   public static int var_c07 = 100;
   public static int var_c18 = 0;
   public static int var_c79 = 1;
   public static boolean[] var_c8b = new boolean[]{true, false, false, false, false, false, true, false, false};
   public static int[] var_ced = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
   public static int var_d38 = 0;
   public static int var_d46 = 0;
   public static boolean[] var_d98 = new boolean[]{false, false};
   public static String var_ded = "";
   public static int var_e3a = 0;
   public static int var_e72 = 0;
   public static Class_1e1 var_e96 = null;
   public static Random var_ea3 = new Random();
   public static boolean var_eba = false;
   public static byte var_ef9 = 0;
   public static int var_f27;
   private static int var_f32 = Class_48.sub_a0(1310720, 92682);
   private static int var_f5f = 0;
   private static int var_f88 = 0;
   public static boolean var_fe0 = false;
   public static int var_1044 = 0;
   public static boolean var_10a4 = false;
   public static int var_10bb;
   public static int var_1103;
   public static int var_1142;
   public static int var_1171;
   public static Class_445[] var_118d;
   public static int var_119a;

   public static void sub_52() {
      Class_3aa.sub_57c();
      sub_5f4();
      var_553 = new Class_1b6(0, 1572864, 0, 65536);
      var_563 = new Class_71(0, 0, 0, 0);
      var_b02 = new Vector();
      var_b3f = new Vector();
      var_b86 = new Vector();
      var_bd3 = new Vector();
      var_118d = new Class_445[64];
      var_119a = 0;
      Class_241.var_27f = 0;
      var_894 = new Class_318((byte)0, 8, 8, 0, 0, new int[]{16777215, 16711680});
      byte[] var0 = new byte[]{17, 17, 17, 17, 17, 17, 17, 17};
      byte[] var1 = new byte[]{17, 16, 16, 16, 16, 17, 16, 17};
      byte[] var2 = new byte[]{17, 1, 1, 1, 1, 17, 1, 17};
      var_894.sub_2a(0, var0);
      var_894.sub_2a(2, var1);
      var_894.sub_2a(4, var2);
      var_894.sub_2a(6, var0);
      var_843 = new int[69120];
      var_926 = new short[288];
      var_a80 = new Class_b0();
      var_ab4 = new int[240];

      for(int var3 = 0; var3 < 240; ++var3) {
         var_ab4[var3] = Class_48.sub_3c(var3 - 120 << 16, 7864320) >> 2;
      }

      var_acc = new int[289];
      var_acc[0] = 0;

      for(int var4 = 1; var4 < 289; ++var4) {
         var_acc[var4] = 65536 / var4;
      }

      var_10bb = Class_48.sub_3c(65536, 17301600);
      var_1103 = Class_48.sub_a0(Class_48.sub_3c(65536, 15794176), 102943);
      var_1142 = Class_48.sub_3c(65536, 18874368);
      var_1171 = Class_48.sub_3c(65536, 411775);
      Class_3aa.sub_57c();
   }

   public static void sub_6f() {
      sub_391();
      var_505.sub_133();
      var_553.sub_8e(var_505.var_383);
      var_5c2 = var_505.sub_57().sub_13a(var_553.var_49, var_553.var_ba);
      var_b86.removeAllElements();
      var_bd3.removeAllElements();
      var_119a = 0;
      Class_241.var_27f = 0;
      var_ded = "";
      var_e3a = 0;
      var_e72 = 0;
      var_e96 = null;
   }

   public static void sub_bc(byte var0) {
      sub_3f5(sub_f6(var0));
   }

   private static Class_358 sub_cb(byte var0) {
      if (var0 == 51) {
         return null;
      } else {
         Class_358 var1;
         return (var1 = var_75[var0]) != null && var1.var_84 != null ? var1 : null;
      }
   }

   private static Class_318 sub_f6(byte var0) {
      if (var0 == 0) {
         return var_894;
      } else {
         Class_318 var1;
         return (var1 = var_85[var0 + 128]) != null && var1.var_11 > 0 ? var1 : var_894;
      }
   }

   private static boolean sub_102(Class_13c var0, Class_13c var1, int var2, int var3, int var4) {
      if (var0.var_83 <= 327680 && var1.var_83 <= 327680) {
         return false;
      } else {
         int var5;
         int var6 = (var5 = var4 << 16) + Class_48.sub_11f(var0.var_2b - var1.var_2b, var0.var_83 - var1.var_83);
         var_7ba = var5;
         var_817 = var6;
         var_5f8.var_2b = var0.var_2b;
         var_5f8.var_83 = var0.var_83;
         var_654.var_2b = var1.var_2b;
         var_654.var_83 = var1.var_83;
         int var7;
         if (var1.var_83 < 327680) {
            var7 = Class_48.sub_3c(var0.var_83 - 327680, var0.var_83 - var1.var_83);
            var_654.var_83 = 327680;
            if (var7 == Integer.MAX_VALUE) {
               var_654.var_2b = var1.var_2b > var0.var_2b ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               var_817 = var6 > var5 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               var_654.var_2b = var1.var_2b > var0.var_2b ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               var_817 = var6 > var5 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               var_654.var_2b = (int)((long)(var1.var_2b - var0.var_2b) * (long)var7 >> 16) + var0.var_2b;
               var_817 = (int)((long)(var6 - var5) * (long)var7 >> 16) + var5;
            }
         }

         if (var0.var_83 < 327680) {
            var7 = Class_48.sub_3c(var1.var_83 - 327680, var1.var_83 - var0.var_83);
            var_5f8.var_83 = 327680;
            if (var7 == Integer.MAX_VALUE) {
               var_5f8.var_2b = var0.var_2b > var1.var_2b ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               var_7ba = var5 > var6 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               var_5f8.var_2b = var0.var_2b > var1.var_2b ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               var_7ba = var5 > var6 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               var_5f8.var_2b = (int)((long)(var0.var_2b - var1.var_2b) * (long)var7 >> 16) + var1.var_2b;
               var_7ba = (int)((long)(var5 - var6) * (long)var7 >> 16) + var6;
            }
         }

         long var11 = 33776997205278720L / (long)var_5f8.var_83 >> 16;
         long var9 = 33776997205278720L / (long)var_654.var_83 >> 16;
         var_5f8.var_2b = (int)((long)var_5f8.var_2b * var11 >> 16);
         if (var_5f8.var_2b > 7864320) {
            return false;
         } else {
            var_654.var_2b = (int)((long)var_654.var_2b * var9 >> 16);
            if (var_654.var_2b < -7864320) {
               return false;
            } else if (var_654.var_2b < var_5f8.var_2b) {
               return false;
            } else {
               var_6a4 = (int)((long)var2 * var11 >> 16);
               var_6f4 = (int)((long)var3 * var11 >> 16);
               var_723 = (int)((long)var2 * var9 >> 16);
               var_771 = (int)((long)var3 * var9 >> 16);
               return true;
            }
         }
      }
   }

   private static void sub_14e(Class_21c var0, Class_1e1 var1, Class_8e var2, Class_13c[] var3, int var4, int var5, int var6, int var7) {
      Class_30a var8 = var2.var_13d;
      int var9 = -var5 + (-var8.var_ac << 16);
      int var10 = -var5 + (-var8.var_82 << 16);
      if (sub_102(var3[var0.var_80 & '\uffff'], var3[var0.var_10b & '\uffff'], var9, var10, var0.var_1d1 & '\uffff')) {
         Class_13c var11 = var_5f8;
         Class_13c var12 = var_654;
         int var13 = var11.var_2b + 7864320 >> 16;
         int var14 = var_6a4 + 9437184 >> 16;
         int var15 = var12.var_2b + 7864320 >> 16;
         int var16 = var_723 + 9437184 >> 16;
         int var17 = var_6f4 + 9437184 >> 16;
         int var18 = var_771 + 9437184 >> 16;
         int var19 = var8.var_ac - var8.var_82;
         Class_318 var20 = sub_f6(var2.var_aa);
         int var21 = var2.var_1a2 & '\uffff';
         int var22 = var20.var_6f - var19 + var21;
         if (!var1.sub_25c()) {
            var22 = var21;
         }

         var1.sub_2ea();
         int var23 = (var2.var_192 & '\uffff') << 16;
         sub_420(var8, var20, var20, var13, var14, var17, var17, var17, var11.var_83, var15, var16, var18, var18, var18, var12.var_83, var_7ba + var23, var_817 - var_7ba, var22, var19, var22, var19, -var4, -var6, var7, var9, var10);
      }

   }

   private static void sub_17a(Class_21c var0, Class_1e1 var1, Class_8e var2, Class_8e var3, Class_13c[] var4, int var5, int var6, int var7, int var8) {
      Class_30a var9 = var2.var_13d;
      Class_30a var10 = var3.var_13d;
      int var11 = -var6 + (-var9.var_ac << 16);
      int var12 = -var6 + (-var9.var_82 << 16);
      int var13 = -var6 + (-var10.var_ac << 16);
      int var14 = -var6 + (-var10.var_82 << 16);
      if (sub_102(var4[var0.var_80 & '\uffff'], var4[var0.var_10b & '\uffff'], var11, var12, var0.var_1d1 & '\uffff')) {
         Class_13c var15 = var_5f8;
         Class_13c var16 = var_654;
         int var17 = var15.var_2b + 7864320 >> 16;
         int var18 = var_6a4 + 9437184 >> 16;
         int var19 = var_6f4 + 9437184 >> 16;
         int var20 = var16.var_2b + 7864320 >> 16;
         int var21 = var_723 + 9437184 >> 16;
         int var22 = var_771 + 9437184 >> 16;
         int var23 = Class_48.sub_3c(var13, var15.var_83) * 120 + 9437184 >> 16;
         int var24 = Class_48.sub_3c(var14, var15.var_83) * 120 + 9437184 >> 16;
         int var25 = Class_48.sub_3c(var13, var16.var_83) * 120 + 9437184 >> 16;
         int var26 = Class_48.sub_3c(var14, var16.var_83) * 120 + 9437184 >> 16;
         int var27 = var9.var_ac - var10.var_ac;
         int var28 = var10.var_82 - var9.var_82;
         Class_318 var29 = sub_f6(var2.var_5e);
         Class_318 var30 = sub_f6(var2.var_c3);
         if (var10.var_fd == 51) {
            var29 = var_894;
         }

         if (var10.var_dd == 51) {
            var30 = var_894;
         }

         int var31 = var2.var_1a2 & '\uffff';
         int var32 = var29.var_6f - var27 + var31;
         if (var1.sub_232()) {
            var32 = var31;
         }

         int var33 = var9.var_ac - var10.var_82 + var31;
         if (!var1.sub_25c()) {
            var33 = var31;
         }

         var1.sub_2ea();
         int var34 = (var2.var_192 & '\uffff') << 16;
         sub_420(var9, var29, var30, var17, var18, var23, var24, var19, var15.var_83, var20, var21, var25, var26, var22, var16.var_83, var_7ba + var34, var_817 - var_7ba, var32, var27, var33, var28, -var5, -var7, var8, var11, var12);
      }

   }

   private static void sub_1cf(Class_21c var0, Class_13c[] var1, int var2, int var3, int var4, int var5) {
      Class_1e1 var6;
      Class_8e var7 = (var6 = var0.var_131).var_e5;
      Class_8e var8;
      if ((var8 = var6.var_133) != null) {
         if (var0.var_125) {
            sub_17a(var0, var6, var7, var8, var1, var2, var3, var4, var5);
         } else {
            sub_17a(var0, var6, var8, var7, var1, var2, var3, var4, var5);
         }
      } else if (var0.var_125) {
         sub_14e(var0, var6, var7, var1, var2, var3, var4, var5);
      } else {
         throw new IllegalStateException();
      }
   }

   private static void sub_1e9(Class_110 var0, int var1, int var2, int var3, long var4, long var6) {
      Class_30a var8 = var0.sub_da();
      Vector var9 = var0.var_193;
      var_119a = 0;

      int var10;
      Class_445 var11;
      for(var10 = 0; var10 < var9.size(); ++var10) {
         Class_71 var12;
         int var13 = (var12 = (var11 = (Class_445)var9.elementAt(var10)).var_c).var_49 - var1;
         int var14 = var12.var_ba - var3;
         var11.var_2f3.var_2b = (int)(var6 * (long)var13 - var4 * (long)var14 >> 16);
         var11.var_2f3.var_83 = (int)(var4 * (long)var13 + var6 * (long)var14 >> 16);
         if (var11.var_2f3.var_83 > 327680) {
            byte var15 = var11.sub_a9();
            byte var16 = var11.sub_b7();
            if (var15 != 0 || var16 != 0) {
               Class_445 var10000;
               int var18;
               label99: {
                  short var10001;
                  if (var11.var_24 >= 59 && var11.var_24 <= 63) {
                     var10000 = var11;
                     var10001 = var8.var_ac;
                  } else {
                     if (var11.var_24 >= 100 && var11.var_24 <= 102) {
                        var10000 = var11;
                        var18 = -var11.var_c.var_7a;
                        break label99;
                     }

                     var10000 = var11;
                     var10001 = var8.var_82;
                  }

                  var18 = -var10001 << 16;
               }

               var10000.var_319 = var18 - var2;
               Class_318 var19;
               if (var15 != 0) {
                  var10000 = var11;
                  var19 = var_85[var15 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.var_363 = var19;
               if (var16 != 0) {
                  var10000 = var11;
                  var19 = var_85[var16 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.var_399 = var19;
               var_118d[var_119a++] = var11;
               if (var_119a >= 64) {
                  break;
               }
            }
         }
      }

      for(var10 = 1; var10 < var_119a; ++var10) {
         var11 = var_118d[var10];

         int var17;
         for(var17 = var10; var17 > 0 && var_118d[var17 - 1].sub_11b(var11); --var17) {
            var_118d[var17] = var_118d[var17 - 1];
         }

         var_118d[var17] = var11;
      }

      for(var10 = 0; var10 < var_119a; ++var10) {
         if ((var11 = var_118d[var10]).sub_172()) {
            if (var11.var_399 != null) {
               var11.sub_1ee();
               sub_410(var11.var_399, var8.sub_15(), (var11.var_2f3.var_2b >> 16) + 120, (var11.var_319 >> 16) + 144, var11.var_2f3.var_83, var11.var_41b, var11.var_440);
            }

            if (var11.var_363 != null) {
               var11.sub_194();
               sub_410(var11.var_363, var8.sub_15(), (var11.var_2f3.var_2b >> 16) + 120, (var11.var_319 >> 16) + 144, var11.var_2f3.var_83, var11.var_3e0, var11.var_404);
            }
         }
      }

   }

   private static void sub_246(int var0, int var1, int var2, int var3) {
      var_505.sub_4e6();
      Class_13c[] var4 = var_505.sub_b8(var0, var2, var3);
      var_505.sub_178();
      Class_241.var_27f = 0;
      var_505.sub_57().sub_110(var_553, var_505.sub_115(var0, var2));
      int var5 = var2 << 8;
      int var6 = var0 << 8;
      int var7 = var3 << 1;
      int var8 = Class_48.sub_1a6(var3);
      int var9 = Class_48.sub_1cb(var3);
      var_10a4 = Class_3aa.var_98c == 1 && var_d38 != 0;
      var_a80.sub_4e();
      Class_110.sub_85();

      int var10;
      Class_110 var11;
      for(var10 = 0; var10 < Class_241.var_27f; ++var10) {
         var11 = Class_241.var_24c[var10];
         if (var10 > 0 && Class_110.sub_33()) {
            Class_241.var_27f = var10;
            break;
         }

         if (var10 >= var_b02.size()) {
            short[] var12 = new short[240];
            short[] var13 = new short[240];
            var_b02.addElement(var12);
            var_b3f.addElement(var13);
         }

         System.arraycopy(Class_110.var_144, 0, (short[])((short[])var_b02.elementAt(var10)), 0, 240);
         System.arraycopy(Class_110.var_130, 0, (short[])((short[])var_b3f.elementAt(var10)), 0, 240);
         Class_21c[] var15 = var11.var_dc;

         for(int var16 = 0; var16 < var15.length; ++var16) {
            sub_1cf(var15[var16], var4, var6, var1, var5, var7);
         }
      }

      var_a80.sub_e5(var8, var9, -var6, -var5);

      for(var10 = Class_241.var_27f - 1; var10 >= 0; --var10) {
         var11 = Class_241.var_24c[var10];
         System.arraycopy(var_b02.elementAt(var10), 0, Class_110.var_144, 0, 240);
         System.arraycopy(var_b3f.elementAt(var10), 0, Class_110.var_130, 0, 240);
         sub_1e9(var11, var0, var1, var2, (long)var8, (long)var9);
      }

   }

   public static int sub_296(Graphics var0, int var1) {
      var_5c2 = var_505.sub_57().sub_13a(var_553.var_49, var_553.var_ba);
      boolean var2 = false;
      int var3 = var1 - var_f88;
      var_f88 = var1;
      int var4 = Class_48.sub_11f(var_553.var_40, var_553.var_72);
      var_f5f += var3 * var4 >> 2;
      int var8 = Class_48.sub_1a6(var_f5f);
      int var5 = var_ef9 << 15;
      if ((var_ef9 & 1) > 0) {
         var5 = -var5;
      }

      var_f27 = (var_5c2.var_82 + Class_3e6.var_70 << 16) + var8 + var5;
      sub_246(var_553.var_49, -var_f27, var_553.var_ba, var_553.var_d7);
      if (var_eba) {
         int var6 = 69120;

         for(int var7 = 0; var7 < var6; ++var7) {
            int[] var10000 = var_843;
            var10000[var7] |= 16711680;
         }

         var_eba = false;
      }

      if (var_ef9 == 16) {
         --var_ef9;
      }

      var0.drawRGB(var_843, 0, 240, 0, 0, 240, 288, false);
      if (var_5c2.sub_5c() == 666) {
         switch(Class_3aa.var_259) {
         case 3:
            if (!var_c8b[8]) {
               var_ded = "get the sniper rifle!";
               var_e3a = 30;
               break;
            }
         default:
            Class_3aa.var_295 = Class_3aa.var_259++;
            var_117 = 0;
            var_480 = 1;
            break;
         case 4:
            var_ded = "i think that's the wall|she mentioned";
            var_e3a = 30;
         }
      }

      if ((Class_3aa.var_e8b & 1) == 0 && Class_3aa.var_259 == 0 && var_5c2.var_10 == 31) {
         var_ded = "press 1 to open the door";
         var_e3a = 30;
      }

      return var8;
   }

   public static boolean sub_2f3() {
      if (var_e3a > 0) {
         --var_e3a;
      }

      if (var_161) {
         var_553.sub_d3(0, -196608);
      }

      if (var_1ed) {
         var_553.sub_d3(-196608, 0);
      }

      if (var_1bf) {
         var_553.sub_d3(0, 131072);
      }

      if (var_1fa) {
         var_553.sub_d3(196608, 0);
      }

      if (var_223) {
         var_553.sub_f9(0, 0, -4500);
      }

      if (var_280) {
         var_553.sub_f9(0, 0, 4500);
      }

      int var0 = Class_48.sub_11f(var_553.var_40, var_553.var_72);
      Class_1e1 var1 = null;
      if (var0 > 262144) {
         var_553.sub_4a();
         var1 = var_505.sub_205(var_553, var_5c2);
         var_553.sub_4a();
         Class_1e1 var2 = var_505.sub_205(var_553, var_5c2);
         if (var1 == null) {
            var1 = var2;
         }
      } else {
         var_553.sub_36();
         var1 = var_505.sub_205(var_553, var_5c2);
      }

      int var21;
      if (!var_3e3) {
         if (var1 != null) {
            var_e96 = (var21 = var1.sub_5e()) != 1 && var21 != 11 && var21 != 26 && var21 != 28 && var21 != 51 && var21 != 62 ? null : var1;
         }
      } else {
         var_e96 = null;
      }

      int var3;
      int var4;
      int var5;
      int var6;
      int var7;
      int var10000;
      if (var_e96 != null) {
         var21 = var_553.var_d7;
         var3 = Class_48.sub_1a6(102943 - var21);
         var4 = Class_48.sub_1cb(102943 - var21);
         var5 = 1310720;
         var6 = var_553.var_49 + Class_48.sub_a0(var5, var4);
         var7 = var_553.var_ba + Class_48.sub_a0(var5, var3);
         Class_13c[] var8;
         Class_13c var9 = (var8 = var_505.var_138)[var_e96.var_22 & '\uffff'];
         Class_13c var10 = var8[var_e96.var_5c & '\uffff'];
         if (Class_3e6.sub_365(var_553.var_49, var_553.var_ba, var6, var7, var9.var_2b, var9.var_83, var10.var_2b, var10.var_83)) {
            var10000 = var_e72 + 1;
         } else {
            var_e96 = null;
            var10000 = 0;
         }

         var_e72 = var10000;
         if (var_e72 >= 50) {
            var_ded = var_e96.sub_5e() == 62 ? "press 1 to move the lift" : "press 1 to open the door";
            var_e3a = 10;
         }
      } else {
         var_e72 = 0;
      }

      if (var_446) {
         Class_3aa.var_ef8 = !Class_3aa.var_ef8;
         var_446 = false;
      }

      int var12;
      byte var10001;
      int var35;
      if (var_3e3) {
         Class_2d8 var22;
         Class_2d8 var41;
         if (var_5c2.sub_5c() == 10 && (var22 = sub_34f(var_5c2)).var_126 == 0) {
            if (var_5c2.var_82 == var22.var_a0) {
               var41 = var22;
               var10001 = 1;
            } else {
               var41 = var22;
               var10001 = 2;
            }

            var41.var_126 = var10001;
         }

         var21 = var_553.var_d7;
         var3 = Class_48.sub_1a6(102943 - var21);
         var4 = Class_48.sub_1cb(102943 - var21);
         var5 = 1310720;
         var6 = var_553.var_49 + Class_48.sub_a0(var5, var4);
         var7 = var_553.var_ba + Class_48.sub_a0(var5, var3);
         Class_1e1[] var30 = var_505.var_210;
         Class_13c[] var32 = var_505.var_138;

         label389:
         for(var35 = 0; var35 < var30.length; ++var35) {
            Class_1e1 var11;
            if ((var12 = (var11 = var30[var35]).sub_5e()) == 1 || var12 == 11 || var12 == 26 || var12 == 28 || var12 == 51 || var12 == 62) {
               Class_13c var13 = var32[var11.var_22 & '\uffff'];
               Class_13c var14 = var32[var11.var_5c & '\uffff'];
               if (Class_3e6.sub_365(var_553.var_49, var_553.var_ba, var6, var7, var13.var_2b, var13.var_83, var14.var_2b, var14.var_83)) {
                  if ((Class_3aa.var_e8b & 1) == 0) {
                     Class_3aa.var_e8b = (byte)(Class_3aa.var_e8b | 1);
                  }

                  Class_197 var38;
                  byte var42;
                  switch(var12) {
                  case 1:
                     (var38 = sub_33a(var11.var_133.var_13d)).var_d5 = 1;
                     var38.var_87 = var11.var_e5.var_13d.var_ac;
                     break label389;
                  case 11:
                     if (Class_3aa.var_259 == 7 && var_ced[6] == 0) {
                        var_ded = "we'll need some dynamite|maybe i should look for some";
                        var_e3a = 50;
                        break label389;
                     }

                     Class_3aa.var_295 = Class_3aa.var_259++;
                     var_117 = var11.sub_90();
                     var42 = 1;
                     break;
                  case 26:
                     if (var_d98[0]) {
                        (var38 = sub_33a(var11.var_133.var_13d)).var_d5 = 1;
                        var38.var_87 = var11.var_e5.var_13d.var_ac;
                     } else {
                        var_ded = var_d98[1] ? "oops, i need another key..." : "oh, i need a key...";
                        var_e3a = 50;
                     }
                     break label389;
                  case 28:
                     if (var_d98[1]) {
                        (var38 = sub_33a(var11.var_133.var_13d)).var_d5 = 1;
                        var38.var_87 = var11.var_e5.var_13d.var_ac;
                     } else {
                        var_ded = var_d98[0] ? "oops, i need another key..." : "oh, i need a key...";
                        var_e3a = 50;
                     }
                     break label389;
                  case 51:
                     Class_3aa.var_295 = Class_3aa.var_259--;
                     var_117 = var11.sub_90();
                     var42 = -1;
                     break;
                  case 62:
                     Class_30a var15;
                     Class_2d8 var16;
                     if ((var16 = sub_34f(var15 = var11.var_133.var_13d)).var_126 == 0) {
                        if (var15.var_82 == var16.var_a0) {
                           var41 = var16;
                           var10001 = 1;
                        } else {
                           var41 = var16;
                           var10001 = 2;
                        }

                        var41.var_126 = var10001;
                     }
                  default:
                     break label389;
                  }

                  var_480 = var42;
                  break;
               }
            }
         }

         var_3e3 = false;
      }

      Class_30a var43;
      for(var21 = 0; var21 < var_b86.size(); ++var21) {
         Class_197 var23;
         if ((var23 = (Class_197)var_b86.elementAt(var21)).var_46 == var_5c2 && var23.var_d5 == 2) {
            var23.var_d5 = 1;
         }

         Class_197 var44;
         switch(var23.var_d5) {
         case 0:
            continue;
         case 1:
            var43 = var23.var_46;
            var43.var_ac = (short)(var43.var_ac + 2);
            if (var23.var_46.var_ac < var23.var_87) {
               continue;
            }

            var23.var_46.var_ac = var23.var_87;
            var44 = var23;
            var10001 = 100;
            break;
         case 2:
            var43 = var23.var_46;
            var43.var_ac = (short)(var43.var_ac - 2);
            if (var23.var_46.var_ac > var23.var_46.var_82) {
               continue;
            }

            var23.var_46.var_ac = var23.var_46.var_82;
            var44 = var23;
            var10001 = 0;
            break;
         default:
            ++var23.var_d5;
            if (var23.var_d5 < 200) {
               continue;
            }

            var44 = var23;
            var10001 = 2;
         }

         var44.var_d5 = var10001;
      }

      for(var21 = 0; var21 < var_bd3.size(); ++var21) {
         Class_2d8 var24;
         short var25;
         short var45;
         switch((var24 = (Class_2d8)var_bd3.elementAt(var21)).var_126) {
         case 0:
         default:
            continue;
         case 1:
            var43 = var24.var_41;
            var43.var_ac = (short)(var43.var_ac + 2);
            var43 = var24.var_41;
            var43.var_82 = (short)(var43.var_82 + 2);
            if (var24.var_41.var_82 < var24.var_f4) {
               continue;
            }

            var25 = (short)(var24.var_41.var_ac - var24.var_41.var_82);
            var24.var_41.var_82 = var24.var_f4;
            var43 = var24.var_41;
            var45 = var24.var_f4;
            break;
         case 2:
            var43 = var24.var_41;
            var43.var_ac = (short)(var43.var_ac - 2);
            var43 = var24.var_41;
            var43.var_82 = (short)(var43.var_82 - 2);
            if (var24.var_41.var_82 > var24.var_a0) {
               continue;
            }

            var25 = (short)(var24.var_41.var_ac - var24.var_41.var_82);
            var24.var_41.var_82 = var24.var_a0;
            var43 = var24.var_41;
            var45 = var24.var_a0;
         }

         var43.var_ac = (short)(var45 + var25);
         var24.var_126 = 0;
      }

      if (var_505.sub_50b()) {
         return true;
      } else {
         Class_445[] var26 = var_505.var_254;

         for(var3 = 0; var3 < var26.length; ++var3) {
            Class_445 var27;
            if ((var27 = var26[var3]) != null && var27.var_2cf != -1) {
               Class_71 var28;
               int var31;
               if (var27.var_2cf == 0) {
                  var28 = var27.var_c;
                  if (var_505.sub_115(var28.var_49, var28.var_ba).sub_ba(var_5c2)) {
                     if ((var7 = var28.var_49 - var_553.var_49) < 0) {
                        var7 = -var7;
                     }

                     if ((var31 = var28.var_ba - var_553.var_ba) < 0) {
                        var31 = -var31;
                     }

                     if (var7 + var31 <= 67108864 && var_505.sub_3ce(var_553, var28)) {
                        var27.var_2cf = 1;
                     }
                  }
               } else {
                  if ((var6 = (var28 = var27.var_c).var_49 - var_553.var_49) < 0) {
                     var6 = -var6;
                  }

                  if ((var7 = var28.var_ba - var_553.var_ba) < 0) {
                     var7 = -var7;
                  }

                  if (var6 + var7 > 67108864) {
                     var27.var_2cf = 0;
                  }
               }

               if (var27.var_296 > 0) {
                  --var27.var_296;
               }

               switch(var5 = var27.var_24) {
               case 10:
               case 12:
               default:
                  continue;
               case 3001:
               case 3002:
               case 3003:
               case 3004:
               case 3005:
               case 3006:
               }

               Class_71 var29;
               Class_30a var33;
               Class_445 var46;
               if (var27.var_296 == 0) {
                  switch(var27.var_2cf) {
                  case 1:
                     var27.var_2cf = 2;
                     var27.var_296 = (var_ea3.nextInt() & Integer.MAX_VALUE) % Class_3aa.var_177d[var_c79];
                     var27.var_1ce = 0;
                     break;
                  case 2:
                     if (((var6 = var_ea3.nextInt() & Integer.MAX_VALUE) & 1) == 0) {
                        var27.var_2cf = 3;
                        var27.var_296 = var6 % Class_3aa.var_180b[var_c79] + Class_3aa.var_17b5[var_c79];
                        var46 = var27;
                        var10001 = 2;
                     } else {
                        var27.var_2cf = 1;
                        var27.var_296 = (var_ea3.nextInt() & Integer.MAX_VALUE) % Class_3aa.var_1851[var_c79];
                        var46 = var27;
                        var10001 = 0;
                     }

                     var46.var_1ce = var10001;
                     break;
                  case 3:
                     var29 = var27.var_c;
                     var33 = var_505.sub_115(var29.var_49, var29.var_ba);
                     if (var_505.sub_3ce(var_553, var29)) {
                        var27.var_2cf = 4;
                        var27.var_296 = 2;
                        if (var5 != 3002) {
                           var27.var_1ce = 3;
                        }

                        if (var5 == 3001) {
                           Class_3aa.sub_84e(4, false, 100, 1);
                           var_505.sub_3f2(var29, var33);
                        } else if (var5 == 3002) {
                           Class_3aa.sub_84e(5, false, 80, 1);
                           var_505.sub_420(var29, var33);
                        } else {
                           label320: {
                              var31 = 0;
                              int[] var48;
                              switch(var5) {
                              case 3003:
                                 Class_3aa.sub_84e(2, false, 80, 0);
                                 var48 = Class_3aa.var_122a;
                                 break;
                              case 3004:
                                 Class_3aa.sub_84e(2, false, 80, 0);
                                 var48 = Class_3aa.var_125b;
                                 break;
                              case 3005:
                                 Class_3aa.sub_84e(2, false, 80, 0);
                                 var48 = Class_3aa.var_1284;
                                 break;
                              case 3006:
                                 Class_3aa.sub_84e(3, false, 80, 0);
                                 var48 = Class_3aa.var_12d2;
                                 break;
                              default:
                                 break label320;
                              }

                              var31 = var48[var_c79];
                           }

                           if (var31 > 0) {
                              Class_3aa.sub_882(var31 * 10);
                           }

                           if (sub_59b(var31)) {
                              return true;
                           }
                        }
                     } else {
                        var27.var_2cf = 2;
                        var27.var_296 = (var_ea3.nextInt() & Integer.MAX_VALUE) % Class_3aa.var_177d[var_c79];
                        var27.var_1ce = 0;
                     }
                     break;
                  case 4:
                     var27.var_2cf = 2;
                     var27.var_296 = (var_ea3.nextInt() & Integer.MAX_VALUE) % Class_3aa.var_177d[var_c79];
                     var27.var_1ce = 0;
                     break;
                  case 5:
                     var6 = var_ea3.nextInt() & Integer.MAX_VALUE;
                     var27.var_2cf = 3;
                     var27.var_296 = var6 % Class_3aa.var_180b[var_c79] + Class_3aa.var_17b5[var_c79];
                     var27.var_1ce = 2;
                     break;
                  case 6:
                     var27.var_2cf = -1;
                     if (var5 == 3002) {
                        var46 = var27;
                        var10001 = 5;
                     } else {
                        var46 = var27;
                        var10001 = 6;
                     }

                     var46.var_1ce = var10001;
                     var_505.sub_5cd(var27);
                  }
               }

               if (var27.var_2cf == 2) {
                  if ((var27.var_296 & 3) == 0) {
                     if (var27.var_1ce == 0) {
                        var46 = var27;
                        var10001 = 1;
                     } else {
                        var46 = var27;
                        var10001 = 0;
                     }

                     var46.var_1ce = var10001;
                  }

                  var29 = var27.var_c;
                  var33 = var_505.sub_115(var29.var_49, var29.var_ba);
                  var31 = var29.var_49 - var_553.var_49;
                  int var34 = var29.var_ba - var_553.var_ba;
                  int var36;
                  if ((var35 = Class_48.sub_11f(var31, var34)) > var_f32) {
                     var36 = Class_48.sub_a0(Class_48.sub_d7(var31, var35), var27.sub_27());
                     var12 = Class_48.sub_a0(Class_48.sub_d7(var34, var35), var27.sub_27());
                     int var37;
                     if ((var_ea3.nextInt() & Integer.MAX_VALUE) % Class_3aa.var_1ad2[var_c79] == 0) {
                        int var47;
                        if ((var_ea3.nextInt() & 1) == 0) {
                           var37 = var36;
                           var36 += -var12;
                           var10000 = var12;
                           var47 = var37;
                        } else {
                           var37 = var36;
                           var36 += var12;
                           var10000 = var12;
                           var47 = -var37;
                        }

                        var12 = var10000 + var47;
                     }

                     var37 = var36 > 0 ? var36 : -var36;
                     int var39 = var12 > 0 ? var12 : -var12;
                     int var40 = var37 >> 18;
                     int var17;
                     if ((var17 = var39 >> 18) > var40) {
                        var40 = var17;
                     }

                     ++var40;
                     int var18 = var36 / var40;
                     int var19 = var12 / var40;

                     for(int var20 = 0; var20 < var40; ++var20) {
                        var_563.var_49 = var29.var_49 - var18;
                        var_563.var_ba = var29.var_ba - var19;
                        if (!var_505.sub_1dd(var27, var_563, var33)) {
                           break;
                        }

                        var29.var_49 = var_563.var_49;
                        var29.var_ba = var_563.var_ba;
                     }
                  } else {
                     var36 = var_ea3.nextInt() & Integer.MAX_VALUE;
                     var27.var_2cf = 3;
                     var27.var_296 = var36 % Class_3aa.var_180b[var_c79] + Class_3aa.var_17b5[var_c79];
                     var27.var_1ce = 2;
                  }
               }
            }
         }

         if (var_5c2.sub_5c() == 555) {
            Class_3aa.sub_882(10);
            if (sub_59b(1)) {
               return true;
            }
         }

         if (var_ef9 < 16 && var_ef9 > 0) {
            --var_ef9;
         }

         var_553.sub_79(39322, 65536, 39322, 26214);
         return false;
      }
   }

   private static Class_197 sub_33a(Class_30a var0) {
      for(int var1 = 0; var1 < var_b86.size(); ++var1) {
         Class_197 var2;
         if ((var2 = (Class_197)var_b86.elementAt(var1)).var_46 == var0) {
            return var2;
         }
      }

      Class_197 var3;
      (var3 = new Class_197()).var_46 = var0;
      var_b86.addElement(var3);
      return var3;
   }

   private static Class_2d8 sub_34f(Class_30a var0) {
      for(int var1 = 0; var1 < var_bd3.size(); ++var1) {
         Class_2d8 var2;
         if ((var2 = (Class_2d8)var_bd3.elementAt(var1)).var_41 == var0) {
            return var2;
         }
      }

      Class_2d8 var6;
      (var6 = new Class_2d8()).var_126 = 0;
      var6.var_a0 = 32767;
      var6.var_f4 = -32768;
      Class_1e1[] var7 = var_505.var_210;

      for(int var3 = 0; var3 < var7.length; ++var3) {
         Class_1e1 var4;
         if ((var4 = var7[var3]).sub_5e() == 62 && var4.var_133.var_13d == var0) {
            Class_30a var5;
            if ((var5 = var4.var_e5.var_13d).var_82 > var6.var_f4) {
               var6.var_f4 = var5.var_82;
            }

            if (var5.var_82 < var6.var_a0) {
               var6.var_a0 = var5.var_82;
            }
         }
      }

      var6.var_41 = var0;
      var_bd3.addElement(var6);
      return var6;
   }

   private static void sub_391() {
      var_161 = false;
      var_1ed = false;
      var_1bf = false;
      var_1fa = false;
      var_161 = false;
      var_223 = false;
      var_1bf = false;
      var_280 = false;
      var_2cd = false;
      var_2f7 = false;
      var_344 = false;
      var_364 = false;
      var_3e3 = false;
      var_446 = false;
      var_3b8 = false;
      var_480 = 0;
      var_4c8 = 0;
   }

   private static void sub_3f5(Class_318 var0) {
      var_8f4 = var0;
   }

   private static void sub_410(Class_318 var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      int var10000;
      label68: {
         if (var0.var_cf > 0) {
            var10000 = var2 - var0.var_cf * var5 / var0.var_11;
         } else {
            if (var0.var_cf >= 0) {
               break label68;
            }

            var10000 = var2 + var0.var_cf;
         }

         var2 = var10000;
      }

      label62: {
         if (var0.var_121 > 0) {
            var10000 = var3 - var0.var_121 * var6 / var0.var_6f;
         } else {
            if (var0.var_121 >= 0) {
               break label62;
            }

            var10000 = var3 + var0.var_121;
         }

         var3 = var10000;
      }

      int var8 = 0;
      int var9 = var5;
      if (0 + var2 < 240 && var5 + var2 >= 0) {
         if (0 + var2 < 0) {
            var8 = 0 - (0 + var2);
         }

         if (var5 + var2 >= 240) {
            var9 = var5 + (239 - (var5 + var2));
         }

         short var11;
         int var12;
         int var13;
         int var14;
         int var16;
         label45: {
            short var10 = var0.var_11;
            var11 = var0.var_6f;
            var12 = (var10 << 16) / (var5 + 1);
            var14 = (var13 = (var8 - 0) * var12) >>> 16;
            var16 = var4 >> 22;
            byte var21;
            if ((var16 = var_10a4 && var16 < 3 ? var1 + (4 >> var16) : var1 - var16) < 0) {
               var21 = 0;
            } else {
               if (var16 <= 15) {
                  break label45;
               }

               var21 = 15;
            }

            var16 = var21;
         }

         int[] var17 = var0.var_191[var16];
         int var18 = var3 + var6;

         for(int var19 = var8; var19 <= var9; ++var19) {
            sub_4e2(var0.sub_47(var14), var14 & 1, var17, var19 + var2, var3, var18, 0, var11);
            var14 = (var13 += var12) >>> 16;
         }

      }
   }

   private static void sub_420(Class_30a var0, Class_318 var1, Class_318 var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14, int var15, int var16, int var17, int var18, int var19, int var20, int var21, int var22, int var23, int var24, int var25) {
      int var26 = var3;
      int var27 = var9;
      if (var3 < 240 && var9 >= 0) {
         short[] var28 = Class_110.var_144;
         short[] var29 = Class_110.var_130;
         if (var3 < 0) {
            var26 = 0;
         }

         if (var9 >= 240) {
            var27 = 239;
         }

         short var30 = var1.var_6f;
         short var31 = var2.var_6f;
         int var32 = var9 - var3 + 1;
         int var33 = var26 - var3;
         int var34 = (var10 - var4 << 16) / var32;
         int var35 = (var4 << 16) + var33 * var34 + '耀';
         int var36 = (var11 - var5 << 16) / var32;
         int var37 = (var5 << 16) + var33 * var36 + '耀';
         int var38 = (var12 - var6 << 16) / var32;
         int var39 = (var6 << 16) + var33 * var38 + '耀';
         int var40 = (var13 - var7 << 16) / var32;
         int var41 = (var7 << 16) + var33 * var40 + '耀';
         var_9fa = Integer.MIN_VALUE;
         var_9e4 = Integer.MIN_VALUE;
         var_9b4 = Integer.MIN_VALUE;
         var_958 = Integer.MIN_VALUE;
         var_a34 = Integer.MIN_VALUE;
         var_a27 = Integer.MIN_VALUE;
         short var42 = (short)var0.var_10;
         Class_358 var43 = var0.var_214;
         Class_358 var44 = var0.var_262;
         int var45 = var0.var_282 = var0.sub_15();
         long var46;
         long var48 = (var46 = (long)(var8 - var14)) >> 16;
         long var50 = (long)(var9 - var3 + 1 << 16) * (long)var14;
         var0.var_2bf = var24 * 120 >> 16;
         var0.var_2f0 = var25 * 120 >> 16;

         for(int var52 = var26; var52 <= var27; ++var52) {
            if (var28[var52] < var29[var52]) {
               int var59;
               int var60;
               label114: {
                  long var53 = (long)(var52 - var3 << 16);
                  long var55 = (long)var8 * var53;
                  long var57 = var50 + var53 * var46;
                  var59 = (int)(var55 / (var57 >> 16));
                  var60 = (int)((long)var8 - var48 * (long)var59 >> 22);
                  byte var10000;
                  if ((var60 = var_10a4 && var60 < 3 ? var45 + (4 >> var60) : var45 - var60) < 0) {
                     var10000 = 0;
                  } else {
                     if (var60 <= 15) {
                        break label114;
                     }

                     var10000 = 15;
                  }

                  var60 = var10000;
               }

               int var61 = var35 >> 16;
               int var62 = var37 >> 16;
               int var63 = var39 >> 16;
               int var64 = var41 >> 16;
               int var65 = (int)((long)var59 * (long)var16 >> 16) + var15 >> 16;
               if (var39 >= var41 && var44 != null) {
                  sub_477(var42, var52, var64 + 1, var25);
                  if (var64 < var29[var52]) {
                     var29[var52] = (short)var64;
                  }
               }

               if (var43 == null) {
                  sub_523(var52, 0, var61, var23);
               }

               label105: {
                  int var10001;
                  int var10002;
                  short[] var66;
                  if (var35 < var37) {
                     sub_50e(var1.sub_7b(var65), var65 & 1, var1.var_191[var60], var52, var61, var62, var17, var18, var30);
                     if (var43 != null) {
                        sub_4be(var42, var52, var61, var24);
                     }

                     if (var62 <= var28[var52] || var1 == var_894) {
                        break label105;
                     }

                     var66 = var28;
                     var10001 = var52;
                     var10002 = var62;
                  } else {
                     if (var43 == null) {
                        break label105;
                     }

                     sub_4be(var42, var52, var61, var24);
                     if (var61 <= var28[var52]) {
                        break label105;
                     }

                     var66 = var28;
                     var10001 = var52;
                     var10002 = var61;
                  }

                  var66[var10001] = (short)var10002;
               }

               if (var44 == null) {
                  sub_523(var52, var64 + 1, 287, var23);
               }

               if (var39 < var41) {
                  sub_50e(var2.sub_7b(var65), var65 & 1, var2.var_191[var60], var52, var63, var64, var19, var20, var31);
                  if (var44 != null) {
                     sub_477(var42, var52, var64 + 1, var25);
                  }

                  if (var63 < var29[var52] && var2 != var_894) {
                     var29[var52] = (short)var63;
                  }
               }
            }

            if (var37 == var39) {
               var29[var52] = var28[var52];
            }

            var35 += var34;
            var37 += var36;
            var39 += var38;
            var41 += var40;
         }

         short var67;
         int var68;
         if (var_9e4 >= 0) {
            var67 = (short)var_a34;

            for(var68 = var_9e4; var68 <= var_9fa; ++var68) {
               var_a80.sub_82(var_926[var68], var67, var42, var68);
            }
         }

         if (var_958 >= 0) {
            var67 = (short)var_a27;

            for(var68 = var_958; var68 <= var_9b4; ++var68) {
               var_a80.sub_82(var_926[var68], var67, var42, var68);
            }
         }

      }
   }

   public static void sub_430(int var0, int var1, int var2, byte[] var3, int[][] var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      int var11;
      int var14;
      int var15;
      label31: {
         var11 = var2 * 240;
         int var12;
         int var13 = (var12 = var2 - 144) < 0 ? -var_acc[-var12] : var_acc[var12];
         var15 = (var14 = var9 * var13 >> 8) >> 14;
         byte var10000;
         if ((var15 = var_10a4 && var15 < 3 ? var5 + (4 >> var15) : var5 - var15) < 0) {
            var10000 = 0;
         } else {
            if (var15 <= 15) {
               break label31;
            }

            var10000 = 15;
         }

         var15 = var10000;
      }

      int[] var16 = var4[var15];
      int var17 = var_ab4[var0];
      int var18 = var_ab4[var1];
      int var19 = (var6 + (var7 * var17 >> 14)) * var14 - var8 >> 6;
      int var20 = (var7 - (var6 * var17 >> 14)) * var14 - var10;
      int var21 = (var18 - var17) * var_acc[var1 - var0 + 1] >> 16;
      int var22 = (var7 * var21 >> 14) * var14 >> 6;
      int var23 = (-var6 * var21 >> 14) * var14;
      var0 += var11;
      var1 += var11;
      int[] var24 = var_843;

      for(int var25 = var0; var25 <= var1; ++var25) {
         var24[var25] = var16[var3[(var19 & 16515072) + (var20 & 1056964608) >> 18]];
         var19 += var22;
         var20 += var23;
      }

   }

   private static void sub_477(short var0, int var1, int var2, int var3) {
      short var4 = Class_110.var_144[var1];
      short var5 = Class_110.var_130[var1];
      if (var2 <= var5 && var2 > 144 && var3 > 0) {
         int var6 = var2;
         short var7 = var5;
         if (var2 < var4) {
            var6 = var4;
         }

         short var8 = (short)var1;
         short var9;
         int var10;
         if (var_a34 == var1 - 1) {
            var9 = (short)var_a34;
            var10 = var6 > var_9fa + 1 ? var6 : var_9fa + 1;
            int var11 = var5 < var_9e4 - 1 ? var5 : var_9e4 - 1;
            int var12 = var_9e4 > var5 + 1 ? var_9e4 : var5 + 1;
            int var13 = var_9fa < var6 - 1 ? var_9fa : var6 - 1;

            int var14;
            for(var14 = var6; var14 <= var11; ++var14) {
               var_926[var14] = var8;
            }

            for(var14 = var10; var14 <= var7; ++var14) {
               var_926[var14] = var8;
            }

            for(var14 = var_9e4; var14 <= var13; ++var14) {
               var_a80.sub_82(var_926[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9fa; ++var14) {
               var_a80.sub_82(var_926[var14], var9, var0, var14);
            }
         } else {
            if (var_9e4 >= 0) {
               var9 = (short)var_a34;

               for(var10 = var_9e4; var10 <= var_9fa; ++var10) {
                  var_a80.sub_82(var_926[var10], var9, var0, var10);
               }
            }

            for(int var15 = var6; var15 <= var7; ++var15) {
               var_926[var15] = var8;
            }
         }

         var_a34 = var1;
         var_9e4 = var6;
         var_9fa = var7;
      }
   }

   private static void sub_4be(short var0, int var1, int var2, int var3) {
      short var4 = Class_110.var_144[var1];
      short var5 = Class_110.var_130[var1];
      if (var2 >= var4 && var2 < 144 && var3 < 0) {
         int var7 = var2;
         if (var2 > var5) {
            var7 = var5;
         }

         short var8 = (short)var1;
         short var9;
         int var10;
         if (var_a27 == var1 - 1) {
            var9 = (short)var_a27;
            var10 = var4 > var_9b4 + 1 ? var4 : var_9b4 + 1;
            int var11 = var7 < var_958 - 1 ? var7 : var_958 - 1;
            int var12 = var_958 > var7 + 1 ? var_958 : var7 + 1;
            int var13 = var_9b4 < var4 - 1 ? var_9b4 : var4 - 1;

            int var14;
            for(var14 = var4; var14 <= var11; ++var14) {
               var_926[var14] = var8;
            }

            for(var14 = var10; var14 <= var7; ++var14) {
               var_926[var14] = var8;
            }

            for(var14 = var_958; var14 <= var13; ++var14) {
               var_a80.sub_82(var_926[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9b4; ++var14) {
               var_a80.sub_82(var_926[var14], var9, var0, var14);
            }
         } else {
            if (var_958 >= 0) {
               var9 = (short)var_a27;

               for(var10 = var_958; var10 <= var_9b4; ++var10) {
                  var_a80.sub_82(var_926[var10], var9, var0, var10);
               }
            }

            for(int var15 = var4; var15 <= var7; ++var15) {
               var_926[var15] = var8;
            }
         }

         var_a27 = var1;
         var_958 = var4;
         var_9b4 = var7;
      }
   }

   private static void sub_4e2(byte[] var0, int var1, int[] var2, int var3, int var4, int var5, int var6, int var7) {
      short var8 = Class_110.var_144[var3];
      short var9 = Class_110.var_130[var3];
      if (var4 <= var9 && var5 >= var8) {
         int var10 = var4;
         int var11 = var5;
         if (var5 > var9) {
            var11 = var9;
         }

         if (var4 < var8) {
            var10 = var8;
         }

         if (var10 < 0) {
            var10 = 0;
         }

         if (var11 >= 288) {
            var11 = 287;
         }

         if (var11 >= 0 && var10 < 288 && var5 > var4 && var6 <= var0.length) {
            int var12 = var10 * 240 + var3;
            int var13 = var11 * 240 + var3;
            int var14 = (var14 = var5 - var4) > 288 ? (var7 << 16) / var14 : var7 * var_acc[var14];
            int var15 = (var10 - var4) * var14 + (var6 << 16);
            int var16 = var0.length;
            int[] var17 = var_843;
            int var18;
            int var19;
            int var20;
            if (var1 == 0) {
               for(var18 = var12; var18 <= var13; var18 += 240) {
                  if ((var19 = var15 >>> 16) < var16 && (var20 = var0[var19] >> 4 & 15) != 0) {
                     var17[var18] = var2[var20];
                  }

                  var15 += var14;
               }

            } else {
               for(var18 = var12; var18 <= var13; var18 += 240) {
                  if ((var19 = var15 >>> 16) < var16 && (var20 = var0[var19] & 15) != 0) {
                     var17[var18] = var2[var20];
                  }

                  var15 += var14;
               }

            }
         }
      }
   }

   private static void sub_50e(byte[] var0, int var1, int[] var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      short var9 = Class_110.var_144[var3];
      short var10 = Class_110.var_130[var3];
      if (var5 >= var9 && var4 <= var10) {
         int var11 = var4;
         int var12 = var5;
         if (var4 < var9) {
            var11 = var9;
         }

         if (var5 > var10) {
            var12 = var10;
         }

         int var13 = var11 * 240 + var3;
         int var14 = var12 * 240 + var3;
         int var15 = (var15 = var5 - var4) > 288 ? (var7 - 1 << 16) / var15 : (var7 - 1) * var_acc[var15];
         int var16 = var8 - 1;
         int var17 = (var11 - var4) * var15 + ((var6 & var16) << 16);
         int[] var18 = var_843;
         int var19;
         switch(var8 + var1) {
         case 16:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 983040) >> 16] >> 4 & 15];
               var17 += var15;
            }

            return;
         case 17:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 983040) >> 16] & 15];
               var17 += var15;
            }

            return;
         case 64:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 4128768) >> 16] >> 4 & 15];
               var17 += var15;
            }

            return;
         case 65:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 4128768) >> 16] & 15];
               var17 += var15;
            }

            return;
         case 128:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 8323072) >> 16] >> 4 & 15];
               var17 += var15;
            }

            return;
         case 129:
            for(var19 = var13; var19 <= var14; var19 += 240) {
               var18[var19] = var2[var0[(var17 & 8323072) >> 16] & 15];
               var17 += var15;
            }
         default:
         }
      }
   }

   private static void sub_523(int var0, int var1, int var2, int var3) {
      short var4 = Class_110.var_144[var0];
      short var5 = Class_110.var_130[var0];
      int var6 = var1;
      int var7 = var2;
      if (var1 < var4) {
         var6 = var4;
      }

      if (var2 > var5) {
         var7 = var5;
      }

      int var8;
      int var9 = Class_48.sub_1cb(var8 = Class_48.sub_a0(var0 - 120 << 16, var_1103));
      int var10 = Class_48.sub_a0(var0 - 120, var_10bb);
      int var11 = Class_48.sub_1a6(var8);
      int var13 = Class_48.sub_a0(Class_48.sub_a0(102943, var11 + Class_48.sub_a0(var9, var10)) + var3, var_1171) >> 8;
      byte[] var14 = var_8f4.sub_7b(var13);
      int[] var15 = var_8f4.var_191[8];
      int var16 = var6 * 240 + var0;
      int var17 = var7 * 240 + var0;
      int var18;
      int var19 = -(var18 = Class_48.sub_a0(var9 * 200, var_1142)) * (144 - var6) + 6553600;
      int[] var20 = var_843;
      int var21;
      if ((var13 & 1) == 0) {
         for(var21 = var16; var21 <= var17; var21 += 240) {
            var20[var21] = var15[var14[var19 >> 16 & 127] >> 4 & 15];
            var19 += var18;
         }

      } else {
         for(var21 = var16; var21 <= var17; var21 += 240) {
            var20[var21] = var15[var14[var19 >> 16 & 127] & 15];
            var19 += var18;
         }

      }
   }

   public static int sub_558(int var0) {
      ++var0;
      int var1 = var0;
      if (var0 > 7) {
         var0 = 0;
      } else {
         var0 = 0;

         for(int var2 = var1; var2 <= 7; ++var2) {
            int var3 = var2 != 3 && var2 != 4 ? var2 : 1;
            if (var_c8b[var2] && var_ced[var3] > 0) {
               var0 = var2;
               break;
            }
         }
      }

      return var0;
   }

   public static int sub_577(int var0) {
      if (var0 == 0) {
         return var0;
      } else {
         int var1 = var0 != 3 && var0 != 4 ? var0 : 1;
         if (var_ced[var1] > 0) {
            return var0;
         } else {
            int var2 = 0;

            for(int var3 = 7; var3 > 0; --var3) {
               if (var3 != 6) {
                  var1 = var3 != 3 && var3 != 4 ? var3 : 1;
                  if (var_c8b[var3] && var_ced[var1] > 0) {
                     var2 = var3;
                     break;
                  }
               }
            }

            if (var2 == 0 && var_c8b[6] && var_ced[6] > 0) {
               var2 = 6;
            }

            return var2;
         }
      }
   }

   public static boolean sub_59b(int var0) {
      var_c18 -= var0;
      if (var_c18 < 0) {
         var0 = -var_c18;
         var_c18 = 0;
      } else {
         var0 = 0;
      }

      var_eba = true;
      var_c07 -= var0;
      if (var_c07 <= 0) {
         var_c07 = 0;
         return true;
      } else {
         return false;
      }
   }

   private static void sub_5f4() {
      var_75 = new Class_358[128];
      var_85 = new Class_318[256];
      var_e1 = new Hashtable();
   }

   private static void sub_60e() {
      var_505 = null;
      var_37 = 0;

      int var0;
      for(var0 = 0; var0 < 128; ++var0) {
         var_75[var0] = null;
      }

      for(var0 = 0; var0 < 256; ++var0) {
         var_85[var0] = null;
      }

      var_e1.clear();
   }

   public static boolean sub_61c(String var0, boolean var1) {
      sub_60e();

      try {
         InputStream var3 = (new Object()).getClass().getResourceAsStream(var0);
         DataInputStream var4 = new DataInputStream(var3);
         var_505 = new Class_3e6();
         var4.readByte();
         Class_13c[] var6 = new Class_13c[sub_7c5(var4) / 4];

         for(int var7 = 0; var7 < var6.length; ++var7) {
            var6[var7] = new Class_13c(sub_775(var4) << 16, sub_775(var4) << 16);
         }

         var_505.sub_9a(var6);
         Class_1e1[] var25 = new Class_1e1[sub_7c5(var4) / 11];

         int var8;
         short var14;
         short var15;
         for(var8 = 0; var8 < var25.length; ++var8) {
            short var9 = sub_775(var4);
            short var10 = sub_775(var4);
            byte var11 = var4.readByte();
            byte var12 = var4.readByte();
            byte var13 = var4.readByte();
            var14 = sub_775(var4);
            var15 = sub_775(var4);
            var25[var8] = new Class_1e1(var9, var10, var14, var15, var11, var12, var13);
         }

         var_505.var_210 = var25;
         int var5 = sub_7c5(var4);
         if (var_117 == 0) {
            var_117 = 1;
         }

         var8 = var5 / 10;
         Class_445[] var26 = null;
         if (var1) {
            var26 = new Class_445[var8];
         }

         short var31;
         short var33;
         for(int var27 = 0; var27 < var8; ++var27) {
            short var29 = sub_775(var4);
            var31 = sub_775(var4);
            var33 = sub_775(var4);
            var14 = sub_775(var4);
            var15 = sub_775(var4);
            Class_445[] var10000;
            int var10001;
            Class_445 var10002;
            if (var14 >= 1 && var14 <= 4) {
               if (var_117 == var14) {
                  var_505.var_383 = new Class_71(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943);
               }

               if (!var1) {
                  continue;
               }

               var10000 = var26;
               var10001 = var27;
               var10002 = null;
            } else {
               if (!var1) {
                  continue;
               }

               var10000 = var26;
               var10001 = var27;
               var10002 = new Class_445(new Class_71(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943), var33, var14, var15);
            }

            var10000[var10001] = var10002;
         }

         if (var1) {
            var_505.var_254 = var26;
         }

         Class_8e[] var28 = new Class_8e[sub_7c5(var4) / 8];

         byte var16;
         byte var38;
         for(int var30 = 0; var30 < var28.length; ++var30) {
            var31 = sub_775(var4);
            var33 = sub_775(var4);
            byte var36 = sub_65b(var4.readByte());
            var38 = sub_65b(var4.readByte());
            var16 = sub_65b(var4.readByte());
            byte var17 = var4.readByte();
            var28[var30] = new Class_8e(var36, var16, var38, var17, var31, var33);
            if (var36 != 0) {
               sub_80f(var36);
            }

            if (var38 != 0) {
               sub_80f(var38);
            }

            if (var16 != 0) {
               sub_80f(var16);
            }
         }

         var_505.var_370 = var28;
         Class_30a[] var32 = new Class_30a[sub_7c5(var4) / 12];
         short var42 = 0;

         while(true) {
            var31 = var42;
            short var18;
            short var44;
            if (var42 >= var32.length) {
               var_505.var_32c = var32;
               Class_241[] var34 = new Class_241[sub_7c5(var4) / 12];

               short var41;
               int var48;
               for(int var35 = 0; var35 < var34.length; ++var35) {
                  var14 = sub_775(var4);
                  var15 = sub_775(var4);
                  var41 = sub_775(var4);
                  var44 = sub_775(var4);
                  int var46 = sub_775(var4) & '\uffff';
                  var48 = sub_775(var4) & '\uffff';
                  var34[var35] = new Class_241(var14 << 16, var15 << 16, var41 << 16, var44 << 16, var46, var48);
               }

               var_505.var_3c0 = var34;
               Class_110[] var37 = new Class_110[(var5 = sub_7c5(var4)) / 4];

               for(int var39 = 0; var39 < var37.length; ++var39) {
                  var15 = sub_775(var4);
                  var41 = sub_775(var4);
                  var37[var39] = new Class_110(var15, var41);
               }

               var_505.var_401 = var37;
               Class_241.var_24c = new Class_110[var5 / 4];
               Class_21c[] var40 = new Class_21c[sub_7c5(var4) / 9];

               int var43;
               for(var43 = 0; var43 < var40.length; ++var43) {
                  var41 = sub_775(var4);
                  var44 = sub_775(var4);
                  var18 = sub_775(var4);
                  boolean var49 = var4.readByte() == 0;
                  short var20 = sub_775(var4);
                  var40[var43] = new Class_21c(var41, var44, var18, var49, var20);
               }

               var_505.var_433 = var40;
               sub_7c5(var4);
               boolean[][] var45 = new boolean[var43 = var_505.var_32c.length][var43];
               int var47 = 0;
               var48 = 0;

               while(var48 < var43) {
                  int var50 = var4.readByte() & 255;

                  for(int var21 = 0; var21 < 8 & var48 < var43; ++var21) {
                     var45[var47][var48] = (var50 & 1 << var21) == 1 << var21;
                     ++var47;
                     if (var47 >= var43) {
                        var47 = 0;
                        ++var48;
                     }
                  }
               }

               for(var48 = 0; var48 < var43; ++var48) {
                  var_505.var_32c[var48].var_1dd = var45[var48];
               }

               var4.close();
               break;
            }

            var33 = sub_775(var4);
            var14 = sub_775(var4);
            var38 = sub_65b(var4.readByte());
            var16 = sub_65b(var4.readByte());
            var44 = sub_775(var4);
            var18 = sub_775(var4);
            short var19 = sub_775(var4);
            var32[var31] = new Class_30a(var31, var33, var14, var38, var16, (short)(var44 >> 4 & 15), var18, var19);
            if (var38 != 0) {
               sub_855(var38);
            }

            if (var16 != 0) {
               sub_855(var16);
            }

            var42 = (short)(var31 + 1);
         }
      } catch (Exception var22) {
         return false;
      } catch (OutOfMemoryError var23) {
         return false;
      }

      int var24;
      if ((var24 = var0.lastIndexOf(47)) != -1) {
         var0.substring(0, var24 + 1);
      }

      var_37 = 1;
      return true;
   }

   private static byte sub_65b(byte var0) {
      byte var10000;
      switch(var0) {
      case 6:
         return 5;
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      case 37:
      case 38:
      case 41:
      case 44:
      case 45:
      case 46:
      case 49:
      default:
         return var0;
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
         return 17;
      case 39:
      case 40:
         var10000 = 35;
         break;
      case 42:
      case 43:
         return 41;
      case 47:
      case 48:
         var10000 = 46;
         break;
      case 50:
         var10000 = 49;
      }

      sub_80f(var10000);
      return var0;
   }

   public static boolean sub_691(String var0, int var1, String var2, int var3) {
      if (var_37 < 1) {
         throw new IllegalStateException();
      } else if (var_37 > 1) {
         throw new IllegalStateException();
      } else {
         try {
            int var4 = 0;
            Hashtable var5 = new Hashtable();
            int var6 = 1;

            label233:
            while(true) {
               InputStream var7;
               DataInputStream var8;
               short var9;
               short var10;
               short var12;
               short var13;
               byte var15;
               int var16;
               short var17;
               short var18;
               int var19;
               int var40;
               int[] var41;
               int[][] var42;
               if (var6 > var1) {
                  for(var6 = 1; var6 <= var3; ++var6) {
                     if ((var7 = (new Object()).getClass().getResourceAsStream(var2 + Integer.toString(var6))) == null) {
                        throw new IllegalStateException();
                     }

                     if ((sub_6ea(var8 = new DataInputStream(var7)) & '\uffff') != 39251) {
                        throw new IllegalStateException();
                     }

                     var9 = sub_6ea(var8);
                     var10 = sub_6ea(var8);
                     sub_73f(var8);

                     for(var16 = 0; var16 < var9; ++var16) {
                        var15 = var8.readByte();
                        var12 = sub_6ea(var8);
                        var13 = sub_6ea(var8);
                        var17 = sub_6ea(var8);
                        var18 = sub_6ea(var8);
                        short var43 = sub_6ea(var8);
                        short var45 = sub_6ea(var8);
                        int var44;
                        int var47 = (var44 = var12 * var13 * var45) / 8;
                        if (var44 % 8 > 0) {
                           ++var47;
                        }

                        if (sub_8bc(var15)) {
                           Class_318 var48 = new Class_318(var15, var12, var13, var17, var18);
                           byte[] var49 = new byte[var47];
                           var8.readFully(var49, 0, var47);
                           byte[] var26 = null;

                           for(int var27 = 0; var27 < var12; ++var27) {
                              if ((var27 & 1) == 0) {
                                 var26 = new byte[var13];
                              }

                              sub_920(var49, var27 * var13, var26, 0, var13, var45, var27 & 1);
                              var48.sub_2a(var27, var26);
                           }

                           var_85[var15 + 128] = var48;
                           var5.put(new Byte(var15), new Integer(var4 + var43));
                        } else {
                           sub_7e1(var8, var47);
                        }
                     }

                     for(var16 = 0; var16 < var10; ++var16) {
                        var40 = sub_73f(var8);
                        if (!var5.contains(new Integer(var4 + var16))) {
                           sub_7e1(var8, 4 * var40);
                        } else {
                           var41 = new int[var40];

                           for(var19 = 0; var19 < var40; ++var19) {
                              var41[var19] = sub_73f(var8);
                           }

                           var42 = Class_318.sub_f8(var41);
                           var_e1.put(Integer.toString(var4 + var16), var42);
                        }
                     }

                     var4 += var10;
                     var8.close();
                  }

                  for(var6 = 0; var6 < 128; ++var6) {
                     Class_358 var30;
                     if ((var30 = var_75[var6]) != null) {
                        if (var30.var_84 == null) {
                           throw new IllegalStateException();
                        }

                        var30.var_bb = (int[][])((int[][])var_e1.get(((Integer)((Integer)var5.get(new Byte(var30.var_115)))).toString()));
                     }
                  }

                  byte[][] var31 = (byte[][])null;
                  byte[][] var32 = (byte[][])null;
                  byte[][] var33 = (byte[][])null;
                  Class_318 var34;
                  if ((var34 = var_85[163]) != null) {
                     var31 = var34.var_157;
                  }

                  if ((var34 = var_85[174]) != null) {
                     var32 = var34.var_157;
                  }

                  if ((var34 = var_85[177]) != null) {
                     var33 = var34.var_157;
                  }

                  for(int var35 = 0; var35 < 256; ++var35) {
                     Class_318 var11;
                     if ((var11 = var_85[var35]) != null) {
                        byte var38;
                        label150: {
                           byte var10000;
                           switch(var38 = var11.var_1d6) {
                           case 35:
                              var11.sub_9d(var31, 0, 0, 64, 128, 0, 0, true);
                           case 36:
                           case 37:
                           case 38:
                           case 41:
                           case 42:
                           case 43:
                           case 44:
                           case 45:
                           default:
                              break label150;
                           case 39:
                              var11.sub_9d(var31, 0, 0, 64, 128, 0, 0, true);
                              var11.sub_9d(var31, 0, 128, 64, 18, 0, 0, false);
                              var11.sub_9d(var31, 64, 17, 10, 111, 54, 17, false);
                              var10000 = 35;
                              break;
                           case 40:
                              var11.sub_9d(var31, 0, 0, 64, 128, 0, 0, true);
                              var11.sub_9d(var31, 0, 128, 64, 18, 0, 0, false);
                              var11.sub_9d(var31, 74, 17, 10, 111, 0, 17, false);
                              var10000 = 35;
                              break;
                           case 46:
                              var11.sub_9d(var32, 0, 0, 64, 128, 0, 0, true);
                              break label150;
                           case 47:
                              var11.sub_9d(var32, 0, 0, 64, 128, 0, 0, true);
                              var11.sub_9d(var32, 64, 0, 14, 128, 50, 0, false);
                              var10000 = 46;
                              break;
                           case 48:
                              var11.sub_9d(var32, 0, 0, 64, 128, 0, 0, true);
                              var11.sub_9d(var32, 64, 0, 14, 128, 50, 0, false);
                              var11.sub_9d(var32, 78, 0, 20, 24, 19, 43, false);
                              var10000 = 46;
                              break;
                           case 49:
                              var11.sub_9d(var33, 0, 0, 64, 128, 0, 0, true);
                              break label150;
                           case 50:
                              var11.sub_9d(var33, 0, 0, 64, 128, 0, 0, true);
                              var11.sub_9d(var33, 64, 0, 20, 27, 25, 19, false);
                              var10000 = 49;
                           }

                           var38 = var10000;
                        }

                        if (var11.var_11 != 0) {
                           var11.var_191 = (int[][])((int[][])var_e1.get(((Integer)((Integer)var5.get(new Byte(var38)))).toString()));
                        }
                     }
                  }

                  Class_30a[] var36 = var_505.var_32c;
                  int var37 = 0;

                  while(true) {
                     if (var37 >= var36.length) {
                        break label233;
                     }

                     Class_30a var39;
                     (var39 = var36[var37]).var_214 = sub_cb(var39.var_fd);
                     var39.var_262 = sub_cb(var39.var_dd);
                     ++var37;
                  }
               }

               if ((var7 = (new Object()).getClass().getResourceAsStream(var0 + Integer.toString(var6))) == null) {
                  throw new IllegalStateException();
               }

               if ((sub_6ea(var8 = new DataInputStream(var7)) & '\uffff') != 39252) {
                  throw new IllegalStateException();
               }

               var9 = sub_6ea(var8);
               var10 = sub_6ea(var8);
               sub_73f(var8);

               for(var16 = 0; var16 < var9; ++var16) {
                  var15 = var8.readByte();
                  var12 = sub_6ea(var8);
                  var13 = sub_6ea(var8);
                  var17 = sub_6ea(var8);
                  var18 = sub_6ea(var8);
                  int var20 = (var19 = var12 * var13 * var18) / 8;
                  if (var19 % 8 > 0) {
                     ++var20;
                  }

                  if (sub_887(var15)) {
                     if (var12 != 64 || var12 != 64) {
                        throw new IllegalStateException();
                     }

                     byte[] var21 = new byte[var20];
                     byte[] var22 = new byte[var12 * var13];
                     var8.readFully(var21, 0, var20);
                     sub_912(var21, 0, var22, 0, var12 * var13, var18);
                     var_75[var15] = new Class_358(var15, var22);
                     var5.put(new Byte(var15), new Integer(var4 + var17));
                  } else if (sub_8bc(var15)) {
                     Class_318 var46 = new Class_318(var15, var12, var13, 0, 0);
                     byte[] var23 = new byte[var20];
                     var8.readFully(var23, 0, var20);
                     byte[] var24 = null;

                     for(int var25 = 0; var25 < var12; ++var25) {
                        if ((var25 & 1) == 0) {
                           var24 = new byte[var13];
                        }

                        sub_920(var23, var25 * var13, var24, 0, var13, var18, var25 & 1);
                        var46.sub_2a(var25, var24);
                     }

                     var_85[var15 + 128] = var46;
                     var5.put(new Byte(var15), new Integer(var4 + var17));
                  } else {
                     sub_7e1(var8, var20);
                  }
               }

               for(var16 = 0; var16 < var10; ++var16) {
                  var40 = sub_73f(var8);
                  if (!var5.contains(new Integer(var4 + var16))) {
                     sub_7e1(var8, 4 * var40);
                  } else {
                     var41 = new int[var40];

                     for(var19 = 0; var19 < var40; ++var19) {
                        var41[var19] = sub_73f(var8);
                     }

                     var42 = Class_318.sub_f8(var41);
                     var_e1.put(Integer.toString(var4 + var16), var42);
                  }
               }

               var4 += var10;
               var8.close();
               ++var6;
            }
         } catch (Exception var28) {
            return false;
         } catch (OutOfMemoryError var29) {
            return false;
         }

         var_37 = 2;
         return true;
      }
   }

   private static short sub_6ea(DataInputStream var0) {
      return (short)((var0.read() << 8) + var0.read());
   }

   private static int sub_73f(DataInputStream var0) {
      return (var0.read() << 24) + (var0.read() << 16) + (var0.read() << 8) + var0.read();
   }

   private static short sub_775(DataInputStream var0) {
      return (short)(var0.read() + (var0.read() << 8));
   }

   private static int sub_7c5(DataInputStream var0) {
      return var0.read() + (var0.read() << 8) + (var0.read() << 16) + (var0.read() << 24);
   }

   private static void sub_7e1(DataInputStream var0, int var1) {
      while(var1 > 0) {
         int var2 = var1 > 4096 ? 4096 : var1;
         int var3 = (int)var0.skip((long)var2);
         var1 -= var3;
      }

   }

   public static void sub_80f(byte var0) {
      if (!sub_8bc(var0)) {
         var_85[var0 + 128] = new Class_318(var0, 0, 0, 0, 0);
      }

   }

   private static void sub_855(byte var0) {
      if (var0 != 51) {
         if (!sub_887(var0)) {
            var_75[var0] = new Class_358(var0);
         }

      }
   }

   private static boolean sub_887(int var0) {
      if (var0 >= 0 && var0 < 128) {
         return var_75[var0] != null;
      } else {
         return false;
      }
   }

   private static boolean sub_8bc(int var0) {
      var0 += 128;
      if (var0 >= 0 && var0 < 256) {
         return var_85[var0] != null;
      } else {
         return false;
      }
   }

   public static void sub_912(byte[] var0, int var1, byte[] var2, int var3, int var4, int var5) {
      int var6 = var1 * var5 / 8;
      int var7 = var1 * var5 % 8;
      int var8 = (1 << var5) - 1;
      int var9 = var4 + var3;

      for(int var10 = var3; var10 < var9; ++var10) {
         byte var11 = var0[var6];
         int var12;
         int var10000;
         if ((var12 = 8 - (var5 + var7)) >= 0) {
            var10000 = var11 >> var12;
         } else {
            var12 = -var12;
            var10000 = (byte)(var11 << var12) | (var0[var6 + 1] & 255) >> 8 - var12;
         }

         var11 = (byte)var10000;
         if ((var7 += var5) > 7) {
            var7 -= 8;
            ++var6;
         }

         var2[var10] = (byte)(var11 & var8);
      }

   }

   private static void sub_920(byte[] var0, int var1, byte[] var2, int var3, int var4, int var5, int var6) {
      int var7 = var1 * var5 / 8;
      int var8 = var1 * var5 % 8;
      int var9 = (1 << var5) - 1;
      int var10 = var4 + var3;

      for(int var11 = var3; var11 < var10; ++var11) {
         byte var12 = var0[var7];
         int var13;
         int var10000;
         if ((var13 = 8 - (var5 + var8)) >= 0) {
            var10000 = var12 >> var13;
         } else {
            var13 = -var13;
            var10000 = (byte)(var12 << var13) | (var0[var7 + 1] & 255) >> 8 - var13;
         }

         var12 = (byte)var10000;
         if ((var8 += var5) > 7) {
            var8 -= 8;
            ++var7;
         }

         byte[] var14;
         int var10001;
         int var10002;
         if (var6 == 0) {
            var14 = var2;
            var10001 = var11;
            var10002 = (var12 & var9) << 4;
         } else {
            var14 = var2;
            var10001 = var11;
            var10002 = var2[var11] | (byte)(var12 & var9);
         }

         var14[var10001] = (byte)var10002;
      }

   }

   public static void sub_95f() {
      var_c07 = 100;
      var_c18 = 0;

      for(int var0 = 0; var0 < var_c8b.length; ++var0) {
         var_c8b[var0] = false;
         var_ced[var0] = 0;
      }

      var_c8b[0] = true;
      var_c8b[6] = true;
      var_d38 = 0;
      var_d46 = 0;
      var_ded = "";
      var_e3a = 0;
      var_e72 = 0;
      var_e96 = null;
      var_eba = false;
      var_ef9 = 0;
      var_f5f = 0;
      var_f88 = 0;
      var_fe0 = true;
      var_1044 = 1;
      Class_3aa.var_e8b = 0;
      var_117 = 0;
   }
}
