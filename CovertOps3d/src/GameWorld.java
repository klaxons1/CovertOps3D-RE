import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/**
 * Represents the game world containing geometry, objects, and handles physics/collision.
 *
 * Suggested field renames:
 * - collisionTestPoint -> collisionResultPoint
 * - lastWallIndex -> lastCollidedWallIndex
 * - transformedVertices -> viewTransformedVertices
 * - staticObjects -> worldObjects (contains both static items and NPCs)
 * - projectiles -> activeProjectiles
 * - pickupItems -> droppedPickups
 * - bspSectors -> sectorNodes
 *
 * Suggested constant additions:
 * - COLLISION_RADIUS = 655360 (10 units in 16.16 fixed-point)
 * - OBJECT_COLLISION_RADIUS = 1310720 (20 units)
 * - PICKUP_RADIUS = 1966080 (30 units)
 * - ENEMY_HIT_RADIUS = 327680 (5 units)
 * - ANGLE_90_DEGREES = 102943 (PI/2 in fixed-point angle system)
 * - MAX_HITSCAN_RANGE = 67108864 (1024 units)
 * - PROJECTILE_SPEED = 1048576 (16 units per tick)
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

    public static int MIN_WALL_HEIGHT = 16;
    public static int MIN_CEILING_CLEARANCE = 50;
    public static int PLAYER_HEIGHT_OFFSET = 40;

    private Point2D collisionTestPoint = new Point2D(0, 0);
    private int lastWallIndex;

    public Point2D[] vertices;
    private Point2D[] transformedVertices;
    public WallDefinition[] wallDefinitions;
    public GameObject[] staticObjects;      // Consider: worldObjects
    private Vector projectiles;             // Consider: activeProjectiles
    private Vector pickupItems;             // Consider: droppedPickups
    public SectorData[] sectors;
    public WallSurface[] wallSurfaces;
    public Transform3D worldOrigin;
    public BSPNode[] bspNodes;
    public Sector[] bspSectors;             // Consider: sectorNodes
    public WallSegment[] wallSegments;

    public GameWorld() {
        new Point2D(0, 0); // Unused allocation - likely leftover from debugging
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

            // Standard 2D rotation: x' = x*cos - y*sin, y' = x*sin + y*cos
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
        // Clear all dynamic object lists from sectors
        for (int i = 0; i < this.bspSectors.length; i++) {
            this.bspSectors[i].clearDynamicObjects();
        }

        // Re-add static objects to their sectors
        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject gameObject = this.staticObjects[i];
            if (gameObject != null) {
                gameObject.addToWorld(this);
            }
        }

        // Add projectiles to sectors
        for (int i = 0; i < this.projectiles.size(); i++) {
            ((GameObject)this.projectiles.elementAt(i)).addToWorld(this);
        }

        // Add dropped pickups to sectors
        for (int i = 0; i < this.pickupItems.size(); i++) {
            ((GameObject)this.pickupItems.elementAt(i)).addToWorld(this);
        }
    }

    /**
     * Determines which side of a line a point is on using cross product.
     * Used for collision detection and BSP traversal.
     */
    private static boolean isPointOnLeftSideOfLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        // Handle vertical lines
        if (lineStart.x == lineEnd.x) {
            if (point.x <= lineStart.x) {
                return lineStart.y - lineEnd.y > 0;
            } else {
                return lineStart.y - lineEnd.y < 0;
            }
        }

        // Handle horizontal lines
        if (lineStart.y == lineEnd.y) {
            if (point.y <= lineStart.y) {
                return lineStart.x - lineEnd.x < 0;
            } else {
                return lineStart.x - lineEnd.x > 0;
            }
        }

        // General case: use cross product
        int deltaX = point.x - lineStart.x;
        int deltaY = point.y - lineStart.y;
        long crossProduct = (long)(lineStart.y - lineEnd.y) * (long)deltaX;
        return (long)(lineStart.x - lineEnd.x) * (long)deltaY >= crossProduct;
    }

    /**
     * Checks if an entity can move to the proposed position.
     * Returns false if collision with blocking object occurs.
     *
     * @param entity The entity trying to move (excluded from collision checks)
     * @param proposedPos The proposed new position
     * @param currentSector The sector the entity is currently in
     * @return true if movement is allowed, false if blocked
     */
    public final boolean checkCollision(GameObject entity, Transform3D proposedPos, SectorData currentSector) {
        this.collisionTestPoint.x = proposedPos.x;
        this.collisionTestPoint.y = proposedPos.z;

        // Check wall collisions and adjust position
        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];
            if (wall.isCollidable() || isWallPassableForSector(currentSector, wall)) {
                this.resolveWallCollision(wall);
            }
        }

        // Calculate AABB bounds for entity
        int entityMinX = this.collisionTestPoint.x - COLLISION_RADIUS;
        int entityMaxX = this.collisionTestPoint.x + COLLISION_RADIUS;
        int entityMinZ = this.collisionTestPoint.y - COLLISION_RADIUS;
        int entityMaxZ = this.collisionTestPoint.y + COLLISION_RADIUS;

        // Check collision with other game objects
        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject other = this.staticObjects[i];
            if (other != null && other != entity && other.aiState != -1) {
                Transform3D otherTransform = other.transform;
                int otherMinX = otherTransform.x - COLLISION_RADIUS;
                int otherMaxX = otherTransform.x + COLLISION_RADIUS;
                int otherMinZ = otherTransform.z - COLLISION_RADIUS;
                int otherMaxZ = otherTransform.z + COLLISION_RADIUS;

                // AABB intersection test
                if (entityMinX <= otherMaxX && entityMaxX >= otherMinX &&
                        entityMinZ <= otherMaxZ && entityMaxZ >= otherMinZ) {
                    switch (other.objectType) {
                        case 10:    // Switch/Lever
                        case 12:    // Button
                        case 3001:  // Elite Soldier
                        case 3002:  // Boss
                        case 3003:  // Regular Soldier
                        case 3004:  // Officer
                        case 3005:  // Guard
                        case 3006:  // Special Unit
                            return false;
                    }
                }
            }
        }

        // Update proposed position with collision-adjusted values
        proposedPos.x = this.collisionTestPoint.x;
        proposedPos.z = this.collisionTestPoint.y;
        return true;
    }

    /**
     * Handles player movement including collision detection and item pickup.
     *
     * @param player The player's physics body
     * @param currentSector The sector the player is in
     * @return The wall that was touched (for interaction), or null
     *
     * Suggested constant renames in MainGameCanvas:
     * - var_1616 -> AMMO_PANZERFAUST_PICKUP (amount: {3,3,3})
     * - var_1677 -> AMMO_SONIC_PICKUP (amount: {3,3,3})
     * - var_14be -> AMMO_LUGER_SMALL (amount: {10,10,10})
     * - var_14f5 -> AMMO_MAUSER_SMALL (amount: {10,10,10})
     * - var_151d -> AMMO_PANZERFAUST_SMALL (amount: {1,1,1})
     * - var_156b -> AMMO_SONIC_SMALL (amount: {6,6,6})
     * - var_157a -> AMMO_LUGER_WEAPON (amount: {10,10,10})
     * - var_1592 -> AMMO_MAUSER_WEAPON (amount: {10,10,10})
     * - var_15c4 -> AMMO_RIFLE_WEAPON (amount: {20,20,20})
     * - var_15d0 -> AMMO_STEN_WEAPON (amount: {20,20,20})
     * - var_1630 -> AMMO_DYNAMITE_PICKUP (amount: {1,1,1})
     * - var_16c7 -> HEALTH_SMALL (amount: {25,25,25})
     * - var_16e8 -> HEALTH_LARGE (amount: {50,50,50})
     * - var_1731 -> ARMOR_PICKUP (amount: {25,25,25})
     */
    public final WallDefinition handlePlayerMovement(PhysicsBody player, SectorData currentSector) {
        WallDefinition touchedWall = null;
        this.collisionTestPoint.x = player.x;
        this.collisionTestPoint.y = player.z;

        int newLastWallIndex = -1;

        // First check the last wall we collided with (optimization)
        if (this.lastWallIndex != -1) {
            WallDefinition lastWall = this.wallDefinitions[this.lastWallIndex];
            if ((lastWall.isPassable() || isWallPassableForSector(currentSector, lastWall))
                    && this.resolveWallCollision(lastWall)) {
                newLastWallIndex = this.lastWallIndex;
                touchedWall = lastWall;
            }
        }

        // Check all other walls
        for (int i = 0; i < this.wallDefinitions.length; i++) {
            if (i == this.lastWallIndex) continue;

            WallDefinition wall = this.wallDefinitions[i];
            if ((wall.isPassable() || isWallPassableForSector(currentSector, wall))
                    && this.resolveWallCollision(wall)) {
                if (newLastWallIndex == -1) {
                    newLastWallIndex = i;
                }
                // Prefer interactive walls over normal walls
                if (touchedWall == null || touchedWall.getWallType() == 0) {
                    touchedWall = wall;
                }
            }
        }

        // Check collision with blocking objects (NPCs, switches, etc.)
        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject obj = this.staticObjects[i];
            if (obj == null || obj.aiState == -1) continue;

            switch (obj.objectType) {
                case 10:    // Switch/Lever
                case 12:    // Button
                case 3001:  // Elite Soldier
                case 3002:  // Boss
                case 3003:  // Regular Soldier
                case 3004:  // Officer
                case 3005:  // Guard
                case 3006:  // Special Unit
                    Transform3D objTransform = obj.transform;
                    int deltaX = this.collisionTestPoint.x - objTransform.x;
                    int deltaZ = this.collisionTestPoint.y - objTransform.z;
                    int absDeltaX = deltaX < 0 ? -deltaX : deltaX;
                    int absDeltaZ = deltaZ < 0 ? -deltaZ : deltaZ;

                    if (absDeltaX < OBJECT_COLLISION_RADIUS && absDeltaZ < OBJECT_COLLISION_RADIUS) {
                        // Special message for level 4 switch
                        if (obj.objectType == 10 && MainGameCanvas.currentLevelId == 4) {
                            GameEngine.messageText = GameEngine.ammoCounts[6] > 0
                                    ? "find the wall i told you|and blow it up!"
                                    : "go, get the dynamite!";
                            GameEngine.messageTimer = 30;
                        }

                        // Push player out of collision
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

        // Update player position
        player.x = this.collisionTestPoint.x;
        player.z = this.collisionTestPoint.y;
        this.lastWallIndex = newLastWallIndex;

        // Check for dropped pickup items
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
                    case 2004: // Panzerfaust ammo drop
                        GameEngine.weaponsAvailable[5] = true;
                        GameEngine.ammoCounts[5] += MainGameCanvas.var_1616[GameEngine.difficultyLevel];
                        triggerWeaponSwitch(5);
                        break;

                    case 2006: // Sonic gun + level transition (boss drop)
                        GameEngine.weaponsAvailable[7] = true;
                        GameEngine.ammoCounts[7] += MainGameCanvas.var_1677[GameEngine.difficultyLevel];
                        triggerWeaponSwitch(7);
                        triggerLevelTransition();
                        break;

                    case 2007: // Luger ammo drop
                        GameEngine.ammoCounts[1] += MainGameCanvas.var_14be[GameEngine.difficultyLevel];
                        break;

                    case 2008: // Mauser ammo drop
                        GameEngine.ammoCounts[2] += MainGameCanvas.var_14f5[GameEngine.difficultyLevel];
                        break;

                    case 2010: // Panzerfaust single ammo
                        GameEngine.ammoCounts[5] += MainGameCanvas.var_151d[GameEngine.difficultyLevel];
                        break;

                    case 2047: // Sonic gun ammo
                        GameEngine.ammoCounts[7] += MainGameCanvas.var_156b[GameEngine.difficultyLevel];
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

        // Check for static world pickups
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
                case 5: // Key 1
                    GameEngine.keysCollected[0] = true;
                    break;

                case 13: // Key 2
                    GameEngine.keysCollected[1] = true;
                    break;

                case 82: // Sniper rifle (special item flag)
                    GameEngine.weaponsAvailable[8] = true;
                    GameEngine.messageText = "go now to the agent anna";
                    GameEngine.messageTimer = 30;
                    break;

                case 2001: // Luger weapon pickup
                    GameEngine.weaponsAvailable[1] = true;
                    GameEngine.ammoCounts[1] += MainGameCanvas.var_157a[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(1);
                    break;

                case 2002: // Mauser weapon pickup
                    if (MainGameCanvas.currentLevelId == 3) {
                        GameEngine.messageText = "to change weapon press 3";
                        GameEngine.messageTimer = 30;
                    }
                    GameEngine.weaponsAvailable[2] = true;
                    GameEngine.ammoCounts[2] += MainGameCanvas.var_1592[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(2);
                    break;

                case 2003: // Rifle weapon pickup
                    GameEngine.weaponsAvailable[3] = true;
                    GameEngine.ammoCounts[1] += MainGameCanvas.var_15c4[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(3);
                    break;

                case 2004: // Panzerfaust weapon pickup
                    GameEngine.weaponsAvailable[5] = true;
                    GameEngine.ammoCounts[5] += MainGameCanvas.var_1616[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(5);
                    break;

                case 2005: // Dynamite pickup
                    GameEngine.weaponsAvailable[6] = true;
                    GameEngine.ammoCounts[6] += MainGameCanvas.var_1630[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(6);
                    break;

                case 2006: // Sonic gun + level transition
                    GameEngine.weaponsAvailable[7] = true;
                    GameEngine.ammoCounts[7] += MainGameCanvas.var_1677[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(7);
                    triggerTransition = true;
                    break;

                case 2007: // Luger ammo
                    GameEngine.ammoCounts[1] += MainGameCanvas.var_14be[GameEngine.difficultyLevel];
                    break;

                case 2008: // Mauser ammo
                    GameEngine.ammoCounts[2] += MainGameCanvas.var_14f5[GameEngine.difficultyLevel];
                    break;

                case 2010: // Panzerfaust ammo
                    GameEngine.ammoCounts[5] += MainGameCanvas.var_151d[GameEngine.difficultyLevel];
                    break;

                case 2012: // Large health pack
                    if (GameEngine.playerHealth >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerHealth += MainGameCanvas.var_16e8[GameEngine.difficultyLevel];
                    if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                    }
                    break;

                case 2013: // Level transition item
                    triggerTransition = true;
                    break;

                case 2014: // Small health pack
                    if (GameEngine.playerHealth >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerHealth += MainGameCanvas.var_16c7[GameEngine.difficultyLevel];
                    if (GameEngine.playerHealth > 100) {
                        GameEngine.playerHealth = 100;
                    }
                    break;

                case 2015: // Armor pickup
                    if (GameEngine.playerArmor >= 100) {
                        collected = false;
                        break;
                    }
                    GameEngine.playerArmor += MainGameCanvas.var_1731[GameEngine.difficultyLevel];
                    if (GameEngine.playerArmor > 100) {
                        GameEngine.playerArmor = 100;
                    }
                    break;

                case 2024: // Sten weapon pickup
                    GameEngine.weaponsAvailable[4] = true;
                    GameEngine.ammoCounts[1] += MainGameCanvas.var_15d0[GameEngine.difficultyLevel];
                    triggerWeaponSwitch(4);
                    break;

                case 2047: // Sonic gun ammo
                    GameEngine.ammoCounts[7] += MainGameCanvas.var_156b[GameEngine.difficultyLevel];
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

    /** Helper to trigger weapon switch animation */
    private static void triggerWeaponSwitch(int weaponId) {
        GameEngine.pendingWeaponSwitch = weaponId;
        GameEngine.weaponSwitchAnimationActive = true;
        GameEngine.weaponAnimationState = 8;
    }

    /** Helper to trigger level transition */
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

        // Check back sector accessibility
        if (backSector != currentSector) {
            // Check step height
            if (backSector.floorHeight - currentSector.floorHeight > MIN_WALL_HEIGHT) {
                return true;
            }
            // Check ceiling clearance
            short maxFloorHeight = backSector.floorHeight;
            if (currentSector.floorHeight > maxFloorHeight) {
                maxFloorHeight = currentSector.floorHeight;
            }
            if (backSector.ceilingHeight - maxFloorHeight < MIN_CEILING_CLEARANCE) {
                return true;
            }
        }

        // Check front sector accessibility
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

        // Check if either sector is collapsed (zero or negative height)
        if (backSector.ceilingHeight - backSector.floorHeight <= 0) {
            return true;
        }
        if (frontSector.ceilingHeight - frontSector.floorHeight <= 0) {
            return true;
        }

        // Check if sectors don't overlap vertically
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

        // Calculate wall properties
        int wallDeltaX = endVertex.x - startVertex.x;
        int wallDeltaY = endVertex.y - startVertex.y;
        int wallCenterX = startVertex.x + (wallDeltaX >> 1);
        int wallCenterY = startVertex.y + (wallDeltaY >> 1);
        int wallHalfExtentX = wallDeltaX >= 0 ? wallDeltaX >> 1 : -(wallDeltaX >> 1);
        int wallHalfExtentY = wallDeltaY >= 0 ? wallDeltaY >> 1 : -(wallDeltaY >> 1);

        // Calculate relative position to wall center
        int relativeToCenterX = this.collisionTestPoint.x - wallCenterX;
        int relativeToCenterY = this.collisionTestPoint.y - wallCenterY;
        int absRelativeX = relativeToCenterX >= 0 ? relativeToCenterX : -relativeToCenterX;
        int absRelativeY = relativeToCenterY >= 0 ? relativeToCenterY : -relativeToCenterY;

        // Check X-axis overlap
        int overlapX = wallHalfExtentX + COLLISION_RADIUS - absRelativeX;
        if (overlapX <= 0) return false;

        // Check Y-axis overlap
        int overlapY = wallHalfExtentY + COLLISION_RADIUS - absRelativeY;
        if (overlapY <= 0) return false;

        // Determine correction direction (separate on axis with smallest overlap)
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

        // Determine which side of wall we're on and get appropriate normal direction
        int normalX, normalY;
        if (isPointOnLeftSideOfLine(this.collisionTestPoint, wallStart, wallEnd)) {
            normalX = -wallNormal.x;
            normalY = -wallNormal.y;
        } else {
            normalX = wallNormal.x;
            normalY = wallNormal.y;
        }

        // Calculate penetration depth
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

        // Calculate position relative to wall bounds
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

        // Calculate dot product with normal
        int dotProduct = (int)((long)relativeX * (long)normalX + (long)relativeY * (long)normalY >> 16);

        if (dotProduct >= 0) return false;

        // Calculate push-out vector
        int pushX = (int)((long)normalX * (long)(-dotProduct) >> 16);
        int pushY = (int)((long)normalY * (long)(-dotProduct) >> 16);
        int pushMagnitude = (pushX >= 0 ? pushX : -pushX) + (pushY >= 0 ? pushY : -pushY);
        int correctionMagnitude = (correctionX >= 0 ? correctionX : -correctionX)
                + (correctionY >= 0 ? correctionY : -correctionY);

        // Apply smaller correction
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
            return false; // Lines are parallel
        }

        long numeratorA = (long)(x4 - x3) * (long)(y1 - y3) - (long)(y4 - y3) * (long)(x1 - x3);
        long numeratorB = (long)(x2 - x1) * (long)(y1 - y3) - (long)(y2 - y1) * (long)(x1 - x3);

        // Check if intersection point lies within both segments
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
        // Test intersection with all four sides of bounding box
        return doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY - radius, circleX + radius, circleY - radius)  // Top
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY - radius, circleX + radius, circleY + radius)  // Right
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX + radius, circleY + radius, circleX - radius, circleY + radius)  // Bottom
                || doLineSegmentsIntersect(lineX1, lineY1, lineX2, lineY2,
                circleX - radius, circleY + radius, circleX - radius, circleY - radius); // Left
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

        // Offset spawn position slightly forward
        int spawnX = origin.x + 20 * cosAngle;
        int spawnZ = origin.z + 20 * sinAngle;
        int spawnY = origin.y + ((sector.floorHeight + 40) << 16);

        Transform3D projectileTransform = new Transform3D(spawnX, spawnY, spawnZ, angleToPlayer);

        GameObject projectile = new GameObject(projectileTransform, 0, 101, 0); // 101 = enemy projectile
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

        // Base spawn position
        int baseX = origin.x + 20 * cosAngle;
        int baseZ = origin.z + 20 * sinAngle;
        int spawnY = origin.y + ((sector.floorHeight + 40) << 16);

        // Calculate perpendicular offset for spread
        sinAngle = MathUtils.fastSin(angleToPlayer);
        cosAngle = MathUtils.fastCos(angleToPlayer);
        int offsetX = 10 * cosAngle;
        int offsetZ = -10 * sinAngle;

        // Create left projectile
        Transform3D leftTransform = new Transform3D(baseX + offsetX, spawnY, baseZ + offsetZ, angleToPlayer);
        GameObject leftProjectile = new GameObject(leftTransform, 0, 102, 0); // 102 = spread projectile
        leftProjectile.addSpriteFrame((byte)0, (byte)-71);
        leftProjectile.spriteFrameIndex = 0;
        this.projectiles.addElement(leftProjectile);

        // Create right projectile
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
     *
     * Suggested constant renames in MainGameCanvas:
     * - var_f4a -> DAMAGE_FIST ({5,5,5})
     * - var_f5c -> DAMAGE_LUGER ({25,25,25})
     * - var_f76 -> DAMAGE_MAUSER ({30,30,30})
     * - var_fa7 -> DAMAGE_RIFLE ({25,25,25})
     * - var_ff1 -> DAMAGE_STEN ({25,25,25})
     */
    public final void fireWeapon() {
        int playerAngle = GameEngine.player.rotation;
        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - playerAngle);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - playerAngle);

        // Determine weapon range (melee/explosive weapons have shorter range)
        boolean isShortRangeWeapon = GameEngine.currentWeapon == 0
                || GameEngine.currentWeapon == 5
                || GameEngine.currentWeapon == 7;
        int weaponRange = isShortRangeWeapon ? OBJECT_COLLISION_RADIUS : MAX_HITSCAN_RANGE;

        int targetX = GameEngine.player.x + MathUtils.fixedPointMultiply(weaponRange, cosAngle);
        int targetZ = GameEngine.player.z + MathUtils.fixedPointMultiply(weaponRange, sinAngle);

        // Handle Panzerfaust (weapon 5) - fires rocket projectile
        if (GameEngine.currentWeapon == 5) {
            HelperUtils.playSound(4, false, 100, 2);
            Transform3D rocketTransform = new Transform3D(targetX,
                    GameEngine.cameraHeight - COLLISION_RADIUS, targetZ, playerAngle);

            if (!this.isProjectilePathBlocked(GameEngine.player.x, GameEngine.player.z,
                    rocketTransform.x, rocketTransform.z, rocketTransform.y)) {
                GameObject rocket = new GameObject(rocketTransform, 0, 100, 0); // 100 = player rocket
                rocket.addSpriteFrame((byte)0, (byte)-44);
                rocket.addSpriteFrame((byte)0, (byte)-45);
                rocket.spriteFrameIndex = 0;
                this.projectiles.addElement(rocket);
            }
            return;
        }

        // Handle Sonic Gun (weapon 7) - fires spread projectiles
        if (GameEngine.currentWeapon == 7) {
            HelperUtils.playSound(5, false, 100, 2);

            sinAngle = MathUtils.fastSin(playerAngle);
            cosAngle = MathUtils.fastCos(playerAngle);
            int offsetX = 10 * cosAngle;
            int offsetZ = -10 * sinAngle;

            // Left projectile
            Transform3D leftTransform = new Transform3D(targetX - offsetX,
                    GameEngine.cameraHeight - COLLISION_RADIUS, targetZ - offsetZ, playerAngle);
            if (!this.isProjectilePathBlocked(GameEngine.player.x, GameEngine.player.z,
                    leftTransform.x, leftTransform.z, leftTransform.y)) {
                GameObject leftProjectile = new GameObject(leftTransform, 0, 102, 0);
                leftProjectile.addSpriteFrame((byte)0, (byte)-71);
                leftProjectile.spriteFrameIndex = 0;
                this.projectiles.addElement(leftProjectile);
            }

            // Right projectile
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

        // Handle hitscan weapons (fist, pistols, rifles)
        boolean hitEnemy = false;

        for (int i = 0; i < this.staticObjects.length; i++) {
            GameObject enemy = this.staticObjects[i];
            if (enemy == null || enemy.aiState == -1) continue;

            Transform3D enemyTransform = enemy.transform;

            if (!this.checkLineOfSight(GameEngine.player, enemyTransform)) continue;

            if (doesLineIntersectCircle(GameEngine.player.x, GameEngine.player.z,
                    targetX, targetZ, enemyTransform.x, enemyTransform.z, ENEMY_HIT_RADIUS)) {

                int damage = 0;
                int hitSound = 0;

                switch (GameEngine.currentWeapon) {
                    case 0: // Fist
                        damage = MainGameCanvas.var_f4a[GameEngine.difficultyLevel];
                        break;

                    case 1: // Luger
                        damage = MainGameCanvas.var_f5c[GameEngine.difficultyLevel];
                        hitSound = 7;
                        break;

                    case 2: // Mauser
                        damage = MainGameCanvas.var_f76[GameEngine.difficultyLevel];
                        hitSound = 7;
                        break;

                    case 3: // Rifle
                        damage = MainGameCanvas.var_fa7[GameEngine.difficultyLevel];
                        hitSound = 9;
                        break;

                    case 4: // Sten
                        damage = MainGameCanvas.var_ff1[GameEngine.difficultyLevel];
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

        // Play miss sound if no enemy was hit
        if (!hitEnemy) {
            if (GameEngine.currentWeapon == 1 || GameEngine.currentWeapon == 2) {
                HelperUtils.playSound((GameEngine.random.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
            }
            if (GameEngine.currentWeapon == 3 || GameEngine.currentWeapon == 4) {
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
            // Enemy killed
            enemy.health = 0;
            enemy.aiState = 6; // Death state

            switch (enemy.objectType) {
                case 3001: // Elite Soldier
                case 3003: // Regular Soldier
                case 3004: // Officer
                case 3005: // Guard
                case 3006: // Special Unit
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 5; // Death frame
                    break;

                case 3002: // Boss (different death animation)
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 4;
                    break;
            }
        } else {
            // Enemy hurt but alive
            enemy.aiState = 5; // Hurt state

            switch (enemy.objectType) {
                case 3001:
                case 3003:
                case 3004:
                case 3005:
                case 3006:
                    enemy.stateTimer = 5;
                    enemy.spriteFrameIndex = 4; // Hurt frame
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
            // Animate rockets and enemy projectiles
            if (projectile.objectType == 100 || projectile.objectType == 101) {
                projectile.spriteFrameIndex ^= 1;
            }
        }
    }

    /**
     * Updates all active projectiles and handles their collisions.
     *
     * Suggested constant renames:
     * - var_104c -> DAMAGE_SONIC_PROJECTILE ({65536,65536,65536} - 1.0 in fixed-point)
     * - var_1071 -> DAMAGE_ROCKET ({150,150,150})
     * - var_1113 -> EXPLOSION_MAX_DAMAGE ({400,400,400})
     * - var_10c4 -> EXPLOSION_FALLOFF_RATE ({65536,65536,65536} - 1.0 in fixed-point)
     *
     * @return true if player was killed
     */
    public final boolean updateProjectiles() {
        for (int i = 0; i < this.projectiles.size(); i++) {
            GameObject projectile = (GameObject) this.projectiles.elementAt(i);

            // Handle grenades (type 103)
            if (projectile.objectType == 103) {
                if (projectile.detonationTimer <= 0) continue;

                projectile.detonationTimer--;
                if (projectile.detonationTimer != 0) continue;

                // Grenade explosion
                HelperUtils.playSound(4, false, 100, 2);

                // Damage player if in line of sight
                if (this.checkLineOfSight(projectile.transform, GameEngine.player)) {
                    int deltaX = projectile.transform.x - GameEngine.player.x;
                    int deltaZ = projectile.transform.z - GameEngine.player.z;
                    int distance = MathUtils.fastHypot(deltaX, deltaZ);
                    int scaledDistance = MathUtils.fixedPointMultiply(distance,
                            MainGameCanvas.var_10c4[GameEngine.difficultyLevel]) >> 16;
                    int explosionDamage = MainGameCanvas.var_1113[GameEngine.difficultyLevel] - scaledDistance;

                    if (explosionDamage > 0) {
                        HelperUtils.vibrateDevice(explosionDamage * 10);
                        if (GameEngine.applyDamage(explosionDamage)) {
                            return true;
                        }
                    }
                }

                // Damage enemies in range
                for (int j = 0; j < this.staticObjects.length; j++) {
                    GameObject enemy = this.staticObjects[j];
                    if (enemy == null || enemy.aiState == -1) continue;

                    if (this.checkLineOfSight(projectile.transform, enemy.transform)) {
                        int deltaX = projectile.transform.x - enemy.transform.x;
                        int deltaZ = projectile.transform.z - enemy.transform.z;
                        int distance = MathUtils.fastHypot(deltaX, deltaZ);
                        int scaledDistance = MathUtils.fixedPointMultiply(distance,
                                MainGameCanvas.var_10c4[GameEngine.difficultyLevel]) >> 16;
                        int explosionDamage = MainGameCanvas.var_1113[GameEngine.difficultyLevel] - scaledDistance;

                        if (explosionDamage > 0) {
                            applyDamageToEnemy(enemy, explosionDamage);
                        }
                    }
                }

                GameEngine.screenShake = 16;

                // Special level 4 objective: blow up marked wall
                if (MainGameCanvas.currentLevelId == 4) {
                    Transform3D grenadePos = projectile.transform;
                    if (this.getSectorDataAtPoint(grenadePos.x, grenadePos.z).getSectorType() == 666) {
                        triggerLevelTransition();
                    }
                }

                this.projectiles.removeElementAt(i--);
                continue;
            }

            // Handle moving projectiles (rockets, enemy shots, sonic blasts)
            Transform3D projTransform = projectile.transform;
            int prevX = projTransform.x;
            int prevZ = projTransform.z;

            projTransform.moveRelative(0, -PROJECTILE_SPEED);

            int newX = projTransform.x;
            int newZ = projTransform.z;
            boolean projectileHit = false;

            // Determine damage based on projectile type
            int projectileDamage = (projectile.objectType == 102)
                    ? MainGameCanvas.var_104c[GameEngine.difficultyLevel]  // Sonic
                    : MainGameCanvas.var_1071[GameEngine.difficultyLevel]; // Rocket/enemy

            // Check collision with player
            if (doesLineIntersectCircle(prevX, prevZ, newX, newZ,
                    GameEngine.player.x, GameEngine.player.z, COLLISION_RADIUS)) {
                if (projectile.objectType == 101) {
                    HelperUtils.playSound(4, false, 100, 2); // Enemy rocket hit sound
                }
                HelperUtils.vibrateDevice(projectileDamage * 10);
                if (GameEngine.applyDamage(projectileDamage)) {
                    return true;
                }
                projectileHit = true;
            }

            // Check collision with enemies
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

            // Check collision with walls
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

        // Normalize delta values
        int normalizedX = MathUtils.preciseDivide(deltaX, distance);
        int normalizedZ = MathUtils.preciseDivide(deltaZ, distance);

        long absNormalizedZ = (long)(normalizedZ < 0 ? -normalizedZ : normalizedZ);
        long absNormalizedX = (long)(normalizedX < 0 ? -normalizedX : normalizedX);

        // Calculate base angle using arctangent
        int angle = (absNormalizedX < 6L) ? 0
                : MathUtils.fastAtan((int)((absNormalizedZ << 32) / absNormalizedX >> 16));

        // Adjust for quadrant
        if (normalizedX < 0) {
            angle = ANGLE_180_DEGREES - angle;
        }
        if (normalizedZ > 0) {
            angle = ANGLE_360_DEGREES - angle;
        }

        // Offset to match game's coordinate system
        angle += ANGLE_90_DEGREES;
        if (angle >= ANGLE_360_DEGREES) {
            angle -= ANGLE_360_DEGREES;
        }

        return angle;
    }

    /**
     * Throws a grenade in the direction the player is facing.
     * @return true (always succeeds)
     */
    public final boolean throwGrenade() {
        int playerAngle = GameEngine.player.rotation;
        int sinAngle = MathUtils.fastSin(ANGLE_90_DEGREES - playerAngle);
        int cosAngle = MathUtils.fastCos(ANGLE_90_DEGREES - playerAngle);

        int spawnDistance = COLLISION_RADIUS;
        int spawnX = GameEngine.player.x + MathUtils.fixedPointMultiply(spawnDistance, cosAngle);
        int spawnZ = GameEngine.player.z + MathUtils.fixedPointMultiply(spawnDistance, sinAngle);

        Transform3D grenadeTransform = new Transform3D(spawnX, 0, spawnZ, playerAngle);

        // Create grenade with 100 tick fuse timer
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
            case 3001: // Elite Soldier drops Panzerfaust ammo
                pickup = new GameObject(deadEnemy.transform, 0, 2004, 0);
                pickup.addSpriteFrame((byte)0, (byte)-43);
                break;

            case 3002: // Boss drops Sonic Gun
                pickup = new GameObject(deadEnemy.transform, 0, 2006, 0);
                pickup.addSpriteFrame((byte)0, (byte)-72);
                break;

            case 3003: // Regular Soldier
            case 3005: // Guard
            case 3006: // Special Unit - all drop Luger ammo
                pickup = new GameObject(deadEnemy.transform, 0, 2007, 0);
                pickup.addSpriteFrame((byte)0, (byte)-48);
                break;

            case 3004: // Officer drops Mauser ammo
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
        // Draw all visible walls
        for (int i = 0; i < this.wallDefinitions.length; i++) {
            WallDefinition wall = this.wallDefinitions[i];

            // Skip transparent portal walls and non-rendered walls
            if ((wall.getWallType() == 0 && wall.isTransparent()) || !wall.isRendered()) {
                continue;
            }

            int startVertexId = wall.startVertexId & 0xFFFF;
            int endVertexId = wall.endVertexId & 0xFFFF;
            Point2D startVertex = this.transformedVertices[startVertexId];
            Point2D endVertex = this.transformedVertices[endVertexId];

            // Set color: yellow for special walls, red for normal walls
            int wallColor = (wall.getWallType() != 0) ? 0xFFFF00 : 0xFF0000;
            graphics.setColor(wallColor);

            // Transform to screen coordinates
            int screenX1 = (startVertex.x >> 18) + 120;
            int screenY1 = -(startVertex.y >> 18) + 144;
            int screenX2 = (endVertex.x >> 18) + 120;
            int screenY2 = -(endVertex.y >> 18) + 144;

            graphics.drawLine(screenX1, screenY1, screenX2, screenY2);
        }

        // Draw player indicator (green triangle)
        graphics.setColor(0x00FF00);
        graphics.drawLine(120, 139, 116, 149);
        graphics.drawLine(120, 139, 124, 149);
        graphics.drawLine(116, 149, 124, 149);
    }
}