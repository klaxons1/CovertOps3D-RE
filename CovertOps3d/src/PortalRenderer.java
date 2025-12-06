import java.util.Vector;

/**
 * Portal-based renderer for a 2.5D game engine.
 * Handles rendering of walls, floors, ceilings, skybox, and dynamic objects
 * using a BSP traversal and portal visibility system.
 */
public class PortalRenderer {

    // ==================== Screen Resolution Constants ====================

// ==================== Screen Resolution Constants ====================

    /** Screen width in pixels */
    public static final int VIEWPORT_WIDTH = 240;

    /** Screen height in pixels */
    public static final int VIEWPORT_HEIGHT = 288;

    /** Half of screen width (horizontal center) */
    public static final int HALF_VIEWPORT_WIDTH = VIEWPORT_WIDTH / 2;

    /** Half of screen height (vertical center) */
    public static final int HALF_VIEWPORT_HEIGHT = VIEWPORT_HEIGHT / 2;

    /** Maximum valid X coordinate */
    public static final int MAX_VIEWPORT_X = VIEWPORT_WIDTH - 1;

    /** Maximum valid Y coordinate */
    public static final int MAX_VIEWPORT_Y = VIEWPORT_HEIGHT - 1;

    /** Total number of pixels in screen buffer */
    public static final int SCREEN_BUFFER_SIZE = VIEWPORT_WIDTH * VIEWPORT_HEIGHT;

    /** Half screen width in fixed-point 16.16 format */
    public static final int HALF_VIEWPORT_WIDTH_FP = HALF_VIEWPORT_WIDTH << 16;

    /** Half screen height in fixed-point 16.16 format */
    private static final int HALF_VIEWPORT_HEIGHT_FP = HALF_VIEWPORT_HEIGHT << 16;

// ==================== Rendering Constants ====================

    /** Near clipping plane distance in fixed-point (5.0) */
    private static final int NEAR_CLIP_DISTANCE = 327680;

    /**
     * Projection constant for perspective transformation.
     * Calculated as HALF_SCREEN_WIDTH << 48 for 90° horizontal FOV.
     */
    private static final long PROJECTION_CONSTANT = (long) HALF_VIEWPORT_WIDTH << 48;

    /** Fixed-point rounding constant (0.5 in 16.16 format) */
    private static final int FP_HALF = 32768;

    // ==================== Public Fields ====================

    /** Screen pixel buffer for rendering */
    public static int[] screenBuffer;

    /** History of floor clipping arrays for each visible sector */
    public static Vector floorClipHistory;

    /** History of ceiling clipping arrays for each visible sector */
    public static Vector ceilingClipHistory;

    /** Flag indicating if gun fire lighting effect is active */
    public static boolean gunFireLighting = false;

    /** Skybox horizontal scale factor */
    public static int skyboxScaleX;

    /** Skybox angle correction factor */
    public static int skyboxAngleFactor;

    /** Skybox vertical scale factor */
    public static int skyboxScaleY;

    /** Skybox vertical offset factor */
    public static int skyboxOffsetFactor;

    /** Array of visible game objects for rendering */
    public static GameObject[] visibleGameObjects;

    /** Count of currently visible objects */
    public static int visibleObjectsCount;

    // ==================== Private Fields ====================

    /** Clipped wall segment start point (reused to avoid allocations) */
    private static Point2D clippedWallStart = new Point2D(0, 0);

    /** Clipped wall segment end point (reused to avoid allocations) */
    private static Point2D clippedWallEnd = new Point2D(0, 0);

    /** Projected ceiling Y coordinate at wall start */
    private static int projectedCeilingStart;

    /** Projected floor Y coordinate at wall start */
    private static int projectedFloorStart;

    /** Projected ceiling Y coordinate at wall end */
    private static int projectedCeilingEnd;

    /** Projected floor Y coordinate at wall end */
    private static int projectedFloorEnd;

    /** Clipped texture U coordinate at wall start */
    private static int clippedTextureStartU;

    /** Clipped texture U coordinate at wall end */
    private static int clippedTextureEndU;

    /** Current skybox texture */
    private static Texture skyboxTexture;

    /** Depth buffer for span rendering */
    static short[] depthBuffer;

    /** Floor span start Y coordinate */
    private static int floorSpanStart;

    /** Floor span end Y coordinate */
    private static int floorSpanEnd;

    /** Ceiling span start Y coordinate */
    private static int ceilingSpanStart;

    /** Ceiling span end Y coordinate */
    private static int ceilingSpanEnd;

    /** Last X column where floor span was updated */
    private static int lastFloorColumnX;

    /** Last X column where ceiling span was updated */
    private static int lastCeilingColumnX;

    /** Render utilities for span-based rendering */
    static RenderUtils renderUtils;

    /** Lookup table for angle corrections */
    static int[] angleCorrectionTable;

    /** Lookup table for reciprocal values (1/x) */
    static int[] reciprocalTable;

    /**
     * Clips a wall segment to the near clipping plane and projects it to screen coordinates.
     *
     * @param startPoint           Start point of the wall segment in view space
     * @param endPoint             End point of the wall segment in view space
     * @param relativeCeilingHeight Ceiling height relative to camera in fixed-point
     * @param relativeFloorHeight   Floor height relative to camera in fixed-point
     * @param textureOffset        Texture horizontal offset
     * @return true if the wall segment is visible after clipping, false otherwise
     */
    private static boolean clipAndProjectWallSegment(Point2D startPoint, Point2D endPoint,
                                                     int relativeCeilingHeight, int relativeFloorHeight, int textureOffset) {

        if (startPoint.y <= NEAR_CLIP_DISTANCE && endPoint.y <= NEAR_CLIP_DISTANCE) {
            return false;
        }

        int textureStartU = textureOffset << 16;
        int textureEndU = textureStartU + MathUtils.fastHypot(startPoint.x - endPoint.x, startPoint.y - endPoint.y);
        clippedTextureStartU = textureStartU;
        clippedTextureEndU = textureEndU;

        clippedWallStart.x = startPoint.x;
        clippedWallStart.y = startPoint.y;
        clippedWallEnd.x = endPoint.x;
        clippedWallEnd.y = endPoint.y;

        // Clip end point to near plane if behind it
        if (endPoint.y < NEAR_CLIP_DISTANCE) {
            int interpolationFactor = MathUtils.fixedPointDivide(startPoint.y - NEAR_CLIP_DISTANCE, startPoint.y - endPoint.y);
            clippedWallEnd.y = NEAR_CLIP_DISTANCE;

            if (interpolationFactor == Integer.MAX_VALUE) {
                clippedWallEnd.x = endPoint.x > startPoint.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                clippedTextureEndU = textureEndU > textureStartU ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (interpolationFactor == Integer.MIN_VALUE) {
                clippedWallEnd.x = endPoint.x > startPoint.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                clippedTextureEndU = textureEndU > textureStartU ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
                clippedWallEnd.x = (int)((long)(endPoint.x - startPoint.x) * (long)interpolationFactor >> 16) + startPoint.x;
                clippedTextureEndU = (int)((long)(textureEndU - textureStartU) * (long)interpolationFactor >> 16) + textureStartU;
            }
        }

        // Clip start point to near plane if behind it
        if (startPoint.y < NEAR_CLIP_DISTANCE) {
            int interpolationFactor = MathUtils.fixedPointDivide(endPoint.y - NEAR_CLIP_DISTANCE, endPoint.y - startPoint.y);
            clippedWallStart.y = NEAR_CLIP_DISTANCE;

            if (interpolationFactor == Integer.MAX_VALUE) {
                clippedWallStart.x = startPoint.x > endPoint.x ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                clippedTextureStartU = textureStartU > textureEndU ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else if (interpolationFactor == Integer.MIN_VALUE) {
                clippedWallStart.x = startPoint.x > endPoint.x ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                clippedTextureStartU = textureStartU > textureEndU ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
                clippedWallStart.x = (int)((long)(startPoint.x - endPoint.x) * (long)interpolationFactor >> 16) + endPoint.x;
                clippedTextureStartU = (int)((long)(textureStartU - textureEndU) * (long)interpolationFactor >> 16) + textureEndU;
            }
        }

        // Project to screen space
        long startDepthReciprocal = PROJECTION_CONSTANT / (long)clippedWallStart.y >> 16;
        long endDepthReciprocal = PROJECTION_CONSTANT / (long)clippedWallEnd.y >> 16;

        clippedWallStart.x = (int)((long)clippedWallStart.x * startDepthReciprocal >> 16);
        if (clippedWallStart.x > HALF_VIEWPORT_WIDTH_FP) {
            return false;
        }

        clippedWallEnd.x = (int)((long)clippedWallEnd.x * endDepthReciprocal >> 16);
        if (clippedWallEnd.x < -HALF_VIEWPORT_WIDTH_FP) {
            return false;
        }

        if (clippedWallEnd.x < clippedWallStart.x) {
            return false;
        }

        projectedCeilingStart = (int)((long)relativeCeilingHeight * startDepthReciprocal >> 16);
        projectedFloorStart = (int)((long)relativeFloorHeight * startDepthReciprocal >> 16);
        projectedCeilingEnd = (int)((long)relativeCeilingHeight * endDepthReciprocal >> 16);
        projectedFloorEnd = (int)((long)relativeFloorHeight * endDepthReciprocal >> 16);

        return true;
    }

    /**
     * Renders a solid (non-portal) wall segment with a single texture.
     *
     * @param wallSegment         The wall segment to render
     * @param wallDef             Wall definition containing surface data
     * @param surface             The wall surface with texture information
     * @param transformedVertices Array of transformed vertex positions
     * @param cameraX             Camera X position in fixed-point
     * @param cameraY             Camera Y (height) position in fixed-point
     * @param cameraZ             Camera Z position in fixed-point
     * @param viewAngle           View angle multiplied by 2
     */
    private static void renderSolidWallSegment(WallSegment wallSegment, WallDefinition wallDef,
                                               WallSurface surface, Point2D[] transformedVertices,
                                               int cameraX, int cameraY, int cameraZ, int viewAngle) {

        SectorData sectorData = surface.linkedSector;
        int relativeCeilingHeight = -cameraY + (-sectorData.ceilingHeight << 16);
        int relativeFloorHeight = -cameraY + (-sectorData.floorHeight << 16);

        int startVertexIndex = wallSegment.startVertexIndex & 0xFFFF;
        int endVertexIndex = wallSegment.endVertexIndex & 0xFFFF;
        int textureOffset = wallSegment.textureOffset & 0xFFFF;

        if (clipAndProjectWallSegment(transformedVertices[startVertexIndex],
                transformedVertices[endVertexIndex],
                relativeCeilingHeight, relativeFloorHeight, textureOffset)) {

            Point2D screenStart = clippedWallStart;
            Point2D screenEnd = clippedWallEnd;

            int screenStartX = (screenStart.x + HALF_VIEWPORT_WIDTH_FP) >> 16;
            int screenCeilingStartY = (projectedCeilingStart + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int screenEndX = (screenEnd.x + HALF_VIEWPORT_WIDTH_FP) >> 16;
            int screenCeilingEndY = (projectedCeilingEnd + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int screenFloorStartY = (projectedFloorStart + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int screenFloorEndY = (projectedFloorEnd + HALF_VIEWPORT_HEIGHT_FP) >> 16;

            int wallHeight = sectorData.ceilingHeight - sectorData.floorHeight;
            Texture wallTexture = LevelLoader.getTexture(surface.mainTextureId);

            int textureOffsetY = surface.textureOffsetY & 0xFFFF;
            int adjustedTextureOffsetY = wallTexture.height - wallHeight + textureOffsetY;
            if (!wallDef.isSecret()) {
                adjustedTextureOffsetY = textureOffsetY;
            }

            wallDef.markAsRendered();

            int textureOffsetX = (surface.textureOffsetX & 0xFFFF) << 16;

            drawWallColumn(sectorData, wallTexture, wallTexture,
                    screenStartX, screenCeilingStartY, screenFloorStartY, screenFloorStartY, screenFloorStartY, screenStart.y,
                    screenEndX, screenCeilingEndY, screenFloorEndY, screenFloorEndY, screenFloorEndY, screenEnd.y,
                    clippedTextureStartU + textureOffsetX, clippedTextureEndU - clippedTextureStartU,
                    adjustedTextureOffsetY, wallHeight, adjustedTextureOffsetY, wallHeight,
                    -cameraX, -cameraZ, viewAngle, relativeCeilingHeight, relativeFloorHeight);
        }
    }

    /**
     * Renders a portal wall segment with upper and lower textures.
     *
     * @param wallSegment         The wall segment to render
     * @param wallDef             Wall definition containing surface data
     * @param frontSurface        Front-facing wall surface
     * @param backSurface         Back-facing wall surface (portal target)
     * @param transformedVertices Array of transformed vertex positions
     * @param cameraX             Camera X position in fixed-point
     * @param cameraY             Camera Y (height) position in fixed-point
     * @param cameraZ             Camera Z position in fixed-point
     * @param viewAngle           View angle multiplied by 2
     */
    private static void renderPortalWallSegment(WallSegment wallSegment, WallDefinition wallDef,
                                                WallSurface frontSurface, WallSurface backSurface, Point2D[] transformedVertices,
                                                int cameraX, int cameraY, int cameraZ, int viewAngle) {

        SectorData frontSector = frontSurface.linkedSector;
        SectorData backSector = backSurface.linkedSector;

        int frontCeilingHeight = -cameraY + (-frontSector.ceilingHeight << 16);
        int frontFloorHeight = -cameraY + (-frontSector.floorHeight << 16);
        int backCeilingHeight = -cameraY + (-backSector.ceilingHeight << 16);
        int backFloorHeight = -cameraY + (-backSector.floorHeight << 16);

        int startVertexIndex = wallSegment.startVertexIndex & 0xFFFF;
        int endVertexIndex = wallSegment.endVertexIndex & 0xFFFF;
        int textureOffset = wallSegment.textureOffset & 0xFFFF;

        if (clipAndProjectWallSegment(transformedVertices[startVertexIndex],
                transformedVertices[endVertexIndex],
                frontCeilingHeight, frontFloorHeight, textureOffset)) {

            Point2D screenStart = clippedWallStart;
            Point2D screenEnd = clippedWallEnd;

            int screenStartX = (screenStart.x + HALF_VIEWPORT_WIDTH_FP) >> 16;
            int frontCeilingStartY = (projectedCeilingStart + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int frontFloorStartY = (projectedFloorStart + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int screenEndX = (screenEnd.x + HALF_VIEWPORT_WIDTH_FP) >> 16;
            int frontCeilingEndY = (projectedCeilingEnd + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int frontFloorEndY = (projectedFloorEnd + HALF_VIEWPORT_HEIGHT_FP) >> 16;

            int backCeilingStartY = (MathUtils.fixedPointDivide(backCeilingHeight, screenStart.y) * HALF_VIEWPORT_WIDTH + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int backFloorStartY = (MathUtils.fixedPointDivide(backFloorHeight, screenStart.y) * HALF_VIEWPORT_WIDTH + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int backCeilingEndY = (MathUtils.fixedPointDivide(backCeilingHeight, screenEnd.y) * HALF_VIEWPORT_WIDTH + HALF_VIEWPORT_HEIGHT_FP) >> 16;
            int backFloorEndY = (MathUtils.fixedPointDivide(backFloorHeight, screenEnd.y) * HALF_VIEWPORT_WIDTH + HALF_VIEWPORT_HEIGHT_FP) >> 16;

            int upperWallHeight = frontSector.ceilingHeight - backSector.ceilingHeight;
            int lowerWallHeight = backSector.floorHeight - frontSector.floorHeight;

            Texture upperTexture = LevelLoader.getTexture(frontSurface.upperTextureId);
            Texture lowerTexture = LevelLoader.getTexture(frontSurface.lowerTextureId);

            if (backSector.floorTextureId == 51) {
                upperTexture = LevelLoader.defaultErrorTexture;
            }
            if (backSector.ceilingTextureId == 51) {
                lowerTexture = LevelLoader.defaultErrorTexture;
            }

            int textureOffsetY = frontSurface.textureOffsetY & 0xFFFF;
            int upperTextureOffset = upperTexture.height - upperWallHeight + textureOffsetY;
            if (wallDef.isDoor()) {
                upperTextureOffset = textureOffsetY;
            }

            int lowerTextureOffset = frontSector.ceilingHeight - backSector.floorHeight + textureOffsetY;
            if (!wallDef.isSecret()) {
                lowerTextureOffset = textureOffsetY;
            }

            wallDef.markAsRendered();

            int textureOffsetX = (frontSurface.textureOffsetX & 0xFFFF) << 16;

            drawWallColumn(frontSector, upperTexture, lowerTexture,
                    screenStartX, frontCeilingStartY, backCeilingStartY, backFloorStartY, frontFloorStartY, screenStart.y,
                    screenEndX, frontCeilingEndY, backCeilingEndY, backFloorEndY, frontFloorEndY, screenEnd.y,
                    clippedTextureStartU + textureOffsetX, clippedTextureEndU - clippedTextureStartU,
                    upperTextureOffset, upperWallHeight, lowerTextureOffset, lowerWallHeight,
                    -cameraX, -cameraZ, viewAngle, frontCeilingHeight, frontFloorHeight);
        }
    }

    /**
     * Renders a wall segment, dispatching to solid or portal rendering as appropriate.
     *
     * @param wallSegment         The wall segment to render
     * @param transformedVertices Array of transformed vertex positions
     * @param cameraX             Camera X position in fixed-point
     * @param cameraY             Camera Y (height) position in fixed-point
     * @param cameraZ             Camera Z position in fixed-point
     * @param viewAngle           View angle multiplied by 2
     */
    private static void renderWallSegment(WallSegment wallSegment, Point2D[] transformedVertices,
                                          int cameraX, int cameraY, int cameraZ, int viewAngle) {

        WallDefinition wallDef = wallSegment.wallDefinition;
        WallSurface frontSurface = wallDef.frontSurface;
        WallSurface backSurface = wallDef.backSurface;

        if (backSurface != null) {
            if (wallSegment.isFrontFacing) {
                renderPortalWallSegment(wallSegment, wallDef, frontSurface, backSurface,
                        transformedVertices, cameraX, cameraY, cameraZ, viewAngle);
            } else {
                renderPortalWallSegment(wallSegment, wallDef, backSurface, frontSurface,
                        transformedVertices, cameraX, cameraY, cameraZ, viewAngle);
            }
        } else if (wallSegment.isFrontFacing) {
            renderSolidWallSegment(wallSegment, wallDef, frontSurface,
                    transformedVertices, cameraX, cameraY, cameraZ, viewAngle);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Renders all dynamic objects (sprites) within a sector.
     *
     * @param sector      The sector containing the objects
     * @param cameraX     Camera X position
     * @param cameraY     Camera Y (height) position
     * @param cameraZ     Camera Z position
     * @param sinAngle    Sine of view angle
     * @param cosAngle    Cosine of view angle
     */
    private static void renderDynamicObjects(Sector sector, int cameraX, int cameraY, int cameraZ,
                                             long sinAngle, long cosAngle) {

        SectorData sectorData = sector.getSectorData();
        Vector dynamicObjects = sector.dynamicObjects;
        visibleObjectsCount = 0;

        // Transform objects to view space and collect visible ones
        for (int i = 0; i < dynamicObjects.size(); i++) {
            GameObject gameObject = (GameObject)dynamicObjects.elementAt(i);
            Transform3D transform = gameObject.transform;

            int relativeX = transform.x - cameraX;
            int relativeZ = transform.z - cameraZ;

            gameObject.screenPos.x = (int)(cosAngle * (long)relativeX - sinAngle * (long)relativeZ >> 16);
            gameObject.screenPos.y = (int)(sinAngle * (long)relativeX + cosAngle * (long)relativeZ >> 16);

            if (gameObject.screenPos.y > NEAR_CLIP_DISTANCE) {
                byte spriteIndex1 = gameObject.getCurrentSprite1();
                byte spriteIndex2 = gameObject.getCurrentSprite2();

                if (spriteIndex1 != 0 || spriteIndex2 != 0) {
                    int objectHeight;

                    // Ceiling-mounted objects
                    if (gameObject.objectType >= 59 && gameObject.objectType <= 63) {
                        objectHeight = -sectorData.ceilingHeight << 16;
                    }
                    // Floating objects
                    else if (gameObject.objectType >= 100 && gameObject.objectType <= 102) {
                        objectHeight = -gameObject.transform.y;
                    }
                    // Floor-based objects
                    else {
                        objectHeight = -sectorData.floorHeight << 16;
                    }

                    gameObject.screenHeight = objectHeight - cameraY;

                    gameObject.torsoTexture = (spriteIndex1 != 0)
                            ? LevelLoader.textureTable[spriteIndex1 + 128]
                            : null;
                    gameObject.legsTexture = (spriteIndex2 != 0)
                            ? LevelLoader.textureTable[spriteIndex2 + 128]
                            : null;

                    visibleGameObjects[visibleObjectsCount++] = gameObject;

                    if (visibleObjectsCount >= 64) {
                        break;
                    }
                }
            }
        }

        // Sort objects by depth (insertion sort)
        for (int i = 1; i < visibleObjectsCount; i++) {
            GameObject currentObject = visibleGameObjects[i];
            int insertPos = i;

            while (insertPos > 0 && visibleGameObjects[insertPos - 1].compareDepth(currentObject)) {
                visibleGameObjects[insertPos] = visibleGameObjects[insertPos - 1];
                insertPos--;
            }

            visibleGameObjects[insertPos] = currentObject;
        }

        // Render sorted objects
        for (int i = 0; i < visibleObjectsCount; i++) {
            GameObject gameObject = visibleGameObjects[i];

            if (gameObject.projectToScreen()) {
                int lightLevel = sectorData.getLightLevel();
                int screenX = (gameObject.screenPos.x >> 16) + HALF_VIEWPORT_WIDTH;
                int screenY = (gameObject.screenHeight >> 16) + HALF_VIEWPORT_HEIGHT;
                int depth = gameObject.screenPos.y;

                if (gameObject.legsTexture != null) {
                    gameObject.calculateSpriteSize2();
                    drawSprite(gameObject.legsTexture, lightLevel, screenX, screenY,
                            depth, gameObject.spriteWidth2, gameObject.spriteHeight2);
                }

                if (gameObject.torsoTexture != null) {
                    gameObject.calculateSpriteSize1();
                    drawSprite(gameObject.torsoTexture, lightLevel, screenX, screenY,
                            depth, gameObject.spriteWidth1, gameObject.spriteHeight1);
                }
            }
        }
    }

    /**
     * Main rendering function that draws the entire visible world.
     *
     * @param playerX     Player X position
     * @param playerY     Player Y (height) position
     * @param playerZ     Player Z position
     * @param playerAngle Player view angle
     */
    static void renderWorld(int playerX, int playerY, int playerZ, int playerAngle) {
        LevelLoader.gameWorld.toggleProjectileSprites();
        Point2D[] transformedVertices = LevelLoader.gameWorld.transformVertices(playerX, playerZ, playerAngle);
        LevelLoader.gameWorld.updateWorld();

        BSPNode.visibleSectorsCount = 0;
        LevelLoader.gameWorld.getRootBSPNode().traverseBSP(GameEngine.player,
                LevelLoader.gameWorld.getSectorDataAtPoint(playerX, playerZ));

        int cameraZFixed = playerZ << 8;
        int cameraXFixed = playerX << 8;
        int doubleAngle = playerAngle << 1;
        int sinAngle = MathUtils.fastSin(playerAngle);
        int cosAngle = MathUtils.fastCos(playerAngle);

        gunFireLighting = MainGameCanvas.weaponSpriteFrame == 1 && GameEngine.currentWeapon != 0;

        renderUtils.resetRenderer();
        Sector.resetClipArrays();

        // Render walls for each visible sector
        for (int sectorIndex = 0; sectorIndex < BSPNode.visibleSectorsCount; sectorIndex++) {
            Sector currentSector = BSPNode.visibleSectorsList[sectorIndex];

            if (sectorIndex > 0 && Sector.isRenderComplete()) {
                BSPNode.visibleSectorsCount = sectorIndex;
                break;
            }

            // Expand clip history if needed
            if (sectorIndex >= floorClipHistory.size()) {
                short[] floorClipCopy = new short[VIEWPORT_WIDTH];
                short[] ceilingClipCopy = new short[VIEWPORT_WIDTH];
                floorClipHistory.addElement(floorClipCopy);
                ceilingClipHistory.addElement(ceilingClipCopy);
            }

            // Save current clip state
            System.arraycopy(Sector.floorClip, 0,
                    (short[])floorClipHistory.elementAt(sectorIndex), 0, VIEWPORT_WIDTH);
            System.arraycopy(Sector.ceilingClip, 0,
                    (short[])ceilingClipHistory.elementAt(sectorIndex), 0, VIEWPORT_WIDTH);

            // Render all walls in sector
            WallSegment[] walls = currentSector.walls;
            for (int wallIndex = 0; wallIndex < walls.length; wallIndex++) {
                renderWallSegment(walls[wallIndex], transformedVertices,
                        cameraXFixed, playerY, cameraZFixed, doubleAngle);
            }
        }

        // Render floor and ceiling spans
        renderUtils.renderAllSpans(sinAngle, cosAngle, -cameraXFixed, -cameraZFixed);

        // Render dynamic objects in back-to-front order
        for (int sectorIndex = BSPNode.visibleSectorsCount - 1; sectorIndex >= 0; sectorIndex--) {
            Sector currentSector = BSPNode.visibleSectorsList[sectorIndex];

            // Restore clip state for this sector
            System.arraycopy(floorClipHistory.elementAt(sectorIndex), 0,
                    Sector.floorClip, 0, VIEWPORT_WIDTH);
            System.arraycopy(ceilingClipHistory.elementAt(sectorIndex), 0,
                    Sector.ceilingClip, 0, VIEWPORT_WIDTH);

            renderDynamicObjects(currentSector, playerX, playerY, playerZ,
                    (long)sinAngle, (long)cosAngle);
        }
    }

    /**
     * Sets the texture to use for skybox rendering.
     *
     * @param texture The skybox texture
     */
    static void setSkyboxTexture(Texture texture) {
        skyboxTexture = texture;
    }

    /**
     * Draws a sprite at the specified screen position.
     *
     * @param texture     The sprite texture
     * @param lightLevel  Sector light level
     * @param screenX     Screen X position (center)
     * @param screenY     Screen Y position (bottom)
     * @param depth       Depth value for lighting calculation
     * @param spriteWidth Rendered sprite width
     * @param spriteHeight Rendered sprite height
     */
    private static void drawSprite(Texture texture, int lightLevel, int screenX, int screenY,
                                   int depth, int spriteWidth, int spriteHeight) {

        // Apply horizontal offset
        if (texture.horizontalOffset > 0) {
            screenX = screenX - texture.horizontalOffset * spriteWidth / texture.width;
        } else if (texture.horizontalOffset < 0) {
            screenX = screenX + texture.horizontalOffset;
        }

        // Apply vertical offset
        if (texture.verticalOffset > 0) {
            screenY = screenY - texture.verticalOffset * spriteHeight / texture.height;
        } else if (texture.verticalOffset < 0) {
            screenY = screenY + texture.verticalOffset;
        }

        int clipLeft = 0;
        int clipRight = spriteWidth;

        // Check horizontal visibility
        if (screenX >= VIEWPORT_WIDTH || screenX + spriteWidth < 0) {
            return;
        }

        // Clip to screen bounds
        if (screenX < 0) {
            clipLeft = -screenX;
        }
        if (screenX + spriteWidth >= VIEWPORT_WIDTH) {
            clipRight = MAX_VIEWPORT_X - screenX;
        }

        short textureWidth = texture.width;
        short textureHeight = texture.height;
        int textureStepU = (textureWidth << 16) / (spriteWidth + 1);
        int textureU = clipLeft * textureStepU;
        int textureColumn = textureU >>> 16;

        // Calculate light level with depth attenuation
        int depthFactor = depth >> 22;
        int effectiveLightLevel;
        if (gunFireLighting && depthFactor < 3) {
            effectiveLightLevel = lightLevel + (4 >> depthFactor);
        } else {
            effectiveLightLevel = lightLevel - depthFactor;
        }
        effectiveLightLevel = Math.max(0, Math.min(15, effectiveLightLevel));

        int[] colorPalette = texture.colorPalettes[effectiveLightLevel];
        int bottomY = screenY + spriteHeight;

        for (int column = clipLeft; column <= clipRight; column++) {
            drawSpriteColumn(texture.getPixelRow(textureColumn), textureColumn & 1, colorPalette,
                    column + screenX, screenY, bottomY, 0, textureHeight);
            textureU += textureStepU;
            textureColumn = textureU >>> 16;
        }
    }

    /**
     * Draws wall columns with upper and lower textures, handling portal visibility.
     *
     * @param sectorData          Sector data for lighting and floor/ceiling rendering
     * @param upperTexture        Upper wall texture (above portal)
     * @param lowerTexture        Lower wall texture (below portal)
     * @param startX              Screen X start position
     * @param ceilingStartY       Ceiling Y at start
     * @param upperBottomStartY   Upper wall bottom Y at start
     * @param lowerTopStartY      Lower wall top Y at start
     * @param floorStartY         Floor Y at start
     * @param startDepth          Depth at start
     * @param endX                Screen X end position
     * @param ceilingEndY         Ceiling Y at end
     * @param upperBottomEndY     Upper wall bottom Y at end
     * @param lowerTopEndY        Lower wall top Y at end
     * @param floorEndY           Floor Y at end
     * @param endDepth            Depth at end
     * @param textureStartU       Texture U at start
     * @param textureRangeU       Texture U range
     * @param upperTextureOffsetV Upper texture V offset
     * @param upperWallHeight     Upper wall height in texels
     * @param lowerTextureOffsetV Lower texture V offset
     * @param lowerWallHeight     Lower wall height in texels
     * @param cameraX             Camera X position
     * @param cameraZ             Camera Z position
     * @param viewAngle           View angle
     * @param relativeCeiling     Relative ceiling height
     * @param relativeFloor       Relative floor height
     */
    private static void drawWallColumn(SectorData sectorData, Texture upperTexture, Texture lowerTexture,
                                       int startX, int ceilingStartY, int upperBottomStartY, int lowerTopStartY, int floorStartY, int startDepth,
                                       int endX, int ceilingEndY, int upperBottomEndY, int lowerTopEndY, int floorEndY, int endDepth,
                                       int textureStartU, int textureRangeU,
                                       int upperTextureOffsetV, int upperWallHeight, int lowerTextureOffsetV, int lowerWallHeight,
                                       int cameraX, int cameraZ, int viewAngle, int relativeCeiling, int relativeFloor) {

        int clippedStartX = startX;
        int clippedEndX = endX;

        if (startX >= VIEWPORT_WIDTH || endX < 0) {
            return;
        }

        short[] floorClip = Sector.floorClip;
        short[] ceilingClip = Sector.ceilingClip;

        if (startX < 0) {
            clippedStartX = 0;
        }
        if (endX >= VIEWPORT_WIDTH) {
            clippedEndX = MAX_VIEWPORT_X;
        }

        short upperTextureHeight = upperTexture.height;
        short lowerTextureHeight = lowerTexture.height;

        int columnCount = endX - startX + 1;
        int columnOffset = clippedStartX - startX;

        // Calculate interpolation steps
        int ceilingYStep = ((ceilingEndY - ceilingStartY) << 16) / columnCount;
        int ceilingY = (ceilingStartY << 16) + columnOffset * ceilingYStep + FP_HALF;

        int upperBottomYStep = ((upperBottomEndY - upperBottomStartY) << 16) / columnCount;
        int upperBottomY = (upperBottomStartY << 16) + columnOffset * upperBottomYStep + FP_HALF;

        int lowerTopYStep = ((lowerTopEndY - lowerTopStartY) << 16) / columnCount;
        int lowerTopY = (lowerTopStartY << 16) + columnOffset * lowerTopYStep + FP_HALF;

        int floorYStep = ((floorEndY - floorStartY) << 16) / columnCount;
        int floorY = (floorStartY << 16) + columnOffset * floorYStep + FP_HALF;

        // Reset span tracking
        ceilingSpanEnd = Integer.MIN_VALUE;
        ceilingSpanStart = Integer.MIN_VALUE;
        floorSpanEnd = Integer.MIN_VALUE;
        floorSpanStart = Integer.MIN_VALUE;
        lastCeilingColumnX = Integer.MIN_VALUE;
        lastFloorColumnX = Integer.MIN_VALUE;

        short sectorId = (short)sectorData.sectorId;
        Sprite floorTexture = sectorData.floorTexture;
        Sprite ceilingTexture = sectorData.ceilingTexture;
        int lightLevel = sectorData.lightLevel = sectorData.getLightLevel();

        // Depth interpolation for perspective-correct texturing
        long depthRange = (long)(startDepth - endDepth);
        long depthRangeShifted = depthRange >> 16;
        long depthDenominator = (long)(endX - startX + 1 << 16) * (long)endDepth;

        sectorData.floorOffsetX = relativeCeiling * HALF_VIEWPORT_WIDTH >> 16;
        sectorData.ceilingOffsetX = relativeFloor * HALF_VIEWPORT_WIDTH >> 16;

        for (int column = clippedStartX; column <= clippedEndX; column++) {
            if (floorClip[column] < ceilingClip[column]) {
                // Calculate perspective-correct texture coordinate
                long columnOffset64 = (long)(column - startX << 16);
                long depthNumerator = (long)startDepth * columnOffset64;
                long depthTotal = depthDenominator + columnOffset64 * depthRange;
                int textureU = (int)(depthNumerator / (depthTotal >> 16));

                // Calculate light level with depth attenuation
                int depthFactor = (int)((long)startDepth - depthRangeShifted * (long)textureU >> 22);
                int effectiveLightLevel;
                if (gunFireLighting && depthFactor < 3) {
                    effectiveLightLevel = lightLevel + (4 >> depthFactor);
                } else {
                    effectiveLightLevel = lightLevel - depthFactor;
                }
                effectiveLightLevel = Math.max(0, Math.min(15, effectiveLightLevel));

                int screenCeilingY = ceilingY >> 16;
                int screenUpperBottomY = upperBottomY >> 16;
                int screenLowerTopY = lowerTopY >> 16;
                int screenFloorY = floorY >> 16;

                int textureColumn = ((int)((long)textureU * (long)textureRangeU >> 16) + textureStartU) >> 16;

                // Update ceiling span if portal exists
                if (lowerTopY >= floorY && ceilingTexture != null) {
                    updateCeilingSpan(sectorId, column, screenFloorY + 1, relativeFloor);
                    if (screenFloorY < ceilingClip[column]) {
                        ceilingClip[column] = (short)screenFloorY;
                    }
                }

                // Draw skybox for floor if no floor texture
                if (floorTexture == null) {
                    drawSkyboxColumn(column, 0, screenCeilingY, viewAngle);
                }

                // Draw upper wall or update floor span
                if (ceilingY < upperBottomY) {
                    drawWallTextureColumn(upperTexture.getPixelRowFast(textureColumn), textureColumn & 1,
                            upperTexture.colorPalettes[effectiveLightLevel], column,
                            screenCeilingY, screenUpperBottomY, upperTextureOffsetV, upperWallHeight, upperTextureHeight);

                    if (floorTexture != null) {
                        updateFloorSpan(sectorId, column, screenCeilingY, relativeCeiling);
                    }

                    if (screenUpperBottomY > floorClip[column] && upperTexture != LevelLoader.defaultErrorTexture) {
                        floorClip[column] = (short)screenUpperBottomY;
                    }
                } else if (floorTexture != null) {
                    updateFloorSpan(sectorId, column, screenCeilingY, relativeCeiling);
                    if (screenCeilingY > floorClip[column]) {
                        floorClip[column] = (short)screenCeilingY;
                    }
                }

                // Draw skybox for ceiling if no ceiling texture
                if (ceilingTexture == null) {
                    drawSkyboxColumn(column, screenFloorY + 1, MAX_VIEWPORT_Y, viewAngle);
                }

                // Draw lower wall
                if (lowerTopY < floorY) {
                    drawWallTextureColumn(lowerTexture.getPixelRowFast(textureColumn), textureColumn & 1,
                            lowerTexture.colorPalettes[effectiveLightLevel], column,
                            screenLowerTopY, screenFloorY, lowerTextureOffsetV, lowerWallHeight, lowerTextureHeight);

                    if (ceilingTexture != null) {
                        updateCeilingSpan(sectorId, column, screenFloorY + 1, relativeFloor);
                    }

                    if (screenLowerTopY < ceilingClip[column] && lowerTexture != LevelLoader.defaultErrorTexture) {
                        ceilingClip[column] = (short)screenLowerTopY;
                    }
                }
            }

            // Close portal if upper and lower walls meet
            if (upperBottomY == lowerTopY) {
                ceilingClip[column] = floorClip[column];
            }

            // Step interpolants
            ceilingY += ceilingYStep;
            upperBottomY += upperBottomYStep;
            lowerTopY += lowerTopYStep;
            floorY += floorYStep;
        }

        // Flush remaining ceiling spans
        if (ceilingSpanStart >= 0) {
            short lastColumnX = (short)lastCeilingColumnX;
            for (int row = ceilingSpanStart; row <= ceilingSpanEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], lastColumnX, sectorId, row);
            }
        }

        // Flush remaining floor spans
        if (floorSpanStart >= 0) {
            short lastColumnX = (short)lastFloorColumnX;
            for (int row = floorSpanStart; row <= floorSpanEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], lastColumnX, sectorId, row);
            }
        }
    }

    /**
     * Draws a horizontal span of floor or ceiling texture.
     *
     * @param startColumn   Start column (X) of the span
     * @param endColumn     End column (X) of the span
     * @param row           Row (Y) of the span
     * @param texturePixels Texture pixel data
     * @param colorPalettes Color palettes for different light levels
     * @param lightLevel    Base light level
     * @param sinAngle      Sine of view angle
     * @param cosAngle      Cosine of view angle
     * @param cameraX       Camera X position
     * @param heightOffset  Height offset for perspective
     * @param cameraZ       Camera Z position
     */
    public static void drawFlatSurface(int startColumn, int endColumn, int row, byte[] texturePixels,
                                       int[][] colorPalettes, int lightLevel, int sinAngle, int cosAngle,
                                       int cameraX, int heightOffset, int cameraZ) {

        int rowOffset = row * VIEWPORT_WIDTH;
        int rowFromCenter = row - HALF_VIEWPORT_HEIGHT;
        int perspectiveFactor = rowFromCenter < 0 ? -reciprocalTable[-rowFromCenter] : reciprocalTable[rowFromCenter];
        int scaledPerspective = (heightOffset * perspectiveFactor) >> 8;

        // Calculate light level with distance attenuation
        int depthFactor = scaledPerspective >> 14;
        int effectiveLightLevel;
        if (gunFireLighting && depthFactor < 3) {
            effectiveLightLevel = lightLevel + (4 >> depthFactor);
        } else {
            effectiveLightLevel = lightLevel - depthFactor;
        }
        effectiveLightLevel = Math.max(0, Math.min(15, effectiveLightLevel));

        int[] colorPalette = colorPalettes[effectiveLightLevel];

        int startAngle = angleCorrectionTable[startColumn];
        int endAngle = angleCorrectionTable[endColumn];

        int textureU = ((sinAngle + (cosAngle * startAngle >> 14)) * scaledPerspective - cameraX) >> 6;
        int textureV = (cosAngle - (sinAngle * startAngle >> 14)) * scaledPerspective - cameraZ;

        int angleDelta = (endAngle - startAngle) * reciprocalTable[endColumn - startColumn + 1] >> 16;
        int textureStepU = ((cosAngle * angleDelta >> 14) * scaledPerspective) >> 6;
        int textureStepV = ((-sinAngle * angleDelta >> 14) * scaledPerspective);

        int startPixelIndex = startColumn + rowOffset;
        int endPixelIndex = endColumn + rowOffset;
        int[] buffer = screenBuffer;

        for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex++) {
            buffer[pixelIndex] = colorPalette[texturePixels[(textureU & 16515072) + (textureV & 1056964608) >> 18]];
            textureU += textureStepU;
            textureV += textureStepV;
        }
    }

    /**
     * Updates the ceiling span tracking for span-based rendering.
     *
     * @param sectorId   Current sector ID
     * @param column     Current column (X)
     * @param topY       Top Y of the visible ceiling area
     * @param heightOffset Height offset for rendering
     */
    private static void updateCeilingSpan(short sectorId, int column, int topY, int heightOffset) {
        short floorClipY = Sector.floorClip[column];
        short ceilingClipY = Sector.ceilingClip[column];

        if (topY > ceilingClipY || topY <= HALF_VIEWPORT_HEIGHT || heightOffset <= 0) {
            return;
        }

        int clippedTopY = topY;
        short bottomY = ceilingClipY;

        if (topY < floorClipY) {
            clippedTopY = floorClipY;
        }

        short columnShort = (short)column;

        if (lastCeilingColumnX == column - 1) {
            // Continue existing span
            short prevColumnX = (short)lastCeilingColumnX;
            int newSpanStart = clippedTopY > ceilingSpanEnd + 1 ? clippedTopY : ceilingSpanEnd + 1;
            int extendedBottom = ceilingClipY < ceilingSpanStart - 1 ? ceilingClipY : ceilingSpanStart - 1;
            int trimmedStart = ceilingSpanStart > ceilingClipY + 1 ? ceilingSpanStart : ceilingClipY + 1;
            int trimmedEnd = ceilingSpanEnd < clippedTopY - 1 ? ceilingSpanEnd : clippedTopY - 1;

            // Add new rows to depth buffer
            for (int row = clippedTopY; row <= extendedBottom; row++) {
                depthBuffer[row] = columnShort;
            }
            for (int row = newSpanStart; row <= bottomY; row++) {
                depthBuffer[row] = columnShort;
            }

            // Flush completed span rows
            for (int row = ceilingSpanStart; row <= trimmedEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
            }
            for (int row = trimmedStart; row <= ceilingSpanEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
            }
        } else {
            // Start new span
            if (ceilingSpanStart >= 0) {
                short prevColumnX = (short)lastCeilingColumnX;
                for (int row = ceilingSpanStart; row <= ceilingSpanEnd; row++) {
                    renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
                }
            }

            for (int row = clippedTopY; row <= bottomY; row++) {
                depthBuffer[row] = columnShort;
            }
        }

        lastCeilingColumnX = column;
        ceilingSpanStart = clippedTopY;
        ceilingSpanEnd = bottomY;
    }

    /**
     * Updates the floor span tracking for span-based rendering.
     *
     * @param sectorId   Current sector ID
     * @param column     Current column (X)
     * @param bottomY    Bottom Y of the visible floor area
     * @param heightOffset Height offset for rendering
     */
    private static void updateFloorSpan(short sectorId, int column, int bottomY, int heightOffset) {
        short floorClipY = Sector.floorClip[column];
        short ceilingClipY = Sector.ceilingClip[column];

        if (bottomY < floorClipY || bottomY >= HALF_VIEWPORT_HEIGHT || heightOffset >= 0) {
            return;
        }

        int clippedBottomY = bottomY;
        if (bottomY > ceilingClipY) {
            clippedBottomY = ceilingClipY;
        }

        short columnShort = (short)column;

        if (lastFloorColumnX == column - 1) {
            // Continue existing span
            short prevColumnX = (short)lastFloorColumnX;
            int newSpanStart = floorClipY > floorSpanEnd + 1 ? floorClipY : floorSpanEnd + 1;
            int extendedTop = clippedBottomY < floorSpanStart - 1 ? clippedBottomY : floorSpanStart - 1;
            int trimmedStart = floorSpanStart > clippedBottomY + 1 ? floorSpanStart : clippedBottomY + 1;
            int trimmedEnd = floorSpanEnd < floorClipY - 1 ? floorSpanEnd : floorClipY - 1;

            // Add new rows to depth buffer
            for (int row = floorClipY; row <= extendedTop; row++) {
                depthBuffer[row] = columnShort;
            }
            for (int row = newSpanStart; row <= clippedBottomY; row++) {
                depthBuffer[row] = columnShort;
            }

            // Flush completed span rows
            for (int row = floorSpanStart; row <= trimmedEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
            }
            for (int row = trimmedStart; row <= floorSpanEnd; row++) {
                renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
            }
        } else {
            // Start new span
            if (floorSpanStart >= 0) {
                short prevColumnX = (short)lastFloorColumnX;
                for (int row = floorSpanStart; row <= floorSpanEnd; row++) {
                    renderUtils.addRenderSpan(depthBuffer[row], prevColumnX, sectorId, row);
                }
            }

            for (int row = floorClipY; row <= clippedBottomY; row++) {
                depthBuffer[row] = columnShort;
            }
        }

        lastFloorColumnX = column;
        floorSpanStart = floorClipY;
        floorSpanEnd = clippedBottomY;
    }

    /**
     * Draws a single vertical column of a sprite with transparency.
     *
     * @param pixelData    Packed pixel data (2 pixels per byte)
     * @param pixelNibble  Which nibble to read (0 = high, 1 = low)
     * @param colorPalette Color palette for the sprite
     * @param column       Screen column (X)
     * @param topY         Top Y of sprite column
     * @param bottomY      Bottom Y of sprite column
     * @param textureStartV Starting V coordinate in texture
     * @param textureHeight Texture height for scaling
     */
    private static void drawSpriteColumn(byte[] pixelData, int pixelNibble, int[] colorPalette,
                                         int column, int topY, int bottomY, int textureStartV, int textureHeight) {

        short floorClipY = Sector.floorClip[column];
        short ceilingClipY = Sector.ceilingClip[column];

        if (topY > ceilingClipY || bottomY < floorClipY) {
            return;
        }

        int clippedTopY = topY;
        int clippedBottomY = bottomY;

        if (bottomY > ceilingClipY) {
            clippedBottomY = ceilingClipY;
        }
        if (topY < floorClipY) {
            clippedTopY = floorClipY;
        }
        if (clippedTopY < 0) {
            clippedTopY = 0;
        }
        if (clippedBottomY >= VIEWPORT_HEIGHT) {
            clippedBottomY = MAX_VIEWPORT_Y;
        }

        if (clippedBottomY < 0 || clippedTopY >= VIEWPORT_HEIGHT || bottomY <= topY || textureStartV > pixelData.length) {
            return;
        }

        int startPixelIndex = clippedTopY * VIEWPORT_WIDTH + column;
        int endPixelIndex = clippedBottomY * VIEWPORT_WIDTH + column;

        int columnHeight = bottomY - topY;
        int textureStepV = columnHeight > VIEWPORT_HEIGHT
                ? (textureHeight << 16) / columnHeight
                : textureHeight * reciprocalTable[columnHeight];

        int textureV = (clippedTopY - topY) * textureStepV + (textureStartV << 16);
        int pixelDataLength = pixelData.length;
        int[] buffer = screenBuffer;

        if (pixelNibble == 0) {
            for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                int texelIndex = textureV >>> 16;
                if (texelIndex < pixelDataLength) {
                    int colorIndex = (pixelData[texelIndex] >> 4) & 15;
                    if (colorIndex != 0) {
                        buffer[pixelIndex] = colorPalette[colorIndex];
                    }
                }
                textureV += textureStepV;
            }
        } else {
            for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                int texelIndex = textureV >>> 16;
                if (texelIndex < pixelDataLength) {
                    int colorIndex = pixelData[texelIndex] & 15;
                    if (colorIndex != 0) {
                        buffer[pixelIndex] = colorPalette[colorIndex];
                    }
                }
                textureV += textureStepV;
            }
        }
    }

    /**
     * Draws a single vertical column of wall texture (no transparency).
     *
     * @param pixelData     Packed pixel data (2 pixels per byte)
     * @param pixelNibble   Which nibble to read (0 = high, 1 = low)
     * @param colorPalette  Color palette for the texture
     * @param column        Screen column (X)
     * @param topY          Top Y of wall column
     * @param bottomY       Bottom Y of wall column
     * @param textureOffsetV Texture V offset
     * @param wallHeight    Wall height in texels
     * @param textureHeight Texture height for masking
     */
    private static void drawWallTextureColumn(byte[] pixelData, int pixelNibble, int[] colorPalette,
                                              int column, int topY, int bottomY, int textureOffsetV, int wallHeight, int textureHeight) {

        short floorClipY = Sector.floorClip[column];
        short ceilingClipY = Sector.ceilingClip[column];

        if (bottomY < floorClipY || topY > ceilingClipY) {
            return;
        }

        int clippedTopY = topY;
        int clippedBottomY = bottomY;

        if (topY < floorClipY) {
            clippedTopY = floorClipY;
        }
        if (bottomY > ceilingClipY) {
            clippedBottomY = ceilingClipY;
        }

        int startPixelIndex = clippedTopY * VIEWPORT_WIDTH + column;
        int endPixelIndex = clippedBottomY * VIEWPORT_WIDTH + column;

        int columnHeight = bottomY - topY;
        int textureStepV = columnHeight > VIEWPORT_HEIGHT
                ? ((wallHeight - 1) << 16) / columnHeight
                : (wallHeight - 1) * reciprocalTable[columnHeight];

        int textureMask = textureHeight - 1;
        int textureV = (clippedTopY - topY) * textureStepV + ((textureOffsetV & textureMask) << 16);
        int[] buffer = screenBuffer;

        // Optimized loops for common texture heights
        switch (textureHeight + pixelNibble) {
            case 16: // Height 16, high nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[(pixelData[(textureV & 0xF0000) >> 16] >> 4) & 15];
                    textureV += textureStepV;
                }
                return;

            case 17: // Height 16, low nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[pixelData[(textureV & 0xF0000) >> 16] & 15];
                    textureV += textureStepV;
                }
                return;

            case 64: // Height 64, high nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[(pixelData[(textureV & 0x3F0000) >> 16] >> 4) & 15];
                    textureV += textureStepV;
                }
                return;

            case 65: // Height 64, low nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[pixelData[(textureV & 0x3F0000) >> 16] & 15];
                    textureV += textureStepV;
                }
                return;

            case 128: // Height 128, high nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[(pixelData[(textureV & 0x7F0000) >> 16] >> 4) & 15];
                    textureV += textureStepV;
                }
                return;

            case 129: // Height 128, low nibble
                for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                    buffer[pixelIndex] = colorPalette[pixelData[(textureV & 0x7F0000) >> 16] & 15];
                    textureV += textureStepV;
                }
                return;

            default:
                // Generic case - should not normally be reached
                return;
        }
    }

    /**
     * Draws a single vertical column of skybox texture.
     *
     * @param column    Screen column (X)
     * @param topY      Top Y of column
     * @param bottomY   Bottom Y of column
     * @param viewAngle Current view angle
     */
    private static void drawSkyboxColumn(int column, int topY, int bottomY, int viewAngle) {
        short floorClipY = Sector.floorClip[column];
        short ceilingClipY = Sector.ceilingClip[column];

        int clippedTopY = topY;
        int clippedBottomY = bottomY;

        if (topY < floorClipY) {
            clippedTopY = floorClipY;
        }
        if (bottomY > ceilingClipY) {
            clippedBottomY = ceilingClipY;
        }


        int columnAngle = MathUtils.fixedPointMultiply((column - HALF_VIEWPORT_WIDTH) << 16, skyboxAngleFactor);
        int angleCos = MathUtils.fastCos(columnAngle);
        int scaledX = MathUtils.fixedPointMultiply(column - HALF_VIEWPORT_WIDTH, skyboxScaleX);
        int angleSin = MathUtils.fastSin(columnAngle);

        int textureColumn = MathUtils.fixedPointMultiply(
                MathUtils.fixedPointMultiply(102943, angleSin + MathUtils.fixedPointMultiply(angleCos, scaledX)) + viewAngle,
                skyboxOffsetFactor) >> 8;

        byte[] pixelData = skyboxTexture.getPixelRowFast(textureColumn);
        int[] colorPalette = skyboxTexture.colorPalettes[8];

        int startPixelIndex = clippedTopY * VIEWPORT_WIDTH + column;
        int endPixelIndex = clippedBottomY * VIEWPORT_WIDTH + column;

        int textureStepV = MathUtils.fixedPointMultiply(angleCos * 200, skyboxScaleY);
        int textureV = -textureStepV * (HALF_VIEWPORT_HEIGHT - clippedTopY) + 6553600;

        int[] buffer = screenBuffer;

        if ((textureColumn & 1) == 0) {
            for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                buffer[pixelIndex] = colorPalette[(pixelData[(textureV >> 16) & 127] >> 4) & 15];
                textureV += textureStepV;
            }
        } else {
            for (int pixelIndex = startPixelIndex; pixelIndex <= endPixelIndex; pixelIndex += VIEWPORT_WIDTH) {
                buffer[pixelIndex] = colorPalette[pixelData[(textureV >> 16) & 127] & 15];
                textureV += textureStepV;
            }
        }
    }

    static void copyToScreenBuffer(int[] sprite, int width, int height, int x, int y, boolean redTint) {
        int bufferIdx = VIEWPORT_WIDTH * y + x;
        int spriteIdx = 0;

        if (!redTint) {
            for(int row = 0; row < height; ++row) {
                for(int col = 0; col < width; ++col) {
                    int pixel = sprite[spriteIdx++];
                    if (pixel != 16711935) {
                        screenBuffer[bufferIdx + col] = pixel;
                    }
                }
                bufferIdx += VIEWPORT_WIDTH;
            }
        } else {
            for(int row = 0; row < height; ++row) {
                for(int col = 0; col < width; ++col) {
                    int pixel = sprite[spriteIdx++];
                    if (pixel != 16711935) {
                        screenBuffer[bufferIdx + col] = pixel | 16711680;
                    }
                }
                bufferIdx += VIEWPORT_WIDTH;
            }
        }
    }
}