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

    public static void initializeEngine() {
      MainGameCanvas.freeMemory();
      LevelLoader.initResourceArrays();
      player = new PhysicsBody(0, 1572864, 0, 65536);
      tempTransform = new Transform3D(0, 0, 0, 0);
      PortalRenderer.floorClipHistory = new Vector();
      PortalRenderer.ceilingClipHistory = new Vector();
      doorControllers = new Vector();
      elevatorControllers = new Vector();
      PortalRenderer.visibleGameObjects = new GameObject[64];
      PortalRenderer.visibleObjectsCount = 0;
      BSPNode.visibleSectorsCount = 0;
      LevelLoader.defaultErrorTexture = new Texture((byte)0, 8, 8, 0, 0, new int[]{16777215, 16711680});
      byte[] var0 = new byte[]{17, 17, 17, 17, 17, 17, 17, 17};
      byte[] var1 = new byte[]{17, 16, 16, 16, 16, 17, 16, 17};
      byte[] var2 = new byte[]{17, 1, 1, 1, 1, 17, 1, 17};
      LevelLoader.defaultErrorTexture.setPixelData(0, var0);
      LevelLoader.defaultErrorTexture.setPixelData(2, var1);
      LevelLoader.defaultErrorTexture.setPixelData(4, var2);
      LevelLoader.defaultErrorTexture.setPixelData(6, var0);
      PortalRenderer.screenBuffer = new int[69120];
      PortalRenderer.depthBuffer = new short[288];
      PortalRenderer.renderUtils = new RenderUtils();
      PortalRenderer.angleCorrectionTable = new int[240];

      for(int var3 = 0; var3 < 240; ++var3) {
         PortalRenderer.angleCorrectionTable[var3] = MathUtils.fixedPointDivide(var3 - 120 << 16, 7864320) >> 2;
      }

      PortalRenderer.reciprocalTable = new int[289];
      PortalRenderer.reciprocalTable[0] = 0;

      for(int var4 = 1; var4 < 289; ++var4) {
         PortalRenderer.reciprocalTable[var4] = 65536 / var4;
      }

      PortalRenderer.skyboxScaleX = MathUtils.fixedPointDivide(65536, 17301600);
      PortalRenderer.skyboxAngleFactor = MathUtils.fixedPointMultiply(MathUtils.fixedPointDivide(65536, 15794176), 102943);
      PortalRenderer.skyboxScaleY = MathUtils.fixedPointDivide(65536, 18874368);
      PortalRenderer.skyboxOffsetFactor = MathUtils.fixedPointDivide(65536, 411775);
      MainGameCanvas.freeMemory();
   }

   public static void resetLevelState() {
      clearInputState();
      LevelLoader.gameWorld.initializeWorld();
      player.copyFrom(LevelLoader.gameWorld.worldOrigin);
      currentSector = LevelLoader.gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);
      doorControllers.removeAllElements();
      elevatorControllers.removeAllElements();
      PortalRenderer.visibleObjectsCount = 0;
      BSPNode.visibleSectorsCount = 0;
      messageText = "";
      messageTimer = 0;
      interactionTimer = 0;
      activeInteractable = null;
   }

   public static void handleWeaponChange(byte var0) {
      PortalRenderer.setSkyboxTexture(LevelLoader.getTexture(var0));
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
      PortalRenderer.renderWorld(player.x, -cameraHeight, player.z, player.rotation);
      if (damageFlash) {
         int var6 = 69120;

         for(int var7 = 0; var7 < var6; ++var7) {
            int[] var10000 = PortalRenderer.screenBuffer;
            var10000[var7] |= 16711680;
         }

         damageFlash = false;
      }

      if (screenShake == 16) {
         --screenShake;
      }

      var0.drawRGB(PortalRenderer.screenBuffer, 0, 240, 0, 0, 240, 288, false);
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

      if ((SaveSystem.gameProgressFlags & 1) == 0 && MainGameCanvas.currentLevelId == 0 && currentSector.sectorId == 31) {
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
                  if ((SaveSystem.gameProgressFlags & 1) == 0) {
                     SaveSystem.gameProgressFlags = (byte)(SaveSystem.gameProgressFlags | 1);
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
      SaveSystem.gameProgressFlags = 0;
      LevelLoader.levelVariant = 0;
   }
}
