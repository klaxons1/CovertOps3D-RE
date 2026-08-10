import javax.microedition.lcdui.Image;

/**
 * Handles initialization of rendering resources and level-specific asset loading.
 * Extracted from MainGameCanvas to separate resource management concerns.
 * Keeps original performance: reuses arrays, avoids allocations.
 */
public final class LevelResourceManager {

    private final MainGameCanvas canvas;
    private final FontRenderer fontRenderer;
    private final WeaponManager weaponManager;

    private GameObject[] cachedStaticObjects;
    private GameObject[] nextLevelObjects;

    public LevelResourceManager(MainGameCanvas canvas, FontRenderer fontRenderer, WeaponManager weaponManager) {
        this.canvas = canvas;
        this.fontRenderer = fontRenderer;
        this.weaponManager = weaponManager;
    }

    public void initializeGameResources(HudRenderer hudRenderer) {
        try {
            DebugLogger.log("LevelResourceManager", "initializeGameResources start");
            Image statusBar = Image.createImage("/gamedata/sprites/bar.png");
            Image crosshair = Image.createImage("/gamedata/sprites/aim.png");
            hudRenderer.setStatusBarImage(statusBar);
            hudRenderer.setCrosshairImage(crosshair);
            fontRenderer.loadLargeFont("/gamedata/sprites/font.png");

            weaponManager.initialize();

            MathUtils.initializeMathTables();
            GameEngine.initializeEngine();
            DebugLogger.log("LevelResourceManager", "initializeGameResources ok");
        } catch (Exception e) {
            DebugLogger.logException("LevelResourceManager.init", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("LevelResourceManager.init", e);
        }
    }

    public void loadLevelResources() {
        try {
            HelperUtils.freeMemory();
            String fullLevelPath = MainGameCanvas.LEVEL_PATH_PREFIX + MainGameCanvas.LEVEL_FILE_NAMES[MainGameCanvas.currentLevelId];
            if (MainGameCanvas.previousLevelId < MainGameCanvas.currentLevelId) {
                if (MainGameCanvas.previousLevelId > -1) {
                    cachedStaticObjects = LevelLoader.gameWorld.staticObjects;
                }
                boolean mapLoaded = LevelLoader.loadMapData(fullLevelPath, nextLevelObjects == null);
                if (!mapLoaded) {
                    DebugLogger.log("LevelResourceManager", "loadMapData FAILED path=" + fullLevelPath);
                    // Don't exit immediately, keep logging visible for debugging
                    // CovertOps3D.exitApplication();
                    return;
                }
                if (nextLevelObjects != null) {
                    LevelLoader.gameWorld.staticObjects = nextLevelObjects;
                    nextLevelObjects = null;
                } else {
                    GameEngine.keysCollected[0] = false;
                    GameEngine.keysCollected[1] = false;
                }
            } else {
                nextLevelObjects = LevelLoader.gameWorld.staticObjects;
                boolean mapLoaded = LevelLoader.loadMapData(fullLevelPath, cachedStaticObjects == null);
                if (!mapLoaded) {
                    DebugLogger.log("LevelResourceManager", "loadMapData FAILED backtrack path=" + fullLevelPath);
                    return;
                }
                if (cachedStaticObjects != null) {
                    LevelLoader.gameWorld.staticObjects = cachedStaticObjects;
                    cachedStaticObjects = null;
                }
            }

            HelperUtils.freeMemory();
            GameEngine.resetLevelState();

            LevelLoader.preloadTexture((byte)25);

            byte[] texSeries1A = new byte[]{-23, -25, -28, -30, -32, 0, 0};
            byte[] texSeries1B = new byte[]{-24, -26, -29, -31, -33, -34, -27};
            byte[] texSeries2A = new byte[]{-35, -36, -38, -39, -40, 0, 0};
            byte[] texSeries3A = new byte[]{-86, -88, -91, -93, -95, 0, 0};
            byte[] texSeries3B = new byte[]{-87, -89, -92, -94, -96, -97, -90};
            byte[] texSeries4A = new byte[]{-73, -75, -78, -80, -82, 0, 0};
            byte[] texSeries4B = new byte[]{-74, -76, -79, -81, -83, -84, -77};
            byte[] texSeriesDoor1 = new byte[]{-2, -1};
            byte[] texSeriesDoor2 = new byte[]{-3, 0};
            byte[] texSeries5A = new byte[]{-59, -61, -64, -66, -68, 0, 0};
            byte[] texSeries5B = new byte[]{-60, -62, -65, -67, -69, -70, -63};
            byte[] texSingle1 = new byte[]{-9};
            byte[] texSingle2 = new byte[]{-10};
            byte[] texSeries6A = new byte[]{-4, -6, -11, -13, 0, 0};
            byte[] texSeries6B = new byte[]{-5, -7, -12, -14, -15, -8};

            GameObject[] objects = LevelLoader.gameWorld.staticObjects;

            for (int i = 0; i < objects.length; ++i) {
                GameObject obj = objects[i];
                if (obj == null) continue;
                GameObject target;

                switch (obj.objectType) {
                    case 5: case 13:
                        obj.addSpriteFrame((byte)0, (byte)-53);
                        LevelLoader.preloadTexture((byte)-53);
                        continue;
                    case 10:
                        GameObject.preloadObjectTextures(obj, texSeriesDoor1, texSeriesDoor2);
                        if (MainGameCanvas.LEVEL_FILE_NAMES[MainGameCanvas.currentLevelId].equals("06c")) {
                            obj.spriteFrameIndex = 1;
                        }
                        continue;
                    case 12:
                        GameObject.preloadObjectTextures(obj, texSingle1, texSingle2);
                        continue;
                    case 26:
                        obj.addSpriteFrame((byte)0, (byte)-16);
                        LevelLoader.preloadTexture((byte)-16);
                        continue;
                    case 60:
                        obj.addSpriteFrame((byte)0, (byte)-18);
                        LevelLoader.preloadTexture((byte)-18);
                        continue;
                    case 61:
                        obj.addSpriteFrame((byte)0, (byte)-17);
                        LevelLoader.preloadTexture((byte)-17);
                        continue;
                    case 82:
                        obj.addSpriteFrame((byte)0, (byte)-21);
                        LevelLoader.preloadTexture((byte)-21);
                        continue;
                    case 2001:
                        obj.addSpriteFrame((byte)0, (byte)-19);
                        LevelLoader.preloadTexture((byte)-19);
                        continue;
                    case 2002:
                        obj.addSpriteFrame((byte)0, (byte)-20);
                        LevelLoader.preloadTexture((byte)-20);
                        continue;
                    case 2003:
                        obj.addSpriteFrame((byte)0, (byte)-22);
                        LevelLoader.preloadTexture((byte)-22);
                        continue;
                    case 2004:
                        obj.addSpriteFrame((byte)0, (byte)-43);
                        LevelLoader.preloadTexture((byte)-43);
                        continue;
                    case 2005:
                        obj.addSpriteFrame((byte)0, (byte)-50);
                        LevelLoader.preloadTexture((byte)-50);
                        continue;
                    case 2006:
                        obj.addSpriteFrame((byte)0, (byte)-72);
                        LevelLoader.preloadTexture((byte)-72);
                        continue;
                    case 2007:
                        target = obj;
                        break;
                    case 2008:
                        obj.addSpriteFrame((byte)0, (byte)-54);
                        LevelLoader.preloadTexture((byte)-54);
                        continue;
                    case 2010:
                        obj.addSpriteFrame((byte)0, (byte)-57);
                        LevelLoader.preloadTexture((byte)-57);
                        continue;
                    case 2012:
                        obj.addSpriteFrame((byte)0, (byte)-55);
                        LevelLoader.preloadTexture((byte)-55);
                        continue;
                    case 2013:
                        obj.addSpriteFrame((byte)0, (byte)-49);
                        LevelLoader.preloadTexture((byte)-49);
                        continue;
                    case 2014:
                        obj.addSpriteFrame((byte)0, (byte)-52);
                        LevelLoader.preloadTexture((byte)-52);
                        continue;
                    case 2015:
                        obj.addSpriteFrame((byte)0, (byte)-58);
                        LevelLoader.preloadTexture((byte)-58);
                        continue;
                    case 2024:
                        obj.addSpriteFrame((byte)0, (byte)-85);
                        LevelLoader.preloadTexture((byte)-85);
                        continue;
                    case 2047:
                        obj.addSpriteFrame((byte)0, (byte)-56);
                        LevelLoader.preloadTexture((byte)-56);
                        continue;
                    case 3001:
                        GameObject.preloadObjectTextures(obj, texSeries5A, texSeries5B);
                        LevelLoader.preloadTexture((byte)-57);
                        continue;
                    case 3002:
                        GameObject.preloadObjectTextures(obj, texSeries6A, texSeries6B);
                        LevelLoader.preloadTexture((byte)-56);
                        continue;
                    case 3003:
                        GameObject.preloadObjectTextures(obj, texSeries1A, texSeries1B);
                        LevelLoader.preloadTexture((byte)-48);
                        continue;
                    case 3004:
                        GameObject.preloadObjectTextures(obj, texSeries3A, texSeries3B);
                        LevelLoader.preloadTexture((byte)-54);
                        continue;
                    case 3005:
                        GameObject.preloadObjectTextures(obj, texSeries2A, texSeries3B);
                        LevelLoader.preloadTexture((byte)-48);
                        continue;
                    case 3006:
                        GameObject.preloadObjectTextures(obj, texSeries4A, texSeries4B);
                        LevelLoader.preloadTexture((byte)-54);
                        continue;
                    default:
                        target = obj;
                }

                target.addSpriteFrame((byte)0, (byte)-48);
                LevelLoader.preloadTexture((byte)-48);
            }

            LevelLoader.preloadTexture((byte)-44);
            LevelLoader.preloadTexture((byte)-45);
            LevelLoader.preloadTexture((byte)-46);
            LevelLoader.preloadTexture((byte)-47);
            LevelLoader.preloadTexture((byte)-71);
            LevelLoader.preloadTexture((byte)-51);
            LevelLoader.preloadTexture((byte)-43);

            if (MainGameCanvas.currentLevelId == 10) {
                LevelLoader.preloadTexture((byte)-72);
            }

            HelperUtils.freeMemory();

            boolean assetsLoaded = LevelLoader.loadGameAssets("/gamedata/textures/tx", 4, "/gamedata/textures/sp", 4);
            if (!assetsLoaded) {
                DebugLogger.log("LevelResourceManager", "loadGameAssets FAILED");
                return;
            }

            GameEngine.changeSkyboxTexture((byte)25);
            HelperUtils.freeMemory();
            DebugLogger.log("LevelResourceManager", "loadLevelResources ok id=" + MainGameCanvas.currentLevelId);
        } catch (Exception e) {
            DebugLogger.logException("LevelResourceManager.loadLevel", e);
        } catch (OutOfMemoryError e) {
            DebugLogger.logOutOfMemory("LevelResourceManager.loadLevel", e);
        }
    }
}
