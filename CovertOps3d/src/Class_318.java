public final class Class_318 {
   public short var_11;
   public short var_6f;
   public short var_cf;
   public short var_121;
   public byte[][] var_157;
   public int[][] var_191;
   public byte var_1d6;
   private int var_229;

   public Class_318(byte var1, int var2, int var3, int var4, int var5) {
      this.var_1d6 = var1;
      this.var_11 = (short)var2;
      this.var_6f = (short)var3;
      this.var_229 = var2 - 1;
      this.var_cf = (short)var4;
      this.var_121 = (short)var5;
      Class_318 var10000;
      byte[][] var10001;
      if (var2 > 0) {
         var10000 = this;
         var10001 = new byte[(var2 >> 1) + (var2 & 1)][];
      } else {
         var10000 = this;
         var10001 = (byte[][])null;
      }

      var10000.var_157 = var10001;
      this.var_191 = (int[][])null;
   }

   public Class_318(byte var1, int var2, int var3, int var4, int var5, int[] var6) {
      this(var1, var2, var3, var4, var5);
      this.var_191 = sub_f8(var6);
   }

   public final void sub_2a(int var1, byte[] var2) {
      this.var_157[var1 >> 1] = var2;
   }

   public final byte[] sub_47(int var1) {
      if (var1 >= this.var_11) {
         var1 %= this.var_11;
      }

      while(var1 < 0) {
         var1 = (var1 + this.var_11 * 1000) % this.var_11;
      }

      return this.var_157[var1 >> 1];
   }

   public final byte[] sub_7b(int var1) {
      return this.var_157[(var1 & this.var_229) >> 1];
   }

   public final void sub_9d(byte[][] var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8) {
      int var9;
      int var10;
      if (var8) {
         this.var_11 = (short)var4;
         this.var_6f = (short)var5;
         this.var_229 = this.var_11 - 1;
         var9 = (this.var_11 >> 1) + (this.var_11 & 1);
         this.var_157 = new byte[var9][];

         for(var10 = 0; var10 < var9; ++var10) {
            this.var_157[var10] = new byte[this.var_6f];
            System.arraycopy(var1[var10], 0, this.var_157[var10], 0, this.var_6f);
         }

      } else {
         for(var9 = 0; var9 < var4; ++var9) {
            var10 = var9 + var2 >> 1;
            int var11 = var9 + var2 & 1;
            int var12 = var9 + var6 >> 1;
            int var13 = var9 + var6 & 1;
            int var14;
            if (var11 == 0) {
               if (var13 == 0) {
                  for(var14 = 0; var14 < var5; ++var14) {
                     this.var_157[var12][var7 + var14] = (byte)(this.var_157[var12][var7 + var14] & 15 | var1[var10][var3 + var14] & 240);
                  }
               } else {
                  for(var14 = 0; var14 < var5; ++var14) {
                     this.var_157[var12][var7 + var14] = (byte)(this.var_157[var12][var7 + var14] & 240 | var1[var10][var3 + var14] >> 4 & 15);
                  }
               }
            } else if (var13 == 0) {
               for(var14 = 0; var14 < var5; ++var14) {
                  this.var_157[var12][var7 + var14] = (byte)(this.var_157[var12][var7 + var14] & 15 | var1[var10][var3 + var14] << 4 & 240);
               }
            } else {
               for(var14 = 0; var14 < var5; ++var14) {
                  this.var_157[var12][var7 + var14] = (byte)(this.var_157[var12][var7 + var14] & 240 | var1[var10][var3 + var14] & 15);
               }
            }
         }

      }
   }

   public static int[][] sub_f8(int[] var0) {
      return sub_14b(var0, var0.length);
   }

   private static int[][] sub_14b(int[] var0, int var1) {
      int[] var2 = new int[]{-128, -112, -96, -80, -64, -48, -32, -16, 0, 16, 32, 48, 64, 80, 96, 112};
      int[][] var3 = new int[16][var1];
      int var4 = 2130706432;

      for(int var5 = 0; var5 < 16; ++var5) {
         for(int var6 = 0; var6 < var1; ++var6) {
            short var10000;
            int var7;
            int var8;
            int var9;
            label34: {
               var7 = ((var0[var6] & 16711680) >> 16) + var2[var5];
               var8 = ((var0[var6] & '\uff00') >> 8) + var2[var5];
               var9 = (var0[var6] & 255) + var2[var5];
               if (var7 < 0) {
                  var10000 = 0;
               } else {
                  if (var7 <= 255) {
                     break label34;
                  }

                  var10000 = 255;
               }

               var7 = var10000;
            }

            label40: {
               if (var8 < 0) {
                  var10000 = 0;
               } else {
                  if (var8 <= 255) {
                     break label40;
                  }

                  var10000 = 255;
               }

               var8 = var10000;
            }

            label46: {
               if (var9 < 0) {
                  var10000 = 0;
               } else {
                  if (var9 <= 255) {
                     break label46;
                  }

                  var10000 = 255;
               }

               var9 = var10000;
            }

            var3[var5][var6] = var4 + (var7 << 16) + (var8 << 8) + var9;
         }
      }

      return var3;
   }
}
