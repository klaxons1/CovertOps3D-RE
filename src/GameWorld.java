import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/**
 * Represents the game world containing geometry, objects, and handles physics/collision.
 *
 */
public final class GameWorld {

    // Collision constants (in 16.16 fixed-point format)
    private static final int COLLISION_RADIUS = 655360;           // 10 units
    private static final int OBJECT_COLLISION_RADIUS = 1310720;   // 20 units
    private static final int PICKUP_RADIUS = 1966080;             // 30 units
    private static final int ENEMY_HIT_RADIUS = 327680;           // 5 units
    private static final int PROJECTILE_SPEED = 1048576;          // 16 units per tick

    // Angle constants (game uses custom angle system, not radians)
    private static final int ANGLE_90_DEGREES = 102943;
    private static final int ANGLE_180_DEGREES = 205887;
    private static final int ANGLE_360_DEGREES = 411775;

    // Max weapon range for hitscan weapons
    private static final int MAX_HITSCAN_RANGE = 67108864;        // 1024 units

    // Pickup ammo amounts by difficulty
    public static final int[] AMMO_PANZERFAUST_PICKUP = {3, 3, 3};
    public static final int[] AMMO_SONIC_PICKUP = {3, 3, 3};
    public static final int[] AMMO_LUGER_SMALL = {10, 10, 10};
    public static final int[] AMMO_MAUSER_SMALL = {10, 10, 10};
    public static final int[] AMMO_PANZERFAUST_SMALL = {1, 1, 1};
    public static final int[] AMMO_SONIC_SMALL = {6, 6, 6};
    public static final int[] AMMO_LUGER_WEAPON = {10, 10, 10};
    public static final int[] AMMO_MAUSER_WEAPON = {10, 10, 10};
    public static final int[] AMMO_RIFLE_WEAPON = {20, 20, 20};
    public static final int[] AMMO_STEN_WEAPON = {20, 20, 20};
    public static final int[] AMMO_DYNAMITE_PICKUP = {1, 1, 1};

    // Health and armor pickups
    public static final int[] HEALTH_SMALL = {25, 25, 25};
    public static final int[] HEALTH_LARGE = {50, 50, 50};
    public static final int[] ARMOR_PICKUP = {25, 25, 25};

    // Explosion mechanics
    public static final int[] EXPLOSION_FALLOFF_RATE = {65536, 65536, 65536};
    public static final int[] EXPLOSION_MAX_DAMAGE = {400, 400, 400};

    public static int MIN_WALL_HEIGHT = 16;
    public static int MIN_CEILING_CLEARANCE = 50;
    public static int PLAYER_HEIGHT_OFFSET = 40;

    private Point2D collisionTestPoint = new Point2D(0, 0);
    private int lastWallIndex;

    public Point2D[] vertices;
    private Point2D[] transformedVertices;
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

    public final void setVertices(Point2D[] newVertices) {
        this.vertices = newVertices;
        this.transformedVertices = new Point2D[newVertices.length];

        for (int i = 0; i < this.transformedVertices.length; i++) {
            this.transformedVertices[i] = new Point2D(0, 0);
        }
    }

    /**
     * Transforms all vertices from world space to view space.
     * Uses rotation matrix multiplication with 16.16 fixed-point math.
     */
    public final Point2D[] transformVertices(int originX, int originY, int angle) {
        long sinAngle = (long) MathUtils.fastSin(angle);
        long cosAngle = (long) MathUtils.fastCos(angle);

        for (int i = 0; i < this.vertices.length; i++) {
            int relativeX = this.vertices[i].x - originX;
            int relativeY = this.vertices[i].y - originY;

            this.transformedVertices[i].x = (int)(cosAngle * (long)relativeX - sinAngle * (long)relativeY >> 16);
            this.transformedVertices[i].y = (int)(sinAngle * (long)relativeX + cosAngle * (long)relativeY >> 16);
        }

        return this.transformedVertices;
    }

    public final Sector getSectorAtPoint(int x, int z) {
        return this.getRootBSPNode().findSectorNodeAtPoint(x, z);
    }

    public final SectorData getSectorDataAtPoint(int x, int z) {
        return this.getRootBSPNode().findSectorAtPoint(x, z);
    }

    /**
     * Initializes all world structures after loading.
     */
    public final void initializeWorld() {
        for (int i = 0; i < this.bspNodes.length; i++) {
            this.bspNodes[i].initializeBSPNode(this);
        }

        for (int i = 0; i < this.wallDefinitions.length; i++) {
            this.wallDefinitions[i].initializeWall(this);
        }

        for (int i = 0; i < this.wallSurfaces.length; i++) {
            this.wallSurfaces[i].resolveSectorLink(this);
        }

        for (int i = 0; i < this.wallSegments.length; i++) {
            this.wallSegments[i].initializeWallSegment(this);
        }

        for (int i = 0; i < this.bspSectors.length; i++) {
            this.bspSectors[i].initializeWalls(this);
        }

        this.updateWorld();
        this.getRootBSPNode().calculateVisibleSectors();
    }

    /**
     * Updates spatial partitioning for all dynamic objects.
     */
    public final void updateWorld() {
        for (int i = 0; i < this.bspSectors.length; i++) {
            this.bspSectors[i].clearDynamicObjects();
        }

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject gameObject = this.staticObjects[i];
            if (gameObject != null) {
                gameObject.addToWorld(this);
            }
        }

        for (int i = 0; i < this.projectiles.size(); i++) {
            ((GameObject)this.projectiles.elementAt(i)).addToWorld(this);
        }

        for (int i = 0; i < this.pickupItems.size(); i++) {
            ((GameObject)this.pickupItems.elementAt(i)).addToWorld(this);
        }
    }

    /**
     * Determines which side of a line a point is on using cross product.
     * Used for collision detection and BSP traversal.
     */
    private static boolean isPointOnLeftSideOfLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        if (lineStart.x == lineEnd.x) {
            if (point.x <= lineStart.x) {
                return lineStart.y - lineEnd.y > 0;
            } else {
                return lineStart.y - lineEnd.y < 0;
            }
        }

        if (lineStart.y == lineEnd.y) {
            if (point.y <= lineStart.y) {
                return lineStart.x - lineEnd.x < 0;
            } else {
                return lineStart.x - lineEnd.x > 0;
            }
        }

        int deltaX = point.x - lineStart.x;
        int deltaY = point.y - lineStart.y;
        long crossProduct = (long)(lineStart.y - lineEnd.y) * (long)deltaX;
        return (long)(lineStart.x - lineEnd.x) * (long)deltaY >= crossProduct;
    }

    /**
     * Checks if an entity can move to the proposed position.
     * Returns false if collision with blocking object occurs.
     */
    public final boolean checkCollision(GameObject entity, Transform3D proposedPos, SectorData currentSector) {
        this.collisionTestPoint.x = proposedPos.x;
        this.collisionTestPoint.y = proposedPos.z;

        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];
            if (wall.isCollidable() || isWallPassableForSector(currentSector, wall)) {
                this.resolveWallCollision(wall);
            }
        }

        int entityMinX = this.collisionTestPoint.x - COLLISION_RADIUS;
        int entityMaxX = this.collisionTestPoint.x + COLLISION_RADIUS;
        int entityMinZ = this.collisionTestPoint.y - COLLISION_RADIUS;
        int entityMaxZ = this.collisionTestPoint.y + COLLISION_RADIUS;

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject other = this.staticObjects[i];
            if (other != null && other != entity && other.aiState != -1) {
                Transform3D otherTransform = other.transform;
                int otherMinX = otherTransform.x - COLLISION_RADIUS;
                int otherMaxX = otherTransform.x + COLLISION_RADIUS;
                int otherMinZ = otherTransform.z - COLLISION_RADIUS;
                int otherMaxZ = otherTransform.z + COLLISION_RADIUS;

                if (entityMinX <= otherMaxX && entityMaxX >= otherMinX &&
                        entityMinZ <= otherMaxZ && entityMaxZ >= otherMinZ) {
                    switch (other.objectType) {
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

        proposedPos.x = this.collisionTestPoint.x;
        proposedPos.z = this.collisionTestPoint.y;
        return true;
    }

    /**
     * Handles player movement including collision detection and item pickup.
     */
    public final WallDefinition handlePlayerMovement(PhysicsBody player, SectorData currentSector) {
        WallDefinition touchedWall = null;
        this.collisionTestPoint.x = player.x;
        this.collisionTestPoint.y = player.z;

        int newLastWallIndex = -1;

        if (this.lastWallIndex != -1) {
            WallDefinition lastWall = this.wallDefinitions[this.lastWallIndex];
            if ((lastWall.isPassable() || isWallPassableForSector(currentSector, lastWall))
                    && this.resolveWallCollision(lastWall)) {
                newLastWallIndex = this.lastWallIndex;
                touchedWall = lastWall;
            }
        }

        for (int i = 0; i < this.wallDefinitions.length; i++) {
            if (i == this.lastWallIndex) continue;

            WallDefinition wall = this.wallDefinitions[i];
            if ((wall.isPassable() || isWallPassableForSector(currentSector, wall))
                    && this.resolveWallCollision(wall)) {
                if (newLastWallIndex == -1) {
                    newLastWallIndex = i;
                }
                if (touchedWall == null || touchedWall.getWallType() == 0) {
                    touchedWall = wall;
                }
            }
        }

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject obj = this.staticObjects[i];
            if (obj == null || obj.aiState == -1) continue;

            switch (obj.objectType) {
                case 10:
                case 12:
                case 3001:
                case 3002:
                case 3003:
                case 3004:
                case 3005:
                case 3006:
                    Transform3D objTransform = obj.transform;
                    int deltaX = this.collisionTestPoint.x - objTransform.x;
                    int deltaZ = this.collisionTestPoint.y - objTransform.z;
                    int absDeltaX = deltaX < 0 ? -deltaX : deltaX;
                    int absDeltaZ = deltaZ < 0 ? -deltaZ : deltaZ;

                    if (absDeltaX < OBJECT_COLLISION_RADIUS && absDeltaZ < OBJECT_COLLISION_RADIUS) {
                        if (obj.objectType == 10 && MainGameCanvas.currentLevelId == 4) {
                            GameEngine.messageText = GameEngine.ammoCounts[6] > 0
                                    ? TextStrings.FIND_THE_WALL_I_TOLD_YOU_AND_BLOW_IT_UP
                                    : TextStrings.GO_GET_THE_DYNAMITE;
                            GameEngine.messageTimer = 30;
                        }

                        if (absDeltaX > absDeltaZ) {
                            if (deltaX > 0) {
                                this.collisionTestPoint.x += OBJECT_COLLISION_RADIUS - absDeltaX;
                            } else {
                                this.collisionTestPoint.x -= OBJECT_COLLISION_RADIUS - absDeltaX;
                            }
                        } else {
                            if (deltaZ > 0) {
                                this.collisionTestPoint.y += OBJECT_COLLISION_RADIUS - absDeltaZ;
                            } else {
                                this.collisionTestPoint.y -= OBJECT_COLLISION_RADIUS - absDeltaZ;
                            }
                        }
                    }
                    break;
            }
        }

        player.x = this.collisionTestPoint.x;
        player.z = this.collisionTestPoint.y;
        this.lastWallIndex = newLastWallIndex;

        for (int i = 0; i < this.pickupItems.size(); i++) {
            GameObject pickup = (GameObject) this.pickupItems.elementAt(i);
            int pickupType = pickup.objectType;
            Transform3D pickupTransform = pickup.transform;

            int deltaX = player.x - pickupTransform.x;
            int deltaZ = player.z - pickupTransform.z;
            int absDeltaX = deltaX < 0 ? -deltaX : deltaX;
            int absDeltaZ = deltaZ < 0 ? -deltaZ : deltaZ;

            if (absDeltaX < PICKUP_RADIUS && absDeltaZ < PICKUP_RADIUS) {
                boolean collected = true;

                switch (pickupType) {
                    case 2004:
                        GameEngine.weaponsAvailable[WeaponFactory.PANZERFAUST] = true;
                        GameEngine.ammoCounts[WeaponFactory.PANZERFAUST] += AMMO_PANZERFAUST_PICKUP[GameEngine.difficultyLevel];
                        triggerWeaponSwitch(WeaponFactory.PANZERFAUST);
                        break;

                    case 2006:
                        GameEngine.weaponsAvailable[WeaponFactory.SONIC] = true;
                        GameEngine.ammoCounts[WeaponFactory.SONIC] += AMMO_SONIC_PICKUP[GameEngine.difficultyLevel];
                        triggerWeaponSwitch(WeaponFactory.SONIC);
                        triggerLevelTransition();
                        break;

                    case 2007:
                        GameEngine.ammoCounts[WeaponFactory.LUGER] += AMMO_LUGER_SMALL[GameEngine.difficultyLevel];
                        break;

                    case 2008:
                        GameEngine.ammoCounts[WeaponFactory.MAUSER] += AMMO_MAUSER_SMALL[GameEngine.difficultyLevel];
                        break;

                    case 2010:
                        GameEngine.ammoCounts[WeaponFactory.PANZERFAUST] += AMMO_PANZERFAUST_SMALL[GameEngine.difficultyLevel];
                        break;

                    case 2047:
                        GameEngine.ammoCounts[WeaponFactory.SONIC] += AMMO_SONIC_SMALL[GameEngine.difficultyLevel];
                        break;

                    default:
                        collected = false;
                        break;
                }

                if (collected) {
                    this.pickupItems.removeElementAt(i--);
                    HelperUtils.playSound(1, false, 80, 0);
                }
            }
        }

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject obj = this.staticObjects[i];
            if (obj == null) continue;

            int objType = obj.objectType;
            Transform3D objTransform = obj.transform;

            int deltaX = player.x - objTransform.x;
            int deltaZ = player.z - objTransform.z;
            int absDeltaX = deltaX < 0 ? -deltaX : deltaX;
            int absDeltaZ = deltaZ < 0 ? -deltaZ : deltaZ;

            if (absDeltaX >= PICKUP_RADIUS || absDeltaZ >= PICKUP_RADIUS) continue;

            boolean collected = true;
            boolean triggerTransition = false;

            switch (objType) {
                case 5:
                    GameEngine.keysCollected[0] = true;
                    break;

                case 13:
                    GameEngine.keysCollected[1] = true;
                    break;

                case 82:
                    GameEngine.weaponsAvailable[8] = true;
                    GameEngine.messageText = TextStrings.GO_ANNA;
                    GameEngine.messageTimer = 30;
                    break;

                case 2001:
                    GameEngine.weaponsAvailable[WeaponFactory.LUGER] = true;
                    GameEngine.ammoCounts[WeaponFactory.LUGER] += AMMO_LUGER_WEAPON[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.LUGER);
                    break;

                case 2002:
                    if (MainGameCanvas.currentLevelId == 3) {
                        GameEngine.messageText = TextStrings.TO_CHANGE_WEAPON_PRESS_3;
                        GameEngine.messageTimer = 30;
                    }
                    GameEngine.weaponsAvailable[WeaponFactory.MAUSER] = true;
                    GameEngine.ammoCounts[WeaponFactory.MAUSER] += AMMO_MAUSER_WEAPON[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.MAUSER);
                    break;

                case 2003:
                    GameEngine.weaponsAvailable[WeaponFactory.RIFLE] = true;
                    GameEngine.ammoCounts[WeaponFactory.LUGER] += AMMO_RIFLE_WEAPON[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.RIFLE);
                    break;

                case 2004:
                    GameEngine.weaponsAvailable[WeaponFactory.PANZERFAUST] = true;
                    GameEngine.ammoCounts[WeaponFactory.PANZERFAUST] += AMMO_PANZERFAUST_PICKUP[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.PANZERFAUST);
                    break;

                case 2005:
                    GameEngine.weaponsAvailable[WeaponFactory.DYNAMITE] = true;
                    GameEngine.ammoCounts[WeaponFactory.DYNAMITE] += AMMO_DYNAMITE_PICKUP[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.DYNAMITE);
                    break;

                case 2006:
                    GameEngine.weaponsAvailable[WeaponFactory.SONIC] = true;
                    GameEngine.ammoCounts[WeaponFactory.SONIC] += AMMO_SONIC_PICKUP[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.SONIC);
                    triggerTransition = true;
                    break;

                case 2007:
                    GameEngine.ammoCounts[WeaponFactory.LUGER] += AMMO_LUGER_SMALL[GameEngine.difficultyLevel];
                    break;

                case 2008:
                    GameEngine.ammoCounts[WeaponFactory.MAUSER] += AMMO_MAUSER_SMALL[GameEngine.difficultyLevel];
                    break;

                case 2010:
                    GameEngine.ammoCounts[WeaponFactory.PANZERFAUST] += AMMO_PANZERFAUST_SMALL[GameEngine.difficultyLevel];
                    break;

                case 2012:
                    if (GameEngine.playerHealth >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerHealth += HEALTH_LARGE[GameEngine.difficultyLevel];
                    if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                    }
                    break;

                case 2013:
                    triggerTransition = true;
                    break;

                case 2014:
                    if (GameEngine.playerHealth >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerHealth += HEALTH_SMALL[GameEngine.difficultyLevel];
                    if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                    }
                    break;

                case 2015:
                    if (GameEngine.playerArmor >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerArmor += ARMOR_PICKUP[GameEngine.difficultyLevel];
                    if (GameEngine.playerArmor > 100) {
                        GameEngine.playerArmor = 100;
                    }
                    break;

                case 2024:
                    GameEngine.weaponsAvailable[WeaponFactory.STEN] = true;
                    GameEngine.ammoCounts[WeaponFactory.LUGER] += AMMO_STEN_WEAPON[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(WeaponFactory.STEN);
                    break;

                case 2047:
                    GameEngine.ammoCounts[WeaponFactory.SONIC] += AMMO_SONIC_SMALL[GameEngine.difficultyLevel];
                    break;

                default:
                    collected = false;
                    break;
            }

            if (collected) {
                this.staticObjects[i] = null;
                HelperUtils.playSound(1, false, 80, 0);

                if (triggerTransition) {
                    triggerLevelTransition();
                }
            }
        }

        return touchedWall;
    }

    private static void triggerWeaponSwitch(int weaponId) {
        // Use WeaponManager for weapon switching
        if (MainGameCanvas.weaponManager != null) {
            MainGameCanvas.weaponManager.forceWeaponSwitch(weaponId);
        }

        // Keep GameEngine in sync for compatibility
        GameEngine.pendingWeaponSwitch = weaponId;
        GameEngine.weaponSwitchAnimationActive = true;
        GameEngine.weaponAnimationState = 8;
    }

    private static void triggerLevelTransition() {
        MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId++;
        LevelLoader.levelVariant = 0;
        GameEngine.levelTransitionState = 1;
    }

    /**
     * Checks if a wall should block movement for an entity in the given sector.
     * Considers floor height differences and ceiling clearance.
     */
    private static boolean isWallPassableForSector(SectorData currentSector, WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;

        if (backSector != currentSector) {
            if (backSector.floorHeight - currentSector.floorHeight > MIN_WALL_HEIGHT) {
                return true;
            }
            short maxFloorHeight = backSector.floorHeight;
            if (currentSector.floorHeight > maxFloorHeight) {
                maxFloorHeight = currentSector.floorHeight;
            }
            if (backSector.ceilingHeight - maxFloorHeight < MIN_CEILING_CLEARANCE) {
                return true;
            }
        }

        if (frontSector != currentSector) {
            if (frontSector.floorHeight - currentSector.floorHeight > MIN_WALL_HEIGHT) {
                return true;
            }
            short maxFloorHeight = frontSector.floorHeight;
            if (currentSector.floorHeight > maxFloorHeight) {
                maxFloorHeight = currentSector.floorHeight;
            }
            if (frontSector.ceilingHeight - maxFloorHeight < MIN_CEILING_CLEARANCE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a wall is completely solid (no passage between sectors).
     */
    private static boolean isWallSolid(WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;

        if (backSector.ceilingHeight - backSector.floorHeight <= 0) {
            return true;
        }
        if (frontSector.ceilingHeight - frontSector.floorHeight <= 0) {
            return true;
        }

        if (backSector.floorHeight >= frontSector.ceilingHeight) {
            return true;
        }
        return frontSector.floorHeight >= backSector.ceilingHeight;
    }

    /**
     * Checks if a wall blocks projectiles at the given height.
     */
    private static boolean isWallHeightBlocking(int projectileHeight, WallDefinition wall) {
        SectorData backSector = wall.backSurface.linkedSector;
        SectorData frontSector = wall.frontSurface.linkedSector;

        return backSector.ceilingHeight <= projectileHeight
                || backSector.floorHeight >= projectileHeight
                || frontSector.ceilingHeight <= projectileHeight
                || frontSector.floorHeight >= projectileHeight;
    }

    /**
     * Resolves collision with a wall using AABB collision.
     * Returns true if collision occurred.
     */
    private boolean resolveWallCollision(WallDefinition wall) {
        Point2D startVertex = this.vertices[wall.startVertexId & 0xFFFF];
        Point2D endVertex = this.vertices[wall.endVertexId & 0xFFFF];

        int wallDeltaX = endVertex.x - startVertex.x;
        int wallDeltaY = endVertex.y - startVertex.y;
        int wallCenterX = startVertex.x + (wallDeltaX >> 1);
        int wallCenterY = startVertex.y + (wallDeltaY >> 1);
        int wallHalfExtentX = wallDeltaX >= 0 ? wallDeltaX >> 1 : -(wallDeltaX >> 1);
        int wallHalfExtentY = wallDeltaY >= 0 ? wallDeltaY >> 1 : -(wallDeltaY >> 1);

        int relativeToCenterX = this.collisionTestPoint.x - wallCenterX;
        int relativeToCenterY = this.collisionTestPoint.y - wallCenterY;
        int absRelativeX = relativeToCenterX >= 0 ? relativeToCenterX : -relativeToCenterX;
        int absRelativeY = relativeToCenterY >= 0 ? relativeToCenterY : -relativeToCenterY;

        int overlapX = wallHalfExtentX + COLLISION_RADIUS - absRelativeX;
        if (overlapX <= 0) return false;

        int overlapY = wallHalfExtentY + COLLISION_RADIUS - absRelativeY;
        if (overlapY <= 0) return false;

        int correctionX, correctionY;
        if (overlapX < overlapY) {
            correctionX = relativeToCenterX < 0 ? -overlapX : overlapX;
            correctionY = 0;
        } else {
            correctionX = 0;
            correctionY = relativeToCenterY < 0 ? -overlapY : overlapY;
        }

        return this.adjustCollisionPoint(correctionX, correctionY, startVertex, endVertex,
                wall.normalVector, wallCenterX, wallCenterY, wallHalfExtentX, wallHalfExtentY);
    }

    /**
     * Adjusts collision point based on wall normal and overlap.
     */
    private boolean adjustCollisionPoint(int correctionX, int correctionY,
                                         Point2D wallStart, Point2D wallEnd, Point2D wallNormal,
                                         int wallCenterX, int wallCenterY, int wallHalfExtentX, int wallHalfExtentY) {

        int normalX, normalY;
        if (isPointOnLeftSideOfLine(this.collisionTestPoint, wallStart, wallEnd)) {
            normalX = -wallNormal.x;
            normalY = -wallNormal.y;
        } else {
            normalX = wallNormal.x;
            normalY = wallNormal.y;
        }

        int penetration;
        if (wallHalfExtentX > wallHalfExtentY) {
            if (normalY >= 0) {
                penetration = wallCenterY + wallHalfExtentY - (this.collisionTestPoint.y - COLLISION_RADIUS);
            } else {
                penetration = -((wallCenterY - wallHalfExtentY) - (this.collisionTestPoint.y + COLLISION_RADIUS));
            }
        } else {
            if (normalX >= 0) {
                penetration = wallCenterX + wallHalfExtentX - (this.collisionTestPoint.x - COLLISION_RADIUS);
            } else {
                penetration = -((wallCenterX - wallHalfExtentX) - (this.collisionTestPoint.x + COLLISION_RADIUS));
            }
        }

        if (penetration <= 0) return false;

        int relativeX, relativeY;
        if (normalX >= 0) {
            relativeX = (this.collisionTestPoint.x - COLLISION_RADIUS) - (wallCenterX + wallHalfExtentX);
        } else {
            relativeX = (this.collisionTestPoint.x + COLLISION_RADIUS) - (wallCenterX - wallHalfExtentX);
        }
        if (normalY >= 0) {
            relativeY = (this.collisionTestPoint.y - COLLISION_RADIUS) - (wallCenterY - wallHalfExtentY);
        } else {
            relativeY = (this.collisionTestPoint.y + COLLISION_RADIUS) - (wallCenterY + wallHalfExtentY);
        }

        int dotProduct = (int)((long)relativeX * (long)normalX + (long)relativeY * (long)normalY >> 16);

        if (dotProduct >= 0) return false;

        int pushX = (int)((long)normalX * (long)(-dotProduct) >> 16);
        int pushY = (int)((long)normalY * (long)(-dotProduct) >> 16);
        int pushMagnitude = (pushX >= 0 ? pushX : -pushX) + (pushY >= 0 ? pushY : -pushY);
        int correctionMagnitude = (correctionX >= 0 ? correctionX : -correctionX)
                + (correctionY >= 0 ? correctionY : -correctionY);

        if (correctionMagnitude < pushMagnitude) {
            this.collisionTestPoint.x += correctionX;
            this.collisionTestPoint.y += correctionY;
        } else {
            this.collisionTestPoint.x += pushX;
            this.collisionTestPoint.y += pushY;
        }

        return true;
    }

    /**
     * Tests if two line segments intersect using parametric form.
     */
    public static boolean doLineSegmentsIntersect(int x1, int y1, int x2, int y2,
                                                  int x3, int y3, int x4, int y4) {
        long denominator = (long)(y4 - y3) * (long)(x2 - x1) - (long)(x4 - x3) * (long)(y2 - y1);

        if (denominator == 0L) {
            return false;
        }

        long numeratorA = (long)(x4 - x3) * (long)(y1 - y3) - (long)(y4 - y3) * (long)(x1 - x3);
        long numeratorB = (long)(x2 - x1) * (long)(y1 - y3) - (long)(y2 - y1) * (long)(x1 - x3);

        if (denominator > 0L) {
            return numeratorA >= 0L && numeratorA <= denominator
                    && numeratorB >= 0L && numeratorB <= denominator;
        } else {
            return numeratorA <= 0L && numeratorA >= denominator
                    && numeratorB <= 0L && numeratorB >= denominator;
        }
    }

    /**
     * Tests if a line segment intersects with an axis-aligned bounding box (approximating a circle).
     */
    private static boolean doesLineIntersectCircle(int lineX1, int lineY1, int lineX2, int lineY2,
                                                   int circleX, int circleY, int radius) {
        return doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY - radius, circleX + radius, circleY - radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY - radius, circleX + radius, circleY + radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY + radius, circleX - radius, circleY + radius)
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY + radius, circleX - radius, circleY - radius);
    }

    /**
     * Checks if there's an unobstructed line of sight between two points.
     */
    public final boolean checkLineOfSight(Transform3D from, Transform3D to) {
        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];
            if (wall.isSolid() || isWallSolid(wall)) {
                Point2D wallStart = this.vertices[wall.startVertexId & 0xFFFF];
                Point2D wallEnd = this.vertices[wall.endVertexId & 0xFFFF];

                if (doLineSegmentsIntersect(to.x, to.z, from.x, from.z,
                        wallStart.x, wallStart.y, wallEnd.x, wallEnd.y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates and fires a projectile from an enemy towards the player.
     * Used by Elite Soldiers (type 3001).
     */
    public final void shootProjectile(Transform3D origin, SectorData sector) {
        int angleToPlayer = calculateAngleBetweenPoints(origin.x, origin.z,
                GameEngine.player.x, GameEngine.player.z);

        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - angleToPlayer);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - angleToPlayer);

        int spawnX = origin.x + 20 * cosAngle;
        int spawnZ = origin.z + 20 * sinAngle;
        int spawnY = origin.y + ((sector.floorHeight + 40) << 16);

        Transform3D projectileTransform = new Transform3D(spawnX, spawnY, spawnZ, angleToPlayer);

        GameObject projectile = new GameObject(projectileTransform, 0, 101, 0);
        projectile.addSpriteFrame((byte)0, (byte)-46);
        projectile.addSpriteFrame((byte)0, (byte)-47);
        projectile.spriteFrameIndex = 0;

        this.projectiles.addElement(projectile);
    }

    /**
     * Creates and fires spread projectiles from the boss.
     * Used by Boss (type 3002).
     */
    public final void shootSpreadWeapon(Transform3D origin, SectorData sector) {
        int angleToPlayer = calculateAngleBetweenPoints(origin.x, origin.z,
                GameEngine.player.x, GameEngine.player.z);

        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - angleToPlayer);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - angleToPlayer);

        int baseX = origin.x + 20 * cosAngle;
        int baseZ = origin.z + 20 * sinAngle;
        int spawnY = origin.y + ((sector.floorHeight + 40) << 16);

        sinAngle = MathUtils.fastSin(angleToPlayer);
        cosAngle = MathUtils.fastCos(angleToPlayer);
        int offsetX = 10 * cosAngle;
        int offsetZ = -10 * sinAngle;

        Transform3D leftTransform = new Transform3D(baseX + offsetX, spawnY, baseZ + offsetZ, angleToPlayer);
        GameObject leftProjectile = new GameObject(leftTransform, 0, 102, 0);
        leftProjectile.addSpriteFrame((byte)0, (byte)-71);
        leftProjectile.spriteFrameIndex = 0;
        this.projectiles.addElement(leftProjectile);

        Transform3D rightTransform = new Transform3D(baseX - offsetX, spawnY, baseZ - offsetZ, angleToPlayer);
        GameObject rightProjectile = new GameObject(rightTransform, 0, 102, 0);
        rightProjectile.addSpriteFrame((byte)0, (byte)-71);
        rightProjectile.spriteFrameIndex = 0;
        this.projectiles.addElement(rightProjectile);
    }

    /**
     * Checks if a projectile's path is blocked by walls.
     */
    private boolean isProjectilePathBlocked(int startX, int startZ, int endX, int endZ, int height) {
        int heightInUnits = height >> 16;

        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];
            if (wall.isSolid() || isWallHeightBlocking(heightInUnits, wall)) {
                Point2D wallStart = this.vertices[wall.startVertexId & 0xFFFF];
                Point2D wallEnd = this.vertices[wall.endVertexId & 0xFFFF];

                if (doLineSegmentsIntersect(startX, startZ, endX, endZ,
                        wallStart.x, wallStart.y, wallEnd.x, wallEnd.y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fires the player's current weapon.
     */
    public final void fireWeapon() {
        int currentWeaponId = GameEngine.currentWeapon;
        int playerAngle = GameEngine.player.rotation;
        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - playerAngle);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - playerAngle);

        boolean isShortRangeWeapon = currentWeaponId == WeaponFactory.FIST
                || currentWeaponId == WeaponFactory.PANZERFAUST
                || currentWeaponId == WeaponFactory.SONIC;
        int weaponRange = isShortRangeWeapon ? OBJECT_COLLISION_RADIUS : MAX_HITSCAN_RANGE;

        int targetX = GameEngine.player.x + MathUtils.fixedPointMultiply(weaponRange, cosAngle);
        int targetZ = GameEngine.player.z + MathUtils.fixedPointMultiply(weaponRange, sinAngle);

        if (currentWeaponId == WeaponFactory.PANZERFAUST) {
            HelperUtils.playSound(4, false, 100, 2);
            Transform3D rocketTransform = new Transform3D(targetX,
                    GameEngine.cameraHeight - COLLISION_RADIUS, targetZ, playerAngle);

            if (!this.isProjectilePathBlocked(GameEngine.player.x, GameEngine.player.z,
                    rocketTransform.x, rocketTransform.z, rocketTransform.y)) {
                GameObject rocket = new GameObject(rocketTransform, 0, 100, 0);
                rocket.addSpriteFrame((byte)0, (byte)-44);
                rocket.addSpriteFrame((byte)0, (byte)-45);
                rocket.spriteFrameIndex = 0;
                this.projectiles.addElement(rocket);
            }
            return;
        }

        if (currentWeaponId == WeaponFactory.SONIC) {
            HelperUtils.playSound(5, false, 100, 2);

            sinAngle = MathUtils.fastSin(playerAngle);
            cosAngle = MathUtils.fastCos(playerAngle);
            int offsetX = 10 * cosAngle;
            int offsetZ = -10 * sinAngle;

            Transform3D leftTransform = new Transform3D(targetX - offsetX,
                    GameEngine.cameraHeight - COLLISION_RADIUS, targetZ - offsetZ, playerAngle);
            if (!this.isProjectilePathBlocked(GameEngine.player.x, GameEngine.player.z,
                    leftTransform.x, leftTransform.z, leftTransform.y)) {
                GameObject leftProjectile = new GameObject(leftTransform, 0, 102, 0);
                leftProjectile.addSpriteFrame((byte)0, (byte)-71);
                leftProjectile.spriteFrameIndex = 0;
                this.projectiles.addElement(leftProjectile);
            }

            Transform3D rightTransform = new Transform3D(targetX + offsetX,
                    GameEngine.cameraHeight - COLLISION_RADIUS, targetZ + offsetZ, playerAngle);
            if (!this.isProjectilePathBlocked(GameEngine.player.x, GameEngine.player.z,
                    rightTransform.x, rightTransform.z, rightTransform.y)) {
                GameObject rightProjectile = new GameObject(rightTransform, 0, 102, 0);
                rightProjectile.addSpriteFrame((byte)0, (byte)-71);
                rightProjectile.spriteFrameIndex = 0;
                this.projectiles.addElement(rightProjectile);
            }
            return;
        }

        boolean hitEnemy = false;
        Weapon currentWeapon = MainGameCanvas.weaponManager.getCurrentWeapon();

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject enemy = this.staticObjects[i];
            if (enemy == null || enemy.aiState == -1) continue;

            Transform3D enemyTransform = enemy.transform;

            if (!this.checkLineOfSight(GameEngine.player, enemyTransform)) continue;

            if (doesLineIntersectCircle(GameEngine.player.x, GameEngine.player.z,
                    targetX, targetZ, enemyTransform.x, enemyTransform.z, ENEMY_HIT_RADIUS)) {

                int damage = currentWeapon.getDamage(GameEngine.difficultyLevel);
                int hitSound = 0;

                switch (currentWeaponId) {
                    case WeaponFactory.FIST:
                        break;

                    case WeaponFactory.LUGER:
                    case WeaponFactory.MAUSER:
                        hitSound = 7;
                        break;

                    case WeaponFactory.RIFLE:
                    case WeaponFactory.STEN:
                        hitSound = 9;
                        break;
                }

                if (hitSound != 0) {
                    HelperUtils.playSound(hitSound, false, 100, 1);
                    hitEnemy = true;
                }

                applyDamageToEnemy(enemy, damage);
                break;
            }
        }

        if (!hitEnemy) {
            if (currentWeaponId == WeaponFactory.LUGER || currentWeaponId == WeaponFactory.MAUSER) {
                HelperUtils.playSound((GameEngine.random.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
            }
            if (currentWeaponId == WeaponFactory.RIFLE || currentWeaponId == WeaponFactory.STEN) {
                HelperUtils.playSound((GameEngine.random.nextInt() & 1) == 0 ? 3 : 8, false, 100, 1);
            }
        }
    }

    /**
     * Applies damage to an enemy and updates its AI state.
     */
    private static void applyDamageToEnemy(GameObject enemy, int damage) {
        enemy.health -= damage;

        if (enemy.health <= 0) {
            enemy.health = 0;
            enemy.aiState = 6;

            switch (enemy.objectType) {
                case 3001:
                case 3003:
                case 3004:
                case 3005:
                case 3006:
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 5;
                    break;

                case 3002:
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 4;
                    break;
            }
        } else {
            enemy.aiState = 5;

            switch (enemy.objectType) {
                case 3001:
                case 3003:
                case 3004:
                case 3005:
                case 3006:
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 4;
                    break;

                case 3002:
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 3;
                    break;
            }
        }
    }

    /**
     * Toggles projectile sprite animation frames.
     */
    public final void toggleProjectileSprites() {
        for (int i = 0; i < this.projectiles.size(); i++) {
            GameObject projectile = (GameObject) this.projectiles.elementAt(i);
            if (projectile.objectType == 100 || projectile.objectType == 101) {
                projectile.spriteFrameIndex ^= 1;
            }
        }
    }

    /**
     * Updates all active projectiles and handles their collisions.
     */
    public final boolean updateProjectiles() {
        for (int i = 0; i < this.projectiles.size(); i++) {
            GameObject projectile = (GameObject) this.projectiles.elementAt(i);

            if (projectile.objectType == 103) {
                if (projectile.detonationTimer <= 0) continue;

                projectile.detonationTimer--;
                if (projectile.detonationTimer != 0) continue;

                HelperUtils.playSound(4, false, 100, 2);

                if (this.checkLineOfSight(projectile.transform, GameEngine.player)) {
                    int deltaX = projectile.transform.x - GameEngine.player.x;
                    int deltaZ = projectile.transform.z - GameEngine.player.z;
                    int distance = MathUtils.fastHypot(deltaX, deltaZ);
                    int scaledDistance = MathUtils.fixedPointMultiply(distance,
                            EXPLOSION_FALLOFF_RATE[GameEngine.difficultyLevel]) >> 16;
                    int explosionDamage = EXPLOSION_MAX_DAMAGE[GameEngine.difficultyLevel] - scaledDistance;

                    if (explosionDamage > 0) {
                        HelperUtils.vibrateDevice(explosionDamage * 10);
                        if (GameEngine.applyDamage(explosionDamage)) {
                            return true;
                        }
                    }
                }

                for (int j = 0; j < this.staticObjects.length; j++) {
                    GameObject enemy = this.staticObjects[j];
                    if (enemy == null || enemy.aiState == -1) continue;

                    if (this.checkLineOfSight(projectile.transform, enemy.transform)) {
                        int deltaX = projectile.transform.x - enemy.transform.x;
                        int deltaZ = projectile.transform.z - enemy.transform.z;
                        int distance = MathUtils.fastHypot(deltaX, deltaZ);
                        int scaledDistance = MathUtils.fixedPointMultiply(distance,
                                EXPLOSION_FALLOFF_RATE[GameEngine.difficultyLevel]) >> 16;
                        int explosionDamage = EXPLOSION_MAX_DAMAGE[GameEngine.difficultyLevel] - scaledDistance;

                        if (explosionDamage > 0) {
                            applyDamageToEnemy(enemy, explosionDamage);
                        }
                    }
                }

                GameEngine.screenShake = 16;

                if (MainGameCanvas.currentLevelId == 4) {
                    Transform3D grenadePos = projectile.transform;
                    if (this.getSectorDataAtPoint(grenadePos.x, grenadePos.z).getSectorType() == 666) {
                        triggerLevelTransition();
                    }
                }

                this.projectiles.removeElementAt(i--);
                continue;
            }

            Transform3D projTransform = projectile.transform;
            int prevX = projTransform.x;
            int prevZ = projTransform.z;

            projTransform.moveRelative(0, -PROJECTILE_SPEED);

            int newX = projTransform.x;
            int newZ = projTransform.z;
            boolean projectileHit = false;

            int projectileDamage;
            if (projectile.objectType == 102) {
                projectileDamage = MainGameCanvas.weaponManager.getWeapon(WeaponFactory.SONIC).getDamage(GameEngine.difficultyLevel);
            } else {
                projectileDamage = MainGameCanvas.weaponManager.getWeapon(WeaponFactory.PANZERFAUST).getDamage(GameEngine.difficultyLevel);
            }

            if (doesLineIntersectCircle(prevX, prevZ, newX, newZ,
                    GameEngine.player.x, GameEngine.player.z, COLLISION_RADIUS)) {
                if (projectile.objectType == 101) {
                    HelperUtils.playSound(4, false, 100, 2);
                }
                HelperUtils.vibrateDevice(projectileDamage * 10);
                if (GameEngine.applyDamage(projectileDamage)) {
                    return true;
                }
                projectileHit = true;
            }

            for (int j = 0; j < this.staticObjects.length; j++) {
                GameObject enemy = this.staticObjects[j];
                if (enemy == null || enemy.aiState == -1) continue;

                Transform3D enemyTransform = enemy.transform;
                int hitRadius = (projectile.objectType == 102) ? COLLISION_RADIUS : ENEMY_HIT_RADIUS;

                if (doesLineIntersectCircle(prevX, prevZ, newX, newZ,
                        enemyTransform.x, enemyTransform.z, hitRadius)) {
                    applyDamageToEnemy(enemy, projectileDamage);
                    projectileHit = true;
                }
            }

            if (!projectileHit) {
                int projectileHeight = projTransform.y >> 16;

                for (int j = 0; j < this.wallDefinitions.length; j++) {
                    WallDefinition wall = this.wallDefinitions[j];
                    if (wall.isSolid() || isWallHeightBlocking(projectileHeight, wall)) {
                        Point2D wallStart = this.vertices[wall.startVertexId & 0xFFFF];
                        Point2D wallEnd = this.vertices[wall.endVertexId & 0xFFFF];

                        if (doLineSegmentsIntersect(prevX, prevZ, newX, newZ,
                                wallStart.x, wallStart.y, wallEnd.x, wallEnd.y)) {
                            projectileHit = true;
                            break;
                        }
                    }
                }
            }

            if (projectileHit) {
                this.projectiles.removeElementAt(i--);
            }
        }

        return false;
    }

    /**
     * Calculates the angle from one point to another.
     * Returns angle in game's custom angle system.
     */
    private static int calculateAngleBetweenPoints(int fromX, int fromZ, int toX, int toZ) {
        int deltaX = toX - fromX;
        int deltaZ = toZ - fromZ;
        int distance = MathUtils.fastHypot(deltaX, deltaZ);

        int normalizedX = MathUtils.preciseDivide(deltaX, distance);
        int normalizedZ = MathUtils.preciseDivide(deltaZ, distance);

        long absNormalizedZ = (long)(normalizedZ < 0 ? -normalizedZ : normalizedZ);
        long absNormalizedX = (long)(normalizedX < 0 ? -normalizedX : normalizedX);

        int angle = (absNormalizedX < 6L) ? 0
                : MathUtils.fastAtan((int)((absNormalizedZ << 32) / absNormalizedX >> 16));

        if (normalizedX < 0) {
            angle = ANGLE_180_DEGREES - angle;
        }
        if (normalizedZ > 0) {
            angle = ANGLE_360_DEGREES - angle;
        }

        angle += ANGLE_90_DEGREES;
        if (angle >= ANGLE_360_DEGREES) {
            angle -= ANGLE_360_DEGREES;
        }

        return angle;
    }

    /**
     * Throws a grenade in the direction the player is facing.
     */
    public final boolean throwGrenade() {
        int playerAngle = GameEngine.player.rotation;
        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - playerAngle);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - playerAngle);

        int spawnDistance = COLLISION_RADIUS;
        int spawnX = GameEngine.player.x + MathUtils.fixedPointMultiply(spawnDistance, cosAngle);
        int spawnZ = GameEngine.player.z + MathUtils.fixedPointMultiply(spawnDistance, sinAngle);

        Transform3D grenadeTransform = new Transform3D(spawnX, 0, spawnZ, playerAngle);

        GameObject grenade = new GameObject(grenadeTransform, 0, 103, 100);
        grenade.addSpriteFrame((byte)0, (byte)-51);
        grenade.spriteFrameIndex = 0;

        this.projectiles.addElement(grenade);
        return true;
    }

    /**
     * Spawns a pickup item when an enemy dies.
     */
    public final void spawnPickUp(GameObject deadEnemy) {
        GameObject pickup = null;

        switch (deadEnemy.objectType) {
            case 3001:
                pickup = new GameObject(deadEnemy.transform, 0, 2004, 0);
                pickup.addSpriteFrame((byte)0, (byte)-43);
                break;

            case 3002:
                pickup = new GameObject(deadEnemy.transform, 0, 2006, 0);
                pickup.addSpriteFrame((byte)0, (byte)-72);
                break;

            case 3003:
            case 3005:
            case 3006:
                pickup = new GameObject(deadEnemy.transform, 0, 2007, 0);
                pickup.addSpriteFrame((byte)0, (byte)-48);
                break;

            case 3004:
                pickup = new GameObject(deadEnemy.transform, 0, 2008, 0);
                pickup.addSpriteFrame((byte)0, (byte)-54);
                break;
        }

        if (pickup != null) {
            this.pickupItems.addElement(pickup);
        }
    }

    /**
     * Draws the minimap overlay.
     */
    public final void drawMapOnScreen(Graphics graphics) {
        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];

            if ((wall.getWallType() == 0 && wall.isTransparent()) || !wall.isRendered()) {
                continue;
            }

            int startVertexId = wall.startVertexId & 0xFFFF;
            int endVertexId = wall.endVertexId & 0xFFFF;
            Point2D startVertex = this.transformedVertices[startVertexId];
            Point2D endVertex = this.transformedVertices[endVertexId];

            int wallColor = (wall.getWallType() != 0) ? 0xFFFF00 : 0xFF0000;
            graphics.setColor(wallColor);

            int screenX1 = (startVertex.x >> 18) + 120;
            int screenY1 = -(startVertex.y >> 18) + 144;
            int screenX2 = (endVertex.x >> 18) + 120;
            int screenY2 = -(endVertex.y >> 18) + 144;

            graphics.drawLine(screenX1, screenY1, screenX2, screenY2);
        }

        graphics.setColor(0x00FF00);
        graphics.drawLine(120, 139, 116, 149);
        graphics.drawLine(120, 139, 124, 149);
        graphics.drawLine(116, 149, 124, 149);
    }
}