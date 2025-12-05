import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/**
 * Core game engine managing game logic, physics, AI, and rendering coordination.
 */
public final class GameEngine {

    // ==================== Input State ====================

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

    // ==================== Player State ====================

    public static PhysicsBody player;
    public static int playerHealth = 100;
    public static int playerArmor = 0;
    public static int cameraHeight;

    // ==================== Weapon State ====================

    public static boolean[] weaponsAvailable = new boolean[]{
            true, false, false, false, false, false, true, false, false
    };
    public static int[] ammoCounts = new int[9];
    public static int currentWeapon = 0;
    public static int pendingWeaponSwitch = 0;
    public static int weaponCooldownTimer = 0;
    public static boolean weaponSwitchAnimationActive = false;
    public static int weaponAnimationState = 0;

    // ==================== World State ====================

    public static Transform3D tempTransform;
    public static SectorData currentSector;
    public static Vector doorControllers;
    public static Vector elevatorControllers;

    // ==================== UI State ====================

    public static String messageText = "";
    public static int messageTimer = 0;
    public static int interactionTimer = 0;
    public static WallDefinition activeInteractable = null;
    public static boolean[] keysCollected = new boolean[]{false, false};

    // ==================== Level State ====================

    public static int levelTransitionState;
    public static int difficultyLevel = 1;

    // ==================== Effects ====================

    public static boolean damageFlash = false;
    public static byte screenShake = 0;

    // ==================== Internal State ====================

    public static Random random = new Random();

    private static int enemyAggroDistance = MathUtils.fixedPointMultiply(1310720, 92682);
    private static int cameraBobTimer = 0;
    private static int lastGameLogicTime = 0;

    /**
     * Initializes the game engine and all rendering subsystems.
     * Must be called once before game starts.
     */
    public static void initializeEngine() {
        HelperUtils.freeMemory();
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

        // Create default error texture (checkerboard pattern)
        LevelLoader.defaultErrorTexture = new Texture(
                (byte)0, 8, 8, 0, 0,
                new int[]{16777215, 16711680}
        );
        byte[] row0 = new byte[]{17, 17, 17, 17, 17, 17, 17, 17};
        byte[] row1 = new byte[]{17, 16, 16, 16, 16, 17, 16, 17};
        byte[] row2 = new byte[]{17, 1, 1, 1, 1, 17, 1, 17};
        LevelLoader.defaultErrorTexture.setPixelData(0, row0);
        LevelLoader.defaultErrorTexture.setPixelData(2, row1);
        LevelLoader.defaultErrorTexture.setPixelData(4, row2);
        LevelLoader.defaultErrorTexture.setPixelData(6, row0);

        // Initialize rendering buffers using constants
        PortalRenderer.screenBuffer = new int[PortalRenderer.SCREEN_BUFFER_SIZE];
        PortalRenderer.depthBuffer = new short[PortalRenderer.VIEWPORT_HEIGHT];
        PortalRenderer.renderUtils = new RenderUtils();

        // Initialize angle correction table for perspective projection
        PortalRenderer.angleCorrectionTable = new int[PortalRenderer.VIEWPORT_WIDTH];
        for (int x = 0; x < PortalRenderer.VIEWPORT_WIDTH; x++) {
            int offsetFromCenter = (x - PortalRenderer.HALF_VIEWPORT_WIDTH) << 16;
            PortalRenderer.angleCorrectionTable[x] =
                    MathUtils.fixedPointDivide(offsetFromCenter, PortalRenderer.HALF_VIEWPORT_WIDTH_FP) >> 2;
        }

        // Initialize reciprocal lookup table (1/x) for fast division
        PortalRenderer.reciprocalTable = new int[PortalRenderer.VIEWPORT_HEIGHT + 1];
        PortalRenderer.reciprocalTable[0] = 0;
        for (int i = 1; i <= PortalRenderer.VIEWPORT_HEIGHT; i++) {
            PortalRenderer.reciprocalTable[i] = 65536 / i;
        }

        // Initialize skybox scaling parameters
        PortalRenderer.skyboxScaleX = MathUtils.fixedPointDivide(65536, 17301600);
        PortalRenderer.skyboxAngleFactor = MathUtils.fixedPointMultiply(
                MathUtils.fixedPointDivide(65536, 15794176), 102943
        );
        PortalRenderer.skyboxScaleY = MathUtils.fixedPointDivide(65536, 18874368);
        PortalRenderer.skyboxOffsetFactor = MathUtils.fixedPointDivide(65536, 411775);

        HelperUtils.freeMemory();
    }

    /**
     * Resets level state when loading a new level or restarting.
     */
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

    /**
     * Changes the skybox texture.
     *
     * @param textureId ID of the new skybox texture
     */
    public static void changeSkyboxTexture(byte textureId) {
        PortalRenderer.setSkyboxTexture(LevelLoader.getTexture(textureId));
    }

    /**
     * Renders a single frame to the screen.
     *
     * @param graphics Graphics context to draw to
     * @param currentTime Current game time in milliseconds
     * @return Camera vertical offset due to head bob
     */
    public static int renderFrame(Graphics graphics, int currentTime) {
        currentSector = LevelLoader.gameWorld.getRootBSPNode().findSectorAtPoint(player.x, player.z);

        int deltaTime = currentTime - lastGameLogicTime;
        lastGameLogicTime = currentTime;

        // Calculate head bob based on movement speed
        int movementSpeed = MathUtils.fastHypot(player.velocityX, player.velocityY);
        cameraBobTimer += (deltaTime * movementSpeed) >> 2;
        int headBobOffset = MathUtils.fastSin(cameraBobTimer);

        // Apply screen shake effect
        int shakeOffset = screenShake << 15;
        if ((screenShake & 1) > 0) {
            shakeOffset = -shakeOffset;
        }

        cameraHeight = ((currentSector.floorHeight + GameWorld.PLAYER_HEIGHT_OFFSET) << 16)
                + headBobOffset + shakeOffset;

        // Render the 3D world
        PortalRenderer.renderWorld(player.x, -cameraHeight, player.z, player.rotation);

        // Apply damage flash effect
        if (damageFlash) {
            int pixelCount = PortalRenderer.SCREEN_BUFFER_SIZE;
            for (int i = 0; i < pixelCount; i++) {
                PortalRenderer.screenBuffer[i] |= 0xFF0000;
            }
            damageFlash = false;
        }

        // Decay screen shake
        if (screenShake == 16) {
            screenShake--;
        }

        // Draw framebuffer to screen
        graphics.drawRGB(
                PortalRenderer.screenBuffer,
                0,
                PortalRenderer.VIEWPORT_WIDTH,
                0, 0,
                PortalRenderer.VIEWPORT_WIDTH,
                PortalRenderer.VIEWPORT_HEIGHT,
                false
        );

        // Check level exit trigger
        if (currentSector.getSectorType() == 666) {
            switch (MainGameCanvas.currentLevelId) {
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
                    break;
            }
        }

        // Tutorial hint
        if ((SaveSystem.gameProgressFlags & 1) == 0
                && MainGameCanvas.currentLevelId == 0
                && currentSector.sectorId == 31) {
            messageText = "press 1 to open the door";
            messageTimer = 30;
        }

        return headBobOffset;
    }

    /**
     * Updates game logic including physics, AI, and interactions.
     *
     * @return true if player died, false otherwise
     */
    public static boolean updateGameLogic() {
        if (messageTimer > 0) {
            messageTimer--;
        }

        // Apply player input forces
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

        // Handle player movement with collision detection
        int movementSpeed = MathUtils.fastHypot(player.velocityX, player.velocityY);
        WallDefinition hitWall = null;

        if (movementSpeed > 262144) {
            player.applyDampedVelocity();
            hitWall = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
            player.applyDampedVelocity();
            WallDefinition hitWall2 = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
            if (hitWall == null) {
                hitWall = hitWall2;
            }
        } else {
            player.applyVelocity();
            hitWall = LevelLoader.gameWorld.handlePlayerMovement(player, currentSector);
        }

        // Track interactable wall
        int wallType;
        if (!useKey) {
            if (hitWall != null) {
                wallType = hitWall.getWallType();
                if (wallType == 1 || wallType == 11 || wallType == 26
                        || wallType == 28 || wallType == 51 || wallType == 62) {
                    activeInteractable = hitWall;
                } else {
                    activeInteractable = null;
                }
            }
        } else {
            activeInteractable = null;
        }

        // Check if player is looking at interactable wall
        if (activeInteractable != null) {
            int playerAngle = player.rotation;
            int sinAngle = MathUtils.fastSin(102943 - playerAngle);
            int cosAngle = MathUtils.fastCos(102943 - playerAngle);
            int rayLength = 1310720;
            int rayEndX = player.x + MathUtils.fixedPointMultiply(rayLength, cosAngle);
            int rayEndZ = player.z + MathUtils.fixedPointMultiply(rayLength, sinAngle);

            Point2D[] vertices = LevelLoader.gameWorld.vertices;
            Point2D wallStart = vertices[activeInteractable.startVertexId & 0xFFFF];
            Point2D wallEnd = vertices[activeInteractable.endVertexId & 0xFFFF];

            if (GameWorld.doLineSegmentsIntersect(
                    player.x, player.z, rayEndX, rayEndZ,
                    wallStart.x, wallStart.y, wallEnd.x, wallEnd.y)) {
                interactionTimer++;
            } else {
                activeInteractable = null;
                interactionTimer = 0;
            }

            if (interactionTimer >= 50) {
                messageText = (activeInteractable.getWallType() == 62)
                        ? "press 1 to move the lift"
                        : "press 1 to open the door";
                messageTimer = 10;
            }
        } else {
            interactionTimer = 0;
        }

        // Toggle map
        if (toggleMapInput) {
            MainGameCanvas.mapEnabled = !MainGameCanvas.mapEnabled;
            toggleMapInput = false;
        }

        // Handle use key interactions
        if (useKey) {
            // Check if standing on elevator
            if (currentSector.getSectorType() == 10) {
                ElevatorController elevator = getElevatorController(currentSector);
                if (elevator.elevatorState == 0) {
                    elevator.elevatorState = (short) ((currentSector.floorHeight == elevator.minHeight) ? 1 : 2);
                }
            }

            // Cast ray to find interactable walls
            int playerAngle = player.rotation;
            int sinAngle = MathUtils.fastSin(102943 - playerAngle);
            int cosAngle = MathUtils.fastCos(102943 - playerAngle);
            int rayLength = 1310720;
            int rayEndX = player.x + MathUtils.fixedPointMultiply(rayLength, cosAngle);
            int rayEndZ = player.z + MathUtils.fixedPointMultiply(rayLength, sinAngle);

            WallDefinition[] walls = LevelLoader.gameWorld.wallDefinitions;
            Point2D[] vertices = LevelLoader.gameWorld.vertices;

            for (int i = 0; i < walls.length; i++) {
                WallDefinition wall = walls[i];
                wallType = wall.getWallType();

                if (wallType == 1 || wallType == 11 || wallType == 26
                        || wallType == 28 || wallType == 51 || wallType == 62) {

                    Point2D wallStart = vertices[wall.startVertexId & 0xFFFF];
                    Point2D wallEnd = vertices[wall.endVertexId & 0xFFFF];

                    if (GameWorld.doLineSegmentsIntersect(
                            player.x, player.z, rayEndX, rayEndZ,
                            wallStart.x, wallStart.y, wallEnd.x, wallEnd.y)) {

                        if ((SaveSystem.gameProgressFlags & 1) == 0) {
                            SaveSystem.gameProgressFlags = (byte)(SaveSystem.gameProgressFlags | 1);
                        }

                        DoorController door;

                        switch (wallType) {
                            case 1:
                                door = getDoorController(wall.backSurface.linkedSector);
                                door.doorState = 1;
                                door.targetCeilingHeight = wall.frontSurface.linkedSector.ceilingHeight;
                                break;

                            case 11:
                                if (MainGameCanvas.currentLevelId == 7 && ammoCounts[6] == 0) {
                                    messageText = "we'll need some dynamite|maybe i should look for some";
                                    messageTimer = 50;
                                    break;
                                }
                                MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId++;
                                LevelLoader.levelVariant = wall.getSpecialType();
                                levelTransitionState = 1;
                                break;

                            case 26:
                                if (keysCollected[0]) {
                                    door = getDoorController(wall.backSurface.linkedSector);
                                    door.doorState = 1;
                                    door.targetCeilingHeight = wall.frontSurface.linkedSector.ceilingHeight;
                                } else {
                                    messageText = keysCollected[1]
                                            ? "oops, i need another key..."
                                            : "oh, i need a key...";
                                    messageTimer = 50;
                                }
                                break;

                            case 28:
                                if (keysCollected[1]) {
                                    door = getDoorController(wall.backSurface.linkedSector);
                                    door.doorState = 1;
                                    door.targetCeilingHeight = wall.frontSurface.linkedSector.ceilingHeight;
                                } else {
                                    messageText = keysCollected[0]
                                            ? "oops, i need another key..."
                                            : "oh, i need a key...";
                                    messageTimer = 50;
                                }
                                break;

                            case 51:
                                MainGameCanvas.previousLevelId = MainGameCanvas.currentLevelId--;
                                LevelLoader.levelVariant = wall.getSpecialType();
                                levelTransitionState = -1;
                                break;

                            case 62:
                                SectorData elevatorSector = wall.backSurface.linkedSector;
                                ElevatorController elevator = getElevatorController(elevatorSector);
                                if (elevator.elevatorState == 0) {
                                    elevator.elevatorState =
                                            (short) ((elevatorSector.floorHeight == elevator.minHeight) ? 1 : 2);
                                }
                                break;
                        }
                        break;
                    }
                }
            }

            useKey = false;
        }

        // Update door controllers
        for (int i = 0; i < doorControllers.size(); i++) {
            DoorController door = (DoorController)doorControllers.elementAt(i);

            if (door.controlledSector == currentSector && door.doorState == 2) {
                door.doorState = 1;
            }

            SectorData controlledSector;

            switch (door.doorState) {
                case 0:
                    break;

                case 1:
                    controlledSector = door.controlledSector;
                    controlledSector.ceilingHeight = (short)(controlledSector.ceilingHeight + 2);
                    if (door.controlledSector.ceilingHeight >= door.targetCeilingHeight) {
                        door.controlledSector.ceilingHeight = door.targetCeilingHeight;
                        door.doorState = 100;
                    }
                    break;

                case 2:
                    controlledSector = door.controlledSector;
                    controlledSector.ceilingHeight = (short)(controlledSector.ceilingHeight - 2);
                    if (door.controlledSector.ceilingHeight <= door.controlledSector.floorHeight) {
                        door.controlledSector.ceilingHeight = door.controlledSector.floorHeight;
                        door.doorState = 0;
                    }
                    break;

                default:
                    door.doorState++;
                    if (door.doorState >= 200) {
                        door.doorState = 2;
                    }
                    break;
            }
        }

        // Update elevator controllers
        for (int i = 0; i < elevatorControllers.size(); i++) {
            ElevatorController elevator = (ElevatorController)elevatorControllers.elementAt(i);
            SectorData controlledSector;

            switch (elevator.elevatorState) {
                case 0:
                    break;

                case 1:
                    controlledSector = elevator.controlledSector;
                    controlledSector.ceilingHeight = (short)(controlledSector.ceilingHeight + 2);
                    controlledSector.floorHeight = (short)(controlledSector.floorHeight + 2);

                    if (elevator.controlledSector.floorHeight >= elevator.maxHeight) {
                        short heightDifference = (short)(elevator.controlledSector.ceilingHeight
                                - elevator.controlledSector.floorHeight);
                        elevator.controlledSector.floorHeight = elevator.maxHeight;
                        elevator.controlledSector.ceilingHeight =
                                (short)(elevator.maxHeight + heightDifference);
                        elevator.elevatorState = 0;
                    }
                    break;

                case 2:
                    controlledSector = elevator.controlledSector;
                    controlledSector.ceilingHeight = (short)(controlledSector.ceilingHeight - 2);
                    controlledSector.floorHeight = (short)(controlledSector.floorHeight - 2);

                    if (elevator.controlledSector.floorHeight <= elevator.minHeight) {
                        short heightDifference = (short)(elevator.controlledSector.ceilingHeight
                                - elevator.controlledSector.floorHeight);
                        elevator.controlledSector.floorHeight = elevator.minHeight;
                        elevator.controlledSector.ceilingHeight =
                                (short)(elevator.minHeight + heightDifference);
                        elevator.elevatorState = 0;
                    }
                    break;
            }
        }

        // Update projectiles
        if (LevelLoader.gameWorld.updateProjectiles()) {
            return true;
        }

        // Update enemy AI
        GameObject[] enemies = LevelLoader.gameWorld.staticObjects;

        for (int i = 0; i < enemies.length; i++) {
            GameObject enemy = enemies[i];

            if (enemy == null || enemy.aiState == -1) {
                continue;
            }

            Transform3D enemyTransform = enemy.transform;

            // Check if enemy should wake up
            if (enemy.aiState == 0) {
                if (LevelLoader.gameWorld.getSectorDataAtPoint(enemyTransform.x, enemyTransform.z)
                        .isSectorVisible(currentSector)) {

                    int distX = enemyTransform.x - player.x;
                    int distZ = enemyTransform.z - player.z;
                    if (distX < 0) distX = -distX;
                    if (distZ < 0) distZ = -distZ;

                    if (distX + distZ <= 67108864
                            && LevelLoader.gameWorld.checkLineOfSight(player, enemyTransform)) {
                        enemy.aiState = 1;
                    }
                }
            } else {
                int distX = enemyTransform.x - player.x;
                int distZ = enemyTransform.z - player.z;
                if (distX < 0) distX = -distX;
                if (distZ < 0) distZ = -distZ;

                if (distX + distZ > 67108864) {
                    enemy.aiState = 0;
                }
            }

            if (enemy.stateTimer > 0) {
                enemy.stateTimer--;
            }

            int enemyType = enemy.objectType;

            if (enemyType != 3001 && enemyType != 3002 && enemyType != 3003
                    && enemyType != 3004 && enemyType != 3005 && enemyType != 3006) {
                continue;
            }

            // Update AI state machine
            if (enemy.stateTimer == 0) {
                int randValue;

                switch (enemy.aiState) {
                    case 1:
                        enemy.aiState = 2;
                        enemy.stateTimer = (random.nextInt() & Integer.MAX_VALUE)
                                % MainGameCanvas.enemyReactionTime[difficultyLevel];
                        enemy.spriteFrameIndex = 0;
                        break;

                    case 2:
                        randValue = random.nextInt() & Integer.MAX_VALUE;
                        if ((randValue & 1) == 0) {
                            enemy.aiState = 3;
                            enemy.stateTimer = randValue % MainGameCanvas.var_180b[difficultyLevel]
                                    + MainGameCanvas.var_17b5[difficultyLevel];
                            enemy.spriteFrameIndex = 2;
                        } else {
                            enemy.aiState = 1;
                            enemy.stateTimer = (random.nextInt() & Integer.MAX_VALUE)
                                    % MainGameCanvas.var_1851[difficultyLevel];
                            enemy.spriteFrameIndex = 0;
                        }
                        break;

                    case 3:
                        SectorData enemySector = LevelLoader.gameWorld.getSectorDataAtPoint(
                                enemyTransform.x, enemyTransform.z);

                        if (LevelLoader.gameWorld.checkLineOfSight(player, enemyTransform)) {
                            enemy.aiState = 4;
                            enemy.stateTimer = 2;

                            if (enemyType != 3002) {
                                enemy.spriteFrameIndex = 3;
                            }

                            if (enemyType == 3001) {
                                HelperUtils.playSound(4, false, 100, 1);
                                LevelLoader.gameWorld.shootProjectile(enemyTransform, enemySector);
                            } else if (enemyType == 3002) {
                                HelperUtils.playSound(5, false, 80, 1);
                                LevelLoader.gameWorld.shootSpreadWeapon(enemyTransform, enemySector);
                            } else {
                                int damage = 0;
                                int[] damageTable = null;

                                switch (enemyType) {
                                    case 3003:
                                        HelperUtils.playSound(2, false, 80, 0);
                                        damageTable = MainGameCanvas.enemyDamageEasy;
                                        break;
                                    case 3004:
                                        HelperUtils.playSound(2, false, 80, 0);
                                        damageTable = MainGameCanvas.enemyDamageNormal;
                                        break;
                                    case 3005:
                                        HelperUtils.playSound(2, false, 80, 0);
                                        damageTable = MainGameCanvas.enemyDamageHard;
                                        break;
                                    case 3006:
                                        HelperUtils.playSound(3, false, 80, 0);
                                        damageTable = MainGameCanvas.var_12d2;
                                        break;
                                }

                                if (damageTable != null) {
                                    damage = damageTable[difficultyLevel];
                                }

                                if (damage > 0) {
                                    HelperUtils.vibrateDevice(damage * 10);
                                }

                                if (applyDamage(damage)) {
                                    return true;
                                }
                            }
                        } else {
                            enemy.aiState = 2;
                            enemy.stateTimer = (random.nextInt() & Integer.MAX_VALUE)
                                    % MainGameCanvas.enemyReactionTime[difficultyLevel];
                            enemy.spriteFrameIndex = 0;
                        }
                        break;

                    case 4:
                        enemy.aiState = 2;
                        enemy.stateTimer = (random.nextInt() & Integer.MAX_VALUE)
                                % MainGameCanvas.enemyReactionTime[difficultyLevel];
                        enemy.spriteFrameIndex = 0;
                        break;

                    case 5:
                        randValue = random.nextInt() & Integer.MAX_VALUE;
                        enemy.aiState = 3;
                        enemy.stateTimer = randValue % MainGameCanvas.var_180b[difficultyLevel]
                                + MainGameCanvas.var_17b5[difficultyLevel];
                        enemy.spriteFrameIndex = 2;
                        break;

                    case 6:
                        enemy.aiState = -1;
                        enemy.spriteFrameIndex = (enemyType == 3002) ? 5 : 6;
                        LevelLoader.gameWorld.spawnPickUp(enemy);
                        break;
                }
            }

            // Chase AI
            if (enemy.aiState == 2) {
                if ((enemy.stateTimer & 3) == 0) {
                    enemy.spriteFrameIndex = (enemy.spriteFrameIndex == 0) ? 1 : 0;
                }

                SectorData enemySector = LevelLoader.gameWorld.getSectorDataAtPoint(
                        enemyTransform.x, enemyTransform.z);

                int deltaX = enemyTransform.x - player.x;
                int deltaZ = enemyTransform.z - player.z;
                int distance = MathUtils.fastHypot(deltaX, deltaZ);

                if (distance > enemyAggroDistance) {
                    int velocityX = MathUtils.fixedPointMultiply(
                            MathUtils.preciseDivide(deltaX, distance), enemy.getMovementSpeed());
                    int velocityZ = MathUtils.fixedPointMultiply(
                            MathUtils.preciseDivide(deltaZ, distance), enemy.getMovementSpeed());

                    if ((random.nextInt() & Integer.MAX_VALUE)
                            % MainGameCanvas.var_1ad2[difficultyLevel] == 0) {
                        int tempVelX = velocityX;
                        if ((random.nextInt() & 1) == 0) {
                            velocityX += -velocityZ;
                            velocityZ += tempVelX;
                        } else {
                            velocityX += velocityZ;
                            velocityZ += -tempVelX;
                        }
                    }

                    int absVelX = velocityX > 0 ? velocityX : -velocityX;
                    int absVelZ = velocityZ > 0 ? velocityZ : -velocityZ;
                    int maxAbsVel = (absVelX >> 18) > (absVelZ >> 18) ? (absVelX >> 18) : (absVelZ >> 18);
                    int steps = maxAbsVel + 1;

                    int stepX = velocityX / steps;
                    int stepZ = velocityZ / steps;

                    for (int step = 0; step < steps; step++) {
                        tempTransform.x = enemyTransform.x - stepX;
                        tempTransform.z = enemyTransform.z - stepZ;

                        if (!LevelLoader.gameWorld.checkCollision(enemy, tempTransform, enemySector)) {
                            break;
                        }

                        enemyTransform.x = tempTransform.x;
                        enemyTransform.z = tempTransform.z;
                    }
                } else {
                    int randValue = random.nextInt() & Integer.MAX_VALUE;
                    enemy.aiState = 3;
                    enemy.stateTimer = randValue % MainGameCanvas.var_180b[difficultyLevel]
                            + MainGameCanvas.var_17b5[difficultyLevel];
                    enemy.spriteFrameIndex = 2;
                }
            }
        }

        // Damage floor
        if (currentSector.getSectorType() == 555) {
            HelperUtils.vibrateDevice(10);
            if (applyDamage(1)) {
                return true;
            }
        }

        // Decay screen shake
        if (screenShake < 16 && screenShake > 0) {
            screenShake--;
        }

        // Apply velocity damping
        player.scaleVelocity(39322, 65536, 39322, 26214);

        return false;
    }

    /**
     * Gets or creates a door controller for a sector.
     */
    private static DoorController getDoorController(SectorData sector) {
        for (int i = 0; i < doorControllers.size(); i++) {
            DoorController door = (DoorController)doorControllers.elementAt(i);
            if (door.controlledSector == sector) {
                return door;
            }
        }

        DoorController newDoor = new DoorController();
        newDoor.controlledSector = sector;
        doorControllers.addElement(newDoor);
        return newDoor;
    }

    /**
     * Gets or creates an elevator controller for a sector.
     */
    private static ElevatorController getElevatorController(SectorData sector) {
        for (int i = 0; i < elevatorControllers.size(); i++) {
            ElevatorController elevator = (ElevatorController)elevatorControllers.elementAt(i);
            if (elevator.controlledSector == sector) {
                return elevator;
            }
        }

        ElevatorController newElevator = new ElevatorController();
        newElevator.elevatorState = 0;
        newElevator.minHeight = 32767;
        newElevator.maxHeight = -32768;

        WallDefinition[] walls = LevelLoader.gameWorld.wallDefinitions;
        for (int i = 0; i < walls.length; i++) {
            WallDefinition wall = walls[i];

            if (wall.getWallType() == 62 && wall.backSurface.linkedSector == sector) {
                SectorData targetSector = wall.frontSurface.linkedSector;

                if (targetSector.floorHeight > newElevator.maxHeight) {
                    newElevator.maxHeight = targetSector.floorHeight;
                }
                if (targetSector.floorHeight < newElevator.minHeight) {
                    newElevator.minHeight = targetSector.floorHeight;
                }
            }
        }

        newElevator.controlledSector = sector;
        elevatorControllers.addElement(newElevator);
        return newElevator;
    }

    /**
     * Clears all input flags.
     */
    private static void clearInputState() {
        inputForward = false;
        inputLeft = false;
        inputBackward = false;
        inputRight = false;
        inputLookUp = false;
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

    /**
     * Cycles to the next available weapon with ammo.
     */
    public static int cycleWeaponForward(int currentWeapon) {
        currentWeapon++;
        int startWeapon = currentWeapon;

        if (currentWeapon > 7) {
            currentWeapon = 0;
        } else {
            currentWeapon = 0;

            for (int weaponId = startWeapon; weaponId <= 7; weaponId++) {
                int ammoType = (weaponId != 3 && weaponId != 4) ? weaponId : 1;

                if (weaponsAvailable[weaponId] && ammoCounts[ammoType] > 0) {
                    currentWeapon = weaponId;
                    break;
                }
            }
        }

        return currentWeapon;
    }

    /**
     * Finds the next available weapon with ammo, or returns fists.
     */
    public static int findNextAvailableWeapon(int preferredWeapon) {
        if (preferredWeapon == 0) {
            return preferredWeapon;
        }

        int ammoType = (preferredWeapon != 3 && preferredWeapon != 4) ? preferredWeapon : 1;
        if (ammoCounts[ammoType] > 0) {
            return preferredWeapon;
        }

        int fallbackWeapon = 0;
        for (int weaponId = 7; weaponId > 0; weaponId--) {
            if (weaponId == 6) continue;

            ammoType = (weaponId != 3 && weaponId != 4) ? weaponId : 1;
            if (weaponsAvailable[weaponId] && ammoCounts[ammoType] > 0) {
                fallbackWeapon = weaponId;
                break;
            }
        }

        if (fallbackWeapon == 0 && weaponsAvailable[6] && ammoCounts[6] > 0) {
            fallbackWeapon = 6;
        }

        return fallbackWeapon;
    }

    /**
     * Applies damage to player, reducing armor first then health.
     *
     * @return true if player died, false otherwise
     */
    public static boolean applyDamage(int damage) {
        playerArmor -= damage;
        if (playerArmor < 0) {
            damage = -playerArmor;
            playerArmor = 0;
        } else {
            damage = 0;
        }

        damageFlash = true;
        playerHealth -= damage;

        if (playerHealth <= 0) {
            playerHealth = 0;
            return true;
        }

        return false;
    }

    /**
     * Resets all player progress (health, weapons, keys, etc).
     */
    public static void resetPlayerProgress() {
        playerHealth = 100;
        playerArmor = 0;

        for (int i = 0; i < weaponsAvailable.length; i++) {
            weaponsAvailable[i] = false;
            ammoCounts[i] = 0;
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