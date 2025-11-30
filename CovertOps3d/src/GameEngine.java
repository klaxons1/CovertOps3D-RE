import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public final class GameEngine {
    public static boolean inputForward;
   public static boolean inputBackward;
   public static boolean inputLeft;
   public static boolean inputRight;
   public static boolean inputLookUp;
   public static boolean inputLookDown;
   public static boolean inputFire;
   public static boolean inputStrafe;
   public static boolean inputRun;
   public static boolean inputBack;
   public static boolean selectNextWeapon;
   public static boolean useKey;
   public static boolean toggleMapInput;
   public static int levelTransitionState;
   public static int weaponCooldownTimer = 0;
    public static PhysicsBody player;
   public static Transform3D tempTransform;
   public static SectorData currentSector;
   private static Point2D clippedWallStart = new Point2D(0, 0);
   private static Point2D clippedWallEnd = new Point2D(0, 0);
   private static int projectedCeilingStart;
   private static int projectedFloorStart;
   private static int projectedCeilingEnd;
   private static int projectedFloorEnd;
   private static int clippedTextureStartU;
   private static int clippedTextureEndU;
   public static int[] screenBuffer;
    private static Texture skyboxTexture;
   private static short[] depthBuffer;
   private static int playerViewHeight;
   private static int var_9b4;
   private static int var_9e4;
   private static int var_9fa;
   private static int var_a27;
   private static int var_a34;
   private static RenderUtils renderUtils;
   private static int[] angleCorrectionTable;
   private static int[] reciprocalTable;
   public static Vector floorClipHistory;
   public static Vector ceilingClipHistory;
   public static Vector doorControllers;
   public static Vector elevatorControllers;
   public static int playerHealth = 100;
   public static int playerArmor = 0;
   public static int difficultyLevel = 1;
   public static boolean[] weaponsAvailable = new boolean[]{true, false, false, false, false, false, true, false, false};
   public static int[] ammoCounts = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
   public static int currentWeapon = 0;
   public static int pendingWeaponSwitch = 0;
   public static boolean[] keysCollected = new boolean[]{false, false};
   public static String messageText = "";
   public static int messageTimer = 0;
   public static int interactionTimer = 0;
   public static WallDefinition activeInteractable = null;
   public static Random random = new Random();
   public static boolean damageFlash = false;
   public static byte screenShake = 0;
   public static int cameraHeight;
   private static int enemyAggroDistance = MathUtils.fixedPointMultiply(1310720, 92682);
   private static int cameraBobTimer = 0;
   private static int lastGameLogicTime = 0;
   public static boolean weaponSwitchAnimationActive = false;
   public static int weaponAnimationState = 0;
   public static boolean gunFireLighting = false;
   public static int skyboxScaleX;
   public static int skyboxAngleFactor;
   public static int skyboxScaleY;
   public static int skyboxOffsetFactor;
   public static GameObject[] visibleGameObjects;
   public static int visibleObjectsCount;

   public static void initializeEngine() {
      MainGameCanvas.freeMemory();
      LevelLoader.initResourceArrays();
      player = new PhysicsBody(0, 1572864, 0, 65536);
      tempTransform = new Transform3D(0, 0, 0, 0);
      floorClipHistory = new Vector();
      ceilingClipHistory = new Vector();
      doorControllers = new Vector();
      elevatorControllers = new Vector();
      visibleGameObjects = new GameObject[64];
      visibleObjectsCount = 0;
      BSPNode.visibleSectorsCount = 0;
      LevelLoader.defaultErrorTexture = new Texture((byte)0, 8, 8, 0, 0, new int[]{16777215, 16711680});
      byte[] var0 = new byte[]{17, 17, 17, 17, 17, 17, 17, 17};
      byte[] var1 = new byte[]{17, 16, 16, 16, 16, 17, 16, 17};
      byte[] var2 = new byte[]{17, 1, 1, 1, 1, 17, 1, 17};
      LevelLoader.defaultErrorTexture.setPixelData(0, var0);
      LevelLoader.defaultErrorTexture.setPixelData(2, var1);
      LevelLoader.defaultErrorTexture.setPixelData(4, var2);
      LevelLoader.defaultErrorTexture.setPixelData(6, var0);
      screenBuffer = new int[69120];
      depthBuffer = new short[288];
      renderUtils = new RenderUtils();
      angleCorrectionTable = new int[240];

      for(int var3 = 0; var3 < 240; ++var3) {
         angleCorrectionTable[var3] = MathUtils.fixedPointDivide(var3 - 120 << 16, 7864320) >> 2;
      }

      reciprocalTable = new int[289];
      reciprocalTable[0] = 0;

      for(int var4 = 1; var4 < 289; ++var4) {
         reciprocalTable[var4] = 65536 / var4;
      }

      skyboxScaleX = MathUtils.fixedPointDivide(65536, 17301600);
      skyboxAngleFactor = MathUtils.fixedPointMultiply(MathUtils.fixedPointDivide(65536, 15794176), 102943);
      skyboxScaleY = MathUtils.fixedPointDivide(65536, 18874368);
      skyboxOffsetFactor = MathUtils.fixedPointDivide(65536, 411775);
      MainGameCanvas.freeMemory();
   }

   public static void resetLevelState() {
      clearInputState();
      LevelLoader.gameWorld.initializeWorld();
      player.copyFrom(LevelLoader.gameWorld.worldOrigin);
      currentSector = LevelLoader.gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);
      doorControllers.removeAllElements();
      elevatorControllers.removeAllElements();
      visibleObjectsCount = 0;
      BSPNode.visibleSectorsCount = 0;
      messageText = "";
      messageTimer = 0;
      interactionTimer = 0;
      activeInteractable = null;
   }

   public static void handleWeaponChange(byte var0) {
      setSkyboxTexture(LevelLoader.getTexture(var0));
   }

    private static boolean clipAndProjectWallSegment(Point2D var0, Point2D var1, int var2, int var3, int var4) {
      if (var0.y <= 327680 && var1.y <= 327680) {
         return false;
      } else {
         int var5;
         int var6 = (var5 = var4 << 16) + MathUtils.fastHypot(var0.x - var1.x, var0.y - var1.y);
         clippedTextureStartU = var5;
         clippedTextureEndU = var6;
         clippedWallStart.x = var0.x;
         clippedWallStart.y = var0.y;
         clippedWallEnd.x = var1.x;
         clippedWallEnd.y = var1.y;
         int var7;
         if (var1.y < 327680) {
            var7 = MathUtils.fixedPointDivide(var0.y - 327680, var0.y - var1.y);
            clippedWallEnd.y = 327680;
            if (var7 == Integer.MAX_VALUE) {
               clippedWallEnd.x = var1.x > var0.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               clippedTextureEndU = var6 > var5 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               clippedWallEnd.x = var1.x > var0.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               clippedTextureEndU = var6 > var5 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               clippedWallEnd.x = (int)((long)(var1.x - var0.x) * (long)var7 >> 16) + var0.x;
               clippedTextureEndU = (int)((long)(var6 - var5) * (long)var7 >> 16) + var5;
            }
         }

         if (var0.y < 327680) {
            var7 = MathUtils.fixedPointDivide(var1.y - 327680, var1.y - var0.y);
            clippedWallStart.y = 327680;
            if (var7 == Integer.MAX_VALUE) {
               clippedWallStart.x = var0.x > var1.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
               clippedTextureStartU = var5 > var6 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (var7 == Integer.MIN_VALUE) {
               clippedWallStart.x = var0.x > var1.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
               clippedTextureStartU = var5 > var6 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
               clippedWallStart.x = (int)((long)(var0.x - var1.x) * (long)var7 >> 16) + var1.x;
               clippedTextureStartU = (int)((long)(var5 - var6) * (long)var7 >> 16) + var6;
            }
         }

         long var11 = 33776997205278720L / (long) clippedWallStart.y >> 16;
         long var9 = 33776997205278720L / (long) clippedWallEnd.y >> 16;
         clippedWallStart.x = (int)((long) clippedWallStart.x * var11 >> 16);
         if (clippedWallStart.x > 7864320) {
            return false;
         } else {
            clippedWallEnd.x = (int)((long) clippedWallEnd.x * var9 >> 16);
            if (clippedWallEnd.x < -7864320) {
               return false;
            } else if (clippedWallEnd.x < clippedWallStart.x) {
               return false;
            } else {
                projectedCeilingStart = (int)((long)var2 * var11 >> 16);
                projectedFloorStart = (int)((long)var3 * var11 >> 16);
                projectedCeilingEnd = (int)((long)var2 * var9 >> 16);
                projectedFloorEnd = (int)((long)var3 * var9 >> 16);
               return true;
            }
         }
      }
   }

   private static void renderSolidWallSegment(WallSegment var0, WallDefinition var1, WallSurface var2, Point2D[] var3, int var4, int var5, int var6, int var7) {
      SectorData var8 = var2.linkedSector;
      int var9 = -var5 + (-var8.ceilingHeight << 16);
      int var10 = -var5 + (-var8.floorHeight << 16);
      if (clipAndProjectWallSegment(var3[var0.startVertexIndex & '\uffff'], var3[var0.endVertexIndex & '\uffff'], var9, var10, var0.textureOffset & '\uffff')) {
         Point2D var11 = clippedWallStart;
         Point2D var12 = clippedWallEnd;
         int var13 = var11.x + 7864320 >> 16;
         int var14 = projectedCeilingStart + 9437184 >> 16;
         int var15 = var12.x + 7864320 >> 16;
         int var16 = projectedCeilingEnd + 9437184 >> 16;
         int var17 = projectedFloorStart + 9437184 >> 16;
         int var18 = projectedFloorEnd + 9437184 >> 16;
         int var19 = var8.ceilingHeight - var8.floorHeight;
         Texture var20 = LevelLoader.getTexture(var2.mainTextureId);
         int var21 = var2.textureOffsetY & '\uffff';
         int var22 = var20.height - var19 + var21;
         if (!var1.isSecret()) {
            var22 = var21;
         }

         var1.markAsRendered();
         int var23 = (var2.textureOffsetX & '\uffff') << 16;
         drawWallColumn(var8, var20, var20, var13, var14, var17, var17, var17, var11.y, var15, var16, var18, var18, var18, var12.y, clippedTextureStartU + var23, clippedTextureEndU - clippedTextureStartU, var22, var19, var22, var19, -var4, -var6, var7, var9, var10);
      }

   }

   private static void renderPortalWallSegment(WallSegment var0, WallDefinition var1, WallSurface var2, WallSurface var3, Point2D[] var4, int var5, int var6, int var7, int var8) {
      SectorData var9 = var2.linkedSector;
      SectorData var10 = var3.linkedSector;
      int var11 = -var6 + (-var9.ceilingHeight << 16);
      int var12 = -var6 + (-var9.floorHeight << 16);
      int var13 = -var6 + (-var10.ceilingHeight << 16);
      int var14 = -var6 + (-var10.floorHeight << 16);
      if (clipAndProjectWallSegment(var4[var0.startVertexIndex & '\uffff'], var4[var0.endVertexIndex & '\uffff'], var11, var12, var0.textureOffset & '\uffff')) {
         Point2D var15 = clippedWallStart;
         Point2D var16 = clippedWallEnd;
         int var17 = var15.x + 7864320 >> 16;
         int var18 = projectedCeilingStart + 9437184 >> 16;
         int var19 = projectedFloorStart + 9437184 >> 16;
         int var20 = var16.x + 7864320 >> 16;
         int var21 = projectedCeilingEnd + 9437184 >> 16;
         int var22 = projectedFloorEnd + 9437184 >> 16;
         int var23 = MathUtils.fixedPointDivide(var13, var15.y) * 120 + 9437184 >> 16;
         int var24 = MathUtils.fixedPointDivide(var14, var15.y) * 120 + 9437184 >> 16;
         int var25 = MathUtils.fixedPointDivide(var13, var16.y) * 120 + 9437184 >> 16;
         int var26 = MathUtils.fixedPointDivide(var14, var16.y) * 120 + 9437184 >> 16;
         int var27 = var9.ceilingHeight - var10.ceilingHeight;
         int var28 = var10.floorHeight - var9.floorHeight;
         Texture var29 = LevelLoader.getTexture(var2.upperTextureId);
         Texture var30 = LevelLoader.getTexture(var2.lowerTextureId);
         if (var10.floorTextureId == 51) {
            var29 = LevelLoader.defaultErrorTexture;
         }

         if (var10.ceilingTextureId == 51) {
            var30 = LevelLoader.defaultErrorTexture;
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
         drawWallColumn(var9, var29, var30, var17, var18, var23, var24, var19, var15.y, var20, var21, var25, var26, var22, var16.y, clippedTextureStartU + var34, clippedTextureEndU - clippedTextureStartU, var32, var27, var33, var28, -var5, -var7, var8, var11, var12);
      }

   }

   private static void renderWallSegment(WallSegment var0, Point2D[] var1, int var2, int var3, int var4, int var5) {
      WallDefinition var6;
      WallSurface var7 = (var6 = var0.wallDefinition).frontSurface;
      WallSurface var8;
      if ((var8 = var6.backSurface) != null) {
         if (var0.isFrontFacing) {
            renderPortalWallSegment(var0, var6, var7, var8, var1, var2, var3, var4, var5);
         } else {
            renderPortalWallSegment(var0, var6, var8, var7, var1, var2, var3, var4, var5);
         }
      } else if (var0.isFrontFacing) {
         renderSolidWallSegment(var0, var6, var7, var1, var2, var3, var4, var5);
      } else {
         throw new IllegalStateException();
      }
   }

   private static void renderDynamicObjects(Sector var0, int var1, int var2, int var3, long var4, long var6) {
      SectorData var8 = var0.getSectorData();
      Vector var9 = var0.dynamicObjects;
      visibleObjectsCount = 0;

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
                  var19 = LevelLoader.textureTable[var15 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.texture1 = var19;
               if (var16 != 0) {
                  var10000 = var11;
                  var19 = LevelLoader.textureTable[var16 + 128];
               } else {
                  var10000 = var11;
                  var19 = null;
               }

               var10000.texture2 = var19;
               visibleGameObjects[visibleObjectsCount++] = var11;
               if (visibleObjectsCount >= 64) {
                  break;
               }
            }
         }
      }

      for(var10 = 1; var10 < visibleObjectsCount; ++var10) {
         var11 = visibleGameObjects[var10];

         int var17;
         for(var17 = var10; var17 > 0 && visibleGameObjects[var17 - 1].compareDepth(var11); --var17) {
            visibleGameObjects[var17] = visibleGameObjects[var17 - 1];
         }

         visibleGameObjects[var17] = var11;
      }

      for(var10 = 0; var10 < visibleObjectsCount; ++var10) {
         if ((var11 = visibleGameObjects[var10]).projectToScreen()) {
            if (var11.texture2 != null) {
               var11.calculateSpriteSize2();
               drawSprite(var11.texture2, var8.getLightLevel(), (var11.screenPos.x >> 16) + 120, (var11.screenHeight >> 16) + 144, var11.screenPos.y, var11.spriteWidth2, var11.spriteHeight2);
            }

            if (var11.texture1 != null) {
               var11.calculateSpriteSize1();
               drawSprite(var11.texture1, var8.getLightLevel(), (var11.screenPos.x >> 16) + 120, (var11.screenHeight >> 16) + 144, var11.screenPos.y, var11.spriteWidth1, var11.spriteHeight1);
            }
         }
      }

   }

   private static void renderWorld(int var0, int var1, int var2, int var3) {
      LevelLoader.gameWorld.toggleProjectileSprites();
      Point2D[] var4 = LevelLoader.gameWorld.transformVertices(var0, var2, var3);
      LevelLoader.gameWorld.updateWorld();
      BSPNode.visibleSectorsCount = 0;
      LevelLoader.gameWorld.getRootBSPNode().traverseBSP(player, LevelLoader.gameWorld.getSectorDataAtPoint(var0, var2));
      int var5 = var2 << 8;
      int var6 = var0 << 8;
      int var7 = var3 << 1;
      int var8 = MathUtils.fastSin(var3);
      int var9 = MathUtils.fastCos(var3);
      gunFireLighting = MainGameCanvas.weaponSpriteFrame == 1 && currentWeapon != 0;
      renderUtils.resetRenderer();
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

      renderUtils.renderAllSpans(var8, var9, -var6, -var5);

      for(var10 = BSPNode.visibleSectorsCount - 1; var10 >= 0; --var10) {
         var11 = BSPNode.visibleSectorsList[var10];
         System.arraycopy(floorClipHistory.elementAt(var10), 0, Sector.floorClip, 0, 240);
         System.arraycopy(ceilingClipHistory.elementAt(var10), 0, Sector.ceilingClip, 0, 240);
         renderDynamicObjects(var11, var0, var1, var2, (long)var8, (long)var9);
      }

   }

   public static int renderFrame(Graphics var0, int var1) {
      currentSector = LevelLoader.gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);
      boolean var2 = false;
      int var3 = var1 - lastGameLogicTime;
      lastGameLogicTime = var1;
      int var4 = MathUtils.fastHypot(player.velocityX, player.velocityY);
      cameraBobTimer += var3 * var4 >> 2;
      int var8 = MathUtils.fastSin(cameraBobTimer);
      int var5 = screenShake << 15;
      if ((screenShake & 1) > 0) {
         var5 = -var5;
      }

      cameraHeight = (currentSector.floorHeight + GameWorld.PLAYER_HEIGHT_OFFSET << 16) + var8 + var5;
      renderWorld(player.x, -cameraHeight, player.z, player.rotation);
      if (damageFlash) {
         int var6 = 69120;

         for(int var7 = 0; var7 < var6; ++var7) {
            int[] var10000 = screenBuffer;
            var10000[var7] |= 16711680;
         }

         damageFlash = false;
      }

      if (screenShake == 16) {
         --screenShake;
      }

      var0.drawRGB(screenBuffer, 0, 240, 0, 0, 240, 288, false);
      if (currentSector.getSectorType() == 666) {
         switch(MainGameCanvas.currentLevelId) {
         case 3:
            if (!weaponsAvailable[8]) {
               messageText = "get the sniper rifle!";
               messageTimer = 30;
               break;
            }
         default:
            MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId++;
            LevelLoader.levelVariant = 0;
            levelTransitionState = 1;
            break;
         case 4:
            messageText = "i think that's the wall|she mentioned";
            messageTimer = 30;
         }
      }

      if ((MainGameCanvas.gameProgressFlags & 1) == 0 && MainGameCanvas.currentLevelId == 0 && currentSector.sectorId == 31) {
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
         var1 = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
         player.applyDampedVelocity();
         WallDefinition var2 = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
         if (var1 == null) {
            var1 = var2;
         }
      } else {
         player.applyVelocity();
         var1 = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
      }

      int var21;
      if (!useKey) {
         if (var1 != null) {
            activeInteractable = (var21 = var1.getWallType()) != 1 && var21 != 11 && var21 != 26 && var21 != 28 && var21 != 51 && var21 != 62 ? null : var1;
         }
      } else {
         activeInteractable = null;
      }

      int var3;
      int var4;
      int var5;
      int var6;
      int var7;
      int var10000;
      if (activeInteractable != null) {
         var21 = player.rotation;
         var3 = MathUtils.fastSin(102943 - var21);
         var4 = MathUtils.fastCos(102943 - var21);
         var5 = 1310720;
         var6 = player.x + MathUtils.fixedPointMultiply(var5, var4);
         var7 = player.z + MathUtils.fixedPointMultiply(var5, var3);
         Point2D[] var8;
         Point2D var9 = (var8 = LevelLoader.gameWorld.vertices)[activeInteractable.startVertexId & '\uffff'];
         Point2D var10 = var8[activeInteractable.endVertexId & '\uffff'];
         if (GameWorld.doLineSegmentsIntersect(player.x, player.z, var6, var7, var9.x, var9.y, var10.x, var10.y)) {
            var10000 = interactionTimer + 1;
         } else {
            activeInteractable = null;
            var10000 = 0;
         }

         interactionTimer = var10000;
         if (interactionTimer >= 50) {
            messageText = activeInteractable.getWallType() == 62 ? "press 1 to move the lift" : "press 1 to open the door";
            messageTimer = 10;
         }
      } else {
         interactionTimer = 0;
      }

      if (toggleMapInput) {
         MainGameCanvas.mapEnabled = !MainGameCanvas.mapEnabled;
         toggleMapInput = false;
      }

      int var12;
      byte var10001;
      int var35;
      if (useKey) {
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
         WallDefinition[] var30 = LevelLoader.gameWorld.wallDefinitions;
         Point2D[] var32 = LevelLoader.gameWorld.vertices;

         label389:
         for(var35 = 0; var35 < var30.length; ++var35) {
            WallDefinition var11;
            if ((var12 = (var11 = var30[var35]).getWallType()) == 1 || var12 == 11 || var12 == 26 || var12 == 28 || var12 == 51 || var12 == 62) {
               Point2D var13 = var32[var11.startVertexId & '\uffff'];
               Point2D var14 = var32[var11.endVertexId & '\uffff'];
               if (GameWorld.doLineSegmentsIntersect(player.x, player.z, var6, var7, var13.x, var13.y, var14.x, var14.y)) {
                  if ((MainGameCanvas.gameProgressFlags & 1) == 0) {
                     MainGameCanvas.gameProgressFlags = (byte)(MainGameCanvas.gameProgressFlags | 1);
                  }

                  DoorController var38;
                  byte var42;
                  switch(var12) {
                  case 1:
                     (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                     var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     break label389;
                  case 11:
                     if (MainGameCanvas.currentLevelId == 7 && ammoCounts[6] == 0) {
                        messageText = "we'll need some dynamite|maybe i should look for some";
                        messageTimer = 50;
                        break label389;
                     }

                     MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId++;
                     LevelLoader.levelVariant = var11.getSpecialType();
                     var42 = 1;
                     break;
                  case 26:
                     if (keysCollected[0]) {
                        (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                        var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     } else {
                        messageText = keysCollected[1] ? "oops, i need another key..." : "oh, i need a key...";
                        messageTimer = 50;
                     }
                     break label389;
                  case 28:
                     if (keysCollected[1]) {
                        (var38 = getDoorController(var11.backSurface.linkedSector)).doorState = 1;
                        var38.targetCeilingHeight = var11.frontSurface.linkedSector.ceilingHeight;
                     } else {
                        messageText = keysCollected[0] ? "oops, i need another key..." : "oh, i need a key...";
                        messageTimer = 50;
                     }
                     break label389;
                  case 51:
                     MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId--;
                     LevelLoader.levelVariant = var11.getSpecialType();
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

                  levelTransitionState = var42;
                  break;
               }
            }
         }

         useKey = false;
      }

      SectorData var43;
      for(var21 = 0; var21 < doorControllers.size(); ++var21) {
         DoorController var23;
         if ((var23 = (DoorController) doorControllers.elementAt(var21)).controlledSector == currentSector && var23.doorState == 2) {
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

      for(var21 = 0; var21 < elevatorControllers.size(); ++var21) {
         ElevatorController var24;
         short var25;
         short var45;
         switch((var24 = (ElevatorController) elevatorControllers.elementAt(var21)).elevatorState) {
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

      if (LevelLoader.gameWorld.updateProjectiles()) {
         return true;
      } else {
         GameObject[] var26 = LevelLoader.gameWorld.staticObjects;

         for(var3 = 0; var3 < var26.length; ++var3) {
            GameObject var27;
            if ((var27 = var26[var3]) != null && var27.aiState != -1) {
               Transform3D var28;
               int var31;
               if (var27.aiState == 0) {
                  var28 = var27.transform;
                  if (LevelLoader.gameWorld.getSectorDataAtPoint(var28.x, var28.z).isSectorVisible(currentSector)) {
                     if ((var7 = var28.x - player.x) < 0) {
                        var7 = -var7;
                     }

                     if ((var31 = var28.z - player.z) < 0) {
                        var31 = -var31;
                     }

                     if (var7 + var31 <= 67108864 && LevelLoader.gameWorld.checkLineOfSight(player, var28)) {
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
                     var27.stateTimer = (random.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.enemyReactionTime[difficultyLevel];
                     var27.currentState = 0;
                     break;
                  case 2:
                     if (((var6 = random.nextInt() & Integer.MAX_VALUE) & 1) == 0) {
                        var27.aiState = 3;
                        var27.stateTimer = var6 % MainGameCanvas.var_180b[difficultyLevel] + MainGameCanvas.var_17b5[difficultyLevel];
                        var46 = var27;
                        var10001 = 2;
                     } else {
                        var27.aiState = 1;
                        var27.stateTimer = (random.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_1851[difficultyLevel];
                        var46 = var27;
                        var10001 = 0;
                     }

                     var46.currentState = var10001;
                     break;
                  case 3:
                     var29 = var27.transform;
                     var33 = LevelLoader.gameWorld.getSectorDataAtPoint(var29.x, var29.z);
                     if (LevelLoader.gameWorld.checkLineOfSight(player, var29)) {
                        var27.aiState = 4;
                        var27.stateTimer = 2;
                        if (var5 != 3002) {
                           var27.currentState = 3;
                        }

                        if (var5 == 3001) {
                           MainGameCanvas.playSound(4, false, 100, 1);
                           LevelLoader.gameWorld.shootProjectile(var29, var33);
                        } else if (var5 == 3002) {
                           MainGameCanvas.playSound(5, false, 80, 1);
                           LevelLoader.gameWorld.shootSpreadWeapon(var29, var33);
                        } else {
                           label320: {
                              var31 = 0;
                              int[] var48;
                              switch(var5) {
                              case 3003:
                                 MainGameCanvas.playSound(2, false, 80, 0);
                                 var48 = MainGameCanvas.enemyDamageEasy;
                                 break;
                              case 3004:
                                 MainGameCanvas.playSound(2, false, 80, 0);
                                 var48 = MainGameCanvas.enemyDamageNormal;
                                 break;
                              case 3005:
                                 MainGameCanvas.playSound(2, false, 80, 0);
                                 var48 = MainGameCanvas.enemyDamageHard;
                                 break;
                              case 3006:
                                 MainGameCanvas.playSound(3, false, 80, 0);
                                 var48 = MainGameCanvas.var_12d2;
                                 break;
                              default:
                                 break label320;
                              }

                              var31 = var48[difficultyLevel];
                           }

                           if (var31 > 0) {
                              MainGameCanvas.vibrateDevice(var31 * 10);
                           }

                           if (applyDamage(var31)) {
                              return true;
                           }
                        }
                     } else {
                        var27.aiState = 2;
                        var27.stateTimer = (random.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.enemyReactionTime[difficultyLevel];
                        var27.currentState = 0;
                     }
                     break;
                  case 4:
                     var27.aiState = 2;
                     var27.stateTimer = (random.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.enemyReactionTime[difficultyLevel];
                     var27.currentState = 0;
                     break;
                  case 5:
                     var6 = random.nextInt() & Integer.MAX_VALUE;
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
                     LevelLoader.gameWorld.spawnPickUp(var27);
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
                  var33 = LevelLoader.gameWorld.getSectorDataAtPoint(var29.x, var29.z);
                  var31 = var29.x - player.x;
                  int var34 = var29.z - player.z;
                  int var36;
                  if ((var35 = MathUtils.fastHypot(var31, var34)) > enemyAggroDistance) {
                     var36 = MathUtils.fixedPointMultiply(MathUtils.preciseDivide(var31, var35), var27.getMovementSpeed());
                     var12 = MathUtils.fixedPointMultiply(MathUtils.preciseDivide(var34, var35), var27.getMovementSpeed());
                     int var37;
                     if ((random.nextInt() & Integer.MAX_VALUE) % MainGameCanvas.var_1ad2[difficultyLevel] == 0) {
                        int var47;
                        if ((random.nextInt() & 1) == 0) {
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
                        if (!LevelLoader.gameWorld.checkCollision(var27, tempTransform, var33)) {
                           break;
                        }

                        var29.x = tempTransform.x;
                        var29.z = tempTransform.z;
                     }
                  } else {
                     var36 = random.nextInt() & Integer.MAX_VALUE;
                     var27.aiState = 3;
                     var27.stateTimer = var36 % MainGameCanvas.var_180b[difficultyLevel] + MainGameCanvas.var_17b5[difficultyLevel];
                     var27.currentState = 2;
                  }
               }
            }
         }

         if (currentSector.getSectorType() == 555) {
            MainGameCanvas.vibrateDevice(10);
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
      for(int var1 = 0; var1 < doorControllers.size(); ++var1) {
         DoorController var2;
         if ((var2 = (DoorController) doorControllers.elementAt(var1)).controlledSector == var0) {
            return var2;
         }
      }

      DoorController var3;
      (var3 = new DoorController()).controlledSector = var0;
      doorControllers.addElement(var3);
      return var3;
   }

   private static ElevatorController getElevatorController(SectorData var0) {
      for(int var1 = 0; var1 < elevatorControllers.size(); ++var1) {
         ElevatorController var2;
         if ((var2 = (ElevatorController) elevatorControllers.elementAt(var1)).controlledSector == var0) {
            return var2;
         }
      }

      ElevatorController var6;
      (var6 = new ElevatorController()).elevatorState = 0;
      var6.minHeight = 32767;
      var6.maxHeight = -32768;
      WallDefinition[] var7 = LevelLoader.gameWorld.wallDefinitions;

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
      elevatorControllers.addElement(var6);
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
      inputBack = false;
      useKey = false;
      toggleMapInput = false;
      selectNextWeapon = false;
      levelTransitionState = 0;
      weaponCooldownTimer = 0;
   }

   private static void setSkyboxTexture(Texture var0) {
      skyboxTexture = var0;
   }

   private static void drawSprite(Texture var0, int var1, int var2, int var3, int var4, int var5, int var6) {
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
            if ((var16 = gunFireLighting && var16 < 3 ? var1 + (4 >> var16) : var1 - var16) < 0) {
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
            drawSpriteColumn(var0.getPixelRow(var14), var14 & 1, var17, var19 + var2, var3, var18, 0, var11);
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
         playerViewHeight = Integer.MIN_VALUE;
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
                  if ((var60 = gunFireLighting && var60 < 3 ? var45 + (4 >> var60) : var45 - var60) < 0) {
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
                  updateCeilingSpan(var42, var52, var64 + 1, var25);
                  if (var64 < var29[var52]) {
                     var29[var52] = (short)var64;
                  }
               }

               if (var43 == null) {
                  drawSkyboxColumn(var52, 0, var61, var23);
               }

               label105: {
                  int var10001;
                  int var10002;
                  short[] var66;
                  if (var35 < var37) {
                     drawWallTextureColumn(var1.getPixelRowFast(var65), var65 & 1, var1.colorPalettes[var60], var52, var61, var62, var17, var18, var30);
                     if (var43 != null) {
                        updateFloorSpan(var42, var52, var61, var24);
                     }

                     if (var62 <= var28[var52] || var1 == LevelLoader.defaultErrorTexture) {
                        break label105;
                     }

                     var66 = var28;
                     var10001 = var52;
                     var10002 = var62;
                  } else {
                     if (var43 == null) {
                        break label105;
                     }

                     updateFloorSpan(var42, var52, var61, var24);
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
                  drawSkyboxColumn(var52, var64 + 1, 287, var23);
               }

               if (var39 < var41) {
                  drawWallTextureColumn(var2.getPixelRowFast(var65), var65 & 1, var2.colorPalettes[var60], var52, var63, var64, var19, var20, var31);
                  if (var44 != null) {
                     updateCeilingSpan(var42, var52, var64 + 1, var25);
                  }

                  if (var63 < var29[var52] && var2 != LevelLoader.defaultErrorTexture) {
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
               renderUtils.addRenderSpan(depthBuffer[var68], var67, var42, var68);
            }
         }

         if (playerViewHeight >= 0) {
            var67 = (short)var_a27;

            for(var68 = playerViewHeight; var68 <= var_9b4; ++var68) {
               renderUtils.addRenderSpan(depthBuffer[var68], var67, var42, var68);
            }
         }

      }
   }

   public static void drawFlatSurface(int var0, int var1, int var2, byte[] var3, int[][] var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      int var11;
      int var14;
      int var15;
      label31: {
         var11 = var2 * 240;
         int var12;
         int var13 = (var12 = var2 - 144) < 0 ? -reciprocalTable[-var12] : reciprocalTable[var12];
         var15 = (var14 = var9 * var13 >> 8) >> 14;
         byte var10000;
         if ((var15 = gunFireLighting && var15 < 3 ? var5 + (4 >> var15) : var5 - var15) < 0) {
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
      int var17 = angleCorrectionTable[var0];
      int var18 = angleCorrectionTable[var1];
      int var19 = (var6 + (var7 * var17 >> 14)) * var14 - var8 >> 6;
      int var20 = (var7 - (var6 * var17 >> 14)) * var14 - var10;
      int var21 = (var18 - var17) * reciprocalTable[var1 - var0 + 1] >> 16;
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

   private static void updateCeilingSpan(short var0, int var1, int var2, int var3) {
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
               renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9fa; ++var14) {
               renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }
         } else {
            if (var_9e4 >= 0) {
               var9 = (short)var_a34;

               for(var10 = var_9e4; var10 <= var_9fa; ++var10) {
                  renderUtils.addRenderSpan(depthBuffer[var10], var9, var0, var10);
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

   private static void updateFloorSpan(short var0, int var1, int var2, int var3) {
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
            int var11 = var7 < playerViewHeight - 1 ? var7 : playerViewHeight - 1;
            int var12 = playerViewHeight > var7 + 1 ? playerViewHeight : var7 + 1;
            int var13 = var_9b4 < var4 - 1 ? var_9b4 : var4 - 1;

            int var14;
            for(var14 = var4; var14 <= var11; ++var14) {
               depthBuffer[var14] = var8;
            }

            for(var14 = var10; var14 <= var7; ++var14) {
               depthBuffer[var14] = var8;
            }

            for(var14 = playerViewHeight; var14 <= var13; ++var14) {
               renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }

            for(var14 = var12; var14 <= var_9b4; ++var14) {
               renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
            }
         } else {
            if (playerViewHeight >= 0) {
               var9 = (short)var_a27;

               for(var10 = playerViewHeight; var10 <= var_9b4; ++var10) {
                  renderUtils.addRenderSpan(depthBuffer[var10], var9, var0, var10);
               }
            }

            for(int var15 = var4; var15 <= var7; ++var15) {
               depthBuffer[var15] = var8;
            }
         }

         var_a27 = var1;
         playerViewHeight = var4;
         var_9b4 = var7;
      }
   }

   private static void drawSpriteColumn(byte[] var0, int var1, int[] var2, int var3, int var4, int var5, int var6, int var7) {
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
            int var14 = (var14 = var5 - var4) > 288 ? (var7 << 16) / var14 : var7 * reciprocalTable[var14];
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

   private static void drawWallTextureColumn(byte[] var0, int var1, int[] var2, int var3, int var4, int var5, int var6, int var7, int var8) {
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
         int var15 = (var15 = var5 - var4) > 288 ? (var7 - 1 << 16) / var15 : (var7 - 1) * reciprocalTable[var15];
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

   private static void drawSkyboxColumn(int var0, int var1, int var2, int var3) {
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
      int var9 = MathUtils.fastCos(var8 = MathUtils.fixedPointMultiply(var0 - 120 << 16, skyboxAngleFactor));
      int var10 = MathUtils.fixedPointMultiply(var0 - 120, skyboxScaleX);
      int var11 = MathUtils.fastSin(var8);
      int var13 = MathUtils.fixedPointMultiply(MathUtils.fixedPointMultiply(102943, var11 + MathUtils.fixedPointMultiply(var9, var10)) + var3, skyboxOffsetFactor) >> 8;
      byte[] var14 = skyboxTexture.getPixelRowFast(var13);
      int[] var15 = skyboxTexture.colorPalettes[8];
      int var16 = var6 * 240 + var0;
      int var17 = var7 * 240 + var0;
      int var18;
      int var19 = -(var18 = MathUtils.fixedPointMultiply(var9 * 200, skyboxScaleY)) * (144 - var6) + 6553600;
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

   public static int cycleWeaponForward(int var0) {
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

   public static int findNextAvailableWeapon(int var0) {
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

      damageFlash = true;
      playerHealth -= var0;
      if (playerHealth <= 0) {
         playerHealth = 0;
         return true;
      } else {
         return false;
      }
   }

    public static void resetPlayerProgress() {
      playerHealth = 100;
      playerArmor = 0;

      for(int var0 = 0; var0 < weaponsAvailable.length; ++var0) {
         weaponsAvailable[var0] = false;
         ammoCounts[var0] = 0;
      }

      weaponsAvailable[0] = true;
      weaponsAvailable[6] = true;
      currentWeapon = 0;
      pendingWeaponSwitch = 0;
      messageText = "";
      messageTimer = 0;
      interactionTimer = 0;
      activeInteractable = null;
      damageFlash = false;
      screenShake = 0;
      cameraBobTimer = 0;
      lastGameLogicTime = 0;
      weaponSwitchAnimationActive = true;
      weaponAnimationState = 1;
      MainGameCanvas.gameProgressFlags = 0;
      LevelLoader.levelVariant = 0;
   }
}
