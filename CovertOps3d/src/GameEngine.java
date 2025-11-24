import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public final class GameEngine {
   private static int resourceLoadState;
   private static Sprite[] spriteTable;
   private static Texture[] textureTable;
   private static Hashtable paletteCache;
   public static int levelVariant = 0;
   public static boolean inputForward;
   public static boolean inputBackward;
   public static boolean inputLeft;
   public static boolean inputRight;
   public static boolean inputLookUp;
   public static boolean inputLookDown;
   public static boolean inputFire;
   public static boolean inputStrafe;
   public static boolean inputRun;
   public static boolean var_364;
   public static boolean var_3b8;
   public static boolean var_3e3;
   public static boolean var_446;
   public static int var_480;
   public static int var_4c8 = 0;
   public static GameWorld gameWorld = null;
   public static PhysicsBody player;
   public static Transform3D tempTransform;
   public static SectorData currentSector;
   private static Point2D var_5f8 = new Point2D(0, 0);
   private static Point2D var_654 = new Point2D(0, 0);
   private static int var_6a4;
   private static int var_6f4;
   private static int var_723;
   private static int var_771;
   private static int var_7ba;
   private static int var_817;
   public static int[] screenBuffer;
   private static Texture var_894;
   private static Texture var_8f4;
   private static short[] depthBuffer;
   private static int var_958;
   private static int var_9b4;
   private static int var_9e4;
   private static int var_9fa;
   private static int var_a27;
   private static int var_a34;
   private static RenderUtils var_a80;
   private static int[] var_ab4;
   private static int[] var_acc;
   public static Vector floorClipHistory;
   public static Vector ceilingClipHistory;
   public static Vector var_b86;
   public static Vector var_bd3;
   public static int playerHealth = 100;
   public static int playerArmor = 0;
   public static int difficultyLevel = 1;
   public static boolean[] weaponsAvailable = new boolean[]{true, false, false, false, false, false, true, false, false};
   public static int[] ammoCounts = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
   public static int currentWeapon = 0;
   public static int var_d46 = 0;
   public static boolean[] var_d98 = new boolean[]{false, false};
   public static String messageText = "";
   public static int messageTimer = 0;
   public static int var_e72 = 0;
   public static WallDefinition var_e96 = null;
   public static Random var_ea3 = new Random();
   public static boolean var_eba = false;
   public static byte screenShake = 0;
   public static int cameraHeight;
   private static int var_f32 = MathUtils.fixedPointMultiply(1310720, 92682);
   private static int var_f5f = 0;
   private static int var_f88 = 0;
   public static boolean levelComplete = false;
   public static int var_1044 = 0;
   public static boolean var_10a4 = false;
   public static int var_10bb;
   public static int var_1103;
   public static int var_1142;
   public static int var_1171;
   public static GameObject[] var_118d;
   public static int var_119a;

   public static void initializeEngine() {
      MainGameCanvas.sub_57c();
      sub_5f4();
      player = new PhysicsBody(0, 1572864, 0, 65536);
      tempTransform = new Transform3D(0, 0, 0, 0);
      floorClipHistory = new Vector();
      ceilingClipHistory = new Vector();
      var_b86 = new Vector();
      var_bd3 = new Vector();
      var_118d = new GameObject[64];
      var_119a = 0;
      BSPNode.visibleSectorsCount = 0;
      var_894 = new Texture((byte)0, 8, 8, 0, 0, new int[]{16777215, 16711680});
      byte[] var0 = new byte[]{17, 17, 17, 17, 17, 17, 17, 17};
      byte[] var1 = new byte[]{17, 16, 16, 16, 16, 17, 16, 17};
      byte[] var2 = new byte[]{17, 1, 1, 1, 1, 17, 1, 17};
      var_894.setPixelData(0, var0);
      var_894.setPixelData(2, var1);
      var_894.setPixelData(4, var2);
      var_894.setPixelData(6, var0);
      screenBuffer = new int[69120];
      depthBuffer = new short[288];
      var_a80 = new RenderUtils();
      var_ab4 = new int[240];

      for(int var3 = 0; var3 < 240; ++var3) {
         var_ab4[var3] = MathUtils.fixedPointDivide(var3 - 120 << 16, 7864320) >> 2;
      }

      var_acc = new int[289];
      var_acc[0] = 0;

      for(int var4 = 1; var4 < 289; ++var4) {
         var_acc[var4] = 65536 / var4;
      }

      var_10bb = MathUtils.fixedPointDivide(65536, 17301600);
      var_1103 = MathUtils.fixedPointMultiply(MathUtils.fixedPointDivide(65536, 15794176), 102943);
      var_1142 = MathUtils.fixedPointDivide(65536, 18874368);
      var_1171 = MathUtils.fixedPointDivide(65536, 411775);
      MainGameCanvas.sub_57c();
   }

   public static void resetLevelState() {
      clearInputState();
      gameWorld.initializeWorld();
      player.copyFrom(gameWorld.worldOrigin);
      currentSector = gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);
      var_b86.removeAllElements();
      var_bd3.removeAllElements();
      var_119a = 0;
      BSPNode.visibleSectorsCount = 0;
      messageText = "";
      messageTimer = 0;
      var_e72 = 0;
      var_e96 = null;
   }

   public static void handleWeaponChange(byte var0) {
      sub_3f5(sub_f6(var0));
   }

   private static Sprite sub_cb(byte var0) {
      if (var0 == 51) {
         return null;
      } else {
         Sprite var1;
         return (var1 = spriteTable[var0]) != null && var1.pixelData != null ? var1 : null;
      }
   }

   private static Texture sub_f6(byte var0) {
      if (var0 == 0) {
         return var_894;
      } else {
         Texture var1;
         return (var1 = textureTable[var0 + 128]) != null && var1.width > 0 ? var1 : var_894;
      }
   }

   private static boolean sub_102(Point2D var0, Point2D var1, int var2, int var3, int var4) {
      if (var0.y <= 327680 && var1.y <= 327680) {
         return false;
      } else {
         int var5;
         int var6 = (var5 = var4 << 16) + MathUtils.fastHypot(var0.x - var1.x, var0.y - var1.y);
         var_7ba = var5;
         var_817 = var6;
         var_5f8.x = var0.x;
         var_5f8.y = var0.y;
         var_654.x = var1.x;
         var_654.y = var1.y;
         int var7;
         if (var1.y < 327680) {
            var7 = MathUtils.fixedPointDivide(var0.y - 327680, var0.y - var1.y);
            var_654.y = 327680;
            if (var7 == Integer.MAX_VALUE) {
               var_654.x = var1.x > var0.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               var_817 = var6 > var5 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               var_654.x = var1.x > var0.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               var_817 = var6 > var5 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               var_654.x = (int)((long)(var1.x - var0.x) * (long)var7 >> 16) + var0.x;
               var_817 = (int)((long)(var6 - var5) * (long)var7 >> 16) + var5;
            }
         }

         if (var0.y < 327680) {
            var7 = MathUtils.fixedPointDivide(var1.y - 327680, var1.y - var0.y);
            var_5f8.y = 327680;
            if (var7 == Integer.MAX_VALUE) {
               var_5f8.x = var0.x > var1.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               var_7ba = var5 > var6 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               var_5f8.x = var0.x > var1.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               var_7ba = var5 > var6 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               var_5f8.x = (int)((long)(var0.x - var1.x) * (long)var7 >> 16) + var1.x;
               var_7ba = (int)((long)(var5 - var6) * (long)var7 >> 16) + var6;
            }
         }

         long var11 = 33776997205278720L / (long)var_5f8.y >> 16;
         long var9 = 33776997205278720L / (long)var_654.y >> 16;
         var_5f8.x = (int)((long)var_5f8.x * var11 >> 16);
         if (var_5f8.x > 7864320) {
            return false;
         } else {
            var_654.x = (int)((long)var_654.x * var9 >> 16);
            if (var_654.x < -7864320) {
               return false;
            } else if (var_654.x < var_5f8.x) {
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

   private static void sub_14e(WallSegment var0, WallDefinition var1, WallSurface var2, Point2D[] var3, int var4, int var5, int var6, int var7) {
      SectorData var8 = var2.linkedSector;
      int var9 = -var5 + (-var8.ceilingHeight << 16);
      int var10 = -var5 + (-var8.floorHeight << 16);
      if (sub_102(var3[var0.startVertexIndex & '\uffff'], var3[var0.endVertexIndex & '\uffff'], var9, var10, var0.textureOffset & '\uffff')) {
         Point2D var11 = var_5f8;
         Point2D var12 = var_654;
         int var13 = var11.x + 7864320 >> 16;
         int var14 = var_6a4 + 9437184 >> 16;
         int var15 = var12.x + 7864320 >> 16;
         int var16 = var_723 + 9437184 >> 16;
         int var17 = var_6f4 + 9437184 >> 16;
         int var18 = var_771 + 9437184 >> 16;
         int var19 = var8.ceilingHeight - var8.floorHeight;
         Texture var20 = sub_f6(var2.mainTextureId);
         int var21 = var2.textureOffsetY & '\uffff';
         int var22 = var20.height - var19 + var21;
         if (!var1.isSecret()) {
            var22 = var21;
         }

         var1.markAsRendered();
         int var23 = (var2.textureOffsetX & '\uffff') << 16;
         drawWallColumn(var8, var20, var20, var13, var14, var17, var17, var17, var11.y, var15, var16, var18, var18, var18, var12.y, var_7ba + var23, var_817 - var_7ba, var22, var19, var22, var19, -var4, -var6, var7, var9, var10);
      }

   }

   private static void sub_17a(WallSegment var0, WallDefinition var1, WallSurface var2, WallSurface var3, Point2D[] var4, int var5, int var6, int var7, int var8) {
      SectorData var9 = var2.linkedSector;
      SectorData var10 = var3.linkedSector;
      int var11 = -var6 + (-var9.ceilingHeight << 16);
      int var12 = -var6 + (-var9.floorHeight << 16);
      int var13 = -var6 + (-var10.ceilingHeight << 16);
      int var14 = -var6 + (-var10.floorHeight << 16);
      if (sub_102(var4[var0.startVertexIndex & '\uffff'], var4[var0.endVertexIndex & '\uffff'], var11, var12, var0.textureOffset & '\uffff')) {
         Point2D var15 = var_5f8;
         Point2D var16 = var_654;
         int var17 = var15.x + 7864320 >> 16;
         int var18 = var_6a4 + 9437184 >> 16;
         int var19 = var_6f4 + 9437184 >> 16;
         int var20 = var16.x + 7864320 >> 16;
         int var21 = var_723 + 9437184 >> 16;
         int var22 = var_771 + 9437184 >> 16;
         int var23 = MathUtils.fixedPointDivide(var13, var15.y) * 120 + 9437184 >> 16;
         int var24 = MathUtils.fixedPointDivide(var14, var15.y) * 120 + 9437184 >> 16;
         int var25 = MathUtils.fixedPointDivide(var13, var16.y) * 120 + 9437184 >> 16;
         int var26 = MathUtils.fixedPointDivide(var14, var16.y) * 120 + 9437184 >> 16;
         int var27 = var9.ceilingHeight - var10.ceilingHeight;
         int var28 = var10.floorHeight - var9.floorHeight;
         Texture var29 = sub_f6(var2.upperTextureId);
         Texture var30 = sub_f6(var2.lowerTextureId);
         if (var10.floorTextureId == 51) {
            var29 = var_894;
         }

         if (var10.ceilingTextureId == 51) {
            var30 = var_894;
         }

         int var31 = var2.textureOffsetY & '\uffff';
         int var32 = var29.height - var27 + var31;
         if (var1.isDoor()) {
            var32 = var31;
         }

         int var33 = var9.ceilingHeight - var10.floorHeight + var31;
         if (!var1.isSecret()) {
            var33 = var31;
         }

         var1.markAsRendered();
         int var34 = (var2.textureOffsetX & '\uffff') << 16;
         drawWallColumn(var9, var29, var30, var17, var18, var23, var24, var19, var15.y, var20, var21, var25, var26, var22, var16.y, var_7ba + var34, var_817 - var_7ba, var32, var27, var33, var28, -var5, -var7, var8, var11, var12);
      }

   }

   private static void renderWallSegment(WallSegment var0, Point2D[] var1, int var2, int var3, int var4, int var5) {
      WallDefinition var6;
      WallSurface var7 = (var6 = var0.wallDefinition).frontSurface;
      WallSurface var8;
      if ((var8 = var6.backSurface) != null) {
         if (var0.isFrontFacing) {
            sub_17a(var0, var6, var7, var8, var1, var2, var3, var4, var5);
         } else {
            sub_17a(var0, var6, var8, var7, var1, var2, var3, var4, var5);
         }
      } else if (var0.isFrontFacing) {
         sub_14e(var0, var6, var7, var1, var2, var3, var4, var5);
      } else {
         throw new IllegalStateException();
      }
   }

   private static void renderDynamicObjects(Sector var0, int var1, int var2, int var3, long var4, long var6) {
      SectorData var8 = var0.getSectorData();
      Vector var9 = var0.dynamicObjects;
      var_119a = 0;

      int var10;
      GameObject var11;
      for(var10 = 0; var10 < var9.size(); ++var10) {
         Transform3D var12;
         int var13 = (var12 = (var11 = (GameObject)var9.elementAt(var10)).transform).x - var1;
         int var14 = var12.z - var3;
         var11.screenPos.x = (int)(var6 * (long)var13 - var4 * (long)var14 >> 16);
         var11.screenPos.y = (int)(var4 * (long)var13 + var6 * (long)var14 >> 16);
         if (var11.screenPos.y > 327680) {
            byte var15 = var11.getCurrentSprite1();
            byte var16 = var11.getCurrentSprite2();
            if (var15 != 0 || var16 != 0) {
               GameObject var10000;
               int var18;
               label99: {
                  short var10001;
                  if (var11.objectType >= 59 && var11.objectType <= 63) {
                     var10000 = var11;
                     var10001 = var8.ceilingHeight;
                  } else {
                     if (var11.objectType >= 100 && var11.objectType <= 102) {
                        var10000 = var11;
                        var18 = -var11.transform.y;
                        break label99;
                     }

                     var10000 = var11;
                     var10001 = var8.floorHeight;
                  }

                  var18 = -var10001 << 16;
               }

               var10000.screenHeight = var18 - var2;
               Texture var19;
               if (var15 != 0) {
                  var10000 = var11;
                  var19 = textureTable[var15 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.texture1 = var19;
               if (var16 != 0) {
                  var10000 = var11;
                  var19 = textureTable[var16 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.texture2 = var19;
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
         for(var17 = var10; var17 > 0 && var_118d[var17 - 1].compareDepth(var11); --var17) {
            var_118d[var17] = var_118d[var17 - 1];
         }

         var_118d[var17] = var11;
      }

      for(var10 = 0; var10 < var_119a; ++var10) {
         if ((var11 = var_118d[var10]).projectToScreen()) {
            if (var11.texture2 != null) {
               var11.calculateSpriteSize2();
               sub_410(var11.texture2, var8.getLightLevel(), (var11.screenPos.x >> 16) + 120, (var11.screenHeight >> 16) + 144, var11.screenPos.y, var11.spriteWidth2, var11.spriteHeight2);
            }

            if (var11.texture1 != null) {
               var11.calculateSpriteSize1();
               sub_410(var11.texture1, var8.getLightLevel(), (var11.screenPos.x >> 16) + 120, (var11.screenHeight >> 16) + 144, var11.screenPos.y, var11.spriteWidth1, var11.spriteHeight1);
            }
         }
      }

   }

   private static void renderWorld(int var0, int var1, int var2, int var3) {
      gameWorld.sub_4e6();
      Point2D[] var4 = gameWorld.transformVertices(var0, var2, var3);
      gameWorld.updateWorld();
      BSPNode.visibleSectorsCount = 0;
      gameWorld.getRootBSPNode().traverseBSP(player, gameWorld.getSectorDataAtPoint(var0, var2));
      int var5 = var2 << 8;
      int var6 = var0 << 8;
      int var7 = var3 << 1;
      int var8 = MathUtils.fastSin(var3);
      int var9 = MathUtils.fastCos(var3);
      var_10a4 = MainGameCanvas.var_98c == 1 && currentWeapon != 0;
      var_a80.resetRenderer();
      Sector.resetClipArrays();

      int var10;
      Sector var11;
      for(var10 = 0; var10 < BSPNode.visibleSectorsCount; ++var10) {
         var11 = BSPNode.visibleSectorsList[var10];
         if (var10 > 0 && Sector.isRenderComplete()) {
            BSPNode.visibleSectorsCount = var10;
            break;
         }

         if (var10 >= floorClipHistory.size()) {
            short[] var12 = new short[240];
            short[] var13 = new short[240];
            floorClipHistory.addElement(var12);
            ceilingClipHistory.addElement(var13);
         }

         System.arraycopy(Sector.floorClip, 0, (short[])((short[]) floorClipHistory.elementAt(var10)), 0, 240);
         System.arraycopy(Sector.ceilingClip, 0, (short[])((short[]) ceilingClipHistory.elementAt(var10)), 0, 240);
         WallSegment[] var15 = var11.walls;

         for(int var16 = 0; var16 < var15.length; ++var16) {
            renderWallSegment(var15[var16], var4, var6, var1, var5, var7);
         }
      }

      var_a80.renderAllSpans(var8, var9, -var6, -var5);

      for(var10 = BSPNode.visibleSectorsCount - 1; var10 >= 0; --var10) {
         var11 = BSPNode.visibleSectorsList[var10];
         System.arraycopy(floorClipHistory.elementAt(var10), 0, Sector.floorClip, 0, 240);
         System.arraycopy(ceilingClipHistory.elementAt(var10), 0, Sector.ceilingClip, 0, 240);
         renderDynamicObjects(var11, var0, var1, var2, (long)var8, (long)var9);
      }

   }

   public static int renderFrame(Graphics var0, int var1) {
      currentSector = gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);
      boolean var2 = false;
      int var3 = var1 - var_f88;
      var_f88 = var1;
      int var4 = MathUtils.fastHypot(player.velocityX, player.velocityY);
      var_f5f += var3 * var4 >> 2;
      int var8 = MathUtils.fastSin(var_f5f);
      int var5 = screenShake << 15;
      if ((screenShake & 1) > 0) {
         var5 = -var5;
      }

      cameraHeight = (currentSector.floorHeight + GameWorld.PLAYER_HEIGHT_OFFSET << 16) + var8 + var5;
      renderWorld(player.x, -cameraHeight, player.z, player.rotation);
      if (var_eba) {
         int var6 = 69120;

         for(int var7 = 0; var7 < var6; ++var7) {
            int[] var10000 = screenBuffer;
            var10000[var7] |= 16711680;
         }

         var_eba = false;
      }

      if (screenShake == 16) {
         --screenShake;
      }

      var0.drawRGB(screenBuffer, 0, 240, 0, 0, 240, 288, false);
      if (currentSector.getSectorType() == 666) {
         switch(MainGameCanvas.var_259) {
         case 3:
            if (!weaponsAvailable[8]) {
               messageText = "get the sniper rifle!";
               messageTimer = 30;
               break;
            }
         default:
            MainGameCanvas.var_295 = MainGameCanvas.var_259++;
            levelVariant = 0;
            var_480 = 1;
            break;
         case 4:
            messageText = "i think that's the wall|she mentioned";
            messageTimer = 30;
         }
      }

      if ((MainGameCanvas.var_e8b & 1) == 0 && MainGameCanvas.var_259 == 0 && currentSector.sectorId == 31) {
         messageText = "press 1 to open the door";
         messageTimer = 30;
      }

      return var8;
   }

   public static boolean updateGameLogic() {
      if (messageTimer > 0) {
         --messageTimer;
      }

      if (inputForward) {
         player.applyHorizontalForce(0, -196608);
      }

      if (inputLeft) {
         player.applyHorizontalForce(-196608, 0);
      }

      if (inputBackward) {
         player.applyHorizontalForce(0, 131072);
      }

      if (inputRight) {
         player.applyHorizontalForce(196608, 0);
      }

      if (inputLookUp) {
         player.applyForce(0, 0, -4500);
      }

      if (inputLookDown) {
         player.applyForce(0, 0, 4500);
      }

      int var0 = MathUtils.fastHypot(player.velocityX, player.velocityY);
      WallDefinition var1 = null;
      if (var0 > 262144) {
         player.applyDampedVelocity();
         var1 = gameWorld.handlePlayerMovement(player, currentSector);
         player.applyDampedVelocity();
         WallDefinition var2 = gameWorld.handlePlayerMovement(player, currentSector);
         if (var1 == null) {
            var1 = var2;
         }
      } else {
         player.applyVelocity();
         var1 = gameWorld.handlePlayerMovement(player, currentSector);
      }

      int var21;
      if (!var_3e3) {
         if (var1 != null) {
            var_e96 = (var21 = var1.getWallType()) != 1 && var21 != 11 && var21 != 26 && var21 != 28 && var21 != 51 && var21 != 62 ? null : var1;
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
         var21 = player.rotation;
         var3 = MathUtils.fastSin(102943 - var21);
         var4 = MathUtils.fastCos(102943 - var21);
         var5 = 1310720;
         var6 = player.x + MathUtils.fixedPointMultiply(var5, var4);
         var7 = player.z + MathUtils.fixedPointMultiply(var5, var3);
         Point2D[] var8;
         Point2D var9 = (var8 = gameWorld.vertices)[var_e96.startVertexId & '\uffff'];
         Point2D var10 = var8[var_e96.endVertexId & '\uffff'];
         if (GameWorld.sub_365(player.x, player.z, var6, var7, var9.x, var9.y, var10.x, var10.y)) {
            var10000 = var_e72 + 1;
         } else {
            var_e96 = null;
            var10000 = 0;
         }

         var_e72 = var10000;
         if (var_e72 >= 50) {
            messageText = var_e96.getWallType() == 62 ? "press 1 to move the lift" : "press 1 to open the door";
            messageTimer = 10;
         }
      } else {
         var_e72 = 0;
      }

      if (var_446) {
         MainGameCanvas.mapEnabled = !MainGameCanvas.mapEnabled;
         var_446 = false;
      }

      int var12;
      byte var10001;
      int var35;
      if (var_3e3) {
         ElevatorController var22;
         ElevatorController var41;
         if (currentSector.getSectorType() == 10 && (var22 = getElevatorController(currentSector)).elevatorState == 0) {
            if (currentSector.floorHeight == var22.minHeight) {
               var41 = var22;
               var10001 = 1;
            } else {
               var41 = var22;
               var10001 = 2;
            }

            var41.elevatorState = var10001;
         }

         var21 = player.rotation;
         var3 = MathUtils.fastSin(102943 - var21);
         var4 = MathUtils.fastCos(102943 - var21);
         var5 = 1310720;
         var6 = player.x + MathUtils.fixedPointMultiply(var5, var4);
         var7 = player.z + MathUtils.fixedPointMultiply(var5, var3);
         WallDefinition[] var30 = gameWorld.wallDefinitions;
         Point2D[] var32 = gameWorld.vertices;

         label389:
         for(var35 = 0; var35 < var30.length; ++var35) {
            WallDefinition var11;
            if ((var12 = (var11 = var30[var35]).getWallType()) == 1 || var12 == 11 || var12 == 26 || var12 == 28 || var12 == 51 || var12 == 62) {
               Point2D var13 = var32[var11.startVertexId & '\uffff'];
               Point2D var14 = var32[var11.endVertexId & '\uffff'];
               if (GameWorld.sub_365(player.x, player.z, var6, var7, var13.x, var13.y, var14.x, var14.y)) {
                  if ((MainGameCanvas.var_e8b & 1) == 0) {
                     MainGameCanvas.var_e8b = (byte)(MainGameCanvas.var_e8b | 1);
                  }

                  DoorController var38;
                  byte var42;
                  switch(var12) {
                  case 1:
                     (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                     var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     break label389;
                  case 11:
                     if (MainGameCanvas.var_259 == 7 && ammoCounts[6] == 0) {
                        messageText = "we'll need some dynamite|maybe i should look for some";
                        messageTimer = 50;
                        break label389;
                     }

                     MainGameCanvas.var_295 = MainGameCanvas.var_259++;
                     levelVariant = var11.getSpecialType();
                     var42 = 1;
                     break;
                  case 26:
                     if (var_d98[0]) {
                        (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                        var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     } else {
                        messageText = var_d98[1] ? "oops, i need another key..." : "oh, i need a key...";
                        messageTimer = 50;
                     }
                     break label389;
                  case 28:
                     if (var_d98[1]) {
                        (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                        var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     } else {
                        messageText = var_d98[0] ? "oops, i need another key..." : "oh, i need a key...";
                        messageTimer = 50;
                     }
                     break label389;
                  case 51:
                     MainGameCanvas.var_295 = MainGameCanvas.var_259--;
                     levelVariant = var11.getSpecialType();
                     var42 = -1;
                     break;
                  case 62:
                     SectorData var15;
                     ElevatorController var16;
                     if ((var16 = getElevatorController(var15 = var11.backSurface.linkedSector)).elevatorState == 0) {
                        if (var15.floorHeight == var16.minHeight) {
                           var41 = var16;
                           var10001 = 1;
                        } else {
                           var41 = var16;
                           var10001 = 2;
                        }

                        var41.elevatorState = var10001;
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

      SectorData var43;
      for(var21 = 0; var21 < var_b86.size(); ++var21) {
         DoorController var23;
         if ((var23 = (DoorController)var_b86.elementAt(var21)).controlledSector == currentSector && var23.doorState == 2) {
            var23.doorState = 1;
         }

         DoorController var44;
         switch(var23.doorState) {
         case 0:
            continue;
         case 1:
            var43 = var23.controlledSector;
            var43.ceilingHeight = (short)(var43.ceilingHeight + 2);
            if (var23.controlledSector.ceilingHeight < var23.targetCeilingHeight) {
               continue;
            }

            var23.controlledSector.ceilingHeight = var23.targetCeilingHeight;
            var44 = var23;
            var10001 = 100;
            break;
         case 2:
            var43 = var23.controlledSector;
            var43.ceilingHeight = (short)(var43.ceilingHeight - 2);
            if (var23.controlledSector.ceilingHeight > var23.controlledSector.floorHeight) {
               continue;
            }

            var23.controlledSector.ceilingHeight = var23.controlledSector.floorHeight;
            var44 = var23;
            var10001 = 0;
            break;
         default:
            ++var23.doorState;
            if (var23.doorState < 200) {
               continue;
            }

            var44 = var23;
            var10001 = 2;
         }

         var44.doorState = var10001;
      }

      for(var21 = 0; var21 < var_bd3.size(); ++var21) {
         ElevatorController var24;
         short var25;
         short var45;
         switch((var24 = (ElevatorController)var_bd3.elementAt(var21)).elevatorState) {
         case 0:
         default:
            continue;
         case 1:
            var43 = var24.controlledSector;
            var43.ceilingHeight = (short)(var43.ceilingHeight + 2);
            var43 = var24.controlledSector;
            var43.floorHeight = (short)(var43.floorHeight + 2);
            if (var24.controlledSector.floorHeight < var24.maxHeight) {
               continue;
            }

            var25 = (short)(var24.controlledSector.ceilingHeight - var24.controlledSector.floorHeight);
            var24.controlledSector.floorHeight = var24.maxHeight;
            var43 = var24.controlledSector;
            var45 = var24.maxHeight;
            break;
         case 2:
            var43 = var24.controlledSector;
            var43.ceilingHeight = (short)(var43.ceilingHeight - 2);
            var43 = var24.controlledSector;
            var43.floorHeight = (short)(var43.floorHeight - 2);
            if (var24.controlledSector.floorHeight > var24.minHeight) {
               continue;
            }

            var25 = (short)(var24.controlledSector.ceilingHeight - var24.controlledSector.floorHeight);
            var24.controlledSector.floorHeight = var24.minHeight;
            var43 = var24.controlledSector;
            var45 = var24.minHeight;
         }

         var43.ceilingHeight = (short)(var45 + var25);
         var24.elevatorState = 0;
      }

      if (gameWorld.updateProjectiles()) {
         return true;
      } else {
         GameObject[] var26 = gameWorld.staticObjects;

         for(var3 = 0; var3 < var26.length; ++var3) {
            GameObject var27;
            if ((var27 = var26[var3]) != null && var27.aiState != -1) {
               Transform3D var28;
               int var31;
               if (var27.aiState == 0) {
                  var28 = var27.transform;
                  if (gameWorld.getSectorDataAtPoint(var28.x, var28.z).isSectorVisible(currentSector)) {
                     if ((var7 = var28.x - player.x) < 0) {
                        var7 = -var7;
                     }

                     if ((var31 = var28.z - player.z) < 0) {
                        var31 = -var31;
                     }

                     if (var7 + var31 <= 67108864 && gameWorld.checkLineOfSight(player, var28)) {
                        var27.aiState = 1;
                     }
                  }
               } else {
                  if ((var6 = (var28 = var27.transform).x - player.x) < 0) {
                     var6 = -var6;
                  }

                  if ((var7 = var28.z - player.z) < 0) {
                     var7 = -var7;
                  }

                  if (var6 + var7 > 67108864) {
                     var27.aiState = 0;
                  }
               }

               if (var27.stateTimer > 0) {
                  --var27.stateTimer;
               }

               switch(var5 = var27.objectType) {
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

               Transform3D var29;
               SectorData var33;
               GameObject var46;
               if (var27.stateTimer == 0) {
                  switch(var27.aiState) {
                  case 1:
                     var27.aiState = 2;
                     var27.stateTimer = (var_ea3.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_177d[difficultyLevel];
                     var27.currentState = 0;
                     break;
                  case 2:
                     if (((var6 = var_ea3.nextInt() & Integer.MAX_VALUE) & 1) == 0) {
                        var27.aiState = 3;
                        var27.stateTimer = var6 % MainGameCanvas.var_180b[difficultyLevel] + MainGameCanvas.var_17b5[difficultyLevel];
                        var46 = var27;
                        var10001 = 2;
                     } else {
                        var27.aiState = 1;
                        var27.stateTimer = (var_ea3.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_1851[difficultyLevel];
                        var46 = var27;
                        var10001 = 0;
                     }

                     var46.currentState = var10001;
                     break;
                  case 3:
                     var29 = var27.transform;
                     var33 = gameWorld.getSectorDataAtPoint(var29.x, var29.z);
                     if (gameWorld.checkLineOfSight(player, var29)) {
                        var27.aiState = 4;
                        var27.stateTimer = 2;
                        if (var5 != 3002) {
                           var27.currentState = 3;
                        }

                        if (var5 == 3001) {
                           MainGameCanvas.sub_84e(4, false, 100, 1);
                           gameWorld.shootProjectile(var29, var33);
                        } else if (var5 == 3002) {
                           MainGameCanvas.sub_84e(5, false, 80, 1);
                           gameWorld.shootSpreadWeapon(var29, var33);
                        } else {
                           label320: {
                              var31 = 0;
                              int[] var48;
                              switch(var5) {
                              case 3003:
                                 MainGameCanvas.sub_84e(2, false, 80, 0);
                                 var48 = MainGameCanvas.var_122a;
                                 break;
                              case 3004:
                                 MainGameCanvas.sub_84e(2, false, 80, 0);
                                 var48 = MainGameCanvas.var_125b;
                                 break;
                              case 3005:
                                 MainGameCanvas.sub_84e(2, false, 80, 0);
                                 var48 = MainGameCanvas.var_1284;
                                 break;
                              case 3006:
                                 MainGameCanvas.sub_84e(3, false, 80, 0);
                                 var48 = MainGameCanvas.var_12d2;
                                 break;
                              default:
                                 break label320;
                              }

                              var31 = var48[difficultyLevel];
                           }

                           if (var31 > 0) {
                              MainGameCanvas.sub_882(var31 * 10);
                           }

                           if (applyDamage(var31)) {
                              return true;
                           }
                        }
                     } else {
                        var27.aiState = 2;
                        var27.stateTimer = (var_ea3.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_177d[difficultyLevel];
                        var27.currentState = 0;
                     }
                     break;
                  case 4:
                     var27.aiState = 2;
                     var27.stateTimer = (var_ea3.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_177d[difficultyLevel];
                     var27.currentState = 0;
                     break;
                  case 5:
                     var6 = var_ea3.nextInt() & Integer.MAX_VALUE;
                     var27.aiState = 3;
                     var27.stateTimer = var6 % MainGameCanvas.var_180b[difficultyLevel] + MainGameCanvas.var_17b5[difficultyLevel];
                     var27.currentState = 2;
                     break;
                  case 6:
                     var27.aiState = -1;
                     if (var5 == 3002) {
                        var46 = var27;
                        var10001 = 5;
                     } else {
                        var46 = var27;
                        var10001 = 6;
                     }

                     var46.currentState = var10001;
                     gameWorld.spawnPickUp(var27);
                  }
               }

               if (var27.aiState == 2) {
                  if ((var27.stateTimer & 3) == 0) {
                     if (var27.currentState == 0) {
                        var46 = var27;
                        var10001 = 1;
                     } else {
                        var46 = var27;
                        var10001 = 0;
                     }

                     var46.currentState = var10001;
                  }

                  var29 = var27.transform;
                  var33 = gameWorld.getSectorDataAtPoint(var29.x, var29.z);
                  var31 = var29.x - player.x;
                  int var34 = var29.z - player.z;
                  int var36;
                  if ((var35 = MathUtils.fastHypot(var31, var34)) > var_f32) {
                     var36 = MathUtils.fixedPointMultiply(MathUtils.preciseDivide(var31, var35), var27.getMovementSpeed());
                     var12 = MathUtils.fixedPointMultiply(MathUtils.preciseDivide(var34, var35), var27.getMovementSpeed());
                     int var37;
                     if ((var_ea3.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_1ad2[difficultyLevel] == 0) {
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
                        tempTransform.x = var29.x - var18;
                        tempTransform.z = var29.z - var19;
                        if (!gameWorld.checkCollision(var27, tempTransform, var33)) {
                           break;
                        }

                        var29.x = tempTransform.x;
                        var29.z = tempTransform.z;
                     }
                  } else {
                     var36 = var_ea3.nextInt() & Integer.MAX_VALUE;
                     var27.aiState = 3;
                     var27.stateTimer = var36 % MainGameCanvas.var_180b[difficultyLevel] + MainGameCanvas.var_17b5[difficultyLevel];
                     var27.currentState = 2;
                  }
               }
            }
         }

         if (currentSector.getSectorType() == 555) {
            MainGameCanvas.sub_882(10);
            if (applyDamage(1)) {
               return true;
            }
         }

         if (screenShake < 16 && screenShake > 0) {
            --screenShake;
         }

         player.scaleVelocity(39322, 65536, 39322, 26214);
         return false;
      }
   }

   private static DoorController getDoorController(SectorData var0) {
      for(int var1 = 0; var1 < var_b86.size(); ++var1) {
         DoorController var2;
         if ((var2 = (DoorController)var_b86.elementAt(var1)).controlledSector == var0) {
            return var2;
         }
      }

      DoorController var3;
      (var3 = new DoorController()).controlledSector = var0;
      var_b86.addElement(var3);
      return var3;
   }

   private static ElevatorController getElevatorController(SectorData var0) {
      for(int var1 = 0; var1 < var_bd3.size(); ++var1) {
         ElevatorController var2;
         if ((var2 = (ElevatorController)var_bd3.elementAt(var1)).controlledSector == var0) {
            return var2;
         }
      }

      ElevatorController var6;
      (var6 = new ElevatorController()).elevatorState = 0;
      var6.minHeight = 32767;
      var6.maxHeight = -32768;
      WallDefinition[] var7 = gameWorld.wallDefinitions;

      for(int var3 = 0; var3 < var7.length; ++var3) {
         WallDefinition var4;
         if ((var4 = var7[var3]).getWallType() == 62 && var4.backSurface.linkedSector == var0) {
            SectorData var5;
            if ((var5 = var4.frontSurface.linkedSector).floorHeight > var6.maxHeight) {
               var6.maxHeight = var5.floorHeight;
            }

            if (var5.floorHeight < var6.minHeight) {
               var6.minHeight = var5.floorHeight;
            }
         }
      }

      var6.controlledSector = var0;
      var_bd3.addElement(var6);
      return var6;
   }

   private static void clearInputState() {
      inputForward = false;
      inputLeft = false;
      inputBackward = false;
      inputRight = false;
      inputForward = false;
      inputLookUp = false;
      inputBackward = false;
      inputLookDown = false;
      inputFire = false;
      inputStrafe = false;
      inputRun = false;
      var_364 = false;
      var_3e3 = false;
      var_446 = false;
      var_3b8 = false;
      var_480 = 0;
      var_4c8 = 0;
   }

   private static void sub_3f5(Texture var0) {
      var_8f4 = var0;
   }

   private static void sub_410(Texture var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      int var10000;
      label68: {
         if (var0.horizontalOffset > 0) {
            var10000 = var2 - var0.horizontalOffset * var5 / var0.width;
         } else {
            if (var0.horizontalOffset >= 0) {
               break label68;
            }

            var10000 = var2 + var0.horizontalOffset;
         }

         var2 = var10000;
      }

      label62: {
         if (var0.verticalOffset > 0) {
            var10000 = var3 - var0.verticalOffset * var6 / var0.height;
         } else {
            if (var0.verticalOffset >= 0) {
               break label62;
            }

            var10000 = var3 + var0.verticalOffset;
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
            short var10 = var0.width;
            var11 = var0.height;
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

         int[] var17 = var0.colorPalettes[var16];
         int var18 = var3 + var6;

         for(int var19 = var8; var19 <= var9; ++var19) {
            sub_4e2(var0.getPixelRow(var14), var14 & 1, var17, var19 + var2, var3, var18, 0, var11);
            var14 = (var13 += var12) >>> 16;
         }

      }
   }

   private static void drawWallColumn(SectorData var0, Texture var1, Texture var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14, int var15, int var16, int var17, int var18, int var19, int var20, int var21, int var22, int var23, int var24, int var25) {
      int var26 = var3;
      int var27 = var9;
      if (var3 < 240 && var9 >= 0) {
         short[] var28 = Sector.floorClip;
         short[] var29 = Sector.ceilingClip;
         if (var3 < 0) {
            var26 = 0;
         }

         if (var9 >= 240) {
            var27 = 239;
         }

         short var30 = var1.height;
         short var31 = var2.height;
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
         short var42 = (short)var0.sectorId;
         Sprite var43 = var0.floorTexture;
         Sprite var44 = var0.ceilingTexture;
         int var45 = var0.lightLevel = var0.getLightLevel();
         long var46;
         long var48 = (var46 = (long)(var8 - var14)) >> 16;
         long var50 = (long)(var9 - var3 + 1 << 16) * (long)var14;
         var0.floorOffsetX = var24 * 120 >> 16;
         var0.ceilingOffsetX = var25 * 120 >> 16;

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
                     sub_50e(var1.getPixelRowFast(var65), var65 & 1, var1.colorPalettes[var60], var52, var61, var62, var17, var18, var30);
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
                  sub_50e(var2.getPixelRowFast(var65), var65 & 1, var2.colorPalettes[var60], var52, var63, var64, var19, var20, var31);
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
               var_a80.addRenderSpan(depthBuffer[var68], var67, var42, var68);
            }
         }

         if (var_958 >= 0) {
            var67 = (short)var_a27;

            for(var68 = var_958; var68 <= var_9b4; ++var68) {
               var_a80.addRenderSpan(depthBuffer[var68], var67, var42, var68);
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
      int[] var24 = screenBuffer;

      for(int var25 = var0; var25 <= var1; ++var25) {
         var24[var25] = var16[var3[(var19 & 16515072) + (var20 & 1056964608) >> 18]];
         var19 += var22;
         var20 += var23;
      }

   }

   private static void sub_477(short var0, int var1, int var2, int var3) {
      short var4 = Sector.floorClip[var1];
      short var5 = Sector.ceilingClip[var1];
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
               depthBuffer[var14] = var8;
            }

            for(var14 = var10; var14 <= var7; ++var14) {
               depthBuffer[var14] = var8;
            }

            for(var14 = var_9e4; var14 <= var13; ++var14) {
               var_a80.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9fa; ++var14) {
               var_a80.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }
         } else {
            if (var_9e4 >= 0) {
               var9 = (short)var_a34;

               for(var10 = var_9e4; var10 <= var_9fa; ++var10) {
                  var_a80.addRenderSpan(depthBuffer[var10], var9, var0, var10);
               }
            }

            for(int var15 = var6; var15 <= var7; ++var15) {
               depthBuffer[var15] = var8;
            }
         }

         var_a34 = var1;
         var_9e4 = var6;
         var_9fa = var7;
      }
   }

   private static void sub_4be(short var0, int var1, int var2, int var3) {
      short var4 = Sector.floorClip[var1];
      short var5 = Sector.ceilingClip[var1];
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
               depthBuffer[var14] = var8;
            }

            for(var14 = var10; var14 <= var7; ++var14) {
               depthBuffer[var14] = var8;
            }

            for(var14 = var_958; var14 <= var13; ++var14) {
               var_a80.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9b4; ++var14) {
               var_a80.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }
         } else {
            if (var_958 >= 0) {
               var9 = (short)var_a27;

               for(var10 = var_958; var10 <= var_9b4; ++var10) {
                  var_a80.addRenderSpan(depthBuffer[var10], var9, var0, var10);
               }
            }

            for(int var15 = var4; var15 <= var7; ++var15) {
               depthBuffer[var15] = var8;
            }
         }

         var_a27 = var1;
         var_958 = var4;
         var_9b4 = var7;
      }
   }

   private static void sub_4e2(byte[] var0, int var1, int[] var2, int var3, int var4, int var5, int var6, int var7) {
      short var8 = Sector.floorClip[var3];
      short var9 = Sector.ceilingClip[var3];
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
            int[] var17 = screenBuffer;
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
      short var9 = Sector.floorClip[var3];
      short var10 = Sector.ceilingClip[var3];
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
         int[] var18 = screenBuffer;
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
      short var4 = Sector.floorClip[var0];
      short var5 = Sector.ceilingClip[var0];
      int var6 = var1;
      int var7 = var2;
      if (var1 < var4) {
         var6 = var4;
      }

      if (var2 > var5) {
         var7 = var5;
      }

      int var8;
      int var9 = MathUtils.fastCos(var8 = MathUtils.fixedPointMultiply(var0 - 120 << 16, var_1103));
      int var10 = MathUtils.fixedPointMultiply(var0 - 120, var_10bb);
      int var11 = MathUtils.fastSin(var8);
      int var13 = MathUtils.fixedPointMultiply(MathUtils.fixedPointMultiply(102943, var11 + MathUtils.fixedPointMultiply(var9, var10)) + var3, var_1171) >> 8;
      byte[] var14 = var_8f4.getPixelRowFast(var13);
      int[] var15 = var_8f4.colorPalettes[8];
      int var16 = var6 * 240 + var0;
      int var17 = var7 * 240 + var0;
      int var18;
      int var19 = -(var18 = MathUtils.fixedPointMultiply(var9 * 200, var_1142)) * (144 - var6) + 6553600;
      int[] var20 = screenBuffer;
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
            if (weaponsAvailable[var2] && ammoCounts[var3] > 0) {
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
         if (ammoCounts[var1] > 0) {
            return var0;
         } else {
            int var2 = 0;

            for(int var3 = 7; var3 > 0; --var3) {
               if (var3 != 6) {
                  var1 = var3 != 3 && var3 != 4 ? var3 : 1;
                  if (weaponsAvailable[var3] && ammoCounts[var1] > 0) {
                     var2 = var3;
                     break;
                  }
               }
            }

            if (var2 == 0 && weaponsAvailable[6] && ammoCounts[6] > 0) {
               var2 = 6;
            }

            return var2;
         }
      }
   }

   public static boolean applyDamage(int var0) {
      playerArmor -= var0;
      if (playerArmor < 0) {
         var0 = -playerArmor;
         playerArmor = 0;
      } else {
         var0 = 0;
      }

      var_eba = true;
      playerHealth -= var0;
      if (playerHealth <= 0) {
         playerHealth = 0;
         return true;
      } else {
         return false;
      }
   }

   private static void sub_5f4() {
      spriteTable = new Sprite[128];
      textureTable = new Texture[256];
      paletteCache = new Hashtable();
   }

   private static void sub_60e() {
      gameWorld = null;
      resourceLoadState = 0;

      int var0;
      for(var0 = 0; var0 < 128; ++var0) {
         spriteTable[var0] = null;
      }

      for(var0 = 0; var0 < 256; ++var0) {
         textureTable[var0] = null;
      }

      paletteCache.clear();
   }

   public static boolean loadMapData(String var0, boolean var1) {
      sub_60e();

      try {
         InputStream var3 = (new Object()).getClass().getResourceAsStream(var0);
         DataInputStream var4 = new DataInputStream(var3);
         gameWorld = new GameWorld();
         var4.readByte();
         Point2D[] var6 = new Point2D[readIntLE(var4) / 4];

         for(int var7 = 0; var7 < var6.length; ++var7) {
            var6[var7] = new Point2D(readShortLE(var4) << 16, readShortLE(var4) << 16);
         }

         gameWorld.setVertices(var6);
         WallDefinition[] var25 = new WallDefinition[readIntLE(var4) / 11];

         int var8;
         short var14;
         short var15;
         for(var8 = 0; var8 < var25.length; ++var8) {
            short var9 = readShortLE(var4);
            short var10 = readShortLE(var4);
            byte var11 = var4.readByte();
            byte var12 = var4.readByte();
            byte var13 = var4.readByte();
            var14 = readShortLE(var4);
            var15 = readShortLE(var4);
            var25[var8] = new WallDefinition(var9, var10, var14, var15, var11, var12, var13);
         }

         gameWorld.wallDefinitions = var25;
         int var5 = readIntLE(var4);
         if (levelVariant == 0) {
            levelVariant = 1;
         }

         var8 = var5 / 10;
         GameObject[] var26 = null;
         if (var1) {
            var26 = new GameObject[var8];
         }

         short var31;
         short var33;
         for(int var27 = 0; var27 < var8; ++var27) {
            short var29 = readShortLE(var4);
            var31 = readShortLE(var4);
            var33 = readShortLE(var4);
            var14 = readShortLE(var4);
            var15 = readShortLE(var4);
            GameObject[] var10000;
            int var10001;
            GameObject var10002;
            if (var14 >= 1 && var14 <= 4) {
               if (levelVariant == var14) {
                  gameWorld.worldOrigin = new Transform3D(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943);
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
               var10002 = new GameObject(new Transform3D(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943), var33, var14, var15);
            }

            var10000[var10001] = var10002;
         }

         if (var1) {
            gameWorld.staticObjects = var26;
         }

         WallSurface[] var28 = new WallSurface[readIntLE(var4) / 8];

         byte var16;
         byte var38;
         for(int var30 = 0; var30 < var28.length; ++var30) {
            var31 = readShortLE(var4);
            var33 = readShortLE(var4);
            byte var36 = sub_65b(var4.readByte());
            var38 = sub_65b(var4.readByte());
            var16 = sub_65b(var4.readByte());
            byte var17 = var4.readByte();
            var28[var30] = new WallSurface(var36, var16, var38, var17, var31, var33);
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

         gameWorld.wallSurfaces = var28;
         SectorData[] var32 = new SectorData[readIntLE(var4) / 12];
         short var42 = 0;

         while(true) {
            var31 = var42;
            short var18;
            short var44;
            if (var42 >= var32.length) {
               gameWorld.sectors = var32;
               BSPNode[] var34 = new BSPNode[readIntLE(var4) / 12];

               short var41;
               int var48;
               for(int var35 = 0; var35 < var34.length; ++var35) {
                  var14 = readShortLE(var4);
                  var15 = readShortLE(var4);
                  var41 = readShortLE(var4);
                  var44 = readShortLE(var4);
                  int var46 = readShortLE(var4) & '\uffff';
                  var48 = readShortLE(var4) & '\uffff';
                  var34[var35] = new BSPNode(var14 << 16, var15 << 16, var41 << 16, var44 << 16, var46, var48);
               }

               gameWorld.bspNodes = var34;
               Sector[] var37 = new Sector[(var5 = readIntLE(var4)) / 4];

               for(int var39 = 0; var39 < var37.length; ++var39) {
                  var15 = readShortLE(var4);
                  var41 = readShortLE(var4);
                  var37[var39] = new Sector(var15, var41);
               }

               gameWorld.bspSectors = var37;
               BSPNode.visibleSectorsList = new Sector[var5 / 4];
               WallSegment[] var40 = new WallSegment[readIntLE(var4) / 9];

               int var43;
               for(var43 = 0; var43 < var40.length; ++var43) {
                  var41 = readShortLE(var4);
                  var44 = readShortLE(var4);
                  var18 = readShortLE(var4);
                  boolean var49 = var4.readByte() == 0;
                  short var20 = readShortLE(var4);
                  var40[var43] = new WallSegment(var41, var44, var18, var49, var20);
               }

               gameWorld.wallSegments = var40;
               readIntLE(var4);
               boolean[][] var45 = new boolean[var43 = gameWorld.sectors.length][var43];
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
                  gameWorld.sectors[var48].visitedFlags = var45[var48];
               }

               var4.close();
               break;
            }

            var33 = readShortLE(var4);
            var14 = readShortLE(var4);
            var38 = sub_65b(var4.readByte());
            var16 = sub_65b(var4.readByte());
            var44 = readShortLE(var4);
            var18 = readShortLE(var4);
            short var19 = readShortLE(var4);
            var32[var31] = new SectorData(var31, var33, var14, var38, var16, (short)(var44 >> 4 & 15), var18, var19);
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

      resourceLoadState = 1;
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
      if (resourceLoadState < 1) {
         throw new IllegalStateException();
      } else if (resourceLoadState > 1) {
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

                     if ((readShortBE(var8 = new DataInputStream(var7)) & '\uffff') != 39251) {
                        throw new IllegalStateException();
                     }

                     var9 = readShortBE(var8);
                     var10 = readShortBE(var8);
                     readIntBE(var8);

                     for(var16 = 0; var16 < var9; ++var16) {
                        var15 = var8.readByte();
                        var12 = readShortBE(var8);
                        var13 = readShortBE(var8);
                        var17 = readShortBE(var8);
                        var18 = readShortBE(var8);
                        short var43 = readShortBE(var8);
                        short var45 = readShortBE(var8);
                        int var44;
                        int var47 = (var44 = var12 * var13 * var45) / 8;
                        if (var44 % 8 > 0) {
                           ++var47;
                        }

                        if (sub_8bc(var15)) {
                           Texture var48 = new Texture(var15, var12, var13, var17, var18);
                           byte[] var49 = new byte[var47];
                           var8.readFully(var49, 0, var47);
                           byte[] var26 = null;

                           for(int var27 = 0; var27 < var12; ++var27) {
                              if ((var27 & 1) == 0) {
                                 var26 = new byte[var13];
                              }

                              decompressTexture(var49, var27 * var13, var26, 0, var13, var45, var27 & 1);
                              var48.setPixelData(var27, var26);
                           }

                           textureTable[var15 + 128] = var48;
                           var5.put(new Byte(var15), new Integer(var4 + var43));
                        } else {
                           skipBytes(var8, var47);
                        }
                     }

                     for(var16 = 0; var16 < var10; ++var16) {
                        var40 = readIntBE(var8);
                        if (!var5.contains(new Integer(var4 + var16))) {
                           skipBytes(var8, 4 * var40);
                        } else {
                           var41 = new int[var40];

                           for(var19 = 0; var19 < var40; ++var19) {
                              var41[var19] = readIntBE(var8);
                           }

                           var42 = Texture.createColorPalettes(var41);
                           paletteCache.put(Integer.toString(var4 + var16), var42);
                        }
                     }

                     var4 += var10;
                     var8.close();
                  }

                  for(var6 = 0; var6 < 128; ++var6) {
                     Sprite var30;
                     if ((var30 = spriteTable[var6]) != null) {
                        if (var30.pixelData == null) {
                           throw new IllegalStateException();
                        }

                        var30.colorPalettes = (int[][])((int[][]) paletteCache.get(((Integer)((Integer)var5.get(new Byte(var30.spriteId)))).toString()));
                     }
                  }

                  byte[][] var31 = (byte[][])null;
                  byte[][] var32 = (byte[][])null;
                  byte[][] var33 = (byte[][])null;
                  Texture var34;
                  if ((var34 = textureTable[163]) != null) {
                     var31 = var34.pixelData;
                  }

                  if ((var34 = textureTable[174]) != null) {
                     var32 = var34.pixelData;
                  }

                  if ((var34 = textureTable[177]) != null) {
                     var33 = var34.pixelData;
                  }

                  for(int var35 = 0; var35 < 256; ++var35) {
                     Texture var11;
                     if ((var11 = textureTable[var35]) != null) {
                        byte var38;
                        label150: {
                           byte var10000;
                           switch(var38 = var11.textureType) {
                           case 35:
                              var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
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
                              var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
                              var11.compositeTexture(var31, 0, 128, 64, 18, 0, 0, false);
                              var11.compositeTexture(var31, 64, 17, 10, 111, 54, 17, false);
                              var10000 = 35;
                              break;
                           case 40:
                              var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
                              var11.compositeTexture(var31, 0, 128, 64, 18, 0, 0, false);
                              var11.compositeTexture(var31, 74, 17, 10, 111, 0, 17, false);
                              var10000 = 35;
                              break;
                           case 46:
                              var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                              break label150;
                           case 47:
                              var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                              var11.compositeTexture(var32, 64, 0, 14, 128, 50, 0, false);
                              var10000 = 46;
                              break;
                           case 48:
                              var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                              var11.compositeTexture(var32, 64, 0, 14, 128, 50, 0, false);
                              var11.compositeTexture(var32, 78, 0, 20, 24, 19, 43, false);
                              var10000 = 46;
                              break;
                           case 49:
                              var11.compositeTexture(var33, 0, 0, 64, 128, 0, 0, true);
                              break label150;
                           case 50:
                              var11.compositeTexture(var33, 0, 0, 64, 128, 0, 0, true);
                              var11.compositeTexture(var33, 64, 0, 20, 27, 25, 19, false);
                              var10000 = 49;
                           }

                           var38 = var10000;
                        }

                        if (var11.width != 0) {
                           var11.colorPalettes = (int[][])((int[][]) paletteCache.get(((Integer)((Integer)var5.get(new Byte(var38)))).toString()));
                        }
                     }
                  }

                  SectorData[] var36 = gameWorld.sectors;
                  int var37 = 0;

                  while(true) {
                     if (var37 >= var36.length) {
                        break label233;
                     }

                     SectorData var39 = null;
                     (var39 = var36[var37]).floorTexture = sub_cb(var39.floorTextureId);
                     var39.ceilingTexture = sub_cb(var39.ceilingTextureId);
                     ++var37;
                  }
               }

               if ((var7 = (new Object()).getClass().getResourceAsStream(var0 + Integer.toString(var6))) == null) {
                  throw new IllegalStateException();
               }

               if ((readShortBE(var8 = new DataInputStream(var7)) & '\uffff') != 39252) {
                  throw new IllegalStateException();
               }

               var9 = readShortBE(var8);
               var10 = readShortBE(var8);
               readIntBE(var8);

               for(var16 = 0; var16 < var9; ++var16) {
                  var15 = var8.readByte();
                  var12 = readShortBE(var8);
                  var13 = readShortBE(var8);
                  var17 = readShortBE(var8);
                  var18 = readShortBE(var8);
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
                     decompressSprite(var21, 0, var22, 0, var12 * var13, var18);
                     spriteTable[var15] = new Sprite(var15, var22);
                     var5.put(new Byte(var15), new Integer(var4 + var17));
                  } else if (sub_8bc(var15)) {
                     Texture var46 = new Texture(var15, var12, var13, 0, 0);
                     byte[] var23 = new byte[var20];
                     var8.readFully(var23, 0, var20);
                     byte[] var24 = null;

                     for(int var25 = 0; var25 < var12; ++var25) {
                        if ((var25 & 1) == 0) {
                           var24 = new byte[var13];
                        }

                        decompressTexture(var23, var25 * var13, var24, 0, var13, var18, var25 & 1);
                        var46.setPixelData(var25, var24);
                     }

                     textureTable[var15 + 128] = var46;
                     var5.put(new Byte(var15), new Integer(var4 + var17));
                  } else {
                     skipBytes(var8, var20);
                  }
               }

               for(var16 = 0; var16 < var10; ++var16) {
                  var40 = readIntBE(var8);
                  if (!var5.contains(new Integer(var4 + var16))) {
                     skipBytes(var8, 4 * var40);
                  } else {
                     var41 = new int[var40];

                     for(var19 = 0; var19 < var40; ++var19) {
                        var41[var19] = readIntBE(var8);
                     }

                     var42 = Texture.createColorPalettes(var41);
                     paletteCache.put(Integer.toString(var4 + var16), var42);
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

         resourceLoadState = 2;
         return true;
      }
   }

   private static short readShortBE(DataInputStream var0) throws IOException {
      return (short)((var0.read() << 8) + var0.read());
   }

   private static int readIntBE(DataInputStream var0) throws IOException {
      return (var0.read() << 24) + (var0.read() << 16) + (var0.read() << 8) + var0.read();
   }

   private static short readShortLE(DataInputStream var0) throws IOException {
      return (short)(var0.read() + (var0.read() << 8));
   }

   private static int readIntLE(DataInputStream var0) throws IOException {
      return var0.read() + (var0.read() << 8) + (var0.read() << 16) + (var0.read() << 24);
   }

   private static void skipBytes(DataInputStream var0, int var1) throws IOException {
      while(var1 > 0) {
         int var2 = var1 > 4096 ? 4096 : var1;
         int var3 = (int)var0.skip((long)var2);
         var1 -= var3;
      }

   }

   public static void sub_80f(byte var0) {
      if (!sub_8bc(var0)) {
         textureTable[var0 + 128] = new Texture(var0, 0, 0, 0, 0);
      }

   }

   private static void sub_855(byte var0) {
      if (var0 != 51) {
         if (!sub_887(var0)) {
            spriteTable[var0] = new Sprite(var0);
         }

      }
   }

   private static boolean sub_887(int var0) {
      if (var0 >= 0 && var0 < 128) {
         return spriteTable[var0] != null;
      } else {
         return false;
      }
   }

   private static boolean sub_8bc(int var0) {
      var0 += 128;
      if (var0 >= 0 && var0 < 256) {
         return textureTable[var0] != null;
      } else {
         return false;
      }
   }

   public static void decompressSprite(byte[] var0, int var1, byte[] var2, int var3, int var4, int var5) {
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

   private static void decompressTexture(byte[] var0, int var1, byte[] var2, int var3, int var4, int var5, int var6) {
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
      playerHealth = 100;
      playerArmor = 0;

      for(int var0 = 0; var0 < weaponsAvailable.length; ++var0) {
         weaponsAvailable[var0] = false;
         ammoCounts[var0] = 0;
      }

      weaponsAvailable[0] = true;
      weaponsAvailable[6] = true;
      currentWeapon = 0;
      var_d46 = 0;
      messageText = "";
      messageTimer = 0;
      var_e72 = 0;
      var_e96 = null;
      var_eba = false;
      screenShake = 0;
      var_f5f = 0;
      var_f88 = 0;
      levelComplete = true;
      var_1044 = 1;
      MainGameCanvas.var_e8b = 0;
      levelVariant = 0;
   }
}
