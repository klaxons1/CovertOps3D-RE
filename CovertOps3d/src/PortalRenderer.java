import java.util.Vector;

public class PortalRenderer {
    public static int[] screenBuffer;
    public static Vector floorClipHistory;
    public static Vector ceilingClipHistory;
    public static boolean gunFireLighting = false;
    public static int skyboxScaleX;
    public static int skyboxAngleFactor;
    public static int skyboxScaleY;
    public static int skyboxOffsetFactor;
    public static GameObject[] visibleGameObjects;
    public static int visibleObjectsCount;
    private static Point2D clippedWallStart = new Point2D(0, 0);
    private static Point2D clippedWallEnd = new Point2D(0, 0);
    private static int projectedCeilingStart;
    private static int projectedFloorStart;
    private static int projectedCeilingEnd;
    private static int projectedFloorEnd;
    private static int clippedTextureStartU;
    private static int clippedTextureEndU;
    private static Texture skyboxTexture;
    static short[] depthBuffer;
    private static int floorSpanStart;
    private static int floorSpanEnd;
    private static int ceilingSpanStart;
    private static int ceilingSpanEnd;
    private static int lastFloorColumnX;
    private static int lastCeilingColumnX;
    static RenderUtils renderUtils;
    static int[] angleCorrectionTable;
    static int[] reciprocalTable;

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

    static void renderWorld(int var0, int var1, int var2, int var3) {
     LevelLoader.gameWorld.toggleProjectileSprites();
     Point2D[] var4 = LevelLoader.gameWorld.transformVertices(var0, var2, var3);
     LevelLoader.gameWorld.updateWorld();
     BSPNode.visibleSectorsCount = 0;
     LevelLoader.gameWorld.getRootBSPNode().traverseBSP(GameEngine.player, LevelLoader.gameWorld.getSectorDataAtPoint(var0, var2));
     int var5 = var2 << 8;
     int var6 = var0 << 8;
     int var7 = var3 << 1;
     int var8 = MathUtils.fastSin(var3);
     int var9 = MathUtils.fastCos(var3);
     gunFireLighting = MainGameCanvas.weaponSpriteFrame == 1 && GameEngine.currentWeapon != 0;
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

    static void setSkyboxTexture(Texture var0) {
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
          ceilingSpanEnd = Integer.MIN_VALUE;
          ceilingSpanStart = Integer.MIN_VALUE;
          floorSpanEnd = Integer.MIN_VALUE;
          floorSpanStart = Integer.MIN_VALUE;
          lastCeilingColumnX = Integer.MIN_VALUE;
          lastFloorColumnX = Integer.MIN_VALUE;
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
          if (ceilingSpanStart >= 0) {
             var67 = (short) lastCeilingColumnX;

             for(var68 = ceilingSpanStart; var68 <= ceilingSpanEnd; ++var68) {
                renderUtils.addRenderSpan(depthBuffer[var68], var67, var42, var68);
             }
          }

          if (floorSpanStart >= 0) {
             var67 = (short) lastFloorColumnX;

             for(var68 = floorSpanStart; var68 <= floorSpanEnd; ++var68) {
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
          if (lastCeilingColumnX == var1 - 1) {
             var9 = (short) lastCeilingColumnX;
             var10 = var6 > ceilingSpanEnd + 1 ? var6 : ceilingSpanEnd + 1;
             int var11 = var5 < ceilingSpanStart - 1 ? var5 : ceilingSpanStart - 1;
             int var12 = ceilingSpanStart > var5 + 1 ? ceilingSpanStart : var5 + 1;
             int var13 = ceilingSpanEnd < var6 - 1 ? ceilingSpanEnd : var6 - 1;

             int var14;
             for(var14 = var6; var14 <= var11; ++var14) {
                depthBuffer[var14] = var8;
             }

             for(var14 = var10; var14 <= var7; ++var14) {
                depthBuffer[var14] = var8;
             }

             for(var14 = ceilingSpanStart; var14 <= var13; ++var14) {
                renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
             }

             for(var14 = var12; var14 <= ceilingSpanEnd; ++var14) {
                renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
             }
          } else {
             if (ceilingSpanStart >= 0) {
                var9 = (short) lastCeilingColumnX;

                for(var10 = ceilingSpanStart; var10 <= ceilingSpanEnd; ++var10) {
                   renderUtils.addRenderSpan(depthBuffer[var10], var9, var0, var10);
                }
             }

             for(int var15 = var6; var15 <= var7; ++var15) {
                depthBuffer[var15] = var8;
             }
          }

          lastCeilingColumnX = var1;
          ceilingSpanStart = var6;
          ceilingSpanEnd = var7;
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
          if (lastFloorColumnX == var1 - 1) {
             var9 = (short) lastFloorColumnX;
             var10 = var4 > floorSpanEnd + 1 ? var4 : floorSpanEnd + 1;
             int var11 = var7 < floorSpanStart - 1 ? var7 : floorSpanStart - 1;
             int var12 = floorSpanStart > var7 + 1 ? floorSpanStart : var7 + 1;
             int var13 = floorSpanEnd < var4 - 1 ? floorSpanEnd : var4 - 1;

             int var14;
             for(var14 = var4; var14 <= var11; ++var14) {
                depthBuffer[var14] = var8;
             }

             for(var14 = var10; var14 <= var7; ++var14) {
                depthBuffer[var14] = var8;
             }

             for(var14 = floorSpanStart; var14 <= var13; ++var14) {
                renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
             }

             for(var14 = var12; var14 <= floorSpanEnd; ++var14) {
                renderUtils.addRenderSpan(depthBuffer[var14], var9, var0, var14);
             }
          } else {
             if (floorSpanStart >= 0) {
                var9 = (short) lastFloorColumnX;

                for(var10 = floorSpanStart; var10 <= floorSpanEnd; ++var10) {
                   renderUtils.addRenderSpan(depthBuffer[var10], var9, var0, var10);
                }
             }

             for(int var15 = var4; var15 <= var7; ++var15) {
                depthBuffer[var15] = var8;
             }
          }

          lastFloorColumnX = var1;
          floorSpanStart = var4;
          floorSpanEnd = var7;
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
}
