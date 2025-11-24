import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public final class Class_3e6 {
   public static int var_14 = 16;
   public static int var_38 = 50;
   public static int var_70 = 40;
   private Point2D var_cd = new Point2D(0, 0);
   private int var_f8;
   public Point2D[] var_138;
   private Point2D[] var_1af;
   public Class_1e1[] var_210;
   public Class_445[] var_254;
   private Vector var_2a8;
   private Vector var_2f6;
   public Class_30a[] var_32c;
   public Class_8e[] var_370;
   public Class_71 var_383;
   public Class_241[] var_3c0;
   public Class_110[] var_401;
   public Class_21c[] var_433;

   public Class_3e6() {
      new Point2D(0, 0);
      this.var_2a8 = new Vector();
      this.var_2f6 = new Vector();
      this.var_f8 = -1;
   }

   public final Class_241 sub_57() {
      return this.var_3c0[this.var_3c0.length - 1];
   }

   public final void sub_9a(Point2D[] var1) {
      this.var_138 = var1;
      this.var_1af = new Point2D[var1.length];

      for(int var2 = 0; var2 < this.var_1af.length; ++var2) {
         this.var_1af[var2] = new Point2D(0, 0);
      }

   }

   public final Point2D[] sub_b8(int var1, int var2, int var3) {
      long var4 = (long) MathUtils.fastSin(var3);
      long var6 = (long) MathUtils.fastCos(var3);

      for(int var8 = 0; var8 < this.var_138.length; ++var8) {
         int var9 = this.var_138[var8].x - var1;
         int var10 = this.var_138[var8].y - var2;
         this.var_1af[var8].x = (int)(var6 * (long)var9 - var4 * (long)var10 >> 16);
         this.var_1af[var8].y = (int)(var4 * (long)var9 + var6 * (long)var10 >> 16);
      }

      return this.var_1af;
   }

   public final Class_110 sub_e0(int var1, int var2) {
      return this.sub_57().sub_153(var1, var2);
   }

   public final Class_30a sub_115(int var1, int var2) {
      return this.sub_57().sub_13a(var1, var2);
   }

   public final void sub_133() {
      int var1;
      for(var1 = 0; var1 < this.var_3c0.length; ++var1) {
         this.var_3c0[var1].sub_5c(this);
      }

      for(var1 = 0; var1 < this.var_210.length; ++var1) {
         this.var_210[var1].sub_ca(this);
      }

      for(var1 = 0; var1 < this.var_370.length; ++var1) {
         this.var_370[var1].sub_55(this);
      }

      for(var1 = 0; var1 < this.var_433.length; ++var1) {
         this.var_433[var1].sub_35(this);
      }

      for(var1 = 0; var1 < this.var_401.length; ++var1) {
         this.var_401[var1].sub_148(this);
      }

      this.sub_178();
      this.sub_57().sub_1a8();
   }

   public final void sub_178() {
      int var1;
      for(var1 = 0; var1 < this.var_401.length; ++var1) {
         this.var_401[var1].sub_fc();
      }

      for(var1 = 0; var1 < this.var_254.length; ++var1) {
         Class_445 var2;
         if ((var2 = this.var_254[var1]) != null) {
            var2.sub_57(this);
         }
      }

      for(var1 = 0; var1 < this.var_2a8.size(); ++var1) {
         ((Class_445)this.var_2a8.elementAt(var1)).sub_57(this);
      }

      for(var1 = 0; var1 < this.var_2f6.size(); ++var1) {
         ((Class_445)this.var_2f6.elementAt(var1)).sub_57(this);
      }

   }

   private static boolean sub_196(Point2D var0, Point2D var1, Point2D var2) {
      if (var1.x == var2.x) {
         if (var0.x <= var1.x) {
            return var1.y - var2.y > 0;
         } else {
            return var1.y - var2.y < 0;
         }
      } else if (var1.y == var2.y) {
         if (var0.y <= var1.y) {
            return var1.x - var2.x < 0;
         } else {
            return var1.x - var2.x > 0;
         }
      } else {
         int var3 = var0.x - var1.x;
         int var4 = var0.y - var1.y;
         long var5 = (long)(var1.y - var2.y) * (long)var3;
         return (long)(var1.x - var2.x) * (long)var4 >= var5;
      }
   }

   public final boolean sub_1dd(Class_445 var1, Class_71 var2, Class_30a var3) {
      this.var_cd.x = var2.var_49;
      this.var_cd.y = var2.var_ba;

      int var4;
      for(var4 = 0; var4 < this.var_210.length; ++var4) {
         Class_1e1 var5;
         if ((var5 = this.var_210[var4]).sub_18c() || sub_24b(var3, var5)) {
            this.sub_2d4(var5);
         }
      }

      var4 = this.var_cd.x - 655360;
      int var15 = this.var_cd.x + 655360;
      int var6 = this.var_cd.y - 655360;
      int var7 = this.var_cd.y + 655360;

      for(int var8 = 0; var8 < this.var_254.length; ++var8) {
         Class_445 var9;
         if ((var9 = this.var_254[var8]) != null && var9 != var1 && var9.var_2cf != -1) {
            Class_71 var10;
            int var11 = (var10 = var9.var_c).var_49 - 655360;
            int var12 = var10.var_49 + 655360;
            int var13 = var10.var_ba - 655360;
            int var14 = var10.var_ba + 655360;
            if (var4 <= var12 && var15 >= var11 && var6 <= var14 && var7 >= var13) {
               switch(var9.var_24) {
               case 10:
               case 12:
               case 3001:
               case 3002:
               case 3003:
               case 3004:
               case 3005:
               case 3006:
                  return false;
               }
            }
         }
      }

      var2.var_49 = this.var_cd.x;
      var2.var_ba = this.var_cd.y;
      return true;
   }

   public final Class_1e1 sub_205(Class_1b6 var1, Class_30a var2) {
      Class_1e1 var5 = null;
      this.var_cd.x = var1.var_49;
      this.var_cd.y = var1.var_ba;
      int var6 = -1;
      Class_1e1 var7;
      if (this.var_f8 != -1 && ((var7 = this.var_210[this.var_f8]).sub_129() || sub_24b(var2, var7)) && this.sub_2d4(var7)) {
         var6 = this.var_f8;
         var5 = var7;
      }

      int var16;
      for(var16 = 0; var16 < this.var_210.length; ++var16) {
         Class_1e1 var8;
         if (var16 != this.var_f8 && ((var8 = this.var_210[var16]).sub_129() || sub_24b(var2, var8)) && this.sub_2d4(var8)) {
            if (var6 == -1) {
               var6 = var16;
            }

            if (var5 == null || var5.sub_5e() == 0) {
               var5 = var8;
            }
         }
      }

      var16 = 1310720;

      Class_445 var9;
      int var12;
      int var13;
      int var14;
      int var17;
      for(var17 = 0; var17 < this.var_254.length; ++var17) {
         if ((var9 = this.var_254[var17]) != null && var9.var_2cf != -1) {
            switch(var9.var_24) {
            case 10:
            case 12:
            case 3001:
            case 3002:
            case 3003:
            case 3004:
            case 3005:
            case 3006:
               Class_71 var10 = var9.var_c;
               int var11 = this.var_cd.x - var10.var_49;
               var12 = this.var_cd.y - var10.var_ba;
               var13 = var11 < 0 ? -var11 : var11;
               var14 = var12 < 0 ? -var12 : var12;
               if (var13 < var16 && var14 < var16) {
                  if (var9.var_24 == 10 && Class_3aa.var_259 == 4) {
                     Class_29e.var_ded = Class_29e.var_ced[6] > 0 ? "find the wall i told you|and blow it up!" : "go, get the dynamite!";
                     Class_29e.var_e3a = 30;
                  }

                  Point2D var10000;
                  if (var13 > var14) {
                     if (var11 > 0) {
                        var10000 = this.var_cd;
                        var10000.x += var16 - var13;
                     } else {
                        var10000 = this.var_cd;
                        var10000.x -= var16 - var13;
                     }
                  } else {
                     int var10001;
                     if (var12 > 0) {
                        var10000 = this.var_cd;
                        var10001 = var10000.y + (var16 - var14);
                     } else {
                        var10000 = this.var_cd;
                        var10001 = var10000.y - (var16 - var14);
                     }

                     var10000.y = var10001;
                  }
               }
            }
         }
      }

      var1.var_49 = this.var_cd.x;
      var1.var_ba = this.var_cd.y;
      this.var_f8 = var6;
      var16 = 1966080;

      int var15;
      int var18;
      Class_71 var19;
      int[] var20;
      for(var17 = 0; var17 < this.var_2f6.size(); ++var17) {
         var18 = (var9 = (Class_445)this.var_2f6.elementAt(var17)).var_24;
         var19 = var9.var_c;
         var12 = var1.var_49 - var19.var_49;
         var13 = var1.var_ba - var19.var_ba;
         var14 = var12 < 0 ? -var12 : var12;
         var15 = var13 < 0 ? -var13 : var13;
         if (var14 < var16 && var15 < var16) {
            switch(var18) {
            case 2004:
               Class_29e.var_c8b[5] = true;
               var20 = Class_29e.var_ced;
               var20[5] += Class_3aa.var_1616[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               Class_29e.var_d46 = 5;
               Class_29e.var_fe0 = true;
               Class_29e.var_1044 = 8;
               break;
            case 2006:
               Class_29e.var_c8b[7] = true;
               var20 = Class_29e.var_ced;
               var20[7] += Class_3aa.var_1677[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               Class_29e.var_d46 = 7;
               Class_29e.var_fe0 = true;
               Class_29e.var_1044 = 8;
               Class_3aa.var_295 = Class_3aa.var_259++;
               Class_29e.var_117 = 0;
               Class_29e.var_480 = 1;
               break;
            case 2007:
               var20 = Class_29e.var_ced;
               var20[1] += Class_3aa.var_14be[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               break;
            case 2008:
               var20 = Class_29e.var_ced;
               var20[2] += Class_3aa.var_14f5[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               break;
            case 2010:
               var20 = Class_29e.var_ced;
               var20[5] += Class_3aa.var_151d[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               break;
            case 2047:
               var20 = Class_29e.var_ced;
               var20[7] += Class_3aa.var_156b[Class_29e.var_c79];
               this.var_2f6.removeElementAt(var17--);
               break;
            default:
               continue;
            }

            Class_3aa.sub_84e(1, false, 80, 0);
         }
      }

      for(var17 = 0; var17 < this.var_254.length; ++var17) {
         if ((var9 = this.var_254[var17]) != null) {
            var18 = var9.var_24;
            var19 = var9.var_c;
            var12 = var1.var_49 - var19.var_49;
            var13 = var1.var_ba - var19.var_ba;
            var14 = var12 < 0 ? -var12 : var12;
            var15 = var13 < 0 ? -var13 : var13;
            if (var14 < var16 && var15 < var16) {
               label173: {
                  switch(var18) {
                  case 5:
                     Class_29e.var_d98[0] = true;
                     this.var_254[var17] = null;
                     break label173;
                  case 13:
                     Class_29e.var_d98[1] = true;
                     this.var_254[var17] = null;
                     break label173;
                  case 82:
                     Class_29e.var_c8b[8] = true;
                     Class_29e.var_ded = "go now to the agent anna";
                     Class_29e.var_e3a = 30;
                     this.var_254[var17] = null;
                     break label173;
                  case 2001:
                     Class_29e.var_c8b[1] = true;
                     var20 = Class_29e.var_ced;
                     var20[1] += Class_3aa.var_157a[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 1;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2002:
                     if (Class_3aa.var_259 == 3) {
                        Class_29e.var_ded = "to change weapon press 3";
                        Class_29e.var_e3a = 30;
                     }

                     Class_29e.var_c8b[2] = true;
                     var20 = Class_29e.var_ced;
                     var20[2] += Class_3aa.var_1592[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 2;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2003:
                     Class_29e.var_c8b[3] = true;
                     var20 = Class_29e.var_ced;
                     var20[1] += Class_3aa.var_15c4[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 3;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2004:
                     Class_29e.var_c8b[5] = true;
                     var20 = Class_29e.var_ced;
                     var20[5] += Class_3aa.var_1616[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 5;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2005:
                     Class_29e.var_c8b[6] = true;
                     var20 = Class_29e.var_ced;
                     var20[6] += Class_3aa.var_1630[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 6;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2006:
                     Class_29e.var_c8b[7] = true;
                     var20 = Class_29e.var_ced;
                     var20[7] += Class_3aa.var_1677[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 7;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break;
                  case 2007:
                     var20 = Class_29e.var_ced;
                     var20[1] += Class_3aa.var_14be[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     break label173;
                  case 2008:
                     var20 = Class_29e.var_ced;
                     var20[2] += Class_3aa.var_14f5[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     break label173;
                  case 2010:
                     var20 = Class_29e.var_ced;
                     var20[5] += Class_3aa.var_151d[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     break label173;
                  case 2012:
                     if (Class_29e.var_c07 >= 100) {
                        continue;
                     }

                     Class_29e.var_c07 += Class_3aa.var_16e8[Class_29e.var_c79];
                     if (Class_29e.var_c07 > 100) {
                        Class_29e.var_c07 = 100;
                     }

                     this.var_254[var17] = null;
                     break label173;
                  case 2013:
                     this.var_254[var17] = null;
                     break;
                  case 2014:
                     if (Class_29e.var_c07 >= 100) {
                        continue;
                     }

                     Class_29e.var_c07 += Class_3aa.var_16c7[Class_29e.var_c79];
                     if (Class_29e.var_c07 > 100) {
                        Class_29e.var_c07 = 100;
                     }

                     this.var_254[var17] = null;
                     break label173;
                  case 2015:
                     if (Class_29e.var_c18 >= 100) {
                        continue;
                     }

                     Class_29e.var_c18 += Class_3aa.var_1731[Class_29e.var_c79];
                     if (Class_29e.var_c18 > 100) {
                        Class_29e.var_c18 = 100;
                     }

                     this.var_254[var17] = null;
                     break label173;
                  case 2024:
                     Class_29e.var_c8b[4] = true;
                     var20 = Class_29e.var_ced;
                     var20[1] += Class_3aa.var_15d0[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     Class_29e.var_d46 = 4;
                     Class_29e.var_fe0 = true;
                     Class_29e.var_1044 = 8;
                     break label173;
                  case 2047:
                     var20 = Class_29e.var_ced;
                     var20[7] += Class_3aa.var_156b[Class_29e.var_c79];
                     this.var_254[var17] = null;
                     break label173;
                  default:
                     continue;
                  }

                  Class_3aa.var_295 = Class_3aa.var_259++;
                  Class_29e.var_117 = 0;
                  Class_29e.var_480 = 1;
               }

               Class_3aa.sub_84e(1, false, 80, 0);
            }
         }
      }

      return var5;
   }

   private static boolean sub_24b(Class_30a var0, Class_1e1 var1) {
      Class_30a var2 = var1.var_133.var_13d;
      Class_30a var3 = var1.var_e5.var_13d;
      short var4;
      if (var2 != var0) {
         if (var2.var_82 - var0.var_82 > var_14) {
            return true;
         }

         var4 = var2.var_82;
         if (var0.var_82 > var4) {
            var4 = var0.var_82;
         }

         if (var2.var_ac - var4 < var_38) {
            return true;
         }
      }

      if (var3 != var0) {
         if (var3.var_82 - var0.var_82 > var_14) {
            return true;
         }

         var4 = var3.var_82;
         if (var0.var_82 > var4) {
            var4 = var0.var_82;
         }

         if (var3.var_ac - var4 < var_38) {
            return true;
         }
      }

      return false;
   }

   private static boolean sub_289(Class_1e1 var0) {
      Class_30a var1 = var0.var_133.var_13d;
      Class_30a var2 = var0.var_e5.var_13d;
      if (var1.var_ac - var1.var_82 <= 0) {
         return true;
      } else if (var2.var_ac - var2.var_82 <= 0) {
         return true;
      } else if (var1.var_82 >= var2.var_ac) {
         return true;
      } else {
         return var2.var_82 >= var1.var_ac;
      }
   }

   private static boolean sub_2bb(int var0, Class_1e1 var1) {
      Class_30a var2 = var1.var_133.var_13d;
      Class_30a var3 = var1.var_e5.var_13d;
      return var2.var_ac <= var0 || var2.var_82 >= var0 || var3.var_ac <= var0 || var3.var_82 >= var0;
   }

   private boolean sub_2d4(Class_1e1 var1) {
      Point2D var2 = this.var_138[var1.var_22 & '\uffff'];
      Point2D var3;
      int var4 = (var3 = this.var_138[var1.var_5c & '\uffff']).x - var2.x;
      int var5 = var3.y - var2.y;
      int var6 = var2.x + (var4 >> 1);
      int var7 = var2.y + (var5 >> 1);
      int var8 = var4 >= 0 ? var4 >> 1 : -(var4 >> 1);
      int var9 = var5 >= 0 ? var5 >> 1 : -(var5 >> 1);
      int var10 = (var4 = this.var_cd.x - var6) >= 0 ? var4 : -var4;
      int var11 = var8 + 655360 - var10;
      if (0 < var11) {
         int var12 = (var5 = this.var_cd.y - var7) >= 0 ? var5 : -var5;
         int var13 = var9 + 655360 - var12;
         if (0 < var13) {
            if (var11 < var13) {
               if (var4 < 0) {
                  var11 = -var11;
                  var13 = 0;
               } else {
                  var13 = 0;
               }
            } else if (var5 < 0) {
               var11 = 0;
               var13 = -var13;
            } else {
               var11 = 0;
            }

            return this.sub_30b(var11, var13, var2, var3, var1.var_88, var6, var7, var8, var9);
         }
      }

      return false;
   }

   private boolean sub_30b(int var1, int var2, Point2D var3, Point2D var4, Point2D var5, int var6, int var7, int var8, int var9) {
      int var10;
      int var10000;
      if (sub_196(this.var_cd, var3, var4)) {
         var10 = -var5.x;
         var10000 = -var5.y;
      } else {
         var10 = var5.x;
         var10000 = var5.y;
      }

      int var11;
      int var10001;
      label68: {
         var11 = var10000;
         if (var8 > var9) {
            if (var11 >= 0) {
               var10000 = var7 + var9 - (this.var_cd.y - 655360);
               break label68;
            }

            var10000 = var7 - var9;
            var10001 = this.var_cd.y;
         } else {
            if (var10 >= 0) {
               var10000 = var6 + var8 - (this.var_cd.x - 655360);
               break label68;
            }

            var10000 = var6 - var8;
            var10001 = this.var_cd.x;
         }

         var10000 = -(var10000 - (var10001 + 655360));
      }

      int var13 = var10000;
      if (0 < var13) {
         if (var10 >= 0) {
            var10000 = this.var_cd.x - 655360;
            var10001 = var6 + var8;
         } else {
            var10000 = this.var_cd.x + 655360;
            var10001 = var6 - var8;
         }

         int var14 = var10000 - var10001;
         if (var11 >= 0) {
            var10000 = this.var_cd.y - 655360;
            var10001 = var7 - var9;
         } else {
            var10000 = this.var_cd.y + 655360;
            var10001 = var7 + var9;
         }

         int var15 = var10000 - var10001;
         int var18;
         if ((var18 = (int)((long)var14 * (long)var10 + (long)var15 * (long)var11 >> 16)) < 0) {
            int var16 = (int)((long)var10 * (long)(-var18) >> 16);
            int var17 = (int)((long)var11 * (long)(-var18) >> 16);
            int var19 = (var16 >= 0 ? var16 : -var16) + (var17 >= 0 ? var17 : -var17);
            Point2D var21;
            if ((var1 >= 0 ? var1 : -var1) + (var2 >= 0 ? var2 : -var2) < var19) {
               var21 = this.var_cd;
               var21.x += var1;
               var21 = this.var_cd;
               var21.y += var2;
               return true;
            }

            var21 = this.var_cd;
            var21.x += var16;
            var21 = this.var_cd;
            var21.y += var17;
            return true;
         }
      }

      return false;
   }

   public static boolean sub_365(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      long var8;
      if ((var8 = (long)(var7 - var5) * (long)(var2 - var0) - (long)(var6 - var4) * (long)(var3 - var1)) == 0L) {
         return false;
      } else {
         long var10 = (long)(var6 - var4) * (long)(var1 - var5) - (long)(var7 - var5) * (long)(var0 - var4);
         long var12 = (long)(var2 - var0) * (long)(var1 - var5) - (long)(var3 - var1) * (long)(var0 - var4);
         return var8 > 0L && var10 >= 0L && var10 <= var8 && var12 >= 0L && var12 <= var8 || var8 < 0L && var10 <= 0L && var10 >= var8 && var12 <= 0L && var12 >= var8;
      }
   }

   private static boolean sub_37f(int var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      return sub_365(var0, var1, var2, var3, var4 - var6, var5 - var6, var4 + var6, var5 - var6) || sub_365(var0, var1, var2, var3, var4 + var6, var5 - var6, var4 + var6, var5 + var6) || sub_365(var0, var1, var2, var3, var4 + var6, var5 + var6, var4 - var6, var5 + var6) || sub_365(var0, var1, var2, var3, var4 - var6, var5 + var6, var4 - var6, var5 - var6);
   }

   public final boolean sub_3ce(Class_71 var1, Class_71 var2) {
      for(int var3 = 0; var3 < this.var_210.length; ++var3) {
         Class_1e1 var4;
         if ((var4 = this.var_210[var3]).sub_1d1() || sub_289(var4)) {
            Point2D var5 = this.var_138[var4.var_22 & '\uffff'];
            Point2D var6 = this.var_138[var4.var_5c & '\uffff'];
            if (sub_365(var2.var_49, var2.var_ba, var1.var_49, var1.var_ba, var5.x, var5.y, var6.x, var6.y)) {
               return false;
            }
         }
      }

      return true;
   }

   public final void sub_3f2(Class_71 var1, Class_30a var2) {
      int var3 = sub_547(var1.var_49, var1.var_ba, Class_29e.var_553.var_49, Class_29e.var_553.var_ba);
      int var4 = MathUtils.fastSin(102943 - var3);
      int var5 = MathUtils.fastCos(102943 - var3);
      int var7 = var1.var_49 + 20 * var5;
      int var8 = var1.var_ba + 20 * var4;
      Class_71 var9 = new Class_71(var7, var1.var_7a + (var2.var_82 + 40 << 16), var8, var3);
      Class_445 var10;
      (var10 = new Class_445(var9, 0, 101, 0)).sub_ea((byte)0, (byte)-46);
      var10.sub_ea((byte)0, (byte)-47);
      var10.var_1ce = 0;
      this.var_2a8.addElement(var10);
   }

   public final void sub_420(Class_71 var1, Class_30a var2) {
      int var3 = sub_547(var1.var_49, var1.var_ba, Class_29e.var_553.var_49, Class_29e.var_553.var_ba);
      int var4 = MathUtils.fastSin(102943 - var3);
      int var5 = MathUtils.fastCos(102943 - var3);
      int var7 = var1.var_49 + 20 * var5;
      int var8 = var1.var_ba + 20 * var4;
      var4 = MathUtils.fastSin(var3);
      var5 = MathUtils.fastCos(var3);
      int var9 = 10 * var5;
      int var10 = -10 * var4;
      Class_71 var11 = new Class_71(var7 + var9, var1.var_7a + (var2.var_82 + 40 << 16), var8 + var10, var3);
      Class_445 var12;
      (var12 = new Class_445(var11, 0, 102, 0)).sub_ea((byte)0, (byte)-71);
      var12.var_1ce = 0;
      this.var_2a8.addElement(var12);
      var11 = new Class_71(var7 - var9, var1.var_7a + (var2.var_82 + 40 << 16), var8 - var10, var3);
      (var12 = new Class_445(var11, 0, 102, 0)).sub_ea((byte)0, (byte)-71);
      var12.var_1ce = 0;
      this.var_2a8.addElement(var12);
   }

   private boolean sub_47e(int var1, int var2, int var3, int var4, int var5) {
      int var6 = var5 >> 16;

      for(int var7 = 0; var7 < this.var_210.length; ++var7) {
         Class_1e1 var8;
         if ((var8 = this.var_210[var7]).sub_1d1() || sub_2bb(var6, var8)) {
            Point2D var9 = this.var_138[var8.var_22 & '\uffff'];
            Point2D var10 = this.var_138[var8.var_5c & '\uffff'];
            if (sub_365(var1, var2, var3, var4, var9.x, var9.y, var10.x, var10.y)) {
               return true;
            }
         }
      }

      return false;
   }

   public final void sub_4ab() {
      int var1 = Class_29e.var_553.var_d7;
      int var2 = 67108864;
      int var3 = MathUtils.fastSin(102943 - var1);
      int var4 = MathUtils.fastCos(102943 - var1);
      int var5 = Class_29e.var_d38 != 0 && Class_29e.var_d38 != 5 && Class_29e.var_d38 != 7 ? var2 : 1310720;
      int var6 = Class_29e.var_553.var_49 + MathUtils.fixedPointMultiply(var5, var4);
      int var7 = Class_29e.var_553.var_ba + MathUtils.fixedPointMultiply(var5, var3);
      if (Class_29e.var_d38 == 5) {
         Class_3aa.sub_84e(4, false, 100, 2);
         Class_71 var15 = new Class_71(var6, Class_29e.var_f27 - 655360, var7, var1);
         if (!this.sub_47e(Class_29e.var_553.var_49, Class_29e.var_553.var_ba, var15.var_49, var15.var_ba, var15.var_7a)) {
            Class_445 var16;
            (var16 = new Class_445(var15, 0, 100, 0)).sub_ea((byte)0, (byte)-44);
            var16.sub_ea((byte)0, (byte)-45);
            var16.var_1ce = 0;
            this.var_2a8.addElement(var16);
         }

      } else {
         int var9;
         if (Class_29e.var_d38 == 7) {
            Class_3aa.sub_84e(5, false, 100, 2);
            var3 = MathUtils.fastSin(var1);
            var4 = MathUtils.fastCos(var1);
            int var14 = 10 * var4;
            var9 = -10 * var3;
            Class_71 var17 = new Class_71(var6 - var14, Class_29e.var_f27 - 655360, var7 - var9, var1);
            Class_445 var18;
            if (!this.sub_47e(Class_29e.var_553.var_49, Class_29e.var_553.var_ba, var17.var_49, var17.var_ba, var17.var_7a)) {
               (var18 = new Class_445(var17, 0, 102, 0)).sub_ea((byte)0, (byte)-71);
               var18.var_1ce = 0;
               this.var_2a8.addElement(var18);
            }

            var17 = new Class_71(var6 + var14, Class_29e.var_f27 - 655360, var7 + var9, var1);
            if (!this.sub_47e(Class_29e.var_553.var_49, Class_29e.var_553.var_ba, var17.var_49, var17.var_ba, var17.var_7a)) {
               (var18 = new Class_445(var17, 0, 102, 0)).sub_ea((byte)0, (byte)-71);
               var18.var_1ce = 0;
               this.var_2a8.addElement(var18);
            }

         } else {
            boolean var8 = false;

            for(var9 = 0; var9 < this.var_254.length; ++var9) {
               Class_445 var10;
               if ((var10 = this.var_254[var9]) != null && var10.var_2cf != -1) {
                  Class_71 var11 = var10.var_c;
                  if (this.sub_3ce(Class_29e.var_553, var11)) {
                     int var12 = 327680;
                     if (sub_37f(Class_29e.var_553.var_49, Class_29e.var_553.var_ba, var6, var7, var11.var_49, var11.var_ba, var12)) {
                        int var13;
                        label85: {
                           byte var19;
                           label84: {
                              var13 = 0;
                              int[] var10000;
                              switch(Class_29e.var_d38) {
                              case 0:
                                 var13 = Class_3aa.var_f4a[Class_29e.var_c79];
                                 break label85;
                              case 1:
                                 var13 = Class_3aa.var_f5c[Class_29e.var_c79];
                                 var19 = 7;
                                 break label84;
                              case 2:
                                 var13 = Class_3aa.var_f76[Class_29e.var_c79];
                                 var19 = 7;
                                 break label84;
                              case 3:
                                 var10000 = Class_3aa.var_fa7;
                                 break;
                              case 4:
                                 var10000 = Class_3aa.var_ff1;
                                 break;
                              default:
                                 break label85;
                              }

                              var13 = var10000[Class_29e.var_c79];
                              var19 = 9;
                           }

                           Class_3aa.sub_84e(var19, false, 100, 1);
                           var8 = true;
                        }

                        sub_4c6(var10, var13);
                        break;
                     }
                  }
               }
            }

            if (!var8) {
               if (Class_29e.var_d38 == 1 || Class_29e.var_d38 == 2) {
                  Class_3aa.sub_84e((Class_29e.var_ea3.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
               }

               if (Class_29e.var_d38 == 3 || Class_29e.var_d38 == 4) {
                  Class_3aa.sub_84e((Class_29e.var_ea3.nextInt() & 1) == 0 ? 3 : 8, false, 100, 1);
               }
            }

         }
      }
   }

   private static void sub_4c6(Class_445 var0, int var1) {
      var0.var_243 -= var1;
      Class_445 var10000;
      byte var10001;
      if (var0.var_243 <= 0) {
         var0.var_243 = 0;
         var0.var_2cf = 6;
         switch(var0.var_24) {
         case 3001:
         case 3003:
         case 3004:
         case 3005:
         case 3006:
            var0.var_296 = 5;
            var10000 = var0;
            var10001 = 5;
            break;
         case 3002:
            var0.var_296 = 5;
            var10000 = var0;
            var10001 = 4;
            break;
         default:
            return;
         }

         var10000.var_1ce = var10001;
      } else {
         var0.var_2cf = 5;
         switch(var0.var_24) {
         case 3001:
         case 3003:
         case 3004:
         case 3005:
         case 3006:
            var0.var_296 = 5;
            var10000 = var0;
            var10001 = 4;
            break;
         case 3002:
            var0.var_296 = 5;
            var10000 = var0;
            var10001 = 3;
            break;
         default:
            return;
         }

         var10000.var_1ce = var10001;
      }
   }

   public final void sub_4e6() {
      for(int var1 = 0; var1 < this.var_2a8.size(); ++var1) {
         Class_445 var2;
         if ((var2 = (Class_445)this.var_2a8.elementAt(var1)).var_24 == 100 || var2.var_24 == 101) {
            var2.var_1ce ^= 1;
         }
      }

   }

   public final boolean sub_50b() {
      for(int var1 = 0; var1 < this.var_2a8.size(); ++var1) {
         Class_445 var2;
         int var4;
         int var5;
         int var6;
         int var7;
         Class_71 var15;
         Class_3e6 var10000;
         if ((var2 = (Class_445)this.var_2a8.elementAt(var1)).var_24 == 103) {
            if (var2.var_f7 <= 0) {
               continue;
            }

            --var2.var_f7;
            if (var2.var_f7 != 0) {
               continue;
            }

            Class_3aa.sub_84e(4, false, 100, 2);
            int var3;
            if (this.sub_3ce(var2.var_c, Class_29e.var_553)) {
               var3 = var2.var_c.var_49 - Class_29e.var_553.var_49;
               var4 = var2.var_c.var_ba - Class_29e.var_553.var_ba;
               if ((var5 = Class_3aa.var_1113[Class_29e.var_c79] - (MathUtils.fixedPointMultiply(MathUtils.fastHypot(var3, var4), Class_3aa.var_10c4[Class_29e.var_c79]) >> 16)) > 0) {
                  Class_3aa.sub_882(var5 * 10);
                  if (Class_29e.sub_59b(var5)) {
                     return true;
                  }
               }
            }

            for(var3 = 0; var3 < this.var_254.length; ++var3) {
               Class_445 var16;
               if ((var16 = this.var_254[var3]) != null && var16.var_2cf != -1) {
                  Object var17 = null;
                  if (this.sub_3ce(var2.var_c, var16.var_c)) {
                     var6 = var2.var_c.var_49 - var16.var_c.var_49;
                     var7 = var2.var_c.var_ba - var16.var_c.var_ba;
                     int var8;
                     if ((var8 = Class_3aa.var_1113[Class_29e.var_c79] - (MathUtils.fixedPointMultiply(MathUtils.fastHypot(var6, var7), Class_3aa.var_10c4[Class_29e.var_c79]) >> 16)) > 0) {
                        sub_4c6(var16, var8);
                     }
                  }
               }
            }

            Class_29e.var_ef9 = 16;
            if (Class_3aa.var_259 == 4) {
               var15 = var2.var_c;
               if (this.sub_115(var15.var_49, var15.var_ba).sub_5c() == 666) {
                  Class_3aa.var_295 = Class_3aa.var_259++;
                  Class_29e.var_117 = 0;
                  Class_29e.var_480 = 1;
               }
            }

            var10000 = this;
         } else {
            var4 = (var15 = var2.var_c).var_49;
            var5 = var15.var_ba;
            var15.sub_ca(0, -1048576);
            var6 = var15.var_49;
            var7 = var15.var_ba;
            boolean var18 = false;
            int var9 = (var2.var_24 == 102 ? Class_3aa.var_104c : Class_3aa.var_1071)[Class_29e.var_c79];
            if (sub_37f(var4, var5, var6, var7, Class_29e.var_553.var_49, Class_29e.var_553.var_ba, 655360)) {
               if (var2.var_24 == 101) {
                  Class_3aa.sub_84e(4, false, 100, 2);
               }

               Class_3aa.sub_882(var9 * 10);
               if (Class_29e.sub_59b(var9)) {
                  return true;
               }

               var18 = true;
            }

            int var10;
            for(var10 = 0; var10 < this.var_254.length; ++var10) {
               Class_445 var11;
               if ((var11 = this.var_254[var10]) != null && var11.var_2cf != -1) {
                  Class_71 var12 = var11.var_c;
                  int var13 = var2.var_24 == 102 ? 655360 : 327680;
                  if (sub_37f(var4, var5, var6, var7, var12.var_49, var12.var_ba, var13)) {
                     sub_4c6(var11, var9);
                     var18 = true;
                  }
               }
            }

            if (!var18) {
               var10 = var15.var_7a >> 16;

               for(int var19 = 0; var19 < this.var_210.length; ++var19) {
                  Class_1e1 var20;
                  if ((var20 = this.var_210[var19]).sub_1d1() || sub_2bb(var10, var20)) {
                     Point2D var21 = this.var_138[var20.var_22 & '\uffff'];
                     Point2D var14 = this.var_138[var20.var_5c & '\uffff'];
                     if (sub_365(var4, var5, var6, var7, var21.x, var21.y, var14.x, var14.y)) {
                        var18 = true;
                        break;
                     }
                  }
               }
            }

            if (!var18) {
               continue;
            }

            var10000 = this;
         }

         var10000.var_2a8.removeElementAt(var1--);
      }

      return false;
   }

   private static int sub_547(int var0, int var1, int var2, int var3) {
      int var4 = var2 - var0;
      int var5 = var3 - var1;
      int var6 = MathUtils.fastHypot(var4, var5);
      var4 = MathUtils.preciseDivide(var4, var6);
      long var7 = (long)((var5 = MathUtils.preciseDivide(var5, var6)) < 0 ? -var5 : var5);
      long var9;
      int var11 = (var9 = (long)(var4 < 0 ? -var4 : var4)) < 6L ? 0 : MathUtils.fastAtan((int)((var7 << 32) / var9 >> 16));
      if (var4 < 0) {
         var11 = 205887 - var11;
      }

      if (var5 > 0) {
         var11 = 411775 - var11;
      }

      if ((var11 += 102943) >= 411775) {
         var11 -= 411775;
      }

      return var11;
   }

   public final boolean sub_57a() {
      int var1 = Class_29e.var_553.var_d7;
      int var3 = MathUtils.fastSin(102943 - var1);
      int var4 = MathUtils.fastCos(102943 - var1);
      int var5 = 655360;
      int var6 = Class_29e.var_553.var_49 + MathUtils.fixedPointMultiply(var5, var4);
      int var7 = Class_29e.var_553.var_ba + MathUtils.fixedPointMultiply(var5, var3);
      Class_71 var8 = new Class_71(var6, 0, var7, var1);
      Class_445 var9;
      (var9 = new Class_445(var8, 0, 103, 100)).sub_ea((byte)0, (byte)-51);
      var9.var_1ce = 0;
      this.var_2a8.addElement(var9);
      return true;
   }

   public final void sub_5cd(Class_445 var1) {
      Class_445 var2;
      label15: {
         var2 = null;
         Class_445 var10000;
         byte var10001;
         byte var10002;
         switch(var1.var_24) {
         case 3001:
            var10000 = var2 = new Class_445(var1.var_c, 0, 2004, 0);
            var10001 = 0;
            var10002 = -43;
            break;
         case 3002:
            var10000 = var2 = new Class_445(var1.var_c, 0, 2006, 0);
            var10001 = 0;
            var10002 = -72;
            break;
         case 3003:
         case 3005:
         case 3006:
            var10000 = var2 = new Class_445(var1.var_c, 0, 2007, 0);
            var10001 = 0;
            var10002 = -48;
            break;
         case 3004:
            var10000 = var2 = new Class_445(var1.var_c, 0, 2008, 0);
            var10001 = 0;
            var10002 = -54;
            break;
         default:
            break label15;
         }

         var10000.sub_ea(var10001, var10002);
      }

      this.var_2f6.addElement(var2);
   }

   public final void sub_5ec(Graphics var1) {
      for(int var2 = 0; var2 < this.var_210.length; ++var2) {
         Class_1e1 var3;
         if (((var3 = this.var_210[var2]).sub_5e() != 0 || !var3.sub_1f8()) && var3.sub_29e()) {
            int var4 = var3.var_22 & '\uffff';
            int var5 = var3.var_5c & '\uffff';
            Point2D var6 = this.var_1af[var4];
            Point2D var7 = this.var_1af[var5];
            Graphics var10000;
            int var10001;
            if (var3.sub_5e() != 0) {
               var10000 = var1;
               var10001 = 16776960;
            } else {
               var10000 = var1;
               var10001 = 16711680;
            }

            var10000.setColor(var10001);
            int var8 = (var6.x >> 18) + 120;
            int var9 = -(var6.y >> 18) + 144;
            int var10 = (var7.x >> 18) + 120;
            int var11 = -(var7.y >> 18) + 144;
            var1.drawLine(var8, var9, var10, var11);
         }
      }

      var1.setColor(65280);
      var1.drawLine(120, 139, 116, 149);
      var1.drawLine(120, 139, 124, 149);
      var1.drawLine(116, 149, 124, 149);
   }
}
