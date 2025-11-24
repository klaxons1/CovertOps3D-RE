import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public final class GameWorld {
   public static int MIN_WALL_HEIGHT = 16;
   public static int MIN_CEILING_CLEARANCE = 50;
   public static int PLAYER_HEIGHT_OFFSET = 40;
   private Point2D collisionTestPoint = new Point2D(0, 0);
   private int lastWallIndex;
   public Point2D[] vertices;
   private Point2D[] transformedVerticles;
   public WallDefinition[] wallDefinitions;
   public GameObject[] staticObjects;
   private Vector projectiles;
   private Vector pickupItems;
   public SectorData[] sectors;
   public WallSurface[] wallSurfaces;
   public Transform3D worldOrigin;
   public BSPNode[] bspNodes;
   public Sector[] bspSectors;
   public WallSegment[] wallSegments;

   public GameWorld() {
      new Point2D(0, 0);
      this.projectiles = new Vector();
      this.pickupItems = new Vector();
      this.lastWallIndex = -1;
   }

   public final BSPNode getRootBSPNode() {
      return this.bspNodes[this.bspNodes.length - 1];
   }

   public final void setVertices(Point2D[] var1) {
      this.vertices = var1;
      this.transformedVerticles = new Point2D[var1.length];

      for(int var2 = 0; var2 < this.transformedVerticles.length; ++var2) {
         this.transformedVerticles[var2] = new Point2D(0, 0);
      }

   }

   public final Point2D[] transformVertices(int var1, int var2, int var3) {
      long var4 = (long) MathUtils.fastSin(var3);
      long var6 = (long) MathUtils.fastCos(var3);

      for(int var8 = 0; var8 < this.vertices.length; ++var8) {
         int var9 = this.vertices[var8].x - var1;
         int var10 = this.vertices[var8].y - var2;
         this.transformedVerticles[var8].x = (int)(var6 * (long)var9 - var4 * (long)var10 >> 16);
         this.transformedVerticles[var8].y = (int)(var4 * (long)var9 + var6 * (long)var10 >> 16);
      }

      return this.transformedVerticles;
   }

   public final Sector getSectorAtPoint(int var1, int var2) {
      return this.getRootBSPNode().findSectorNodeAtPoint(var1, var2);
   }

   public final SectorData getSectorDataAtPoint(int var1, int var2) {
      return this.getRootBSPNode().findSectorAtPoint(var1, var2);
   }

   public final void initializeWorld() {
      int var1;
      for(var1 = 0; var1 < this.bspNodes.length; ++var1) {
         this.bspNodes[var1].initializeBSPNode(this);
      }

      for(var1 = 0; var1 < this.wallDefinitions.length; ++var1) {
         this.wallDefinitions[var1].initializeWall(this);
      }

      for(var1 = 0; var1 < this.wallSurfaces.length; ++var1) {
         this.wallSurfaces[var1].resolveSectorLink(this);
      }

      for(var1 = 0; var1 < this.wallSegments.length; ++var1) {
         this.wallSegments[var1].initializeWallSegment(this);
      }

      for(var1 = 0; var1 < this.bspSectors.length; ++var1) {
         this.bspSectors[var1].initializeWalls(this);
      }

      this.updateWorld();
      this.getRootBSPNode().calculateVisibleSectors();
   }

   public final void updateWorld() {
      int var1;
      for(var1 = 0; var1 < this.bspSectors.length; ++var1) {
         this.bspSectors[var1].clearDynamicObjects();
      }

      for(var1 = 0; var1 < this.staticObjects.length; ++var1) {
         GameObject var2;
         if ((var2 = this.staticObjects[var1]) != null) {
            var2.addToWorld(this);
         }
      }

      for(var1 = 0; var1 < this.projectiles.size(); ++var1) {
         ((GameObject)this.projectiles.elementAt(var1)).addToWorld(this);
      }

      for(var1 = 0; var1 < this.pickupItems.size(); ++var1) {
         ((GameObject)this.pickupItems.elementAt(var1)).addToWorld(this);
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

   public final boolean checkCollision(GameObject var1, Transform3D var2, SectorData var3) {
      this.collisionTestPoint.x = var2.x;
      this.collisionTestPoint.y = var2.z;

      int var4;
      for(var4 = 0; var4 < this.wallDefinitions.length; ++var4) {
         WallDefinition var5;
         if ((var5 = this.wallDefinitions[var4]).isCollidable() || sub_24b(var3, var5)) {
            this.sub_2d4(var5);
         }
      }

      var4 = this.collisionTestPoint.x - 655360;
      int var15 = this.collisionTestPoint.x + 655360;
      int var6 = this.collisionTestPoint.y - 655360;
      int var7 = this.collisionTestPoint.y + 655360;

      for(int var8 = 0; var8 < this.staticObjects.length; ++var8) {
         GameObject var9;
         if ((var9 = this.staticObjects[var8]) != null && var9 != var1 && var9.aiState != -1) {
            Transform3D var10;
            int var11 = (var10 = var9.transform).x - 655360;
            int var12 = var10.x + 655360;
            int var13 = var10.z - 655360;
            int var14 = var10.z + 655360;
            if (var4 <= var12 && var15 >= var11 && var6 <= var14 && var7 >= var13) {
               switch(var9.objectType) {
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

      var2.x = this.collisionTestPoint.x;
      var2.z = this.collisionTestPoint.y;
      return true;
   }

   public final WallDefinition handlePlayerMovement(PhysicsBody var1, SectorData var2) {
      WallDefinition var5 = null;
      this.collisionTestPoint.x = var1.x;
      this.collisionTestPoint.y = var1.z;
      int var6 = -1;
      WallDefinition var7;
      if (this.lastWallIndex != -1 && ((var7 = this.wallDefinitions[this.lastWallIndex]).isPassable() || sub_24b(var2, var7)) && this.sub_2d4(var7)) {
         var6 = this.lastWallIndex;
         var5 = var7;
      }

      int var16;
      for(var16 = 0; var16 < this.wallDefinitions.length; ++var16) {
         WallDefinition var8;
         if (var16 != this.lastWallIndex && ((var8 = this.wallDefinitions[var16]).isPassable() || sub_24b(var2, var8)) && this.sub_2d4(var8)) {
            if (var6 == -1) {
               var6 = var16;
            }

            if (var5 == null || var5.getWallType() == 0) {
               var5 = var8;
            }
         }
      }

      var16 = 1310720;

      GameObject var9;
      int var12;
      int var13;
      int var14;
      int var17;
      for(var17 = 0; var17 < this.staticObjects.length; ++var17) {
         if ((var9 = this.staticObjects[var17]) != null && var9.aiState != -1) {
            switch(var9.objectType) {
            case 10:
            case 12:
            case 3001:
            case 3002:
            case 3003:
            case 3004:
            case 3005:
            case 3006:
               Transform3D var10 = var9.transform;
               int var11 = this.collisionTestPoint.x - var10.x;
               var12 = this.collisionTestPoint.y - var10.z;
               var13 = var11 < 0 ? -var11 : var11;
               var14 = var12 < 0 ? -var12 : var12;
               if (var13 < var16 && var14 < var16) {
                  if (var9.objectType == 10 && Class_3aa.var_259 == 4) {
                     GameEngine.messageText = GameEngine.ammoCounts[6] > 0 ? "find the wall i told you|and blow it up!" : "go, get the dynamite!";
                     GameEngine.messageTimer = 30;
                  }

                  Point2D var10000;
                  if (var13 > var14) {
                     if (var11 > 0) {
                        var10000 = this.collisionTestPoint;
                        var10000.x += var16 - var13;
                     } else {
                        var10000 = this.collisionTestPoint;
                        var10000.x -= var16 - var13;
                     }
                  } else {
                     int var10001;
                     if (var12 > 0) {
                        var10000 = this.collisionTestPoint;
                        var10001 = var10000.y + (var16 - var14);
                     } else {
                        var10000 = this.collisionTestPoint;
                        var10001 = var10000.y - (var16 - var14);
                     }

                     var10000.y = var10001;
                  }
               }
            }
         }
      }

      var1.x = this.collisionTestPoint.x;
      var1.z = this.collisionTestPoint.y;
      this.lastWallIndex = var6;
      var16 = 1966080;

      int var15;
      int var18;
      Transform3D var19;
      int[] var20;
      for(var17 = 0; var17 < this.pickupItems.size(); ++var17) {
         var18 = (var9 = (GameObject)this.pickupItems.elementAt(var17)).objectType;
         var19 = var9.transform;
         var12 = var1.x - var19.x;
         var13 = var1.z - var19.z;
         var14 = var12 < 0 ? -var12 : var12;
         var15 = var13 < 0 ? -var13 : var13;
         if (var14 < var16 && var15 < var16) {
            switch(var18) {
            case 2004:
               GameEngine.weaponsAvailable[5] = true;
               var20 = GameEngine.ammoCounts;
               var20[5] += Class_3aa.var_1616[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               GameEngine.var_d46 = 5;
               GameEngine.levelComplete = true;
               GameEngine.var_1044 = 8;
               break;
            case 2006:
               GameEngine.weaponsAvailable[7] = true;
               var20 = GameEngine.ammoCounts;
               var20[7] += Class_3aa.var_1677[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               GameEngine.var_d46 = 7;
               GameEngine.levelComplete = true;
               GameEngine.var_1044 = 8;
               Class_3aa.var_295 = Class_3aa.var_259++;
               GameEngine.levelVariant = 0;
               GameEngine.var_480 = 1;
               break;
            case 2007:
               var20 = GameEngine.ammoCounts;
               var20[1] += Class_3aa.var_14be[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               break;
            case 2008:
               var20 = GameEngine.ammoCounts;
               var20[2] += Class_3aa.var_14f5[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               break;
            case 2010:
               var20 = GameEngine.ammoCounts;
               var20[5] += Class_3aa.var_151d[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               break;
            case 2047:
               var20 = GameEngine.ammoCounts;
               var20[7] += Class_3aa.var_156b[GameEngine.difficultyLevel];
               this.pickupItems.removeElementAt(var17--);
               break;
            default:
               continue;
            }

            Class_3aa.sub_84e(1, false, 80, 0);
         }
      }

      for(var17 = 0; var17 < this.staticObjects.length; ++var17) {
         if ((var9 = this.staticObjects[var17]) != null) {
            var18 = var9.objectType;
            var19 = var9.transform;
            var12 = var1.x - var19.x;
            var13 = var1.z - var19.z;
            var14 = var12 < 0 ? -var12 : var12;
            var15 = var13 < 0 ? -var13 : var13;
            if (var14 < var16 && var15 < var16) {
               label173: {
                  switch(var18) {
                  case 5:
                     GameEngine.var_d98[0] = true;
                     this.staticObjects[var17] = null;
                     break label173;
                  case 13:
                     GameEngine.var_d98[1] = true;
                     this.staticObjects[var17] = null;
                     break label173;
                  case 82:
                     GameEngine.weaponsAvailable[8] = true;
                     GameEngine.messageText = "go now to the agent anna";
                     GameEngine.messageTimer = 30;
                     this.staticObjects[var17] = null;
                     break label173;
                  case 2001:
                     GameEngine.weaponsAvailable[1] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[1] += Class_3aa.var_157a[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 1;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2002:
                     if (Class_3aa.var_259 == 3) {
                        GameEngine.messageText = "to change weapon press 3";
                        GameEngine.messageTimer = 30;
                     }

                     GameEngine.weaponsAvailable[2] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[2] += Class_3aa.var_1592[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 2;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2003:
                     GameEngine.weaponsAvailable[3] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[1] += Class_3aa.var_15c4[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 3;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2004:
                     GameEngine.weaponsAvailable[5] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[5] += Class_3aa.var_1616[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 5;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2005:
                     GameEngine.weaponsAvailable[6] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[6] += Class_3aa.var_1630[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 6;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2006:
                     GameEngine.weaponsAvailable[7] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[7] += Class_3aa.var_1677[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 7;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break;
                  case 2007:
                     var20 = GameEngine.ammoCounts;
                     var20[1] += Class_3aa.var_14be[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     break label173;
                  case 2008:
                     var20 = GameEngine.ammoCounts;
                     var20[2] += Class_3aa.var_14f5[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     break label173;
                  case 2010:
                     var20 = GameEngine.ammoCounts;
                     var20[5] += Class_3aa.var_151d[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     break label173;
                  case 2012:
                     if (GameEngine.playerHealth >= 100) {
                        continue;
                     }

                     GameEngine.playerHealth += Class_3aa.var_16e8[GameEngine.difficultyLevel];
                     if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                     }

                     this.staticObjects[var17] = null;
                     break label173;
                  case 2013:
                     this.staticObjects[var17] = null;
                     break;
                  case 2014:
                     if (GameEngine.playerHealth >= 100) {
                        continue;
                     }

                     GameEngine.playerHealth += Class_3aa.var_16c7[GameEngine.difficultyLevel];
                     if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                     }

                     this.staticObjects[var17] = null;
                     break label173;
                  case 2015:
                     if (GameEngine.playerArmor >= 100) {
                        continue;
                     }

                     GameEngine.playerArmor += Class_3aa.var_1731[GameEngine.difficultyLevel];
                     if (GameEngine.playerArmor > 100) {
                        GameEngine.playerArmor = 100;
                     }

                     this.staticObjects[var17] = null;
                     break label173;
                  case 2024:
                     GameEngine.weaponsAvailable[4] = true;
                     var20 = GameEngine.ammoCounts;
                     var20[1] += Class_3aa.var_15d0[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     GameEngine.var_d46 = 4;
                     GameEngine.levelComplete = true;
                     GameEngine.var_1044 = 8;
                     break label173;
                  case 2047:
                     var20 = GameEngine.ammoCounts;
                     var20[7] += Class_3aa.var_156b[GameEngine.difficultyLevel];
                     this.staticObjects[var17] = null;
                     break label173;
                  default:
                     continue;
                  }

                  Class_3aa.var_295 = Class_3aa.var_259++;
                  GameEngine.levelVariant = 0;
                  GameEngine.var_480 = 1;
               }

               Class_3aa.sub_84e(1, false, 80, 0);
            }
         }
      }

      return var5;
   }

   private static boolean sub_24b(SectorData var0, WallDefinition var1) {
      SectorData var2 = var1.backSurface.linkedSector;
      SectorData var3 = var1.frontSurface.linkedSector;
      short var4;
      if (var2 != var0) {
         if (var2.floorHeight - var0.floorHeight > MIN_WALL_HEIGHT) {
            return true;
         }

         var4 = var2.floorHeight;
         if (var0.floorHeight > var4) {
            var4 = var0.floorHeight;
         }

         if (var2.ceilingHeight - var4 < MIN_CEILING_CLEARANCE) {
            return true;
         }
      }

      if (var3 != var0) {
         if (var3.floorHeight - var0.floorHeight > MIN_WALL_HEIGHT) {
            return true;
         }

         var4 = var3.floorHeight;
         if (var0.floorHeight > var4) {
            var4 = var0.floorHeight;
         }

         if (var3.ceilingHeight - var4 < MIN_CEILING_CLEARANCE) {
            return true;
         }
      }

      return false;
   }

   private static boolean sub_289(WallDefinition var0) {
      SectorData var1 = var0.backSurface.linkedSector;
      SectorData var2 = var0.frontSurface.linkedSector;
      if (var1.ceilingHeight - var1.floorHeight <= 0) {
         return true;
      } else if (var2.ceilingHeight - var2.floorHeight <= 0) {
         return true;
      } else if (var1.floorHeight >= var2.ceilingHeight) {
         return true;
      } else {
         return var2.floorHeight >= var1.ceilingHeight;
      }
   }

   private static boolean sub_2bb(int var0, WallDefinition var1) {
      SectorData var2 = var1.backSurface.linkedSector;
      SectorData var3 = var1.frontSurface.linkedSector;
      return var2.ceilingHeight <= var0 || var2.floorHeight >= var0 || var3.ceilingHeight <= var0 || var3.floorHeight >= var0;
   }

   private boolean sub_2d4(WallDefinition var1) {
      Point2D var2 = this.vertices[var1.startVertexId & '\uffff'];
      Point2D var3;
      int var4 = (var3 = this.vertices[var1.endVertexId & '\uffff']).x - var2.x;
      int var5 = var3.y - var2.y;
      int var6 = var2.x + (var4 >> 1);
      int var7 = var2.y + (var5 >> 1);
      int var8 = var4 >= 0 ? var4 >> 1 : -(var4 >> 1);
      int var9 = var5 >= 0 ? var5 >> 1 : -(var5 >> 1);
      int var10 = (var4 = this.collisionTestPoint.x - var6) >= 0 ? var4 : -var4;
      int var11 = var8 + 655360 - var10;
      if (0 < var11) {
         int var12 = (var5 = this.collisionTestPoint.y - var7) >= 0 ? var5 : -var5;
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

            return this.sub_30b(var11, var13, var2, var3, var1.normalVector, var6, var7, var8, var9);
         }
      }

      return false;
   }

   private boolean sub_30b(int var1, int var2, Point2D var3, Point2D var4, Point2D var5, int var6, int var7, int var8, int var9) {
      int var10;
      int var10000;
      if (sub_196(this.collisionTestPoint, var3, var4)) {
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
               var10000 = var7 + var9 - (this.collisionTestPoint.y - 655360);
               break label68;
            }

            var10000 = var7 - var9;
            var10001 = this.collisionTestPoint.y;
         } else {
            if (var10 >= 0) {
               var10000 = var6 + var8 - (this.collisionTestPoint.x - 655360);
               break label68;
            }

            var10000 = var6 - var8;
            var10001 = this.collisionTestPoint.x;
         }

         var10000 = -(var10000 - (var10001 + 655360));
      }

      int var13 = var10000;
      if (0 < var13) {
         if (var10 >= 0) {
            var10000 = this.collisionTestPoint.x - 655360;
            var10001 = var6 + var8;
         } else {
            var10000 = this.collisionTestPoint.x + 655360;
            var10001 = var6 - var8;
         }

         int var14 = var10000 - var10001;
         if (var11 >= 0) {
            var10000 = this.collisionTestPoint.y - 655360;
            var10001 = var7 - var9;
         } else {
            var10000 = this.collisionTestPoint.y + 655360;
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
               var21 = this.collisionTestPoint;
               var21.x += var1;
               var21 = this.collisionTestPoint;
               var21.y += var2;
               return true;
            }

            var21 = this.collisionTestPoint;
            var21.x += var16;
            var21 = this.collisionTestPoint;
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

   public final boolean checkLineOfSight(Transform3D var1, Transform3D var2) {
      for(int var3 = 0; var3 < this.wallDefinitions.length; ++var3) {
         WallDefinition var4;
         if ((var4 = this.wallDefinitions[var3]).isSolid() || sub_289(var4)) {
            Point2D var5 = this.vertices[var4.startVertexId & '\uffff'];
            Point2D var6 = this.vertices[var4.endVertexId & '\uffff'];
            if (sub_365(var2.x, var2.z, var1.x, var1.z, var5.x, var5.y, var6.x, var6.y)) {
               return false;
            }
         }
      }

      return true;
   }

   public final void shootProjectile(Transform3D var1, SectorData var2) {
      int var3 = sub_547(var1.x, var1.z, GameEngine.player.x, GameEngine.player.z);
      int var4 = MathUtils.fastSin(102943 - var3);
      int var5 = MathUtils.fastCos(102943 - var3);
      int var7 = var1.x + 20 * var5;
      int var8 = var1.z + 20 * var4;
      Transform3D var9 = new Transform3D(var7, var1.y + (var2.floorHeight + 40 << 16), var8, var3);
      GameObject var10;
      (var10 = new GameObject(var9, 0, 101, 0)).addSpriteFrame((byte)0, (byte)-46);
      var10.addSpriteFrame((byte)0, (byte)-47);
      var10.currentState = 0;
      this.projectiles.addElement(var10);
   }

   public final void shootSpreadWeapon(Transform3D var1, SectorData var2) {
      int var3 = sub_547(var1.x, var1.z, GameEngine.player.x, GameEngine.player.z);
      int var4 = MathUtils.fastSin(102943 - var3);
      int var5 = MathUtils.fastCos(102943 - var3);
      int var7 = var1.x + 20 * var5;
      int var8 = var1.z + 20 * var4;
      var4 = MathUtils.fastSin(var3);
      var5 = MathUtils.fastCos(var3);
      int var9 = 10 * var5;
      int var10 = -10 * var4;
      Transform3D var11 = new Transform3D(var7 + var9, var1.y + (var2.floorHeight + 40 << 16), var8 + var10, var3);
      GameObject var12;
      (var12 = new GameObject(var11, 0, 102, 0)).addSpriteFrame((byte)0, (byte)-71);
      var12.currentState = 0;
      this.projectiles.addElement(var12);
      var11 = new Transform3D(var7 - var9, var1.y + (var2.floorHeight + 40 << 16), var8 - var10, var3);
      (var12 = new GameObject(var11, 0, 102, 0)).addSpriteFrame((byte)0, (byte)-71);
      var12.currentState = 0;
      this.projectiles.addElement(var12);
   }

   private boolean sub_47e(int var1, int var2, int var3, int var4, int var5) {
      int var6 = var5 >> 16;

      for(int var7 = 0; var7 < this.wallDefinitions.length; ++var7) {
         WallDefinition var8;
         if ((var8 = this.wallDefinitions[var7]).isSolid() || sub_2bb(var6, var8)) {
            Point2D var9 = this.vertices[var8.startVertexId & '\uffff'];
            Point2D var10 = this.vertices[var8.endVertexId & '\uffff'];
            if (sub_365(var1, var2, var3, var4, var9.x, var9.y, var10.x, var10.y)) {
               return true;
            }
         }
      }

      return false;
   }

   public final void fireWeapon() {
      int var1 = GameEngine.player.rotation;
      int var2 = 67108864;
      int var3 = MathUtils.fastSin(102943 - var1);
      int var4 = MathUtils.fastCos(102943 - var1);
      int var5 = GameEngine.currentWeapon != 0 && GameEngine.currentWeapon != 5 && GameEngine.currentWeapon != 7 ? var2 : 1310720;
      int var6 = GameEngine.player.x + MathUtils.fixedPointMultiply(var5, var4);
      int var7 = GameEngine.player.z + MathUtils.fixedPointMultiply(var5, var3);
      if (GameEngine.currentWeapon == 5) {
         Class_3aa.sub_84e(4, false, 100, 2);
         Transform3D var15 = new Transform3D(var6, GameEngine.cameraHeight - 655360, var7, var1);
         if (!this.sub_47e(GameEngine.player.x, GameEngine.player.z, var15.x, var15.z, var15.y)) {
            GameObject var16;
            (var16 = new GameObject(var15, 0, 100, 0)).addSpriteFrame((byte)0, (byte)-44);
            var16.addSpriteFrame((byte)0, (byte)-45);
            var16.currentState = 0;
            this.projectiles.addElement(var16);
         }

      } else {
         int var9;
         if (GameEngine.currentWeapon == 7) {
            Class_3aa.sub_84e(5, false, 100, 2);
            var3 = MathUtils.fastSin(var1);
            var4 = MathUtils.fastCos(var1);
            int var14 = 10 * var4;
            var9 = -10 * var3;
            Transform3D var17 = new Transform3D(var6 - var14, GameEngine.cameraHeight - 655360, var7 - var9, var1);
            GameObject var18;
            if (!this.sub_47e(GameEngine.player.x, GameEngine.player.z, var17.x, var17.z, var17.y)) {
               (var18 = new GameObject(var17, 0, 102, 0)).addSpriteFrame((byte)0, (byte)-71);
               var18.currentState = 0;
               this.projectiles.addElement(var18);
            }

            var17 = new Transform3D(var6 + var14, GameEngine.cameraHeight - 655360, var7 + var9, var1);
            if (!this.sub_47e(GameEngine.player.x, GameEngine.player.z, var17.x, var17.z, var17.y)) {
               (var18 = new GameObject(var17, 0, 102, 0)).addSpriteFrame((byte)0, (byte)-71);
               var18.currentState = 0;
               this.projectiles.addElement(var18);
            }

         } else {
            boolean var8 = false;

            for(var9 = 0; var9 < this.staticObjects.length; ++var9) {
               GameObject var10;
               if ((var10 = this.staticObjects[var9]) != null && var10.aiState != -1) {
                  Transform3D var11 = var10.transform;
                  if (this.checkLineOfSight(GameEngine.player, var11)) {
                     int var12 = 327680;
                     if (sub_37f(GameEngine.player.x, GameEngine.player.z, var6, var7, var11.x, var11.z, var12)) {
                        int var13;
                        label85: {
                           byte var19;
                           label84: {
                              var13 = 0;
                              int[] var10000;
                              switch(GameEngine.currentWeapon) {
                              case 0:
                                 var13 = Class_3aa.var_f4a[GameEngine.difficultyLevel];
                                 break label85;
                              case 1:
                                 var13 = Class_3aa.var_f5c[GameEngine.difficultyLevel];
                                 var19 = 7;
                                 break label84;
                              case 2:
                                 var13 = Class_3aa.var_f76[GameEngine.difficultyLevel];
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

                              var13 = var10000[GameEngine.difficultyLevel];
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
               if (GameEngine.currentWeapon == 1 || GameEngine.currentWeapon == 2) {
                  Class_3aa.sub_84e((GameEngine.var_ea3.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
               }

               if (GameEngine.currentWeapon == 3 || GameEngine.currentWeapon == 4) {
                  Class_3aa.sub_84e((GameEngine.var_ea3.nextInt() & 1) == 0 ? 3 : 8, false, 100, 1);
               }
            }

         }
      }
   }

   private static void sub_4c6(GameObject var0, int var1) {
      var0.health -= var1;
      GameObject var10000;
      byte var10001;
      if (var0.health <= 0) {
         var0.health = 0;
         var0.aiState = 6;
         switch(var0.objectType) {
         case 3001:
         case 3003:
         case 3004:
         case 3005:
         case 3006:
            var0.stateTimer = 5;
            var10000 = var0;
            var10001 = 5;
            break;
         case 3002:
            var0.stateTimer = 5;
            var10000 = var0;
            var10001 = 4;
            break;
         default:
            return;
         }

         var10000.currentState = var10001;
      } else {
         var0.aiState = 5;
         switch(var0.objectType) {
         case 3001:
         case 3003:
         case 3004:
         case 3005:
         case 3006:
            var0.stateTimer = 5;
            var10000 = var0;
            var10001 = 4;
            break;
         case 3002:
            var0.stateTimer = 5;
            var10000 = var0;
            var10001 = 3;
            break;
         default:
            return;
         }

         var10000.currentState = var10001;
      }
   }

   public final void sub_4e6() {
      for(int var1 = 0; var1 < this.projectiles.size(); ++var1) {
         GameObject var2;
         if ((var2 = (GameObject)this.projectiles.elementAt(var1)).objectType == 100 || var2.objectType == 101) {
            var2.currentState ^= 1;
         }
      }

   }

   public final boolean updateProjectiles() {
      for(int var1 = 0; var1 < this.projectiles.size(); ++var1) {
         GameObject var2;
         int var4;
         int var5;
         int var6;
         int var7;
         Transform3D var15;
         GameWorld var10000;
         if ((var2 = (GameObject)this.projectiles.elementAt(var1)).objectType == 103) {
            if (var2.spawnDelay <= 0) {
               continue;
            }

            --var2.spawnDelay;
            if (var2.spawnDelay != 0) {
               continue;
            }

            Class_3aa.sub_84e(4, false, 100, 2);
            int var3;
            if (this.checkLineOfSight(var2.transform, GameEngine.player)) {
               var3 = var2.transform.x - GameEngine.player.x;
               var4 = var2.transform.z - GameEngine.player.z;
               if ((var5 = Class_3aa.var_1113[GameEngine.difficultyLevel] - (MathUtils.fixedPointMultiply(MathUtils.fastHypot(var3, var4), Class_3aa.var_10c4[GameEngine.difficultyLevel]) >> 16)) > 0) {
                  Class_3aa.sub_882(var5 * 10);
                  if (GameEngine.applyDamage(var5)) {
                     return true;
                  }
               }
            }

            for(var3 = 0; var3 < this.staticObjects.length; ++var3) {
               GameObject var16;
               if ((var16 = this.staticObjects[var3]) != null && var16.aiState != -1) {
                  Object var17 = null;
                  if (this.checkLineOfSight(var2.transform, var16.transform)) {
                     var6 = var2.transform.x - var16.transform.x;
                     var7 = var2.transform.z - var16.transform.z;
                     int var8;
                     if ((var8 = Class_3aa.var_1113[GameEngine.difficultyLevel] - (MathUtils.fixedPointMultiply(MathUtils.fastHypot(var6, var7), Class_3aa.var_10c4[GameEngine.difficultyLevel]) >> 16)) > 0) {
                        sub_4c6(var16, var8);
                     }
                  }
               }
            }

            GameEngine.screenShake = 16;
            if (Class_3aa.var_259 == 4) {
               var15 = var2.transform;
               if (this.getSectorDataAtPoint(var15.x, var15.z).getSectorType() == 666) {
                  Class_3aa.var_295 = Class_3aa.var_259++;
                  GameEngine.levelVariant = 0;
                  GameEngine.var_480 = 1;
               }
            }

            var10000 = this;
         } else {
            var4 = (var15 = var2.transform).x;
            var5 = var15.z;
            var15.moveRelative(0, -1048576);
            var6 = var15.x;
            var7 = var15.z;
            boolean var18 = false;
            int var9 = (var2.objectType == 102 ? Class_3aa.var_104c : Class_3aa.var_1071)[GameEngine.difficultyLevel];
            if (sub_37f(var4, var5, var6, var7, GameEngine.player.x, GameEngine.player.z, 655360)) {
               if (var2.objectType == 101) {
                  Class_3aa.sub_84e(4, false, 100, 2);
               }

               Class_3aa.sub_882(var9 * 10);
               if (GameEngine.applyDamage(var9)) {
                  return true;
               }

               var18 = true;
            }

            int var10;
            for(var10 = 0; var10 < this.staticObjects.length; ++var10) {
               GameObject var11;
               if ((var11 = this.staticObjects[var10]) != null && var11.aiState != -1) {
                  Transform3D var12 = var11.transform;
                  int var13 = var2.objectType == 102 ? 655360 : 327680;
                  if (sub_37f(var4, var5, var6, var7, var12.x, var12.z, var13)) {
                     sub_4c6(var11, var9);
                     var18 = true;
                  }
               }
            }

            if (!var18) {
               var10 = var15.y >> 16;

               for(int var19 = 0; var19 < this.wallDefinitions.length; ++var19) {
                  WallDefinition var20;
                  if ((var20 = this.wallDefinitions[var19]).isSolid() || sub_2bb(var10, var20)) {
                     Point2D var21 = this.vertices[var20.startVertexId & '\uffff'];
                     Point2D var14 = this.vertices[var20.endVertexId & '\uffff'];
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

         var10000.projectiles.removeElementAt(var1--);
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

   public final boolean throwGrenade() {
      int var1 = GameEngine.player.rotation;
      int var3 = MathUtils.fastSin(102943 - var1);
      int var4 = MathUtils.fastCos(102943 - var1);
      int var5 = 655360;
      int var6 = GameEngine.player.x + MathUtils.fixedPointMultiply(var5, var4);
      int var7 = GameEngine.player.z + MathUtils.fixedPointMultiply(var5, var3);
      Transform3D var8 = new Transform3D(var6, 0, var7, var1);
      GameObject var9;
      (var9 = new GameObject(var8, 0, 103, 100)).addSpriteFrame((byte)0, (byte)-51);
      var9.currentState = 0;
      this.projectiles.addElement(var9);
      return true;
   }

   public final void spawnPickUp(GameObject var1) {
      GameObject var2;
      label15: {
         var2 = null;
         GameObject var10000;
         byte var10001;
         byte var10002;
         switch(var1.objectType) {
         case 3001:
            var10000 = var2 = new GameObject(var1.transform, 0, 2004, 0);
            var10001 = 0;
            var10002 = -43;
            break;
         case 3002:
            var10000 = var2 = new GameObject(var1.transform, 0, 2006, 0);
            var10001 = 0;
            var10002 = -72;
            break;
         case 3003:
         case 3005:
         case 3006:
            var10000 = var2 = new GameObject(var1.transform, 0, 2007, 0);
            var10001 = 0;
            var10002 = -48;
            break;
         case 3004:
            var10000 = var2 = new GameObject(var1.transform, 0, 2008, 0);
            var10001 = 0;
            var10002 = -54;
            break;
         default:
            break label15;
         }

         var10000.addSpriteFrame(var10001, var10002);
      }

      this.pickupItems.addElement(var2);
   }

   public final void drawDebugInfo(Graphics var1) {
      for(int var2 = 0; var2 < this.wallDefinitions.length; ++var2) {
         WallDefinition var3;
         if (((var3 = this.wallDefinitions[var2]).getWallType() != 0 || !var3.isTransparent()) && var3.isRendered()) {
            int var4 = var3.startVertexId & '\uffff';
            int var5 = var3.endVertexId & '\uffff';
            Point2D var6 = this.transformedVerticles[var4];
            Point2D var7 = this.transformedVerticles[var5];
            Graphics var10000;
            int var10001;
            if (var3.getWallType() != 0) {
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
